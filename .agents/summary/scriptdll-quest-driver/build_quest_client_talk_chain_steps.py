#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-10f/10h：SimpleTalk wave A+B 链式路由登记表（quest_client_talk_chain_steps.tsv）。

数据流（真端表权威 + 客户端背书 + XML 转写）：
  1. 行集 = p0c10e 普查 wave A（OK_A_PURE_CHAIN）+ wave B 物品轴批（B_COMPOUND 且
     真端行不含 con_quest/cutscene——这两轴留独立裁定波，出 p0c10h-chain-deferred-compound.tsv）；
  2. 逐行转写退役前 XML 的 TalkToNpc 路由（conditions/actions/after-commit 逐字编码，词汇
     fail-closed——未知元素即中止；wave B 词汇：HAS_ITEM 条件 + GIVE_ITEM 动作 +
     npc-start accept-actions 接取发物）；
  3. 交叉核验：npc_id ∈ 该行真端表解析 NPC 集（census 名字列）；show-quest-page 的页名必须
     出现在该行客户端页集（quest-dialog-pages.csv）或全局对话框常页白名单；
     物品轴计数对真端表逐行对账（give_item*/remove_item* 数量不符即 fail-closed）；
  4. 输出 TSV：quest_id / seq / npc_id / action / source / target / conditions / actions /
     after_commits / page_check。
  5. P0c-35：进度行投影裁定（p0c35-progress-row-overrides.tsv）——真端 collect_progress =
     客户端任务书 [%collectitem] 行；唯一 START 行被压成 var0=0 时按裁定投影到该行，否则运行时
     掉落门（START 且 var0==collectingStep）不可达。
  6. P0c-34：编译器级糖元素 npc-item-report（item_check 门）逐字转写为 I 记录——真端
     SimpleTalk <item_check> 标志 + 真端 quest.xml collect_item 符号经 item_name_index.tsv
     解析到同一 item-id/数量，双重背书 fail-closed（判例 QE-059：糖元素不转写则客户端
     检查按钮没有服务端路由）。

编译器只读本登记表，不读 XML（退役后登记表仍是真端表 + 客户端数据的忠实转写）。

