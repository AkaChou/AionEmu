#!/usr/bin/env python3
"""P5-2b：把锁定形状超出标准交付模板的任务暂缓退役（恢复 XML + 目录回插 + 暂缓登记）。

用法：python3 -B p52b_defer_handin_quests.py 19010:ACCEPT_GRANT_UNEXPRESSED 80745:LEGACY_WINDOW_SHORTCUT ...
幂等：已在暂缓清单/已恢复的任务跳过。
"""
import os
import re
import subprocess
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
PROD_DIR = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quests'
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml'
DEFERRED = Path(__file__).resolve().parent / 'p52-handin-deferred-quests.tsv'
XML_REL = 'src/main/resources/aion/data/static_data/quest_definition/quests'


def deferred_ids():
    if not DEFERRED.is_file():
        return set()
    return {line.split('\t')[0] for line in DEFERRED.read_text(encoding='utf-8').splitlines()
            if line.strip() and not line.startswith('#')}


def main(args):
    if not args:
        print('usage: p52b_defer_handin_quests.py ID:REASON...')
        return 2
    known = deferred_ids()
    entries = []
    for arg in args:
        quest_id, reason = arg.split(':', 1)
        if quest_id in known:
            print(f'{quest_id}: already deferred ({reason})')
            continue
        with DEFERRED.open('a', encoding='utf-8') as handle:
            handle.write(f'{quest_id}\t{reason}\n')
        entries.append((quest_id, reason))
    restored, missing = [], []
    for quest_id, _ in entries:
        target = PROD_DIR / f'{quest_id}.xml'
        rel = f'{XML_REL}/{quest_id}.xml'
        blob = subprocess.run(['git', '-C', str(REPO), 'show', f'HEAD:{rel}'],
                              capture_output=True)
        if blob.returncode != 0:
            missing.append(quest_id)
            continue
        target.write_bytes(blob.stdout)
        restored.append(quest_id)
    if restored:
        text = CATALOG.read_text(encoding='utf-8')
        lines = text.splitlines(keepends=True)
        wanted = {int(q) for q in restored}
        # 删掉可能残留的旧条目，再按 id 序回插。 / Drop stale entries, re-insert in id order.
        lines = [line for line in lines
                 if not (match := re.search(r'<definition id="(\d+)"', line))
                 or int(match.group(1)) not in wanted]
        insertions = sorted(wanted)
        result, cursor = [], 0
        for line in lines:
            match = re.search(r'<definition id="(\d+)"', line)
            if match:
                while cursor < len(insertions) and insertions[cursor] < int(match.group(1)):
                    result.append(f'  <definition id="{insertions[cursor]}" '
                        f'resource="aion/data/static_data/quest_definition/quests/'
                        f'{insertions[cursor]}.xml" mode="EXECUTABLE" />\n')
                    cursor += 1
            result.append(line)
        while cursor < len(insertions):
            result.append(f'  <definition id="{insertions[cursor]}" '
                f'resource="aion/data/static_data/quest_definition/quests/'
                f'{insertions[cursor]}.xml" mode="EXECUTABLE" />\n')
            cursor += 1
        CATALOG.write_text(''.join(result), encoding='utf-8')
    print(f'deferred={len(entries)} restored={len(restored)} missing={missing}')
    return 0 if not missing else 1


if __name__ == '__main__':
    raise SystemExit(main(sys.argv[1:]))
