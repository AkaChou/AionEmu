#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-6：把退役 XML 里被移除的"存档修复边"登记成**可选的**一次性 DB 归一化清单。

背景：真端形状下 `var0` 是击杀计数、任务书行由客户端 `SECTION_n` 门控推导，服务端不再持有
"任务书行号"，因此旧 XML 的 `enter-world` 自愈边（把旧存档的 packed 行推到领奖行 / 把
"未听简报先杀怪"的存档归一化）按 P3 既有裁定一并移除。提示词口径：**不要求零进度损失**，
一次性 DB 归一化只是可选项，但要登记成机器可读的清单。

判据（逐行来自 git HEAD 的退役 XML，可复核）：所有携带 `enter-world`/`enter-zone` 事件的转移，
输出其 (源节点, 目标节点, 事件, 条件, 动作)。

字段口径：`quest_vars` 是 32 位打包值，`var0` 占 bit 0..5（SECTION_0），
`var5` 占 bit 30（简报标志位，宽 1）。

输出：p0c6-legacy-save-normalization.tsv（quest_id, legacy_state, removed_action, retail_expectation, evidence）
用法：python3 -B p0c6_build_legacy_save_normalization.py
"""
import os
import re
import subprocess
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
HERE = Path(__file__).resolve().parent
DECISIONS = REPO / 'src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv'
RETAIL_TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml'
OUT = HERE / 'p0c6-legacy-save-normalization.tsv'
QUESTS_REL = 'src/main/resources/aion/data/static_data/quest_definition/quests'


def head_xml(quest_id):
	proc = subprocess.run(['git', 'show', 'HEAD:%s/%d.xml' % (QUESTS_REL, quest_id)], cwd=REPO,
		capture_output=True, text=True)
	return proc.stdout if proc.returncode == 0 else ''


def adopted_ids():
	"""本次退役的 21 行（ADOPT_RETAIL）。 / The 21 retired rows of this slice."""
	ids = []
	for line in DECISIONS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		if len(parts) >= 5 and parts[1] == 'ADOPT_RETAIL' and parts[2] == 'BRIEFING_STEP':
			ids.append(int(parts[0]))
	return sorted(ids)


def text_of(block, tag):
	match = re.search(r'<%s>(.*?)</%s>' % (tag, tag), block, re.S)
	return re.sub(r'\s+', ' ', match.group(1)).strip() if match else ''


def repair_edges(quest_id):
	"""退役 XML 里的 enter-world/enter-zone 边。 / Enter-world/enter-zone edges of the retired XML."""
	text = head_xml(quest_id)
	edges = []
	for match in re.finditer(r'<transition\s([^>]*)>([\s\S]*?)</transition>', text):
		attrs, body = match.group(1), match.group(2)
		if 'enter-world' not in body and 'enter-zone' not in body:
			continue
		source = re.search(r'source="([^"]+)"', attrs)
		target = re.search(r'target="([^"]+)"', attrs)
		event = 'enter-world' if 'enter-world' in body else 'enter-zone'
		edges.append((
			source.group(1) if source else '(none)',
			target.group(1) if target else '(none)',
			event,
			text_of(body, 'conditions') or '-',
			text_of(body, 'actions') or '-',
		))
	return edges


def var0(value):
	"""var0 在 quest_vars 里的位掩码表达式。 / Bit-mask expression for var0."""
	return '(quest_vars & 63) = %d' % value


def retail_counts():
	"""真端表每行的 (槽位, 要求数)：count1..8。 / Retail per-slot required counts."""
	counts = {}
	for match in re.finditer(r'<id id="(\d+)">([\s\S]*?)</id>',
			RETAIL_TABLE.read_text(encoding='utf-8')):
		slots = []
		for slot in range(1, 9):
			found = re.search(r'<count%d>(\d+)</count%d>' % (slot, slot), match.group(2))
			count = re.search(r'<monster%d>' % slot, match.group(2))
			if found and count:
				slots.append('var%d=%s' % (slot - 1, found.group(1)))
		counts[int(match.group(1))] = ','.join(slots) or '-'
	return counts



def main():
	counts = retail_counts()
	rows = []
	for quest_id in adopted_ids():
		for source, target, event, conditions, actions in repair_edges(quest_id):
			legacy = []
			for status in re.findall(r'status-is status="([A-Z]+)"', conditions):
				legacy.append('status=' + status)
			for field, value in re.findall(r'variable-is field="(\w+)" value="(\d+)"', conditions):
				legacy.append('%s=%s [%s]' % (field, value, var0(int(value)) if field == 'var0' else 'bit30'))
			removed = []
			for field, value in re.findall(r'set-variable field="(\w+)" value="(\d+)"', actions):
				removed.append('%s:=%s' % (field, value))
			expectation = ('真端形状：REWARD 投影满段计数（%s）＋ 客户端 SECTION 门控推导任务书行，'
				'服务端无行号可漂移；**不要**把旧边写入的行号搬过来' % counts.get(quest_id, '-'))
			rows.append((quest_id, source, target, event, ' '.join(legacy) or '-',
				' '.join(removed) or '-', expectation))
	rows.sort(key=lambda row: (row[0], row[1], row[2]))
	text = ('# P0c-6 退役 XML 的"存档修复边"→ 可选一次性 DB 归一化清单\n'
		'# 口径：真端驱动后 var0 是击杀计数（bit 0..5 = SECTION_0）、var5 是简报标志位（bit 30）；\n'
		'#       旧存档若停在旧 XML 的"行号/未听简报"形状，可按本表做一次性归一化（**可选**，不要求零进度损失）。\n'
		'# 列：quest_id / 旧源节点 / 旧目标节点 / 事件 / 被移除边的条件 / 被移除边的动作 / 真端形状\n')
	for row in rows:
		text += '\t'.join(str(cell) for cell in row) + '\n'
	OUT.write_text(text, encoding='utf-8')
	print('归一化清单 ->', OUT, '（%d 条退役修复边，覆盖 %d 个任务）'
		% (len(rows), len({row[0] for row in rows})))
	for row in rows:
		print(' ', row[0], row[1], '--', row[3], '->', row[2], '|', row[4], '|', row[5])
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
