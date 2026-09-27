#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-4 普查：真端世界文件（Map/Worlds/*/world*.xml）的 questscript_area 表。

背景：生产 `definitions/compact/ai/ai-areas.xml` 的 `<quest_area ... quests="...">` 是
`RetailAreaEngine`（进区域 → QuestService.startQuest）的输入，SimpleHunt 的 `_area_` 行能否迁移取决于
它在真端世界文件里是否绑定到具体 quest id。本脚本以真端为金标准重算该表。

输出：p0c4-world-questscript-area.tsv（world_dir, area_name, quests_raw, quest_ids, points, bottom, top）
"""
import re
import sys
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
WORLDS = Path('/Users/mc/IdeaProjects/58Server/Map/Worlds')
OUT = Path(__file__).resolve().parent / 'p0c4-world-questscript-area.tsv'
TARGET_FILES = ('world.xml', 'world_M.xml', 'world_N.xml')
AREA_RE = re.compile(r'<questscript_area\b[^>]*>(.*?)</questscript_area>', re.S)


def read(path):
    raw = Path(path).read_bytes()
    for enc in ('utf-16', 'utf-8'):
        try:
            return raw.decode(enc)
        except UnicodeError:
            continue
    return raw.decode('utf-16', errors='replace')


def text_of(block, tag):
    m = re.search(r'<%s>(.*?)</%s>' % (tag, tag), block, re.S)
    if not m:
        return None
    value = m.group(1).strip()
    return value or None


def main():
    rows = []
    scanned = 0
    for world_dir in sorted(p for p in WORLDS.iterdir() if p.is_dir()):
        for filename in TARGET_FILES:
            path = world_dir / filename
            if not path.is_file():
                continue
            scanned += 1
            try:
                s = read(path)
            except OSError as exc:  # noqa: PERF203 - 单文件失败不阻断全量普查
                print('WARN unreadable', path, exc, file=sys.stderr)
                continue
            for m in AREA_RE.finditer(s):
                block = m.group(1)
                quests_raw = text_of(block, 'quest') or ''
                ids = sorted({int(x) for x in re.findall(r'\d+', quests_raw)})
                points = len(re.findall(r'<data>', block))
                rows.append({
                    'world_dir': world_dir.name,
                    'file': filename,
                    'area_name': text_of(block, 'name') or '',
                    'quests_raw': quests_raw,
                    'quest_ids': ','.join(str(x) for x in ids),
                    'points': points,
                    'bottom': text_of(block, 'bottom') or '',
                    'top': text_of(block, 'top') or '',
                })
    cols = ['world_dir', 'file', 'area_name', 'quests_raw', 'quest_ids', 'points', 'bottom', 'top']
    with OUT.open('w', encoding='utf-8') as fh:
        fh.write('# P0c-4 真端世界文件 questscript_area 普查（生成脚本 p0c4_world_questscript_area_scan.py）\n')
        fh.write('\t'.join(cols) + '\n')
        for row in rows:
            fh.write('\t'.join(str(row[c]) for c in cols) + '\n')

    bound = [r for r in rows if r['quest_ids']]
    quests = sorted({int(x) for r in bound for x in r['quest_ids'].split(',')})
    print('扫描 world 文件:', scanned)
    print('questscript_area 总数:', len(rows))
    print('绑定 quest 的条目:', len(bound))
    print('涉及 quest id 数量:', len(quests))
    print('输出:', OUT)
    return 0


if __name__ == '__main__':
    sys.exit(main())
