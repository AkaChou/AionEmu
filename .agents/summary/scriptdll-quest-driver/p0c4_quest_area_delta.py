#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-4 对账：真端世界文件 questscript_area ↔ 生产 ai-areas.xml 的 <quest_area>。

真端 = Map/Worlds/<world>/world*.xml 的 <questscript_area>（金标准）；
生产 = src/main/resources/aion/definitions/compact/ai/ai-areas.xml 的 <quest_area>。

输出：
  p0c4-quest-area-delta.tsv         逐条对账（键 = world_name + area_name）
  p0c4-quest-area-missing-quests.tsv 生产缺失的 quest id（按 id 聚合，含真端世界/区域名）
"""
import os
import re
import sys
from collections import defaultdict
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
SCAN = Path(__file__).resolve().parent / 'p0c4-world-questscript-area.tsv'
PROD = REPO / 'src/main/resources/aion/definitions/compact/ai/ai-areas.xml'
DELTA_OUT = Path(__file__).resolve().parent / 'p0c4-quest-area-delta.tsv'
MISSING_OUT = Path(__file__).resolve().parent / 'p0c4-quest-area-missing-quests.tsv'


def read_retail():
    """真端条目：同名区域在 world.xml / world_M.xml / world_N.xml 重复，按 world+name 去重。"""
    rows = {}
    for line in SCAN.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if parts[0] == 'world_dir':
            continue
        world_dir, filename, name, quests_raw, quest_ids, points, bottom, top = parts
        key = (world_dir.lower(), name)
        ids = tuple(int(x) for x in quest_ids.split(',') if x)
        prev = rows.get(key)
        if prev is None:
            rows[key] = {'world': world_dir.lower(), 'name': name, 'quest_ids': ids,
                         'points': int(points), 'bottom': bottom, 'top': top, 'files': {filename}}
        else:
            prev['files'].add(filename)
            if prev['quest_ids'] != ids:
                print('WARN 同名区域 quest 列表不一致:', key, prev['quest_ids'], ids, file=sys.stderr)
    return rows


def read_production():
    s = PROD.read_text(encoding='utf-8')
    rows = {}
    for m in re.finditer(r'<quest_area\b([^>]*)>(.*?)</quest_area>', s, re.S):
        attrs, body = m.group(1), m.group(2)
        def attr(tag):
            a = re.search(r'(?<![A-Za-z0-9_])%s="([^"]*)"' % tag, attrs)
            return a.group(1) if a else ''
        name = attr('name')
        key = (attr('world_name').lower(), name)
        ids = tuple(int(x) for x in re.findall(r'\d+', attr('quests')))
        rows[key] = {'world_id': attr('world_id'), 'world': attr('world_name').lower(), 'name': name,
                     'quest_ids': ids, 'points': len(re.findall(r'<point\b', body)),
                     'bottom': attr('bottom'), 'top': attr('top')}
    return rows


def main():
    retail = read_retail()
    prod = read_production()

    delta = []
    for key in sorted(set(retail) | set(prod)):
        r, p = retail.get(key), prod.get(key)
        if r and p:
            status = 'MATCH' if set(r['quest_ids']) == set(p['quest_ids']) else 'QUESTS_DIFFER'
        elif r:
            status = 'MISSING_IN_PROD'
        else:
            status = 'EXTRA_IN_PROD'
        delta.append({
            'status': status,
            'world_name': key[0],
            'area_name': key[1],
            'retail_quests': ','.join(str(x) for x in (r or {}).get('quest_ids', ())),
            'prod_quests': ','.join(str(x) for x in (p or {}).get('quest_ids', ())),
            'retail_points': (r or {}).get('points', ''),
            'prod_points': (p or {}).get('points', ''),
            'prod_world_id': (p or {}).get('world_id', ''),
        })
    cols = ['status', 'world_name', 'area_name', 'retail_quests', 'prod_quests',
            'retail_points', 'prod_points', 'prod_world_id']
    with DELTA_OUT.open('w', encoding='utf-8') as fh:
        fh.write('# P0c-4 <quest_area> 对账（真端世界文件 ↔ 生产 ai-areas.xml）\n')
        fh.write('\t'.join(cols) + '\n')
        for row in delta:
            fh.write('\t'.join(str(row[c]) for c in cols) + '\n')

    prod_ids = {q for p in prod.values() for q in p['quest_ids']}
    retail_ids = {q for r in retail.values() for q in r['quest_ids']}
    missing = sorted(retail_ids - prod_ids)
    owners = defaultdict(list)
    for r in retail.values():
        for q in r['quest_ids']:
            owners[q].append('%s/%s' % (r['world'], r['name']))
    with MISSING_OUT.open('w', encoding='utf-8') as fh:
        fh.write('# P0c-4 生产 ai-areas.xml 缺失的真端 quest_area 绑定\n')
        fh.write('quest_id\tretail_areas\n')
        for q in missing:
            fh.write('%d\t%s\n' % (q, '; '.join(sorted(set(owners[q])))))

    from collections import Counter
    print('本条对账统计（键 = world_name + area_name）：')
    print('  真端条目:', len(retail), ' 生产条目:', len(prod))
    for status, count in sorted(Counter(r['status'] for r in delta).items()):
        print('  %-16s %d' % (status, count))
    print('  真端 quest id:', len(retail_ids), ' 生产 quest id:', len(prod_ids))
    print('  真端有 / 生产无的 quest id:', len(missing))
    print('  生产有 / 真端无的 quest id:', len(sorted(prod_ids - retail_ids)))
    print('输出:', DELTA_OUT, MISSING_OUT)
    return 0


if __name__ == '__main__':
    sys.exit(main())