用法：python3 -B build_quest_client_talk_chain_steps.py
"""
import re
import csv
import collections
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
CENSUS = HERE / 'p0c10e-talk-chain-census.tsv'
QDIR = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quests'
CLIENT_CSV = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-pages.csv'
CLIENT_ACTIONS = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-action-details.csv'
OUT = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv'
RETENTION = REPO / 'src/test/resources/quest/retail-xml-retention.tsv'
REWARD_REG = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv'
# P0c-46 角色轴：客户端任务书**角色列**权威（start/end/progress npc 集；198 个任务两行 ⇒ 全行并集）。
CLIENT_ROLE_INDEX = REPO / 'docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv'
# P0c-46 交付角色域（与被剪 NPC 无关的角色判定；页名与真端报告页族一致）。
ROLE_DELIVER_ACTIONS = {'SELECT_QUEST_REWARD', 'CHECK_USER_HAS_QUEST_ITEM',
                        'CHECK_USER_HAS_QUEST_ITEM_SIMPLE', 'SET_SUCCEED'}
ROLE_DELIVER_PAGE = re.compile(
    r'^(SHOW_SELECT_QUEST_REWARD_WINDOW\d*|SELECT5|SELECT6|SELECT\d*_?REWARD.*)$')
# P0c-57 接取角色域（接取动作 ∪ 接取页；被剪 NPC 的这两类记录随重复接取入口块一起剪）。
# Accept role domain: accept actions/pages pruned together with the duplicate NPC_START block.
ACCEPT_ROLE_ACTIONS = {'ASK_QUEST_ACCEPT', 'QUEST_ACCEPT_1', 'QUEST_ACCEPT_2', 'QUEST_ACCEPT_3',
                       'QUEST_ACCEPT_4', 'QUEST_ACCEPT_SIMPLE', 'QUEST_REFUSE_1', 'QUEST_REFUSE_2',
                       'QUEST_REFUSE_SIMPLE'}
# 续页动作名（`SELECT1_1`/`SELECT1_1_1` 是**对话动作**名，不在上面那组动作 id 常量里）。
# Continuation dialog action names (SELECT1_1 / SELECT1_1_1 are dialog actions, not action-id constants).
ACCEPT_ROLE_DIALOG_ACTIONS = ACCEPT_ROLE_ACTIONS | {'SELECT1_1', 'SELECT1_1_1'}
ACCEPT_ROLE_PAGE = re.compile(r'^(SELECT1|SELECT1_\d+|SHOW_ASK_QUEST_ACCEPT_WINDOW)$')

# TALK 路由：action 单数或 actions 复数（空格分隔多动作 → 逐事件出记录）；
# target 后可有额外属性（priority 等，全行集实测 55 处）——[^>]* 容忍，勿再写死 `>`。
TR = re.compile(r'<transition source="([^"]+)" target="([^"]+)"([^>]*)>\s*<event>\s*'
    r'<dialog type="TALK_TO_NPC" npc-id="(\d+)"((?:\s+(?:action|actions)="[^"]*")?)\s*/>(.*?)</transition>', re.S)
ACTIONS_ATTR = re.compile(r'(?:action|actions)="([A-Z0-9_. ]+)"')  # 含 `.`：A..B 区间 token（判例 4970）
# QA：QUEST_ACTION 无目标事件（targetless dialog action）。
QA = re.compile(r'<transition source="([^"]+)" target="([^"]+)"([^>]*)>\s*<event>\s*'
    r'<dialog type="QUEST_ACTION" action="([A-Z0-9_]+)"\s*/>(.*?)</transition>', re.S)
# I：编译器级糖元素 npc-item-report（item_check 门）。XML 期由 QuestXmlBlockExpander 展开成
# 39/20002 各成功/失败 4 条 TalkToNpc 路由；退役行必须逐字转写，否则门随 XML 一起消失（QE-059）。
IT = re.compile(r'<npc-item-report([^>]*)/>')
# E：EnterWorld 无源事件（QE-051 奖励行合同：旧存档停在错误行号，进世界时纠正并下发状态包；
# 全 9 行实测形状 = target="reward" + {status-is, variable-is} + set-variable + sync-quest-state）。
EW = re.compile(r'<transition target="([^"]+)"[^>]*>\s*<event>\s*'
    r'<enter-world/>\s*</event>(.*?)</transition>', re.S)
# 全局对话框常页（非任务信件页，客户端 CSV 不携带）：全局任务簿与引擎常页。
# 引擎常页：全局任务簿页 / 领奖窗 / 接取窗 / 报告页——非任务信件页，客户端 CSV 不携带。
GLOBAL_PAGES = {'SELECT_QUEST', 'SELECT5', 'SELECT1', 'SELECT2', 'SELECT3', 'SELECT4',
	'DEFAULT_SUCCESS',
	'SHOW_SELECT_QUEST_REWARD_WINDOW1', 'SHOW_ASK_QUEST_ACCEPT_WINDOW', 'QUEST_ACCEPT_1',
	'QUEST_REFUSE_1'}

# P0c-47：领奖窗页与其 after-commit 令牌（阶段推进行「窗外溢」收窄的唯一令牌域）。
REWARD_WINDOW_PAGE = 'SHOW_SELECT_QUEST_REWARD_WINDOW1'
WINDOW_TOKEN = 'DIALOG:SHOW_QUEST_PAGE:' + REWARD_WINDOW_PAGE

COND_ENC = {'start-eligible': 'START_ELIGIBLE'}
AFTER_ENC = {'close-dialog': 'CLOSE', 'teleport-player-current-or-default': 'TELEPORT',
	'refresh-player-stats': 'REFRESH_PLAYER_STATS'}


def fail(msg):
    print('FAIL-CLOSED: %s' % msg)
    sys.exit(1)


def quest_xml(qid):
    """转写源：生产目录在盘优先；已退役行（退役=删除）从 git HEAD 历史读取。"""
    path = QDIR / ('%d.xml' % qid)
    if path.exists():
        return path.read_text(encoding='utf-8')
    rel = path.relative_to(REPO).as_posix()
    return subprocess.run(['git', 'show', 'HEAD:' + rel],
        capture_output=True, check=True).stdout.decode('utf-8')


def encode_conditions(block, qid):
    if not block:
        return '-'
    out = []
    for m in re.finditer(r'<([a-z-]+)([^>]*)/>', block):
        name, attrs = m.group(1), m.group(2)
        if name == 'start-eligible':
            out.append('START_ELIGIBLE')
        elif name == 'status-is':
            out.append('STATUS_IS:%s' % re.search(r'status="([A-Z]+)"', attrs).group(1))
        elif name == 'has-item':
            # wave B 物品轴：XML has-item 无 expected 属性（全量实测）→ 编译器解码恒 true。
            iid = re.search(r'item-id="(\d+)"', attrs).group(1)
            cnt = re.search(r'count="(\d+)"', attrs)
            exp = re.search(r'expected="([^"]*)"', attrs)
            if exp and exp.group(1) != 'true':
                fail('%s has-item expected=%s 越界（仅支持缺省 true）' % (qid, exp.group(1)))
            out.append('HAS_ITEM:%s:%s' % (iid, cnt.group(1) if cnt else '1'))
        elif name in ('variable-is', 'variable-at-least', 'variable-below'):
            field = re.search(r'field="([^"]*)"', attrs).group(1)
            value = re.search(r'value="(-?\d+)"', attrs).group(1)
            op = {'variable-is': 'VAR_IS', 'variable-at-least': 'VAR_AT_LEAST',
                'variable-below': 'VAR_BELOW'}[name]
            out.append('%s:%s=%s' % (op, field, value))
        else:
            fail('%s 未知条件元素 <%s>' % (qid, name))
    return ';'.join(out) if out else '-'


def encode_actions(block, qid):
    if not block:
        return '-'
    out = []
    for m in re.finditer(r'<([a-z-]+)([^>]*)/>', block):
        name, attrs = m.group(1), m.group(2)
        if name == 'set-variable':
            field = re.search(r'field="([^"]*)"', attrs).group(1)
            value = re.search(r'value="(-?\d+)"', attrs).group(1)
            out.append('SET_VAR:%s=%s' % (field, value))
        elif name == 'remove-item':
            iid = re.search(r'item-id="(\d+)"', attrs).group(1)
            cnt = re.search(r'count="(\d+)"', attrs)
            out.append('REMOVE_ITEM:%s:%s' % (iid, cnt.group(1) if cnt else '1'))
        elif name == 'give-item':
            # wave B 物品轴：阶段发物（give_itemN → SETPRO 路由）与接取发物（give_item →
            # npc-start accept-actions），均逐字转写。
            iid = re.search(r'item-id="(\d+)"', attrs).group(1)
            cnt = re.search(r'count="(\d+)"', attrs)
            out.append('GIVE_ITEM:%s:%s' % (iid, cnt.group(1) if cnt else '1'))
        elif name == 'complete-quest':
            idx = re.search(r'reward-index="(-?\d+)"', attrs)
            out.append('COMPLETE_QUEST:%s' % (idx.group(1) if idx else '0'))
        elif name == 'grant-reward':
            kind = re.search(r'kind="([A-Z]+)"', attrs).group(1)
            rid = re.search(r'id="(-?\d+)"', attrs)
            amount = re.search(r'amount="(-?\d+)"', attrs).group(1)
            mode = re.search(r'amount-mode="([A-Z_]+)"', attrs)
            out.append('GRANT:%s:%s:%s:%s' % (kind, rid.group(1) if rid else '0', amount,
                mode.group(1) if mode else 'EXACT'))
        else:
            fail('%s 未知动作元素 <%s>' % (qid, name))
    return ';'.join(out) if out else '-'


def encode_after(block, qid, page_checks):
    if not block:
        return '-'
    out = []
    for m in re.finditer(r'<([a-z-]+)([^>]*)/>', block):
        name, attrs = m.group(1), m.group(2)
        if name == 'sync-quest-state':
            mode = re.search(r'mode="([A-Z_]+)"', attrs).group(1)
            out.append('SYNC:%s' % mode)
        elif name == 'close-dialog':
            out.append('CLOSE')
        elif name == 'refresh-player-stats':
            out.append('REFRESH_PLAYER_STATS')
        elif name == 'play-movie':
            mid = re.search(r'movie-id="(\d+)"', attrs)
            mtype = re.search(r'movie-type="([A-Z_]+)"', attrs)
            out.append('MOVIE:%s:%s' % (mid.group(1) if mid else '0',
                mtype.group(1) if mtype else 'CUTSCENE'))
        elif name == 'teleport-player-current-or-default':
            # 仅支持 current-or-default 变体；坐标逐字转写（编译器回放 TeleportPlayer）。
            if 'teleport-player-fixed-instance' in block or 'next-available' in block:
                fail('%s 其它 teleport 变体未支持' % qid)
            vals = [re.search(r'%s="([^"]*)"' % k, attrs).group(1)
                for k in ('world-id', 'x', 'y', 'z', 'heading')]
            out.append('TELEPORT:' + ':'.join(vals))
        elif name == 'dialog':
            # type 决定 Java 动作：SHOW_QUEST_PAGE → ShowQuestDialog；SHOW_SELECTION_PAGE →
            # ShowQuestSelectionDialog（QuestDefinitionXmlCompiler.parseDialogAfterCommit 同口径）。
            tm = re.search(r'type="([A-Z_]+)"', attrs)
            pm = re.search(r'page="([A-Z0-9_]+)"', attrs)
            if not tm or not pm:
                fail('%s after-commit dialog 缺 type/page：%s' % (qid, attrs))
            dtype, page = tm.group(1), pm.group(1)
            if dtype not in ('SHOW_QUEST_PAGE', 'SHOW_SELECTION_PAGE'):
                fail('%s after-commit dialog type 越界：%s' % (qid, dtype))
            out.append('DIALOG:%s:%s' % (dtype, page))
            page_checks.append(page)
        else:
            fail('%s 未知 after-commit 元素 <%s>' % (qid, name))
    return ';'.join(out) if out else '-'


# cs1_haction 动作 id → 枚举名（QuestDialogAction 的续页/接取子集；builder 静态快照）
HACTION_MAP = {1007: 'ASK_QUEST_ACCEPT', 1012: 'SELECT1_1', 1353: 'SELECT2_1',
    1694: 'SELECT3_1', 2035: 'SELECT4_1', 2376: 'SELECT5_1', 2717: 'SELECT6_1',
    3058: 'SELECT7_1'}


item_channel = []


def synthesize_canonical(qid, body, npc_ids_by_name, work_items, out, collect_gates=None):
    """P0c-10i：轴分歧行的 canonical 规范合成（真端表为形状权威，弃 XML 逐字转写）。

    阶梯 started→s1..sK→reward→complete（var0=阶段号，P 布局 0/6/0/63）；
    talk_npc(k) 页流（SELECT2 / SETPRO(k) 授/收）+ 报告流（SELECT_QUEST_REWARD 门控+交付消费）。
    物品 id 经 census 名字集核对（talk/reward NPC 名），item symbol 解析真端 item 名登记。
    P0c-11：collect_gates = [(item_id, count), ...]（真端 quest.xml collect_item 通道，
    item_name_index.tsv 解析）——报告/交付门物品从 collect_item 解析而非 give_item 符号集
    （判例 1932：collect_item1=quest_1932a ↔ 182206008，老 XML SETPRO1 门同物）。
    """
    acquired = re.search(r'<acquired_npc_name>([^<]*)</acquired_npc_name>', body).group(1).strip()
    reward = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', body).group(1).strip()
    # 阶段轴：give_item1/remove_item1 ... give_itemN（带后缀=阶段发物）；无编号 give_item=接取发物
    stages = []
    k = 1
    while re.search(r'<talk_npc%d>' % k, body):
        talk_name = re.search(r'<talk_npc%d>([^<]*)</talk_npc%d>' % (k, k), body).group(1).strip()
        gm = re.search(r'<give_item%d>([^<]*)</give_item%d>' % (k, k), body)
        rm = re.search(r'<remove_item%d>([^<]*)</remove_item%d>' % (k, k), body)
        stages.append((talk_name, gm.group(1) if gm else None, rm.group(1) if rm else None))
        k += 1
    K = len(stages)
    # item symbol → 生产 item id：quest_data.xml 的 quest_work_items 按序对应真端表符号
    # （判例 1131：give ITEM_QUEST_1131A→首项 182200506、give_item1 ITEM_DOC_QUEST_1131B→次项
    # 182200507；2515 三符号 ↔ 三 work_items）。尾字母（A/B/C...）定序数。
    # 本任务用到的全部符号（按字母升序）↔ quest_work_items 清单序（判例 2515：A/C/E ↔
    # 182204412/414/416——符号字母非连续但排序对齐清单）。
    symbols = sorted(set(re.findall(r'<(?:give_item\d*|remove_item\d*)>(ITEM_[A-Z0-9_]+)', body)))
    item_index = getattr(synthesize_canonical, 'item_ids_by_name', {})

    def item_id(sym):
        stem = sym.strip().split(' ')[0]
        if stem not in symbols:
            fail('%d 符号不在符号集：%s ↔ %s' % (qid, stem, symbols))
        # P0c-38：首选**真端 item 名索引**（item_name_index.tsv，符号去 ITEM_ 前缀转小写；
        # 与 P0c-11 collect_item 通道、P0c-33 判据同一口径，真端物品模板是权威）。
        # quest_data.xml 的 quest_work_items 序数对齐只是历史通道（该文件陈旧），仅在索引无解时退回。
        normalized = stem.replace('ITEM_', '').lower()
        indexed = item_index.get(normalized)
        idx = symbols.index(stem)
        positional = str(work_items[idx][0]) if idx < len(work_items) else None
        if indexed:
            if positional and positional != indexed:
                item_channel.append((qid, stem, indexed, positional, 'DIVERGED_RETAIL_WINS'))
            else:
                item_channel.append((qid, stem, indexed, positional or '-', 'MATCH' if positional else 'INDEX_ONLY'))
            return indexed
        if positional:
            item_channel.append((qid, stem, '-', positional, 'POSITIONAL_FALLBACK'))
            return positional
        fail('%d 符号两通道均无解：%s（item_name_index 与 quest_data work_items 都未命中）' % (qid, stem))
    out.append('%d\tP\t0\t6\t0\t63\tPERSISTENT\tLOCAL' % qid)
    # 阶梯：K 段 → s1..sK（var0=1..K，判例 1183：reward 与末段 sK 同 var0）。
    for label, status, var0 in ([('unaccepted', 'NONE', '0'), ('started', 'START', '0')]
            + [('s%d' % i, 'START', str(i)) for i in range(1, K + 1)]
            + [('reward', 'REWARD', str(K)), ('complete', 'COMPLETE', '0')]):
        out.append('%d\tN\t%s\t%s\t%s' % (qid, label, status, var0))
	# B 记录：NPC_START（无编号 give_item → accept-actions 接取发物）+ NPC_REPORT + NPC_COMPLETE。
    start_accepts = '-'
    g0 = re.search(r'<give_item>([^<]*)</give_item>', body)
    if g0:
        sym, _, cnt = g0.group(1).strip().rpartition(' ')
        start_accepts = 'GIVE_ITEM:%s:%s' % (item_id(sym), cnt or '1')
    def npc_id_of(name):
        ids = npc_ids_by_name.get(name.strip())
        if not ids or len(ids) != 1 or not ids[0].isdigit():
            fail('%d NPC 名解析失败（B 块）：%s → %s' % (qid, name, ids))
        return ids[0]
    out.append('%d\tB\tNPC_START\t%s\tunaccepted\tstarted\tunaccepted started|SELECT1|%s'
        % (qid, npc_id_of(acquired), start_accepts))
    out.append('%d\tB\tNPC_REPORT\t%s\ts%d\treward\tSELECT5'
        % (qid, npc_id_of(reward), K))
    # NPC_COMPLETE 从真端元数据授予固定/可选奖励。 / Grant fixed and selectable retail rewards.
    out.append('%d\tB\tNPC_COMPLETE\t%s\treward\tcomplete\tcri=0|fixed=RETAIL|actions=SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD|finish=SELECTION_DIALOG|preview=USE_OBJECT SELECT_QUEST_REWARD|choice=RETAIL|fallback=-'
        % (qid, npc_id_of(reward)))
    seq = 0
    def R(npc_name, action, source, target, conds='-', acts='-', afters='-', page_check='-', prio='-'):
        nonlocal seq
        seq += 1
        # R 列序（13 列）：qid R seq npc_id action src tgt conds acts afters npc_check page_check prio
        # npc_id 经 npc_name_index.tsv 解析（多 id/未解析 = fail-closed——canonical 行必须可路由）。
        ids = npc_ids_by_name.get(npc_name.strip())
        if not ids or len(ids) != 1 or not ids[0].isdigit():
            fail('%d NPC 名解析失败：%s → %s' % (qid, npc_name, ids))
        out.append('%d\tR\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\tNAME:%s\t%s\t%s' % (
            qid, seq, ids[0], action, source, target, conds, acts, afters, npc_name,
            page_check, prio))
    # 过场轴（P0c-10k）：cs1_haction = 触发动作 id（SELECT{n}_1 / ASK_QUEST_ACCEPT 续页按钮），
    # cutsceneid1 = movie id → 对应触发路由的 after 追加 MOVIE 边（无 haction = 传送门行 → KEEP）。
    cs = re.search(r'<cutsceneid1>(\d+)</', body)
    hact = re.search(r'<cs1_haction>(\d+)</', body)
    haction_id = hact.group(1) if hact else None
    # 接取 NPC 不持有 talk_npc 阶段页；进行态回无按钮页。 / The acquired NPC does not own talk stages.
    movie_after_qa = (cs and haction_id and HACTION_MAP.get(int(haction_id)) == 'ASK_QUEST_ACCEPT')
    qa_after = ('DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW;'
                'MOVIE:%s:CUTSCENE' % cs.group(1)) if movie_after_qa else \
        'DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW'
    R(acquired, 'QUEST_SELECT', 'started', 'started', '-', '-',
      'DIALOG:SHOW_QUEST_PAGE:DEFAULT_SUCCESS', 'DEFAULT_SUCCESS=GLOBAL')
    R(acquired, 'ASK_QUEST_ACCEPT', 'started', 'started', '-', '-', qa_after, '-')
    for i, (talk_name, give, remove) in enumerate(stages):
        src = 'started' if i == 0 else 's%d' % i
        dst = 's%d' % (i + 1)
        acts = []
        if give:
            sym, _, cnt = give.rpartition(' ')
            acts.append('GIVE_ITEM:%s:%s' % (item_id(sym), cnt or '1'))
        if remove:
            sym, _, cnt = remove.rpartition(' ')
            acts.append('REMOVE_ITEM:%s:%s' % (item_id(sym), cnt or '1'))
        # 第 i 段用客户端 SELECT(i+2)，沿真实按钮链走到 SETPRO。 /
        # Stage i follows the actual client page buttons from SELECT(i+2) to SETPRO.
        stage_page = 'SELECT%d' % (i + 2)
        advance_id = 10000 + i
        page_actions = synthesize_canonical.client_actions.get(qid, {})
        page_ids = synthesize_canonical.client_page_ids.get(qid, {})
        R(talk_name, 'QUEST_SELECT', src, src, '-', '-',
          'DIALOG:SHOW_QUEST_PAGE:' + stage_page, stage_page + '=CLIENT')
        current = stage_page
        visited = set()
        while advance_id not in page_actions.get(current.lower(), set()):
            if current in visited:
                fail('%d %s 客户端续页循环' % (qid, current))
            visited.add(current)
            next_pages = [(action, page_ids[action])
                for action in page_actions.get(current.lower(), set()) if action in page_ids]
            if len(next_pages) != 1:
                fail('%d %s 续页不能唯一定位：%s' % (qid, current, next_pages))
            next_action, next_page = next_pages[0]
            movie_bit = (';MOVIE:%s:CUTSCENE' % cs.group(1)) \
                if (cs and haction_id and int(haction_id) == next_action) else ''
            R(talk_name, str(next_action), src, src, '-', '-',
              'DIALOG:SHOW_QUEST_PAGE:' + next_page + movie_bit,
              next_page + '=CLIENT')
            current = next_page
        R(talk_name, 'SETPRO%d' % (i + 1), src, dst, '-', ';'.join(acts) if acts else '-',
          'SYNC:LEVEL_AND_VISIBILITY_REFRESH;CLOSE')
    # 报告流（reward NPC）：SELECT_QUEST_REWARD 推进到领奖窗（item_check 行带 has-item 门控）。
    # P0c-11：门物品解析通道二选一——阶段发物符号（give_itemN）或 collect_item（名字索引）；
    # collect_gates 优先（判例 1932：真端行无 give_itemN，门物品 = collect_item1 解析结果）。
    # P0c-11 门形状分派（客户端页链证据）：select6 在册 → CHECK 按钮对（判例 4056：
    # CHECK_USER_HAS_QUEST_ITEM[_SIMPLE] priority 0/1，失败下 select6 页）；无 select6 →
    # 门落 SELECT_QUEST_REWARD 路由（判例 3204/3340：priority 0 门 + priority 1 回选择页）。
    last_give = next((s[1] for s in reversed(stages) if s[1]), None)
    gate = None
    if '<item_check>' in body and collect_gates:
        gate = collect_gates[0]
    elif '<item_check>' in body and last_give:
        sym, _, cnt = last_give.rpartition(' ')
        gate = (item_id(sym), cnt or '1')
    R(reward, 'QUEST_SELECT', 'reward', 'reward', '-', '-', 'DIALOG:SHOW_QUEST_PAGE:SELECT5', 'SELECT5=GLOBAL')
    if gate and qid in getattr(synthesize_canonical, 'select6_rows', set()):
        # CHECK 按钮对（select6 失败页）：两种按钮 id 同门，运行时互为别名；priority 0/1 消歧。
        for btn in ('CHECK_USER_HAS_QUEST_ITEM', 'CHECK_USER_HAS_QUEST_ITEM_SIMPLE'):
            R(reward, btn, 'reward', 'reward', 'HAS_ITEM:%s:%s' % gate,
              'REMOVE_ITEM:%s:%s' % gate,
              'SYNC:LEVEL_AND_VISIBILITY_REFRESH;DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1',
              '-', prio='0')
            R(reward, btn, 'reward', 'reward', '-', '-',
              'DIALOG:SHOW_QUEST_PAGE:SELECT6', '-', prio='1')
    elif gate:
        R(reward, 'SELECT_QUEST_REWARD', 'reward', 'reward', 'HAS_ITEM:%s:%s' % gate,
          'REMOVE_ITEM:%s:%s' % gate,
          'SYNC:LEVEL_AND_VISIBILITY_REFRESH;DIALOG:SHOW_SELECTION_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1',
          'SHOW_SELECT_QUEST_REWARD_WINDOW1=GLOBAL', prio='0')
        R(reward, 'SELECT_QUEST_REWARD', 'reward', 'reward', '-', '-',
          'DIALOG:SHOW_SELECTION_PAGE:SELECT_QUEST', 'SELECT_QUEST=GLOBAL', prio='1')
    else:
        # 无门行保持 10i 原形状（SELECT_QUEST_REWARD 无条件推进，after 只有开窗——SYNC 由
        # 编译器 reportFlowChain 按 target 状态注入，登记表不带）。
        R(reward, 'SELECT_QUEST_REWARD', 'reward', 'reward', '-', '-',
          'DIALOG:SHOW_SELECTION_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1',
          'SHOW_SELECT_QUEST_REWARD_WINDOW1=GLOBAL')



def stage_client_chain(qid, stage, client_pages, client_actions, client_page_ids):
    """P0c-54：客户端阶段页链——入口 `SELECT{k+1}` → 续页 … → 带 `HACTION_SETPRO{k}` 的末页。

    与 `apply_talk_ladder` 内的页链推导同口径（动作集含推进 id 即末页；续页只跟目标页名以
    `SELECT` 开头的那一支；共号空间里 `FINISH_DIALOG(1008)` 等非延续动作不参与）。
    返回页名序列（含末页）；入口不在客户端页册或链不唯一/成环 ⇒ 空列表（调用方 fail-closed）。
    """
    entry = 'SELECT%d' % (stage + 1)
    if entry not in client_pages.get(qid, set()):
        return []
    advance = 10000 + stage - 1
    chain, current, visited = [], entry, set()
    while True:
        acts = client_actions.get(qid, {}).get(current.lower(), set())
        if advance in acts:
            return chain + [current]
        cont = [(a, client_page_ids[qid][a]) for a in acts
            if a in client_page_ids.get(qid, {}) and str(client_page_ids[qid][a]).startswith('SELECT')]
        if len(cont) != 1:
            return []
        chain.append(current)
        current = cont[0][1]
        if current in visited:
            return []
        visited.add(current)


def apply_talk_ladder(qid, k, mode, basis, lines, npc_ids_by_name, summary_rows,
        collect_progress, client_pages, client_actions, client_page_ids, retail_body):
    """P0c-36：把压平链行改写成中间对话行阶梯（真端 + 客户端三轴 fail-closed）。

    形状：started(START,var0=0) → talk_npc(k) 页链 → 推进 s{k}(var0=k) → … → s{K}；
    s{K} 承接收集/交付段（I 门 / QUEST_SELECT / SET_SUCCEED 的 source 由 started 迁到 s{K}）。
    推进方式由客户端页链证据决定：SETPRO = 阶段末页带 HACTION_SETPRO{k} 按钮；
    TALK = 阶段页链无推进按钮（仅 FINISH_DIALOG）→ 该次对话本身推进（QUEST_SELECT 事件）。
    """
    talks = [m.group(2).strip() for m in re.finditer(r'<talk_npc(\d)>([^<]*)</talk_npc\1>', retail_body)]
    if len(talks) != k:
        fail('%d 阶梯裁定 K=%d 与真端 talk_npc 数 %d 不符' % (qid, k, len(talks)))
    talk_ids = []
    for name in talks:
        ids = npc_ids_by_name.get(name)
        if not ids or len(ids) != 1 or not ids[0].isdigit():
            fail('%d 阶梯 talk NPC 名解析失败：%s → %s' % (qid, name, ids))
        talk_ids.append(int(ids[0]))
    if summary_rows.get(qid, 0) != k + 1:
        fail('%d 阶梯裁定：客户端任务书行数 %s != K+1=%d' % (qid, summary_rows.get(qid), k + 1))
    # 掉落轴：collect_progress==K 时阶梯的 sK 就是掉落生效行；==0 时运行时 isQuestDrop 不做 var0 校验
    # （QuestService.isQuestDrop 只在 collectingStep != 0 时比较 getQuestVarById(0)），阶梯不影响掉落。
    if collect_progress not in (0, k):
        fail('%d 阶梯裁定：真端 collect_progress %s 不在 {0,K=%d}' % (qid, collect_progress, k))
    if mode not in ('SETPRO', 'TALK'):
        fail('%d 阶梯推进方式未知：%s' % (qid, mode))
    reward = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', retail_body)
    reward_name = reward.group(1).strip() if reward else ''
    reward_ids = npc_ids_by_name.get(reward_name)
    if not reward_ids or len(reward_ids) != 1 or not reward_ids[0].isdigit():
        fail('%d 阶梯 reward NPC 名解析失败：%s' % (qid, reward_name))
    reward_id = int(reward_ids[0])

    recs = [line.split('\t') for line in lines]
    started = [r for r in recs if r[1] == 'N' and r[2] == 'started']
    if len(started) != 1 or started[0][3] != 'START':
        fail('%d 阶梯裁定：started 节点缺失或非 START' % qid)
    if started[0][4] not in ('0', str(k), '-'):
        fail('%d 阶梯裁定：started var0=%s 既非压平 0、投影 %d 也非无约束 -（裁定表腐化）'
            % (qid, started[0][4], k))
    started[0][4] = '0'

    # 阶段路由：每阶段页链 select{k+1} → … → 推进。续页只跟「延续页」动作
    # （目标页名以 SELECT 开头）；页 id 与按钮动作共号空间里的 FINISH_DIALOG(1008) 等
    # 非延续动作不参与页链（判例：1008 同时是 quest_complete 页 id）。
    if mode == 'TALK' and k != 1:
        fail('%d 阶梯 TALK 模式仅支持 K=1（对话即推进只对单阶段成立）' % qid)
    stage_lines = []
    for i in range(k):
        stage = i + 1
        src = 'started' if i == 0 else 's%d' % i
        dst = 's%d' % stage
        page = 'SELECT%d' % (stage + 1)
        if page not in client_pages.get(qid, set()):
            fail('%d 阶梯阶段 %d：客户端页 %s 不在册' % (qid, stage, page))
        advance_action = 'SETPRO%d' % stage
        advance_id = 10000 + i
        actions = client_actions.get(qid, {})
        page_ids = client_page_ids.get(qid, {})
        current = page
        visited = set()
        chain = []
        button_page = None
        while True:
            acts = actions.get(current.lower(), set())
            if advance_id in acts:
                button_page = current
                break
            cont = [(a, page_ids[a]) for a in acts
                    if a in page_ids and page_ids[a].startswith('SELECT')]
            if not cont:
                break
            if len(cont) != 1:
                fail('%d 阶梯阶段 %d：页 %s 续页不能唯一定位 %s' % (qid, stage, current, cont))
            chain.append((current, cont[0][1]))
            current = cont[0][1]
            if current in visited:
                fail('%d 阶梯阶段 %d：客户端续页循环' % (qid, stage))
            visited.add(current)
        if mode == 'SETPRO':
            if button_page is None:
                fail('%d 阶梯阶段 %d：客户端页链无 SETPRO%d 推进按钮' % (qid, stage, stage))
            stage_lines.append('%d\tR\t%d\t%s\tQUEST_SELECT\t%s\t%s\t-\t-\t%s\tRETAIL_MATCH\t%s=CLIENT\t-'
                % (qid, 0, talk_ids[i], src, src,
                   'DIALOG:SHOW_QUEST_PAGE:' + page, page))
            for from_page, to_page in chain:
                stage_lines.append('%d\tR\t%d\t%s\t%s\t%s\t%s\t-\t-\t%s\tRETAIL_MATCH\t%s=CLIENT\t-'
                    % (qid, 0, talk_ids[i], to_page, src, src,
                       'DIALOG:SHOW_QUEST_PAGE:' + to_page, to_page))
            stage_lines.append('%d\tR\t%d\t%s\t%s\t%s\t%s\t-\t-\t%s\tRETAIL_MATCH\t%s=CLIENT\t-'
                % (qid, 0, talk_ids[i], advance_action, src, dst,
                   'SYNC:LEVEL_AND_VISIBILITY_REFRESH;CLOSE', button_page))
        else:
            # TALK：单页阶段、无推进按钮 → 本次对话（QUEST_SELECT）下发该页并同时推进。
            if button_page is not None or chain:
                fail('%d 阶梯 TALK 阶段 %d 页链含推进/续页（%s/%s）' % (qid, stage, button_page, chain))
            stage_lines.append('%d\tR\t%d\t%s\tQUEST_SELECT\t%s\t%s\t-\t-\t%s\tRETAIL_MATCH\t%s=CLIENT\t-'
                % (qid, 0, talk_ids[0], 'started', dst,
                   'SYNC:LEVEL_AND_VISIBILITY_REFRESH;DIALOG:SHOW_QUEST_PAGE:' + page, page))
    # 收集/交付段迁移：source=started 的收集段记录迁到 s{K}（FINISH_DIALOG 保留 started 副本，
    # 供接取页关窗按钮路由）。
    moved = []
    extra = []
    last = 's%d' % k
    def retarget(rec, columns):
        for c in columns:
            if rec[c] == 'started':
                rec[c] = last

    for r in recs:
        if r[1] == 'I' and r[3] == 'started':
            retarget(r, (3, 4))
            moved.append('I')
        elif r[1] == 'B' and r[2] == 'NPC_REPORT' and r[4] == 'started':
            retarget(r, (4, 5))
            moved.append('B')
        elif r[1] == 'R' and r[5] == 'started' and int(r[3]) == reward_id:
            action = r[4]
            if action == 'FINISH_DIALOG':
                copy = list(r)
                retarget(copy, (5, 6))
                extra.append(copy)
                moved.append('R:FINISH_DIALOG+copy')
            else:
                retarget(r, (5, 6))
                moved.append('R:' + action)
    # 阶段节点插入（started 之后）
    node_lines = ['%d\tN\ts%d\tSTART\t%d' % (qid, i, i) for i in range(1, k + 1)]
    out_lines = []
    for r in recs:
        out_lines.append('\t'.join(r))
        if r[1] == 'N' and r[2] == 'started':
            out_lines.extend(node_lines)
    # 新增路由追加到该任务块尾（loader 顺序无关），随后按出现序重编 R seq。
    out_lines.extend(stage_lines)
    out_lines.extend('\t'.join(r) for r in extra)
    seq = 0
    for idx, line in enumerate(out_lines):
        r = line.split('\t')
        if r[1] == 'R':
            seq += 1
            r[2] = str(seq)
            out_lines[idx] = '\t'.join(r)
    print('TALK-LADDER %d：K=%d mode=%s talk=%s 迁移=%s（%s）'
        % (qid, k, mode, talk_ids, sorted(set(moved)), basis.split(';')[0]))
    return out_lines


def rebuild_stage_legs(qid, k, report_page, basis, lines, npc_ids_by_name, summary_rows,
        client_pages, client_actions, client_page_ids, client_action_constants, retail_body):
    """P0c-55：多阶段任务的**阶段腿逐阶段重建**（真端 `talk_npc<k>` + 客户端页链 + 塌缩签名三轴 fail-closed）。

    形状：XML 期把阶段腿的**阶段参数**压成了第一阶段的值——每条腿都叫 `SETPRO1`、每个阶段都下发
    `SELECT2/SELECT2_1`（HEAD 版 XML 的「对话链按客户端按钮图重建」注释即该重建模板未参数化）。真端声明
    K 个 `talk_npc<k>`、客户端每阶段各有页链（`SELECT{k+1}` → 按钮 → … → `HACTION_SETPRO{k}`），故按阶段 k：
      * 页行（owner 在该腿源状态上）：入口行页 = `SELECT{k+1}`、续页行动作+页 = `SELECT{k+1}_1`；
      * 腿动作 = `SETPRO{k}`（src→dst 不动，src == 前一跳 dst 的塌缩链本就是阶段链）；
      * 领奖页 = 客户端 `SELECT5`（唯一按钮 `HACTION_SELECT_QUEST_REWARD`），owner = 真端 `reward_npc_name`，
        状态 = packed 值 K 的节点——已有塌缩对话行则**改写其页**，否则**插入**一条。
    越界剪除（与 P0c-54 同域）：阶段页被非「阶段 owner + 源状态」的行下发、或 `SETPRO` 行既不在腿链上
    也不是领奖窗行（`SHOW_SELECT_QUEST_REWARD_WINDOW*`）⇒ 剪除；接取页（`SELECT1*`）行一律不动。
    """
    page_token = re.compile(r'DIALOG:SHOW_QUEST_PAGE:([A-Z0-9_]+)')
    page_action = re.compile(r'SELECT\d+(_\d+)?')

    def page_of(rec):
        return [m.group(1) for m in page_token.finditer(rec[9])]

    def rewrite_page(rec, page):
        tokens = [('DIALOG:SHOW_QUEST_PAGE:' + page) if page_token.fullmatch(t) else t
            for t in rec[9].split(';')]
        rec[9] = ';'.join(tokens)
        rec[11] = page + '=CLIENT'

    # 轴① 真端：talk_npc1..K 连续 + reward_npc_name 唯一解析 + 无收集段（本族全是赠礼/传话链）。
    talks = []
    for stage in range(1, k + 1):
        m = re.search(r'<talk_npc%d>([^<]*)</talk_npc%d>' % (stage, stage), retail_body)
        if not m:
            fail('%d 阶段腿重建：真端缺 talk_npc%d' % (qid, stage))
        ids = npc_ids_by_name.get(m.group(1).strip())
        if not ids or len(ids) != 1 or not ids[0].isdigit():
            fail('%d 阶段腿重建：talk_npc%d 名解析失败 %s → %s' % (qid, stage, m.group(1), ids))
        talks.append(int(ids[0]))
    rew = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', retail_body)
    rids = npc_ids_by_name.get(rew.group(1).strip()) if rew else None
    if not rids or len(rids) != 1 or not rids[0].isdigit():
        fail('%d 阶段腿重建：reward_npc_name 解析失败 %s → %s'
            % (qid, rew.group(1) if rew else '-', rids))
    reward_id = int(rids[0])
    if re.search(r'<collect_progress>\d+</collect_progress>', retail_body):
        fail('%d 阶段腿重建：真端声明 collect_progress（收集段），本通道只服务赠礼/传话链' % qid)
    # 轴② 客户端：任务书行数 K+1 + 每阶段两页链（入口/按钮页）+ 领奖页唯一按钮 = SELECT_QUEST_REWARD。
    if summary_rows.get(qid) != k + 1:
        fail('%d 阶段腿重建：客户端任务书行数 %s != K+1=%d' % (qid, summary_rows.get(qid), k + 1))
    chains = {}
    for stage in range(1, k + 1):
        chain = stage_client_chain(qid, stage, client_pages, client_actions, client_page_ids)
        if len(chain) != 2:
            fail('%d 阶段腿重建：客户端阶段 %d 页链不成立或非两页（入口+按钮页）：%s'
                % (qid, stage, chain))
        chains[stage] = chain
    report_page = report_page.upper()
    stage_pages = {p for chain in chains.values() for p in chain}
    if report_page in stage_pages or report_page not in client_pages.get(qid, set()):
        fail('%d 阶段腿重建：领奖页 %s 不在客户端页册或与阶段页同名（%s）'
            % (qid, report_page, sorted(stage_pages)))
    buttons = client_actions.get(qid, {}).get(report_page.lower(), set())
    if len(buttons) != 1:
        fail('%d 阶段腿重建：领奖页 %s 按钮不唯一：%s' % (qid, report_page, sorted(buttons)))
    constant = client_action_constants.get((qid, report_page.lower(), next(iter(buttons))))
    if constant != 'HACTION_SELECT_QUEST_REWARD':
        fail('%d 阶段腿重建：领奖页 %s 唯一按钮常量 %s != HACTION_SELECT_QUEST_REWARD'
            % (qid, report_page, constant))
    # 轴③ 登记表：腿按 owner 锚定逐跳相接（owner == talk_npc<k>，首跳 src=started），末跳 dst = packed K 节点。
    recs = [line.split('\t') for line in lines]
    nodes = {r[2]: (r[3], r[4]) for r in recs if r[1] == 'N'}
    legs, cursor = {}, 'started'
    for stage in range(1, k + 1):
        hits = [r for r in recs if r[1] == 'R' and r[3] == str(talks[stage - 1])
            and re.fullmatch(r'SETPRO\d+', r[4]) and r[5] == cursor]
        if len(hits) != 1:
            fail('%d 阶段腿重建：阶段 %d 腿行不唯一（owner=%d src=%s，%d 条）'
                % (qid, stage, talks[stage - 1], cursor, len(hits)))
        legs[stage] = hits[0]
        cursor = hits[0][6]
    if nodes.get(cursor, ('-', '-'))[1] != str(k):
        fail('%d 阶段腿重建：腿链末节点 %s packed 值 %s != K=%d（塌缩签名不符）'
            % (qid, cursor, nodes.get(cursor), k))
    legit = {(str(talks[stage - 1]), legs[stage][5]) for stage in legs}
    # 领奖行：三形互斥——①已是领奖页（无需改）②塌缩（领奖 NPC 对话行下发阶段页 ⇒ 改写其页）
    # ③缺（按 packed K 的 REWARD 节点插入一条）。
    collapse = [r for r in recs if r[1] == 'R' and r[3] == str(reward_id) and r[4] == 'QUEST_SELECT'
        and any(p in stage_pages for p in page_of(r)) and (r[3], r[5]) not in legit]
    already = [r for r in recs if r[1] == 'R' and r[3] == str(reward_id) and r[4] == 'QUEST_SELECT'
        and page_of(r) == [report_page]]
    if len(collapse) > 1 or len(already) > 1 or (collapse and already):
        fail('%d 阶段腿重建：领奖行形状不唯一（塌缩 %d 条 / 既有领奖页行 %d 条）'
            % (qid, len(collapse), len(already)))
    if already:
        report_mode = 'EXISTING'
    elif collapse:
        rewrite_page(collapse[0], report_page)
        report_mode = 'REWRITE'
    else:
        kpack = sorted(name for name, (status, packed) in nodes.items()
            if packed == str(k) and status == 'REWARD')
        if len(kpack) != 1:
            fail('%d 阶段腿重建：packed K=%d 的 REWARD 节点不唯一：%s' % (qid, k, kpack))
        recs.append([str(qid), 'R', '0', str(reward_id), 'QUEST_SELECT', kpack[0], kpack[0], '-',
            '-', 'DIALOG:SHOW_QUEST_PAGE:' + report_page, 'RETAIL_MATCH', report_page + '=CLIENT', '-'])
        report_mode = 'INSERT'
    # 逐阶段改写：页行（入口/续页按动作类分派）+ 腿动作名。
    rewritten = 0
    for stage in range(1, k + 1):
        chain = chains[stage]
        src, owner = legs[stage][5], str(talks[stage - 1])
        rows = [r for r in recs if r[1] == 'R' and r[3] == owner and r[5] == src and page_of(r)]
        entry_rows = [r for r in rows if not page_action.fullmatch(r[4])]
        cont_rows = [r for r in rows if page_action.fullmatch(r[4])]
        if not entry_rows or len(cont_rows) > 1:
            fail('%d 阶段腿重建：阶段 %d 页行形状异常（入口 %d / 续页 %d）'
                % (qid, stage, len(entry_rows), len(cont_rows)))
        for rec in entry_rows:
            if page_of(rec) != [chain[0]]:
                rewrite_page(rec, chain[0])
                rewritten += 1
        for rec in cont_rows:
            if rec[4] != chain[-1]:
                rec[4] = chain[-1]
                rewritten += 1
            if page_of(rec) != [chain[-1]]:
                rewrite_page(rec, chain[-1])
                rewritten += 1
        want_action = 'SETPRO%d' % stage
        if legs[stage][4] != want_action:
            legs[stage][4] = want_action
            rewritten += 1
    # 越界剪除（与 P0c-54 同域）：阶段页非 walk 归属行 + 非腿链/非领奖窗的 SETPRO 行。
    pruned = []
    for rec in list(recs):
        if rec[1] != 'R':
            continue
        if any(p in stage_pages for p in page_of(rec)) and (rec[3], rec[5]) not in legit:
            pruned.append(rec)
            recs.remove(rec)
            continue
        if (re.fullmatch(r'SETPRO\d+', rec[4]) and rec[5] != 'unaccepted'
                and rec not in legs.values() and 'SHOW_SELECT_QUEST_REWARD_WINDOW' not in rec[9]):
            pruned.append(rec)
            recs.remove(rec)
    # 收尾守卫①逐阶段页唯一归属：每页 owner 集 == 该阶段 talk_npc<k>；领奖页 owner == 领奖 NPC。
    emitted = collections.defaultdict(set)
    for rec in recs:
        if rec[1] != 'R':
            continue
        for page in page_of(rec):
            emitted[page].add(rec[3])
    for stage, chain in chains.items():
        for page in chain:
            if emitted.get(page) != {str(talks[stage - 1])}:
                fail('%d 阶段腿重建：阶段 %d 页 %s 的 owner 集 %s != {talk_npc%d=%d}'
                    % (qid, stage, page, sorted(emitted.get(page, set())), stage, talks[stage - 1]))
    if emitted.get(report_page) != {str(reward_id)}:
        fail('%d 阶段腿重建：领奖页 %s owner 集 %s != {reward=%d}'
            % (qid, report_page, sorted(emitted.get(report_page, set())), reward_id))
    # 收尾守卫②无页丢失：改写前的阶段/领奖页集合逐页仍被下发（剪除只剪越界行，owner 行在）。
    out_lines = ['\t'.join(rec) for rec in recs]
    seq = 0
    for idx, line in enumerate(out_lines):
        rec = line.split('\t')
        if rec[1] == 'R':
            seq += 1
            rec[2] = str(seq)
            out_lines[idx] = '\t'.join(rec)
    print('STAGE-LEG %d：K=%d talk=%s 页链=%s 腿=%s 领奖=%s@%s 改写=%d 剪除=%d（%s）'
        % (qid, k, talks, [chains[s][0] + '>' + chains[s][1] for s in sorted(chains)],
            [legs[s][4] for s in sorted(legs)], report_page, report_mode, rewritten, len(pruned),
            basis.split(';')[0]))
    return out_lines


def rebind_accept_entrance(qid, box_id, entry_action, entry_page, ask_page, ladder_pages, basis,
        lines, client_pages, client_actions, client_action_constants, client_page_constants,
        client_page_id_by_name, page_enum_ids):
    """P0c-42：物件哨兵接取者的接取阶梯改绑物件 owner（真端 + 客户端双证 fail-closed）。

    判例 1323（`acquired_npc_name=LF2_Lost_JewelBox` = 箱子 730032）：历史 XML 把接取阶梯写成无主
    `Q` 记录（QUEST_ACTION 无目标事件，npcId 记 0），且物件交互路由丢了下发页 ⇒ 客户端 `select1`
    入口页（连同接取窗页与拒绝结果页）从未被下发，玩家打不开接取窗。本变换按客户端页图把阶梯改绑
    到物件 owner：入口路由补入口页下发、紧邻插入 `ASK_QUEST_ACCEPT` → 接取窗页、无主 Q 记录按原位
    改写为绑定物件的 R 记录（页名动作 → 下发该页；`FINISH_DIALOG` → `CLOSE`）。

    `ASK_QUEST_ACCEPT` 的形状取自同族已采纳兄弟行（判例 3001/21136：`QUEST_SELECT→SELECT1` /
    `ASK_QUEST_ACCEPT→SHOW_ASK_QUEST_ACCEPT_WINDOW` / `QUEST_ACCEPT_1→QUEST_ACCEPT_1`），
    审计 `sameDialogOwner` 要求"下发页的路由"与"页按钮的路由"同 owner，故全部绑到箱子。
    """
    pages = client_pages.get(qid, set())
    actions_of = client_actions.get(qid, {})
    if entry_page not in pages:
        fail('%d 接取入口裁定：入口页 %s 不在客户端页册' % (qid, entry_page))
    if ask_page not in pages:
        fail('%d 接取入口裁定：接取窗页 %s 不在客户端页册' % (qid, ask_page))
    # 接取窗页是引擎常页：page_check 用引擎符号（HTML_PAGE_ 前缀去掉），并与枚举号互证
    # （客户端页册的页号必须等于 QuestDialogPage 的枚举号，判例 4 = SHOW_ASK_QUEST_ACCEPT_WINDOW）。
    ask_constant = client_page_constants.get((qid, ask_page), '')
    if not ask_constant.startswith('HTML_PAGE_'):
        fail('%d 接取入口裁定：接取窗页 %s 的 page_constant 异常：%s'
            % (qid, ask_page, ask_constant))
    ask_symbol = ask_constant[len('HTML_PAGE_'):]
    if page_enum_ids.get(ask_symbol) != client_page_id_by_name.get((qid, ask_page)):
        fail('%d 接取入口裁定：接取窗页 %s → 引擎页 %s 枚举号 %s != 客户端页册页号 %s'
            % (qid, ask_page, ask_symbol, page_enum_ids.get(ask_symbol),
                client_page_id_by_name.get((qid, ask_page))))
    # 轴 1：物件哨兵形状——接取不经 NPC 块（B NPC_START 缺失），阶梯只存在于无主 Q 记录。
    if any(r[1] == 'B' and r[2] == 'NPC_START' for r in map(lambda s: s.split('\t'), lines)):
        fail('%d 接取入口裁定：存在 B NPC_START 块（非物件哨兵形状）' % qid)

    def buttons(page_name):
        return actions_of.get(page_name.lower(), set())

    def only_constant(page_name, want):
        """断言该页唯一按钮的常量 = want，返回其按钮 id。 / Assert the page's only button constant."""
        ids = buttons(page_name)
        if len(ids) != 1:
            fail('%d 接取入口裁定：页 %s 按钮不唯一：%s' % (qid, page_name, sorted(ids)))
        bid = next(iter(ids))
        got = client_action_constants.get((qid, page_name.lower(), bid))
        if got != want:
            fail('%d 接取入口裁定：页 %s 按钮 %d 常量 %s != %s'
                % (qid, page_name, bid, got, want))
        return bid

    # 轴 2：入口页唯一按钮 = 打开接取窗（判例 1323 page 1011：「打开盖子看看。」）。
    only_constant(entry_page, 'HACTION_ASK_QUEST_ACCEPT')
    # 轴 3：阶梯结果页唯一按钮 = 结束对话（结果页由 FINISH_DIALOG 收尾，故两个关闭出口必须同 owner）。
    finish_id = None
    for page_name in ladder_pages:
        if page_name not in pages:
            fail('%d 接取入口裁定：阶梯结果页 %s 不在客户端页册' % (qid, page_name))
        bid = only_constant(page_name, 'HACTION_FINISH_DIALOG')
        if finish_id is None:
            finish_id = bid
        elif finish_id != bid:
            fail('%d 接取入口裁定：阶梯结果页关闭按钮不一致（%s vs %s）'
                % (qid, finish_id, bid))
    if ask_page != entry_page:
        # 接取窗页的按钮 = 阶梯页动作。客户端两套 id 空间（按钮 id / 页 id）不同号：按钮 1002
        # HACTION_QUEST_ACCEPT_1 的目标页是 1003 quest_accept_1，故按常量名校验，不按 id。
        ask_buttons = buttons(ask_page)
        if len(ask_buttons) != len(ladder_pages):
            fail('%d 接取入口裁定：接取窗页 %s 按钮数 %d != 阶梯页数 %d'
                % (qid, ask_page, len(ask_buttons), len(ladder_pages)))
        got = sorted(client_action_constants.get((qid, ask_page.lower(), b)) for b in ask_buttons)
        want = sorted('HACTION_' + p for p in ladder_pages)
        if got != want:
            fail('%d 接取入口裁定：接取窗页 %s 按钮常量 %s != 阶梯页动作 %s'
                % (qid, ask_page, got, want))

    recs = [line.split('\t') for line in lines]
    # 轴 4：物件交互路由唯一且丢了下发页。
    entry_route = [r for r in recs if r[1] == 'R' and r[3] == str(box_id) and r[4] == entry_action]
    if len(entry_route) != 1:
        fail('%d 接取入口裁定：物件 %d 的 %s 路由不唯一：%d 条'
            % (qid, box_id, entry_action, len(entry_route)))
    entry_row = entry_route[0]
    if entry_row[9] != '-' or entry_row[11] != '-':
        fail('%d 接取入口裁定：%s 路由已带下发页（after=%s page_check=%s）——裁定表腐化或重复应用'
            % (qid, entry_action, entry_row[9], entry_row[11]))
    # 轴 5：无主 Q 记录动作集 = 阶梯页名集 ∪ {FINISH_DIALOG × |阶梯页|}；关闭出口的 status 与
    #       阶梯页目标 status 一一对应（结果页靠关闭出口收尾）。
    unowned = [r for r in recs if r[1] == 'Q']
    page_rows = [r for r in unowned if r[3] in ladder_pages]
    finish_rows = [r for r in unowned if r[3] == 'FINISH_DIALOG']
    others = [r for r in unowned if r[3] not in ladder_pages and r[3] != 'FINISH_DIALOG']
    if others or len(page_rows) != len(ladder_pages) or len(finish_rows) != len(ladder_pages):
        fail('%d 接取入口裁定：无主 Q 记录形状不符（页行 %s / 关闭行 %d / 其余 %s）'
            % (qid, [r[3] for r in page_rows], len(finish_rows), [r[3] for r in others]))
    if sorted(set(r[3] for r in page_rows)) != sorted(ladder_pages):
        fail('%d 接取入口裁定：无主 Q 阶梯页 %s != 裁定表 %s'
            % (qid, sorted(set(r[3] for r in page_rows)), sorted(ladder_pages)))
    for r in finish_rows:
        if r[4] != r[5]:
            fail('%d 接取入口裁定：关闭出口 %s→%s 跨状态（结果页关闭不迁移状态）' % (qid, r[4], r[5]))
    if sorted(r[4] for r in finish_rows) != sorted(set(r[5] for r in page_rows)):
        fail('%d 接取入口裁定：关闭出口 status %s != 阶梯页目标 status %s'
            % (qid, sorted(r[4] for r in finish_rows), sorted(set(r[5] for r in page_rows))))
    if entry_row[6] != entry_row[5]:
        fail('%d 接取入口裁定：入口路由 %s→%s 跨状态' % (qid, entry_row[5], entry_row[6]))
    if entry_row[6] not in [r[4] for r in finish_rows] + [r[4] for r in page_rows]:
        fail('%d 接取入口裁定：入口路由目标 status %s 不在无主 Q 记录的源 status 内'
            % (qid, entry_row[6]))

    # 改写：无主 Q 记录按原位转 R；入口路由补下发页；ASK 路由紧邻入口路由插入。
    def to_route(row, page_name):
        """Q 记录 → 绑定物件的 R 记录（页名动作下发该页；CLOSE 动作的关闭出口无页）。"""
        if not page_name:
            return [row[0], 'R', row[2], str(box_id), row[3], row[4], row[5], row[6], row[7],
                row[8], 'RETAIL_MATCH', '-', row[9]]
        # 页行：旧 after 的去重保留（同步轴等），去掉 CLOSE 与旧页下发，再补本页下发。
        after = [t for t in row[8].split(';') if t and t != 'CLOSE'
            and not t.startswith('DIALOG:SHOW_QUEST_PAGE:')]
        after.append('DIALOG:SHOW_QUEST_PAGE:' + page_name)
        return [row[0], 'R', row[2], str(box_id), row[3], row[4], row[5], row[6], row[7],
            ';'.join(after) or '-', 'RETAIL_MATCH', '%s=CLIENT' % page_name, row[9]]

    out_lines = []
    inserted = 0
    for r in recs:
        if r is entry_row:
            r[9] = 'DIALOG:SHOW_QUEST_PAGE:' + entry_page
            r[11] = '%s=CLIENT' % entry_page
            out_lines.append('\t'.join(r))
            out_lines.append('\t'.join(['%d' % qid, 'R', '-', str(box_id), 'ASK_QUEST_ACCEPT',
                r[5], r[6], '-', '-', 'DIALOG:SHOW_QUEST_PAGE:' + ask_symbol, 'RETAIL_MATCH',
                '%s=GLOBAL' % ask_symbol, '-']))
            inserted += 1
        elif r[1] == 'Q':
            page_name = r[3] if r[3] in ladder_pages else None
            out_lines.append('\t'.join(to_route(r, page_name)))
        else:
            out_lines.append('\t'.join(r))
    if inserted != 1:
        fail('%d 接取入口裁定：ASK 路由插入点异常（inserted=%d）' % (qid, inserted))
    seq = 0
    for idx, line in enumerate(out_lines):
        r = line.split('\t')
        if r[1] == 'R':
            seq += 1
            r[2] = str(seq)
            out_lines[idx] = '\t'.join(r)
    print('ACCEPT-ENTRANCE %d：物件 %d 承载 %s → 入口页 %s / 接取窗页 %s（引擎 %s）/ 阶梯 %s'
        '（R 行 %d；%s）'
        % (qid, box_id, entry_action, entry_page, ask_page, ask_symbol, ladder_pages, seq, basis))
    return out_lines


