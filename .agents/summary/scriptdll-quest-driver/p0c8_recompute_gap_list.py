#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-8a：用**当前编译器实测**的 IR 差异轴重算 SimpleHunt 对话路由缺口表。

背景：`simplehunt-dialog-route-gaps.txt` 是 M2-c 时代导出的差异清单（P0c-6 后 432 行），
此后合成器不断补齐接线（P0c-2/3/4/5/6），但清单从未按当前编译器重算——实测 565 个保留行里
**155 行已可证 IR 等价**（节点集合 + 转换多重集 + 进度布局三项全等）。本脚本据此重算：

  EQUAL / RAW_EQUAL   → 已可证等价 → 移出缺口表（owner 转 `RETAIL_TABLE`，XML 退役）；
  窄投影轴            → XML 用历史窄位宽（width 1/3）而真端用 6 位 SECTION 网格：节点/转换
                        多重集相等或只差投影，客户端 `SECTION_n` 门控在两种宽度下不可区分
                        （count < 8 时打包位相同）→ 按族形状权威（真端 6 位网格）ADOPT_RETAIL；
  其余差异轴          → 留在缺口表（P0c-8b 逐行按客户端契约裁定）。

输入：p0c8-retention-diff-census.tsv（P0c8RetentionDiffProbeTest 导出，机器可重跑）
输出：
  * 重写 simplehunt-dialog-route-gaps.txt（移除已等价行与窄投影行）；
  * p0c8-equivalence-retire-decisions.tsv（owner 记录：轴 + 结构计数 + 结论）；
  * 追加窄投影行到 retail-simple-hunt-adjudicated-decisions.tsv（ADOPT_RETAIL 裁定）。
