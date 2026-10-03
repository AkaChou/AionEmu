#!/usr/bin/env python3
"""retail 十表 XSD 生成器（2026-10-03，与 DOCTYPE 剥离同批）。

生成口径（逐条写入各 XSD 头注释）：
1. required 行子元素 = 观察全域（该标签在行中出现率 100%）∪ 装载器 fail-closed 必填；
2. 行键唯一（xs:unique）：行 id（装载器对重复 id fail-closed）；quest.xml 追加 name 唯一
   （NativeQuestXmlTable 对非空重名 fail-closed）；
3. int 定型 = 生产代码 int 解析点清单（NativeQuestTableLoader / RetailQuestMetadataCompiler /
   NativeQuestXmlTable / DataDrivenQuestTable）；未列出的子元素一律 xs:string；
4. 枚举/模式 = fail-closed 语义域：talk/use-item 的 item_check 与 collect 的 party_drop 恒 "1"；
   npcfactions 星期位 {0,1}；DD progress 类别 = 8 kind 的大小写不敏感字母类模式（镜像装载端 lower 归一）；
5. 行子元素用 xs:all（装载器按标签名读取、与顺序无关）；容器（quest.xml 11 组职业奖励 /
   DD progress_info）递归建模，重复度按父节点内实测分布定型（>1 即 unbounded）。

输入 = 盘上最终文件（DOCTYPE 剥离后，ElementTree 解析；预定义实体语义与 Xerces 一致，已抽查）。
输出 = 同目录 <stem>.xsd（2 空格缩进，UTF-8+LF，遵循 .agents/rules/formatting.md）。
"""
import pathlib
import textwrap
import re
import xml.etree.ElementTree as ET
from collections import Counter

REPO = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
RETAIL = REPO / 'src/main/resources/aion/data/static_data/quest/retail'
IND = '  '
WEEKDAYS = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun']

