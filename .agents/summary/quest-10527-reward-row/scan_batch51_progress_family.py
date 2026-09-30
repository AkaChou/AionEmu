#!/usr/bin/env python3
"""批次 51 取证扫描：剩余 MISSING_TAIL_ROWS 任务按客户端 quest_script 形态分族。

对每个残留任务输出：客户端行数/行文本、quest_script_monster 的 Progress 声明、quest.xml 的
collect_progress、当前 XML 的 reward 投影、legacy handler 名与关键阶梯行，用于判定
`Progress(a~b)` 单变量 step 走行族 vs SECTION 链式族 vs 行号族。
"""
from __future__ import annotations
import os

import csv
import importlib.util
import re
import subprocess
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
TOPIC = Path(__file__).resolve().parent
CLIENT_QUEST_XML = Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/Quest_unpacked/quest.xml")
SCRIPT_CSV = Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/Quest_unpacked/quest_script_monster.csv")
LEGACY_ROOT = 'origin/history'


def load_audit():
    spec = importlib.util.spec_from_file_location("audit", TOPIC / "audit_reward_row_vs_client_steps.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules['audit'] = module
    spec.loader.exec_module(module)
    return module


def audit_rows() -> list[dict[str, str]]:
    """读取主审计落盘的逐任务结果（audit-output.tsv 由 audit_reward_row_vs_client_steps.py 生成）。"""
    with (TOPIC / 'audit-output.tsv').open(newline='', encoding='utf-8') as handle:
        return list(csv.DictReader(handle, delimiter='\t'))


def residual_tail_rows(audit) -> list[dict[str, str]]:
    """用主审计的“已修复族 / 已登记例外”注册表，从落盘结果里求 MISSING_TAIL_ROWS 残留。"""
    fixed = (audit.BATCH31_GELKMAROS_ROW_LADDER | audit.BATCH32_KALDOR_ROW_LADDER
             | audit.BATCH34_RENTUS_BASE_ROW_LADDER | audit.BATCH35_VALENTINE_TOWER_ROW_LADDER
             | audit.BATCH36_EVENT_ROW_LADDER | audit.BATCH37_TALK_KILL_REPORT_ROW_LADDER
             | audit.BATCH38_BRANCH_CHOICE_REWARD_INDEX | audit.BATCH39_TURN_IN_TALK_REPORT_ROWS
             | audit.BATCH40_THREE_NPC_TALK_ROWS | audit.BATCH41_PANGAIA_FORTRESS_ROWS
             | audit.BATCH42_TOMBSTONE_FLOWER_ROWS | audit.BATCH43_MALODOR_ANTIDOTE_ROWS
             | audit.BATCH44_FOAM_WISP_ROWS | audit.BATCH45_THREE_ROW_AND_BRANCH_ROWS
             | audit.BATCH48_INGGISON_NURSING_ROWS | audit.BATCH49_GELKMAROS_KANTELE_ROWS
             | audit.BATCH50_ALTGARD_MOSBEARS_COUNTER_ROW)
    registered = (audit.BLANK_JOURNAL_SLOT_EXCEPTIONS | audit.CLIENT_ONLY_ISOLATED_QUESTS
                  | audit.COUNTER_SLOT_EXCEPTIONS | audit.MULTI_LAYER_COUNTER_EXCEPTIONS
                  | audit.SHARED_VISIBLE_SLOT_EXCEPTIONS | audit.QE045_LOCKED
                  | audit.CLIENT_SCRIPTED_ROW_EXCEPTIONS)
    return [row for row in audit_rows()
            if row['shape'] == 'MISSING_TAIL_ROWS'
            and int(row['quest_id']) not in fixed
            and int(row['quest_id']) not in registered]


def quest_script(quest_id: int) -> list[list[str]]:
    result = []
    with SCRIPT_CSV.open(newline='', encoding='utf-8', errors='replace') as handle:
        for row in csv.reader(handle):
            if row and row[0] == str(quest_id):
                result.append(row)
    return result


def collect_progress(quest_id: int) -> str:
    text = CLIENT_QUEST_XML.read_text(encoding='utf-8', errors='replace')
    marker = f'<id>{quest_id}</id>'
    start = text.find(marker)
    if start < 0:
        return ''
    end = text.find('</quest>', start)
    block = text[start:end]
    found = re.search(r'<collect_progress>(\d+)</collect_progress>', block)
    item = re.search(r'<collect_item1>([^<]*)</collect_item1>', block)
    return f'{found.group(1) if found else "-"}/{(item.group(1).strip() if item else "-")}'


def legacy_handler(quest_id: int) -> tuple[str, list[str]]:
    listing = subprocess.run(['/usr/bin/git', '-C', str(REPO), 'grep', '-l', f'questId = {quest_id}',
                              LEGACY_ROOT, '--', 'src/main/java/com/aionemu/gameserver/quest/handlers'],
                             capture_output=True, text=True).stdout.split()
    if not listing:
        return '', []
    path = listing[0]
    body = subprocess.run(['/usr/bin/git', '-C', str(REPO), 'show', path],
                          capture_output=True, text=True).stdout
    relevant = [line.strip() for line in body.splitlines()
                if re.search(r'changeQuestStep|defaultCloseDialog|checkQuestItems|defaultOnKillEvent|'
                             r'useQuestItem|checkItemExistence|defaultFollowEndEvent|setStatus', line)]
    return Path(path).name, relevant[:14]


def xml_nodes(quest_id: int) -> tuple[str, str]:
    path = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quests' / f'{quest_id}.xml'
    if not path.is_file():
        return 'NO_DEFINITION', ''
    text = path.read_text(encoding='utf-8')
    nodes = re.findall(r'<node label="([^"]+)" status="(\w+)">\s*(?:<var name="var0" value="(\d+)"/>\s*)?</node>', text)
    rendered = ' '.join(f'{label}({status}{"," + value if value else ""})' for label, status, value in nodes)
    return rendered, text


def main() -> int:
    audit = load_audit()
    argv = sys.argv[1:]
    ids = [int(value) for value in argv] if argv else None
    residual = residual_tail_rows(audit)
    if ids is not None:
        residual = [row for row in residual if int(row['quest_id']) in ids]

    print(f'residual_missing_tail_rows={len(residual)}')
    print('quest\trows\tmissing\treward_step\tmirror\tscript\tcollect\tnodes\tlegacy\tlegacy_lines')
    for row in residual:
        quest_id = int(row['quest_id'])
        script = quest_script(quest_id)
        script_desc = ' | '.join(f'{entry[1]}<-{entry[6]}' for entry in script[:4]) or '-'
        nodes, _ = xml_nodes(quest_id)
        legacy_name, legacy_lines = legacy_handler(quest_id)
        print('\t'.join([
            str(quest_id), row['client_rows'], row['rows_without_state'].replace(' ', ','),
            str(row['reward_var0']), str(audit.mirror_of(quest_id)), script_desc,
            collect_progress(quest_id) or '-', nodes or '-', legacy_name or '-',
            ' ; '.join(legacy_lines) if legacy_lines else '-',
        ]))
    return 0


if __name__ == '__main__':
    sys.exit(main())
