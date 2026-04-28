#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
模块3 ETL 第一版：当前优先打通五条链路，并补上产品日报汇总生成
1) ASIN 映射表 -> m3_product / m3_product_asin_map
2) 子 ASIN 业务报告 -> m3_biz_report_child_daily
3) 父 ASIN 业务报告 -> m3_biz_report_parent_daily
4) SP 广告报告 -> m3_sp_campaign_daily
5) SBV 广告报告 -> m3_sbv_campaign_daily
6) 产品日报汇总 -> m3_product_daily_summary

依赖：
  pip install pandas openpyxl pymysql

示例：
  python ops/import_module3_stage1.py ^
    --host 127.0.0.1 --port 3306 --user root --password 123456 ^
    --database ecommerce_ops --marketplace US --shop-name EXPERLAM ^
    --mapping-file D:\Programs\testdata\流量数据统计ASIN映射表.xlsx ^
    --biz-child-path D:\Programs\testdata\EXPERLAM-美国 ^
    --biz-parent-path D:\Programs\testdata\EXPERLAM-美国 ^
    --sp-path D:\Programs\testdata\EXPERLAM-美国 ^
    --sbv-path D:\Programs\testdata\SBV数据 ^
    --build-summary

说明：
- 当前版本仍属于 ETL Stage1，但已把原始层 5 条主链路补齐。
- SP / SBV 报表中的广告 ASIN 若暂时无法映射为子 ASIN 产品，则会保留原始明细，product_id 允许为空。
- 产品日报汇总当前优先保证“销售 / 流量 / 广告 / 评价”主指标稳定产出；
  natural_* 字段暂按保守策略输出，避免在自然流量口径未完全确认前写入错误值。
- 经 2026-03-17 ~ 2026-03-22 多组真实样本复核后，子卡片 listing_conversion_rate
  当前优先按 `订单商品总数 / 会话数 - 总计` 收敛；子卡片 ad_* 当前仍优先按 SP 口径生成。
"""

import argparse
import datetime as dt
import hashlib
import re
import sys
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

import pandas as pd
import pymysql

DATE_PATTERN = re.compile(r'(20\d{6})')

MAPPING_COLUMNS = [
    '父ASIN',
    '父ASIN产品名',
    '子ASIN',
    '子ASIN产品名',
    '是否有SBV',
    '归属人',
]

CHILD_BIZ_COLUMNS = {
    'parent_asin': '（父）ASIN',
    'child_asin': '（子）ASIN',
    'title': '标题',
    'sessions_total': '会话数 - 总计',
    'page_views_total': '页面浏览量 - 总计 ',
    'conversion_rate_total': '转化率 - 总计',
    'buy_box_pct': '推荐报价（推荐报价展示位）百分比 ',
    'unit_session_pct': '商品会话百分比',
    'ordered_product_sales': '已订购商品销售额',
    'units_ordered': '已订购商品数量',
    'total_order_items': '订单商品总数',
}

PARENT_BIZ_COLUMNS = {
    'parent_asin': '（父）ASIN',
    'title': '标题',
    'sessions_total': '会话数 - 总计',
    'page_views_total': '页面浏览量 - 总计 ',
    'conversion_rate_total': '转化率 - 总计',
    'buy_box_pct': '推荐报价（推荐报价展示位）百分比 ',
    'unit_session_pct': '商品会话百分比',
    'ordered_product_sales': '已订购商品销售额',
    'units_ordered': '已订购商品数量',
    'total_order_items': '订单商品总数',
}

SP_COLUMNS = {
    'report_date': '日期',
    'account_name': '广告组合名称',
    'currency_code': '货币',
    'campaign_name': '广告活动名称',
    'ad_group_name': '广告组名称',
    'publisher_name': '零售商',
    'country_name': '国家/地区',
    'advertised_sku': '广告SKU',
    'ad_asin': '广告ASIN',
    'impressions': '展示量',
    'clicks': '点击量',
    'ctr': '点击率 (CTR)',
    'cpc': '单次点击成本 (CPC)',
    'spend': '花费',
    'ad_sales_7d': '7天总销售额',
    'acos': '广告投入产出比 (ACOS) 总计',
    'roas': '总广告投资回报率 (ROAS)',
    'ad_orders_7d': '7天总订单数(#)',
    'cvr_7d': '7天的转化率',
}

SBV_COLUMN_ALIASES = {
    'campaign_name': ['广告活动名称', '广告活动'],
    'campaign_status': ['广告活动状态', '活动状态'],
    'ad_group_name': ['广告组名称', '广告组'],
    'promoted_asin': ['投放中的ASIN', '投放ASIN', 'ASIN', '推广ASIN'],
    'site_name': ['站点名称', '站点'],
    'impressions': ['曝光量', '展示量'],
    'clicks': ['点击量', '点击'],
    'ctr': ['点击率', 'CTR', '点击率(CTR)', '点击率（CTR）'],
    'cpc': ['单次点击成本', 'CPC', '单次点击成本(CPC)', '单次点击成本（CPC）'],
    'spend': ['花费', '广告花费'],
    'ad_sales': ['广告销售额', '销售额', '广告销售'],
    'acos': ['ACoS', 'ACOS', '广告投入产出比', '广告投入产出比(ACOS)', '广告投入产出比（ACOS）'],
    'ad_orders': ['广告订单数', '订单数', '广告订单'],
    'cvr': ['转化率', 'CVR', '点击转化率'],
}

SUMMARY_HINT_TERMS = ('汇总', '总计', 'summary', 'total')


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', required=True)
    parser.add_argument('--port', type=int, default=3306)
    parser.add_argument('--user', required=True)
    parser.add_argument('--password', required=True)
    parser.add_argument('--database', required=True)

    parser.add_argument('--marketplace', default='US')
    parser.add_argument('--shop-name', default=None)

    parser.add_argument('--mapping-file', default=None, help='流量数据统计ASIN映射表.xlsx')
    parser.add_argument('--biz-child-path', default=None, help='子业务报告文件或目录；目录将递归扫描')
    parser.add_argument('--biz-parent-path', default=None, help='父业务报告文件或目录；目录将递归扫描')
    parser.add_argument('--sp-path', default=None, help='SP 广告报告文件或目录；目录将递归扫描')
    parser.add_argument('--sbv-path', default=None, help='SBV 广告报告文件或目录；目录将递归扫描')

    parser.add_argument('--build-summary', action='store_true', help='导入完成后生成/重算产品日报汇总')
    parser.add_argument('--summary-start-date', default=None, help='汇总起始日期，格式 YYYY-MM-DD')
    parser.add_argument('--summary-end-date', default=None, help='汇总结束日期，格式 YYYY-MM-DD')

    parser.add_argument('--imported-by', default='ops/import_module3_stage1.py')
    return parser.parse_args()


def md5_file(path: Path) -> str:
    md5 = hashlib.md5()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(8192), b''):
            md5.update(chunk)
    return md5.hexdigest()


def parse_stat_date(name: str) -> dt.date:
    match = DATE_PATTERN.search(name)
    if not match:
        raise ValueError(f'无法从文件名提取日期: {name}')
    return dt.datetime.strptime(match.group(1), '%Y%m%d').date()


def parse_stat_date_from_path(path: Path) -> dt.date:
    candidates = [path.name] + [parent.name for parent in path.parents]
    for name in candidates:
        match = DATE_PATTERN.search(name)
        if match:
            return dt.datetime.strptime(match.group(1), '%Y%m%d').date()
    raise ValueError(f'无法从路径提取日期: {path}')


def parse_date_arg(value: Optional[str]) -> Optional[dt.date]:
    if not value:
        return None
    return dt.date.fromisoformat(value)


def normalize_text(value) -> Optional[str]:
    if value is None:
        return None
    if isinstance(value, float) and pd.isna(value):
        return None
    text = str(value).strip()
    if text == '' or text.lower() == 'nan':
        return None
    return text


def normalize_asin(value) -> Optional[str]:
    text = normalize_text(value)
    if text is None:
        return None
    return text.upper()


def normalize_header(value: str) -> str:
    text = str(value or '')
    text = text.replace('\n', '').replace('\r', '').replace('\t', '')
    text = re.sub(r'\s+', '', text)
    return text.strip().lower()


def resolve_columns(df: pd.DataFrame, alias_map: Dict[str, Sequence[str]], required_keys: Optional[Sequence[str]] = None) -> Dict[str, str]:
    normalized_to_actual = {normalize_header(col): col for col in df.columns}
    resolved = {}
    required = set(required_keys or alias_map.keys())

    for key, aliases in alias_map.items():
        actual = None
        for alias in aliases:
            candidate = normalized_to_actual.get(normalize_header(alias))
            if candidate:
                actual = candidate
                break
        if actual:
            resolved[key] = actual
        elif key in required:
            raise ValueError(f'缺少字段: {aliases}')
    return resolved


def contains_summary_hint(*values) -> bool:
    for value in values:
        text = normalize_text(value)
        if not text:
            continue
        lowered = text.lower()
        if any(term in lowered for term in SUMMARY_HINT_TERMS):
            return True
    return False


def parse_bool_flag(value) -> int:
    if value is None:
        return 0
    if isinstance(value, float) and pd.isna(value):
        return 0
    text = str(value).strip().lower()
    return 1 if text in {'1', '1.0', 'true', 'yes', 'y', '是'} else 0


def parse_int(value, default: int = 0) -> int:
    text = normalize_text(value)
    if text is None:
        return default
    text = text.replace(',', '')
    if text.endswith('.0'):
        text = text[:-2]
    try:
        return int(float(text))
    except ValueError:
        return default


def parse_decimal(value, default: float = 0.0) -> float:
    parsed = parse_decimal_nullable(value)
    return parsed if parsed is not None else default


def parse_decimal_nullable(value) -> Optional[float]:
    text = normalize_text(value)
    if text is None:
        return None
    text = text.replace(',', '')
    text = re.sub(r'^[A-Za-z$¥€£]+', '', text)
    try:
        return float(text)
    except ValueError:
        return None


def parse_percent_to_decimal(value) -> Optional[float]:
    text = normalize_text(value)
    if text is None:
        return None
    if text.endswith('%'):
        try:
            return float(text[:-1].replace(',', '')) / 100.0
        except ValueError:
            return None
    try:
        numeric = float(text.replace(',', ''))
        return numeric if numeric <= 1 else numeric / 100.0
    except ValueError:
        return None


def parse_excel_like_date(value, fallback_date: dt.date) -> dt.date:
    if value is None or (isinstance(value, float) and pd.isna(value)):
        return fallback_date
    if isinstance(value, dt.datetime):
        return value.date()
    if isinstance(value, dt.date):
        return value
    try:
        ts = pd.to_datetime(value)
        if pd.isna(ts):
            return fallback_date
        return ts.date()
    except Exception:
        return fallback_date


def safe_ratio(numerator: float, denominator: float) -> Optional[float]:
    if denominator is None or denominator == 0:
        return None
    return numerator / denominator


def ensure_batch(conn, batch_no: str, batch_type: str, shop_name: Optional[str], marketplace: Optional[str],
                 stat_date: Optional[dt.date], source_dir: Optional[str], source_note: Optional[str],
                 imported_by: Optional[str]) -> int:
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO m3_import_batch(
              batch_no, batch_type, shop_name, marketplace, stat_date,
              source_dir, source_note, import_status, imported_by, started_at, finished_at
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, 0, %s, NOW(), NULL)
            ON DUPLICATE KEY UPDATE
              batch_type=VALUES(batch_type),
              shop_name=VALUES(shop_name),
              marketplace=VALUES(marketplace),
              stat_date=VALUES(stat_date),
              source_dir=VALUES(source_dir),
              source_note=VALUES(source_note),
              import_status=0,
              imported_by=VALUES(imported_by),
              started_at=NOW(),
              finished_at=NULL
            """,
            (batch_no, batch_type, shop_name, marketplace, stat_date, source_dir, source_note, imported_by),
        )
        conn.commit()
        cur.execute('SELECT id FROM m3_import_batch WHERE batch_no = %s', (batch_no,))
        return cur.fetchone()[0]


