#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-55 台账收口（续）：DoD 时点读数 + 证据索引行 + 变更日志行；阻塞登记表闭行。
P0c-55 ledger closure (part 2): DoD readings + evidence-index row + changelog row; blocked-row closure."""
import pathlib
import sys

HERE = pathlib.Path(__file__).parent
LEDGER = HERE / 'GOAL-retail-driver-progress.zh-CN.md'
BLOCKED = HERE / 'p0c42-blocked-rows.tsv'

DOD_ANCHOR = '；T3 = `gates/T3-224433.log`（待填））\n'
DOD_ADD = (
    '；**P0c-55 时点（SimpleTalk 链车道 / 续片 32，阶段腿逐阶段重建 9 任务后，判例 QE-080）**：'
    '净树 **20/20**（链门 2 + 审计 17 + 契约门 1 ⇒ **契约门仍 fatal 0**，接取侧未动故页下发不变）、'
    'T1 `gates/T1-220318.log` **75 例 1F**（唯一红 = lane `20035`，行号 `:423 → :430` 系车道改测试文件、消息逐字相同）、'
    'T2（9 id，`gates/T2-221103.log`）**93 例 4F**（四条身份全在装入前 T3 基线身份集内）、'
    'T3 `gates/T3-221828.log` **2013 例 117F/22E/1S** 对装入前两基线 `T3-212101`/`T3-213222` 失败身份集**逐条相同**'
    '（各 139 条失败行 / 107 唯一身份，`only_in_base = []` / `only_in_post = []`）⇒ 零新增零消失；'
    '保真 `FIDELITY_OK` 419803 字节 + 23 表零漂移\n'
)

EVIDENCE_ANCHOR_PREFIX = '| 2026-09-26 | **P0c-47 门禁（净树 / T1 / T2 / T3）** |'
EVIDENCE_ROW = (
    '| 2026-09-26 | **P0c-55 门禁（净树 / 保真 / 普查 / T1 / T2 / T3）** | '
    '`-Dtest=\'RetailSimpleTalkChainGateTest,QuestDialogOrderAuditTest,QuestClientContractGateTest\'`（20/20）+ '
    '`p0c42_builder_fidelity_check.py`（`FIDELITY_OK` 419803 + 23 表）+ `p0c55_stage_leg_census.py`（pre 缺陷 24 → post 0）+ '
    '`run_quest_gates.sh T1`（`gates/T1-220318.log`）+ `run_quest_gates.sh T2 1323 1394 1484 2480 2538 3093 4052 11010 11103`'
    '（`gates/T2-221103.log`）+ `QUEST_FORK_COUNT=2 run_quest_gates.sh T3`（`gates/T3-221828.log`，22:18:28–22:24:37） | '
    '净树 **20/20**；保真 419803 字节 + **23 张**裁定表零漂移（登记表 `5bf47fde…` 由生成器复现）；'
    '普查 pre `{OK 6, REPORT_OK 1, 缺陷 24}` → post **`{OK 22, REPORT_OK 9}`**（31 行全绿）；'
    'T1 **75 例 1F**（lane `20035`；本片 9 id 零命中）；T2 **93 例 4F**（选择器 25 类 = 当前 `affected_quest_tests.py` 复算值，'
    '日志内 25 类逐一运行 = 选择器口径无漂移；四条身份全在装入前 T3 基线身份集内）；'
    '**T3 2013 例 / 117F / 22E / 1S** —— 与**装入前**两基线 `gates/T3-212101.log`（21:28）/ `gates/T3-213222.log`（21:38）'
    '（同 2013/117/22/1）**失败身份集逐条相同**（各 139 条失败行 / 107 个唯一身份；`only_in_base = []`、`only_in_post = []`）'
    '⇒ **零新增、零消失**（本片装入 21:58:22 登记表 / 22:01:57 指纹，严格在基线之后）；'
    '诚实口径：同窗口并发 DataDriven 车道亦在改树（其自身 T3 = `gates/T3-221706.log`，2015 例，**不属本片**）'
    '⇒ T3 覆盖合并树；本片自因面锐化证据 = 改动面仅 2 文件 + `CHANGED` 恰 9 id + 9 id 在全量失败条目零命中 |\n'
)

CHANGELOG_TAIL_GUARD = '**P0c-55'
CHANGELOG_ADD = (
    '\n- **2026-09-26 续片 32 / P0c-55（SimpleTalk 链车道）：阶段腿逐阶段重建 —— 9 个多阶段任务的页/推进动作按真端与客户端复原'
    '（判例 QE-080）**：①**缺口定形**——P0c-54 登记的 `STAGE_PAGE_ADVANCE_NPC_DRIFT_PENDING` 9 任务不是"NPC 漂移"而是**塌缩签名**：'
    'XML 期每条推进都写 `action="SETPRO1"`、每阶段都下发 `SELECT2`/`SELECT2_1`（HEAD 注释自承「对话链按客户端按钮图重建」）'
    '⇒ 阶段 k≥2 把 `var0` 设回 1（后续阶段不可达）+ **34 行 `CLIENT_PAGE_UNREACHED`**（阶段 2/3 页 `1693/1694`、`2034/2035` 与领奖页 `2375` 从未下发）。'
    '②**裁决七轴 fail-closed**（真端 `talk_npc<k>`/`reward_npc_name` 唯一 + 任务书行数 == K+1（QE-051）+ 每阶段两页链 + owner 逐跳相接 + 页行按动作类分派 + '
    '领奖行三形互斥 + 越界剪除 + 收尾两守卫）。③**关键方法判例（QE-080）**——**腿必须按 owner 锚定 + 链位认，不能按页名/动作名认腿**：'
    '首版 census 按 `SETPRO1` 名匹配 = 循环论证（塌缩本身把名字改错）⇒ 假 `LEG_MISSING`/`LEG_DUPLICATE`/`PAGE_CHAIN_WRONG` 三类误判（三轮返工）；'
    '合法重名由链位/状态消歧（2538 `talk1 == talk3`、11010 `reward == talk1`）；页行按**动作类**分派（页名动作行 = 续页行；`QUEST_SELECT`/`USE_OBJECT` = 入口页行，'
    '1394 双入口行合法保留）；领奖行三形（已正确 1323 packed K+1 / 塌缩改写 1394 / 缺则插在 packed K 的 `REWARD` 节点 7 个）；与阶梯表互斥、与接取入口表共存（1323 两表同裁）。'
    '④**落地**——登记表 **行数不变** 5052（+8 插 −7 剪），`43e37883…` → **`5bf47fde…`**（419803 字节）；爆炸半径 `CHANGED` 恰 9 任务 / `ADDED,REMOVED []`；'
    '**IR -49/+49 全落靶面**（阶段 2/3 页路由、阶段 2/3 腿 `10000→10001/10002`、领奖页 `31→2375`），物品轴与领奖窗逐字保留；'
    '**审计 9 任务 34 → 0**、全表 **1348 → 1314**（`ONLY_POST = 0`）；指纹外科重冻 9 值 `c9657153…` → **`aaf2b9c0…`**。'
    '⑤**门禁**——净树 20/20、保真 23 表零漂移、普查 pre 24 缺陷 → post 全绿、T1 75 例 1F（lane `20035`）、T2 93 例 4F（四条全在装入前 T3 基线身份集内）、'
    '**T3 2013 例 117F/22E/1S 与装入前两基线失败身份集逐条相同（ADDED 0 / REMOVED 0）**。'
    '⑥**残留**——接取侧两轴未动（`XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING` 34 项 / 25 任务 = 下一面；`ACCEPT_ENTRY_PAGE_WRONG_PENDING` 7 条）、'
    '11106 `SELECT3_1/SELECT3_2` 双份仍在、运行时目检 PENDING（未启服）。报告 `reports/2026-09-26-P0c55-stage-leg-rebuild.zh-CN.md`。\n'
)

BLOCKED_OLD_IDS = '2611,3001,3023,11070,11105,11106,11117,19004,21004,21036,21136,35017,45010,45017,45024,45026,39003,49003\tSTAGE_PAGE_ADVANCE_NPC_DRIFT_PENDING(新登记)\t'
BLOCKED_NEW_IDS = '1323,1394,1484,2480,2538,3093,4052,11010,11103\tSTAGE_PAGE_ADVANCE_NPC_DRIFT_PENDING(→ P0c-55 收口)\t'
BLOCKED_NOTE = (
    ' ⇒ **P0c-55 收口（STAGE_LEG_PARAM_COLLAPSE_CLOSED）**：该"漂移"实为**阶段腿参数塌缩**——XML 期把 9 任务的每条推进写成 `SETPRO1`、'
    '每阶段下发 `SELECT2`/`SELECT2_1`（模板未按阶段参数化）⇒ 阶段 k≥2 把 `var0` 设回 1、阶段 2/3 页与领奖页共 **34 行 `CLIENT_PAGE_UNREACHED`**。'
    '按真端 `talk_npc<k>`/`reward_npc_name` + 客户端任务书行（K+1 行）+ 每阶段两页链 + **owner 逐跳相接**（不得按页名/动作名认腿，判例 QE-080）七轴重建：'
    '登记表 5052 行不变（+8 插 −7 剪，`5bf47fde…`）、爆炸半径恰 9 任务 / `ADDED,REMOVED []`、IR -49/+49 全落靶面、'
    '**审计 9 任务 34 → 0 / 全表 1348 → 1314（`ONLY_POST = 0`）**、普查 post 31 行全绿、指纹 `c9657153…` → `aaf2b9c0…`、T3 零新增身份。'
    '残留（本行不再跟踪）：接取侧 `XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING` 34 项 / 25 任务与 `ACCEPT_ENTRY_PAGE_WRONG_PENDING` 7 条（转下一面）、'
    '11106 `SELECT3_1/SELECT3_2` 双份（`INFO_STAGE_SPREAD` 28 项 / 25 任务口径，本片裁定表只含 9 塌缩任务 ⇒ 未动）、'
    '运行时/客户端目检 PENDING（未启服）。**首列说明**：登记时首列误抄了 P0c-54 扩散集（18 id），本片按登记正文更正为这 9 个任务 id。'
)

# --- 台账 ------------------------------------------------------------------
ledger = LEDGER.read_text(encoding='utf-8')
if ledger.count(DOD_ANCHOR) != 1:
    sys.exit('FAIL-CLOSED: DoD 锚点命中 %d 次（期望 1）' % ledger.count(DOD_ANCHOR))
if 'P0c-55 时点（SimpleTalk 链车道 / 续片 32' in ledger:
    sys.exit('FAIL-CLOSED: DoD P0c-55 段落已存在（幂等保护）')
ledger = ledger.replace(DOD_ANCHOR, DOD_ANCHOR.rstrip('\n') + DOD_ADD, 1)

lines = ledger.split('\n')
idx = [i for i, ln in enumerate(lines) if ln.startswith(EVIDENCE_ANCHOR_PREFIX)]
if len(idx) != 1:
    sys.exit('FAIL-CLOSED: 证据索引末行锚点命中 %d 次（期望 1）' % len(idx))
if any('P0c-55 门禁（净树 / 保真 / 普查 / T1 / T2 / T3）' in ln for ln in lines):
    sys.exit('FAIL-CLOSED: 证据索引 P0c-55 行已存在（幂等保护）')
lines.insert(idx[0] + 1, EVIDENCE_ROW.rstrip('\n'))
ledger = '\n'.join(lines)

if CHANGELOG_TAIL_GUARD in ledger.split('## 变更日志（每轮追加一行）')[-1]:
    sys.exit('FAIL-CLOSED: 变更日志已含 P0c-55（幂等保护）')
ledger = ledger.rstrip('\n') + '\n' + CHANGELOG_ADD
LEDGER.write_text(ledger, encoding='utf-8')
print('LEDGER_UPDATE2_OK bytes=%d lines=%d' % (len(ledger.encode('utf-8')), len(ledger.split('\n'))))

# --- 阻塞登记表 -------------------------------------------------------------
blocked = BLOCKED.read_text(encoding='utf-8')
if blocked.count(BLOCKED_OLD_IDS) != 1:
    sys.exit('FAIL-CLOSED: 阻塞登记行锚点命中 %d 次（期望 1）' % blocked.count(BLOCKED_OLD_IDS))
if 'STAGE_LEG_PARAM_COLLAPSE_CLOSED' in blocked:
    sys.exit('FAIL-CLOSED: 阻塞登记表已含 P0c-55 闭行说明（幂等保护）')
new_rows = []
for row in blocked.split('\n'):
    if row.startswith(BLOCKED_OLD_IDS):
        rest = row[len(BLOCKED_OLD_IDS):]
        row = BLOCKED_NEW_IDS + rest.rstrip() + BLOCKED_NOTE
    new_rows.append(row)
blocked = '\n'.join(new_rows)
BLOCKED.write_text(blocked, encoding='utf-8')
print('BLOCKED_ROW_CLOSED_OK bytes=%d lines=%d' % (len(blocked.encode('utf-8')), len(blocked.split('\n'))))
