#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-55 收口（续 3）：记忆库判例 T3 读数 + 项目记忆 T2/T3 读数 + 台账家族进度段。
P0c-55 closure (part 3): QE-080 validation T3 reading + project-memory T2/T3 readings + ledger family-progress clause."""
import pathlib
import sys

HERE = pathlib.Path(__file__).parent
CARD = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test/.agents/memory-bank/patterns/quest-engine.md')
PROJ = pathlib.Path('/Users/mc/zcode/cli/memories/projects/aionemu-test-8ba3ed28861d4d74/memory/retail-quest-driver-project.md')
LEDGER = HERE / 'GOAL-retail-driver-progress.zh-CN.md'

# 1) QE-080 validation 追加 T3 读数
card = CARD.read_text(encoding='utf-8')
card_anchor = '净树 20/20、T1 1F（在册 lane 20035）、T2 4F 全既存\n'
card_add = ('；T3 2013 例 117F/22E/1S（`gates/T3-221828.log`）对装入前两基线 `T3-212101`/`T3-213222` 的失败身份集**逐条相同**'
            '（各 139 条失败行 / 107 唯一身份，`only_in_base = []`、`only_in_post = []`）⇒ 零新增零消失\n')
if card.count(card_anchor) != 1:
    sys.exit('FAIL-CLOSED: QE-080 validation 锚点命中 %d 次（期望 1）' % card.count(card_anchor))
if 'T3 2013 例 117F/22E/1S' in card:
    sys.exit('FAIL-CLOSED: QE-080 已含 T3 读数（幂等保护）')
card = card.replace(card_anchor, card_anchor.rstrip('\n') + card_add, 1)
CARD.write_text(card, encoding='utf-8')
print('CARD_UPDATE_OK lines=%d' % len(card.split('\n')))

# 2) 项目记忆：落地行追加 T2/T3
proj = PROJ.read_text(encoding='utf-8')
proj_anchor = '净树 **20/20**；T1 **75 例 1F**（在册 lane 20035）；\n'
proj_add = ('T2（9 id，`gates/T2-221103.log`）**93 例 4F**（四条身份全在装入前 T3 基线身份集内）；**T3 2013 例 117F/22E/1S**'
            '（`gates/T3-221828.log`）对装入前两基线 `T3-212101`/`T3-213222` 失败身份集**逐条相同（ADDED 0 / REMOVED 0）**⇒ 零新增零消失；'
            '诚实口径：同窗口并发 DataDriven lane 亦在改树（其自身 T3 = `T3-221706.log`，2015 例，不属本片）⇒ T3 覆盖合并树，本片自因面锐化证据 = 改动面仅 2 文件 + `CHANGED` 恰 9 id + 9 id 全量失败条目零命中；\n')
if proj.count(proj_anchor) != 1:
    sys.exit('FAIL-CLOSED: 项目记忆落地锚点命中 %d 次（期望 1）' % proj.count(proj_anchor))
if 'T3 2013 例 117F/22E/1S' in proj:
    sys.exit('FAIL-CLOSED: 项目记忆已含 T3 读数（幂等保护）')
proj = proj.replace(proj_anchor, proj_anchor.rstrip('\n') + proj_add, 1)
PROJ.write_text(proj, encoding='utf-8')
print('PROJ_UPDATE_OK lines=%d' % len(proj.split('\n')))

# 3) 台账家族进度：SimpleTalk 行追加阶段轴收口段
ledger = LEDGER.read_text(encoding='utf-8')
fam_anchor = '`quest_client_reward_npcs.tsv`（P0c-2 扩展） |\n'
fam_add = ('；**P0c-54 + P0c-55 阶段轴（SimpleTalk 链车道）**：阶段页跨 NPC 扩散剪除 26 条 / 18 任务 + 39003/49003 补入口页'
           '（登记表 `43e37883…`）；9 个**塌缩任务**（1323/1394/1484/2480/2538/3093/4052/11010/11103）的阶段腿 `SETPRO{k}` / 阶段页 '
           '`SELECT{k+1}` 链 / 领奖页按真端 `talk_npc<k>` + 客户端任务书行（K+1）逐阶段复原（登记表 **`5bf47fde…`**，5052 行；判例 QE-080）'
           '——审计 9 任务未达 **34 → 0**、全表 **1348 → 1314**（`ONLY_POST = 0`）、冻结指纹 **`aaf2b9c0…`**；'
           '**族内 retired / `XML_RETENTION` 计数不变**（本轴只修登记表形态与下发路由，不涉退役与归属翻转） |\n')
if ledger.count(fam_anchor) != 1:
    sys.exit('FAIL-CLOSED: 家族进度 SimpleTalk 行锚点命中 %d 次（期望 1）' % ledger.count(fam_anchor))
if '9 个**塌缩任务**' in ledger:
    sys.exit('FAIL-CLOSED: 家族进度已含 P0c-55 段（幂等保护）')
ledger = ledger.replace(fam_anchor, fam_add, 1)
LEDGER.write_text(ledger, encoding='utf-8')
print('FAMILY_UPDATE_OK lines=%d' % len(ledger.split('\n')))