def finish_batch(conn, batch_id: int, success: bool):
    with conn.cursor() as cur:
        cur.execute(
            'UPDATE m3_import_batch SET import_status=%s, finished_at=NOW() WHERE id=%s',
            (1 if success else 9, batch_id),
        )
    conn.commit()


def create_import_file(conn, batch_id: int, file_type: str, path: Path, shop_name: Optional[str],
                       marketplace: Optional[str], stat_date: Optional[dt.date]) -> int:
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO m3_import_file(
              batch_id, file_type, file_name, file_path, file_md5, file_size_bytes,
              shop_name, marketplace, stat_date, parse_status, error_message
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, 0, NULL)
            """,
            (
                batch_id,
                file_type,
                path.name,
                str(path),
                md5_file(path),
                path.stat().st_size,
                shop_name,
                marketplace,
                stat_date,
            ),
        )
        file_id = cur.lastrowid
    conn.commit()
    return file_id


def finish_import_file(conn, file_id: int, success: bool, error_message: Optional[str] = None):
    with conn.cursor() as cur:
        cur.execute(
            'UPDATE m3_import_file SET parse_status=%s, error_message=%s WHERE id=%s',
            (1 if success else 9, error_message[:1000] if error_message else None, file_id),
        )
    conn.commit()


def upsert_product_get_id(conn, product_code: str, product_name: str, marketplace: str,
                          shop_name: Optional[str], primary_asin: str, remark: Optional[str],
                          owner_name: Optional[str] = None) -> int:
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO m3_product(
              product_code, product_name, owner_name, marketplace, shop_name, primary_asin, remark
            ) VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
              id=LAST_INSERT_ID(id),
              product_name=COALESCE(NULLIF(VALUES(product_name), ''), product_name),
              owner_name=COALESCE(NULLIF(VALUES(owner_name), ''), owner_name),
              shop_name=COALESCE(VALUES(shop_name), shop_name),
              primary_asin=COALESCE(VALUES(primary_asin), primary_asin),
              remark=COALESCE(VALUES(remark), remark)
            """,
            (product_code, product_name, owner_name, marketplace, shop_name, primary_asin, remark),
        )
        return cur.lastrowid


