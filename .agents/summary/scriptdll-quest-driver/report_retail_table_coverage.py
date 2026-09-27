#!/usr/bin/env python3
"""真端服务端模板表（58Server/Map/XML）对本仓库任务的覆盖统计。

输入：/Users/mc/IdeaProjects/58Server/Map/XML/{Quest_*.xml,data_driven_quest.xml,quest.xml}
     src/main/resources/aion/data/static_data/quest_definition/quests/*.xml
输出：每张表的行数 / 与本仓库交集 / 并集覆盖
"""
import glob
import os
import re
import xml.etree.ElementTree as ET

RETAIL = "/Users/mc/IdeaProjects/58Server/Map/XML"
REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../.."))


def read(path):
    raw = open(path, "rb").read()
    enc = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    return raw.decode(enc, errors="replace")


def table_ids(path):
    """<id id="1102"> 形式"""
    out = set()
    for m in re.finditer(r'<id\s+id="(\d+)"', read(path)):
        out.add(int(m.group(1)))
    return out


def plain_ids(path):
    """<id>1102</id> 形式（data_driven_quest.xml / quest.xml）"""
    out = set()
    for m in re.finditer(r"<id>(\d+)</id>", read(path)):
        out.add(int(m.group(1)))
    return out


def repo_quests():
    out = set()
    for p in glob.glob(os.path.join(REPO, "src/main/resources/aion/data/static_data/quest_definition/quests/*.xml")):
        m = re.search(r"/(\d+)\.xml$", p)
        if m:
            out.add(int(m.group(1)))
    return out


def main():
    repo = repo_quests()
    tables = {}
    for p in sorted(glob.glob(os.path.join(RETAIL, "Quest_*.xml"))):
        tables[os.path.basename(p)[:-4]] = table_ids(p)
    tables["data_driven_quest.xml"] = plain_ids(os.path.join(RETAIL, "data_driven_quest.xml"))
    tables["quest.xml(服务器权威表)"] = plain_ids(os.path.join(RETAIL, "quest.xml"))

    print(f"本仓库任务定义 XML: {len(repo)}")
    print()
    print(f"{'表':<28}{'行数':>7}{'∩本仓库':>10}")
    union = set()
    for name, ids in tables.items():
        print(f"{name:<28}{len(ids):>7}{len(ids & repo):>10}")
        if name != "quest.xml(服务器权威表)":
            union |= ids
    print()
    print(f"模板表并集（不含 quest.xml）: {len(union)}   ∩本仓库={len(union & repo)}")
    print(f"本仓库未被模板表覆盖        : {len(repo - union)}")
    rest = sorted(repo - union)
    print(f"  示例: {rest[:25]}")


if __name__ == "__main__":
    main()