用法：python3 -B p0c8_recompute_gap_list.py [--dry-run]
"""
import argparse
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
CENSUS = HERE / 'p0c8-retention-diff-census.tsv'
GAPS = HERE / 'simplehunt-dialog-route-gaps.txt'
REJECTIONS = HERE / 'phase5-3-rejections.txt'
RETIRE_DECISIONS = HERE / 'p0c8-equivalence-retire-decisions.tsv'
MONSTER = Path('/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_monster.csv')
PROD_DECISIONS = Path('/Users/mc/IdeaProjects/AionEmu-test/src/test/resources/quest'
	'/retail-simple-hunt-adjudicated-decisions.tsv')
# 窄投影裁定：quest_id → (轴, 客户端门控证据)。 / Legacy-projection adjudication rows.
NARROW_PROJECTION = {
	11151: ('XML_LAYOUT_WIDTH', 'node/transition 多重集相等（nodes 28/28、transitions 69/69），'
		'只差进度布局位宽：XML var0/var1 width=3（max 4）vs 真端 6 位 SECTION 网格（width=6）'),
	18313: ('XML_LAYOUT_WIDTH+XML_KILL_ROUTE_OVERSPEC', '展开口径下 XML 报 AMBIGUOUS_TRANSITION'
		'（每槽列 3 个种族变体 id，名族展开后路由重叠）→ 展开口径不可用；未展开口径 node sets differ'
		'（XML 4 节点 width=1 历史窄投影 vs 真端 11 节点 6 位网格）'),
}
GAP_HEADER = (
	'# P0c-8a（2026-09-24）重算：用 P0c8RetentionDiffProbeTest 的实测差异轴重建本表。\n'
	'#   已可证 IR 等价的 155 行移出本表（退役，见 p0c8-equivalence-retire-decisions.tsv）；\n'
	'#   2 行窄投影（11151/18313）按族形状权威（真端 6 位 SECTION 网格）裁定退役，\n'
	'#   裁定行见 retail-simple-hunt-adjudicated-decisions.tsv basis=XML_LEGACY_PROJECTION。\n'
	'#   其余行 = 仍未等价的差异轴面，逐行裁定见 reports/2026-09-24-P0c8-*.zh-CN.md。\n'
	'# 历史：M2-c 批次 2 首次导出；P0c-6 移除 22 个 `talk_npc1` 简报行（简报链已接线）。\n')


def client_gates(quest_id):
	"""客户端 quest_monster.csv 的击杀行门控（窄投影裁定的客户端证据）。"""
	gates = []
	for line in MONSTER.read_text(encoding='utf-8', errors='replace').splitlines():
		parts = line.split(',')
		if len(parts) >= 2 and parts[0].strip().isdigit() and int(parts[0].strip()) == quest_id:
			gates.append(parts[1].strip())
	return ' | '.join(gates)


def census():
	"""实测普查：quest_id → (轴, 保留原因, 明细)。"""
	rows = {}
	for line in CENSUS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		rows[int(parts[0])] = (parts[1], parts[2] if len(parts) > 2 else '', parts[3] if len(parts) > 3 else '')
	return rows


def gap_ids():
	"""缺口表 id（**只读非注释行**：注释里的数字不是 id）。"""
	ids = []
	for line in GAPS.read_text(encoding='utf-8').splitlines():
		token = line.strip()
		if not token or token.startswith('#'):
			continue
		if token.isdigit():
			ids.append(int(token))
	return ids


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('--dry-run', action='store_true')
	args = parser.parse_args()

	rows = census()
	equal = sorted(qid for qid, row in rows.items() if row[0] in ('EQUAL', 'RAW_EQUAL'))
	narrow = sorted(qid for qid in NARROW_PROJECTION if qid in rows)
	others = sorted(qid for qid in rows if qid not in set(equal) and qid not in set(narrow))
	print('普查行=%d | 已等价=%d | 窄投影裁定=%d | 其余差异行=%d'
		% (len(rows), len(equal), len(narrow), len(others)))

	# 1) 等价行退役记录（owner = 冻结 IR 指纹；本表是机器可读的判定依据）。
	text = ('# P0c-8a SimpleHunt 覆盖缺口关闭：已可证 IR 等价的保留行 → 退役（真端驱动）\n'
		'# 判据：RetailSimpleHuntEquivalenceGateTest.irEquivalenceProblem == null\n'
		'#       （节点集合 + 转换多重集（含事件/条件/动作/after-commit/优先级）+ 进度布局三项全等）\n'
		'# 证据：p0c8-retention-diff-census.tsv 同行；退役后由冻结 IR 指纹继续承担等价证明。\n'
		'# quest_id\tverdict\tbasis\taxes\tevidence\n')
	for qid in equal:
		axis, reason, detail = rows[qid]
		text += '%d\tADOPT_RETAIL\tEQUIVALENCE_PROVEN\t%s\t%s\n' % (qid, axis, detail)
	for qid in narrow:
		axis, reason, detail = rows[qid]
		gates = client_gates(qid)
		text += '%d\tADOPT_RETAIL\tXML_LEGACY_PROJECTION\t%s\t%s；客户端门控 %s（6 位 SECTION 字段，'\
			'count<8 时两种位宽打包位相同 → 客户端不可区分）；真端 6 位网格为族形状权威（377 行同理）\n' % (
				qid, axis, detail, gates)
	RETIRE_DECISIONS.write_text(text, encoding='utf-8')
	print('退役记录 ->', RETIRE_DECISIONS, '（%d 行 = %d 等价 + %d 窄投影）'
		% (len(equal) + len(narrow), len(equal), len(narrow)))

	# 2) 缺口表重算：移除等价行与窄投影裁定行。
	old = gap_ids()
	drop = set(equal) | set(narrow)
	new = [qid for qid in old if qid not in drop]
	text = GAP_HEADER + '\n'.join(str(qid) for qid in new) + '\n'
	GAPS.write_text(text, encoding='utf-8')
	print('缺口表 %d -> %d 行' % (len(old), len(new)))

	# 2b) phase5-3 拒绝表同样重算：实测已等价的旧拒绝行必须删除（否则保留清单仍按旧口径登记）。
	old_lines = [line for line in REJECTIONS.read_text(encoding='utf-8').splitlines()
		if line.strip() and not line.startswith('#')]
	kept, pruned = [], []
	for line in old_lines:
		m = re.match(r'(\d+):', line)
		if m and int(m.group(1)) in drop:
			pruned.append(int(m.group(1)))
		else:
			kept.append(line)
	if pruned:
		REJECTIONS.write_text('# P0c-8a（2026-09-24）重算：删除实测已可证等价的旧拒绝行 %s。\n%s\n'
			% (','.join(str(q) for q in sorted(pruned)), '\n'.join(kept)), encoding='utf-8')
	print('phase5-3 拒绝表 %d -> %d 行（删除 %s）' % (len(old_lines), len(kept), pruned))

	# 3) 窄投影行登记进裁定表（ADOPT_RETAIL：真端 6 位网格为形状权威，XML 是历史窄投影）。
	existing = PROD_DECISIONS.read_text(encoding='utf-8').splitlines()
	header = [line for line in existing if line.startswith('#')]
	body = [line for line in existing if not line.startswith('#') and line.strip()]
	body = [line for line in body if int(line.split('\t')[0]) not in set(narrow)]
	added = []
	for qid in narrow:
		axis, reason, detail = rows[qid]
		gates = client_gates(qid)
		row = '%d\tADOPT_RETAIL\tXML_LEGACY_PROJECTION\t%s\t%s；客户端门控 %s（6 位 SECTION 字段，'\
			'count<8 时两种位宽打包位相同）；真端 6 位网格为族形状权威' % (qid, axis, detail, gates)
		body.append(row)
		added.append(row)
	body.sort(key=lambda line: int(line.split('\t')[0]))
	header = [line.replace('（P0c-3 哨兵批 + P0c-6 简报批）',
		'（P0c-3 哨兵批 + P0c-6 简报批 + P0c-8a 窄投影批）') for line in header]
	if not any('p0c8_recompute_gap_list' in line for line in header):
		header.append('# 生成：.agents/summary/scriptdll-quest-driver/p0c8_recompute_gap_list.py（窄投影裁定）')
	if not args.dry_run:
		PROD_DECISIONS.write_text('\n'.join(header + body) + '\n', encoding='utf-8')
	print('裁定表 ->', PROD_DECISIONS, '（%d 行）' % len(body))
	for row in added:
		print(' ', row[:190])
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
