#!/usr/bin/env python3
"""切片 2（talk>collectitem 混合链）：链式信件 + 采集检查段登记表（按钮图推导版）。

数据源：真端 data_driven_quest.xml 的步骤类别 + quest-dialog-pages.csv / quest-dialog-action-details.csv
的实际按钮图。只有 DD 步骤类别恰为 {talk, collectitem} 混合（两者皆有、无其他）的任务进入本表，
且客户端 select{i} 阶段序列的类别（推进动作 ∈ {39,20002} = collect 段，SETPRO*/SET_SUCCEED/1009
= talk 段）必须与 DD 步骤序列逐位一致——登记即证据对齐，不做宽松匹配。

行格式：quest_id \t entry_page \t 阶段1 \t 阶段2 ...
每阶段列 = `kind:p1>p2>...:advance` 或 `collect:p:advance:ok_page:fail_page`
（talk 段梯从按钮图导航边导出；collect 段梯只有段首页 + 两个检查结果页）。

用法：python3 -B build_quest_client_talk_collect_chain_pages.py
"""
import csv
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
MAPPING = REPO / 'docs/quest/client-dialog-mapping'
PAGES = MAPPING / 'quest-dialog-pages.csv'
ACTIONS = MAPPING / 'quest-dialog-action-details.csv'
DD_XML = REPO / 'src/main/resources/aion/data/static_data/quest_retail/data_driven_quest.xml'
HANDIN = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_handin_pages.tsv'
TALK = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_pages.tsv'
CHAIN = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_pages.tsv'
OUT = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_collect_chain_pages.tsv'

COLLECT_ADVANCE_IDS = {39, 20002}
TALK_ADVANCE_IDS = set(range(10000, 10012)) | {10255, 1009}
RESULT_PAGES = {'check_user_item_ok', 'check_user_item_fail'}


def dd_step_categories():
    """DD 表逐任务步骤类别（小写）+ 接取类别/参数；留 talk+collectitem(+hunt) 混合且两者皆有的行。

    hunt 步没有客户端 select 页（任务书计数行不占对话段）：含 hunt 的行按「非 hunt 步」
    与客户端段序列对齐；hunt 步自身由真端表 huntBlocks 承载，不进登记表。
    Hunt steps own no client select page (journal counting rows are not dialog stages): rows
    carrying hunt align their NON-hunt steps against the client stage sequence; the hunt shape
    itself lives in the retail table's hunt blocks, not in this registry.
    """
    raw = DD_XML.read_bytes()
    enc = 'utf-16' if raw[:2] in (b'\xff\xfe', b'\xfe\xff') else 'utf-8'
    root = ET.fromstring(raw.decode(enc, errors='ignore').encode('utf-8'))
    out = {}
    for element in root.iter('quest_data_driven'):
        quest_id = int(element.findtext('id'))
        categories = []
        for data in element.iter('data'):
            category = data.findtext('category_progress_')
            if category is not None:
                categories.append(category.strip().lower())
        kinds = set(categories)
        # enterarea 步占行不占对话段（enterarea 交织片）：范围放行含 EA 的混合行，段序列按
        # 「非 hunt 且非 enterarea 步」对齐。
        # enterarea steps occupy a row without a dialog stage (the enterarea interleave slice):
        # EA-carrying mixed rows register too, aligned on non-hunt non-enterarea steps.
        has_ea = 'enterarea' in kinds
        # itemplay 步占行不占对话段（itemplay 切片）：范围放行含 ItemPlay 的混合行，段序列按
        # 「非 hunt 且非 enterarea 且非 itemplay 步」对齐。
        # ItemPlay steps occupy a row without a dialog stage (the itemplay slice): ItemPlay-
        # carrying mixed rows register too, aligned on non-hunt non-enterarea non-itemplay steps.
        # pvp 计数步同理占行不占对话段（续片 22：80846 形 pvp+collectitem）：放行含 PVP 的混合行，
        # 段序列按非计数段（hunt/enterarea/itemplay/pvp）对齐。资格句维持「有对话段（talk/hunt）
        # 或 PVP+collectitem 组合」——纯 collectitem 行（采集族）由 HANDIN 登记表覆盖，不在此列。
        # PVP counter steps likewise occupy a row without a dialog stage (slice 22, the 80846
        # pvp+collectitem shape): PVP-carrying mixed rows register too, aligned on the non-counter
        # steps (hunt/enterarea/itemplay/pvp). The eligibility clause stays "has a dialog stage
        # (talk/hunt) or the pvp+collectitem pairing" — pure collectitem rows belong to the collect
        # family's HANDIN registry instead.
        if kinds <= {'talk', 'collectitem', 'hunt', 'enterarea', 'itemplay', 'pvp'} \
                and ('talk' in kinds or 'hunt' in kinds
                     or ('pvp' in kinds and 'collectitem' in kinds)) \
                and ('collectitem' in kinds or has_ea or 'itemplay' in kinds or 'pvp' in kinds):
            acquire = (element.findtext('category_acquire_') or '').strip().lower()
            acquire_val = (element.findtext('value0_acquire_') or '').strip()
            out[quest_id] = (['collect' if c == 'collectitem' else c for c in categories],
                             acquire, acquire_val)
    return out


