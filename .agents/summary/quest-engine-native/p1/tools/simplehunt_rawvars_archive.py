#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P1 切换批前置：SimpleHunt 专属 raw vars 存档分类（只读 SELECT）。

计划不变式 7：native 写入前必须有存档位形结论。本脚本把 P0a 的全局分类收到 SimpleHunt
族：相机规约从入仓 Quest_SimpleHunt.xml 表行推导（宽度规则 = 任一 count>63 ⇒ 10 位，
与 p1/tools/reconcile_hunt_table_vs_camera.py 同口径），范围 = 全表 1863 行（含休眠行——
休眠行相机缺位，任何非零 vars 都记 unexpected 并单独标注）。

分类口径（沿 P0a §4.4.5）：
  canonical          vars 落在该任务相机可达值集合（各槽 0..required 的组合）
  high_bit           vars >= 0x40000000（真端守卫区）
  packed_unexpected  无高位但不落在可达集合（含休眠行非零）
  invalid            超出 32 位 / 负值
  no_camera_nonzero  休眠行（无相机）但 vars 非零（unexpected 的子集，单列）
"""

import collections
import itertools
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

import pymysql  # noqa: N817

REPO = pathlib.Path(__file__).resolve().parents[5]
TABLE = REPO / 'src/main/resources/aion/data/static_data/quest/retail/Quest_SimpleHunt.xml'
OUT = pathlib.Path(__file__).resolve().parent
DB = dict(host='127.0.0.1', port=3306, user='root', password='123456',
          database='al_server_gs', charset='utf8mb4',
          cursorclass=pymysql.cursors.Cursor, read_timeout=20)

MAX_SIX_BIT = 63


def parse_table():
    raw = TABLE.read_text(encoding='utf-8')
    root = ET.fromstring(re.sub(r'<!DOCTYPE.*?\]>', '', raw, flags=re.S))
    specs = {}
    for el in root:
        if el.tag != 'id':
            continue
        counts = {}
        for c in el:
            m = re.fullmatch(r'count([1-5])', c.tag)
            if m:
                counts[int(m.group(1))] = int(c.text.strip())
        if not counts:
            specs[int(el.get('id'))] = None  # 休眠零计数行
            continue
        width = 10 if any(v > MAX_SIX_BIT for v in counts.values()) else 6
        specs[int(el.get('id'))] = (width, counts)
    return specs


def reachable_values(width, counts):
    bits = 10 if width == 10 else 6
    total = 1
    for req in counts.values():
        total *= (req + 1)
        if total > 200000:
            return None
    vals = {0}
    per_slot = [list(range(0, req + 1)) for req in counts.values()]
    shifts = [(slot - 1) * bits for slot in counts]
    for combo in itertools.product(*per_slot):
        vals.add(sum(k << s for k, s in zip(combo, shifts)))
    return vals


def main():
    specs = parse_table()
    conn = pymysql.connect(**DB)
    try:
        with conn.cursor() as cur:
            cur.execute("SHOW TABLES LIKE '%quest%'")
            tables = [r[0] for r in cur.fetchall()]
            var_table = None
            for t in tables:
                cur.execute(f'SHOW COLUMNS FROM `{t}`')
                cols = {r[0] for r in cur.fetchall()}
                if {'quest_id', 'quest_vars'} <= cols:
                    var_table = (t, cols)
                    break
            if not var_table:
                print('NO_VARS_TABLE_FOUND')
                return 1
            t, cols = var_table
            qcol = 'quest_id' if 'quest_id' in cols else 'questId'
            vcol = 'quest_vars' if 'quest_vars' in cols else 'questVars'
            cur.execute(f'SELECT `{qcol}`, `{vcol}`, COUNT(*) FROM `{t}` '
                        f'WHERE `{qcol}` IN ({",".join(["%s"] * len(specs))}) '
                        f'GROUP BY `{qcol}`, `{vcol}`',
                        tuple(sorted(specs)))
            rows = cur.fetchall()
    finally:
        conn.close()

    stats = collections.Counter()
    unexpected = []          # (qid, vars, players)
    dormant_nonzero = []
    n_rows = 0
    for qid, var, cnt in rows:
        n_rows += cnt
        if var is None or var < 0:
            stats['null_or_signed'] += cnt
            unexpected.append((qid, var, cnt))
            continue
        if var > 0xFFFFFFFF:
            stats['invalid_over_32bit'] += cnt
            unexpected.append((qid, var, cnt))
            continue
        spec = specs.get(qid)
        if spec is None:
            if var != 0:
                stats['no_camera_nonzero'] += cnt
                dormant_nonzero.append((qid, var, cnt))
            else:
                stats['canonical_dormant_zero'] += cnt
            continue
        width, counts = spec
        if var >= 0x40000000:
            stats['high_bit'] += cnt
            unexpected.append((qid, var, cnt))
            continue
        vals = reachable_values(width, counts)
        if vals is not None and var in vals:
            stats['canonical'] += cnt
        else:
            stats['packed_unexpected'] += cnt
            unexpected.append((qid, var, cnt))

    print(f'simplehunt distinct (quest,vars) groups: {len(rows)}; player rows: {n_rows}')
    print('distribution:', dict(stats))
    lines = ['questId\tvars\tparent_rows\tbucket']
    for qid, var, cnt in unexpected:
        bucket = 'null_or_signed' if var is None or var < 0 else (
            'invalid_over_32bit' if var > 0xFFFFFFFF else 'high_bit' if var >= 0x40000000
            else 'packed_unexpected')
        lines.append(f'{qid}\t{var}\t{cnt}\t{bucket}')
    for qid, var, cnt in dormant_nonzero:
        lines.append(f'{qid}\t{var}\t{cnt}\tno_camera_nonzero')
    (OUT / 'simplehunt-rawvars-unexpected.tsv').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(f'unexpected groups: {len(unexpected) + len(dormant_nonzero)} '
          f'-> simplehunt-rawvars-unexpected.tsv')
    print('VERDICT:', 'CLEAN' if not unexpected and not dormant_nonzero else 'REVIEW_NEEDED')
    return 0


if __name__ == '__main__':
    sys.exit(main())
