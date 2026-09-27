#!/usr/bin/env python3
"""统计各真端任务族模板表与本仓库 6224 个生产任务定义的覆盖面。

用法: python3 -B retail_family_coverage.py
真端表为 UTF-16，行形如 <id id="1102">…</id>；DataDriven 用 <id id=…> 同构。
"""
import re
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
RETAIL = Path('/Users/mc/IdeaProjects/58Server/Map/XML')
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml'
TABLES = ['Quest_SimpleHunt.xml', 'Quest_SimpleTalk.xml', 'Quest_SimpleCollectItem.xml', 'Quest_SimpleUseItem.xml',
          'Quest_SimpleItemPlay.xml', 'Quest_SimpleSerialHunt.xml', 'Quest_SimpleGather.xml', 'Quest_CombineTask.xml',
          'data_driven_quest.xml']


def repo_ids():
    text = CATALOG.read_text(encoding='utf-8')
    return {int(v) for v in re.findall(r'<definition id="(\d+)"', text)}


def table_ids(path):
    text = path.read_text(encoding='utf-16')
    ids = {int(v) for v in re.findall(r'<id id="(\d+)"', text)}
    if not ids:  # data_driven_quest.xml 用 <id>N</id>
        ids = {int(v) for v in re.findall(r'<id>(\d+)</id>', text)}
    return ids


def main():
    repo = repo_ids()
    print(f"repo 生产任务定义 = {len(repo)}")
    covered = set()
    for name in TABLES:
        path = RETAIL / name
        if not path.is_file():
            print(f"  {name:28s} 缺失")
            continue
        ids = table_ids(path)
        inter = ids & repo
        covered |= inter
        print(f"  {name:28s} rows={len(ids):5d}  ∩repo={len(inter):5d}  "
              f"({len(inter) * 100.0 / len(repo):5.1f}% of repo)")
    print(f"模板并集 ∩repo = {len(covered)} ({len(covered) * 100.0 / len(repo):.1f}%)")
    print(f"无模板行的仓库任务 = {len(repo - covered)}")
    for name in TABLES[:6]:
        path = RETAIL / name
        if path.is_file():
            ids = table_ids(path) - repo
            print(f"  {name:28s} 真端有行但本仓库未移植 = {len(ids)}")
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
