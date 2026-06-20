"""Execute neo4j_disease_layer.cypher against Neo4j."""
from neo4j import GraphDatabase
from pathlib import Path

CYPHER_FILE = Path(__file__).resolve().parents[1] / "neo4j_disease_layer.cypher"

driver = GraphDatabase.driver("bolt://127.0.0.1:7687", auth=("neo4j", "neo4j123456"))

content = CYPHER_FILE.read_text(encoding="utf-8")

# Split into individual statements; skip empty / comment-only blocks
statements = []
for block in content.split(";"):
    block = block.strip()
    if block and not block.startswith("//"):
        statements.append(block)

print(f"Total statements: {len(statements)}")

ok = 0
errors = 0
with driver.session() as session:
    for i, stmt in enumerate(statements):
        try:
            session.run(stmt)
            ok += 1
        except Exception as e:
            errors += 1
            msg = str(e)[:120]
            print(f"  [{i+1}] FAIL: {msg}")
            if errors >= 10:
                print("  Too many errors, stopping.")
                break
        if (i + 1) % 2000 == 0:
            print(f"  [{i+1}/{len(statements)}] ok={ok} err={errors}")

print(f"\nDone: {ok} succeeded, {errors} failed")

# Verify
with driver.session() as session:
    result = session.run("MATCH (d:Disease) RETURN count(d) AS cnt")
    cnt = result.single()["cnt"]
    print(f"Disease nodes in Neo4j: {cnt}")

driver.close()