CONFIGS = [
	dict(stem='Quest_SimpleHunt', root='quest_simplehunts', row='id', id_kind='attr',
	     loader_required=['acquired_npc_name', 'dev_name'],
	     int_tags={'con_quest', 'cutsceneid1', 'cs1_haction'} | {f'count{i}' for i in range(1, 6)},
	     enums={}, min_inclusive=set(),
	     zh='行 = <id id>（id 属性必填、非负、全表唯一）；count1..5 为整数（装载器 requireOwnInt），'
	        'monsterN 缺 countN 属真端非法形——共现约束 XSD 1.0 无法表达，由装载器 fail-closed 兜底。',
	     en='Rows are <id id> (required unique non-negative attr id); count1..5 are ints '
	        '(requireOwnInt); monsterN-without-countN is illegal in retail but a co-occurrence '
	        'constraint XSD 1.0 cannot express — the loader fail-closes on it instead.'),
	dict(stem='Quest_SimpleSerialHunt', root='quest_simpleserialhunts', row='id', id_kind='attr',
	     loader_required=[],
	     int_tags={f'count_{n}' for n in ['first', 'second', 'third', 'fourth', 'fifth']},
	     enums={}, min_inclusive=set(),
	     zh='行 = <id id>；count_first..fifth 为整数（装载器直接 parseInt，非数字即炸）。',
	     en='Rows are <id id>; count_first..fifth are ints (the loader parseInts them directly).'),
	dict(stem='Quest_SimpleTalk', root='quest_simpletalks', row='id', id_kind='attr',
	     loader_required=['acquired_npc_name', 'reward_npc_name'],
	     int_tags={'con_quest', 'cutsceneid1', 'cs1_haction'},
	     enums={'item_check': ['1']}, min_inclusive=set(),
	     zh='双 NPC 为装载器必填（缺失/空白 fail-closed）；item_check 出现即恒 "1"（观察形，'
	        '装载器按 == "1" 判定）。',
	     en='The two-NPC pair is loader-mandatory; item_check is always "1" when present (observed '
	        'shape; the loader tests == "1").'),
	dict(stem='Quest_SimpleCollectItem', root='quest_simplecollectitems', row='id', id_kind='attr',
	     loader_required=['acquired_npc_name', 'reward_npc_name'],
	     int_tags={'con_quest', 'cutsceneid1', 'cs1_haction'},
	     enums={'party_drop': ['1']}, min_inclusive=set(),
	     zh='双 NPC 为装载器必填；object1..4 可缺（五个 TEST 行无采集物 = 装载器视为不可路由）；'
	        'party_drop 出现即恒 "1"。',
	     en='The two-NPC pair is loader-mandatory; object1..4 may be absent (the five TEST rows carry '
	        'none = unroutable at load); party_drop is always "1" when present.'),
	dict(stem='Quest_SimpleUseItem', root='quest_simpleuseitems', row='id', id_kind='attr',
	     loader_required=['use_item_name', 'reward_npc_name'],
	     int_tags={'con_quest', 'cutsceneid1', 'cs1_haction'},
	     enums={'item_check': ['1']}, min_inclusive=set(),
	     zh='use_item_name 与 reward_npc_name 为装载器必填（本族无接取 NPC 列）；item_check 出现即恒 "1"。',
	     en='use_item_name and reward_npc_name are loader-mandatory (no accept-NPC column in this '
	        'family); item_check is always "1" when present.'),
	dict(stem='Quest_SimpleItemPlay', root='quest_simpleitemplays', row='id', id_kind='attr',
	     loader_required=['acquired_npc_name', 'reward_npc_name'],
	     int_tags={'con_quest', 'cutsceneid1', 'cs1_haction'},
	     enums={}, min_inclusive=set(),
	     zh='双 NPC 为装载器必填；give_item/remove_item 为第 K 中继步绑定的发放/扣除（位置保留）。',
	     en='The two-NPC pair is loader-mandatory; give/remove_itemK are step-scoped (positions kept).'),
	dict(stem='Quest_CombineTask', root='quest_combinetasks', row='id', id_kind='attr',
	     loader_required=['task_npc', 'combineskill', 'recipe_name', 'product'],
	     int_tags={'combine_skillpoint'}, enums={}, min_inclusive={'combine_skillpoint'},
	     slots={'give_component': 8},
	     zh='task_npc/combineskill/recipe_name/product 为装载器必填；combine_skillpoint 缺省 0、'
	        '出现即非负整数（负值 fail-closed）；give_component1..8 = 装载器八槽位（当前数据 1..2）。',
	     en='task_npc/combineskill/recipe_name/product are loader-mandatory; combine_skillpoint '
	        'defaults to 0 and must be a non-negative int; give_component1..8 mirror the loader\'s '
	        'eight slots (data currently uses 1..2).'),
	dict(stem='quest', root='quests', row='quest', id_kind='child', id_name='id', unique_name='name',
	     loader_required=[],
	     int_tags={'minlevel_permitted', 'maxlevel_permitted', 'max_repeat_count', 'reward_repeat_count',
	               'quest_cooltime', 'collect_progress', 'max_count_limitedquest',
	               'count_recover_limitedquest', 'use_class_reward', 'combine_skillpoint',
	               'bm_restrict_category', 'reward_gold_ext', 'reward_exp_ext'},
	     int_patterns=[r'^drop_prob_\d+$', r'^drop_each_member_\d+$',
	                   r'^reward_(gold|exp|abyss_point|glory_point|dp|cp|exp_boost|abyss_op_point)\d+$'],
	     enums={}, min_inclusive=set(),
	     zh='行 = <quest>（<id> 子元素必填、非负、全表唯一；name 全表唯一 = 装载器对非空重名 '
	        'fail-closed）；221 个子元素 = 观察全域清单；int 定型见生成器清单（= 元数据编译器 '
	        'integer() 解析点）；11 组 *_selectable_reward 容器 = 1..N 个 <data>，每 data 恰 1 件 '
	        '职业可选物品（观察形）。',
	     en='Rows are <quest> (required unique non-negative <id> child; unique name = the loader '
	        'fail-closes on duplicate non-blank names); the 221 child elements are the observed '
	        'universe; int typing mirrors the metadata compiler integer() sites; the eleven '
	        '*_selectable_reward containers hold 1..N <data>, each exactly one class item (observed).'),
	dict(stem='data_driven_quest', root='quest_data_drivens', row='quest_data_driven', id_kind='child',
	     id_name='id', loader_required=[],
	     int_tags=set(), enums={}, min_inclusive=set(),
	     progress_pattern_kinds=['hunt', 'collectitem', 'pvp', 'talk', 'enterarea', 'itemplay',
	                             'enterworld', 'talkfobj'],
	     zh='行 = <quest_data_driven>（<id> 子元素必填 = 装载器 parseInt 直解）；progress_info 内 '
	        'data = 1..N 个进度步，每步 category_progress_ 必填（8 kind 的大小写不敏感字母类模式 = '
	        '镜像装载端 lower 归一）且 value0_progress_ 必填（载荷列，缺失 fail-closed）；列号合法性'
	        '（含附加动作列）由装载器在运行期 fail-closed，XSD 1.0 不能表达。',
	     en='Rows are <quest_data_driven> (required <id> = the loader parseInts it); each '
	        'progress_info holds 1..N <data> steps whose category_progress_ is required (case-'
	        'insensitive letter-class pattern mirroring the loader lower-casing) and value0_progress_ '
	        'is required (payload column, missing = fail closed); column-legality checks live in the '
	        'loader because XSD 1.0 cannot express them.'),
	dict(stem='npcfactions_quest', root='npcfactions_quests', row='quest_id', id_kind='attr',
	     id_attr_name='quest_id', loader_required=['dev_name', 'npcfaction_name'],
	     int_tags=set(), enums={d: ['0', '1'] for d in WEEKDAYS}, min_inclusive=set(),
	     zh='真端源件副本（本仓无运行时装载器；消费面 = 阵营合同台账生成）。行 = <quest_id quest_id>'
	        '（属性必填、非负、全表唯一）；星期位 mon..sun ∈ {0,1}（全 0 = 真端本征不轮换）。',
	     en='Retail source copy (no runtime loader in this repo; consumed by the contract ledger '
	        'generation). Rows are <quest_id quest_id> (required unique non-negative attr); the '
	        'weekday flags mon..sun are {0,1} (all-zero = the retail table itself does not rotate).'),
]


