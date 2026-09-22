#!/usr/bin/env python3
"""离线模拟 QuestPrerequisiteRetailContractTest 三个断言（只读，替代未授权的 Maven 运行）。"""
from __future__ import annotations
import xml.etree.ElementTree as ET
from pathlib import Path

QUESTS = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
TSV = Path("src/test/resources/quest/quest-prerequisite-retail-contract.tsv")

def effective(path: Path) -> set[int]:
    root = ET.parse(path).getroot()
    eff = {int(x.get("id")) for x in root.findall("./metadata/prerequisites/quest")}
    eff |= {int(c.get("quest-id")) for c in root.findall("./metadata/start-conditions/condition")
            if (c.get("type") or "").lower() == "finished"}
    eff |= {int(c.get("quest-id")) for g in root.findall("./metadata/start-condition-groups/group")
            for c in g.findall("condition") if (c.get("type") or "").lower() == "finished"}
    return eff

def main():
    contract = {}
    for line in TSV.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#") or line.startswith("quest_id\t"):
            continue
        q, branches = line.split("\t")
        contract[int(q)] = [[int(x) for x in b.split(",")] for b in branches.split(";")]
    files = {int(p.stem): p for p in QUESTS.glob("*.xml") if p.stem.isdigit()}
    eff = {q: effective(p) for q, p in files.items()}

    violations, checked, skipped = [], 0, 0
    for q, branches in contract.items():
        if q not in eff:
            continue
        for b in branches:
            if not set(b) <= set(files):
                skipped += 1
                continue
            checked += 1
            if not set(b) <= eff[q]:
                violations.append((q, b, sorted(eff[q])))
    print(f"[gate1] checked_branches={checked} skipped_branches={skipped} violations={len(violations)}")
    for v in violations[:10]:
        print("   ", v)

    batch = {2533:2532,3050:3049,15471:15402,15551:15550,15552:15551,15553:15552,15554:15553,
             15563:15550,15595:15550,15673:15550,16823:16822,16824:16821,16825:16822,18035:18036,
             18821:18830,18993:18992,21004:21001,21080:21065,21201:21200,2641:2619,26823:26822,
             28035:28036,30719:30708,49004:49003,80343:80341}
    bad = [(q, sorted(eff[q])) for q, exp in batch.items() if eff[q] != {exp}]
    print(f"[gate2] batch_exact_failures={len(bad)} {bad[:5]}")
    print("[gate2] 2641 has 2640:", 2640 in eff[2641])

    for q, dep in {1870:1868, 2869:2868, 2870:2868}.items():
        ported = dep in files
        print(f"[gate3] quest {q} dep {dep} ported={ported} declared={dep in eff[q]}")

if __name__ == "__main__":
    main()
