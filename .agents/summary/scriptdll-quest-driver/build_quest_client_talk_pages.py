#!/usr/bin/env python3
"""P5-3 wave A：DataDriven 无进度行的「极简对话信件」登记表。

签名（三者同时成立才登记）：
  - select_none（接取窗）与 select_success（成功收尾页）齐备；
  - select_acqusitive_quest_desc 在场（DD 信件模板标记）；
  - 不含 select1 / select2 / ask_quest_accept / check_user_item_ok / TalkFOBJ 系页面
    （这些是规范接取/交付检查/链式词汇，走别的形状）。

输出列：quest_id, entry_page(select_none), completion_page(select_success)。
已登记 handin 的任务跳过（交付流优先）。
用法：python3 -B build_quest_client_talk_pages.py
"""
import csv
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
MAPPING = REPO / 'docs/quest/client-dialog-mapping'
PAGES = MAPPING / 'quest-dialog-pages.csv'
HANDIN = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_handin_pages.tsv'
OUT = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_pages.tsv'

EXCLUDED_PAGES = {'select1', 'select2', 'select3', 'ask_quest_accept', 'check_user_item_ok',
                  'check_user_item_fail'}
REQUIRED_PAGES = {'select_none', 'select_success', 'select_acqusitive_quest_desc'}


def main():
    by_quest = {}
    with PAGES.open(encoding='utf-8-sig') as handle:
        for row in csv.DictReader(handle):
            by_quest.setdefault(int(row['quest_id']), {})[row['html_page_name']] = int(row['page_id'])
    handin = {int(line.split('\t')[0]) for line in HANDIN.read_text(encoding='utf-8').splitlines()
              if line.strip() and not line.startswith('#')}
    rows = []
    for quest_id in sorted(by_quest):
        if quest_id in handin:
            continue
        names = by_quest[quest_id]
        if not REQUIRED_PAGES <= set(names):
            continue
        if set(names) & EXCLUDED_PAGES:
            continue
        rows.append(f"{quest_id}\t{names['select_none']}\t{names['select_success']}")
    target = OUT
    target.write_text('\n'.join([
        '# 客户端极简对话信件登记（quest_id, entry_page=select_none, completion_page=select_success）',
        '# 来源：docs/quest/client-dialog-mapping/quest-dialog-pages.csv（签名 = select_none + select_success + '
        'select_acqusitive_quest_desc 且不含 select1/select2/ask_quest_accept/check_user_item_*）',
        '# 生成：python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_talk_pages.py',
        '# 语义：DataDriven 无进度行（Talk 接取 → 交付 NPC 对话即完成）的接取入口页与完成收尾页；'
        '已在 handin 登记的任务走交付流，不进本表',
    ] + rows) + '\n', encoding='utf-8')
    print(f'{OUT.name}: {len(rows)} rows')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
