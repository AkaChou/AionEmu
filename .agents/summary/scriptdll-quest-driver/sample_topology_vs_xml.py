#!/usr/bin/env python3
"""Phase 1 抽样对账：注册表步骤 vs 现有 quest XML 结构。

用法: python3 -B sample_topology_vs_xml.py --out sample_topology_vs_xml.txt
"""
from __future__ import annotations
import os

import argparse
import collections
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

QUESTS = Path(f"{REPO}/src/main/resources/aion/data/static_data/quest/definitions/quests")
SAMPLES = [1102, 1517, 15551, 15552, 15563, 16824, 2641, 5000, 21296, 10501, 1365, 80343]
TAGS = ("node", "transition", "dialog", "actions", "counter", "counter-grid", "kill-chain",
        "kill-routes", "npc-item-report", "npc-complete", "after-commit")


def load_registry(path: Path):
    per_quest = collections.defaultdict(list)
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#") or line.startswith("quest_id\t"):
            continue
        cols = line.split("\t")
        per_quest[int(cols[0])].append((int(cols[1]), cols[2], cols[3], cols[4]))
    return per_quest


def load_families(path: Path):
    fam = {}
    for line in path.read_text(encoding="utf-8").splitlines()[1:]:
        if not line.strip():
            continue
        cols = line.split("\t")
        fam[cols[0]] = cols[3]
    return fam


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--registry", default="quest_registry.tsv")
    ap.add_argument("--families", default="helper_families.tsv")
    ap.add_argument("--out", default="sample_topology_vs_xml.txt")
    args = ap.parse_args()

    registry = load_registry(Path(args.registry))
    fam = load_families(Path(args.families))
    lines: list[str] = []
    for qid in SAMPLES:
        steps = registry.get(qid, [])
        lines.append(f"=== quest {qid}: registry_steps={len(steps)}")
        for idx, helper, params, owner in steps:
            lines.append(f"    step{idx}  {fam.get(helper, '?')}  {helper}  params=[{params}]  owner={owner}")
        path = QUESTS / f"{qid}.xml"
        if not path.exists():
            lines.append("    (no repo XML)")
            continue
        root = ET.parse(path).getroot()
        counts = collections.Counter(el.tag for el in root.iter())
        shown = " ".join(f"{t}={counts[t]}" for t in TAGS if counts[t])
        meta = root.find("./metadata")
        lines.append(f"    XML: {shown or '-'}")
        lines.append(f"    metadata: level={meta.get('min-level')} category={meta.get('category')} "
                     f"prereq={[q.get('id') for q in meta.findall('./prerequisites/quest')]}")
        lines.append("")
    text = "\n".join(lines) + "\n"
    Path(args.out).write_text(text, encoding="utf-8")
    print(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
