#!/usr/bin/env python3
"""Phase 1.1：注册表之外的任务由哪些表驱动（DataDriven / combine / challenge / CSV）。

输入：
  quest_registry.tsv                     —— ScriptDLL64 注册点（Phase 1）
  <客户端解包根>/Quest_unpacked/*.xml|csv  —— 客户端任务表
  src/main/resources/aion/data/static_data/quest/definitions/quests/*.xml —— 本仓库任务清单
输出：各表覆盖的 quest id 集合与本仓库任务集合的交集统计。
"""
import csv
import glob
import os
import re
import xml.etree.ElementTree as ET

BASE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(BASE, "../../.."))
CLIENT = f"{REPO.parent / 'PycharmProjects' / 'unpak'}/Quest_unpacked"


def registry_quests():
    ids = {}
    with open(os.path.join(BASE, "quest_registry.tsv"), encoding="utf-8") as fh:
        for line in fh:
            if line.startswith("#") or line.startswith("quest_id"):
                continue
            parts = line.split("\t")
            if parts[0].isdigit():
                ids.setdefault(int(parts[0]), 0)
                ids[int(parts[0])] += 1
    return ids


def repo_quests():
    out = set()
    pat = os.path.join(REPO, "src/main/resources/aion/data/static_data/quest/definitions/quests/*.xml")
    for path in glob.glob(pat):
        m = re.search(r"/(\d+)\.xml$", path)
        if m:
            out.add(int(m.group(1)))
    return out


def xml_quest_ids(path):
    """data_driven_quest.xml 等：<quest><id>N</id>…"""
    out = set()
    try:
        tree = ET.parse(path)
    except ET.ParseError:
        return out
    for quest in tree.getroot():
        node = quest.find("id")
        if node is not None and (node.text or "").strip().isdigit():
            out.add(int(node.text.strip()))
    return out


def attr_id_quests(path):
    """combine_task.xml / challenge_task.xml：<id id="5000">"""
    out = set()
    try:
        tree = ET.parse(path)
    except ET.ParseError:
        return out
    for node in tree.getroot():
        raw = (node.get("id") or "").strip()
        if raw.isdigit():
            out.add(int(raw))
    return out


def csv_quest_ids(path, column=0):
    out = set()
    with open(path, newline="", encoding="utf-8", errors="replace") as fh:
        for row in csv.reader(fh, skipinitialspace=True):
            if row and row[column].strip().isdigit():
                out.add(int(row[column].strip()))
    return out


def main():
    reg = registry_quests()
    repo = repo_quests()
    reg_ids = set(reg)
    dd = xml_quest_ids(os.path.join(CLIENT, "data_driven_quest.xml"))
    combine = attr_id_quests(os.path.join(CLIENT, "combine_task.xml"))
    challenge = attr_id_quests(os.path.join(CLIENT, "challenge_task.xml"))
    client_all = xml_quest_ids(os.path.join(CLIENT, "quest.xml"))
    mon = csv_quest_ids(os.path.join(CLIENT, "quest_monster.csv"))
    script = csv_quest_ids(os.path.join(CLIENT, "quest_script_monster.csv"))

    tables = {"DataDriven": dd, "CombineTask": combine, "ChallengeTask": challenge,
              "quest_monster.csv": mon, "quest_script_monster.csv": script}
    print(f"客户端 quest.xml 任务数     : {len(client_all)}")

    print(f"注册表 quest 数            : {len(reg_ids)}")
    print(f"本仓库 quest 定义 XML 数   : {len(repo)}")
    print()
    print("表覆盖（quest 数 / 与本仓库交集）")
    for name, ids in tables.items():
        print(f"  {name:<26} {len(ids):>6}   ∩repo={len(ids & repo):>6}")

    repo_no_reg = repo - reg_ids
    print()
    print(f"本仓库无注册点的任务       : {len(repo_no_reg)}")
    covered = set()
    for name, ids in tables.items():
        hit = repo_no_reg & ids
        covered |= hit
        print(f"  由 {name:<24} 覆盖 {len(hit)}")
    print(f"  合计被表覆盖             : {len(covered)}")
    print(f"  仍无任何表/注册点        : {len(repo_no_reg - covered)}")
    rest = sorted(repo_no_reg - covered)
    print(f"  剩余示例(前 30)          : {rest[:30]}")
    print(f"  其中客户端已不存在       : {len([q for q in rest if q not in client_all])}")

    # SEEN_MARKER 抽样
    for qid in (10501, 15551, 15552, 15563, 16824):
        where = [n for n, ids in tables.items() if qid in ids]
        print(f"  quest {qid}: registry_steps={reg.get(qid, 0)} tables={where}")


if __name__ == "__main__":
    main()