def upsert_asin_map(conn, product_id: int, asin: Optional[str], asin_role: str, asin_name: Optional[str],
                    marketplace: str, shop_name: Optional[str], is_primary: int, runs_sbv: int):
    if not asin:
        return
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO m3_product_asin_map(
              product_id, asin, asin_role, asin_name, marketplace, shop_name,
              is_primary, runs_sbv
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
              asin_name=COALESCE(NULLIF(VALUES(asin_name), ''), asin_name),
              is_primary=GREATEST(is_primary, VALUES(is_primary)),
              runs_sbv=GREATEST(runs_sbv, VALUES(runs_sbv))
            """,
            (product_id, asin, asin_role, asin_name, marketplace, shop_name, is_primary, runs_sbv),
        )


def try_find_product_id_by_asin(conn, asin: Optional[str], marketplace: str, shop_name: Optional[str]) -> Optional[int]:
    if not asin:
        return None
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT product_id
              FROM m3_product_asin_map
             WHERE asin=%s
               AND marketplace=%s
               AND (shop_name=%s OR shop_name IS NULL)
             ORDER BY CASE WHEN shop_name=%s THEN 0 ELSE 1 END, is_primary DESC, id ASC
             LIMIT 1
            """,
            (asin, marketplace, shop_name, shop_name),
        )
        row = cur.fetchone()
        return row[0] if row else None


def import_mapping_file(conn, mapping_file: Path, marketplace: str, imported_by: str):
    batch_no = f'mapping-{dt.datetime.now().strftime("%Y%m%d%H%M%S")}'
    batch_id = ensure_batch(
        conn=conn,
        batch_no=batch_no,
        batch_type='mapping',
        shop_name=None,
        marketplace=marketplace,
        stat_date=None,
        source_dir=str(mapping_file.parent),
        source_note='ASIN映射表导入',
        imported_by=imported_by,
    )
    file_id = create_import_file(conn, batch_id, 'mapping', mapping_file, None, marketplace, None)

    try:
        df = pd.read_excel(mapping_file)
        missing_cols = [col for col in MAPPING_COLUMNS if col not in df.columns]
        if missing_cols:
            raise ValueError(f'映射表缺少字段: {missing_cols}')

        df = df[MAPPING_COLUMNS].copy()
        df['父ASIN'] = df['父ASIN'].ffill()
        df['父ASIN产品名'] = df['父ASIN产品名'].ffill()
        df['归属人'] = df['归属人'].ffill()
        df = df[df['子ASIN'].notna()].copy()

        inserted = 0
        with conn.cursor() as cur:
            for _, row in df.iterrows():
                child_asin = normalize_asin(row['子ASIN'])
                child_name = normalize_text(row['子ASIN产品名']) or child_asin
                parent_asin = normalize_asin(row['父ASIN'])
                parent_name = normalize_text(row['父ASIN产品名'])
                runs_sbv = parse_bool_flag(row['是否有SBV'])
                owner_name = normalize_text(row['归属人'])

                product_id = upsert_product_get_id(
                    conn=conn,
                    product_code=child_asin,
                    product_name=child_name,
                    marketplace=marketplace,
                    shop_name=None,
                    primary_asin=child_asin,
                    remark='imported from mapping file',
                    owner_name=owner_name,
                )
                upsert_asin_map(conn, product_id, child_asin, 'child', child_name, marketplace, None, 1, runs_sbv)
                upsert_asin_map(conn, product_id, parent_asin, 'parent', parent_name, marketplace, None, 0, runs_sbv)
                inserted += 1
            cur.execute(
                'UPDATE m3_import_batch SET import_status=1, finished_at=NOW(), source_note=%s WHERE id=%s',
                (f'ASIN映射表导入完成，记录数={inserted}', batch_id),
            )
        conn.commit()
        finish_import_file(conn, file_id, True)
        print(f'[OK] mapping imported: rows={inserted}, file={mapping_file.name}')
    except Exception as exc:
        conn.rollback()
        finish_import_file(conn, file_id, False, str(exc))
        finish_batch(conn, batch_id, False)
        raise


def find_files(path_value: str, suffix: str, keywords: Iterable[str]) -> Iterable[Path]:
    path = Path(path_value)
    if not path.exists():
        raise FileNotFoundError(f'路径不存在: {path}')
    if path.is_file():
        return [path]
    lowered_keywords = tuple(keywords)
    return sorted([p for p in path.rglob(f'*{suffix}') if any(k in p.name for k in lowered_keywords)])


def find_files_multi(path_value: str, suffixes: Sequence[str], keywords: Iterable[str]) -> Iterable[Path]:
    path = Path(path_value)
    if not path.exists():
        raise FileNotFoundError(f'路径不存在: {path}')
    if path.is_file():
        return [path]

    lowered_keywords = tuple(keywords)
    matched: List[Path] = []
    for suffix in suffixes:
        matched.extend([p for p in path.rglob(f'*{suffix}') if any(k in p.name for k in lowered_keywords)])
    return sorted(set(matched))


