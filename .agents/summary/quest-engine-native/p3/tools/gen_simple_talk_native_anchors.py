#!/usr/bin/env python3
"""P3 重锚生成器：把 25 个旧 IR 形状金标改写为真端表行（native 车道）锚。

口径（计划 §8.9）：断言只来自真端表行 + 静态数据 id（npc_template / item name_desc），
不复用被测实现取数；生成一次后作为冻结金标（表行漂移即红灯）。

输入：Quest_SimpleTalk.xml、quest.xml、npc_template_*.xml、items/item/*.xml、retail-npc-name-aliases.tsv
输出：src/test/java/com/aionemu/gameserver/questEngine/definition/<Class>.java
"""
import pathlib
import re
import sys

REPO = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
DATA = REPO / 'src/main/resources/aion/data/static_data'
TALK = DATA / 'quest/retail/Quest_SimpleTalk.xml'
QUEST = DATA / 'quest/retail/quest.xml'
ALIASES = DATA / 'quest/retail/retail-npc-name-aliases.tsv'
RETENTION = DATA / 'quest/retail/retail-xml-retention.tsv'
NPCS = DATA / 'npcs'
ITEMS = DATA / 'items/item'
OUT_DIR = REPO / 'src/test/java/com/aionemu/gameserver/questEngine/definition'

# 目标类 → (包, [(questId, 原测试方法名), ...], 原意图)
XML_RETAINED = set()

TARGETS = {
    'Quest1141ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [1141], '验证任务 1141 将贝尔布亚的接取交接与酒桶报告、领奖 owner 分离。', 'keepsTheStartNpcSeparateFromTheBarrelReportAndRewardOwner'),
    'Quest1152RetailAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [1152], '接取段走 {@code canonicalAcceptFlow}；交付段现在是**规范交付**——阶段首屏 = 客户端 select2 页，', 'followsTheClientChefDialogAndLegacyTwoStepItemContract'),
    'Quest1163ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [1163], '页梯退役重锚：1163 的阶段首屏由客户端 select2 页派生，行内翻页（select2_1）是客户端本地行为', 'followsTheRetailPotionHandoffAndRewardOwner'),
    'Quest1192ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [1192], '验证任务 1192 的接取页链不会把不存在的 1012 页面回显给客户端。', 'acceptDialogChainUsesTheClientAcceptanceAction'),
    'Quest13305ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [13305], '验证任务 13305 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest13902ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [13900, 13902], '验证任务 13902 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest16922ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [16922], '验证任务 16922 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest18800ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [18800], '验证任务 18800 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest18802ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [18801, 18802], '验证任务 18802 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest1936ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [1936], '验证任务 1936 仅由内里森接取，并仅由赫凯内报告和领奖。', 'keepsTheRetailStartReportAndRewardOwnersExclusive'),
    'Quest2150ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [2150], '验证任务 2150 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest2151ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [2151], '验证任务 2151 将客户端简易接取、报告和领奖路由保留在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest23902ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [23900, 23902], '验证任务 23902 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest26922ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [26922], '验证任务 26922 将客户端报告路由限定在正式 NPC owner，修复迁移产生的双重 owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest28625And28626ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [28625, 28626], '验证 28625/28626 在卡里加陈列柜上使用钥匙的客户端对话合同。', 'reportsTheKaligaCollectionAtTheDisplayCabinet'),
    'Quest28800ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [18800, 18801, 28800, 28801], '验证任务 28800 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest28802ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [28801, 28802], '验证任务 28802 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest30312RetailAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [30312], 'Quest30312RetailAlignmentTest', 'dropsAndEveryTurnInMatchRetailItemRequirements'),
    'Quest30315RetailAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [30315], 'Quest30315RetailAlignmentTest', 'everyCertificationTurnInRequiresAndConsumesTheCertificationItems'),
    'Quest3103ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [3102, 3103], '验证任务 3103 的前置条件以及接取、报告和领奖 NPC owner 合同。', 'keepsTheRetailStartReportAndRewardOwnersExclusive'),
    'Quest4913ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [4913], '验证任务 4913 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。', 'keepsTheRetailSimpleStartReportAndRewardOwnersExclusive'),
    'Quest50009ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [50009], '验证任务 50009 将客户端标准接取与报告、领奖路由限定在正式 NPC owner，修复迁移双重 owner。', 'keepsTheRetailStandardStartReportAndRewardOwnersExclusive'),
    'Quest51009ClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [51009], '验证任务 51009 将客户端标准接取与报告、领奖路由限定在正式 NPC owner。', 'keepsTheRetailStandardStartReportAndRewardOwnersExclusive'),
    'QuestMinionTutorialRetailAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [2266, 3085, 28808], 'QuestMinionTutorialRetailAlignmentTest', 'legacyAcceptItemGrantsSurviveTheTypedMigration'),
    'QuestKaligaCollectionClientDialogAlignmentTest': ('com.aionemu.gameserver.questEngine.definition', [18618, 18619, 18620, 18621, 18622, 18623, 18624, 18625, 18626, 18627, 28618, 28619, 28620, 28621, 28622, 28623, 28624, 28625, 28626, 28627], '验证卡里加 20 个收藏品任务的阵营陈列柜和客户端对话合同。', 'allFactionsUseTheirCabinetAndClientPages'),
}


