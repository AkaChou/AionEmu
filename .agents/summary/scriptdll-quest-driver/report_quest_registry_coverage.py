#!/usr/bin/env python3
"""Phase 1 覆盖报表：注册表 <-> 真端 quest.xml / 模板表 / 本仓库目录。

用法:
    python3 -B report_quest_registry_coverage.py \
        --registry quest_registry.tsv --helpers registry_helpers.tsv \
        --out coverage-report.txt --families helper_families.tsv
"""
from __future__ import annotations

import argparse
import re
from collections import defaultdict
from pathlib import Path

RETAIL = Path("/Users/mc/IdeaProjects/58Server/Map/XML")
REPO_QUESTS = Path("/Users/mc/IdeaProjects/AionEmu-test/src/main/resources/aion/data/static_data/quest_definition/quests")
TEMPLATES = {
    "SimpleHunt": "Quest_SimpleHunt.xml",
    "SimpleTalk": "Quest_SimpleTalk.xml",
    "SimpleCollectItem": "Quest_SimpleCollectItem.xml",
    "SimpleUseItem": "Quest_SimpleUseItem.xml",
    "SimpleItemPlay": "Quest_SimpleItemPlay.xml",
    "SimpleSerialHunt": "Quest_SimpleSerialHunt.xml",
    "CombineTask": "Quest_CombineTask.xml",
    "DataDriven": "data_driven_quest.xml",
}


def load_registry(path: Path):
    per_quest: dict[int, list[tuple[int, str, str]]] = defaultdict(list)
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#") or line.startswith("quest_id\t"):
            continue
        cols = line.split("\t")
        qid, idx, helper, params = cols[0], cols[1], cols[2], cols[3]
        per_quest[int(qid)].append((int(idx), helper, params))
    return per_quest


def load_retail_ids() -> set[int]:
    text = (RETAIL / "quest.xml").read_text(encoding="utf-16", errors="replace")
    return {int(x) for x in re.findall(r"<id>(\d+)</id>", text)}


def load_template_ids() -> dict[str, set[int]]:
    out = {}
    for name, filename in TEMPLATES.items():
        text = (RETAIL / filename).read_text(encoding="utf-16", errors="replace")
        ids = {int(v) for v in re.findall(r'<id(?:\s+id)?="(\d+)"', text)}
        ids |= {int(v) for v in re.findall(r"<id>(\d+)</id>", text)}
        out[name] = ids
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--registry", default="quest_registry.tsv")
    ap.add_argument("--out", default="coverage-report.txt")
    ap.add_argument("--families", default="helper_families.tsv")
    args = ap.parse_args()

    per_quest = load_registry(Path(args.registry))
    registered = set(per_quest)
    retail = load_retail_ids()
    templates = load_template_ids()
    repo = {int(p.stem) for p in REPO_QUESTS.glob("*.xml") if p.stem.isdigit()}

    lines: list[str] = []
    add = lines.append
    add(f"registry quests={len(registered)}  retail quests={len(retail)}  repo quests={len(repo)}")
    add(f"registry ∩ retail={len(registered & retail)} ({len(registered & retail)/len(retail):.1%} of retail)")
    add(f"registry ∩ repo   ={len(registered & repo)} ({len(registered & repo)/len(repo):.1%} of repo)")

    covered_by_template = set().union(*templates.values())
    add("")
    add("== 各族：模板表行数 / 注册表命中 ==")
    for name, ids in templates.items():
        hit = len(ids & registered)
        add(f"  {name:<17} rows={len(ids):<5} registered={hit:<5} {hit/max(1,len(ids)):.0%}")
    add(f"  模板并集={len(covered_by_template)}  真端任务覆盖={len(covered_by_template & retail)}"
        f" ({len(covered_by_template & retail)/len(retail):.0%})")
    no_table = retail - covered_by_template
    add(f"  无模板行的真端任务={len(no_table)}  其中在注册表={len(no_table & registered)}")
    repo_no_template = repo - covered_by_template
    add(f"  本仓库无模板任务={len(repo_no_template)}  其中在注册表={len(repo_no_template & registered)}")

    # helper -> 族归属（最大重叠）
    helper_ids: dict[str, set[int]] = defaultdict(set)
    from collections import Counter
    helper_stubs: Counter[str] = Counter()
    for qid, steps in per_quest.items():
        for _idx, helper, _params in steps:
            helper_ids[helper].add(qid)
            helper_stubs[helper] += 1
    add("")
    add("== 注册辅助函数 -> 族归属（按最大重叠）==")
    fh = Path(args.families).open("w", encoding="utf-8", newline="\n")
    fh.write("helper\tquests\tstubs\tfamily\tfamily_overlap\tid_min\tid_max\n")
    for helper in sorted(helper_ids, key=lambda h: -len(helper_ids[h])):
        ids = helper_ids[helper]
        best, best_ov = "-", 0
        for name, tids in templates.items():
            ov = len(ids & tids)
            if ov > best_ov:
                best, best_ov = name, ov
        if best_ov == 0:
            best = "SCRIPTED(无模板)"
        add(f"  {helper}: quests={len(ids):<5} stubs={helper_stubs[helper]:<5} family={best:<20} overlap={best_ov}")
        fh.write(f"{helper}\t{len(ids)}\t{helper_stubs[helper]}\t{best}\t{best_ov}\t{min(ids)}\t{max(ids)}\n")
    fh.close()

    text = "\n".join(lines) + "\n"
    Path(args.out).write_text(text, encoding="utf-8")
    print(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