def import_biz_child_files(conn, path_value: str, shop_name: str, marketplace: str, imported_by: str):
    files = list(find_files(path_value, '.csv', ['业务报告-子AINS', '业务报告-子ASIN']))
    if not files:
        raise ValueError(f'未找到子业务报告文件: {path_value}')

    batch_no = f'biz-child-{dt.datetime.now().strftime("%Y%m%d%H%M%S")}'
    batch_id = ensure_batch(
        conn=conn,
        batch_no=batch_no,
        batch_type='biz_child',
        shop_name=shop_name,
        marketplace=marketplace,
        stat_date=None,
        source_dir=str(Path(path_value)),
        source_note='子ASIN业务报告导入',
        imported_by=imported_by,
    )

    total_rows = 0

    try:
        for path in files:
            stat_date = parse_stat_date(path.name)
            file_id = create_import_file(conn, batch_id, 'biz_child', path, shop_name, marketplace, stat_date)
            try:
                df = pd.read_csv(path)
                missing_cols = [col for col in CHILD_BIZ_COLUMNS.values() if col not in df.columns]
                if missing_cols:
                    raise ValueError(f'子业务报告缺少字段: {missing_cols}')

                file_rows = 0
                with conn.cursor() as cur:
                    for _, row in df.iterrows():
                        child_asin = normalize_asin(row[CHILD_BIZ_COLUMNS['child_asin']])
                        if not child_asin:
                            continue
                        parent_asin = normalize_asin(row[CHILD_BIZ_COLUMNS['parent_asin']])
                        title = normalize_text(row[CHILD_BIZ_COLUMNS['title']]) or child_asin

                        product_id = upsert_product_get_id(
                            conn=conn,
                            product_code=child_asin,
                            product_name=title,
                            marketplace=marketplace,
                            shop_name=shop_name,
                            primary_asin=child_asin,
                            remark='auto-created/updated from child business report',
                        )
                        upsert_asin_map(conn, product_id, child_asin, 'child', title, marketplace, shop_name, 1, 0)
                        upsert_asin_map(conn, product_id, parent_asin, 'parent', None, marketplace, shop_name, 0, 0)

                        cur.execute(
                            """
                            INSERT INTO m3_biz_report_child_daily(
                              child_asin, parent_asin, title, marketplace, shop_name, stat_date,
                              sessions_total, page_views_total, conversion_rate_total, buy_box_pct, unit_session_pct,
                              ordered_product_sales, units_ordered, total_order_items,
                              source_file_id, batch_id
                            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                            ON DUPLICATE KEY UPDATE
                              parent_asin=VALUES(parent_asin),
                              title=VALUES(title),
                              sessions_total=VALUES(sessions_total),
                              page_views_total=VALUES(page_views_total),
                              conversion_rate_total=VALUES(conversion_rate_total),
                              buy_box_pct=VALUES(buy_box_pct),
                              unit_session_pct=VALUES(unit_session_pct),
                              ordered_product_sales=VALUES(ordered_product_sales),
                              units_ordered=VALUES(units_ordered),
                              total_order_items=VALUES(total_order_items),
                              source_file_id=VALUES(source_file_id),
                              batch_id=VALUES(batch_id)
                            """,
                            (
                                child_asin,
                                parent_asin,
                                title,
                                marketplace,
                                shop_name,
                                stat_date,
                                parse_int(row[CHILD_BIZ_COLUMNS['sessions_total']]),
                                parse_int(row[CHILD_BIZ_COLUMNS['page_views_total']]),
                                parse_percent_to_decimal(row[CHILD_BIZ_COLUMNS['conversion_rate_total']]),
                                parse_percent_to_decimal(row[CHILD_BIZ_COLUMNS['buy_box_pct']]),
                                parse_percent_to_decimal(row[CHILD_BIZ_COLUMNS['unit_session_pct']]),
                                parse_decimal(row[CHILD_BIZ_COLUMNS['ordered_product_sales']]),
                                parse_int(row[CHILD_BIZ_COLUMNS['units_ordered']]),
                                parse_int(row[CHILD_BIZ_COLUMNS['total_order_items']]),
                                file_id,
                                batch_id,
                            ),
                        )
                        file_rows += 1

                    cur.execute('UPDATE m3_import_file SET parse_status=1, error_message=NULL WHERE id=%s', (file_id,))
                conn.commit()
                total_rows += file_rows
                print(f'[OK] biz_child imported: rows={file_rows}, file={path.name}')
            except Exception as exc:
                conn.rollback()
                finish_import_file(conn, file_id, False, str(exc))
                raise

        with conn.cursor() as cur:
            cur.execute(
                'UPDATE m3_import_batch SET import_status=1, finished_at=NOW(), source_note=%s WHERE id=%s',
                (f'子ASIN业务报告导入完成，文件数={len(files)}，记录数={total_rows}', batch_id),
            )
        conn.commit()
    except Exception:
        finish_batch(conn, batch_id, False)
        raise


def import_biz_parent_files(conn, path_value: str, shop_name: str, marketplace: str, imported_by: str):
    files = list(find_files(path_value, '.csv', ['业务报告-父AINS', '业务报告-父ASIN']))
    if not files:
        raise ValueError(f'未找到父业务报告文件: {path_value}')

    batch_no = f'biz-parent-{dt.datetime.now().strftime("%Y%m%d%H%M%S")}'
    batch_id = ensure_batch(
        conn=conn,
        batch_no=batch_no,
        batch_type='biz_parent',
        shop_name=shop_name,
        marketplace=marketplace,
        stat_date=None,
        source_dir=str(Path(path_value)),
        source_note='父ASIN业务报告导入',
        imported_by=imported_by,
    )

    total_rows = 0

    try:
        for path in files:
            stat_date = parse_stat_date(path.name)
            file_id = create_import_file(conn, batch_id, 'biz_parent', path, shop_name, marketplace, stat_date)
            try:
                df = pd.read_csv(path)
                missing_cols = [col for col in PARENT_BIZ_COLUMNS.values() if col not in df.columns]
                if missing_cols:
                    raise ValueError(f'父业务报告缺少字段: {missing_cols}')

                file_rows = 0
                with conn.cursor() as cur:
                    for _, row in df.iterrows():
                        parent_asin = normalize_asin(row[PARENT_BIZ_COLUMNS['parent_asin']])
                        if not parent_asin:
                            continue
                        title = normalize_text(row[PARENT_BIZ_COLUMNS['title']]) or parent_asin

                        cur.execute(
                            """
                            INSERT INTO m3_biz_report_parent_daily(
                              parent_asin, title, marketplace, shop_name, stat_date,
                              sessions_total, page_views_total, conversion_rate_total, buy_box_pct, unit_session_pct,
                              ordered_product_sales, units_ordered, total_order_items,
                              source_file_id, batch_id
                            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                            ON DUPLICATE KEY UPDATE
                              title=VALUES(title),
                              sessions_total=VALUES(sessions_total),
                              page_views_total=VALUES(page_views_total),
                              conversion_rate_total=VALUES(conversion_rate_total),
                              buy_box_pct=VALUES(buy_box_pct),
                              unit_session_pct=VALUES(unit_session_pct),
                              ordered_product_sales=VALUES(ordered_product_sales),
                              units_ordered=VALUES(units_ordered),
                              total_order_items=VALUES(total_order_items),
                              source_file_id=VALUES(source_file_id),
                              batch_id=VALUES(batch_id)
                            """,
                            (
                                parent_asin,
                                title,
                                marketplace,
                                shop_name,
                                stat_date,
                                parse_int(row[PARENT_BIZ_COLUMNS['sessions_total']]),
                                parse_int(row[PARENT_BIZ_COLUMNS['page_views_total']]),
                                parse_percent_to_decimal(row[PARENT_BIZ_COLUMNS['conversion_rate_total']]),
                                parse_percent_to_decimal(row[PARENT_BIZ_COLUMNS['buy_box_pct']]),
                                parse_percent_to_decimal(row[PARENT_BIZ_COLUMNS['unit_session_pct']]),
                                parse_decimal(row[PARENT_BIZ_COLUMNS['ordered_product_sales']]),
                                parse_int(row[PARENT_BIZ_COLUMNS['units_ordered']]),
                                parse_int(row[PARENT_BIZ_COLUMNS['total_order_items']]),
                                file_id,
                                batch_id,
                            ),
                        )
                        file_rows += 1

                    cur.execute('UPDATE m3_import_file SET parse_status=1, error_message=NULL WHERE id=%s', (file_id,))
                conn.commit()
                total_rows += file_rows
                print(f'[OK] biz_parent imported: rows={file_rows}, file={path.name}')
            except Exception as exc:
                conn.rollback()
                finish_import_file(conn, file_id, False, str(exc))
                raise

        with conn.cursor() as cur:
            cur.execute(
                'UPDATE m3_import_batch SET import_status=1, finished_at=NOW(), source_note=%s WHERE id=%s',
                (f'父ASIN业务报告导入完成，文件数={len(files)}，记录数={total_rows}', batch_id),
            )
        conn.commit()
    except Exception:
        finish_batch(conn, batch_id, False)
        raise


