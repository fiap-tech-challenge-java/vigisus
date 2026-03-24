import os
import psycopg2

db_url = os.environ.get("DB_URL", "postgresql://vigisus:vigisus123@localhost:55432/vigisus")

with psycopg2.connect(db_url) as conn:
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) FROM estabelecimentos")
        print("estabelecimentos", cur.fetchone()[0])

        cur.execute("SELECT COUNT(*) FROM leitos")
        print("leitos", cur.fetchone()[0])

        cur.execute("SELECT COUNT(*) FROM servicos_especializados")
        print("servicos", cur.fetchone()[0])

        cur.execute("SELECT COUNT(*) FROM casos_dengue")
        print("casos_dengue", cur.fetchone()[0])

        cur.execute("SELECT COUNT(*) FROM estabelecimentos WHERE co_cnes = '0225363'")
        print("csv_0225363", cur.fetchone()[0])

        cur.execute("SELECT COUNT(*) FROM estabelecimentos WHERE co_cnes = '2078023'")
        print("mock_2078023", cur.fetchone()[0])
