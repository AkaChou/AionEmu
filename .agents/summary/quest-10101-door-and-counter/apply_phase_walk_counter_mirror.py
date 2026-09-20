#!/usr/bin/env python3
"""把“阶段逐步行走”的击杀计数镜像到客户端 quest_summary 的计数槽。

Mirrors the per-kill journal phase walk into the client quest_summary counter slot for quests
whose kill route only advanced var0 while the client renders a ([%3k+2]/N) counter: the client
reads the numerator from SECTION_k, so the server must write it in the same transaction.

Dry run by default; pass --apply to write.
"""
import argparse
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

DIALOG_ROOTS = glob.glob("/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs/*")
QUEST_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"
# quest id -> (kill phase walk already verified; 24011 is excluded: 6 kill transitions vs client /5)
QUESTS = [14011, 14014, 14021, 14022, 24013, 24014, 24015]
TRANSITION_RE = re.compile(r"<transition\b.*?</transition>", re.S)
COUNTER_RE = re.compile(r"\(\[%(\d+)\]\s*/\s*(\d+)\)")


def client_counter(quest_id):
    for root in DIALOG_ROOTS:
        for path in glob.glob(f"{root}/quest_q{quest_id}.html"):
            text = open(path, encoding="utf-8-sig", errors="replace").read()
            block = re.search(r'name="quest_summary"(.*?)</HtmlPage>', text, re.S)
            counters = COUNTER_RE.findall(block.group(1)) if block else []
            if counters:
                return len(counters), [(int(n), int(m)) for n, m in counters]
    return 0, []


def kill_transitions(text):
    out = []
    for match in TRANSITION_RE.finditer(text):
        block = match.group(0)
        if "<kill-npc" not in block and "<kill-npc-set" not in block:
            continue
        source = re.search(r'<transition source="([^"]+)"', block).group(1)
        target = re.search(r'target="([^"]+)"', block).group(1)
        out.append((source, target, match.start(), match.end(), block))
    return out


def patch_quest(quest_id, apply):
    path = f"{QUEST_DIR}/{quest_id}.xml"
    text = open(path, encoding="utf-8").read()
    count, counters = client_counter(quest_id)
    if count != 1:
        sys.exit(f"{quest_id}: expected exactly one client counter, found {counters}")
    slot, required = counters[0][0], counters[0][1]
    if slot % 3 != 2:
        sys.exit(f"{quest_id}: placeholder {slot} is not a value slot")
    section = (slot - 2) // 3

    kills = kill_transitions(text)
    if len(kills) != required:
        sys.exit(f"{quest_id}: {len(kills)} kill transitions vs client /{required}")
    walk = [(s, t) for s, t, *_ in kills]
    for (_, first_target), (second_source, _) in zip(walk, walk[1:]):
        if first_target != second_source:
            sys.exit(f"{quest_id}: kill route is not a chain: {walk}")

    field = f"var{section}"
    if f'name="{field}"' in text:
        sys.exit(f"{quest_id}: {field} already declared")

    var0_line = re.search(r'^([ \t]*)<bit-field name="var0"[^>]*/>\n', text, re.M)
    if var0_line is None:
        sys.exit(f"{quest_id}: no var0 bit-field")
    indent = var0_line.group(1)
    lex = f"var{section}"
    comment = (
        f"{indent}<!-- {lex}：客户端 quest_summary 第 {section + 1} 行计数槽（[%{slot}]/{required}）。\n"
        f"{indent}     阶段仍在 var0 逐步行走，客户端分子读 SECTION_{section}（offset {6 * section}）。\n"
        f"{indent}     {lex}: client quest_summary counter slot ([%{slot}]/{required}); the journal stage\n"
        f"{indent}     still walks var0 per kill and the client numerator reads SECTION_{section}. -->\n"
    )
    field_line = (
        f'{indent}<bit-field name="{field}" offset="{6 * section}" width="6" min="0" '
        f'max="{required}" persistence="PERSISTENT" scope="LOCAL"/>\n'
    )
    text = text[: var0_line.end()] + comment + field_line + text[var0_line.end():]

    # 重新扫描：插入 progress 字段后，之前记录的 transition 偏移已失效。
    # Re-scan: the transition offsets recorded before the progress insert are stale.
    kills = kill_transitions(text)
    if len(kills) != required:
        sys.exit(f"{quest_id}: kill transition count changed to {len(kills)} after progress insert")
    edits = 0
    for index in reversed(range(len(kills))):
        counter = index + 1
        new_block = add_counter_action(kills[index][4], field, counter, quest_id)
        text = text[: kills[index][2]] + new_block + text[kills[index][3]:]
        edits += 1
    if apply:
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(text)
    print(f"quest {quest_id}: {field} @ offset {6 * section} max {required}, "
          f"kill actions -> {list(range(1, required + 1))} ({'applied' if apply else 'dry-run'})")
    return edits


def add_counter_action(block, field, counter, quest_id):
    action = f'        <set-variable field="{field}" value="{counter}"/>\n'
    if f'field="{field}"' in block:
        return block
    if "<actions>" in block:
        var0 = re.search(r'^        <set-variable field="var0"[^>]*/>\n', block, re.M)
        if var0 is None:
            return block.replace("      <actions>\n", "      <actions>\n" + action, 1)
        return block.replace(var0.group(0), var0.group(0) + action, 1)
    anchor = re.search(r"^      </conditions>\n", block, re.M)
    if anchor is None:
        anchor = re.search(r"^      <after-commit>\n", block, re.M)
    if anchor is None:
        sys.exit(f"quest {quest_id}: kill transition without conditions and actions")
    if anchor.group(0).lstrip().startswith("</"):
        # </conditions>：在条件块之后插入 / insert after the closing conditions block
        return block[: anchor.end()] + "      <actions>\n" + action + "      </actions>\n" + block[anchor.end():]
    # <after-commit>：在提交块之前插入 / insert before the after-commit block
    return block[: anchor.start()] + "      <actions>\n" + action + "      </actions>\n" + block[anchor.start():]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    for quest_id in QUESTS:
        patch_quest(quest_id, args.apply)


if __name__ == "__main__":
    main()
