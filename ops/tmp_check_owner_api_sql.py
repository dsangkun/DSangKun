import json
import pymysql

conn = pymysql.connect(
    host='127.0.0.1',
    port=3306,
    user='root',
    password='root123456',
    database='ecommerce_ops',
    charset='utf8mb4',
)

with conn.cursor() as cur:
    cur.execute(
        """
        SELECT p.product_code, p.product_name, p.owner_name, p.shop_name, s.stat_date
          FROM m3_product_daily_summary s
          JOIN m3_product p ON p.id = s.product_id
         WHERE s.stat_date = '2026-03-17'
           AND p.owner_name IS NOT NULL
         ORDER BY s.order_item_count DESC, p.product_code ASC
         LIMIT 10
        """
    )
    rows = cur.fetchall()

print(json.dumps(rows, ensure_ascii=False, indent=2, default=str))
conn.close()
