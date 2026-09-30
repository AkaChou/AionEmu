#!/usr/bin/env python3
"""生成客户端 NPC 集合投影：接取（start_npc_ids）与交付（end_npc_ids）两轴。

来源：外部客户端对话映射 `legacy-quest-dialog-contracts.csv`（client-dialog-mapping 产物，
按 ENVIRONMENT.md 的「同宿主目录约定」解析为 `<仓库根>/../AionEmu-headless/data/client-dialog-mapping/`）。
只投影声明**多于一个** id 的行：单 id 声明由真端名解析直接给出，不需要仲裁；
多 id 行才是「真端逻辑名 → 客户端集合」判据的输入。

产物（客户端数据投影，不是手工补丁；重跑须核对 source_sha256）：
- src/main/resources/quest/quest_client_accept_npc_sets.tsv   （start_type=TALK 且 start_npc_ids > 1）
- src/main/resources/quest/quest_client_handin_npc_sets.tsv   （end_npc_ids > 1）
"""
from __future__ import annotations

import csv
import hashlib
import pathlib

REPO = next(parent for parent in pathlib.Path(__file__).resolve().parents if (parent / "pom.xml").exists())
SOURCE = REPO.parent / "AionEmu-headless" / "data" / "client-dialog-mapping" / "legacy-quest-dialog-contracts.csv"
ACCEPT_TARGET = REPO / "src/main/resources/quest/quest_client_accept_npc_sets.tsv"
HANDIN_TARGET = REPO / "src/main/resources/quest/quest_client_handin_npc_sets.tsv"


def main() -> None:
    raw = SOURCE.read_bytes()
    sha = hashlib.sha256(raw).hexdigest()
    accept: list[tuple[int, list[int]]] = []
    handin: list[tuple[int, list[int]]] = []
    with SOURCE.open(encoding="utf-8-sig", newline="") as fh:
        for row in csv.DictReader(fh):
            quest_id = int(row["quest_id"])
            starts = [int(token) for token in row["start_npc_ids"].split()]
            ends = [int(token) for token in row["end_npc_ids"].split()]
            if row["start_type"] == "TALK" and len(starts) > 1:
                accept.append((quest_id, starts))
            if len(ends) > 1:
                handin.append((quest_id, ends))
    for rows, target, role, pick in (
        (accept, ACCEPT_TARGET, "接取",
         "start_type=TALK 且 start_npc_ids 多于一个的行"),
        (handin, HANDIN_TARGET, "交付",
         "end_npc_ids 多于一个的行（客户端声明的交付/领奖 NPC 集）"),
    ):
        rows.sort()
        lines = [
            f"# Aion 5.8 客户端{role} NPC 集合投影（quest_id \\t npc_id,npc_id,...）",
            f"# 来源：客户端对话映射 legacy-quest-dialog-contracts.csv 的 {pick}",
            f"# source_sha256={sha}",
            "# 消费：RetailSimpleTalkDefinitionCompiler / RetailSimpleItemPlayDefinitionCompiler——"
            "真端名解析出 >1 id 时与该集合逐元素对拍，不等即 fail-closed",
        ]
        lines += [f"{quest_id}\t{','.join(str(i) for i in ids)}" for quest_id, ids in rows]
        target.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"wrote {target.name} rows={len(rows)} sha256={sha}")


if __name__ == "__main__":
    main()
