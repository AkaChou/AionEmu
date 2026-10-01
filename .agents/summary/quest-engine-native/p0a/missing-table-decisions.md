# P0a §4.4.6：缺失表参与范围决策

> P0a 只读审计产物 · 2026-10-01。证据 = 真端加载点（server58-source 反编译）+ 表行数/对拍（`table-source-inventory.tsv`）。
> 本文件是决策建议；入仓动作本身属 P0b，须单独授权。

## 决策总表

| 表 | 真端行数 | 真端加载点（Server64/MainServer） | 本服现状 | 决策 | 说明 |
|---|---:|---|---|---|---|
| `HtmlPages.xml` | 5904 | `fun_262.cpp:3424` + ScriptDLL `fun_913.cpp:2675` | 未入仓 | **P0b 必须入仓** | 页注册表，native 车道运行时必需；UTF-16/DTD 感知 loader |
| `challenge_task.xml` | 123 | `CChallengeTaskDB.cpp:136`（MainServer）+ NPCServer/Cached/ICServer | `legacy/challenge_tasks.xml`（行元素改名 `task`，123 id 全对齐） | **P0b 重新入仓带来源 hash** | id 全对齐；命名差异登记为转换规则 |
| `quest_random_rewards.xml` | 817 | 真端在用 | `legacy/` 副本 **只有 329/817，且 329 行内容全部漂移** | **P0b 重新入仓** | 入仓副本陈旧（488 个源 id 缺失，多为 10001+ 新任务）；现副本不得作为运行时权威 |
| `npcfactions_quest.xml` | 436 | `NpcFactionDB.cpp`（MainServer） | 未入仓（emu 有自建 npc_factions 静态数据） | **P7 前入仓** | DD/阵营接取轴；行键 `quest_id`，436 id |
| `item_quest.xml` | 5674 item 行 | **未发现任何服务端二进制加载**（MainServer/ScriptDLL/NPCServer/Cached 全 grep 零命中） | 未入仓 | **登记不参与** | 疑为客户端/WebCmd 侧表；若后续证据推翻再立案 |
| `jumping_addquest.xml` | 6 | `JumpingCharacterMgr.cpp:1566` | 未入仓 | **暂缓**（feature 级决策） | 属"跃升角色"功能：本服未实现该 feature 则整组不参与 |
| `jumping_endquest.xml` | 70 | `JumpingCharacterMgr.cpp:1975` | 未入仓 | **暂缓**（同上） | 同上 |
| `jumping_item/pc/title.xml` | — | `JumpingCharacterMgr.cpp:1154` 引用 `jumping_item.xml` | 真端数据目录亦无此文件 | **不参与** | 快照数据不全，与本任务无关 |
| `Quest_SimpleGather.xml` | 0（空容器） | 家族表路径模板加载 | 未入仓 | **不参与** | 0 行，无语义；loader 需容忍家族表缺行不缺容器 |

## 量化要点

1. **`quest_random_rewards.xml` 是唯一发现"入仓副本实质陈旧"的表**：329 行 id 交集内全部内容漂移 + 488 id 整行缺失（src_only 样本 10001/10002/10006…）。P0b 必须以 `<真端根>/Map/XML/quest_random_rewards.xml`（sha256 `a8972172…`）重新入仓。
2. `challenge_task.xml` 源路径：`<真端根>/Map/XML/challenge_task.xml`（sha256 `afbd0f50…`，218,118 字节）。
3. 已入仓 8 张真端表全部与源 token 语义等价（`table-semantic-diff.tsv`：7 张 SEMANTIC_EQUAL，SimpleHunt 17 行为 dev_name/逗号空白规范化差异，token 化后零漂移）。byte 级 hash 全部 DIFF（编码/换行），来源清单须记录规范化转换规则：UTF-16→UTF-8、DOCTYPE 内部子集剥离、`,␠\n`→`,\n` 列表空白规范化。
4. DD 行数口径：按根直接子元素 `quest_data_driven` 计 **2492**（计划旧值 2526 的口径未复现，P0a 以本清单为准）。

## 对 §2.5 计划数字的修订

| 表 | 计划旧值 | P0a 实测 | 备注 |
|---|---:|---:|---|
| data_driven_quest.xml | 2526 | 2492 | 口径=根直接子元素 |
| Quest_SimpleHunt.xml | 1865 | 1863 | 同上 |
| item_quest.xml | 5682 | 5674 | `item` 子元素 |
| 其余 | — | 一致 | quest.xml 10035 / Talk 3152 / Combine 574 / Collect 262 / UseItem 160 / ItemPlay 43 / Serial 16 / HtmlPages 5904 / npcfactions 436 / challenge 123 / random_rewards 817 |