def esc(text: str) -> str:
	return (text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
	        .replace('"', '&quot;'))


def letter_classes(kind: str) -> str:
	return ''.join(f'[{c.lower()}{c.upper()}]' if c.isalpha() else c for c in kind)


def validate_int_values(path, int_tags, root):
	bad = [(e.tag, e.text) for e in root.iter()
	       if e.tag in int_tags and e.text and not re.fullmatch(r'[+-]?\d+', e.text.strip())]
	if bad:
		raise SystemExit(f'FAIL {path.name}: non-numeric values for int-typed tags: {bad[:5]}')


class Spec:
	"""一个元素级的建模结果。 / One element-level modelling result."""

	def __init__(self, tag, min_occ, max_occ, kind, payload=None, children=None):
		self.tag = tag
		self.min_occ = min_occ
		self.max_occ = max_occ
		self.kind = kind          # 'string' | 'int' | 'enum' | 'pattern' | 'container'
		self.payload = payload    # minLen / minInclusive / enum values / pattern / child specs
		self.children = children or []


def child_order(parents):
	order = []
	for parent in parents:
		for child in parent:
			if child.tag not in order:
				order.append(child.tag)
	return order


def build_specs(parents, leaf_rule):
	total, parents_with, max_in_parent = Counter(), Counter(), Counter()
	for parent in parents:
		seen = Counter(child.tag for child in parent)
		for tag, n in seen.items():
			parents_with[tag] += 1
			max_in_parent[tag] = max(max_in_parent[tag], n)
	specs = []
	for tag in child_order(parents):
		min_occ = 1 if parents_with[tag] == len(parents) else 0
		max_occ = None if max_in_parent[tag] > 1 else 1
		kids = [child for parent in parents for child in parent if child.tag == tag]
		if any(len(list(kid)) for kid in kids):
			specs.append(Spec(tag, min_occ, max_occ, 'container', children=build_specs(kids, leaf_rule)))
		else:
			kind, payload = leaf_rule(tag)
			specs.append(Spec(tag, min_occ, max_occ, kind, payload))
	return specs


