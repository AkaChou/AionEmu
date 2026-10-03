#!/usr/bin/env python3
"""扫描真端 ScriptDLL64.c 的 IOneQuestScriptNpc 注册面 FUN_180cb5920(name, id)。

背景：P10 首轮扫描只查了「首参字面量」注册口（族表 row 注册 FUN_180cab520 等），
完全漏掉 (name, id) 形参在第三位的 FUN_180cb5920 —— 该函数体实测构造 IOneQuestScriptNpc
（vftable 赋值 + 名字拷贝 + id 入 map）。本脚本把该注册面的 id 全集抽出来，
与 retail-xml-retention.tsv 的 NO_TABLE(176) / XML_RETENTION(740) / RETAIL_TABLE(5484) 求交。
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

SCRIPT = Path(sys.argv[1] if len(sys.argv) > 1 else
              "/Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c")
LEDGER = Path(sys.argv[2] if len(sys.argv) > 2 else
              "/Users/mc/IdeaProjects/AionEmu-test/src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.xml")

src = SCRIPT.read_text(encoding="latin-1", errors="replace")
calls = re.findall(r"FUN_180cb5920\(([^;\n]*)\);", src)

ids = {}
for call in calls:
    last = call.rsplit(",", 1)[-1].strip()
    if re.fullmatch(r"0x[0-9a-fA-F]+", last):
        value = int(last, 16)
    elif re.fullmatch(r"\d+", last):
        value = int(last)
    else:
        continue
    ids.setdefault(value, 0)
    ids[value] += 1

owner = {}
for row in ET.parse(LEDGER).getroot().findall("quest"):
    owner[int(row.findtext("quest_id"))] = (row.findtext("owner") or "", row.findtext("reason") or "")

no_table = {q for q, (o, r) in owner.items() if r == "NO_TABLE"}
xml_ret = {q for q, (o, r) in owner.items() if o == "XML_RETENTION"}
table = {q for q, (o, r) in owner.items() if o == "RETAIL_TABLE"}

hit_ids = sorted(set(ids) & set(owner))
print(f"FUN_180cb5920 调用点: {len(calls)}；可解析第三参: {sum(ids.values())}；唯一 id: {len(ids)}")
print(f"落在 6224 任务全集内的注册 id: {len(hit_ids)}")
print(f"  ∩ NO_TABLE(176)      = {len(hit_ids and (set(hit_ids) & no_table))}")
print(f"  ∩ XML_RETENTION(740) = {len(set(hit_ids) & xml_ret)}")
print(f"  ∩ RETAIL_TABLE(5484) = {len(set(hit_ids) & table)}")
print(f"NO_TABLE 命中明细: {sorted(set(hit_ids) & no_table)}")
print(f"XML_RETENTION 命中明细: {sorted(set(hit_ids) & xml_ret)}")
print(f"注册 id 全集样例(前 40): {hit_ids[:40]}")