def import_sp_files(conn, path_value: str, shop_name: str, marketplace: str, imported_by: str):
    files = list(find_files(path_value, '.xlsx', ['SP广告报告-']))
    if not files:
        raise ValueError(f'未找到 SP 广告报告文件: {path_value}')

    batch_no = f'sp-{dt.datetime.now().strftime("%Y%m%d%H%M%S")}'
    batch_id = ensure_batch(
        conn=conn,
        batch_no=batch_no,
        batch_type='sp',
        shop_name=shop_name,
        marketplace=marketplace,
        stat_date=None,
        source_dir=str(Path(path_value)),
        source_note='SP广告报告导入',
        imported_by=imported_by,
    )

    total_rows = 0

    try:
        for path in files:
            stat_date = parse_stat_date(path.name)
            file_id = create_import_file(conn, batch_id, 'sp', path, shop_name, marketplace, stat_date)
            try:
                df = pd.read_excel(path)
                missing_cols = [col for col in SP_COLUMNS.values() if col not in df.columns]
                if missing_cols:
                    raise ValueError(f'SP 广告报告缺少字段: {missing_cols}')

                file_rows = 0
                with conn.cursor() as cur:
                    for row_no, (_, row) in enumerate(df.iterrows(), start=2):
                        campaign_name = normalize_text(row[SP_COLUMNS['campaign_name']])
                        ad_asin = normalize_asin(row[SP_COLUMNS['ad_asin']])
                        if not campaign_name:
                            continue

                        product_id = try_find_product_id_by_asin(conn, ad_asin, marketplace, shop_name)
                        report_date = parse_excel_like_date(row[SP_COLUMNS['report_date']], stat_date)

                        cur.execute(
                            """
                            INSERT INTO m3_sp_campaign_daily(
                              product_id, ad_asin, report_date, account_name, currency_code,
                              marketplace, shop_name, stat_date, row_no, campaign_name, ad_group_name,
                              publisher_name, targeting_text, advertised_sku, impressions, clicks,
                              ctr, cpc, spend, ad_sales_7d, acos, roas, ad_orders_7d, cvr_7d,
                              source_file_id, batch_id
                            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                            ON DUPLICATE KEY UPDATE
                              product_id=VALUES(product_id),
                              report_date=VALUES(report_date),
                              account_name=VALUES(account_name),
                              currency_code=VALUES(currency_code),
                              ad_group_name=VALUES(ad_group_name),
                              publisher_name=VALUES(publisher_name),
                              targeting_text=VALUES(targeting_text),
                              advertised_sku=VALUES(advertised_sku),
                              impressions=VALUES(impressions),
                              clicks=VALUES(clicks),
                              ctr=VALUES(ctr),
                              cpc=VALUES(cpc),
                              spend=VALUES(spend),
                              ad_sales_7d=VALUES(ad_sales_7d),
                              acos=VALUES(acos),
                              roas=VALUES(roas),
                              ad_orders_7d=VALUES(ad_orders_7d),
                              cvr_7d=VALUES(cvr_7d),
                              source_file_id=VALUES(source_file_id),
                              batch_id=VALUES(batch_id)
                            """,
                            (
                                product_id,
                                ad_asin,
                                report_date,
                                normalize_text(row[SP_COLUMNS['account_name']]),
                                normalize_text(row[SP_COLUMNS['currency_code']]),
                                marketplace,
                                shop_name,
                                stat_date,
                                row_no,
                                campaign_name,
                                normalize_text(row[SP_COLUMNS['ad_group_name']]),
                                normalize_text(row[SP_COLUMNS['publisher_name']]),
                                normalize_text(row.get('投放') or row.get('匹配方式')),
                                normalize_text(row[SP_COLUMNS['advertised_sku']]),
                                parse_int(row[SP_COLUMNS['impressions']]),
                                parse_int(row[SP_COLUMNS['clicks']]),
                                parse_percent_to_decimal(row[SP_COLUMNS['ctr']]),
                                parse_decimal(row[SP_COLUMNS['cpc']]),
                                parse_decimal(row[SP_COLUMNS['spend']]),
                                parse_decimal(row[SP_COLUMNS['ad_sales_7d']]),
                                parse_percent_to_decimal(row[SP_COLUMNS['acos']]),
                                parse_decimal(row[SP_COLUMNS['roas']]),
                                parse_int(row[SP_COLUMNS['ad_orders_7d']]),
                                parse_percent_to_decimal(row[SP_COLUMNS['cvr_7d']]),
                                file_id,
                                batch_id,
                            ),
                        )
                        file_rows += 1

                    cur.execute('UPDATE m3_import_file SET parse_status=1, error_message=NULL WHERE id=%s', (file_id,))
                conn.commit()
                total_rows += file_rows
                print(f'[OK] sp imported: rows={file_rows}, file={path.name}')
            except Exception as exc:
                conn.rollback()
                finish_import_file(conn, file_id, False, str(exc))
                raise

        with conn.cursor() as cur:
            cur.execute(
                'UPDATE m3_import_batch SET import_status=1, finished_at=NOW(), source_note=%s WHERE id=%s',
                (f'SP广告报告导入完成，文件数={len(files)}，记录数={total_rows}', batch_id),
            )
        conn.commit()
    except Exception:
        finish_batch(conn, batch_id, False)
        raise