def emit_element(spec, indent, lines):
	occ = f'minOccurs="{spec.min_occ}"' + ('' if spec.max_occ == 1 else ' maxOccurs="unbounded"')
	if spec.kind == 'container':
		lines.append(f'{indent}<xs:element name="{spec.tag}" {occ}>')
		lines.append(f'{indent}{IND}<xs:complexType>')
		nested = indent + IND + IND
		repeats = any(child.max_occ is None for child in spec.children)
		group = 'sequence' if repeats else 'all'
		lines.append(f'{nested}<xs:{group}>')
		for child in spec.children:
			emit_element(child, nested + IND, lines)
		lines.append(f'{nested}</xs:{group}>')
		lines.append(f'{indent}{IND}</xs:complexType>')
		lines.append(f'{indent}</xs:element>')
		return
	if spec.kind == 'int':
		lines.append(f'{indent}<xs:element name="{spec.tag}" {occ}>')
		lines.append(f'{indent}{IND}<xs:simpleType>')
		lines.append(f'{indent}{IND}{IND}<xs:restriction base="xs:int">')
		if spec.payload:
			lines.append(f'{indent}{IND}{IND}{IND}<xs:minInclusive value="0"/>')
		lines.append(f'{indent}{IND}{IND}</xs:restriction>')
		lines.append(f'{indent}{IND}</xs:simpleType>')
		lines.append(f'{indent}</xs:element>')
		return
	if spec.kind == 'enum':
		lines.append(f'{indent}<xs:element name="{spec.tag}" {occ}>')
		lines.append(f'{indent}{IND}<xs:simpleType>')
		lines.append(f'{indent}{IND}{IND}<xs:restriction base="xs:string">')
		for value in spec.payload:
			lines.append(f'{indent}{IND}{IND}{IND}<xs:enumeration value="{esc(value)}"/>')
		lines.append(f'{indent}{IND}{IND}</xs:restriction>')
		lines.append(f'{indent}{IND}</xs:simpleType>')
		lines.append(f'{indent}</xs:element>')
		return
	if spec.kind == 'pattern':
		lines.append(f'{indent}<xs:element name="{spec.tag}" {occ}>')
		lines.append(f'{indent}{IND}<xs:simpleType>')
		lines.append(f'{indent}{IND}{IND}<xs:restriction base="xs:string">')
		lines.append(f'{indent}{IND}{IND}{IND}<xs:pattern value="{esc(spec.payload)}"/>')
		lines.append(f'{indent}{IND}{IND}</xs:restriction>')
		lines.append(f'{indent}{IND}</xs:simpleType>')
		lines.append(f'{indent}</xs:element>')
		return
	if spec.payload:  # non-blank required string
		lines.append(f'{indent}<xs:element name="{spec.tag}" {occ}>')
		lines.append(f'{indent}{IND}<xs:simpleType>')
		lines.append(f'{indent}{IND}{IND}<xs:restriction base="xs:string">')
		lines.append(f'{indent}{IND}{IND}{IND}<xs:whiteSpace value="collapse"/>')
		lines.append(f'{indent}{IND}{IND}{IND}<xs:minLength value="1"/>')
		lines.append(f'{indent}{IND}{IND}</xs:restriction>')
		lines.append(f'{indent}{IND}</xs:simpleType>')
		lines.append(f'{indent}</xs:element>')
		return
	lines.append(f'{indent}<xs:element name="{spec.tag}" {occ} type="xs:string"/>')


