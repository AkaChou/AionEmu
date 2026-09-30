#!/usr/bin/env python3
"""P5-3 形状判别普查：纯 talk 候选行（Talk+NO_PROGRESS / Talk+talk / none+talk 等）×
客户端页面名集合 × 真端 NPC 名可解析性 × handin 登记重叠。

用法：python3 -B p53_talk_shape_census.py
"""
import os
import csv
import re
import subprocess
from collections import Counter
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/data_driven_quest.xml'
PAGES_CSV = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-pages.csv'
NPCS = sorted(REPO.glob('src/main/resources/aion/data/static_data/npcs/npc_template_*.xml'))
HANDIN = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_handin_pages.tsv'
SUMMARY = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv'
OUT = Path(__file__).resolve().parent / 'p53-talk-shape-census.tsv'

# P5-3 候选组合（纯 talk 链；TalkFOBJ 与混合步骤排除）
CANDIDATE_COMBOS = {'NO_PROGRESS|Talk', 'talk|Talk', 'talk|none'}


def load_npc_names():
    names = set()
    for path in NPCS:
        for match in re.finditer(r'name_desc="([^"]+)"', path.read_text(encoding='utf-8',
                errors='ignore')):
            names.add(match.group(1).strip())
    return names


def main():
    # 候选行：接取×组合
    candidates = {}
    for line in (Path(__file__).resolve().parent / 'p53-step-unsupported-census.tsv').read_text(
            encoding='utf-8').splitlines():
        if line.startswith('#'):
            continue
        quest_id, acquire, combo, values = line.split('\t')
        if f'{combo}|{acquire}' in CANDIDATE_COMBOS:
            # census 行是 acquire\tcombo；这里反查原始表拿 acquire/reward/步骤 npc
            candidates[int(quest_id)] = (acquire, combo, values)
    # 原始表行
    rows = {}
    for block in TABLE.read_text(encoding='utf-8').split('<quest_data_driven>')[1:]:
        id_match = re.search(r'<id>(\d+)</id>', block)
        if not id_match or int(id_match.group(1)) not in candidates:
            continue
        quest_id = int(id_match.group(1))
        acquire_npc = re.search(r'<value0_acquire_>([^<]*)</value0_acquire_>', block)
        reward = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', block)
        steps = re.findall(r'<category_progress_>([^<]*)</category_progress_>\s*'
                           r'(?:<value0_progress_>([^<]*)</value0_progress_>)?', block)
        rows[quest_id] = (acquire_npc.group(1).strip() if acquire_npc else '',
                          reward.group(1).strip() if reward else '',
                          [(c.strip(), (v or '').strip()) for c, v in steps])
    # 客户端页面名
    pages = {}
    with PAGES_CSV.open(encoding='utf-8-sig') as handle:
        for row in csv.DictReader(handle):
            pages.setdefault(int(row['quest_id']), set()).add(row['html_page_name'])
    handin = {int(line.split('\t')[0]) for line in HANDIN.read_text(encoding='utf-8').splitlines()
              if line.strip() and not line.startswith('#')}
    summary = {int(line.split('\t')[0]) for line in SUMMARY.read_text(encoding='utf-8').splitlines()
               if line.strip() and not line.startswith('#')}
    # NPC 名（name_desc 精确匹配语义）
    names = load_npc_names()

    def resolvable(name):
        if not name:
            return 'EMPTY'
        return 'OK' if name in names else 'MISSING'

    detail, shape_count = [], Counter()
    for quest_id in sorted(rows):
        acquire, reward, steps = rows[quest_id]
        pset = pages.get(quest_id, set())
        page_label = ','.join(sorted(pset)) if pset else 'NO_CLIENT_HTML'
        # 形状标签：以页面名为主的粗分类
        if 'select2' in pset:
            client_shape = 'CANONICAL_SELECT2'
        elif 'check_user_item_ok' in pset:
            client_shape = 'HANDIN_VOCAB'
        elif pset:
            client_shape = 'OTHER:' + page_label[:60]
        else:
            client_shape = 'NO_CLIENT_HTML'
        npc_state = f'{resolvable(acquire)}/{resolvable(reward)}'
        step_npcs = ';'.join(v for c, v in steps)
        step_resolve = 'OK' if all(npc in names for _, v in steps for npc in
                                   [s.strip() for s in v.split(',') if s.strip()]) else 'STEP_MISSING'
        shape_count[(client_shape, npc_state, step_resolve,
                     'HANDIN' if quest_id in handin else '-',
                     'SUM' if quest_id in summary else 'NOSUM')] += 1
        detail.append(f'{quest_id}\t{client_shape}\tnpc={npc_state}\tsteps={step_resolve}'
                      f'\t{step_npcs}\tinHandin={"Y" if quest_id in handin else "N"}')
    print('== 形状 × NPC 可解析 × 步骤解析 × 登记 ==')
    for key, count in shape_count.most_common():
        print(f'{count}\t{key}')
    OUT.write_text('# quest_id\tclient_shape\tnpc_state\tstep_resolve\tstep_npcs\tinHandin\n'
                   + '\n'.join(detail) + '\n', encoding='utf-8')
    print(f'written={OUT}')


if __name__ == '__main__':
    main()
