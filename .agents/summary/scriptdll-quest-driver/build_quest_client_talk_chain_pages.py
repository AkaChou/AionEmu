#!/usr/bin/env python3
"""P5-3 wave B：DataDriven Talk 链行的「链式信件」登记表（按钮图推导版）。

每阶段的页面梯与推进动作从 quest-dialog-action-details.csv 的实际按钮图导出：
从 select{i} 出发，按钮动作 id 若等于本任务某页 id → 导航边（入梯）；否则（SETPRO*/SET_SUCCEED/1009
等推进动作）→ 记为该阶段推进动作。行格式：quest_id \t entry_page \t 阶段1 \t 阶段2 ...
每阶段列 = `p1>p2>...:advance`（梯 id 用 > 串联，冒号后为推进动作 id）。

用法：python3 -B build_quest_client_talk_chain_pages.py
"""
import csv
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
MAPPING = REPO / 'docs/quest/client-dialog-mapping'
PAGES = MAPPING / 'quest-dialog-pages.csv'
ACTIONS = MAPPING / 'quest-dialog-action-details.csv'
HANDIN = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_handin_pages.tsv'
TALK = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_pages.tsv'
CURATED_DEFERRED = Path(__file__).resolve().parent / 'p52-handin-deferred-quests.tsv'
OUT = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_pages.tsv'

EXCLUDED_PAGES = {'select_none_1', 'check_user_item_ok', 'check_user_item_fail', 'ask_quest_accept'}
ADVANCE_IDS = set(range(10000, 10012)) | {10255, 1009}


def main():
    by_quest = {}
    with PAGES.open(encoding='utf-8-sig') as handle:
        for row in csv.DictReader(handle):
            by_quest.setdefault(int(row['quest_id']), {})[row['html_page_name']] = int(row['page_id'])
    buttons = {}
    with ACTIONS.open(encoding='utf-8-sig') as handle:
        for row in csv.DictReader(handle):
            if not row.get('action_id', '').strip():
                continue
            buttons.setdefault(int(row['quest_id']), {}).setdefault(
                int(row['page_id']), []).append(int(row['action_id']))
    claimed = set()
    for registry in (HANDIN, TALK):
        for line in registry.read_text(encoding='utf-8').splitlines():
            if line.strip() and not line.startswith('#'):
                claimed.add(int(line.split('\t')[0]))
    if CURATED_DEFERRED.is_file():
        for line in CURATED_DEFERRED.read_text(encoding='utf-8').splitlines():
            if line.strip() and not line.startswith('#'):
                claimed.add(int(line.split('\t')[0]))
    page_names = {}
    for quest_id, names in by_quest.items():
        page_names[quest_id] = {page_id: name for name, page_id in names.items()}
    rows = []
    for quest_id in sorted(by_quest):
        if quest_id in claimed:
            continue
        names = by_quest[quest_id]
        if 'select_none' not in names or 'select1' not in names:
            continue
        if set(names) & EXCLUDED_PAGES:
            continue
        quest_buttons = buttons.get(quest_id, {})
        stage_columns = []
        for index in range(1, 13):
            base = names.get(f'select{index}')
            if base is None:
                break
            ladder, advance, seen = [base], None, {base}
            cursor = base
            while True:
                page_buttons = quest_buttons.get(cursor, [])
                # 推进动作优先：SETPRO*/SET_SUCCEED/1009 与模板页 id 有重叠（如 SETPRO3=10002=
                # DEFAULT_SUCCESS），先按推进语义归类，剩下的才是页面导航。
                # Advance actions win: they collide with template page ids, so classify them first.
                navs = [a for a in page_buttons
                        if a not in ADVANCE_IDS and a in page_names.get(quest_id, {})
                        and a not in seen]
                advances = [a for a in page_buttons if a in ADVANCE_IDS]
                if navs:
                    nxt = navs[0]
                    ladder.append(nxt)
                    seen.add(nxt)
                    cursor = nxt
                    continue
                if advances:
                    advance = advances[0]
                break
            if advance is None:
                stage_columns = []
                break
            stage_columns.append('>'.join(str(p) for p in ladder) + ':' + str(advance))
        if not stage_columns:
            continue
        rows.append(f'{quest_id}\t{names["select_none"]}\t' + '\t'.join(stage_columns))
    target = OUT
    target.write_text('\n'.join([
        '# 客户端链式信件登记（quest_id, entry_page, 阶段1, 阶段2, ...）',
        '# 每阶段列 = `p1>p2>...:advance`：页面梯从按钮图导出（按钮动作 id = 本任务页 id → 导航边；'
        'SETPRO*/SET_SUCCEED/1009 → 推进动作），梯尾冒号后为推进动作 id',
        '# 来源：quest-dialog-pages.csv + quest-dialog-action-details.csv',
        '# 生成：python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_pages.py',
        '# 语义：DataDriven Talk 链行的接取入口页与逐阶段页面梯 + 推进动作；'
        '已在 handin / talk（无进度）登记的任务不进本表',
    ] + rows) + '\n', encoding='utf-8')
    print(f'{OUT.name}: {len(rows)} rows')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
