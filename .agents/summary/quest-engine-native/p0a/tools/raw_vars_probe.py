#!/usr/bin/env python3
"""P0a: raw vars 存档分布审计（只读 SELECT）。

分类口径（计划 §4.4.5）：
  canonical        vars == 0，或该任务相机矩阵可达值（各槽 0..required 的组合）
  high_bit         vars >= 0x40000000（真端守卫区：推进通道标志位）
  packed_unexpected 无高位但不落在该任务可达值集合
  invalid          超出 32 位
仅审计 retail-owned（retention owner=RETAIL_TABLE*）的任务行；owner 与相机矩阵来自 P0a 已产物。
"""
import collections
import csv
import itertools
import pathlib
import sys
import xml.etree.ElementTree as ET

import pymysql  # noqa: N817

HERE = pathlib.Path(__file__).resolve()
REPO = next(candidate for candidate in [HERE, *HERE.parents] if (candidate / 'pom.xml').is_file())
P0A = REPO / '.agents/summary/quest-engine-native/p0a'
DB = dict(host='127.0.0.1', port=3306, user='root', password='123456',
          database='al_server_gs', charset='utf8mb4',
          cursorclass=pymysql.cursors.Cursor, read_timeout=20)


def load_retention_retail_ids():
    ids = set()
    p = REPO / 'src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.xml'
    for row in ET.parse(p).getroot().findall('quest'):
        if (row.findtext('owner') or '').startswith('RETAIL_TABLE'):
            ids.add(int(row.findtext('quest_id')))
    return ids


def load_camera_layout():
    """questId -> list of (shift, required) ；来自 P0a camera-params.tsv。"""
    layout = collections.defaultdict(list)
    with open(P0A / 'camera-params.tsv') as f:
        for row in csv.DictReader(f, delimiter='\t'):
            layout[int(row['questId'])].append((row['width'], int(row['slot']), int(row['required'])))
    return layout


def reachable_values(spec):
    """枚举该任务全部可达 vars 值（required<=1000 时组合数可控，粗防爆炸）。"""
    total = 1
    for _, _, req in spec:
        total *= (req + 1)
        if total > 200000:
            return None
    vals = {0}
    per_slot = []
    for w, slot, req in spec:
        shift = (slot - 1) * (6 if w == '6' else 10)
        per_slot.append(list(range(0, req + 1)) if req <= 1023 else [0])
    for combo in itertools.product(*per_slot):
        v = 0
        for (w, slot, req), k in zip(spec, combo):
            v += k << ((slot - 1) * (6 if w == '6' else 10))
        vals.add(v)
    return vals


def main():
    retail_ids = load_retention_retail_ids()
    layout = load_camera_layout()
    conn = pymysql.connect(**DB)
    try:
        with conn.cursor() as cur:
            cur.execute("SHOW TABLES LIKE '%quest%'")
            tables = [r[0] for r in cur.fetchall()]
            print('quest tables:', tables)
            # find the player quest vars table
            var_table = None
            for t in tables:
                cur.execute(f'SHOW COLUMNS FROM `{t}`')
                cols = {r[0] for r in cur.fetchall()}
                if {'quest_id', 'quest_vars'} <= cols or {'questId', 'questVars'} <= cols:
                    var_table = (t, cols)
                    break
            if not var_table:
                print('NO_VARS_TABLE_FOUND; tables with quest cols:')
                for t in tables:
                    cur.execute(f'SHOW COLUMNS FROM `{t}`')
                    print(' ', t, [r[0] for r in cur.fetchall()])
                return
            t, cols = var_table
            qcol = 'quest_id' if 'quest_id' in cols else 'questId'
            vcol = 'quest_vars' if 'quest_vars' in cols else 'questVars'
            cur.execute(f'SELECT `{qcol}`, `{vcol}`, COUNT(*) FROM `{t}` GROUP BY `{qcol}`, `{vcol}`')
            rows = cur.fetchall()
    finally:
        conn.close()

    stats = collections.Counter()
    by_quest_unexpected = collections.Counter()
    samples = collections.defaultdict(list)
    n_rows = 0
    for qid, var, cnt in rows:
        if qid is None or not retail_ids or qid not in retail_ids:
            continue
        n_rows += cnt
        if var is None or var < 0:
            stats['null_or_signed'] += cnt
            continue
        if var > 0xFFFFFFFF:
            stats['invalid_over_32bit'] += cnt
            continue
        if var >= 0x40000000:
            stats['high_bit'] += cnt
            continue
        spec = layout.get(qid)
        vals = reachable_values(spec) if spec else {0}
        if spec is None or vals is None:
            ok = var == 0
        else:
            ok = var in vals
        if ok:
            stats['canonical'] += cnt
        else:
            stats['packed_unexpected'] += cnt
            by_quest_unexpected[qid] += cnt
            if len(samples[qid]) < 5:
                samples[qid].append(var)
    print(f'retail-owned distinct (quest,vars): {len(rows)}; rows: {n_rows}')
    print('distribution:', dict(stats))
    print('top unexpected quests:', by_quest_unexpected.most_common(15))
    print('samples:', dict(itertools.islice(samples.items(), 10)))


if __name__ == '__main__':
    main()
