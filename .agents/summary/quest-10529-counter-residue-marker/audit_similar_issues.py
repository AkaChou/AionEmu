#!/usr/bin/env python3
"""10529 实机裁决后的两类同型问题全库排查(QE-054/QE-044 延伸,只读)。

排查 A(reward 歧义带误抬,10529 原发病):
  家族 = 客户端 quest_summary 末行与第 0 行 normalize 后同文的任务;
  歧义带 = reward 投影 == 末行索引 == last_start + 1。
  对家族成员比对「当前投影 vs legacy 落盘 step(迁移前 7e9f0316c^ 的 reward 通道调用)」:
  - SUSPECT_MISLIFT:投影 == 末行 != legacy step(10529 同型,批次误抬,任务书会整块空白)
  - HEALTHY_LEGACY:投影 == legacy step(保持落盘值,健康)
  - LEGACY_LASTROW:legacy 本身 step==nextStep==末行(投影=末行合法)
  - NEEDS_REVIEW:其它(无 legacy 证据/多值冲突)
排查 B(离开计数阶段未清零,QE-044 杀怪变体):
  同节点自环 increment-variable 的字段,在「离开该节点的推进边」上未全部 set 0:
  - COUNTER_RESIDUE:列出节点、计数字段、离开边、未清字段。

用法:python3 audit_similar_issues.py
输出:ambiguity-band-audit.tsv、counter-residue-audit.tsv(stdout 摘要)。过程产物不提交。
"""
from __future__ import annotations

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
UNPACK_CANDIDATES = [Path.home() / "PycharmProjects" / "unpak",
                     REPO.parent / "PycharmProjects" / "unpak"]
UNPACK = next((p for p in UNPACK_CANDIDATES if (p / "data_unpacked/Dialogs").is_dir()), None)
DIALOGS = UNPACK / "data_unpacked/Dialogs" if UNPACK else None
LEGACY_REV = "7e9f0316c^"
LEGACY_DIR = "src/main/java/com/aionemu/gameserver/quest/handlers"
FILE_PATTERN = re.compile(r"handlers/(?:[a-z_]+/)?_(\d{4,6})[A-Za-z_0-9]*\.java")
CALL_PATTERNS = [
    re.compile(r"useQuestItem\(env, *[^,]+(?:, *[^,]+)*?, *(\d+), *(\d+), *true"),
    re.compile(r"defaultCloseDialog\(env, *(\d+), *(\d+), *true"),
    re.compile(r"checkQuestItems\(env, *(\d+), *(\d+), *true"),
    re.compile(r"checkQuestItemsSimple\(env, *(\d+), *(\d+), *true"),
    re.compile(r"changeQuestStep\(env, *(\d+), *(\d+), *true"),
]
TOKEN_RE = re.compile(r"\[%[^\]]*\]")


def normalize(fragment: str) -> str:
    text = re.sub(r"<[^>]+>", "", fragment)
    text = TOKEN_RE.sub("", text)
    return re.sub(r"\s+", "", text)


def summary_rows(quest_id: int) -> list[str] | None:
    for path in DIALOGS.glob(f"*/quest_q{quest_id}.html"):
        text = path.read_text(encoding="utf-8", errors="replace")
        m = re.search(r'<HtmlPage name="quest_summary".*?</HtmlPage>', text, re.S)
        if not m:
            return None
        return [normalize(step) for step in re.findall(r"<step>.*?</step>", m.group(0), re.S)]
    return None


def xml_projections(quest_id: int) -> tuple[int | None, int | None]:
    path = QUESTS / f"{quest_id}.xml"
    if not path.exists():
        return None, None
    root = ET.parse(path).getroot()
    reward = None
    starts: list[int] = []
    for node in root.findall("./nodes/node"):
        status = node.get("status", "")
        value = None
        for var in node.findall("var"):
            if var.get("name") == "var0":
                value = int(var.get("value"))
        if status == "REWARD":
            reward = value
        elif status == "START" and node.get("label") != "unaccepted" and value is not None:
            starts.append(value)
    return reward, (max(starts) if starts else None)


def legacy_calls() -> dict[int, set[tuple[int, int]]]:
    result = subprocess.run(
        ["git", "grep", "-n", "-E",
         r"useQuestItem\(env|defaultCloseDialog\(env, *[0-9]+, *[0-9]+, *true|"
         r"checkQuestItems\(env|checkQuestItemsSimple\(env|changeQuestStep\(env, *[0-9]+, *[0-9]+, *true",
         LEGACY_REV, "--", f"{LEGACY_DIR}/*"],
        cwd=REPO, capture_output=True, text=True)
    calls: dict[int, set[tuple[int, int]]] = {}
    for line in result.stdout.splitlines():
        file_match = FILE_PATTERN.search(line)
        if file_match is None:
            continue
        quest_id = int(file_match.group(1))
        for pattern in CALL_PATTERNS:
            for match in pattern.finditer(line):
                calls.setdefault(quest_id, set()).add((int(match.group(1)), int(match.group(2))))
    return calls


