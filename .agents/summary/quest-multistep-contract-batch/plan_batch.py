#!/usr/bin/env python3
"""为每个目标任务生成「行 → NPC → 客户端链」映射计划（只读，输出 TSV）。"""
from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import dump_chain_evidence as D  # noqa: E402

TARGETS = [1183, 1371, 1430, 1479, 1514, 1582, 1643, 1721, 1724, 1922, 1938, 2223, 2239, 2289, 2307, 2372,
           2449, 2513, 2600, 2646, 2692, 2767, 2947, 2962, 3050, 3088, 3966, 3968, 4501, 4542, 4712, 4942]


def npc_index() -> tuple[dict[int, str], dict[str, int]]:
    by_id = D.npc_names()
    by_name = {name.lower(): npc_id for npc_id, name in by_id.items()}
    return by_id, by_name


def candidate_npcs(quest_id: int, facts: dict) -> set[int]:
    ids = {int(v) for v in facts["start_npcs"] + facts["completes"]}
    entry = D.retail_entry(quest_id)
    if entry:
        _tag, attrs = entry
        for key, value in attrs.items():
            if "npc" in key:
                ids.update(int(v) for v in re.findall(r"\d+", value))
    for step in D.retail_steps(quest_id):
        if step.get("ids", "").isdigit():
            ids.add(int(step["ids"]))
    return ids


def row_keys(row: str) -> list[str]:
    return re.findall(r"STR_DIC_N_([A-Za-z0-9_]+)", row)


def main() -> int:
    by_id, by_name = npc_index()
    rows_out = []
    for quest_id in TARGETS:
        summary, pages, first_text = D.client(quest_id)
        chain_list = D.chains(pages, first_text)
        facts = D.xml_facts(quest_id)
        cand = candidate_npcs(quest_id, facts)
        mapping = []
        for index, row in enumerate(summary):
            keys = row_keys(row)
            hit = next((by_name[k.lower()] for k in keys if k.lower() in by_name), None)
            mapping.append(f"{index}:{'/'.join(keys) or '-'}->{hit or '?'}({by_id.get(hit, '?') if hit else '?'})")
        setpro = [c for c in chain_list if c[0].startswith("SETPRO")]
        reward = [c for c in chain_list if c[0] == "SELECT_QUEST_REWARD"]
        status = "AUTO" if (len(summary) >= 2 and len(setpro) == len(summary) - 1 and len(reward) == 1
                            and all("?" not in m.split("->")[1] for m in mapping[:-1])) else "MANUAL"
        rows_out.append({
            "quest_id": quest_id,
            "status": status,
            "rows": len(summary),
            "setpro_chains": len(setpro),
            "reward_chains": len(reward),
            "row_to_npc": " | ".join(mapping),
            "candidate_npcs": " ".join(f"{i}:{by_id.get(i, '?')}" for i in sorted(cand)),
            "chain_seq": " ; ".join(f"{c[0]}:{'>'.join(c[1])}" for c in chain_list),
            "xml_states": " ".join(facts["states"]),
            "xml_pages": " ".join(facts["pages"]),
        })
    columns = list(rows_out[0])
    with Path(__file__).with_name("batch-plan.tsv").open("w", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, delimiter="\t")
        writer.writeheader()
        writer.writerows(rows_out)
    for row in rows_out:
        print(f"{row['quest_id']:>6} {row['status']:>6} rows={row['rows']:>2} setpro={row['setpro_chains']:>2} "
              f"reward={row['reward_chains']} | {row['row_to_npc']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
