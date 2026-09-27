#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 6224 全量 owner/保留清单 retail-xml-retention.tsv（提示词 §4.B / 门禁 E.1、E.5 的数据源）。

行 = (quest_id, owner, family, reason, evidence)：
  owner=RETAIL_TABLE   真端模板表族且**驱动已实现**（当前 SimpleHunt/SimpleTalk）；reason=OK
  owner=XML_RETENTION  继续走 XML，reason ∈ {
    SCRIPTED            真端用 per-quest 注册点脚本驱动（quest_registry.tsv 有注册点），
    NO_TABLE            真端无任何覆盖（无模板行且无注册点），
    SEMANTIC_GAP        有模板行但家族编译器拒绝或与 XML 的 IR 不等价（家族证据文件），
    FAMILY_PENDING:<族>  真端表有行但该族驱动尚未实现（禁止虚报 RETAIL_TABLE），
  }
  owner=SERVER_ONLY    服务端自有任务（真端 quest.xml 无行；EVENT 派发）→ 继续走 XML。

多表重叠按优先级取首个（SimpleHunt > SimpleTalk > CombineTask > CollectItem > UseItem >
ItemPlay > SerialHunt > DataDriven），重叠数记入 evidence 供复核。
输出：.agents/summary/scriptdll-quest-driver/retail-xml-retention.tsv
"""
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
RETAIL = Path('/Users/mc/IdeaProjects/58Server/Map/XML')
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml'
IN_REPO_TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml'
REGISTRY = HERE / 'quest_registry.tsv'
REJECTIONS = HERE / 'phase5-3-rejections.txt'
# 家族编译器拒绝登记表（M2-c 批次 2 起，逐任务记录"真端文件无法表达"的字段）。
# Family compiler rejection registry (since M2-c batch 2).
COMPILER_REJECTS = REPO / 'src/test/resources/quest/retail-simplehunt-compiler-rejects.tsv'
# 真端语义缺口降级登记（M3-d 起）：真端表族 + 驱动虽已实现，但客户端契约无法用模板表表达，
# 已降级回 XML。优先级最高，避免重新生成时把这些行算回 RETAIL_TABLE。
# Highest-priority downgrade registry (M3-d): retail-table rows that cannot express the client
# contract and were deliberately reverted to XML ownership.
DOWNGRADE_REGISTRY = HERE / 'm3d-downgraded-quests.tsv'
OUT = HERE / 'retail-xml-retention.tsv'
OUT_RESOURCE = REPO / 'src/test/resources/quest/retail-xml-retention.tsv'
OUT_MAIN = REPO / 'src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv'

# 已实现驱动的家族：只有这里的家族才允许标 RETAIL_TABLE（生产 RetailQuestDriver 真正注入定义）。
# 其余家族即使真端表有行，也必须标 XML_RETENTION/FAMILY_PENDING，避免清单虚报"已由真端驱动"。
# Only these families have a working retail driver; the rest stay XML until their family slice lands.
IMPLEMENTED_FAMILIES = {'SimpleHunt', 'SimpleSerialHunt', 'SimpleUseItem', 'DataDriven', 'SimpleTalk',
	'CombineTask', 'SimpleCollectItem', 'SimpleItemPlay'}

# SimpleCollectItem：真端合成 vs 历史 XML 的漂移登记（RetailSimpleCollectItemGateTest 生成）。
# 可驱动且差异轴都在"客户端证据已判定 XML 陈旧"（REWARD_ROW / VAR0_FIELD / DIALOG_*）内的行 →
# RETAIL_TABLE/OK；带 ROUTE/OTHER 轴的行仍留 XML（等下一轮逐条定性）。
SIMPLE_COLLECT_DRIFT = REPO / 'src/test/resources/quest/retail-simple-collect-item-drift.tsv'
# M5-b3：60 个带 ROUTE/OTHER 轴的逐任务裁定（真端优先）。ADOPT_RETAIL → 放行退役；KEEP_XML → 真端无法表达，保留 XML。
# M5-b3 adjudication of the ROUTE/OTHER axes; ADOPT_RETAIL retires the XML, KEEP_XML keeps it.
SIMPLE_COLLECT_DECISIONS = HERE / 'm5b3-collect-route-decisions.tsv'
# M5-b3x：20 个类别哨兵行（真端阵营发放形状）的裁定；与 M5-b3 同格式，合并消费。
# M5-b3x adjudication of the category-sentinel (retail faction-grant) rows; merged with M5-b3.
SIMPLE_COLLECT_SENTINEL_DECISIONS = HERE / 'm5b3x-collect-sentinel-decisions.tsv'
# P1b：可选奖励 / 多交付物 49 行的裁定；同格式，合并消费。
# P1b adjudication of the selectable-reward / multi-item rows; merged with the others.
SIMPLE_COLLECT_CHOICE_DECISIONS = HERE / 'p1b-collect-choice-decisions.tsv'
# P3：SimpleSerialHunt 10 行的裁定（客户端链式阶梯）；同格式，合并消费。
# P3 adjudication of the SimpleSerialHunt rows; merged with the others.
SERIAL_HUNT_DECISIONS = HERE / 'p3-serial-hunt-decisions.tsv'
# P3b：SimpleUseItem 104 行的裁定（用物品接取规范形）；同格式，合并消费。
# P3b adjudication of the SimpleUseItem rows; merged with the others.
USE_ITEM_DECISIONS = HERE / 'p3b-use-item-decisions.tsv'
# P1：SimpleItemPlay 15 行的裁定（接取发物 → 用物品演出 → 交付；6 ADOPT + 9 KEEP）；同格式。
# P1 adjudication of the SimpleItemPlay rows (6 ADOPT + 9 KEEP); same format.
ITEM_PLAY_DECISIONS = HERE / 'p1-itemplay-decisions.tsv'
# P0c-10f：SimpleTalk wave A 链式行裁定（登记表回放等价 83 ADOPT + 40 KEEP 变体留 XML）。
# P0c-10f adjudication of the SimpleTalk chain rows (83 ADOPT + 40 KEEP variants); same format.
TALK_CHAIN_DECISIONS = HERE / 'p0c10f-talk-chain-decisions.tsv'
# P5-1：DataDriven Talk+hunt 采纳行裁定；同格式，合并消费。
# P5-1 adjudication of the DataDriven Talk+hunt rows; merged with the others.
DATA_DRIVEN_DECISIONS = HERE / 'p5-datadriven-decisions.tsv'
# SimpleUseItem 漂移登记（拒绝码路径消费；与 collect 同口径）。
# SimpleUseItem drift registry consumed for rejection codes.
SIMPLE_USE_ITEM_DRIFT = REPO / 'src/test/resources/quest/retail-simple-use-item-drift.tsv'
# 允许直接退役的差异轴（其余轴视为语义缺口）。 / Axes that do not block retirement.
COLLECT_RETIRABLE_AXES = {
	'REWARD_ROW', 'VAR0_FIELD', 'DIALOG_10', 'DIALOG_1008', 'DIALOG_1009',
	'DIALOG_39', 'DIALOG_20002', 'DIALOG_1012', 'DIALOG_1013',
}

# P0c-3：SimpleHunt 类别哨兵（_faction_）行的逐任务裁定（真端优先；ADOPT_RETAIL → 退役，KEEP_XML → 保留）。
# P0c-3 adjudication of the SimpleHunt faction-sentinel rows (retail-first).
SIMPLE_HUNT_DECISIONS = REPO / 'src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv'

# SimpleTalk：真端合成 vs 历史 XML 的漂移登记（RetailSimpleTalkGateTest 生成，登记不是退役门槛）。
# 可驱动（非 REJECTED）且不是客户端交付变体的行 → RETAIL_TABLE/OK；其余按拒绝码/变体保留 XML。
SIMPLE_TALK_DRIFT = REPO / 'src/test/resources/quest/retail-simple-talk-drift.tsv'
SIMPLE_TALK_VARIANTS = HERE / 'retail-simple-talk-client-variant.tsv'

# 家族优先级（与生产 RetailQuestCatalog 的判定顺序保持一致）
FAMILIES = [
    ('SimpleHunt', IN_REPO_TABLE),
    ('SimpleTalk', RETAIL / 'Quest_SimpleTalk.xml'),
    ('CombineTask', RETAIL / 'Quest_CombineTask.xml'),
    ('SimpleCollectItem', RETAIL / 'Quest_SimpleCollectItem.xml'),
    ('SimpleUseItem', RETAIL / 'Quest_SimpleUseItem.xml'),
    ('SimpleItemPlay', RETAIL / 'Quest_SimpleItemPlay.xml'),
    ('SimpleSerialHunt', RETAIL / 'Quest_SimpleSerialHunt.xml'),
    # (已在 FAMILIES 中；此处无操作占位以保持 diff 最小)

    ('DataDriven', RETAIL / 'data_driven_quest.xml'),
]


def previous_ledger_ids():
    """上一版保留清单里的 id 全集（追平账：退役 XML 已随 git 历史移出仓库，
    不再有测试作用域副本，故以清单自身承担"冻结宇宙"审计入口）。
    Ids from the previous retention ledger; with fixtures gone the ledger itself
    carries the frozen universe for regeneration.
    """
    if not OUT_RESOURCE.is_file():
        return set()
    ids = set()
    for line in OUT_RESOURCE.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        ids.add(int(line.split('\t')[0]))
    return ids


def live_catalog_ids():
    """当前生产白名单（XML 仍在仓库的任务）。 / Quest ids still backed by production XML."""
    text = CATALOG.read_text(encoding='utf-8')
    return {int(v) for v in re.findall(r'<definition id="(\d+)"', text)}


def retired_ids():
    """已退役的任务 id = 既有清单 - 生产白名单（XML 只在 git 历史里）。
    Retired ids = previous ledger minus the live catalog; their XML lives in git history.
    """
    return previous_ledger_ids() - live_catalog_ids()


def catalog_ids():
    """生产任务全集 = XML 目录 ∪ 既有清单（冻结宇宙，恒为 6224）。"""
    return live_catalog_ids() | previous_ledger_ids()


def table_ids(path):
    for encoding in ('utf-8', 'utf-16'):
        try:
            text = path.read_text(encoding=encoding)
            break
        except UnicodeError:
            continue
    ids = {int(v) for v in re.findall(r'<id id="(\d+)"', text)}
    if not ids:
        ids = {int(v) for v in re.findall(r'<id>(\d+)</id>', text)}
    return ids


def registry_quests():
    rows = {}
    for line in REGISTRY.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if parts[0] == 'quest_id':
            continue
        rows.setdefault(int(parts[0]), []).append(parts[2])
    return rows


def supplement_gap_ids():
    """家族等价门禁导出的对话路由差异任务（SEMANTIC_GAP:<码>）。

    行格式 `quest_id` 或 `quest_id<TAB>稳定码`（P0c-8c 起逐行给出裁定码，缺省 DIALOG_ROUTE）。
    """
    out = {}
    p = HERE / 'simplehunt-dialog-route-gaps.txt'
    if p.is_file():
        for line in p.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            parts = line.split('\t')
            if parts[0].strip().isdigit():
                out[int(parts[0].strip())] = parts[1].strip() if len(parts) > 1 and parts[1].strip() \
                    else 'DIALOG_ROUTE'
    return out


def compiler_rejects():
    """家族编译器拒绝登记表：quest_id → (code, detail)。"""
    out = {}
    if not COMPILER_REJECTS.is_file():
        return out
    for line in COMPILER_REJECTS.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        out[int(parts[0])] = (parts[1], parts[2] if len(parts) > 2 else '')
    return out


def downgrade_registry():
    """真端语义缺口降级登记：quest_id → (code, note)。"""
    out = {}
    if not DOWNGRADE_REGISTRY.is_file():
        return out
    for line in DOWNGRADE_REGISTRY.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        out[int(parts[0])] = (parts[1], parts[2] if len(parts) > 2 else '')
    return out


def simple_talk_classification():
    """SimpleTalk 漂移登记：quest_id → classification（EQUIVALENT/DIFF:*/REJECTED:*）。"""
    out = {}
    if not SIMPLE_TALK_DRIFT.is_file():
        return out
    for line in SIMPLE_TALK_DRIFT.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        out[int(parts[0])] = parts[1]
    return out


def simple_hunt_decisions():
	"""P0c-3 SimpleHunt 哨兵裁定：quest_id → (verdict, basis, evidence)。"""
	out = {}
	if not SIMPLE_HUNT_DECISIONS.is_file():
		return out
	for line in SIMPLE_HUNT_DECISIONS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		if len(parts) >= 5:
			out[int(parts[0])] = (parts[1], parts[2], parts[4])
	return out


def simple_collect_drift():
	"""SimpleCollectItem 漂移登记：quest_id → classification。"""
	out = {}
	if not SIMPLE_COLLECT_DRIFT.is_file():
		return out
	for line in SIMPLE_COLLECT_DRIFT.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		out[int(parts[0])] = parts[1]
	return out


def simple_collect_decisions():
	"""M5-b3 + M5-b3x + P1b 逐任务裁定合并：quest_id → (verdict, basis, evidence)。"""
	out = {}
	for path in (SIMPLE_COLLECT_DECISIONS, SIMPLE_COLLECT_SENTINEL_DECISIONS, SIMPLE_COLLECT_CHOICE_DECISIONS,
			SERIAL_HUNT_DECISIONS, USE_ITEM_DECISIONS, DATA_DRIVEN_DECISIONS):
		if not path.is_file():
			continue
		for line in path.read_text(encoding='utf-8').splitlines():
			if line.startswith('#') or not line.strip():
				continue
			parts = line.split('\t')
			if len(parts) >= 5:
				out[int(parts[0])] = (parts[1], parts[2], parts[4])
	return out


def use_item_drift():
	"""SimpleUseItem 漂移登记：quest_id → classification。"""
	out = {}
	if not SIMPLE_USE_ITEM_DRIFT.is_file():
		return out
	for line in SIMPLE_USE_ITEM_DRIFT.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		out[int(parts[0])] = parts[1]
	return out


def collect_axes(classification):
	"""分类串 → 差异轴集合（去掉方向前缀）。 / Axis names without direction prefix."""
	return {token.split(':', 1)[1] for token in classification.split(':', 1)[1].split()} \
		if classification.startswith('DIFF:') else set()


def simple_talk_client_variants():
    """真端表无法表达的客户端交付变体：quest_id → 客户端按钮。"""
    out = {}
    if not SIMPLE_TALK_VARIANTS.is_file():
        return out
    for line in SIMPLE_TALK_VARIANTS.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        out[int(parts[0])] = parts[1]
    return out


def rejection_ids():
    out = {}
    for line in REJECTIONS.read_text(encoding='utf-8').splitlines():
        m = re.match(r'(\d+):(\w+)', line)
        if m:
            out[int(m.group(1))] = m.group(2)
    return out


def main():
    repo = catalog_ids()
    family_ids = {}
    for name, path in FAMILIES:
        if path.is_file():
            family_ids[name] = table_ids(path)
        else:
            print(f'警告：真端表缺失 {path}')
    retired = retired_ids()
    registry = registry_quests()
    talk_drift = simple_talk_classification()
    talk_variants = simple_talk_client_variants()
    collect_drift = simple_collect_drift()
    use_item_drift_map = use_item_drift()
    collect_decisions = simple_collect_decisions()
    dd_drift = {}
    dd_drift_path = REPO / 'src/test/resources/quest/retail-data-driven-drift.tsv'
    if dd_drift_path.is_file():
        for line in dd_drift_path.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            parts = line.split('\t')
            if len(parts) >= 2:
                dd_drift[int(parts[0])] = parts[1]
    dd_decisions = {}
    dd_path = DATA_DRIVEN_DECISIONS
    if dd_path.is_file():
        for line in dd_path.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            parts = line.split('\t')
            if len(parts) >= 5:
                dd_decisions[int(parts[0])] = (parts[1], parts[2], parts[4])
    use_item_decisions = {}
    use_item_path = USE_ITEM_DECISIONS
    if use_item_path.is_file():
        for line in use_item_path.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            parts = line.split('\t')
            if len(parts) >= 5:
                use_item_decisions[int(parts[0])] = (parts[1], parts[2], parts[4])
    talk_chain_decisions = {}
    talk_chain_path = TALK_CHAIN_DECISIONS
    if talk_chain_path.is_file():
        for line in talk_chain_path.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            parts = line.split('\t')
            if len(parts) >= 5:
                talk_chain_decisions[int(parts[0])] = (parts[1], parts[2], parts[4])
    # P0c-10h wave B 复合行裁定（B-1/B-2 层优先：重裁定升级 p0c10f 的 KEEP 行）；
    # P0c-10m 单步接取发物层最后叠加（116 行采纳，覆盖同键的前层裁定）。
    b1_chain_decisions = {}
    for b1_name in ('p0c10h-talk-chain-b1-decisions.tsv', 'p0c10h-talk-chain-b2-decisions.tsv', 'p0c10h-talk-chain-b3-decisions.tsv',
        'p0c10h-talk-chain-b4-decisions.tsv', 'p0c10h-talk-chain-b5-decisions.tsv',
        'p0c10h-talk-chain-b6-decisions.tsv', 'p0c10h-talk-chain-b7-decisions.tsv',
        'p0c10h-talk-chain-b8-decisions.tsv', 'p0c10i-canonical-decisions.tsv',
        'p0c10j-deferred-decisions.tsv',
        'p0c10h-talk-chain-b6-decisions.tsv',
        'p0c10m-item-decisions.tsv',
        'p0c10n-cutscene-decisions.tsv',
        'p0c10o-workitem-decisions.tsv',
        'p0c11-collect-gate-adopt-decisions.tsv',
        'p0c12-sentinel-decisions.tsv',
        'p0c13-kaliga-decisions.tsv',
        'p0c14-report-decisions.tsv',
        'p0c14-clean-decisions.tsv',
        'p0c16-deferred-decisions.tsv',
        'p0c19-entry-npc-mirror-decisions.tsv',
        'p0c20-28800-adjudication.tsv',
        'p0c21-prereq-bucket-decisions.tsv',
        'p0c22-haramel-decisions.tsv',
        'p0c23-2150-2110-decisions.tsv',
        'p0c24-1141-readoption.tsv',
        'p0c25-30312-30315-decisions.tsv',
        'p0c27-1526-1351-journal-axis-flip.tsv',
        'p0c28-80290-heal-channel-flip.tsv',
        'p0c32-accept-entrance-decisions.tsv'):
        b1_chain_path = HERE / b1_name
        if not b1_chain_path.is_file():
            continue
        for line in b1_chain_path.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            parts = line.split('\t')
            if len(parts) >= 5:
                # 层内最后写入者胜；并行登记裁定来源文件，证据字段可追溯到具体裁定表。
                b1_chain_decisions[int(parts[0])] = (parts[1], parts[2], parts[4], b1_name)
    item_play_decisions = {}
    item_play_path = ITEM_PLAY_DECISIONS
    if item_play_path.is_file():
        for line in item_play_path.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            parts = line.split('\t')
            if len(parts) >= 5:
                item_play_decisions[int(parts[0])] = (parts[1], parts[2], parts[4])
    hunt_decisions = simple_hunt_decisions()
    compiler = compiler_rejects()
    downgrades = downgrade_registry()
    rejections = rejection_ids()
    gaps = supplement_gap_ids()
    rejections.update(gaps)

    rows = []
    counts = {}
    pending = {}
    for qid in sorted(repo):
        hits = [name for name, ids in family_ids.items() if qid in ids]
        if len(hits) > 1:
            evidence = f'overlap={",".join(hits)}'
        else:
            evidence = ''
        # P0c-13 复験采纳优先于 M3-d 降级登记：ADOPT_RETAIL 重裁定反转降级
        # （契约测试对生产视图通过后才写入裁定层，与 wave B 重裁定升级同一先例）。
        # P0c-13 re-adjudication out ranks the M3-d downgrade registry: an ADOPT_RETAIL
        # decision reverses the downgrade (contract test verified against the production view).
        if qid in downgrades and not (qid in b1_chain_decisions
                and b1_chain_decisions[qid][0] == 'ADOPT_RETAIL'):
            code, note = downgrades[qid]
            owner, family = 'XML_RETENTION', hits[0] if hits else '-'
            reason = f'SEMANTIC_GAP:{code}'
            evidence = f'downgraded:{note} {evidence}'.strip()
        elif qid in compiler:
            code, detail = compiler[qid]
            owner, family = 'XML_RETENTION', hits[0] if hits else '-'
            reason = f'SEMANTIC_GAP:{code}'
            evidence = f'retail-simplehunt-compiler-rejects.tsv {detail} {evidence}'.strip()
        elif qid in rejections:
            owner, family = 'XML_RETENTION', hits[0] if hits else '-'
            reason = f'SEMANTIC_GAP:{rejections[qid]}'
            source = 'simplehunt-dialog-route-gaps.txt' if qid in gaps else 'phase5-3-rejections.txt'
            evidence = f'{source} {evidence}'.strip()
        elif hits and hits[0] == 'SimpleTalk' and hits[0] in IMPLEMENTED_FAMILIES:
            owner, family = 'RETAIL_TABLE', hits[0]
            # P0c-10h wave B 裁定层优先于 wave A（19004 等重裁定升级：E 记录修复后等价成立）。
            b1_decision = b1_chain_decisions.get(qid)
            chain_decision = talk_chain_decisions.get(qid)
            classification = talk_drift.get(qid)
            if b1_decision is not None and b1_decision[0] == 'ADOPT_RETAIL':
                reason = 'OK'
                evidence = f'{b1_decision[3]} basis={b1_decision[1]} {evidence}'.strip()
            elif b1_decision is not None and b1_decision[0] == 'KEEP_XML':
                owner = 'XML_RETENTION'
                # 裁定表可带 KEEP_XML:<码> 精确缺口码（p0c10n craft 行）；缺省沿用链行码。
                keep_code = b1_decision[1].split(':', 1)[1] if b1_decision[1].startswith('KEEP_XML:') else 'RETAIL_TALK_CHAIN'
                reason = f'SEMANTIC_GAP:{keep_code}'
                evidence = f'{b1_decision[3]} basis={b1_decision[1]} {evidence}'.strip()
            elif chain_decision is not None and chain_decision[0] == 'ADOPT_RETAIL':
                reason = 'OK'
                evidence = f'p0c10f-talk-chain-decisions.tsv basis={chain_decision[1]} {evidence}'.strip()
            elif chain_decision is not None and chain_decision[0] == 'KEEP_XML':
                owner = 'XML_RETENTION'
                reason = 'SEMANTIC_GAP:RETAIL_TALK_CHAIN'
                evidence = f'p0c10f-talk-chain-decisions.tsv basis={chain_decision[1]} {evidence}'.strip()
            elif classification is None:
                reason = 'SEMANTIC_GAP:DRIFT_REGISTRY_MISSING'
                evidence = f'{evidence} drift-registry-missing'.strip()
            elif classification.startswith('REJECTED:'):
                owner = 'XML_RETENTION'
                reason = f'SEMANTIC_GAP:{classification.split(":", 1)[1]}'
                evidence = f'retail-simple-talk-drift.tsv {evidence}'.strip()
            elif qid in talk_variants:
                owner = 'XML_RETENTION'
                reason = 'SEMANTIC_GAP:CLIENT_REPORT_VARIANT'
                evidence = f'client-button={talk_variants[qid]} {evidence}'.strip()
            else:
                reason = 'OK'
        elif hits and hits[0] == 'SimpleHunt' and hits[0] in IMPLEMENTED_FAMILIES:
            owner, family = 'RETAIL_TABLE', hits[0]
            decision = hunt_decisions.get(qid)
            if decision is not None and decision[0] == 'KEEP_XML':
                owner = 'XML_RETENTION'
                reason = f'SEMANTIC_GAP:{decision[1]}'
                evidence = f'retail-simple-hunt-adjudicated-decisions.tsv {decision[2]} {evidence}'.strip()
            elif decision is not None and decision[0] == 'ADOPT_RETAIL':
                reason = 'OK'
                evidence = f'retail-simple-hunt-adjudicated-decisions.tsv basis={decision[1]} {evidence}'.strip()
            else:
                reason = 'OK'
        elif hits and hits[0] == 'SimpleCollectItem' and hits[0] in IMPLEMENTED_FAMILIES:
            owner, family = 'RETAIL_TABLE', hits[0]
            classification = collect_drift.get(qid)
            decision = collect_decisions.get(qid)
            if decision is not None and decision[0] == 'KEEP_XML':
                owner = 'XML_RETENTION'
                reason = f'SEMANTIC_GAP:{decision[1]}'
                evidence = f'm5b3-collect-route-decisions.tsv {decision[2]} {evidence}'.strip()
            elif decision is not None and decision[0] == 'ADOPT_RETAIL':
                reason = 'OK'
                evidence = f'm5b3-collect-route-decisions.tsv basis={decision[1]} {evidence}'.strip()
            elif classification is None:
                owner = 'XML_RETENTION'
                reason = 'SEMANTIC_GAP:DRIFT_REGISTRY_MISSING'
                evidence = f'{evidence} drift-registry-missing'.strip()
            elif classification.startswith('REJECTED:'):
                owner = 'XML_RETENTION'
                reason = f'SEMANTIC_GAP:{classification.split(":", 1)[1]}'
                evidence = f'retail-simple-collect-item-drift.tsv {evidence}'.strip()
            elif collect_axes(classification) - COLLECT_RETIRABLE_AXES:
                owner = 'XML_RETENTION'
                reason = 'SEMANTIC_GAP:CLIENT_ROUTE_VARIANT'
                axes = ','.join(sorted(collect_axes(classification) - COLLECT_RETIRABLE_AXES))
                evidence = f'unreviewed-axes={axes} {evidence}'.strip()
            else:
                reason = 'OK'
        elif hits and hits[0] == 'SimpleUseItem' and hits[0] in IMPLEMENTED_FAMILIES:
            owner, family = 'RETAIL_TABLE', hits[0]
            classification = use_item_drift_map.get(qid)
            decision = use_item_decisions.get(qid)
            if decision is not None and decision[0] == 'ADOPT_RETAIL':
                reason = 'OK'
                evidence = f'p3b-use-item-decisions.tsv basis={decision[1]} {evidence}'.strip()
            elif classification is not None and classification.startswith('REJECTED:'):
                owner = 'XML_RETENTION'
                reason = f'SEMANTIC_GAP:{classification.split(":", 1)[1]}'
                evidence = f'retail-simple-use-item-drift.tsv {evidence}'.strip()
            else:
                reason = 'OK'
        elif hits and hits[0] == 'DataDriven' and hits[0] in IMPLEMENTED_FAMILIES:
            owner, family = 'RETAIL_TABLE', hits[0]
            dd_decision = dd_decisions.get(qid)
            if dd_decision is not None and dd_decision[0] == 'ADOPT_RETAIL':
                reason = 'OK'
                evidence = f'p5-datadriven-decisions.tsv basis={dd_decision[1]} {evidence}'.strip()
            elif (dd_classification := dd_drift.get(qid)) is not None \
                    and dd_classification.startswith('REJECTED:'):
                # 已实现驱动但该行被稳定码拒绝（形状未覆盖/客户端词汇表超模板）→ 语义缺口，保留 XML。
                owner = 'XML_RETENTION'
                reason = f'SEMANTIC_GAP:{dd_classification.split(":", 1)[1]}'
                evidence = f'retail-data-driven-drift.tsv {evidence}'.strip()
            else:
                owner = 'XML_RETENTION'
                reason = 'FAMILY_PENDING:DataDriven'
        elif hits and hits[0] == 'SimpleItemPlay' and hits[0] in IMPLEMENTED_FAMILIES:
            owner, family = 'RETAIL_TABLE', hits[0]
            decision = item_play_decisions.get(qid)
            if decision is not None and decision[0] == 'ADOPT_RETAIL':
                reason = 'OK'
                evidence = f'p1-itemplay-decisions.tsv basis={decision[1]} {evidence}'.strip()
            elif decision is not None and decision[0] == 'KEEP_XML':
                owner = 'XML_RETENTION'
                reason = f'SEMANTIC_GAP:{decision[1]}'
                evidence = f'p1-itemplay-decisions.tsv {evidence}'.strip()
            else:
                owner = 'XML_RETENTION'
                reason = 'FAMILY_PENDING:SimpleItemPlay'
        elif hits and hits[0] in IMPLEMENTED_FAMILIES:
            owner, family, reason = 'RETAIL_TABLE', hits[0], 'OK'
        elif hits:
            owner, family = 'XML_RETENTION', hits[0]
            reason = f'FAMILY_PENDING:{hits[0]}'
            evidence = f'retail-table-present driver=pending {evidence}'.strip()
        elif qid in registry:
            owner, family, reason = 'XML_RETENTION', '-', 'SCRIPTED'
            evidence = f'registry={",".join(sorted(set(registry[qid])))}'
        else:
            owner, family, reason = 'XML_RETENTION', '-', 'NO_TABLE'
            evidence = 'retail-none'
        if qid in retired:
            evidence = f'retired-xml-in-git-history {evidence}'.strip()
        rows.append((qid, owner, family, reason, evidence))
        base = reason.split(':')[0]
        counts[(owner, base)] = counts.get((owner, base), 0) + 1
        if base == 'FAMILY_PENDING':
            pending[reason] = pending.get(reason, 0) + 1

    header = ('# 6224 全量 owner/保留清单（quest_id, owner, family, reason, evidence）\n'
              '# 由 build_retention_list.py 生成；门禁 RetailOwnershipGateTest 消费本文件。\n')
    for out in (OUT, OUT_RESOURCE, OUT_MAIN):
        with out.open('w', encoding='utf-8') as fh:
            fh.write(header)
            for row in rows:
                fh.write('\t'.join(str(x) for x in row) + '\n')
    print(f'total={len(rows)}')
    for (owner, reason), n in sorted(counts.items(), key=lambda kv: (-kv[1], kv[0])):
        print(f'  {owner}/{reason}\t{n}')
    for reason, n in sorted(pending.items(), key=lambda kv: (-kv[1], kv[0])):
        print(f'    {reason}\t{n}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
