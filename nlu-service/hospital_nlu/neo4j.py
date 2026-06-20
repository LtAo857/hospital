from __future__ import annotations

from typing import Any, Dict, List, Optional


class Neo4jManager:
    def __init__(self, uri: str = "bolt://127.0.0.1:7687", user: str = "neo4j",
                 password: str = "neo4j123456", enabled: bool = True):
        self.uri = uri
        self.user = user
        self.password = password
        self.enabled = enabled
        self._driver = None

    def get_driver(self):
        if not self.enabled:
            return None
        if self._driver is None:
            try:
                from neo4j import GraphDatabase
                self._driver = GraphDatabase.driver(
                    self.uri,
                    auth=(self.user, self.password),
                )
            except Exception:
                return None
        return self._driver

    def query_graph(self, symptom_name: str, population_name: Optional[str] = None) -> List[Dict[str, Any]]:
        driver = self.get_driver()
        if driver is None:
            return []
        try:
            with driver.session() as session:
                if population_name:
                    cypher = (
                        "MATCH (s:Symptom {name: $symptom})-[r:BELONGS_TO]->(d:Department) "
                        "OPTIONAL MATCH (s)-[p:AFFECTS]->(pop:Population {name: $population}) "
                        "RETURN d.name AS department, r.weight AS baseScore, "
                        "CASE WHEN p IS NOT NULL AND p.preferredDepartment = d.name "
                        "     THEN p.relevance ELSE 0.0 END AS popBoost, "
                        "(r.weight + CASE WHEN p IS NOT NULL AND p.preferredDepartment = d.name "
                        "     THEN p.relevance ELSE 0.0 END) AS totalScore "
                        "ORDER BY totalScore DESC LIMIT 5"
                    )
                    result = session.run(cypher, symptom=symptom_name, population=population_name)
                else:
                    cypher = (
                        "MATCH (s:Symptom {name: $symptom})-[r:BELONGS_TO]->(d:Department) "
                        "RETURN d.name AS department, r.weight AS baseScore, "
                        "0.0 AS popBoost, r.weight AS totalScore "
                        "ORDER BY totalScore DESC LIMIT 5"
                    )
                    result = session.run(cypher, symptom=symptom_name)
                departments = []
                for record in result:
                    departments.append({
                        "department": record["department"],
                        "baseScore": record["baseScore"],
                        "popBoost": record.get("popBoost", 0.0),
                        "totalScore": record["totalScore"],
                    })
                return departments
        except Exception:
            return []

    def query_disease_by_symptom(self, symptom_name: str, limit: int = 3) -> List[Dict[str, Any]]:
        """Query diseases linked to a symptom with their descriptions and causes."""
        driver = self.get_driver()
        if driver is None:
            return []
        try:
            with driver.session() as session:
                cypher = (
                    "MATCH (s:Symptom {name: $symptom})-[:HAS_DISEASE]->(d:Disease) "
                    "RETURN d.name AS name, d.desc AS description, d.cause AS cause "
                    "LIMIT $limit"
                )
                result = session.run(cypher, symptom=symptom_name, limit=limit)
                diseases = []
                for record in result:
                    diseases.append({
                        "name": record["name"],
                        "description": record["description"] or "",
                        "cause": record["cause"] or "",
                    })
                return diseases
        except Exception:
            return []

    def query_disease_full(self, disease_name: str) -> Optional[Dict[str, Any]]:
        """Return all attributes of a disease by name."""
        driver = self.get_driver()
        if driver is None:
            return None
        try:
            with driver.session() as session:
                cypher = (
                    "MATCH (d:Disease {name: $name}) "
                    "RETURN d.name AS name, d.desc AS desc, d.cause AS cause, "
                    "d.drugs AS drugs, d.cure_way AS cure_way, d.prevent AS prevent, "
                    "d.do_eat AS do_eat, d.not_eat AS not_eat, d.check AS check"
                )
                result = session.run(cypher, name=disease_name)
                record = result.single()
                if record is None:
                    return None
                return {
                    "name": record["name"],
                    "desc": record["desc"] or "",
                    "cause": record["cause"] or "",
                    "drugs": record["drugs"] or "",
                    "cure_way": record["cure_way"] or "",
                    "prevent": record["prevent"] or "",
                    "do_eat": record["do_eat"] or "",
                    "not_eat": record["not_eat"] or "",
                    "check": record["check"] or "",
                }
        except Exception:
            return None