def import_sbv_files(conn, path_value: str, shop_name: str, marketplace: str, imported_by: str):
    path = Path(path_value)
    if not path.exists():
        raise FileNotFoundError(f'路径不存在: {path}')

    # 当前样本里的 SBV 目录是跨店铺共享目录，不应按不同 shop_name 重复导入同一份文件。
    if shop_name and shop_name.upper() != 'EXPERLAM':
        print(f'[SKIP] sbv import skipped for shop={shop_name}; shared SBV dataset already imported once')
        return
    if path.is_file():
        files = [path]
    else:
        files = sorted([
            p for p in path.rglob('*')
            if p.is_file() and p.suffix.lower() in {'.xlsx', '.xls'} and not p.name.startswith('~$')
        ])
    if not files:
        raise ValueError(f'未找到 SBV 广告报告文件: {path_value}')

    batch_no = f'sbv-{dt.datetime.now().strftime("%Y%m%d%H%M%S")}'
    batch_id = ensure_batch(
        conn=conn,
        batch_no=batch_no,
        batch_type='sbv',
        shop_name=shop_name,
        marketplace=marketplace,
        stat_date=None,
        source_dir=str(Path(path_value)),
        source_note='SBV广告报告导入',
        imported_by=imported_by,
    )

    total_rows = 0

    try:
        for path in files:
            stat_date = parse_stat_date_from_path(path)
            file_id = create_import_file(conn, batch_id, 'sbv', path, shop_name, marketplace, stat_date)
            try:
                df = pd.read_excel(path)
                try:
                    columns = resolve_columns(
                        df,
                        SBV_COLUMN_ALIASES,
                        required_keys=['campaign_name', 'promoted_asin', 'impressions', 'clicks', 'ctr', 'cpc', 'spend', 'ad_sales', 'acos', 'ad_orders', 'cvr'],
                    )
                except ValueError:
                    finish_import_file(conn, file_id, True, 'skipped: unsupported SBV workbook structure')
                    print(f'[SKIP] sbv unsupported workbook structure: {path.name}')
                    continue

                file_rows = 0
                first_effective_row = True
                with conn.cursor() as cur:
                    for row_no, (_, row) in enumerate(df.iterrows(), start=2):
                        campaign_name = normalize_text(row[columns['campaign_name']])
                        promoted_asin = normalize_asin(row[columns['promoted_asin']])
                        campaign_status = normalize_text(row[columns['campaign_status']]) if 'campaign_status' in columns else None
                        ad_group_name = normalize_text(row[columns['ad_group_name']]) if 'ad_group_name' in columns else None

                        if not any([campaign_name, promoted_asin, campaign_status, ad_group_name]):
                            continue

                        product_id = try_find_product_id_by_asin(conn, promoted_asin, marketplace, shop_name)
                        acos_raw = normalize_text(row[columns['acos']])
                        cvr_raw = normalize_text(row[columns['cvr']])
                        acos_value = parse_percent_to_decimal(acos_raw)
                        cvr_value = parse_percent_to_decimal(cvr_raw)

                        is_summary_row = 1 if (first_effective_row or contains_summary_hint(campaign_name, ad_group_name, promoted_asin)) else 0
                        first_effective_row = False

                        cur.execute(
                            """
                            INSERT INTO m3_sbv_campaign_daily(
                              product_id, asin, marketplace, shop_name, stat_date, row_no,
                              is_summary_row, campaign_name, campaign_status, ad_group_name,
                              promoted_asin, site_name, impressions, clicks, ctr, cpc,
                              spend, ad_sales, acos, acos_raw, ad_orders, cvr, cvr_raw,
                              source_file_id, batch_id
                            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                            ON DUPLICATE KEY UPDATE
                              product_id=VALUES(product_id),
                              is_summary_row=VALUES(is_summary_row),
                              campaign_status=VALUES(campaign_status),
                              ad_group_name=VALUES(ad_group_name),
                              promoted_asin=VALUES(promoted_asin),
                              site_name=VALUES(site_name),
                              impressions=VALUES(impressions),
                              clicks=VALUES(clicks),
                              ctr=VALUES(ctr),
                              cpc=VALUES(cpc),
                              spend=VALUES(spend),
                              ad_sales=VALUES(ad_sales),
                              acos=VALUES(acos),
                              acos_raw=VALUES(acos_raw),
                              ad_orders=VALUES(ad_orders),
                              cvr=VALUES(cvr),
                              cvr_raw=VALUES(cvr_raw),
                              source_file_id=VALUES(source_file_id),
                              batch_id=VALUES(batch_id)
                            """,
                            (
                                product_id,
                                promoted_asin,
                                marketplace,
                                shop_name,
                                stat_date,
                                row_no,
                                is_summary_row,
                                campaign_name,
                                campaign_status,
                                ad_group_name,
                                promoted_asin,
                                normalize_text(row[columns['site_name']]) if 'site_name' in columns else None,
                                parse_int(row[columns['impressions']]),
                                parse_int(row[columns['clicks']]),
                                parse_percent_to_decimal(row[columns['ctr']]),
                                parse_decimal(row[columns['cpc']]),
                                parse_decimal(row[columns['spend']]),
                                parse_decimal(row[columns['ad_sales']]),
                                acos_value,
                                acos_raw,
                                parse_int(row[columns['ad_orders']]),
                                cvr_value,
                                cvr_raw,
                                file_id,
                                batch_id,
                            ),
                        )
                        file_rows += 1

                    cur.execute('UPDATE m3_import_file SET parse_status=1, error_message=NULL WHERE id=%s', (file_id,))
                conn.commit()
                total_rows += file_rows
                print(f'[OK] sbv imported: rows={file_rows}, file={path.name}')
            except Exception as exc:
                conn.rollback()
                finish_import_file(conn, file_id, False, str(exc))
                raise

        with conn.cursor() as cur:
            cur.execute(
                'UPDATE m3_import_batch SET import_status=1, finished_at=NOW(), source_note=%s WHERE id=%s',
                (f'SBV广告报告导入完成，文件数={len(files)}，记录数={total_rows}', batch_id),
            )
        conn.commit()
    except Exception:
        finish_batch(conn, batch_id, False)
        raise


def _append_common_filters(filters: List[str], params: List, alias: str,
                           marketplace: Optional[str], start_date: Optional[dt.date], end_date: Optional[dt.date]):
    if marketplace:
        filters.append(f'{alias}.marketplace = %s')
        params.append(marketplace)
    if start_date:
        filters.append(f'{alias}.stat_date >= %s')
        params.append(start_date)
    if end_date:
        filters.append(f'{alias}.stat_date <= %s')
        params.append(end_date)


def fetch_review_metrics(conn, marketplace: Optional[str], start_date: Optional[dt.date], end_date: Optional[dt.date]):
    filters = ['1=1']
    params: List = []
    if marketplace:
        filters.append('p.marketplace = %s')
        params.append(marketplace)
    if start_date:
        filters.append('r.stat_date >= %s')
        params.append(start_date)
    if end_date:
        filters.append('r.stat_date <= %s')
        params.append(end_date)

    sql = f"""
        SELECT r.product_id,
               r.stat_date,
               MAX(r.rating_avg) AS rating_avg,
               MAX(r.review_count_total) AS review_count_total,
               MAX(r.review_count_new) AS review_count_new
          FROM m3_review_daily r
          JOIN m3_product p ON p.id = r.product_id
         WHERE {' AND '.join(filters)}
         GROUP BY r.product_id, r.stat_date
    """
    with conn.cursor() as cur:
        cur.execute(sql, tuple(params))
        rows = cur.fetchall()
    return {
        (row[0], row[1]): {
            'rating_avg': row[2],
            'review_count_total': row[3] or 0,
            'review_count_new': row[4] or 0,
        }
        for row in rows
    }


def fetch_biz_child_metrics(conn, marketplace: Optional[str], start_date: Optional[dt.date], end_date: Optional[dt.date]):
    filters = ['1=1']
    params: List = []
    _append_common_filters(filters, params, 'c', marketplace, start_date, end_date)

    sql = f"""
        SELECT p.id AS product_id,
               c.stat_date,
               SUM(c.ordered_product_sales) AS sales_amount,
               SUM(c.units_ordered) AS units_ordered,
               SUM(c.total_order_items) AS order_item_count,
               SUM(c.sessions_total) AS sessions_total,
               SUM(c.page_views_total) AS page_views_total,
               CASE WHEN SUM(c.sessions_total) = 0 THEN NULL
                    ELSE SUM(COALESCE(c.conversion_rate_total, 0) * c.sessions_total) / SUM(c.sessions_total)
               END AS conversion_rate_total,
               CASE WHEN SUM(c.sessions_total) = 0 THEN NULL
                    ELSE SUM(COALESCE(c.buy_box_pct, 0) * c.sessions_total) / SUM(c.sessions_total)
               END AS buy_box_pct,
               CASE WHEN SUM(c.sessions_total) = 0 THEN NULL
                    ELSE SUM(COALESCE(c.unit_session_pct, 0) * c.sessions_total) / SUM(c.sessions_total)
               END AS unit_session_pct
          FROM m3_biz_report_child_daily c
          JOIN m3_product p
            ON p.primary_asin = c.child_asin
           AND p.marketplace = c.marketplace
         WHERE {' AND '.join(filters)}
         GROUP BY p.id, c.stat_date
    """
    with conn.cursor() as cur:
        cur.execute(sql, tuple(params))
        rows = cur.fetchall()
    return {
        (row[0], row[1]): {
            'sales_amount': float(row[2] or 0),
            'units_ordered': int(row[3] or 0),
            'order_item_count': int(row[4] or 0),
            'sessions_total': int(row[5] or 0),
            'page_views_total': int(row[6] or 0),
            'conversion_rate_total': float(row[7]) if row[7] is not None else None,
            'buy_box_pct': float(row[8]) if row[8] is not None else None,
            'unit_session_pct': float(row[9]) if row[9] is not None else None,
        }
        for row in rows
    }