def main():
    dd = dd_step_categories()
    by_quest = {}
    source_files = {}
    with PAGES.open(encoding='utf-8-sig') as handle:
        for row in csv.DictReader(handle):
            by_quest.setdefault(int(row['quest_id']), {})[row['html_page_name']] = int(row['page_id'])
            source_files[int(row['quest_id'])] = row['source_file']
    buttons = {}
    with ACTIONS.open(encoding='utf-8-sig') as handle:
        for row in csv.DictReader(handle):
            if not row.get('action_id', '').strip():
                continue
            buttons.setdefault(int(row['quest_id']), {}).setdefault(
                int(row['page_id']), []).append(int(row['action_id']))
    claimed = set()
    for registry in (HANDIN, TALK, CHAIN):
        for line in registry.read_text(encoding='utf-8').splitlines():
            if line.strip() and not line.startswith('#'):
                claimed.add(int(line.split('\t')[0]))
    # 客户端 HTML 的过场声明（页名 → movie id）；源文件在 unpacked Dialogs 目录下。
    # CutScene declarations in the client HTML (page name -> movie id); the sources live in the
    # unpacked Dialogs directory.
    html_root = Path('/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs')
    cutscenes = {}
    page_names = {}
    for quest_id, file_rel in source_files.items():
        html = html_root / file_rel
        if not html.is_file():
            continue
        text = html.read_text(encoding='utf-8', errors='ignore')
        page_titles = {}
        for page_match in re.finditer(r'<HtmlPage name="([^"]+)">((?:(?!</HtmlPage>).)*)', text, re.S):
            page_titles[by_quest.get(quest_id, {}).get(page_match.group(1))] = page_match.group(1)
            scene = re.search(r'<CutScene id="(\d+)"', page_match.group(2))
            if scene:
                cutscenes.setdefault(quest_id, {})[page_match.group(1)] = scene.group(1)
        page_names[quest_id] = {page_id: name for name, page_id in by_quest.get(quest_id, {}).items()}
    rows = []
    skipped = {'not_dd_mix': 0, 'claimed': 0, 'no_pages': 0, 'stage_mismatch': 0, 'multi_advance': 0}
    for quest_id, wanted in sorted(dd.items()):
        if quest_id in claimed:
            skipped['claimed'] += 1
            continue
        names = by_quest.get(quest_id)
        wanted_kinds, acquire_cat, _acquire_val = wanted
        wanted = wanted_kinds
        # EnterWorld/EnterArea/None（链式）接取行没有 select_none 接取页（任务由进世界/进区域/
        # 前序完成等事件发放，简报在步骤 1 的 talk NPC）：entry_page=0，其余页照常登记。
        # EnterWorld, EnterArea and none (chain) acquires have no select_none acquire page (the quest
        # is granted by world-entry, zone-entry or the prerequisite completion event and the briefing
        # lives on the step-1 talk npc): entry_page=0, the remaining pages register as usual.
        entry_page = 0
        if names and 'select_none' in names:
            entry_page = names['select_none']
        elif not (names and acquire_cat in ('enterworld', 'enterarea', 'none')):
            # 其余接取形必须有 select_none 接取页（NPC 接取行的简报页）。
            # Every other acquire form must carry the select_none briefing page (the npc-acquired rows).
            skipped['no_pages'] += 1
            continue
        if not names or ('collect' in wanted and not RESULT_PAGES <= set(names)):
            # 结果页仅 collect 行必需；talk/hunt(/EA) 行没有 39 检查页（16823 形）。
            # Result pages are required only for collect rows; talk/hunt(/EA) rows have no 39
            # check pages (the 16823 shape).
            skipped['no_pages'] += 1
            continue
        quest_buttons = buttons.get(quest_id, {})
        stages = []
        # 段首页 = 任务实际存在的 select{i} 页（客户端可跳号：15304 无 select3，末段是 select4）。
        # Stage heads = the select{i} pages that actually exist (the client may skip numbers).
        stage_heads = sorted(
            (int(m.group(1)), page_id) for page_name, page_id in names.items()
            if (m := re.fullmatch(r'select(\d+)', page_name)))
        for _, base in stage_heads:
            page_buttons = quest_buttons.get(base, [])
            collect_advance = [a for a in page_buttons if a in COLLECT_ADVANCE_IDS]
            if collect_advance:
                # collect 段不终止链：段梯 = 段首页 + 首页上的信息导航页（扇出，如 select2_1..3，
                # 排除检查/推进/收尾按钮）；ok 页导航进下一段，段序列继续。
                # A collect stage does not end the chain: its ladder = the head page plus the
                # information navs fanning out of the head (e.g. select2_1..3, excluding
                # check/advance/finish buttons); the ok page navigates into the next stage.
                # FINISH_DIALOG(1008) 是收尾按钮也是 quest_complete 的页 id，导航扇出不收。
                navs = [a for a in page_buttons
                        if a not in COLLECT_ADVANCE_IDS and a not in TALK_ADVANCE_IDS
                        and a in names.values() and a != 1008]
                seen_navs = {base}
                ordered = [base]
                for nav in navs:
                    if nav not in seen_navs:
                        seen_navs.add(nav)
                        ordered.append(nav)
                stages.append(('collect', ordered, collect_advance[0],
                               (names['check_user_item_ok'], names['check_user_item_fail']), ''))
                continue
            # talk 段：整段页梯 = select{i} 出发的导航**图**（按钮 id = 页 id，非推进动作），
            # 推进动作 = 段内唯一的推进按钮（15301 形：select1>select1_1>select1_1_1>select1_1_1_1:SETPRO1）。
            # 按 BFS 收集整个段的导航分量（分支页不得被"取第一个导航"静默线性化——10505 的
            # select4_…>2205 有三个分支 2036/2057/2078，线性化会让 2078/2042/2047 成死按钮）；
            # 段内推进动作出现两个不同 id 时形状二义，如实跳过（stage_mismatch 计数与码位同族）。
            # Talk stage: the ladder is the nav *graph* reachable from select{i} (button id = page id,
            # not the advance action) and the advance is the single advance button of the stage. The
            # whole nav component is collected breadth-first so a branching page is never silently
            # linearised (10505's 2205 fans out to 2036/2057/2078; taking the first nav alone leaves
            # 2078/2042/2047 as dead buttons). Two distinct advance ids inside one stage are ambiguous
            # and skip the row honestly.
            ladder, seen, queue, advance = [base], {base}, [base], []
            while queue:
                cursor = queue.pop(0)
                cursor_buttons = quest_buttons.get(cursor, [])
                advance = [a for a in cursor_buttons if a in TALK_ADVANCE_IDS]
                if advance:
                    break
                for nav in cursor_buttons:
                    if nav in COLLECT_ADVANCE_IDS or nav in TALK_ADVANCE_IDS:
                        continue
                    if nav not in names.values() or nav in seen:
                        continue
                    seen.add(nav)
                    ladder.append(nav)
                    queue.append(nav)
            if not advance:
                break
            if len(advance) > 1:
                skipped['multi_advance'] += 1
                break
            # 梯尾页的 <CutScene id="N"/>：推进动作要附带过场（16942 形：select1_1 的 899）。
            # The tail page's <CutScene id="N"/>: the advance action carries the movie.
            tail_name = page_names.get(quest_id, {}).get(cursor, '')
            movie = cutscenes.get(quest_id, {}).get(tail_name, '')
            stages.append(('talk', ladder, advance[0], None, movie))
        got = [stage[0] for stage in stages]
        wanted_visible = [c for c in wanted if c not in ('hunt', 'enterarea', 'itemplay', 'pvp')]
        if got != wanted_visible:
            skipped['stage_mismatch'] += 1
            continue
        columns = []
        for kind, ladder, advance, result_pages, movie in stages:
            if kind == 'collect':
                columns.append('collect:' + ' > '.join(str(p) for p in ladder)
                               + f':{advance}:{result_pages[0]}:{result_pages[1]}')
            else:
                column = f'talk:{" > ".join(str(p) for p in ladder)}:{advance}'
                if movie:
                    column += f':{movie}'
                columns.append(column)
        rows.append(f'{quest_id}\t{entry_page}\t' + '\t'.join(columns))
    target = OUT
    # --out=<path> 覆盖输出（干跑对照用；默认仍写生产登记表）。
    # --out=<path> overrides the output (dry-run comparison; the default still writes the
    # production registry).
    for argument in sys.argv[1:]:
        if argument.startswith('--out='):
            target = Path(argument[len('--out='):])
    target.write_text('\n'.join([
        '# 客户端 talk+collect 混合链登记（quest_id, entry_page, 阶段1, 阶段2 ...）',
        '# 每阶段列 = `talk:p1>p2>...:advance` 或 `collect:p:advance:ok页:fail页`；'
        '# talk 段梯 = select{i} 出发的按钮图导航链，collect 段 = 39/20002 检查 + 结果页；'
        '# 阶段类别序列与真端表 stepCategories 逐位一致（登记即证据对齐）',
        '# 来源：data_driven_quest.xml + quest-dialog-pages.csv + quest-dialog-action-details.csv',
        '# 生成：python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_talk_collect_chain_pages.py',
    ] + rows) + '\n', encoding='utf-8')
    print(f'{OUT.name}: {len(rows)} rows (dd mixes={len(dd)}) skipped={skipped}')
    for row in rows:
        print(' ', row)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