def read(path, utf16_ok=False):
    raw = path.read_bytes()
    if raw[:2] in (b'\xff\xfe', b'\xfe\xff'):
        return raw.decode('utf-16', errors='replace')
    return raw.decode('utf-8', errors='replace')


def load_xml_retained():
    """SimpleTalk 家族中仍保留 XML 定义（native 只装载不路由）的行。"""
    retained = set()
    if RETENTION.exists():
        for line in read(RETENTION).splitlines():
            if line.startswith('#'):
                continue
            parts = line.split('\t')
            if len(parts) >= 3 and parts[0].strip().isdigit() and parts[1] == 'XML_RETENTION' \
                    and parts[2] == 'SimpleTalk':
                retained.add(int(parts[0]))
    return retained


def load_table():
    text = read(TALK)
    rows = {}
    for qid, body in re.findall(r'<id id="(\d+)">(.*?)</id>', text, re.S):
        fields = {k: v.strip() for k, v in re.findall(r'<(\w+)>(.*?)</\1>', body, re.S)}
        rows[int(qid)] = fields
    return rows


def load_quests():
    text = read(QUEST)
    quests = {}
    for body in re.findall(r'<quest>(.*?)</quest>', text, re.S):
        fields = {}
        for k, v in re.findall(r'<(\w+)>(.*?)</\1>', body, re.S):
            fields.setdefault(k, []).append(v.strip())
        if 'id' in fields:
            quests[int(fields['id'][0])] = fields
    return quests


def load_npcs():
    by_desc, by_name = {}, {}
    for shard in sorted(NPCS.glob('npc_template_*.xml')):
        text = read(shard)
        for attrs in re.findall(r'<npc_template\b([^>]*)>', text):
            get = lambda key: (re.search(rf'\b{key}="([^"]*)"', attrs) or [None, None])[1]
            npc_id = get('npc_id')
            if not npc_id:
                continue
            for key, index in (('name_desc', by_desc), ('name', by_name)):
                value = (get(key) or '').strip().lower()
                if value and value != ' ':
                    index.setdefault(value, set()).add(int(npc_id))
    aliases = {}
    if ALIASES.exists():
        for line in read(ALIASES).splitlines():
            if not line.strip() or line.startswith('#'):
                continue
            parts = line.split('\t')
            if len(parts) >= 2 and parts[0].strip():
                aliases.setdefault(parts[0].strip().lower(), set()).update(
                    int(x) for x in re.findall(r'\d+', parts[1]))
    return by_desc, by_name, aliases


def load_items():
    index = {}
    for path in sorted(ITEMS.glob('*.xml')):
        text = read(path)
        for attrs in re.findall(r'<item_template\b([^>]*)>', text):
            name = re.search(r'name_desc="([^"]*)"', attrs)
            iid = re.search(r'\bid="(\d+)"', attrs)
            if name and iid:
                index.setdefault(name.group(1).strip().lower(), int(iid.group(1)))
    return index


def npc_id(resolver, name):
    by_desc, by_name, aliases = resolver
    if not name:
        return None
    key = name.strip().lower()
    for index in (by_desc, by_name, aliases):
        ids = index.get(key)
        if ids and len(ids) == 1:
            return next(iter(ids))
        if ids:
            return None
    return None


def item_stack(items, symbol):
    if not symbol:
        return None
    parts = symbol.split()
    stem = parts[0].lower()
    iid = items.get(stem)
    if iid is None and stem.startswith('item_'):
        iid = items.get(stem[len('item_'):])
    if iid is None:
        return None
    return (iid, int(parts[1]) if len(parts) > 1 else 1)


