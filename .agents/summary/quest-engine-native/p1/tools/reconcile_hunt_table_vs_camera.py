#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""对拍：入仓 Quest_SimpleHunt.xml 表行推导的相机规约 vs P0a 脚本相机矩阵（camera-params.tsv）。

验证三件事（P1 第二横切证据）：
  1. 宽度规则「任一 count>63 ⇒ 10 位，否则 6 位」与脚本矩阵 1812 任务全部一致；
  2. 脚本 (slot,required) ⊆ 表 (countN,monsterN) 且 required==count；
  3. 表推导 fullValue（各槽 count 按位移或）== 脚本调用里的字面 fullValue。
另产出：表有行而脚本无相机（休眠）任务清单。
只读；输出写本目录。Reconciles table-derived camera specs against the P0a script camera matrix.
"""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parents[5]
TABLE = REPO / "src/main/resources/aion/data/static_data/quest/retail/Quest_SimpleHunt.xml"
CAMERA_TSV = REPO / ".agents/summary/quest-engine-native/p0a/camera-params.tsv"
OUT_DIR = Path(__file__).resolve().parent

MAX_SIX_BIT = 63


def strip_doctype(raw: str) -> str:
    # 真端表带内部 DTD 实体子集（...]>）；ElementTree 不需要实体定义（值不含实体引用）。
    # The retail table carries an internal DTD entity subset; ElementTree needs none of it here.
    return re.sub(r"<!DOCTYPE.*?\]>", "", raw, flags=re.S)


def parse_table() -> dict[int, dict]:
    root = ET.fromstring(strip_doctype(TABLE.read_text(encoding="utf-8")))
    if root.tag != "quest_simplehunts":
        raise SystemExit(f"unexpected root <{root.tag}>")
    rows: dict[int, dict] = {}
    for el in root:
        if el.tag != "id":
            continue  # 注释掉的休眠行（99003/99004 等）天然跳过 / commented-out dormant rows skipped
        qid = int(el.get("id"))
        if qid in rows:
            raise SystemExit(f"duplicate row id {qid}")
        acquire = el.findtext("acquired_npc_name")
        reward = el.findtext("reward_npc_name")
        slots: dict[int, tuple[int, list[str]]] = {}
        for child in el:
            m = re.fullmatch(r"count([1-5])", child.tag)
            if m:
                slot = int(m.group(1))
                count = int(child.text.strip())
                # 源表元素文本可跨行：先归一内部空白再按逗号切名单。
                # Source element text can span lines: normalize inner whitespace, then split the list.
                raw_monsters = " ".join((el.findtext(f"monster{slot}") or "").split())
                monsters = [name for name in re.split(r"\s*,\s*", raw_monsters) if name]
                if slot in slots:
                    raise SystemExit(f"quest {qid}: duplicate count{slot}")
                slots[slot] = (count, monsters)
        rows[qid] = {
            "acquire": (acquire or "").strip(),
            "reward": (reward or "").strip(),
            "slots": slots,
        }
    return rows


def derive_width(slots: dict[int, tuple[int, list[str]]]) -> int:
    # 宽度规则：任一 count 超过 6 位掩码 ⇒ 10 位；否则 6 位。
    # Width rule: any count above the 6-bit mask implies 10-bit; otherwise 6-bit.
    return 10 if any(count > MAX_SIX_BIT for count, _ in slots.values()) else 6


def derive_full_value(slots: dict[int, tuple[int, list[str]]], width: int) -> int:
    mask = 0x3FF if width == 10 else 0x3F
    value = 0
    for slot, (count, _) in slots.items():
        if count > mask:
            raise SystemExit(f"count {count} exceeds {width}-bit mask")
        value |= count << (slot - 1) * (10 if width == 10 else 6)
    return value


def parse_camera_matrix() -> dict[int, dict]:
    quests: dict[int, dict] = {}
    lines = CAMERA_TSV.read_text(encoding="utf-8").splitlines()[1:]
    for line in lines:
        if not line.strip():
            continue  # 并发车道可能写入空行 / concurrent lanes may append blank lines
        cols = line.split("\t")
        qid, width, slot, required, full_value = (int(cols[0]), int(cols[1]), int(cols[2]),
                                                  int(cols[3]), int(cols[4]))
        entry = quests.setdefault(qid, {"width": width, "slots": {}, "full_value": full_value})
        if entry["width"] != width:
            raise SystemExit(f"quest {qid}: mixed widths in matrix")
        if slot in entry["slots"]:
            raise SystemExit(f"quest {qid}: duplicate slot {slot}")
        entry["slots"][slot] = required
    return quests


def main() -> int:
    table = parse_table()
    matrix = parse_camera_matrix()
    mismatches: list[str] = []
    width_rule_failures: list[str] = []
    required_mismatches: list[str] = []
    full_value_mismatches: list[str] = []

    for qid in sorted(matrix):
        script = matrix[qid]
        if qid not in table:
            mismatches.append(f"{qid}: script camera but no table row")
            continue
        row = table[qid]
        derived_width = derive_width(row["slots"])
        if derived_width != script["width"]:
            width_rule_failures.append(
                f"{qid}: rule={derived_width} script={script['width']} counts={row['slots']}")
            continue
        derived_full = derive_full_value(row["slots"], derived_width)
        if derived_full != script["full_value"]:
            full_value_mismatches.append(
                f"{qid}: derived=0x{derived_full:x} script=0x{script['full_value']:x}")
        for slot, required in script["slots"].items():
            if slot not in row["slots"]:
                required_mismatches.append(f"{qid}: script slot {slot} absent from table")
            elif row["slots"][slot][0] != required:
                required_mismatches.append(
                    f"{qid}: slot {slot} table count={row['slots'][slot][0]} script required={required}")

    dormant = sorted(set(table) - set(matrix))
    report = {
        "table_rows": len(table),
        "script_camera_quests": len(matrix),
        "dormant_table_only_quests": len(dormant),
        "width_rule_failures": len(width_rule_failures),
        "required_mismatches": len(required_mismatches),
        "full_value_mismatches": len(full_value_mismatches),
    }
    print("== reconciliation ==")
    for key, value in report.items():
        print(f"{key}: {value}")
    for name, items in (("WIDTH_RULE", width_rule_failures), ("REQUIRED", required_mismatches),
                        ("FULL_VALUE", full_value_mismatches)):
        for item in items[:20]:
            print(f"{name}: {item}")
    lines_out = ["questId\tacquire\treward\tslots(count,monsters)"]
    lines_out += [
        f"{qid}\t{table[qid]['acquire']}\t{table[qid]['reward']}\t"
        + ";".join(f"{s}:{c}:{','.join(m) or '-'}" for s, (c, m) in sorted(table[qid]["slots"].items()))
        for qid in dormant
    ]
    (OUT_DIR / "hunt-dormant-table-only.tsv").write_text("\n".join(lines_out) + "\n", encoding="utf-8")
    ok = not (mismatches or width_rule_failures or required_mismatches or full_value_mismatches)
    print("VERDICT:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
