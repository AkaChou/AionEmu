#!/usr/bin/env python3
"""把 10101.xml 回退到 HEAD 的阶段序列后，只重新施加“计数镜像到客户端槽”修复。

Reverts 10101.xml's speculative journal-row rewrite (kill phase must keep walking 2->3->4 per
kill, matching the retail client Progress(2~!4) contract) and re-applies only the client counter
mirror fix (var2 @ SECTION_2 = offset 12, written 1/2 on each kill, zeroed on the instance
rollback edges).
"""
import re
import subprocess
import sys

PATH = "src/main/resources/aion/data/static_data/quest_definition/quests/10101.xml"
HEAD = "HEAD:src/main/resources/aion/data/static_data/quest_definition/quests/10101.xml"

COMMENT = (
    "    <!-- var2：卫兵击杀计数，客户端 quest_summary 第三行读取 SECTION_2（[%8]/2）；\n"
    "         阶段仍在 var0 行走 2->3->4（客户端 Progress(2~!4)）。\n"
    "         var2: scout kill counter; the client quest_summary third row reads SECTION_2 ([%8]/2);\n"
    "         the stage still walks var0 2->3->4 (client Progress(2~!4)). -->\n"
)

BITFIELD_VAR0 = (
    '    <bit-field name="var0" offset="0" width="4" min="0" max="9" '
    'persistence="PERSISTENT" scope="LOCAL"/>\n'
)
BITFIELD_VAR2 = (
    '    <bit-field name="var2" offset="12" width="6" min="0" max="2" '
    'persistence="PERSISTENT" scope="LOCAL"/>\n'
)

TRANSITION_RE = re.compile(r"<transition\b.*?</transition>", re.S)


def main():
    text = subprocess.run(["git", "show", HEAD], check=True, capture_output=True).stdout.decode()
    if BITFIELD_VAR0 not in text:
        sys.exit("HEAD progress block changed; refusing to patch")

    text = text.replace(BITFIELD_VAR0, BITFIELD_VAR0 + COMMENT + BITFIELD_VAR2, 1)

    stats = {"kill": 0, "rollback": 0}

    def patch(match):
        block = match.group(0)
        source = re.search(r'<transition source="([^"]+)"', block).group(1)
        target = re.search(r'target="([^"]+)"', block).group(1)
        if '<kill-npc npc-id="234680"/>' in block:
            phase = re.search(r'<variable-is field="var0" value="(\d+)"/>', block).group(1)
            counter = {"2": 1, "3": 2}.get(phase)
            if counter is None:
                sys.exit(f"unexpected kill phase {phase}")
            action = re.search(r'        <set-variable field="var0" value="\d+"/>\n', block)
            if action is None:
                sys.exit(f"kill block for phase {phase} has no var0 action")
            old = action.group(0)
            stats["kill"] += 1
            return match.group(0).replace(
                old, old + f'        <set-variable field="var2" value="{counter}"/>\n', 1)
        if source in {"s2", "s3", "s4"} and target == "s1":
            old = '        <set-variable field="var0" value="1"/>\n'
            if old not in block:
                sys.exit(f"rollback block {source}->{target} has no var0 action")
            if 'field="var2"' in block:
                return block
            stats["rollback"] += 1
            return block.replace(
                old, old + '        <set-variable field="var2" value="0"/>\n', 1)
        return block

    text = TRANSITION_RE.sub(patch, text)
    if stats != {"kill": 2, "rollback": 9}:
        sys.exit(f"unexpected patch stats {stats}")

    with open(PATH, "w", encoding="utf-8") as fh:
        fh.write(text)
    print("patched", PATH, stats)


if __name__ == "__main__":
    main()
