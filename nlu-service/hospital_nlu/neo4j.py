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