def main():
    wave_a = []
    wave_b_all = []
    census_names = {}
    census_retail = {}
    census_client = {}
    # P0c-44：census 的 DEVIATION 列（`动作@npc_id` 列表）= 转写形**自己承认**的"该 NPC 在真端
    # 声明集之外服务了动作"——改道裁定的第三方漂移形必须在这一列里有记录（fail-closed 取证）。
    census_deviations = {}
    for line in CENSUS.read_text(encoding='utf-8').splitlines():
        if line.startswith('#'):
            continue
        p = line.split('\t')
        census_deviations[int(p[0])] = set(re.findall(r'@(\d+)', p[6]))
        if p[2] == 'A_PURE_CHAIN' and p[7] == 'OK_A_PURE_CHAIN':
            wave_a.append(int(p[0]))
        elif p[2] == 'B_COMPOUND' and p[7] == 'OK_B_COMPOUND':
            wave_b_all.append(int(p[0]))
        else:
            continue
        # P0c-45：声明集**分通道**——`names` 列有三种形态：纯 id（真端名解析）、`SENTINEL:x`（类别
        # 哨兵，非 NPC）、`CLIENT:a,b`（**复合条目**：客户端交付登记 / 模板索引解析出的多 id）。
        # 旧口径把复合条目当整串比较 ⇒ `'799800' not in {...,'CLIENT:799800,799801'}` 恒真，
        # 93 行被误标 XML_ONLY（判例 QE-074，普查 p0c45_xml_only_npc_census.py v2）。现在：
        # 真端通道记 RETAIL_MATCH、客户端通道记 CLIENT_MATCH、两者皆非才记 XML_ONLY；
        # 守卫（改道/剪除）用**两者并集**——声明证据 = 真端 ∪ 客户端。
        retail_ids, client_ids = set(), set()
        for entry in p[3].split('|'):
            if entry.startswith('CLIENT:'):
                client_ids |= {s.strip() for s in entry[len('CLIENT:'):].split(',') if s.strip().isdigit()}
            elif entry.startswith('SENTINEL:') or not entry.isdigit():
                continue
            else:
                retail_ids.add(entry)
        census_retail[int(p[0])] = retail_ids
        census_client[int(p[0])] = client_ids
        census_names[int(p[0])] = retail_ids | client_ids
    wave_a.sort()
    # wave B 第一批 = 仅物品轴（give/remove/item_check）；con_quest / cutscene 行留裁定波。
    retail_body = {int(m.group(1)): m.group(2) for m in re.finditer(
        r'<id id="(\d+)">(.*?)</id>',
        (REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml')
        .read_text(encoding='utf-8'), re.S)}
    wave_b = []
    deferred_b = []
    for qid in wave_b_all:
        body = retail_body.get(qid, '')
        if '<con_quest>' in body or '<cutscene' in body:
            deferred_b.append(qid)
        else:
            wave_b.append(qid)
    wave_b.sort()
    b_set = set(wave_b)
    # KEEP 行：登记表只是快照（XML 是 owner），页证据不作退役判据；wave B 新行页未核验进裁定清单。
    keep_set = set()
    for line in (HERE / 'p0c10f-talk-chain-decisions.tsv').read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p = line.split('\t')
        if p[1] == 'KEEP_XML':
            keep_set.add(int(p[0]))
    unverified_pages = []
    axis_mismatch = []
    # P0c-10i：轴分歧行（历史微波中被跳过转写的行）改走 canonical 规范合成——
    # 真端表为形状权威（阶段绑定授/收 + 交付消费），XML 只作页名与 NPC 对照素材。
    canonical_pending = set()
    for line in (HERE / 'p0c10h-chain-axis-mismatch.tsv').read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        qid = int(line.strip())
        body = retail_body.get(qid, '')
        if '<con_quest>' in body or '<cutscene' in body:
            continue  # deferred 波另行裁定
        canonical_pending.add(qid)
    # P0c-10j：con_quest 裁定 ADOPT 行（前置语义由后继主表 cond 承载）也走 canonical 合成。
    for line in (HERE / 'p0c10j-deferred-decisions.tsv').read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p = line.split('\t')
        if p[1] == 'ADOPT_RETAIL':
            canonical_pending.add(int(p[0]))
    # P0c-11：阶段门链行（collect_item + SETPRO 门）——B_COMPOUND 残组中门物品走 collect_item
    # 名字索引通道的行（判例 1932：10i KEEP 因 give_item 符号集为空；门物品实从
    # 真端 quest.xml collect_item1 经 item_name_index.tsv 解析，老 XML SETPRO1 门同物）。
    collect_gate_pending = set()
    for line in (HERE / 'p0c11-collect-gate-decisions.tsv').read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p = line.split('\t')
        if p[1] == 'ADOPT_RETAIL':
            collect_gate_pending.add(int(p[0]))
    print('canonical 待合成（P0c-10i）：%d 行；P0c-11 阶段门批：%d 行'
        % (len(canonical_pending), len(collect_gate_pending)))
    npc_ids_by_name = {}
    for line in (HERE / 'npc_name_index.tsv').read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or line.startswith('name_desc') or not line.strip():
            continue
        p = line.rstrip('\n').split('\t')
        npc_ids_by_name[p[0]] = p[1].split('|')
    # P0c-11：collect_item 门物品通道——item_name_index.tsv（与 RetailItemNameIndex 同口径）
    # + 真端 quest.xml collect_itemN 数量。
    item_ids_by_name = {}
    for line in (HERE / 'item_name_index.tsv').read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or line.startswith('name_desc') or not line.strip():
            continue
        p = line.rstrip('\n').split('\t')
        item_ids_by_name[p[0]] = p[1]
    # P0c-34：失败页符号域 = QuestDialogPage 枚举常量名（与 QuestDialogXmlCompiler.dialogPageSymbol
    # 同一解析口径；未知符号在离线编译期同样 fail）。
    page_symbols = set(re.findall(r'^\t([A-Z][A-Z0-9_]*)\(\d+\)',
        (REPO / 'src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogPage.java')
        .read_text(encoding='utf-8'), re.M))
    if len(page_symbols) < 100:
        fail('QuestDialogPage 符号域解析失败：%d' % len(page_symbols))
    # 引擎页符号 → 枚举号（P0c-42 接取窗页互证用）。
    page_enum_ids = {s: int(i) for s, i in re.findall(r'^\t([A-Z][A-Z0-9_]*)\((\d+)\)',
        (REPO / 'src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogPage.java')
        .read_text(encoding='utf-8'), re.M)}
    item_report_crosscheck = []
    # P0c-34：REWARD 投影裁定表（1479：登记表持客户端值、退役 XML 无法回写）+ 客户端任务书行数
    # 通道（quest_client_summary_rows.tsv，QE-051 权威）。覆盖值必须等于客户端末行行号，否则 fail。
    reward_row_overrides = {}
    override_file = HERE / 'p0c34-chain-reward-row-overrides.tsv'
    if override_file.exists():
        for line in override_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p2 = line.split('\t')
            reward_row_overrides[int(p2[0])] = (int(p2[1]), p2[2])
    client_summary_rows = {}
    for line in (REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv'
            ).read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p3 = line.split('\t')
        client_summary_rows[int(p3[0])] = int(p3[1])
    for qid, (value, basis) in reward_row_overrides.items():
        if qid not in client_summary_rows:
            fail('%d REWARD 投影裁定无客户端任务书行数背书' % qid)
        if client_summary_rows[qid] - 1 != value:
            fail('%d REWARD 投影裁定 %d != 客户端末行行号 %d（裁定表腐化）'
                % (qid, value, client_summary_rows[qid] - 1))
    # P0c-36：中间对话行阶梯裁定表（真端 talk_npcK + 客户端任务书行命名 + collect_progress 三轴）。
    # XML 期把多行任务书压成单个 started(var0=0)；本表把这些行改写成逐行阶梯：
    # accept → started(0) → talk_npc(1) 页链 → s1(1) → … → s{K}(K)（s{K} 承接收集/交付段）。
    # Talk-ladder adjudication: flatten rows are rewritten into the per-journal-row ladder.
    ladder_rows = {}
    # 阶梯裁定表按批分文件（P0c-36 首批、P0c-37 余量分批）；同一任务跨表出现即冲突（避免两套裁量）。
    for ladder_name in ('p0c36-talk-ladder-decisions.tsv', 'p0c37-talk-ladder-decisions.tsv'):
        ladder_file = HERE / ladder_name
        if not ladder_file.exists():
            continue
        for line in ladder_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p6 = line.split('\t')
            if int(p6[0]) in ladder_rows:
                fail('%d 阶梯裁定在 %s 与既有裁定表重复' % (int(p6[0]), ladder_name))
            ladder_rows[int(p6[0])] = (int(p6[1]), p6[2], p6[3])
    # P0c-42：接取入口重绑裁定表（物件哨兵接取者：无主 Q 阶梯改绑物件 owner + 补入口页下发）。
    # 与阶梯表互斥——两者改写的都是接取段形状，同任务双表即两套裁量。
    accept_entrance_rows = {}
    accept_file = HERE / 'p0c42-accept-entrance-decisions.tsv'
    if accept_file.exists():
        for line in accept_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p7 = line.split('\t')
            qid = int(p7[0])
            if qid in ladder_rows:
                fail('%d 同时出现在阶梯裁定表与接取入口裁定表（形状冲突）' % qid)
            accept_entrance_rows[qid] = (int(p7[1]), p7[2], p7[3], p7[4], p7[5].split(','), p7[6])
    # P0c-55：阶段腿逐阶段重建裁定表（真端 talk_npc<k> + 客户端页链 + 登记表塌缩签名三轴）。
    # 与阶梯表互斥（两者重写的都是阶段段形状，同任务双表即两套裁量）；与接取入口表**可共存**——
    # 该表只改接取段（物件哨兵 owner 上的 SELECT1/ASK_QUEST_ACCEPT/QUEST_ACCEPT_1 系），本表只改阶段/
    # 领奖段（talk_npc<k> 与 reward NPC 上的 SELECT{k+1}/SETPRO{k}/SELECT5 系），两域页与动作均不相交
    # （1323 同时受两表裁定：接取入口重绑 + 领奖 NPC 上的阶段行剪除）。
    stage_leg_rows = {}
    stage_leg_file = HERE / 'p0c55-stage-leg-decisions.tsv'
    if stage_leg_file.exists():
        for line in stage_leg_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p8 = line.split('\t')
            qid = int(p8[0])
            if qid in ladder_rows:
                fail('%d 同时出现在阶梯裁定表与阶段腿重建表（形状冲突）' % qid)
            stage_leg_rows[qid] = (int(p8[1]), p8[2], p8[3])
    reward_projection_rows = []
    # P0c-35：进度行投影裁定表。真端 collect_progress = 任务书上掉落生效行（客户端 quest_summary 带
    # [%collectitem] 的行），运行时掉落门 QuestService.isQuestDrop 要求 status=START 且 var0==collectingStep；
    # 唯一 START 行被压成 var0=0 的行会让掉落不可达（门物品绝版）。裁定把该 START 行的 var0 投影到
    # collect_progress，fail-closed 校验：行存在、XML 值等于登记的旧值、collect_progress>0、
    # 客户端末行行号 == collect_progress（掉落生效行就是交付行）、该行有 drop 轴或 item_check 门。
    progress_row_overrides = {}
    progress_file = HERE / 'p0c35-progress-row-overrides.tsv'
    if progress_file.exists():
        for line in progress_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p4 = line.split('\t')
            progress_row_overrides[(int(p4[0]), p4[1])] = (p4[2], int(p4[3]), p4[4])
    retail_quest_xml_text = (REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest.xml').read_text(encoding='utf-8')
    collect_gates = {}
    for qid in collect_gate_pending:
        qm = re.search(r'<quest>\s*<id>%d</id>.*?</quest>' % qid, retail_quest_xml_text, re.S)
        gates = []
        if qm:
            for sym, cnt in re.findall(r'<collect_item\d*>(\S+)(?:\s+(\d+))?\s*</collect_item\d*>', qm.group(0)):
                iid = item_ids_by_name.get(sym.strip().lower())
                if not iid:
                    fail('%d collect_item 符号解析失败：%s' % (qid, sym))
                gates.append((iid, cnt or '1'))
        if not gates:
            fail('%d collect_item 通道为空（裁定表与真端数据不符）' % qid)
        collect_gates[qid] = gates
    quest_data = (REPO / 'src/main/resources/aion/data/static_data/quest_data/quest_data.xml').read_text(encoding='utf-8')
    canonical_work_items = {}
    for qid in canonical_pending:
        qm = re.search(r'<quest id="%d"[^>]*>(.*?)</quest>' % qid, quest_data, re.S)
        canonical_work_items[qid] = [] if not qm else [(int(i), int(c)) for i, c in re.findall(
            r'<quest_work_item item_id="(\d+)" count="(\d+)"', qm.group(1))]
    print('wave A 行：%d；wave B 物品轴批：%d；deferred(con_quest/cutscene)：%d'
        % (len(wave_a), len(wave_b), len(deferred_b)))

    client_pages = collections.defaultdict(set)
    client_page_ids = collections.defaultdict(dict)
    # P0c-42 接取入口重绑的引擎页取证：页名 → page_constant（HTML_PAGE_*）与页号。
    client_page_constants = {}
    client_page_id_by_name = {}
    with CLIENT_CSV.open(encoding='utf-8-sig') as fh:
        header = fh.readline().rstrip('\n').split(',')
        col = {name: i for i, name in enumerate(header)}
        for line in fh:
            p = line.rstrip('\n').split(',')
            if len(p) >= len(header):
                qid = int(p[col['quest_id']])
                page = p[col['html_page_name']].upper()
                client_pages[qid].add(page)
                client_page_constants[(qid, page)] = p[col['page_constant']]
                if p[col['page_id']].isdigit():
                    client_page_ids[qid][int(p[col['page_id']])] = page
                    client_page_id_by_name[(qid, page)] = int(p[col['page_id']])
    # P0c-11 门形状分派：客户端任务书带 select6 页的行走 CHECK 按钮对（判例 4056）。
    synthesize_canonical.select6_rows = {q for q, pages in client_pages.items() if 'SELECT6' in pages}
    synthesize_canonical.client_page_ids = client_page_ids
    client_actions = collections.defaultdict(lambda: collections.defaultdict(set))
    # 按钮常量（P0c-42 接取入口重绑按动作语义取证：HACTION_ASK_QUEST_ACCEPT / HACTION_FINISH_DIALOG）。
    client_action_constants = {}
    with CLIENT_ACTIONS.open(encoding='utf-8-sig', newline='') as fh:
        for row in csv.DictReader(fh):
            if (row['source_variant'] == 'active' and row['page_mapping'] == 'exact'
                    and row['action_id'].isdigit()):
                client_actions[int(row['quest_id'])][row['html_page_name'].lower()].add(
                    int(row['action_id']))
                client_action_constants[(int(row['quest_id']), row['html_page_name'].lower(),
                    int(row['action_id']))] = row['action_constant']
    synthesize_canonical.client_actions = client_actions
    synthesize_canonical.item_ids_by_name = item_ids_by_name

    def resolve_npc(qid_, name, axis):
        """NPC 名 → 唯一 id（改道裁定的 NPC 轴取证；多 id/无解 = fail-closed）。"""
        ids = npc_ids_by_name.get(name.strip())
        if not ids or len(ids) != 1 or not ids[0].isdigit():
            fail('%d 改道裁定 %s：NPC 名解析失败 %s → %s' % (qid_, axis, name, ids))
        return ids[0]
    # 客户端任务书角色列（start/end/progress NPC 集，全行并集）——P0c-46 角色收窄与 P0c-47 改道
    # code 共用；在这里提前装载（canonical 装载段要按 code 用 end 轴）。
    # Client journal role columns (union over all rows) shared by the P0c-46 role-narrowing and the
    # P0c-47 canonical code axes; loaded before the canonical table so the end axis is available.
    client_roles = collections.defaultdict(lambda: {'start': set(), 'end': set(), 'progress': set()})
    if CLIENT_ROLE_INDEX.exists():
        for r in csv.DictReader(CLIENT_ROLE_INDEX.open(encoding='utf-8-sig')):
            q4 = int(r['quest_id'])
            for col, key in (('start_npc_ids', 'start'), ('end_npc_ids', 'end'),
                             ('progress_npc_ids', 'progress')):
                client_roles[q4][key] |= {int(x) for x in re.findall(r'\d+', r[col] or '')}
    # P0c-43/44：canonical 改道裁定表——遗留 XML 转写在**结构轴**上与真端分歧（零阶段路由 +
    # 阶段/报告 NPC 不是真端声明的人）⇒ 弃逐字转写，按真端规范合成。与阶梯/接取入口两表互斥
    # （改写的都是同一批形状，同任务双表即两套裁量）。三个已观测漂移形：
    # （改写的都是同一批形状，同任务双表即两套裁量）。三个已观测漂移形：
    #   `LEGACY_REPORT_FLOW_ON_ACQUIRED`（判例 24123）：报告/完成流**全绑接取 NPC**；
    #   `LEGACY_STAGE_OWNER_DRIFT`（判例 2482）：阶段/报告 flow 绑**真端声明集之外的第三方 NPC**
    #   （转写时已被 census 记进 DEVIATION 列、R 行 npc_check 记 XML_ONLY）；
    #   `LEGACY_DELIVER_FLOW_ON_TALKNPC`（P0c-47 判例 2964）：真端 reward == acquired 的行，转写把
    #   交付流绑到**声明的阶段 NPC**（talk_npcK）上（P0c-46 角色域投影判 DELIVER_UNCOVERED）。
    # 共用真端轴：③acquired 唯一解析；④`reward_npc_name` 与裁定表一致、唯一解析且 ≠ acquired
    # （否则无需改道；`..._ON_TALKNPC` 行反向要求 == acquired 且客户端 end 同值）；
    # ⑤每个 talk_npcK ≠ acquired（阶段归属必须换人，否则阶梯自环）；⑥客户端任务书行数 == K+1
    # （QE-051 投影判据）；⑦客户端页册必须有 SELECT{i+2}（i=0..K-1）。
    # 形判轴（按 code）：①code 域；②转写报告 owner 必须符合该 code 的漂移形（全绑接取 / 第三方且
    # 不在真端表 NPC 集、且 census DEVIATION 列记过它 / 等于某个 talk_npcK 阶段 NPC）。
    canonical_resynthesis = {}
    syn_file = HERE / 'p0c43-canonical-resynthesis.tsv'
    if syn_file.exists():
        for line in syn_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p8 = line.split('\t')
            qid = int(p8[0])
            code, legacy_npc, retail_npc = p8[1], p8[2], p8[3]
            if code not in ('LEGACY_REPORT_FLOW_ON_ACQUIRED', 'LEGACY_STAGE_OWNER_DRIFT',
                    'LEGACY_DELIVER_FLOW_ON_TALKNPC'):
                fail('%d 改道裁定 code 不在登记域：%s' % (qid, code))
            if qid in ladder_rows or qid in accept_entrance_rows:
                fail('%d 同时出现在改道裁定表与阶梯/接取入口裁定表（形状冲突）' % qid)
            body = retail_body.get(qid, '')
            if not re.search(r'<acquired_npc_name>', body):
                fail('%d 改道裁定：真端表无该行' % qid)
            acquired_name = re.search(r'<acquired_npc_name>([^<]*)</acquired_npc_name>', body).group(1).strip()
            reward_name = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', body).group(1).strip()
            acquired_id = resolve_npc(qid, acquired_name, 'acquired')
            if reward_name != retail_npc:
                fail('%d 改道裁定：真端 reward_npc_name=%s ≠ 裁定表 %s' % (qid, reward_name, retail_npc))
            reward_id = resolve_npc(qid, reward_name, 'retail_reward')
            if code == 'LEGACY_DELIVER_FLOW_ON_TALKNPC':
                # P0c-47 反向形：真端 reward == acquired（交付 owner 即接取 NPC），客户端 end（若有登记）
                # 必须是同一唯一 id——交付 owner 三源（真端 reward / 客户端 end / 裁定表）一致才改道。
                if reward_id != acquired_id:
                    fail('%d 改道裁定 %s：真端 reward NPC(%s) ≠ 接取 NPC(%s)——非本 code 形状'
                        % (qid, code, reward_id, acquired_id))
                cend = client_roles.get(qid, {}).get('end', set())
                # 类型对齐（QE-076）：client_roles 是 int 集，resolve_npc 返回 str——不转则守卫恒真。
                if cend and cend != {int(reward_id)}:
                    fail('%d 改道裁定 %s：客户端任务书 end=%s 非单值 == owner(%s)'
                        % (qid, code, sorted(cend), reward_id))
            elif reward_id == acquired_id:
                fail('%d 改道裁定：真端 reward NPC 与接取 NPC 同人（%s）——无需改道' % (qid, reward_id))
            stage_names = [m.group(1).strip() for m in
                (re.search(r'<talk_npc%d>([^<]*)</talk_npc%d>' % (k, k), body) for k in range(1, 64)) if m]
            if not stage_names:
                fail('%d 改道裁定：真端无 talk_npc1（非阶段轴形状）' % qid)
            stage_ids = [resolve_npc(qid, name, 'talk_npc%d' % (idx + 1))
                for idx, name in enumerate(stage_names)]
            for idx, stage_id in enumerate(stage_ids, 1):
                if stage_id == acquired_id:
                    fail('%d 改道裁定：talk_npc%d=%s 即接取 NPC(%s)——阶梯自环'
                        % (qid, idx, stage_names[idx - 1], acquired_id))
            k_stages = len(stage_names)
            if client_summary_rows.get(qid) != k_stages + 1:
                fail('%d 改道裁定：客户端任务书行数 %s ≠ K+1=%d（QE-051）'
                    % (qid, client_summary_rows.get(qid), k_stages + 1))
            for i in range(k_stages):
                stage_page = 'SELECT%d' % (i + 2)
                if stage_page not in client_pages.get(qid, set()):
                    fail('%d 改道裁定：阶段 %d 客户端页 %s 不在册' % (qid, i + 1, stage_page))
            legacy_id = resolve_npc(qid, legacy_npc, 'legacy_report')
            declared = {acquired_id, reward_id} | set(stage_ids)
            if code == 'LEGACY_REPORT_FLOW_ON_ACQUIRED':
                if legacy_id != acquired_id:
                    fail('%d 改道裁定：转写报告 NPC %s ≠ 接取 NPC %s(%s)——非本 code 的漂移形'
                        % (qid, legacy_npc, acquired_name, acquired_id))
            elif code == 'LEGACY_DELIVER_FLOW_ON_TALKNPC':
                # 转写交付 owner 必须是**真端声明的阶段 NPC**（talk_npcK）：与"第三方漂移"（声明集之外）
                # 和"全绑接取"（= acquired）两形互斥，且是 P0c-46 DELIVER_UNCOVERED 的结构判据。
                if legacy_id == acquired_id:
                    fail('%d 改道裁定 %s：转写交付 owner 即接取 NPC(%s)——应走 LEGACY_REPORT_FLOW_ON_ACQUIRED'
                        % (qid, code, acquired_id))
                if legacy_id not in stage_ids:
                    fail('%d 改道裁定 %s：转写交付 owner %s(%s) 不是真端 talk_npcK（%s）——非本 code 形状'
                        % (qid, code, legacy_npc, legacy_id, sorted(stage_ids)))
            else:
                if legacy_id in declared:
                    fail('%d 改道裁定：转写报告 NPC %s(%s) 在真端声明集内（%s）——不是第三方漂移形'
                        % (qid, legacy_npc, legacy_id, sorted(declared)))
                if legacy_id in census_names.get(qid, set()):
                    fail('%d 改道裁定：转写报告 NPC %s(%s) 在真端表 NPC 集内——非漂移'
                        % (qid, legacy_npc, legacy_id))
                if legacy_id not in census_deviations.get(qid, set()):
                    fail('%d 改道裁定：census DEVIATION 列未记该 NPC(%s)——转写形未承认漂移，人工重核'
                        % (qid, legacy_id))
            qm_syn = re.search(r'<quest id="%d"[^>]*>(.*?)</quest>' % qid, quest_data, re.S)
            canonical_work_items[qid] = [] if not qm_syn else [(int(i), int(c)) for i, c in
                re.findall(r'<quest_work_item item_id="(\d+)" count="(\d+)"', qm_syn.group(1))]
            canonical_resynthesis[qid] = (code, k_stages)
            canonical_pending.add(qid)
        print('P0c-43/44 canonical 改道：%d 行 %s' % (len(canonical_resynthesis),
            ['%d(%s,K=%d)' % (q, c, k) for q, (c, k) in sorted(canonical_resynthesis.items())]))

    # P0c-45：**多余 owner 剪除**裁定表——登记表里由 XML 转写带入、而真端与客户端都不声明的交付
    # owner（判例 35010/35011：遗留 XML 注释「向 Palas(799805) / Priamos(799806) 任一报告」，而
    # 真端 reward=Palas、客户端任务书 dic=35007(Palas)、客户端交付登记无行、客户端模板索引
    # end_npc=799805 单值——四源一致 ⇒ XML 的"任一"无背书）。Fail-closed 轴：①code 域；
    # ②与阶梯/接取入口/改道三表互斥；③该行 owner 必须是 RETAIL_TABLE（XML 保留行 IR 属 XML，不得进表）；
    # ④多余 NPC ∉ 声明集（真端 ∪ 客户端）；⑤census DEVIATION 列记过该 NPC；⑥客户端交付登记未为它背书；
    # ⑦声明 owner 名 == 真端 reward_npc_name 且唯一解析、≠ 多余 NPC；⑧（转写循环内）覆盖守卫：
    # 多余 NPC 的（动作集, 页集）⊆ 声明 owner 的——否则删行会丢页；⑨（转写循环内）越界守卫：
    # 多余 NPC 在 XML 里只许出现在 <dialog> 与 <npc-complete>，其它元素绑定 = 越界，另行裁定。
    retention_owner = {}
    for line in RETENTION.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        rp = line.split('\t')
        if len(rp) >= 2:
            retention_owner[int(rp[0])] = rp[1]
    client_reward_reg = {}
    for line in REWARD_REG.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        rp = line.split('\t')
        client_reward_reg[int(rp[0])] = rp[1]
    extra_owner_prune = {}
    p45_file = HERE / 'p0c45-extra-owner-decisions.tsv'
    if p45_file.exists():
        for line in p45_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p9 = line.split('\t')
            qid = int(p9[0])
            code, extra_npc, owner_name = p9[1], p9[2], p9[3]
            if code != 'LEGACY_EXTRA_DELIVERY_OWNER':
                fail('%d 剪除裁定 code 不在登记域：%s' % (qid, code))
            if qid in ladder_rows or qid in accept_entrance_rows or qid in canonical_resynthesis:
                fail('%d 同时出现在剪除裁定表与阶梯/接取入口/改道裁定表（形状冲突）' % qid)
            if retention_owner.get(qid) != 'RETAIL_TABLE':
                fail('%d 剪除裁定：owner=%s 非 RETAIL_TABLE（XML 保留行不得进本表）'
                    % (qid, retention_owner.get(qid)))
            if extra_npc in census_names.get(qid, set()):
                fail('%d 剪除裁定：%s 在声明集内（真端 ∪ 客户端）——非多余 owner' % (qid, extra_npc))
            if extra_npc not in census_deviations.get(qid, set()):
                fail('%d 剪除裁定：census DEVIATION 列未记 %s' % (qid, extra_npc))
            if extra_npc in re.findall(r'\d+', client_reward_reg.get(qid, '')):
                fail('%d 剪除裁定：客户端交付登记为 %s 背书——非多余' % (qid, extra_npc))
            body = retail_body.get(qid, '')
            rname = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', body)
            if not rname:
                fail('%d 剪除裁定：真端表无 reward_npc_name' % qid)
            if rname.group(1).strip() != owner_name:
                fail('%d 剪除裁定：声明 owner 名 %s ≠ 真端 reward_npc_name %s'
                    % (qid, owner_name, rname.group(1).strip()))
            owner_id = resolve_npc(qid, owner_name, 'extra_owner')
            if owner_id == extra_npc:
                fail('%d 剪除裁定：多余 NPC 与声明 owner 同人' % qid)
            extra_owner_prune[qid] = (extra_npc, owner_id, owner_name)
        print('P0c-45 多余 owner 剪除：%d 行 %s' % (len(extra_owner_prune),
            ['%d(删%s/留%s)' % (q, e, o) for q, (e, o, nm) in sorted(extra_owner_prune.items())]))

    # P0c-46：**角色域收窄**裁定表——遗留 XML 把"交付/领奖角色"铺到链上每个 NPC（判例 1484：5 个
    # NPC 各带一份**逐字相同**的 npc-complete 块），而真端 `reward_npc_name` 唯一、客户端任务书
    # `end_npc_ids` 唯一且与之相等 ⇒ XML 的角色扩散在两源都无背书。与 P0c-45 的**身份轴**（多余 owner，
    # NPC 不在任何声明集内）正交：本通道的被剪 NPC 是链上**另有角色**的合法 NPC（接取/阶段），只是
    # 不该服务交付角色。五源取证见 `.agents/summary/.../p0c46-role-narrowing-decisions.tsv` 头注释。
    # Fail-closed 轴：①code 域；②与阶梯/接取入口/改道/多余 owner 四表互斥；③owner 必须 RETAIL_TABLE；
    # ④真端 reward_npc_name 唯一解析 == 裁定表 owner_name == owner_npc ≠ prune_npc；⑤客户端 end
    # （任务书索引全行并集）唯一 == owner_npc；⑥prune_npc 另有链上角色（真端 acquired∪talk_npcK 或
    # 客户端 start∪progress）；⑦prune_npc ∉ 交付声明集（真端 reward ∪ 客户端 end）；⑧同任务同 owner；
    # ⑨（转写循环内）覆盖守卫：被剪 NPC 的交付块参数**逐字相同**、交付动作/页集 ⊆ owner 的；
    # ⑩（转写循环内）越界守卫：被剪 NPC 在 XML 里只许出现在 <dialog>/<npc-complete>。
    # （`client_roles` 在 canonical 装载段之前已装载：改道 code 的 end 轴与本表共用同一并集口径。）
    role_narrowing = collections.defaultdict(list)   # qid -> [(prune_npc, owner_id, owner_name)]
    role_owner = {}                                  # qid -> (owner_id, owner_name)
    p46_file = HERE / 'p0c46-role-narrowing-decisions.tsv'
    if p46_file.exists():
        for line in p46_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p10 = line.split('\t')
            qid, code, prune_npc, prune_role = int(p10[0]), p10[1], p10[2], p10[3]
            owner_npc, owner_name = p10[4], p10[5]
            if code != 'LEGACY_ROLE_SPREAD_DELIVER':
                fail('%d 角色收窄裁定 code 不在登记域：%s' % (qid, code))
            if prune_role != 'CHAIN':
                fail('%d 角色收窄裁定 prune_role=%s 非 CHAIN（非链上 NPC 不得走本通道）'
                    % (qid, prune_role))
            if (qid in ladder_rows or qid in accept_entrance_rows or qid in canonical_resynthesis
                    or qid in extra_owner_prune):
                fail('%d 同时出现在角色收窄裁定表与阶梯/接取入口/改道/多余 owner 裁定表（形状冲突）' % qid)
            if retention_owner.get(qid) != 'RETAIL_TABLE':
                fail('%d 角色收窄裁定：owner=%s 非 RETAIL_TABLE（XML 保留行不得进本表）'
                    % (qid, retention_owner.get(qid)))
            body = retail_body.get(qid, '')
            rname = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', body)
            aname = re.search(r'<acquired_npc_name>([^<]*)</acquired_npc_name>', body)
            if not rname:
                fail('%d 角色收窄裁定：真端表无 reward_npc_name' % qid)
            if rname.group(1).strip() != owner_name:
                fail('%d 角色收窄裁定：声明 owner 名 %s ≠ 真端 reward_npc_name %s'
                    % (qid, owner_name, rname.group(1).strip()))
            owner_id = int(resolve_npc(qid, owner_name, 'role_narrowing'))
            if str(owner_id) != owner_npc:
                fail('%d 角色收窄裁定：owner_npc=%s ≠ 真端 reward 解析 %s' % (qid, owner_npc, owner_id))
            pn = int(prune_npc)
            cend = client_roles.get(qid, {}).get('end', set())
            if cend != {owner_id}:
                fail('%d 角色收窄裁定：客户端任务书 end=%s 非单值 owner=%s'
                    % (qid, sorted(cend), owner_id))
            if pn == owner_id or pn in cend:
                fail('%d 角色收窄裁定：被剪 NPC %s 在交付声明集内（真端 reward ∪ 客户端 end）'
                    % (qid, prune_npc))
            chain_ids = set()
            for nm in ([aname.group(1).strip()] if aname else []) + [
                    m.group(2).strip() for m in
                    re.finditer(r'<talk_npc(\d)>([^<]*)</talk_npc\1>', body)]:
                ids2 = npc_ids_by_name.get(nm)
                if ids2 and len(ids2) == 1 and ids2[0].isdigit():
                    chain_ids.add(int(ids2[0]))
            cr = client_roles.get(qid, {})
            chain_ids |= cr.get('start', set()) | cr.get('progress', set())
            if pn not in chain_ids:
                fail('%d 角色收窄裁定：被剪 NPC %s 无其它链上角色（真端 acquired∪talk 与客户端 '
                    'start∪progress 均未声明）' % (qid, prune_npc))
            if role_owner.setdefault(qid, (owner_id, owner_name)) != (owner_id, owner_name):
                fail('%d 角色收窄裁定：同任务多个 owner（%s vs %s）'
                    % (qid, role_owner[qid], (owner_id, owner_name)))
            role_narrowing[qid].append((prune_npc, owner_id, owner_name))
        print('P0c-46 角色域收窄：%d 任务 / %d 条 %s' % (len(role_narrowing),
            sum(len(v) for v in role_narrowing.values()),
            ['%d(剪%s/留%s×%d)' % (q, role_narrowing[q][0][0], role_narrowing[q][0][2],
                len(role_narrowing[q])) for q in sorted(role_narrowing)]))

    # P0c-57：**接取入口（NPC_START）角色收窄**裁定表——遗留 XML 把真端**逐步**行（`talk_npc<k>`）
    # 误读成**接取入口**（铁证：HEAD 版 `3093.xml` 里 `<dialog type="NPC_START" npc-id="798177" .../>`
    # 紧接注释「retail 步骤0(zz_retail_simple_quests.xml):type="TALK" ids="798177"；对话链按客户端按钮图
    # 重建。」），于是**链上每个 NPC 都写了一个 NPC_START 块**；编译器对每个块合成接取流
    # （`acceptFlowChain` + 接取续页）⇒ 每个链上 NPC 都成了"可以接取这个任务"的人，而真端
    # `acquired_npc_name` 与客户端 `start_npc_ids` 都只有**一个**接取人。本通道剪掉非 owner 的
    # **重复块**及其接取族 R 行（owner 侧保留同一块 ⇒ 接取流不丢）。
    # 与 P0c-42 接取入口表（物件哨兵**改绑**，同写接取段）互斥；与 P0c-46 角色收窄表**必须共存**
    # （同一批 NPC 上的交付块与接取块是两域，判例 1484）。
    # Fail-closed 轴：①code 域；②与 P0c-42 接取入口表互斥（同段）；③owner 双侧唯一且相等
    # （真端 acquired 唯一解析 == 客户端 start 唯一 == 裁定 owner_npc，且名字 == 裁定 owner_name）；
    # ④owner 必须 RETAIL_TABLE；⑤被剪 NPC 是链上 NPC（真端 acquired∪talk ∪ 客户端 start∪progress∪end）
    # 且 ≠ owner；⑥（转写循环内）被剪块的 source/target/selection-sources/start-page/accept-actions
    # 与 owner 块**逐字相同**；⑦被剪 R 行只许接取族且 `source == unaccepted`（`QUEST_SELECT` 入口行永不剪）；
    # ⑧（收尾）该任务转写后**只剩 owner 一个 NPC_START 块**（表不完整即 fail-closed）。
    accept_narrowing = collections.defaultdict(list)   # qid -> [(prune_npc, owner_id, owner_name)]
    accept_owner = {}                                  # qid -> (owner_id, owner_name)
    accept_block_extra = {}                            # qid -> extra（owner 块参数，转写循环比对用）
    p57_file = HERE / 'p0c57-accept-entrance-decisions.tsv'
    if p57_file.exists():
        for line in p57_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p11 = line.split('\t')
            qid, code, prune_npc, owner_npc, owner_name = (
                int(p11[0]), p11[1], p11[2], p11[3], p11[4])
            if code != 'LEGACY_ACCEPT_ENTRANCE_SPREAD':
                fail('%d 接取入口裁定 code 不在登记域：%s' % (qid, code))
            if qid in accept_entrance_rows:
                fail('%d 同时出现在接取入口裁定表与 P0c-42 接取入口改绑表（同写接取段，形状冲突）' % qid)
            if retention_owner.get(qid) != 'RETAIL_TABLE':
                fail('%d 接取入口裁定：owner=%s 非 RETAIL_TABLE（XML 保留行不得进本表）'
                    % (qid, retention_owner.get(qid)))
            body = retail_body.get(qid, '')
            aname = re.search(r'<acquired_npc_name>([^<]*)</acquired_npc_name>', body)
            if not aname or aname.group(1).strip() != owner_name:
                fail('%d 接取入口裁定：声明 owner 名 %s ≠ 真端 acquired_npc_name %s'
                    % (qid, owner_name, aname.group(1).strip() if aname else '(缺失)'))
            owner_id = int(resolve_npc(qid, owner_name, 'accept_entrance'))
            if str(owner_id) != owner_npc:
                fail('%d 接取入口裁定：owner_npc=%s ≠ 真端 acquired 解析 %s'
                    % (qid, owner_npc, owner_id))
            cstart = client_roles.get(qid, {}).get('start', set())
            if cstart != {owner_id}:
                fail('%d 接取入口裁定：客户端 start_npc_ids=%s 非单值 owner=%s（双侧必须唯一且相等）'
                    % (qid, sorted(cstart), owner_id))
            pn = int(prune_npc)
            if pn == owner_id:
                fail('%d 接取入口裁定：被剪 NPC 与 owner 同人' % qid)
            chain_ids = set()
            for nm in ([aname.group(1).strip()] if aname else []) + [
                    m.group(2).strip() for m in
                    re.finditer(r'<talk_npc(\d)>([^<]*)</talk_npc\1>', body)] + [
                    m.group(1).strip() for m in
                    re.finditer(r'<reward_npc_name>([^<]*)</reward_npc_name>', body)]:
                ids2 = npc_ids_by_name.get(nm)
                if ids2 and len(ids2) == 1 and ids2[0].isdigit():
                    chain_ids.add(int(ids2[0]))
            cr = client_roles.get(qid, {})
            chain_ids |= cr.get('start', set()) | cr.get('progress', set()) | cr.get('end', set())
            if pn not in chain_ids:
                fail('%d 接取入口裁定：被剪 NPC %s 无其它链上角色（真端 acquired∪talk∪reward 与 '
                    '客户端 start∪progress∪end 均未声明）' % (qid, prune_npc))
            if accept_owner.setdefault(qid, (owner_id, owner_name)) != (owner_id, owner_name):
                fail('%d 接取入口裁定：同任务多个 owner（%s vs %s）'
                    % (qid, accept_owner[qid], (owner_id, owner_name)))
            accept_narrowing[qid].append((prune_npc, owner_id, owner_name))
        print('P0c-57 接取入口收窄：%d 任务 / %d 块 %s' % (len(accept_narrowing),
            sum(len(v) for v in accept_narrowing.values()),
            ['%d(剪%s/留%s×%d)' % (q, ','.join(x[0] for x in accept_narrowing[q]),
                accept_narrowing[q][0][2], len(accept_narrowing[q])) for q in sorted(accept_narrowing)]))

    # P0c-47：**阶段推进行领奖窗外溢**裁定表——P0c-46 残留 DELIVER_UNCOVERED 的第二形：遗留转写把
    # 「下发领奖窗」（DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW1）挂在落到 REWARD 的
    # 阶段推进行（SETPRO<k>）上，而真端/客户端把领奖窗归交付 owner。该阶段 NPC 的交付投影不是 owner
    # 的子集 ⇒ 不得剪行（P0c-46 已判结构不同），本通道只收窄**行内 after-commit**：末位窗外溢令牌
    # 替换为 CLOSE（SYNC 模式逐字保留，客户端阶段按钮文案「结束对话。」）。
    # Fail-closed 轴：①code 域；②与阶梯/接取入口/改道/多余 owner/角色收窄各表互斥；③owner 必须
    # RETAIL_TABLE；④stage_npc 唯一解析且 == 真端 talk_npc<k>（action=SETPRO<k> 的 k）；
    # ⑤owner = 真端 reward_npc_name 唯一解析 == 裁定表 owner_npc ≠ stage_npc，客户端 end（若登记）== owner；
    # ⑥（转写期）该 (npc, action) 行存在、target 投影 = REWARD、after-commit 形状逐字匹配、只此一处窗外溢；
    # ⑦（转写后）无页丢失守卫：owner 名下仍有开领奖窗的记录。
    stage_window_spread = {}
    spread_narrowed = set()
    entry_rows_added = set()          # P0c-54：已插入的阶段入口行任务
    spread_page_pruned = set()        # P0c-54：已剪除的 (qid, extra_npc, page)
    p47_file = HERE / 'p0c47-stage-window-spread.tsv'
    if p47_file.exists():
        for line in p47_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p11 = line.split('\t')
            qid, code = int(p11[0]), p11[1]
            stage_npc, action = p11[2], p11[3]
            owner_npc, stage_name, owner_name = p11[4], p11[5], p11[6]
            if code != 'LEGACY_STAGE_REWARD_WINDOW_SPREAD':
                fail('%d 阶段窗外溢裁定 code 不在登记域：%s' % (qid, code))
            if (qid in ladder_rows or qid in accept_entrance_rows or qid in canonical_resynthesis
                    or qid in extra_owner_prune or qid in role_narrowing):
                fail('%d 同时出现在阶段窗外溢裁定表与阶梯/接取入口/改道/多余 owner/角色收窄裁定表'
                    '（形状冲突）' % qid)
            if retention_owner.get(qid) != 'RETAIL_TABLE':
                fail('%d 阶段窗外溢裁定：owner=%s 非 RETAIL_TABLE（XML 保留行不得进本表）'
                    % (qid, retention_owner.get(qid)))
            body = retail_body.get(qid, '')
            if not re.search(r'<acquired_npc_name>', body):
                fail('%d 阶段窗外溢裁定：真端表无该行' % qid)
            sid = int(resolve_npc(qid, stage_name, 'stage_window_spread'))
            if str(sid) != stage_npc:
                fail('%d 阶段窗外溢裁定：stage_npc=%s ≠ 真端 talk_npc 解析 %s'
                    % (qid, stage_npc, sid))
            # stage_npc 必须是 action=SETPRO<k> 里那个 k 对应真端 talk_npc<k>（阶段归属轴一致）。
            km = re.match(r'^SETPRO(\d+)$', action)
            if not km:
                fail('%d 阶段窗外溢裁定：action=%s 非 SETPRO<k> 形（本通道只收窄阶段推进行）'
                    % (qid, action))
            tk = 'talk_npc%s' % km.group(1)
            tm = re.search(r'<%s>([^<]*)</%s>' % (tk, tk), body)
            if not tm:
                fail('%d 阶段窗外溢裁定：真端无 %s（action=%s 无对应阶段）' % (qid, tk, action))
            if resolve_npc(qid, tm.group(1), tk) != stage_npc:
                fail('%d 阶段窗外溢裁定：%s=%s ≠ 裁定表 stage_npc=%s'
                    % (qid, tk, tm.group(1).strip(), stage_npc))
            rname = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', body)
            if not rname:
                fail('%d 阶段窗外溢裁定：真端表无 reward_npc_name' % qid)
            if rname.group(1).strip() != owner_name:
                fail('%d 阶段窗外溢裁定：声明 owner 名 %s ≠ 真端 reward_npc_name %s'
                    % (qid, owner_name, rname.group(1).strip()))
            oid = int(resolve_npc(qid, owner_name, 'stage_window_spread'))
            if str(oid) != owner_npc:
                fail('%d 阶段窗外溢裁定：owner_npc=%s ≠ 真端 reward 解析 %s' % (qid, owner_npc, oid))
            if oid == sid:
                fail('%d 阶段窗外溢裁定：owner 与阶段 NPC 同人（%s）' % (qid, oid))
            cend = client_roles.get(qid, {}).get('end', set())
            if cend and cend != {oid}:
                fail('%d 阶段窗外溢裁定：客户端任务书 end=%s 非单值 owner=%s'
                    % (qid, sorted(cend), oid))
            if p11[7] and p11[7] != owner_npc:
                fail('%d 阶段窗外溢裁定：client_end 列 %s ≠ owner_npc %s' % (qid, p11[7], owner_npc))
            stage_window_spread[(qid, stage_npc, action)] = (owner_npc, owner_name, stage_name)
        print('P0c-47 阶段窗外溢：%d 条 %s' % (len(stage_window_spread),
            ['%d(%s@%s→%s)' % (q, a, n, v[0]) for (q, n, a), v in sorted(stage_window_spread.items())]))

    # P0c-54：**阶段页轴**裁定表（两个 code）——①`STAGE_ENTRY_MISSING`：客户端阶段页链的**入口页**
    # （`SELECT{k+1}`）被历史转写丢掉（只剩续页与推进），玩家看不到阶段对话起点、审计记
    # `CLIENT_PAGE_UNREACHED` ⇒ 在该阶段 NPC 上补一条 `QUEST_SELECT` 入口行（与阶梯通道同形）；
    # ②`STAGE_PAGE_OWNER_SPREAD`：阶段页链上的页被**非本阶段 NPC**（接取角色 / 别的阶段 / 交付
    # owner）也下发 ⇒ 剪掉越界行（owner 行保留 ⇒ 无页丢失）。与既有六通道互斥（同一批形状，同任务
    # 双表即两套裁量）。Fail-closed 轴：①code 域；②与阶梯/接取入口/改道/多余 owner/角色收窄/阶段窗
    # 外溢互斥；③owner 必须 RETAIL_TABLE；④owner_npc == 真端 `talk_npc<k>` 唯一解析；⑤页必须在客户端
    # 该阶段页链上（含末页）；⑥ENTRY 页必须是页链入口且该阶段有推进/续页行（链在续页一侧完好）；
    # ⑦SPREAD 的 extra_npc ≠ owner 且其角色可判（客户端 start/end/progress 或真端别的 `talk_npcJ`）。
    stage_entry_rows = {}     # qid -> (stage, page, owner_npc)
    stage_spread_prune = {}   # qid -> [(page, extra_npc, owner_npc, stage)]
    entry_page_axis = []      # 被剪 NPC 的阶段页只出现在其 QUEST_SELECT 入口行上（接取入口页轴，另裁）
    sd_file = HERE / 'p0c54-stage-page-decisions.tsv'
    if sd_file.exists():
        for line in sd_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p12 = line.split('\t')
            code, qid, page, extra_npc, owner_npc, stage = (p12[0], int(p12[1]), p12[2],
                p12[3], p12[4], int(p12[5]))
            if code not in ('STAGE_ENTRY_MISSING', 'STAGE_PAGE_OWNER_SPREAD'):
                fail('%d 阶段页轴裁定 code 不在登记域：%s' % (qid, code))
            # 与五个**块级**通道互斥（阶梯 / 接取入口 / 改道 / 多余 owner / 角色收窄——它们整块改写该任务形状）。
            # 与 P0c-47 阶段窗外溢（同族的**行内**收窄）**允许同任务**：两表的靶行不同（本表靶 = 阶段页链上的
            # 页行；P0c-47 靶 = `SETPRO<k>` 推进行的末位窗外溢令牌），且两侧都有「条目未被消费即 fail-closed」
            # 收尾守卫——若本表剪掉 P0c-47 的靶行，P0c-47 的守卫会立刻报警，故重叠是安全且自证的。
            if (qid in ladder_rows or qid in accept_entrance_rows or qid in canonical_resynthesis
                    or qid in extra_owner_prune or qid in role_narrowing):
                fail('%d 阶段页轴裁定与块级通道重复（同任务两套裁量）' % qid)
            if page == REWARD_WINDOW_PAGE:
                fail('%d 阶段页轴裁定：页 %s 是领奖窗页（P0c-47 行内收窄的令牌域，不属阶段页链）'
                    % (qid, page))
            if retention_owner.get(qid) != 'RETAIL_TABLE':
                fail('%d 阶段页轴裁定：owner=%s 非 RETAIL_TABLE（XML 保留行不得进本表）'
                    % (qid, retention_owner.get(qid)))
            tn = re.search(r'<talk_npc%d>([^<]*)</talk_npc%d>' % (stage, stage),
                retail_body.get(qid, ''))
            if not tn:
                fail('%d 阶段页轴裁定：真端无 talk_npc%d（阶段轴形状不成立）' % (qid, stage))
            oid = int(resolve_npc(qid, tn.group(1).strip(), 'stage_page_owner'))
            if str(oid) != owner_npc:
                fail('%d 阶段页轴裁定：owner_npc=%s ≠ 真端 talk_npc%d 解析 %s'
                    % (qid, owner_npc, stage, oid))
            chain = stage_client_chain(qid, stage, client_pages, client_actions, client_page_ids)
            if page not in chain:
                fail('%d 阶段页轴裁定：页 %s 不在客户端阶段 %d 页链 %s' % (qid, page, stage, chain))
            if code == 'STAGE_ENTRY_MISSING':
                if page != chain[0]:
                    fail('%d 阶段入口裁定：%s 非页链入口（%s）' % (qid, page, chain[0]))
                if len(chain) < 2:
                    fail('%d 阶段入口裁定：页链仅入口页（%s）——非「入口被丢」形状' % (qid, chain))
                if qid in stage_entry_rows:
                    fail('%d 阶段入口裁定重复行' % qid)
                stage_entry_rows[qid] = (stage, page, owner_npc)
            else:
                if extra_npc in ('-', owner_npc):
                    fail('%d 阶段页扩散裁定：extra_npc=%s 非法' % (qid, extra_npc))
                eid = int(extra_npc)
                cr = client_roles.get(qid, {})
                other_talk = set()
                for j in range(1, 40):
                    if j == stage:
                        continue
                    mj = re.search(r'<talk_npc%d>([^<]*)</talk_npc%d>' % (j, j), retail_body.get(qid, ''))
                    if mj:
                        ids_j = npc_ids_by_name.get(mj.group(1).strip())
                        if ids_j and len(ids_j) == 1 and ids_j[0].isdigit():
                            other_talk.add(int(ids_j[0]))
                role = ('START' if eid in cr.get('start', set())
                    else 'PROGRESS' if eid in cr.get('progress', set())
                    else 'END' if eid in cr.get('end', set())
                    else 'TALK%d' % stage if eid in other_talk else '')
                if not role:
                    fail('%d 阶段页扩散裁定：越界 NPC %s 角色不可判（客户端 start=%s end=%s progress=%s）'
                        % (qid, extra_npc, sorted(cr.get('start', set())), sorted(cr.get('end', set())),
                            sorted(cr.get('progress', set()))))
                # 只剪**页动作行**（after-commit 下发该页的行）：`QUEST_SELECT` 行是被剪 NPC 自己的
                # **对话入口**（接取/报告窗），删掉整条对话就消失、任务在该 NPC 上不可接 —— 那属
                # 「接取入口页」轴（入口页被写成阶段页），本表不裁；命中 0 条时本行剔除并登记。
                page_rows = entry_rows = 0
                for tm in TR.finditer(quest_xml(qid)):
                    if tm.group(4) != extra_npc:
                        continue
                    am3 = ACTIONS_ATTR.search(tm.group(5) or '')
                    toks3 = am3.group(1).split() if am3 else []
                    fm3 = re.search(r'<after-commit>(.*?)</after-commit>', tm.group(6), re.S)
                    pages3 = [pm3.group(1) for pm3 in re.finditer(
                        r'<dialog type="SHOW_QUEST_PAGE" page="([A-Z0-9_]+)"', fm3.group(1) if fm3 else '')]
                    if page not in pages3:
                        continue
                    if 'QUEST_SELECT' in toks3:
                        entry_rows += 1
                    else:
                        page_rows += 1
                if page_rows == 0:
                    entry_page_axis.append((qid, page, extra_npc, owner_npc, stage, role))
                    continue
                if entry_rows:
                    entry_page_axis.append((qid, page, extra_npc, owner_npc, stage,
                        role + '+QUEST_SELECT入口'))
                stage_spread_prune.setdefault(qid, []).append((page, extra_npc, owner_npc, stage))
        print('P0c-54 阶段页轴：入口补行 %s；越界剪行 %d 条 %s'
            % (sorted(stage_entry_rows), sum(len(v) for v in stage_spread_prune.values()),
                ['%d(%s)' % (q, '/'.join('%s@%s' % (x[0], x[1]) for x in v))
                    for q, v in sorted(stage_spread_prune.items())]))
        if entry_page_axis:
            print('P0c-54 接取入口页轴（本表不裁，登记待议）：%d 条 %s'
                % (len(entry_page_axis),
                    ['%d(%s@%s,%s)' % (q, p, n, r) for q, p, n, _o, _k, r in entry_page_axis]))

    no_routes = []
    out = ['# SimpleTalk wave A 链式登记（真端表权威 + 客户端背书 + XML 转写；编译器只读本表）',
        '# 生成：build_quest_client_talk_chain_steps.py；词汇 fail-closed，页面经客户端 CSV 交叉核验',
        '# 记录三类（kind 列）：N=节点(label,status,var0)；R=TalkToNpc 路由；B=规范块参数',
        '# N: quest_id  N  label  status  var0',
        '# R: quest_id  R  seq  npc_id  action  source  target  conditions  actions  after_commits  npc_check  page_check  priority',
        '# B: quest_id  B  block  npc  source  target  extra',
        '# P: quest_id  P  offset  width  min  max  persistence  scope',
        '# Q: quest_id  Q  seq  action  source  target  conditions  actions  after_commits（QUEST_ACTION 无目标事件）',
        '# E: quest_id  E  target  conditions  actions  after_commits（EnterWorld 无源事件，QE-051 奖励行自愈边）',
        '# I: quest_id  I  npc_id  source  target  item_id  required  remove_count  failure_page（npc-item-report 门，P0c-34）']
    block_out = []
    route_count = 0
    page_hit = 0
    page_total = 0
    prune_dropped = collections.defaultdict(int)  # P0c-45：剪除记录计数（qid → 条数）
    role_dropped = collections.defaultdict(int)   # P0c-46：角色域剪除计数（qid → 条数）
    accept_dropped = collections.defaultdict(int) # P0c-57：接取入口块剪除计数（qid → 块数）
    accept_row_dropped = collections.defaultdict(int) # P0c-57：接取族行剪除计数（qid → 行数）
    for qid in wave_a + wave_b:
        if qid in canonical_resynthesis:
            # P0c-43：该行转写与真端结构分歧（裁定表 P0c-43）——跳过转写，由 canonical 合成批接管。
            print('CANONICAL-OVERRIDE %d：转写与真端结构性分歧，弃转写改走 canonical 合成' % qid)
            continue
        text = quest_xml(qid)
        # P0c-45：多余 owner 剪除（轴⑧覆盖守卫 / 轴⑨越界守卫，判据见裁定表装载注释）。
        prune_npc = None
        if qid in extra_owner_prune:
            prune_npc, owner_id, owner_name = extra_owner_prune[qid]

            def _served(npc_id):
                # 投影 = 该 NPC 的（动作集, 下发页集）；页集走 encode_after 同口径解析。
                # 类型对齐：TR 的 group(4) 是**字符串**，owner_id 是 int——不对齐会让两侧都取空集，
                # 守卫退化成「空集 ⊆ 空集」恒真（P0c-46 复核时实测到，见判例 QE-076）。
                npc_id = str(npc_id)
                acts, pages = set(), set()
                for tm in TR.finditer(text):
                    if tm.group(4) != npc_id:
                        continue
                    am3 = ACTIONS_ATTR.search(tm.group(5) or '')
                    for a in (am3.group(1).split() if am3 else ['-']):
                        acts.add(a)
                    fm3 = re.search(r'<after-commit>(.*?)</after-commit>', tm.group(6), re.S)
                    pgs = []
                    if fm3:
                        encode_after(fm3.group(1), qid, pgs)
                    pages |= set(pgs)
                return acts, pages

            xa, xp = _served(prune_npc)
            oa, op = _served(owner_id)
            if not xa <= oa or not xp <= op:
                fail('%d 剪除裁定：覆盖守卫失败——多余 owner 的动作/页集不是声明 owner 的子集'
                    '（删%s=%s/%s 留%s=%s/%s）' % (qid, prune_npc, sorted(xa), sorted(xp),
                        owner_id, sorted(oa), sorted(op)))
            tags = sorted({m.group(1) for m in re.finditer(r'<([a-z-]+)[^>]*npc-id="%s"' % prune_npc, text)})
            bad = [tag for tag in tags if tag not in ('dialog', 'npc-complete')]
            if bad:
                fail('%d 剪除裁定：多余 owner %s 出现在越界元素 %s（需单独裁定，不得剪除）'
                    % (qid, prune_npc, bad))

            def pruned(npc_field):
                # 该记录是否属于被剪除的多余 owner（剪除时计数）。 / Drop-and-count helper.
                if prune_npc is not None and str(npc_field) == prune_npc:
                    prune_dropped[qid] += 1
                    return True
                return False
        else:
            def pruned(npc_field):
                return False

        # P0c-46：角色域剪除（轴⑨覆盖守卫 / 轴⑩越界守卫）——只剪**交付角色**的记录，
        # 被剪 NPC 的接取/阶段记录必须原样保留（这正是与 P0c-45 身份轴的区别）。
        role_drop = {npn for npn, _, _ in role_narrowing.get(qid, [])}
        if role_drop:
            _r_owner_id, _r_owner_name = role_owner[qid]

            def _nc_sigs(npc_id):
                """npc-complete 块签名（空白归一 + npc-id 掩码 ⇒ 只比较参数与子元素）。"""
                sigs = []
                for bm3 in re.finditer(r'<npc-complete npc-id="%s"' % npc_id, text):
                    st3 = bm3.start()
                    en3 = text.find('>', st3) + 1
                    seg3 = text[st3:en3] if text[st3:en3].rstrip().endswith('/>') else \
                        text[st3:text.find('</npc-complete>', en3) + 15]
                    sigs.append(re.sub(r'npc-id="\d+"', 'npc-id="N"', re.sub(r'\s+', ' ', seg3)).strip())
                return sorted(sigs)

            def _report_sigs(npc_id):
                sigs = []
                for bm3 in re.finditer(r'<dialog type="NPC_REPORT"[^>]*npc-id="%s"[^>]*/>' % npc_id, text):
                    sigs.append(re.sub(r'npc-id="\d+"', 'npc-id="N"',
                        re.sub(r'\s+', ' ', bm3.group(0))).strip())
                return sorted(sigs)

            def _deliver_proj(npc_id):
                """交付角色投影：交付动作集 + 交付页集（页经 encode_after 同口径解析）。"""
                acts, pages = set(), set()
                for tm3 in TR.finditer(text):
                    if tm3.group(4) != npc_id:
                        continue
                    am4 = ACTIONS_ATTR.search(tm3.group(5) or '')
                    for a4 in (am4.group(1).split() if am4 else ['-']):
                        if a4 in ROLE_DELIVER_ACTIONS:
                            acts.add(a4)
                    fm4 = re.search(r'<after-commit>(.*?)</after-commit>', tm3.group(6), re.S)
                    pgs4 = []
                    if fm4:
                        encode_after(fm4.group(1), qid, pgs4)
                    pages |= {pg for pg in pgs4 if ROLE_DELIVER_PAGE.match(pg)}
                return acts, pages

            for _pn in sorted(role_drop):
                # 类型对齐：role_drop 元素是裁定表里的字符串，owner 是 int（QE-076 同款坑）。
                if _nc_sigs(_pn) != _nc_sigs(_r_owner_id) or _report_sigs(_pn) != _report_sigs(_r_owner_id):
                    fail('%d 角色收窄覆盖守卫：交付块参数与 owner 不逐字相同（剪%s / 留%s）'
                        % (qid, _pn, _r_owner_id))
                _pa, _pp = _deliver_proj(str(_pn))
                _oa, _op = _deliver_proj(str(_r_owner_id))
                if not _pa <= _oa or not _pp <= _op:
                    fail('%d 角色收窄覆盖守卫：交付动作/页集不是 owner 子集（剪%s=%s/%s 留%s=%s/%s）'
                        % (qid, _pn, sorted(_pa), sorted(_pp), _r_owner_id, sorted(_oa), sorted(_op)))
                _tags = sorted({m.group(1) for m in
                    re.finditer(r'<([a-z-]+)[^>]*npc-id="%s"' % _pn, text)})
                _bad = [t for t in _tags if t not in ('dialog', 'npc-complete')]
                if _bad:
                    fail('%d 角色收窄越界守卫：被剪 NPC %s 出现在越界元素 %s（需单独裁定）'
                        % (qid, _pn, _bad))

            def role_pruned(npc_field):
                if str(npc_field) in role_drop:
                    role_dropped[qid] += 1
                    return True
                return False

            def role_pruned_route(npc_field, action, pages):
                """R 行按角色剪：只有交付动作/交付页的 R 行随角色一起剪。"""
                if str(npc_field) not in role_drop:
                    return False
                if action in ROLE_DELIVER_ACTIONS or any(ROLE_DELIVER_PAGE.match(p) for p in pages):
                    role_dropped[qid] += 1
                    return True
                return False
        else:
            def role_pruned(npc_field):
                return False

            def role_pruned_route(npc_field, action, pages):
                return False

        # P0c-57：接取入口块与接取族行的剪除 helper（轴⑥块参数逐字相同 / 轴⑦言行域与相位）。
        # P0c-57 accept-entrance prune helpers (block signature identity / row domain + phase).
        accept_drop = {npn for npn, _, _ in accept_narrowing.get(qid, [])}
        if accept_drop:
            _a_owner_id, _a_owner_name = accept_owner[qid]

            def _start_sig(npc_id):
                """NPC_START 块签名（npc-id 掩码 + 空白归一 ⇒ 只比较参数与子元素）。"""
                sigs = []
                for bm5 in re.finditer(r'<dialog type="NPC_START"[^>]*npc-id="%s"' % npc_id, text):
                    st5 = bm5.start()
                    en5 = text.find('>', st5) + 1
                    seg5 = text[st5:en5] if text[st5:en5].rstrip().endswith('/>') else \
                        text[st5:text.find('</dialog>', en5) + 9]
                    sigs.append(re.sub(r'npc-id="\d+"', 'npc-id="N"',
                        re.sub(r'\s+', ' ', seg5)).strip())
                return sorted(sigs)

            _owner_sigs = _start_sig(_a_owner_id)
            if not _owner_sigs:
                fail('%d 接取入口裁定：owner %s 无 NPC_START 块（块缺失形不得走本通道）'
                    % (qid, _a_owner_id))
            for _pn5 in sorted(accept_drop):
                _psig = _start_sig(_pn5)
                if not _psig:
                    fail('%d 接取入口裁定：被剪 NPC %s 无 NPC_START 块（裁定表陈旧）' % (qid, _pn5))
                if _psig != _owner_sigs:
                    fail('%d 接取入口裁定：被剪块参数与 owner 不逐字相同（剪%s=%s / 留%s=%s）'
                        % (qid, _pn5, _psig, _a_owner_id, _owner_sigs))

            def accept_pruned(npc_field):
                if str(npc_field) in accept_drop:
                    accept_dropped[qid] += 1
                    return True
                return False

            def accept_pruned_route(npc_field, action, pages, src):
                """R 行按接取角色剪：只有接取族动作/页的行随块一起剪，且必须仍在接取相位。"""
                if str(npc_field) not in accept_drop:
                    return False
                if not (action in ACCEPT_ROLE_DIALOG_ACTIONS
                        or any(ACCEPT_ROLE_PAGE.match(p) for p in pages)):
                    return False
                if src != 'unaccepted':
                    fail('%d 接取入口裁定：被剪行 source=%s 非 unaccepted（只裁接取相位）'
                        % (qid, src))
                accept_row_dropped[qid] += 1
                return True
        else:
            def accept_pruned(npc_field):
                return False

            def accept_pruned_route(npc_field, action, pages, src):
                return False

        # N 记录完整性闸：节点带非标准结构（多 var / 属性序差异）会让 N 正则静默漏转写，
        # 路由随之悬空（BAD_NODE_REFERENCE 假象）——在写入任何记录前拦截。
        n_tags = len(re.findall(r'<node label="', text))
        n_matched = len(re.findall(
            r'<node label="([^"]+)" status="([A-Z]+)">\s*<var name="var0" value="(\d+)"\s*/>', text)) \
            + len(re.findall(r'<node label="([^"]+)" status="([A-Z]+)"\s*/>', text))
        if n_tags != n_matched:
            if qid in b_set:
                print('NODE-GAP %d：%d/%d 节点未转写——跳过转写' % (qid, n_tags - n_matched, n_tags))
                axis_mismatch.append(qid)
                continue
            if qid in keep_set:
                # KEEP 行：登记表是快照，节点缺口（如无 var 子元素的变体节点，判例 30711）
                # 正是其 KEEP 依据之一——保持部分转写不作退役判据。
                print('NODE-GAP %d（KEEP 快照）：%d/%d 节点未转写——保持部分转写'
                    % (qid, n_matched, n_tags))
            else:
                fail('%d 节点转写缺口：%d/%d（ADOPT 行不允许）' % (qid, n_matched, n_tags))
        # 物品轴计数对账（真端表 vs XML 转写；give_item 无编号 = npc-start accept-actions）。
        # 仅对 wave B 批生效：KEEP 行（3966 等）以 XML 为 owner，真端表无轴但 XML 有交付轴
        # 属已登记差异，不回炉重裁。
        # 判例 1118：交付路由（SELECT_QUEST_REWARD）上 remove 的物品若本任务自给
        # （give-item 同 id）→ 真端 remove_itemN 列只记外部物品回收，自给自消费是隐式语义，
        # 豁免计数。
        if qid in b_set:
            body = retail_body.get(qid, '')
            rg = len(re.findall(r'<give_item\d*>', body))
            rr = len(re.findall(r'<remove_item\d*>', body))
            tg = len(re.findall(r'<give-item[^>]*/>', text))
            give_ids = set(re.findall(r'<give-item item-id="(\d+)"', text))
            tr_total = len(re.findall(r'<remove-item[^>]*/>', text))
            exempt = 0
            for dm in re.finditer(
                    r'<transition[^>]*>(?:(?!</transition>).)*?action="SELECT_QUEST_REWARD"'
                    r'(?:(?!</transition>).)*?</transition>', text, re.S):
                for rid in re.findall(r'<remove-item item-id="(\d+)"', dm.group(0)):
                    if rid in give_ids:
                        exempt += 1
            tr_ = tr_total - exempt
            if (rg, rr) != (tg, tr_):
                # 计数不符 = XML 与真端阶段绑定语义结构性分歧（判例 1183：accept 全发 +
                # 重复发物路由）——逐字转写不成立，该行出登记批，留裁定波按真端规范合成或 KEEP。
                print('AXIS-MISMATCH %d：真端 give/remove=%d/%d，XML=%d/%d（交付豁免 %d）——跳过转写'
                    % (qid, rg, rr, tg, tr_, exempt))
                axis_mismatch.append(qid)
                continue
        pm2 = re.search(r'<bit-field name="var0" offset="(\d+)" width="(\d+)" min="(\d+)" max="(\d+)"'
            r' persistence="([A-Z]+)" scope="([A-Z]+)"\s*/>', text)
        if pm2:
            out.append('%d\tP\t%s\t%s\t%s\t%s\t%s\t%s' % ((qid,) + pm2.groups()))
        # N 节点记录（逐字转写：标签/状态/var0；容忍 ` />` 带空格自闭合）。
        # varless 变体节点（无 var 子元素，判例 30711）→ var0='-'（编译器回放空 projection）。
        for nm in re.finditer(r'<node label="([^"]+)" status="([A-Z]+)">\s*'
                r'<var name="var0" value="(\d+)"\s*/>', text):
            nlabel, nstatus, nvalue = nm.group(1), nm.group(2), nm.group(3)
            override = progress_row_overrides.get((qid, nlabel))
            if override is not None:
                xml_value, new_value, basis = override
                if nvalue != xml_value:
                    fail('%d %s 进度行投影登记旧值 %s != XML 现值 %s（裁定表腐化）'
                        % (qid, nlabel, xml_value, nvalue))
                # collect_progress 与 drop 轴在真端 quest.xml；item_check 在 SimpleTalk 表。
                qxm = re.search(r'<quest>\s*<id>%d</id>.*?</quest>' % qid, retail_quest_xml_text, re.S)
                qbody = qxm.group(0) if qxm else ''
                pm2 = re.search(r'<collect_progress>(\d+)</collect_progress>', qbody)
                prog = int(pm2.group(1)) if pm2 else 0
                if prog <= 0:
                    fail('%d %s 进度行投影裁定无真端 collect_progress 背书' % (qid, nlabel))
                if new_value != prog:
                    fail('%d %s 进度行投影 %d != collect_progress %d' % (qid, nlabel, new_value, prog))
                last_row = client_summary_rows.get(qid, 0) - 1
                if last_row != prog:
                    fail('%d %s 进度行投影: 客户端末行 %d != collect_progress %d（需中间行合成，另行裁定）'
                        % (qid, nlabel, last_row, prog))
                if ('<drop_monster_1>' not in qbody and '<drop_item_1>' not in qbody
                        and '<item_check>' not in retail_body.get(qid, '')):
                    fail('%d %s 进度行投影裁定无 drop/item_check 轴背书' % (qid, nlabel))
                print('PROGRESS-ROW-OVERRIDE %d %s: %s -> %s（%s）'
                    % (qid, nlabel, nvalue, new_value, basis))
                nvalue = str(new_value)
            if nstatus == 'REWARD':
                if qid in reward_row_overrides:
                    nvalue = str(reward_row_overrides[qid][0])
                reward_projection_rows.append((qid, nm.group(3), nvalue,
                    client_summary_rows.get(qid, 0) - 1,
                    'OVERRIDE' if qid in reward_row_overrides else '-'))
            out.append('%d\tN\t%s\t%s\t%s' % (qid, nlabel, nstatus, nvalue))
        for nm in re.finditer(r'<node label="([^"]+)" status="([A-Z]+)"\s*/>', text):
            out.append('%d\tN\t%s\t%s\t-' % (qid, nm.group(1), nm.group(2)))
        # I 记录：item_check 门（编译器级糖元素 npc-item-report，P0c-34）。
        # 形状合同与 QuestXmlBlockExpander.expandNpcItemReport 同口径：source 必须投影 START、
        # target 必须投影 REWARD，remove-count ∈ {缺省=required, ALL, required}。
        # 真端背书 fail-closed：SimpleTalk <item_check> 标志存在 + quest.xml collect_item 符号
        # 经 item_name_index 解析后 (item_id, count) 与 XML (item-id, required) 相符。
        node_status = dict(re.findall(r'<node label="([^"]+)" status="([A-Z]+)"', text))
        for im in IT.finditer(text):
            iattrs = im.group(1)
            inpc = int(re.search(r'npc-id="(\d+)"', iattrs).group(1))
            isrc = re.search(r'source="([^"]*)"', iattrs).group(1)
            itgt = re.search(r'target="([^"]*)"', iattrs).group(1)
            iitem = int(re.search(r'item-id="(\d+)"', iattrs).group(1))
            ireq = int(re.search(r'required="(\d+)"', iattrs).group(1))
            irc = re.search(r'remove-count="([^"]*)"', iattrs)
            irc_s = '-' if not irc or not irc.group(1).strip() else (
                'ALL' if irc.group(1).strip().upper() == 'ALL' else irc.group(1).strip())
            if irc_s.isdigit() and int(irc_s) != ireq:
                fail('%d npc-item-report remove-count=%s 与 required=%d 不符' % (qid, irc_s, ireq))
            ifp = re.search(r'failure-page="([^"]*)"', iattrs)
            ifp_s = '-' if not ifp or not ifp.group(1).strip() else ifp.group(1).strip()
            if ifp_s not in ('-', 'CLOSE') and ifp_s not in page_symbols:
                fail('%d npc-item-report failure-page 符号不在 QuestDialogPage 域：%s' % (qid, ifp_s))
            if node_status.get(isrc) != 'START':
                fail('%d npc-item-report source=%s 未投影 START' % (qid, isrc))
            if node_status.get(itgt) != 'REWARD':
                fail('%d npc-item-report target=%s 未投影 REWARD' % (qid, itgt))
            rbody = retail_body.get(qid, '')
            if '<item_check>' not in rbody:
                fail('%d npc-item-report 无真端 SimpleTalk item_check 背书' % qid)
            gate_pairs = []
            iqm = re.search(r'<quest>\s*<id>%d</id>.*?</quest>' % qid, retail_quest_xml_text, re.S)
            if iqm:
                for sym, cnt in re.findall(
                        r'<collect_item\d*>(\S+)(?:\s+(\d+))?\s*</collect_item\d*>', iqm.group(0)):
                    gate_pairs.append((item_ids_by_name.get(sym.strip().lower()), cnt or '1'))
            if (str(iitem), str(ireq)) not in gate_pairs:
                fail('%d npc-item-report 与真端 collect_item 不符：XML=%d/%d 真端=%s'
                    % (qid, iitem, ireq, gate_pairs))
            item_report_crosscheck.append('%d\t%d\t%s\t%d\t%d\t%s\t%s\t%s' % (
                qid, inpc, 'quest.xml collect_item → %d' % iitem, ireq, iitem, irc_s, ifp_s,
                'MATCH'))
            if pruned(inpc):
                continue
            out.append('%d\tI\t%d\t%s\t%s\t%d\t%d\t%s\t%s' % (
                qid, inpc, isrc, itgt, iitem, ireq, irc_s, ifp_s))
        # NPC_START 两形态：自闭合（wave A）与带 accept-actions 子元素（wave B 接取发物）。
        for bm in re.finditer(r'<dialog type="NPC_START"([^>]*?)(?:/>|>(.*?)</dialog>)', text, re.S):
            attrs, blk_body = bm.group(1), bm.group(2) or ''
            npc = re.search(r'npc-id="(\d+)"', attrs).group(1)
            src = re.search(r'source="([^"]*)"', attrs).group(1)
            dst = re.search(r'target="([^"]*)"', attrs).group(1)
            sel = re.search(r'selection-sources="([^"]*)"', attrs)
            pg = re.search(r'start-page="([^"]*)"', attrs)
            accepts = []
            aam = re.search(r'<accept-actions>(.*?)</accept-actions>', blk_body, re.S)
            if aam:
                for am in re.finditer(r'<([a-z-]+)([^>]*)/>', aam.group(1)):
                    name2, attrs2 = am.group(1), am.group(2)
                    if name2 == 'give-item':
                        iid = re.search(r'item-id="(\d+)"', attrs2).group(1)
                        cnt = re.search(r'count="(\d+)"', attrs2)
                        accepts.append('GIVE_ITEM:%s:%s' % (iid, cnt.group(1) if cnt else '1'))
                    else:
                        fail('%d 未知 accept-action 元素 <%s>' % (qid, name2))
            if pruned(npc) or accept_pruned(npc):
                continue
            out.append('%d\tB\tNPC_START\t%s\t%s\t%s\t%s' % (qid, npc, src, dst,
                (sel.group(1) if sel else '-') + '|' + (pg.group(1) if pg else '-') + '|'
                + (';'.join(accepts) if accepts else '-')))
        for bm in re.finditer(r'<dialog type="NPC_REPORT"[^>]*/>', text):
            blk = bm.group()
            npc = re.search(r'npc-id="(\d+)"', blk).group(1)
            src = re.search(r'source="([^"]*)"', blk).group(1)
            dst = re.search(r'target="([^"]*)"', blk).group(1)
            pg = re.search(r'page="([^"]*)"', blk)
            if pruned(npc) or role_pruned(npc):
                continue
            out.append('%d\tB\tNPC_REPORT\t%s\t%s\t%s\t%s' % (qid, npc, src, dst,
                pg.group(1) if pg else '-'))
        for qm in QA.finditer(text):
            qsrc, qdst, qmid, qaction, qbody = qm.groups()
            qprio = re.search(r'priority="(\d+)"', qmid)
            qprio_s = qprio.group(1) if qprio else '-'
            seq += 1
            qcm = re.search(r'<conditions>(.*?)</conditions>', qbody, re.S)
            qam = re.search(r'<actions>(.*?)</actions>', qbody, re.S)
            qfm = re.search(r'<after-commit>(.*?)</after-commit>', qbody, re.S)
            qchecks = []
            qafters = encode_after(qfm.group(1) if qfm else '', qid, qchecks)
            out.append('%d\tQ\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s' % (qid, seq, qaction,
                qsrc, qdst, encode_conditions(qcm.group(1) if qcm else '', qid),
                encode_actions(qam.group(1) if qam else '', qid), qafters, qprio_s))
        # B/NPC_COMPLETE 全参数转写（extra 分节）：cri=完成奖励档 | fixed=固定奖励索引 |
        # actions=完成动作 raw | finish=收尾模式 | preview=预览动作 raw | choice=ri:actions;... |
        # fallback=动作 raw。与 QuestXmlBlockExpander.expandNpcComplete 语义一一对应。
        # B/NPC_COMPLETE 多块全参数转写（多变体任务可有多个完成块，判例 1484 十块）。
        for cm2 in re.finditer(r'<npc-complete npc-id="(\d+)" source="([^"]*)" target="([^"]*)"', text):
            nc_start = cm2.start()
            tag_end = text.find('>', nc_start) + 1
            open_tag = text[nc_start:tag_end]
            if open_tag.rstrip().endswith('/>'):
                seg = open_tag  # 自闭合：无子元素
            else:
                nc_close = text.find('</npc-complete>', tag_end)
                if nc_close < 0:
                    fail('%d npc-complete 未闭合' % qid)
                seg = text[nc_start:nc_close + 15]
            cri = re.search(r'complete-reward-index="(\d+)"', open_tag)
            fixed = re.search(r'fixed-reward-indices="([\d ]*)"', open_tag)
            acts = re.search(r'\sactions="([^"]*)"', open_tag)
            fin = re.search(r'finish="([A-Z_]+)"', open_tag)
            pv = re.search(r'<preview actions="([^"]*)"\s*/>', seg)
            choices = []
            for ch in re.finditer(r'<choice\b([^>]*)/>', seg):
                cattrs = ch.group(1)
                cri2 = re.search(r'reward-index="(\d+)"', cattrs)
                cact = re.search(r'\saction="([^"]*)"', cattrs)
                cacts = re.search(r'\sactions="([^"]*)"', cattrs)
                if not cri2 or not (cacts or cact):
                    fail('%d choice 子元素缺 reward-index/action：%s' % (qid, cattrs.strip()[:60]))
                choices.append('%s:%s' % (cri2.group(1), (cacts or cact).group(1)))
            fb = re.search(r'<fallback[^>]*?actions="([^"]*)"', seg)
            if '<after-commit' in seg:
                fail('%d npc-complete after-commit 子元素未支持' % qid)
            if not acts and not choices and not fb:
                fail('%d npc-complete 无完成路由（actions/choice/fallback 全缺）' % qid)
            extra = '|'.join([
                'cri=' + (cri.group(1) if cri else '-'),
                'fixed=' + (fixed.group(1).strip() if fixed else '-'),
                'actions=' + (acts.group(1) if acts else '-'),
                'finish=' + (fin.group(1) if fin else '-'),
                'preview=' + (pv.group(1) if pv else '-'),
                'choice=' + (';'.join(choices) if choices else '-'),
                'fallback=' + (fb.group(1) if fb else '-'),
            ])
            if pruned(cm2.group(1)) or role_pruned(cm2.group(1)):
                continue
            out.append('%d\tB\tNPC_COMPLETE\t%s\t%s\t%s\t%s' % (qid, cm2.group(1),
                cm2.group(2), cm2.group(3), extra))
        # E 记录：EnterWorld 无源事件（target 必填、无 source；QE-051 奖励行自愈边）。
        for em in EW.finditer(text):
            edst, ebody = em.groups()
            ecm = re.search(r'<conditions>(.*?)</conditions>', ebody, re.S)
            eam = re.search(r'<actions>(.*?)</actions>', ebody, re.S)
            efm = re.search(r'<after-commit>(.*?)</after-commit>', ebody, re.S)
            echecks = []
            eafters = encode_after(efm.group(1) if efm else '', qid, echecks)
            for page in echecks:
                page_total += 1
                in_letter = page in client_pages.get(qid, set())
                in_global = page in GLOBAL_PAGES
                if in_letter or in_global:
                    page_hit += 1
                elif qid not in keep_set and qid in b_set:
                    unverified_pages.append((qid, page))
                elif qid not in keep_set:
                    fail('%d enter-world 页 %s 未命中' % (qid, page))
            out.append('%d\tE\t%s\t%s\t%s\t%s' % (qid, edst,
                encode_conditions(ecm.group(1) if ecm else '', qid),
                encode_actions(eam.group(1) if eam else '', qid), eafters))
        seq = 0
        # C 记录：can-act 事件路由（掉落箱/交互物；与 R 同编码，事件列区分）
        for cm in re.finditer(r'<transition source="([^"]+)" target="([^"]+)"([^>]*)>\s*<event>\s*'
                r'<can-act template-id="(\d+)" action-type="([A-Z_]+)"\s*/>(.*?)</transition>', text, re.S):
            csrc, cdst, cmid, tpl, ctype, cbody = cm.groups()
            cprio = re.search(r'priority="(\d+)"', cmid)
            cprio_s = cprio.group(1) if cprio else '-'
            seq += 1
            ccm = re.search(r'<conditions>(.*?)</conditions>', cbody, re.S)
            cam = re.search(r'<actions>(.*?)</actions>', cbody, re.S)
            cfm = re.search(r'<after-commit>(.*?)</after-commit>', cbody, re.S)
            cchecks = []
            cafters = encode_after(cfm.group(1) if cfm else '', qid, cchecks)
            cchecks2 = []
            cpage_total_local = 0
            for page in cchecks:
                page_total += 1
                in_letter = page in client_pages.get(qid, set())
                in_global = page in GLOBAL_PAGES
                if in_letter or in_global:
                    page_hit += 1
                    cchecks2.append('%s=%s' % (page, 'CLIENT' if in_letter else 'GLOBAL'))
                elif qid in keep_set:
                    cchecks2.append('%s=SNAP' % page)
                elif qid in b_set:
                    cchecks2.append('%s=UNVERIFIED' % page)
                    unverified_pages.append((qid, page))
                else:
                    fail('%d can-act 页 %s 未命中' % (qid, page))
            if pruned(tpl):
                continue
            out.append('%d\tC\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s' % (qid, seq, tpl, ctype,
                csrc, cdst, encode_conditions(ccm.group(1) if ccm else '', qid),
                encode_actions(cam.group(1) if cam else '', qid), cafters,
                ';'.join(cchecks2) if cchecks2 else '-', cprio_s))
        for m in TR.finditer(text):
            src, dst, mid, npc, action_attr, body = m.groups()
            mprio = re.search(r'priority="(\d+)"', mid)
            prio_s = mprio.group(1) if mprio else '-'
            am2 = ACTIONS_ATTR.search(action_attr or '')
            tokens = am2.group(1).split() if am2 else ['-']
            # NPC 绑定核验：真端表 NPC 集为权威；路由 npc 不在集内 = XML 时代实现漂移
            # （真端表即真端客户端数据导出，表绑定胜——P0c-8c「NPC 漂移→ADOPT」判例），
            # 如实记 XML_ONLY 留证，交裁定波逐行复核。
            npc_check = ('RETAIL_MATCH' if npc in census_retail[qid]
                else 'CLIENT_MATCH' if npc in census_client[qid]
                else 'XML_ONLY:%s' % npc)
            cm = re.search(r'<conditions>(.*?)</conditions>', body, re.S)
            am = re.search(r'<actions>(.*?)</actions>', body, re.S)
            fm = re.search(r'<after-commit>(.*?)</after-commit>', body, re.S)
            page_checks = []
            afters = encode_after(fm.group(1) if fm else '', qid, page_checks)
            checks = []
            for page in page_checks:
                page_total += 1
                in_letter = page in client_pages.get(qid, set())
                in_global = page in GLOBAL_PAGES
                if in_letter or in_global:
                    page_hit += 1
                    checks.append('%s=%s' % (page, 'CLIENT' if in_letter else 'GLOBAL'))
                elif qid in keep_set:
                    checks.append('%s=SNAP' % page)
                elif qid in b_set:
                    checks.append('%s=UNVERIFIED' % page)
                    unverified_pages.append((qid, page))
                else:
                    fail('%d 页 %s 客户端 CSV 与全局白名单均未命中' % (qid, page))
            for action in tokens:
                if pruned(npc) or role_pruned_route(npc, action, page_checks) \
                        or accept_pruned_route(npc, action, page_checks, src):
                    continue
                row_checks = checks
                if qid in stage_spread_prune:
                    # P0c-54：阶段页跨 NPC 扩散——该行把**别的阶段**的页下发在非本阶段 NPC 上 ⇒ 剪除。
                    # 命中判定 = 靶行 NPC + 精确页令牌（页名有前缀关系：子串匹配会把 SELECT2_1 当成
                    # SELECT2）。靶 NPC 名下不含该页的行原样保留；同 NPC 命中两条以上 = 表含糊 ⇒ fail-closed。
                    targets = [x for x in stage_spread_prune[qid] if x[1] == npc]
                    if targets:
                        hit = [x for x in targets
                            if ('DIALOG:SHOW_QUEST_PAGE:' + x[0]) in afters.split(';')]
                        if len(hit) > 1:
                            fail('%d 阶段页扩散裁定 %s@%s：同 NPC 命中 %d 条靶行（表含糊）'
                                % (qid, npc, action, len(hit)))
                        if hit and action != 'QUEST_SELECT':
                            # 只剪**页动作行**：`QUEST_SELECT` 是被剪 NPC 自己的对话入口（接取/报告窗），
                            # 删掉它该 NPC 的对话整条消失（任务不可接）——那属「接取入口页」轴，另登记。
                            spread_page_pruned.add((qid, hit[0][1], hit[0][0]))
                            continue
                spread = stage_window_spread.get((qid, npc, action))
                if spread:
                    # P0c-47：阶段推进行领奖窗外溢收窄——末位窗外溢令牌替换为 CLOSE（SYNC 模式逐字保留）。
                    # 形状必须逐字匹配（窗外溢只此一处且在末位 + target 投影 = REWARD），否则 fail-closed。
                    if node_status.get(dst) != 'REWARD':
                        fail('%d 阶段窗外溢裁定 %s@%s：target %s 投影非 REWARD'
                            % (qid, npc, action, dst))
                    if afters.count(WINDOW_TOKEN) != 1 or not afters.endswith(';' + WINDOW_TOKEN):
                        fail('%d 阶段窗外溢裁定 %s@%s：after-commit 形状 %s 非「SYNC:<mode>;%s」'
                            % (qid, npc, action, afters, WINDOW_TOKEN))
                    head = afters[:-len(WINDOW_TOKEN) - 1]
                    if not head.startswith('SYNC:') or ';' in head:
                        fail('%d 阶段窗外溢裁定 %s@%s：窗外溢前非单一 SYNC token（%s）'
                            % (qid, npc, action, afters))
                    afters = head + ';CLOSE'
                    # 页列与本行 after-commit 同步：改写后该行不再下发领奖窗页，页列必须同步去掉
                    # （否则普查/审计仍会把领奖窗记在该阶段 NPC 名下 = 收窄未生效）。
                    row_checks = [c for c in checks
                        if not c.startswith(REWARD_WINDOW_PAGE + '=')]
                    spread_narrowed.add((qid, npc, action))
                seq += 1
                route_count += 1
                out.append('%d\tR\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s' % (qid, seq, npc,
                    action, src, dst,
                    encode_conditions(cm.group(1) if cm else '', qid),
                    encode_actions(am.group(1) if am else '', qid),
                    afters, npc_check, ';'.join(row_checks) if row_checks else '-', prio_s))
        if qid in stage_entry_rows:
            # P0c-54：阶段入口页被历史转写丢掉 ⇒ 在该阶段 NPC 上补 `QUEST_SELECT` 入口行（同阶梯通道
            # 的形状：入口页 token + page_check=<页>=CLIENT），续页/推进行原样保留。
            stage_k, entry_page, owner_npc = stage_entry_rows[qid]
            entry_src = 'started' if stage_k == 1 else 's%d' % (stage_k - 1)
            seq += 1
            route_count += 1
            out.append('%d\tR\t%d\t%s\tQUEST_SELECT\t%s\t%s\t-\t-\t%s\tRETAIL_MATCH\t%s=CLIENT\t-'
                % (qid, seq, owner_npc, entry_src, entry_src,
                    'DIALOG:SHOW_QUEST_PAGE:' + entry_page, entry_page))
            entry_rows_added.add(qid)
        if seq == 0:
            # 真端表说有链、XML 无链路由（只有规范块）：独立微波，按真端+客户端直接合成或 KEEP。
            # Retail table declares a chain but the legacy XML has no chain routes (canonical blocks
            # only): a separate micro-wave — synthesize from retail+client, or KEEP.
            no_routes.append(qid)
            continue
    if stage_window_spread:
        # P0c-47 收尾守卫①：裁定表每条都必须命中一条转写 R 行（陈旧裁定表 = fail-closed）。
        unconsumed = sorted(set(stage_window_spread) - spread_narrowed)
        if unconsumed:
            fail('阶段窗外溢裁定表有条目未命中转写行（裁定表腐化）：%s' % unconsumed)
        # 收尾守卫②**无页丢失**：改写后每个任务仍有 owner 名下开领奖窗的记录——`B NPC_COMPLETE`
        # 的 preview 非 '-'（编译器按档下发领奖窗页）或 owner 名下记录含领奖窗页令牌。
        spread_owner = {}
        for (q2, _n2, _a2), (own, _onm, _snm) in stage_window_spread.items():
            spread_owner.setdefault(q2, own)
        for q2 in sorted(spread_owner):
            own = spread_owner[q2]
            covered = False
            for line in out:
                parts2 = line.split('\t')
                if len(parts2) < 5 or int(parts2[0]) != q2 or parts2[3] != own:
                    continue
                if parts2[1] == 'B' and parts2[2] == 'NPC_COMPLETE' and 'preview=-' not in line:
                    covered = True
                if REWARD_WINDOW_PAGE in line:
                    covered = True
            if not covered:
                fail('%d 阶段窗外溢裁定：改写后 owner %s 名下无开领奖窗的记录（页丢失）'
                    % (q2, own))
        print('P0C47-WINDOW-SPREAD：收窄 %d 条 %s' % (len(spread_narrowed),
            ['%d(%s@%s)' % (q2, a2, n2) for (q2, n2, a2) in sorted(spread_narrowed)]))
    if stage_entry_rows or stage_spread_prune:
        # P0c-54 收尾守卫①：裁定表每条都必须命中/插入（陈旧裁定表 = fail-closed）。
        missed_entry = sorted(set(stage_entry_rows) - entry_rows_added)
        if missed_entry:
            fail('阶段页轴裁定：入口行未插入 %s（裁定表腐化）' % missed_entry)
        expect_prune = {(q2, x[1], x[0]) for q2, v in stage_spread_prune.items() for x in v}
        unconsumed_prune = sorted(expect_prune - spread_page_pruned)
        if unconsumed_prune:
            fail('阶段页轴裁定：越界行未命中转写行（裁定表腐化）：%s' % unconsumed_prune)
        # 收尾守卫②**无页丢失**：剪除后每页仍由 owner 下发（至少一条 owner 行含该页令牌）。
        owner_of = {}
        for q2, v in stage_spread_prune.items():
            for page2, _x2, own2, _k2 in v:
                owner_of[(q2, page2)] = own2
        for (q2, page2), own2 in sorted(owner_of.items()):
            kept = [line for line in out
                if line.split('\t')[0].isdigit() and int(line.split('\t')[0]) == q2
                and '\tR\t' in line and line.split('\t')[3] == own2
                and ('DIALOG:SHOW_QUEST_PAGE:' + page2) in line.split('\t')[9].split(';')]
            if not kept:
                fail('%d 阶段页轴裁定：剪除后 owner %s 名下无下发页 %s 的记录（页丢失）'
                    % (q2, own2, page2))
        print('P0C54-STAGE-PAGE：入口补行 %d/%d；越界剪行 %d 条 %s'
            % (len(entry_rows_added), len(stage_entry_rows), len(spread_page_pruned),
                ['%d(%s@%s)' % (q2, x[2], x[1]) for (q2, x) in sorted(
                    (q2, (p2, n2, o2)) for (q2, n2, p2) in spread_page_pruned
                    for o2 in [owner_of[(q2, p2)]])]))
    prune_qids = (set(extra_owner_prune) | set(role_narrowing)
        | set(stage_entry_rows) | set(stage_spread_prune) | set(accept_narrowing))
    if prune_qids:
        # 剪除后重编 R 序号（保持 1..N 连续；同 P0c-10i 阶段插入后的口径）——
        # 不留序号空洞，免得后续按 seq 对照的脚本误读。
        seqno = collections.defaultdict(int)
        for i, line in enumerate(out):
            parts = line.split('\t')
            if len(parts) > 2 and parts[1] == 'R' and int(parts[0]) in prune_qids:
                seqno[int(parts[0])] += 1
                parts[2] = str(seqno[int(parts[0])])
                out[i] = '\t'.join(parts)
        for q in sorted(extra_owner_prune):
            print('P0C45-PRUNE %d：剪除多余 owner %s 的记录 %d 条（保留 %s），R 序号重编 1..%d'
                % (q, extra_owner_prune[q][0], prune_dropped[q],
                    extra_owner_prune[q][2], seqno[q]))
        for q in sorted(role_narrowing):
            print('P0C46-ROLE-NARROW %d：剪除交付角色记录 %d 条（被剪 %s / 保留 owner %s），R 序号重编 1..%d'
                % (q, role_dropped[q], '/'.join(pn for pn, _, _ in role_narrowing[q]),
                    role_owner[q][1], seqno[q]))
        for q in sorted(accept_narrowing):
            print('P0C57-ACCEPT-NARROW %d：剪除接取入口块 %d 个 + 接取族行 %d 条（被剪 %s / 保留 owner %s），'
                'R 序号重编 1..%d' % (q, len(accept_narrowing[q]), accept_row_dropped[q],
                    '/'.join(pn for pn, _, _ in accept_narrowing[q]), accept_owner[q][1], seqno[q]))

        # P0c-57 收尾守卫（轴⑧）：收窄后每个任务的 NPC_START 块必须**只剩 owner 一个**——
        # 裁定表漏列被剪 NPC 会让块留在产物里而表却声称已收窄 ⇒ fail-closed。
        # P0c-57 closing guard: exactly one NPC_START block (the owner) must survive per quest.
        for q in sorted(accept_narrowing):
            kept = [ln.split('\t') for ln in out if ln.startswith('%d\tB\tNPC_START\t' % q)]
            owners = sorted({r[3] for r in kept})
            if owners != [str(accept_owner[q][0])]:
                fail('%d 接取入口收尾守卫：收窄后 NPC_START 块 owner=%s ≠ 期望唯一 %s'
                    % (q, owners, accept_owner[q][0]))
            print('P0C57-ACCEPT-KEEP %d：保留 NPC_START 块 1 个（owner %s，start-page %s），剪除 %d 个'
                % (q, owners[0], kept[0][6].split('|')[1], len(accept_narrowing[q])))

    # P0c-10i：canonical 规范合成批（轴分歧行；真端表为形状权威，页名 CLIENT=信件页核验）。
    canonical_done = 0
    canonical_gaps = []
    for qid in sorted(canonical_pending | collect_gate_pending):
        body = retail_body.get(qid, '')
        if not re.search(r'<talk_npc1>', body):
            print('CANONICAL-SKIP %d：无 talk_npc1（非阶段轴形状）' % qid)
            canonical_gaps.append(qid)
            continue
        # P0c-11：collect-gate 行绕过 work_items 守卫（门物品走 collect_item 通道）。
        # P0c-38：work_items 不再是硬门——真端 item 名索引可解全部 give/remove 符号时同样放行
        # （quest_data.xml 陈旧；索引不可解的符号仍 KEEP，理由带符号名）。
        if qid not in collect_gate_pending and not canonical_work_items.get(qid):
            syms = sorted(set(re.findall(
                r'<(?:give_item\d*|remove_item\d*)>(ITEM_[A-Z0-9_]+)', body)))
            unresolved = [s for s in syms if s.replace('ITEM_', '').lower() not in item_ids_by_name]
            if unresolved:
                print('CANONICAL-GAP %d：符号 %s 经真端 item 名索引无解（且 quest_data 无 work_items）——KEEP'
                    % (qid, unresolved))
                canonical_gaps.append(qid)
                continue
            print('CANONICAL-INDEX %d：quest_data 无 work_items，改走真端 item 名索引（%d 符号）'
                % (qid, len(syms)))
        synthesize_canonical(qid, body, npc_ids_by_name,
            canonical_work_items.get(qid, []), out,
            collect_gates=collect_gates.get(qid))
        canonical_done += 1
    print('canonical 合成：%d 行；缺口（KEEP）：%d 行 %s'
        % (canonical_done, len(canonical_gaps), canonical_gaps))
    if item_channel:
        (HERE / 'p0c38-canonical-item-channel.tsv').write_text(
            '# P0c-38 canonical 行 item 符号解析通道（真端 item_name_index vs quest_data work_items 序数）\n'
            '# verdict: MATCH = 两通道一致；INDEX_ONLY = 仅真端索引可解；DIVERGED_RETAIL_WINS = 分歧取真端；'
            'POSITIONAL_FALLBACK = 真端索引无解退回历史通道\n'
            '# quest_id\tsymbol\tretail_index_id\twork_items_id\tverdict\n'
            + '\n'.join('%d\t%s\t%s\t%s\t%s' % r for r in sorted(set(item_channel))) + '\n',
            encoding='utf-8')
        print('item 符号通道留痕：%d 条' % len(set(item_channel)))
    if canonical_gaps:
        (HERE / 'p0c10i-canonical-gaps.tsv').write_text(
            '# canonical 合成缺口（work_items 通道缺失或非阶段轴）——KEEP\n'
            + '\n'.join(str(q) for q in sorted(canonical_gaps)) + '\n', encoding='utf-8')
    diverged = [r for r in reward_projection_rows if int(r[1]) != r[3]]
    if diverged:
        (HERE / 'p0c34-chain-reward-row-divergence.tsv').write_text(
            '# P0c-34 链登记表 REWARD 投影：XML 转写值与客户端任务书末行行号分歧（待裁定波）\n'
            '# quest_id\tXML var0\t登记表实际值\t客户端末行行号\t通道\n'
            + '\n'.join('%d\t%s\t%s\t%d\t%s' % r for r in diverged) + '\n', encoding='utf-8')
        print('REWARD 投影分歧（XML vs 客户端任务书）：%d 行 %s'
            % (len(diverged), sorted(r[0] for r in diverged)))
    if item_report_crosscheck:
        (HERE / 'p0c34-item-report-crosscheck.tsv').write_text(
            '# P0c-34 npc-item-report（item_check 门）转写与真端 cross-check\n'
            '# quest_id\tnpc_id\t真端 collect_item 解析\trequired\titem_id\tremove_count'
            '\tfailure_page\t判定\n'
            + '\n'.join(item_report_crosscheck) + '\n', encoding='utf-8')
        print('I 记录（item_check 门）：%d 行 %s' % (len(item_report_crosscheck),
            [r.split('\t')[0] for r in item_report_crosscheck]))
    if ladder_rows:
        # 按任务分组（发射循环按 quest 连续输出），逐行做阶梯改写。
        groups = collections.OrderedDict()
        headers = [line for line in out if line.startswith('#')]
        for line in out:
            if line.startswith('#'):
                continue
            groups.setdefault(int(line.split('\t')[0]), []).append(line)
        laddered = []
        for qid, (k, mode, basis) in sorted(ladder_rows.items()):
            if qid not in groups:
                fail('%d 阶梯裁定行不在登记输出内（真端表/波次不匹配）' % qid)
            qxm = re.search(r'<quest>\s*<id>%d</id>.*?</quest>' % qid, retail_quest_xml_text, re.S)
            pm3 = re.search(r'<collect_progress>(\d+)</collect_progress>', qxm.group(0)) if qxm else None
            groups[qid] = apply_talk_ladder(qid, k, mode, basis, groups[qid], npc_ids_by_name,
                client_summary_rows, int(pm3.group(1)) if pm3 else 0, client_pages, client_actions,
                client_page_ids, retail_body.get(qid, ''))
            laddered.append(qid)
        out = headers + [line for lines in groups.values() for line in lines]
        print('阶梯改写行：%s' % laddered)
    if accept_entrance_rows:
        # 按任务分组（与阶梯改写同址），逐行做接取入口重绑。
        groups = collections.OrderedDict()
        headers = [line for line in out if line.startswith('#')]
        for line in out:
            if line.startswith('#'):
                continue
            groups.setdefault(int(line.split('\t')[0]), []).append(line)
        rebound = []
        for qid, (box_id, entry_action, entry_page, ask_page, ladder_pages, basis) in \
                sorted(accept_entrance_rows.items()):
            if qid not in groups:
                fail('%d 接取入口裁定行不在登记输出内（真端表/波次不匹配）' % qid)
            groups[qid] = rebind_accept_entrance(qid, box_id, entry_action, entry_page, ask_page,
                ladder_pages, basis, groups[qid], client_pages, client_actions,
                client_action_constants, client_page_constants, client_page_id_by_name,
                page_enum_ids)
            rebound.append(qid)
        out = headers + [line for lines in groups.values() for line in lines]
        print('接取入口重绑行：%s' % rebound)
    if stage_leg_rows:
        # 按任务分组（与阶梯/接取入口改写同址），逐行做阶段腿重建。
        groups = collections.OrderedDict()
        headers = [line for line in out if line.startswith('#')]
        for line in out:
            if line.startswith('#'):
                continue
            groups.setdefault(int(line.split('\t')[0]), []).append(line)
        rebuilt = []
        for qid, (k, report_page, basis) in sorted(stage_leg_rows.items()):
            if qid not in groups:
                fail('%d 阶段腿重建裁定行不在登记输出内（真端表/波次不匹配）' % qid)
            groups[qid] = rebuild_stage_legs(qid, k, report_page, basis, groups[qid],
                npc_ids_by_name, client_summary_rows, client_pages, client_actions, client_page_ids,
                client_action_constants, retail_body.get(qid, ''))
            rebuilt.append(qid)
        out = headers + [line for lines in groups.values() for line in lines]
        print('阶段腿重建行：%s' % rebuilt)
    OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')
    if no_routes:
        (HERE / 'p0c10f-chain-no-routes.tsv').write_text(
            '# XML 无链路由的 wave A 行（真端表有 talk_npcN、XML 仅规范块）——独立微波裁定\n'
            + '\n'.join(str(q) for q in no_routes) + '\n', encoding='utf-8')
    if deferred_b:
        (HERE / 'p0c10h-chain-deferred-compound.tsv').write_text(
            '# wave B 复合行留裁批（真端含 con_quest 或 cutscene 轴，物品轴不完整）——独立裁定波\n'
            + '\n'.join(str(q) for q in sorted(deferred_b)) + '\n', encoding='utf-8')
    if axis_mismatch:
        (HERE / 'p0c10h-chain-axis-mismatch.tsv').write_text(
            '# wave B 行真端轴计数与 XML 结构性分歧（转写不成立）——按真端规范合成或 KEEP\n'
            + '\n'.join(str(q) for q in sorted(axis_mismatch)) + '\n', encoding='utf-8')
    if unverified_pages:
        (HERE / 'p0c10h-chain-unverified-pages.tsv').write_text(
            '# wave B 行客户端页证据未核验（page_check=UNVERIFIED）——裁定波逐行复核\n'
            + '\n'.join('%d\t%s' % it for it in sorted(set(unverified_pages))) + '\n', encoding='utf-8')
    print('页证据未核验（wave B）：%d 处 / %d 行' % (
        len(set(unverified_pages)), len({q for q, _ in unverified_pages})))
    print('登记表 -> %s（%d 行路由）' % (OUT, route_count))
    print('XML 无链路由（残组）：%d 行 %s' % (len(no_routes), no_routes))
    print('页面交叉核验：%d/%d 命中（CLIENT=信件页 / GLOBAL=引擎常页）' % (page_hit, page_total))


if __name__ == '__main__':
    main()
