#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-4 生成器：从真端世界文件导出 ai-areas.xml 的 <quest_area> 片段。

真端 = Map/Worlds/<world>/world.xml 的 <questscript_area>；只导出生产缺失且绑定 quest 的条目。
world_id 取自生产同文件的既有映射（world_name → world_id），不新造映射。

输出：p0c4-quest-area-snippets.xml（可直接粘贴进 ai-areas.xml）
"""
import os
import re
import sys
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
WORLDS = Path(f"{os.environ.get('AION_RETAIL_ROOT', os.path.expanduser('~/IdeaProjects/58Server'))}/Map/Worlds")
PROD = REPO / 'src/main/resources/aion/definitions/compact/ai/ai-areas.xml'
OUT = Path(__file__).resolve().parent / 'p0c4-quest-area-snippets.xml'
# 需要补的生产条目：真端所在世界目录 + 区域名
WANTED = (
    ('df2a', 'InvadePortalDest_41_questArea_02'),
    ('df2a', 'InvadePortalDest_41_questArea_03'),
    ('lf2a', 'InvadePortalDest_42_questArea_02'),
    ('lf2a', 'InvadePortalDest_42_questArea_03'),
)
HUNT = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}/src/main/resources/aion/data/static_data/quest_definition/quests")


def world_id_map():
    """生产 ai-areas.xml 里既有 world_name → world_id（同名必须唯一）。"""
    s = PROD.read_text(encoding='utf-8')
    out = {}
    for m in re.finditer(r'<[a-z_]+ world_id="(\d+)" world_name="([^"]+)"', s):
        world_id, name = int(m.group(1)), m.group(2)
        out.setdefault(name, set()).add(world_id)
    return out


def blocks(world_dir):
    s = (WORLDS / world_dir / 'world.xml').read_bytes().decode('utf-16', errors='replace')
    out = {}
    for m in re.finditer(r'<questscript_area\b[^>]*>(.*?)</questscript_area>', s, re.S):
        body = m.group(1)
        name = re.search(r'<name>(.*?)</name>', body, re.S).group(1).strip()
        quest = (re.search(r'<quest>(.*?)</quest>', body, re.S) or [None, ''])[1].strip()
        points = [(x, y) for x, y in re.findall(
            r'<data>\s*<x>([-\d.eE]+)</x>\s*<y>([-\d.eE]+)</y>\s*</data>', body)]
        bottom = re.search(r'<bottom>([-\d.eE]+)</bottom>', body).group(1)
        top = re.search(r'<top>([-\d.eE]+)</top>', body).group(1)
        out[name] = {'quests': [q for q in re.split(r'[,\s]+', quest) if q],
                     'points': points, 'bottom': bottom, 'top': top}
    return out


def main():
    ids = world_id_map()
    lines = []
    for world_dir, area_name in WANTED:
        candidates = ids.get(world_dir)
        if not candidates or len(candidates) != 1:
            print('SKIP world_id 不唯一或缺失:', world_dir, candidates, file=sys.stderr)
            continue
        world_id = next(iter(candidates))
        block = blocks(world_dir).get(area_name)
        if block is None:
            print('SKIP 真端缺区域:', world_dir, area_name, file=sys.stderr)
            continue
        quests = ','.join(block['quests'])
        lines.append('  <quest_area world_id="%d" world_name="%s" name="%s" quests="%s" bottom="%s" top="%s">'
                     % (world_id, world_dir, area_name, quests, block['bottom'], block['top']))
        for x, y in block['points']:
            lines.append('    <point x="%s" y="%s"/>' % (x, y))
        lines.append('  </quest_area>')
        local = [q for q in block['quests'] if (HUNT / ('%s.xml' % q)).exists()]
        print('条目 %-12s %-34s world=%d quests=%s 本服有 XML=%s'
              % (world_dir, area_name, world_id, quests, ','.join(local) or '-'))
    OUT.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print('输出:', OUT, '条目数:', sum(1 for line in lines if line.startswith('  <quest_area')))
    return 0


if __name__ == '__main__':
    sys.exit(main())
