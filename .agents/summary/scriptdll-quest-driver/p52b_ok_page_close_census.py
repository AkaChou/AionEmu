#!/usr/bin/env python3
"""P5-2b：量化交付成功分支的客户端口径（ok 页本地关闭 → 成功分支应显示领奖窗 5）。

三个证据面对齐：
1. quest_client_handin_pages.tsv（317 行）× quest-dialog-action-details.csv：ok 页(10000)可见按钮类型；
2. 已采纳 240 collect 行（drift ADOPTED ∩ handin）的退役前 XML 交付成功分支实际显示页（git blob）；
3. QuestHandoverContinuationAuditTest.REPAIRED_BRANCHES 正向锁（续接页 5）。

用法：python3 -B p52b_ok_page_close_census.py
"""
import os
import csv
import subprocess
from collections import Counter
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
DRIFT = REPO / 'src/test/resources/quest/retail-data-driven-drift.tsv'
HANDIN = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_handin_pages.tsv'
ACTIONS = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-action-details.csv'
XML_REL = 'src/main/resources/aion/data/static_data/quest/definitions/quests'


def adopted_ids():
    ids = set()
    for line in DRIFT.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) >= 2 and parts[1] == 'ADOPTED':
            ids.add(int(parts[0]))
    return ids


def handin_rows():
    rows = {}
    for line in HANDIN.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        rows[int(parts[0])] = [int(p) for p in parts[1:6]]
    return rows


def ok_actions():
    """quest -> ok 页(10000)可见按钮集合。 / quest -> visible buttons on the ok page (10000)."""
    result = {}
    with ACTIONS.open(encoding='utf-8-sig') as handle:
        for row in csv.DictReader(handle):
            quest = row.get('quest_id')
            page = row.get('page_id')
            action = row.get('action_id')
            if quest and page and action and page.strip() == '10000':
                result.setdefault(int(quest), set()).add(int(action))
    return result


def legacy_success_page(quest_id):
    """退役前 XML 交付成功分支（priority=0 / 带 has-item）显示的页。 / Legacy success-branch page."""
    rel = f'{XML_REL}/{quest_id}.xml'
    blob = subprocess.run(['git', '-C', str(REPO), 'show', f'HEAD:{rel}'],
                          capture_output=True, text=True)
    if blob.returncode != 0:
        return 'NO_XML'
    import re
    text = blob.stdout
    blocks = re.findall(r'<transition [^>]*priority="0"[^>]*>.*?</transition>', text, re.S)
    if not blocks:
        return 'NO_PRI0'
    pages = re.findall(r'<dialog type="SHOW_QUEST_PAGE" page="([A-Z0-9_]+)"/>', blocks[0])
    return pages[0] if pages else 'NO_PAGE'


def main():
    adopted = adopted_ids()
    handin = handin_rows()
    actions = ok_actions()
    detail = []
    ok_button_classes = Counter()
    legacy_pages = Counter()
    both = []
    for quest_id, pages in sorted(handin.items()):
        acts = actions.get(quest_id, set())
        if not acts:
            ok_class = 'OK_PAGE_NO_ACTIONS'
        elif acts <= {1008}:
            ok_class = 'LOCAL_CLOSE'
        elif 1009 in acts:
            ok_class = 'HAS_1009'
        else:
            ok_class = 'OTHER:' + ','.join(map(str, sorted(acts)))
        ok_button_classes[ok_class] += 1
        in_scope = quest_id in adopted
        legacy = legacy_success_page(quest_id) if in_scope else '-'
        if in_scope:
            legacy_pages[legacy] += 1
            both.append((quest_id, ok_class, legacy))
        detail.append(f'{quest_id}\tok={ok_class}\tadopted={in_scope}\tlegacy_success={legacy}')
    print('== ok 页按钮类型分布（317 handin）==')
    for kind, count in ok_button_classes.most_common():
        print(f'{count}\t{kind}')
    print('== 已采纳∩handin 的退役前 XML 成功分支页分布 ==')
    for kind, count in legacy_pages.most_common():
        print(f'{count}\t{kind}')
    print('== 逐行（adopted only）==')
    for quest_id, ok_class, legacy in both:
        print(f'{quest_id}\t{ok_class}\t{legacy}')
    out = REPO / '.agents/summary/scriptdll-quest-driver/p52b-ok-page-close-census.tsv'
    out.write_text('# quest_id\tok_class\tadopted\tlegacy_success\n' + '\n'.join(detail) + '\n',
                   encoding='utf-8')
    print(f'written={out}')


if __name__ == '__main__':
    main()