def audit_a() -> list[tuple]:
    out = []
    for path in sorted(DIALOGS.glob("*/quest_q*.html")):
        m = re.search(r"quest_q(\d+)\.html$", path.name)
        if not m:
            continue
        quest_id = int(m.group(1))
        row_list = summary_rows(quest_id)
        if not row_list or len(row_list) < 3 or not row_list[0] or row_list[0] != row_list[-1]:
            continue
        reward, last_start = xml_projections(quest_id)
        if reward is None or last_start is None:
            out.append((quest_id, len(row_list), reward, last_start, "NO_XML_PROJECTION", ""))
            continue
        last_row = len(row_list) - 1
        calls = legacy.get(quest_id, set())
        legacy_steps = {step for step, _next in calls}
        if reward == last_row == last_start + 1:
            if any(step == last_start and nxt == last_row for step, nxt in calls):
                verdict = "SUSPECT_MISLIFT"
                detail = f"legacy={sorted(calls)}"
            elif reward in legacy_steps:
                verdict = "LEGACY_LASTROW"
                detail = f"legacy={sorted(calls)}"
            else:
                verdict = "NEEDS_REVIEW"
                detail = f"legacy={sorted(calls) if calls else 'none'}"
        elif reward == last_start and (not calls or reward in legacy_steps):
            verdict = "HEALTHY_LEGACY"
            detail = f"legacy={sorted(calls)}" if calls else "no-legacy-evidence"
        elif reward == last_start:
            verdict = "HEALTHY_LEGACY"
            detail = f"legacy={sorted(calls)}"
        else:
            verdict = "NEEDS_REVIEW"
            detail = f"legacy={sorted(calls) if calls else 'none'}"
        out.append((quest_id, len(row_list), reward, last_start, verdict, detail))
    return out


def audit_b() -> list[tuple]:
    out = []
    for path in sorted(QUESTS.glob("*.xml")):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        transitions = root.findall("./transitions/transition")
        nodes = root.findall("./nodes/node")
        node_var = {}
        for node in nodes:
            if node.get("status") != "START":
                continue
            for var in node.findall("var"):
                if var.get("name") == "var0":
                    node_var[node.get("label")] = int(var.get("value"))
        counters: dict[str, set[str]] = {}
        for tr in transitions:
            source = tr.get("source")
            if source is None or tr.get("target") != source:
                continue
            for action in tr.findall("./actions/set-variable"):
                if action.get("field") in ("var1", "var2", "var3", "var4", "var5"):
                    counters.setdefault(source, set()).add(action.get("field"))
            for action in tr.findall("./actions/increment-variable"):
                counters.setdefault(source, set()).add(action.get("action_field") or action.get("field"))
        if not counters:
            continue
        for tr in transitions:
            source, target = tr.get("source"), tr.get("target")
            if source is None or target == source or target is None:
                continue
            if node_var.get(target, node_var.get(source)) == node_var.get(source):
                continue  # 同阶段自环/分支,非离开边
            cleared = {a.get("field") for a in tr.findall("./actions/set-variable")
                       if a.get("value") == "0"}
            residue = counters.get(source, set()) - cleared
            if residue:
                event = tr.find("./event")
                event_tag = next(iter(event), None)
                event_desc = event_tag.tag if event_tag is not None else "?"
                out.append((path.stem, source, target, event_desc,
                            ",".join(sorted(counters.get(source, set()))),
                            ",".join(sorted(residue))))
    return out


legacy = legacy_calls()
rows_a = audit_a()
rows_b = audit_b()

out_a = Path(__file__).resolve().parent / "ambiguity-band-audit.tsv"
out_a.write_text(
    "quest_id\tclient_rows\treward_var0\tlast_start_var0\tverdict\tlegacy_calls\n"
    + "".join("\t".join(str(x) for x in row) + "\n" for row in rows_a), encoding="utf-8")
out_b = Path(__file__).resolve().parent / "counter-residue-audit.tsv"
out_b.write_text(
    "quest_id\tsource\ttarget\tevent\tcounter_fields\tresidue\n"
    + "".join("\t".join(row) + "\n" for row in rows_b), encoding="utf-8")

suspects = [r for r in rows_a if r[4] == "SUSPECT_MISLIFT"]
print(f"[A] family={len(rows_a)} suspects={len(suspects)}")
for row in suspects:
    print("  SUSPECT", row)
for verdict in ("LEGACY_LASTROW", "NEEDS_REVIEW"):
    bucket = [r for r in rows_a if r[4] == verdict]
    print(f"[A] {verdict}={len(bucket)}")
    for row in bucket[:12]:
        print("  ", row)
print(f"[B] counter_residue={len(rows_b)}")
for row in rows_b[:20]:
    print("  RESIDUE", row)