def numbered(fields, prefix):
    out = []
    for key, values in fields.items():
        m = re.fullmatch(rf'{prefix}(\d*)', key)
        if m:
            out.append((int(m.group(1)) if m.group(1) else 0, values[0]))
    return [v for _, v in sorted(out) if v]


def gate(items, quests, quest_id):
    fields = quests.get(quest_id, {})
    symbols = numbered(fields, 'collect_item') or numbered(fields, 'quest_work_item')[:1]
    stacks = [item_stack(items, s) for s in symbols]
    if not symbols:
        return [], 'EMPTY'
    if any(s is None for s in stacks):
        return [], 'FAIL_CLOSED'
    return stacks, 'RESOLVED'


def emit(cls, package, quest_ids, method, intent, data):
    rows, quests, resolver, items = data
    lines = []
    lines.append(f'package {package};')
    lines.append('')
    lines.append('import static org.junit.jupiter.api.Assertions.assertEquals;')
    lines.append('import static org.junit.jupiter.api.Assertions.assertFalse;')
    lines.append('import static org.junit.jupiter.api.Assertions.assertNull;')
    lines.append('import static org.junit.jupiter.api.Assertions.assertTrue;')
    lines.append('')
    lines.append('import java.util.List;')
    lines.append('')
    lines.append('import org.junit.jupiter.api.Test;')
    lines.append('')
    lines.append('import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;')
    lines.append('')
    lines.append('/**')
    lines.append(f' * {intent}')
    lines.append(' * <p>')
    lines.append(' * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，')
    lines.append(' * 本类改为真端表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自')
    lines.append(' * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /')
    lines.append(' * 物品 {@code name_desc}），native 处理器必须逐项一致。')
    lines.append(' * <p>')
    lines.append(' * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;')
    lines.append(' * this class now pins the retail table row through the native handler.')
    lines.append(' */')
    lines.append(f'class {cls} {{')
    lines.append('')
    lines.append('\t@Test')
    lines.append(f'\tvoid {method}() {{')
    lines.append('\t\tSimpleTalkHandler handler = SimpleTalkHandler.instance();')
    for quest_id in quest_ids:
        row = rows[quest_id]
        lines.append('')
        lines.append(f'\t\t// 真端行 {quest_id}：{row.get("acquired_npc_name", "?")} → '
                     f'{row.get("reward_npc_name", "?")}')
        if quest_id in XML_RETAINED:
            lines.append(f'\t\tassertTrue(handler.owns({quest_id}), "{quest_id} 在真端表行集内");')
            lines.append(f'\t\tassertFalse(handler.routes({quest_id}),'
                         f' "{quest_id} 仍保留 XML 定义：native 只装载不路由（单一 owner）");')
        else:
            lines.append(f'\t\tassertTrue(handler.routes({quest_id}), "{quest_id} 必须由 native 车道路由");')
        acquire = npc_id(resolver, row.get('acquired_npc_name'))
        reward = npc_id(resolver, row.get('reward_npc_name'))
        sentinel = (row.get('acquired_npc_name') or '').startswith('_')
        if acquire is not None:
            lines.append(f'\t\tassertEquals({acquire}, handler.acquireNpc({quest_id}), "接取 NPC");')
        elif sentinel:
            lines.append(f'\t\tassertNull(handler.acquireNpc({quest_id}), '
                         f'"{row.get("acquired_npc_name")} 为系统发放哨兵：无接取 NPC");')
            lines.append(f'\t\tassertTrue(handler.isSystemGranted({quest_id}), "系统发放行");')
        else:
            lines.append(f'\t\tassertNull(handler.acquireNpc({quest_id}), '
                         f'"{row.get("acquired_npc_name")} 静态解析缺口：fail-closed 无接取路由");')
        if reward is not None:
            lines.append(f'\t\tassertEquals({reward}, handler.rewardNpc({quest_id}), "交付 NPC");')
        else:
            lines.append(f'\t\tassertNull(handler.rewardNpc({quest_id}), "交付 NPC 未唯一解析（证据面冻结）");')
        if acquire is not None and reward is not None and acquire != reward:
            lines.append(f'\t\tassertFalse(handler.acquireNpc({quest_id}).equals(handler.rewardNpc({quest_id})),')
            lines.append('\t\t\t\t"接取与交付 owner 分离");')
        relays = [row.get(f'talk_npc{i}') for i in (1, 2, 3)]
        relays = [x for x in relays if x]
        lines.append(f'\t\tassertEquals({len(relays)}, handler.relayCount({quest_id}), "中继步数");')
        for index, name in enumerate(relays, start=1):
            rid = npc_id(resolver, name)
            if rid is None:
                continue
            lines.append(f'\t\tassertTrue(handler.relaysForNpc({rid}).stream()'
                         f'.anyMatch(relay -> relay.questId() == {quest_id} && relay.step() == {index}),')
            lines.append(f'\t\t\t\t"中继 {index} 必须挂在该 NPC 上: {name}");')
        accept = item_stack(items, row.get('give_item'))
        if accept is None:
            lines.append(f'\t\tassertNull(handler.acceptGiveItem({quest_id}), "接取侧无发放");')
        else:
            lines.append(f'\t\tassertEquals(new SimpleTalkHandler.ItemStack({accept[0]}, {accept[1]}),')
            lines.append(f'\t\t\t\thandler.acceptGiveItem({quest_id}), "接取侧发放");')
        for step in (1, 2, 3):
            give = item_stack(items, row.get(f'give_item{step}'))
            remove = item_stack(items, row.get(f'remove_item{step}'))
            if give:
                lines.append(f'\t\tassertEquals(new SimpleTalkHandler.ItemStack({give[0]}, {give[1]}),')
                lines.append(f'\t\t\t\thandler.stepGiveItem({quest_id}, {step}), "第 {step} 步发放");')
            if remove:
                lines.append(f'\t\tassertEquals(new SimpleTalkHandler.ItemStack({remove[0]}, {remove[1]}),')
                lines.append(f'\t\t\t\thandler.stepRemoveItem({quest_id}, {step}), "第 {step} 步扣除");')
        kinds, verdict = gate(items, quests, quest_id)
        if row.get('item_check') != '1':
            # item_check 未声明 = 该行无交付门（quest.xml 的 collect/work 通道在此不生效）。
            lines.append(f'\t\tassertTrue(handler.workItems({quest_id}).isEmpty(), '
                         f'"该行未声明 item_check：无交付门");')
            lines.append(f'\t\tassertFalse(handler.unresolvedGate({quest_id}), "无门行不得 fail-closed");')
        elif verdict == 'EMPTY':
            lines.append(f'\t\tassertTrue(handler.workItems({quest_id}).isEmpty(), "无 item_check 门");')
            if row.get('item_check') == '1':
                lines.append(f'\t\tassertTrue(handler.unresolvedGate({quest_id}), '
                             f'"门通道全缺（真端不可接取行）：fail-closed 冻结");')
            else:
                lines.append(f'\t\tassertFalse(handler.unresolvedGate({quest_id}), "非 item_check 行无门");')
        elif verdict == 'FAIL_CLOSED':
            lines.append(f'\t\tassertTrue(handler.unresolvedGate({quest_id}), "门含未解符号：fail-closed");')
            lines.append(f'\t\tassertTrue(handler.workItems({quest_id}).isEmpty(), "fail-closed 不留可解子集");')
        else:
            rendered = ', '.join(f'new SimpleTalkHandler.ItemStack({i}, {c})' for i, c in kinds)
            lines.append(f'\t\tassertEquals(List.of({rendered}), handler.workItems({quest_id}), "交付门");')
        if row.get('cutsceneid1'):
            action = row.get('cs1_haction') or '-1'
            lines.append(f'\t\tassertEquals(new SimpleTalkHandler.Cutscene({row["cutsceneid1"]}, {action}),')
            lines.append(f'\t\t\t\thandler.cutscene({quest_id}), "过场引用（cutsceneid1 / cs1_haction）");')
        else:
            lines.append(f'\t\tassertNull(handler.cutscene({quest_id}), "该行无过场");')
    lines.append('\t}')
    lines.append('}')
    lines.append('')
    path = OUT_DIR / f'{cls}.java'
    path.write_text('\n'.join(lines), encoding='utf-8')
    return path


def main():
    rows = load_table()
    quests = load_quests()
    global XML_RETAINED
    XML_RETAINED = load_xml_retained()
    resolver = load_npcs()
    items = load_items()
    data = (rows, quests, resolver, items)
    written = []
    for cls, (package, quest_ids, intent, method) in TARGETS.items():
        written.append(emit(cls, package, quest_ids, method, intent, data))
    for path in written:
        print('wrote', path.relative_to(REPO))
    return 0


if __name__ == '__main__':
    sys.exit(main())
