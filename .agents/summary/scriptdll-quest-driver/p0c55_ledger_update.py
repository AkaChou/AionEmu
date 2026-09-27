#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-55 台账收口：下一面改写 + 当前切片顶部插入（幂等，锚点必须各命中一次）。
P0c-55 ledger closure: rewrite the next-face bullet and prepend the slice bullet (idempotent, each anchor must match once)."""
import pathlib
import sys

LEDGER = pathlib.Path(__file__).with_name('GOAL-retail-driver-progress.zh-CN.md')

NEXT_FACE = (
    '- **下一面（P0c-55 收口后，续片 33 候选）：接取轴两登记项 —— 接取角色扩散 `XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING`'
    '（34 项 / 25 任务）与接取入口页错位 `ACCEPT_ENTRY_PAGE_WRONG_PENDING`（7 条）**（同属接取侧、**方向与判据都不同**，'
    '落地前必须先分形）——**已取证据（只读，勿重做）**：①扩散 34 项的行形态是接取页（`SELECT1_1` 接取窗续页等）被铺到'
    '**非接取 NPC**：3087@700419/798144、2538@204805/790002、2914@204236、3037@798199、3041@700378、'
    '3093@203784/798177/798179、11010@730323/798906/798931、11103@798963/798973 等（`p0c42-blocked-rows.tsv` 第 32 行），'
    '与 P0c-42 `ACCEPT_PAGE_NOT_EMITTED`（缺页）**方向相反**；②入口页错位 7 条是 `QUEST_SELECT` 行下发的页是**阶段页**'
    '（35017@799806 / 45010@799848 / 45017@799849 / 45024@799842·799843 / 45026@799842·799843，同表第 38 行）；'
    '③**关键交互面（判据必须先证增量效应）**——编译器 `acceptFlow` 会**自己下发**接取页（P0c-43 §6 已实证：`NPC_START` 块 '
    '`startPages=SELECT1` 已覆盖 SIMPLE 按钮族 1011 的 20000/20001 `PAGE_ACTION_MATCHED`；物件接取者另需重绑 `Q`，QE-070 边界）'
    '⇒ "谁下发接取页"是**编译器职责与登记表行的叠印**，禁止只按「页归属 ≠ 角色」单点改行；'
    '④**校准法（QE-072）**：判据必须在已知坏态（`p0c54-registry-pre.tsv` / `p0c43-registry-pre.tsv`）触发、在现态不触发。'
    '**同面未收**：11106 的 `SELECT3_1/SELECT3_2` 在 `talk_npc1/talk_npc2` 上双份（`INFO_STAGE_SPREAD` 28 项 / 25 任务口径，'
    'P0c-55 裁定表只含 9 个塌缩任务 ⇒ 未动）；21033 阶段 2 客户端页缺失 / 80370 之外的 80320 页链断裂仍另轴。\n'
)

SLICE = (
    '- 已完成：**P0c-55（SimpleTalk 链车道 / 续片 32）阶段腿逐阶段重建：9 个多阶段任务的页面/推进动作按真端与客户端复原'
    '（判例 QE-080）**：（编号消歧：与并发 DataDriven 车道的 `P0c-56 / 续片 29`（其跳号自标）各自独立，**唯一键 = lane + 续片号**；'
    '`p0c55*` 为本片独占前缀。）①**缺口定形（不是"NPC 漂移"是"塌缩签名"）**——P0c-54 登记的 `STAGE_PAGE_ADVANCE_NPC_DRIFT_PENDING` '
    '9 任务（1323/1394/1484/2480/2538/3093/4052/11010/11103）在 XML 期被**压成第一阶段参数**：每条推进 `<transition>` 都写 '
    '`action="SETPRO1"`、每个阶段都下发 `SELECT2`/`SELECT2_1`（HEAD 版注释自承「对话链按客户端按钮图重建」= 模板未按阶段参数化）'
    '⇒ 阶段 k≥2 把 `var0` 设回 1（后续阶段状态机不可达）+ **34 行 `CLIENT_PAGE_UNREACHED`**（阶段 2/3 页 `1693/1694`、`2034/2035` 与'
    '领奖页 `2375` 在编译产物 IR 里**从未被下发**；1323 的领奖页本就正确）。②**裁决七轴 fail-closed**（`p0c55-stage-leg-decisions.tsv` 9 行）'
    '——真端轴（`talk_npc1..K` 连续 + 名字唯一解析 + `reward_npc_name` 唯一 + `collect_progress` **不存在**，有收集段即交回阶梯轴）/ '
    '客户端轴（任务书行数 == K+1（QE-051），行 0..K-1 逐行点名 `talk_npc<k>`、行 K 点名 `reward_npc_name`；每阶段两页链 '
    '`SELECT{k+1} → 按钮 → SELECT{k+1}_1` 带 `HACTION_SETPRO{k}`；领奖页 `SELECT5` 唯一按钮常量 == `HACTION_SELECT_QUEST_REWARD` 且与阶段页不同名）/ '
    '登记表轴 / 页行分派 / 领奖行三形 / 越界剪除 / 收尾两守卫。③**关键方法判例（QE-080）——腿必须按 owner 锚定 + 链位认，绝不能按页名/动作名认腿**：'
    '腿 k = `owner == talk_npc<k>` ∧ `src` == 前一跳 `dst`（首跳 `src == started`）∧ 末跳 dst 节点 packed == K；'
    '首版 census 按 `SETPRO1` 名匹配 ⇒ **循环论证**（塌缩本身把名字改错 ⇒ 假 `LEG_MISSING`/`LEG_DUPLICATE`/`PAGE_CHAIN_WRONG` 三类误判，三轮返工）；'
    '合法重名由**链位/状态**消歧（2538 `talk1 == talk3`、11010 `reward == talk1`）。**页行按动作类分派**：页名动作行（`SELECT\\d+(_\\d+)?`）= 续页行'
    '（动作+页 → `SELECT{k+1}_1`），其余（`QUEST_SELECT`/`USE_OBJECT`）= 入口页行（页 → `SELECT{k+1}`）；1394 的 `actions="USE_OBJECT QUEST_SELECT"` '
    '双入口行**合法保留**。**领奖行三形互斥**：已正确（1323，其 reward 节点 packed == K+1 故"插在 packed K"形不适用）/ 塌缩（1394，奖 NPC 的 `QUEST_SELECT` '
    '行下发阶段页 ⇒ 改写其页）/ 缺（其余 7 个，在 packed == K 的 `REWARD` 节点上插入）。**通道共存域**：与**阶梯表互斥**（同改阶段段）、与**接取入口表共存**'
    '（1323 同受两表裁定，接取段 vs 阶段/领奖段页动作不相交）。④**落地（生成器新通道 `rebuild_stage_legs`）**——动作名 `SETPRO{k}` 9 处、入口页 14 处、'
    '续页 8 处、领奖行 1 改写 + 7 插入、越界剪除 7 条；登记表 5052 行**行数不变**（+8 插 −7 剪，`43e37883…` → **`5bf47fde…`**，419803 字节）；'
    '爆炸半径 `CHANGED` 恰 9 任务 / `ADDED,REMOVED []`。⑤**IR/审计（修复面 == 缺陷面）**——IR 逐行对拍 **-49/+49 全落三类靶面**（阶段 2/3 页路由、'
    '阶段 2/3 腿 `10000 → 10001/10002`（阶段 1 保持 `10000`）、领奖页 `dialogId 31 → 2375`），物品轴（`GiveItem/RemoveItem`）与领奖窗行逐字保留；'
    '**审计 9 任务未达/EVIDENCE 34 → 0**、全表 **1348 → 1314**（`ONLY_POST = 0` 零新增）；指纹**外科重冻 9 值**（其余 276 行逐字节不变）'
    '`c9657153…` → **`aaf2b9c0efdd0879f139dfab3fe4a31f`**（主/副本双写）；普查 pre `{OK 6, REPORT_OK 1, 缺陷 24}` → post **`{OK 22, REPORT_OK 9}`**（31 行全绿）。'
    '⑥**门禁**——净树 **20/20**（链门 2 + 审计 17 + 契约门 1）、保真 `FIDELITY_OK` 419803 字节 + **23 张**裁定表零漂移（生成器重跑逐字节相同）、'
    'T1 **75 例 1F**（在册 lane `20035`；两文件之改动面=9 任务，与 lane 零交集）、T2（9 id，`gates/T2-221103.log`）**93 例 4F**（四条身份**全部**在装入前 T3 基线身份集内）、'
    '**T3（`gates/T3-221828.log`，22:18:28→22:24:37）2013 例 / 117F / 22E / 1S，对装入前两基线 `T3-212101`/`T3-213222` 失败身份集逐条相同'
    '（各 139 条失败行 / 107 个唯一身份，`only_in_base = []`、`only_in_post = []`）⇒ 零新增零消失**；诚实口径：同窗口并发 lane 也在改树'
    '（其自身 T3 = 22:17 的 `T3-221706.log`，2015 例，**不属本片**），故 T3 覆盖的是合并树，本片自因面的锐化证据 = 改动面只有 2 文件 + `CHANGED` 恰 9 id + 9 id 零命中。'
    '⑦**残留**——接取侧两轴**未动**（`XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING` 34 项 / 25 任务、`ACCEPT_ENTRY_PAGE_WRONG_PENDING` 7 条：本片剪除面只含阶段/领奖页，'
    '`SELECT1*` 一律不碰，因与编译器 `acceptFlow` 的页下发叠印、判据须另立 ⇒ 转为下一面）；`GATE_VS_ITEM_CHECK_PENDING`（21033/21455）另形；'
    '运行时/客户端目检 **PENDING（未启服，需授权）**。报告 `reports/2026-09-26-P0c55-stage-leg-rebuild.zh-CN.md`；'
    '机械 `p0c55_stage_leg_census.py` / `p0c55_refreeze_fingerprints.py` / `p0c55_ledger_update.py`；对拍产物 `p0c55-registry-pre.tsv` / `p0c55-blast-radius.txt` / '
    '`p0c55-ir-{pre,post}.txt` / `p0c55-audit-{pre,post}.txt` / `p0c55-audit-all-{pre,post}.txt` / `p0c55-fingerprints-dump.tsv` / `p0c55-stage-leg-{census,decisions}.tsv`；'
    '探针源码归档 `P0c55StageLegProbeTest.java.txt` / `P0c55AuditProbeTest.java.txt`（树内 `.java`/`.class` 已删）。\n'
)

text = LEDGER.read_text(encoding='utf-8')

# 锚点一：旧「下一面」行（单行）整行替换。
lines = text.split('\n')
hits = [i for i, ln in enumerate(lines) if ln.startswith('- **下一面（P0c-47 收口后已侦察，续片 31 候选）：阶段页 owner 扩散')]
if len(hits) != 1:
    sys.exit('FAIL-CLOSED: 旧下一面锚点命中 %d 次（期望 1）' % len(hits))
lines[hits[0]] = NEXT_FACE.rstrip('\n')
text = '\n'.join(lines)

# 锚点二：当前切片段首（最新在顶部）。
anchor = '- 已完成：**P0c-54（SimpleTalk 链车道 / 续片 31）阶段页轴：入口页补行（39003/49003）'
if text.count(anchor) != 1:
    sys.exit('FAIL-CLOSED: 当前切片锚点命中 %d 次（期望 1）' % text.count(anchor))
if '**P0c-55（SimpleTalk 链车道 / 续片 32）阶段腿逐阶段重建' in text:
    sys.exit('FAIL-CLOSED: P0c-55 切片条目已存在（幂等保护）')
text = text.replace(anchor, SLICE + anchor, 1)

LEDGER.write_text(text, encoding='utf-8')
print('LEDGER_UPDATE_OK bytes=%d lines=%d' % (len(text.encode('utf-8')), len(text.split('\n'))))