def generate(cfg):
	path = RETAIL / f"{cfg['stem']}.xml"
	root = ET.parse(path).getroot()
	assert root.tag == cfg['root'], f"{cfg['stem']}: root {root.tag} != {cfg['root']}"
	rows = list(root)
	assert rows and all(row.tag == cfg['row'] for row in rows), f"{cfg['stem']}: mixed row tags"
	for row in rows:
		tags = [child.tag for child in row]
		assert len(set(tags)) == len(tags), f'{cfg["stem"]}: duplicate child tag in one row'

	int_tags = set(cfg['int_tags'])
	for pattern in cfg.get('int_patterns', []):
		regex = re.compile(pattern)
		int_tags |= {child.tag for row in rows for child in row if regex.match(child.tag)}
	if int_tags:
		validate_int_values(path, int_tags, root)

	loader_required = set(cfg['loader_required'])
	universal = Counter(child.tag for row in rows for child in row)
	universal = {tag for tag, n in universal.items() if n == len(rows)}

	def leaf_rule(tag):
		if tag == cfg.get('id_name') and cfg['id_kind'] == 'child':
			return 'int', True
		if tag in int_tags:
			return 'int', tag in cfg.get('min_inclusive', set())
		if tag in cfg['enums']:
			return 'enum', cfg['enums'][tag]
		if cfg['stem'] == 'data_driven_quest' and tag == 'category_progress_':
			return 'pattern', '|'.join(letter_classes(k) for k in cfg['progress_pattern_kinds'])
		if tag in loader_required:
			return 'string', True  # 非空白（isBlank fail-closed）
		return 'string', False

	specs = build_specs(rows, leaf_rule)
	if cfg.get('slots'):
		existing = {spec.tag for spec in specs}
		for base, arity in cfg['slots'].items():
			for i in range(1, arity + 1):
				if f'{base}{i}' not in existing:
					specs.append(Spec(f'{base}{i}', 0, 1, 'string', False))
		specs.sort(key=lambda spec: spec.tag)

	required = universal | loader_required
	for spec in specs:
		expected = 1 if spec.tag in required else 0
		assert spec.min_occ == expected or spec.kind == 'container', \
			f'{cfg["stem"]}:{spec.tag} minOccurs {spec.min_occ} != expected {expected}'

	lines = ['<?xml version="1.0" encoding="UTF-8"?>',
	         f"<!-- {cfg['stem']}.xml 表 schema（quest retail 表门禁用，2026-10-03 DOCTYPE 剥离同批）。",
	         '     镜像口径 = 装载器 fail-closed 规则 ∪ 观察形全域钉（证据 + 生成器：',
	         '     .agents/summary/quest-engine-native/p0b/）：']
	lines += textwrap.wrap(f"- {cfg['zh']}", width=62, initial_indent='     ',
	                       subsequent_indent='       ')
	lines += textwrap.wrap(f"{cfg['stem']}.xml schema (quest retail table gate, same batch as the "
	                       "2026-10-03 DOCTYPE strip). Mirrors the loader fail-closed rules plus "
	                       "observed-universe shape pins (evidence + generator under "
	                       "`.agents/summary/quest-engine-native/p0b/`):",
	                       width=108, initial_indent='     ', subsequent_indent='     ')
	lines += textwrap.wrap(f"- {cfg['en']}", width=108, initial_indent='     ',
	                       subsequent_indent='       ')
	lines[-1] += ' -->'
	lines += ['<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified">',
	          f'{IND}<xs:element name="{cfg["root"]}">',
	          f'{IND}{IND}<xs:complexType>',
	          f'{IND}{IND}{IND}<xs:sequence>',
	          f'{IND}{IND}{IND}{IND}<xs:element name="{cfg["row"]}" minOccurs="1" maxOccurs="unbounded">',
	          f'{IND}{IND}{IND}{IND}{IND}<xs:complexType>',
	          f'{IND}{IND}{IND}{IND}{IND}{IND}<xs:all>']
	for spec in specs:
		emit_element(spec, IND * 7, lines)
	lines.append(f'{IND}{IND}{IND}{IND}{IND}{IND}</xs:all>')
	if cfg['id_kind'] == 'attr':
		attr = cfg.get('id_attr_name', 'id')
		lines += [f'{IND}{IND}{IND}{IND}{IND}{IND}<xs:attribute name="{attr}" use="required">',
		          f'{IND}{IND}{IND}{IND}{IND}{IND}{IND}<xs:simpleType>',
		          f'{IND}{IND}{IND}{IND}{IND}{IND}{IND}{IND}<xs:restriction base="xs:int">',
		          f'{IND}{IND}{IND}{IND}{IND}{IND}{IND}{IND}{IND}<xs:minInclusive value="0"/>',
		          f'{IND}{IND}{IND}{IND}{IND}{IND}{IND}{IND}</xs:restriction>',
		          f'{IND}{IND}{IND}{IND}{IND}{IND}{IND}</xs:simpleType>',
		          f'{IND}{IND}{IND}{IND}{IND}{IND}</xs:attribute>']
	lines += [f'{IND}{IND}{IND}{IND}{IND}</xs:complexType>',
	          f'{IND}{IND}{IND}{IND}</xs:element>',
	          f'{IND}{IND}{IND}</xs:sequence>',
	          f'{IND}{IND}</xs:complexType>']
	if cfg['id_kind'] == 'attr':
		key_field = f"@{cfg.get('id_attr_name', 'id')}"
	else:
		key_field = cfg.get('id_name', 'id')
	lines += unique_block(cfg['row'], key_field, f'{cfg["stem"]}-row-key-unique', IND * 2)
	if cfg.get('unique_name'):
		lines += unique_block(cfg['row'], cfg['unique_name'], f'{cfg["stem"]}-name-unique', IND * 2)
	lines += [f'{IND}</xs:element>', '</xs:schema>']

	out = RETAIL / f"{cfg['stem']}.xsd"
	out.write_text('\n'.join(lines) + '\n', encoding='utf-8')
	print(f"{cfg['stem']}.xsd: rows={len(rows)} children={len(specs)} required={len(required)} "
	      f"int-typed={len(int_tags)} containers={sum(1 for s in specs if s.kind == 'container')}")


def unique_block(selector, field, name, indent):
	return [f'{indent}<xs:unique name="{name}">',
	        f'{indent}{IND}<xs:selector xpath="{selector}"/>',
	        f'{indent}{IND}<xs:field xpath="{field}"/>',
	        f'{indent}</xs:unique>']


def main():
	for cfg in CONFIGS:
		generate(cfg)


if __name__ == '__main__':
	main()
