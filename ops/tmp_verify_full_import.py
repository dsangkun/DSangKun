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

res = {}
with conn.cursor() as cur:
    queries = {
        'm3_product': 'select count(*) from m3_product',
        'm3_product_owner_filled': "select count(*) from m3_product where owner_name is not null and owner_name <> ''",
        'm3_product_asin_map': 'select count(*) from m3_product_asin_map',
        'm3_biz_report_child_daily': 'select count(*) from m3_biz_report_child_daily',
        'm3_biz_report_parent_daily': 'select count(*) from m3_biz_report_parent_daily',
        'm3_sp_campaign_daily': 'select count(*) from m3_sp_campaign_daily',
        'm3_sbv_campaign_daily': 'select count(*) from m3_sbv_campaign_daily',
        'm3_product_daily_summary': 'select count(*) from m3_product_daily_summary',
        'summary_v4_rows': 'select count(*) from m3_product_daily_summary where summary_version = 4',
        'shops_in_child': 'select count(distinct shop_name) from m3_biz_report_child_daily',
        'shops_in_parent': 'select count(distinct shop_name) from m3_biz_report_parent_daily',
        'shops_in_sp': 'select count(distinct shop_name) from m3_sp_campaign_daily',
    }
    for key, sql in queries.items():
        cur.execute(sql)
        res[key] = cur.fetchone()[0]

    cur.execute("select distinct shop_name from m3_biz_report_child_daily order by shop_name")
    res['shop_names'] = [row[0] for row in cur.fetchall()]

    cur.execute("select product_code, product_name, owner_name from m3_product where owner_name is not null and owner_name <> '' order by product_code limit 20")
    res['owner_samples'] = cur.fetchall()

    cur.execute(
        """
        select p.product_code, p.product_name, p.owner_name, s.stat_date,
               s.order_item_count, s.sp_clicks, s.sp_spend, s.ad_orders,
               s.listing_conversion_rate, s.summary_version
          from m3_product_daily_summary s
          join m3_product p on p.id = s.product_id
         where p.product_code in ('B0D45HSHDF', 'B0DFXS9X72', 'B0D2D1CW6W')
         order by p.product_code, s.stat_date
         limit 20
        """
    )
    res['summary_samples'] = cur.fetchall()

print(json.dumps(res, ensure_ascii=False, indent=2, default=str))
conn.close()