def fetch_sp_metrics(conn, marketplace: Optional[str], start_date: Optional[dt.date], end_date: Optional[dt.date]):
    filters = ['s.product_id IS NOT NULL']
    params: List = []
    _append_common_filters(filters, params, 's', marketplace, start_date, end_date)

    sql = f"""
        SELECT s.product_id,
               s.stat_date,
               SUM(s.impressions) AS impressions,
               SUM(s.clicks) AS clicks,
               SUM(s.spend) AS spend,
               SUM(s.ad_sales_7d) AS sales,
               SUM(s.ad_orders_7d) AS orders
          FROM m3_sp_campaign_daily s
         WHERE {' AND '.join(filters)}
         GROUP BY s.product_id, s.stat_date
    """
    with conn.cursor() as cur:
        cur.execute(sql, tuple(params))
        rows = cur.fetchall()
    return {
        (row[0], row[1]): {
            'sp_impressions': int(row[2] or 0),
            'sp_clicks': int(row[3] or 0),
            'sp_spend': float(row[4] or 0),
            'sp_sales': float(row[5] or 0),
            'sp_orders': int(row[6] or 0),
        }
        for row in rows
    }


def fetch_sbv_metrics(conn, marketplace: Optional[str], start_date: Optional[dt.date], end_date: Optional[dt.date]):
    filters = ['s.product_id IS NOT NULL']
    params: List = []
    _append_common_filters(filters, params, 's', marketplace, start_date, end_date)

    sql = f"""
        SELECT s.product_id,
               s.stat_date,
               SUM(CASE WHEN s.is_summary_row = 0 THEN 1 ELSE 0 END) AS detail_count,
               SUM(CASE WHEN s.is_summary_row = 0 THEN s.impressions ELSE 0 END) AS detail_impressions,
               SUM(CASE WHEN s.is_summary_row = 0 THEN s.clicks ELSE 0 END) AS detail_clicks,
               SUM(CASE WHEN s.is_summary_row = 0 THEN s.spend ELSE 0 END) AS detail_spend,
               SUM(CASE WHEN s.is_summary_row = 0 THEN s.ad_sales ELSE 0 END) AS detail_sales,
               SUM(CASE WHEN s.is_summary_row = 0 THEN s.ad_orders ELSE 0 END) AS detail_orders,
               SUM(CASE WHEN s.is_summary_row = 1 THEN s.impressions ELSE 0 END) AS summary_impressions,
               SUM(CASE WHEN s.is_summary_row = 1 THEN s.clicks ELSE 0 END) AS summary_clicks,
               SUM(CASE WHEN s.is_summary_row = 1 THEN s.spend ELSE 0 END) AS summary_spend,
               SUM(CASE WHEN s.is_summary_row = 1 THEN s.ad_sales ELSE 0 END) AS summary_sales,
               SUM(CASE WHEN s.is_summary_row = 1 THEN s.ad_orders ELSE 0 END) AS summary_orders
          FROM m3_sbv_campaign_daily s
         WHERE {' AND '.join(filters)}
         GROUP BY s.product_id, s.stat_date
    """
    with conn.cursor() as cur:
        cur.execute(sql, tuple(params))
        rows = cur.fetchall()

    metrics = {}
    for row in rows:
        product_id, stat_date = row[0], row[1]
        detail_count = int(row[2] or 0)
        use_detail = detail_count > 0
        metrics[(product_id, stat_date)] = {
            'sbv_impressions': int((row[3] if use_detail else row[8]) or 0),
            'sbv_clicks': int((row[4] if use_detail else row[9]) or 0),
            'sbv_spend': float((row[5] if use_detail else row[10]) or 0),
            'sbv_sales': float((row[6] if use_detail else row[11]) or 0),
            'sbv_orders': int((row[7] if use_detail else row[12]) or 0),
        }
    return metrics


def build_product_daily_summary(conn, marketplace: Optional[str], start_date: Optional[dt.date], end_date: Optional[dt.date]):
    biz_metrics = fetch_biz_child_metrics(conn, marketplace, start_date, end_date)
    review_metrics = fetch_review_metrics(conn, marketplace, start_date, end_date)
    sp_metrics = fetch_sp_metrics(conn, marketplace, start_date, end_date)
    sbv_metrics = fetch_sbv_metrics(conn, marketplace, start_date, end_date)

    all_keys = set()
    all_keys.update(biz_metrics.keys())
    all_keys.update(review_metrics.keys())
    all_keys.update(sp_metrics.keys())
    all_keys.update(sbv_metrics.keys())

    if not all_keys:
        print('[OK] summary built: no matched product/date records')
        return 0

    upserted = 0
    with conn.cursor() as cur:
        for product_id, stat_date in sorted(all_keys, key=lambda x: (x[1], x[0])):
            biz = biz_metrics.get((product_id, stat_date), {})
            review = review_metrics.get((product_id, stat_date), {})
            sp = sp_metrics.get((product_id, stat_date), {})
            sbv = sbv_metrics.get((product_id, stat_date), {})

            sales_amount = float(biz.get('sales_amount', 0))
            units_ordered = int(biz.get('units_ordered', 0))
            order_item_count = int(biz.get('order_item_count', 0))
            sessions_total = int(biz.get('sessions_total', 0))
            page_views_total = int(biz.get('page_views_total', 0))
            conversion_rate_total = biz.get('conversion_rate_total')
            buy_box_pct = biz.get('buy_box_pct')
            unit_session_pct = biz.get('unit_session_pct')
            listing_conversion_rate = (
                safe_ratio(order_item_count, sessions_total)
                if sessions_total > 0
                else unit_session_pct
                if unit_session_pct is not None
                else conversion_rate_total if conversion_rate_total is not None
                else safe_ratio(units_ordered, sessions_total)
            )

            sp_impressions = int(sp.get('sp_impressions', 0))
            sp_clicks = int(sp.get('sp_clicks', 0))
            sp_spend = float(sp.get('sp_spend', 0))
            sp_sales = float(sp.get('sp_sales', 0))
            sp_orders = int(sp.get('sp_orders', 0))

            sbv_impressions = int(sbv.get('sbv_impressions', 0))
            sbv_clicks = int(sbv.get('sbv_clicks', 0))
            sbv_spend = float(sbv.get('sbv_spend', 0))
            sbv_sales = float(sbv.get('sbv_sales', 0))
            sbv_orders = int(sbv.get('sbv_orders', 0))

            # 子卡片层 ad_* 当前优先按 SP 口径汇总；SBV 拆分指标继续保留在 sbv_* 字段，供父组/对账使用
            # 子卡片 listing_conversion_rate 当前优先按 订单商品总数 / 会话数-总计 收敛，
            # unit_session_pct 仅作为无会话数时的回退口径。
            ad_impressions = sp_impressions
            ad_clicks = sp_clicks
            ad_spend = sp_spend
            ad_sales = sp_sales
            ad_orders = sp_orders
            ad_ctr = safe_ratio(ad_clicks, ad_impressions)
            ad_cpc = safe_ratio(ad_spend, ad_clicks)
            ad_acos = safe_ratio(ad_spend, ad_sales)
            ad_cvr = safe_ratio(ad_orders, ad_clicks)

            natural_order_count = max(order_item_count - ad_orders, 0)
            natural_session_count = 0
            natural_conversion_rate = None

            source_note = 'generated by ops/import_module3_stage1.py; listing_conversion_rate prefers order_item_count/sessions_total and falls back to unit_session_pct; card-level ad_* prefers SP; sbv_* retained for parent/group reconciliation; natural_session_count pending exact business rule'

            cur.execute(
                """
                INSERT INTO m3_product_daily_summary(
                  product_id, stat_date,
                  rating_avg, review_count_total, review_count_new,
                  sales_amount, units_ordered, order_item_count,
                  sessions_total, page_views_total, conversion_rate_total, buy_box_pct, unit_session_pct,
                  listing_conversion_rate,
                  sp_impressions, sp_clicks, sp_spend, sp_sales, sp_orders,
                  sbv_impressions, sbv_clicks, sbv_spend, sbv_sales, sbv_orders,
                  ad_impressions, ad_clicks, ad_ctr, ad_cpc, ad_spend, ad_sales, ad_acos, ad_orders, ad_cvr,
                  natural_order_count, natural_session_count, natural_conversion_rate,
                  summary_version, source_note
                ) VALUES (
                  %s, %s,
                  %s, %s, %s,
                  %s, %s, %s,
                  %s, %s, %s, %s, %s,
                  %s,
                  %s, %s, %s, %s, %s,
                  %s, %s, %s, %s, %s,
                  %s, %s, %s, %s, %s, %s, %s, %s, %s,
                  %s, %s, %s,
                  4, %s
                )
                ON DUPLICATE KEY UPDATE
                  rating_avg=VALUES(rating_avg),
                  review_count_total=VALUES(review_count_total),
                  review_count_new=VALUES(review_count_new),
                  sales_amount=VALUES(sales_amount),
                  units_ordered=VALUES(units_ordered),
                  order_item_count=VALUES(order_item_count),
                  sessions_total=VALUES(sessions_total),
                  page_views_total=VALUES(page_views_total),
                  conversion_rate_total=VALUES(conversion_rate_total),
                  buy_box_pct=VALUES(buy_box_pct),
                  unit_session_pct=VALUES(unit_session_pct),
                  listing_conversion_rate=VALUES(listing_conversion_rate),
                  sp_impressions=VALUES(sp_impressions),
                  sp_clicks=VALUES(sp_clicks),
                  sp_spend=VALUES(sp_spend),
                  sp_sales=VALUES(sp_sales),
                  sp_orders=VALUES(sp_orders),
                  sbv_impressions=VALUES(sbv_impressions),
                  sbv_clicks=VALUES(sbv_clicks),
                  sbv_spend=VALUES(sbv_spend),
                  sbv_sales=VALUES(sbv_sales),
                  sbv_orders=VALUES(sbv_orders),
                  ad_impressions=VALUES(ad_impressions),
                  ad_clicks=VALUES(ad_clicks),
                  ad_ctr=VALUES(ad_ctr),
                  ad_cpc=VALUES(ad_cpc),
                  ad_spend=VALUES(ad_spend),
                  ad_sales=VALUES(ad_sales),
                  ad_acos=VALUES(ad_acos),
                  ad_orders=VALUES(ad_orders),
                  ad_cvr=VALUES(ad_cvr),
                  natural_order_count=VALUES(natural_order_count),
                  natural_session_count=VALUES(natural_session_count),
                  natural_conversion_rate=VALUES(natural_conversion_rate),
                  summary_version=VALUES(summary_version),
                  source_note=VALUES(source_note)
                """,
                (
                    product_id,
                    stat_date,
                    review.get('rating_avg'),
                    int(review.get('review_count_total', 0)),
                    int(review.get('review_count_new', 0)),
                    sales_amount,
                    units_ordered,
                    order_item_count,
                    sessions_total,
                    page_views_total,
                    conversion_rate_total,
                    buy_box_pct,
                    unit_session_pct,
                    listing_conversion_rate,
                    sp_impressions,
                    sp_clicks,
                    sp_spend,
                    sp_sales,
                    sp_orders,
                    sbv_impressions,
                    sbv_clicks,
                    sbv_spend,
                    sbv_sales,
                    sbv_orders,
                    ad_impressions,
                    ad_clicks,
                    ad_ctr,
                    ad_cpc,
                    ad_spend,
                    ad_sales,
                    ad_acos,
                    ad_orders,
                    ad_cvr,
                    natural_order_count,
                    natural_session_count,
                    natural_conversion_rate,
                    source_note,
                ),
            )
            upserted += 1
    conn.commit()
    print(f'[OK] summary built: rows={upserted}')
    return upserted


def main():
    args = parse_args()
    summary_start_date = parse_date_arg(args.summary_start_date)
    summary_end_date = parse_date_arg(args.summary_end_date)

    if summary_start_date and summary_end_date and summary_start_date > summary_end_date:
        print('--summary-start-date 不能晚于 --summary-end-date', file=sys.stderr)
        sys.exit(1)

    has_import_task = any([
        args.mapping_file,
        args.biz_child_path,
        args.biz_parent_path,
        args.sp_path,
        args.sbv_path,
    ])

    if not has_import_task and not args.build_summary:
        print('至少提供导入参数之一，或显式传入 --build-summary', file=sys.stderr)
        sys.exit(1)

    if any([args.biz_child_path, args.biz_parent_path, args.sp_path, args.sbv_path]) and not args.shop_name:
        print('导入业务报告 / SP / SBV 报告时必须提供 --shop-name', file=sys.stderr)
        sys.exit(1)

    conn = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        charset='utf8mb4',
        autocommit=False,
    )

    try:
        if args.mapping_file:
            import_mapping_file(conn, Path(args.mapping_file), args.marketplace, args.imported_by)
        if args.biz_child_path:
            import_biz_child_files(conn, args.biz_child_path, args.shop_name, args.marketplace, args.imported_by)
        if args.biz_parent_path:
            import_biz_parent_files(conn, args.biz_parent_path, args.shop_name, args.marketplace, args.imported_by)
        if args.sp_path:
            import_sp_files(conn, args.sp_path, args.shop_name, args.marketplace, args.imported_by)
        if args.sbv_path:
            import_sbv_files(conn, args.sbv_path, args.shop_name, args.marketplace, args.imported_by)
        if args.build_summary:
            build_product_daily_summary(conn, args.marketplace, summary_start_date, summary_end_date)
        print('模块3 ETL Stage1 执行完成')
    finally:
        conn.close()


if __name__ == '__main__':
    main()
