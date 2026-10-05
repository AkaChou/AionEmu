# 任务目录（QUEST_CATALOG.zh-CN.md）重建记录（2026-10-05）

## 背景

`docs/QUEST_CATALOG.zh-CN.md`（全任务目录：id / 中英文名 / 等级 / 限制 / 前置 / 接取方式 / 定义文件）
原先长期维护，2026-09-30 随第三轮 docs 生成物清理（`1aa8343c8`）被删除——理由记录在
`.agents/summary/CLEANUP-LEDGER.zh-CN.md` §5：生成式产物、1,284 条链接指向已重命名目录、生成链
（4 个生成/刷写脚本）同批失效。**删除时的盲点：引用扫描未覆盖仓外消费方**——
`AionEmu-QuestWiki/scripts/generate-questwiki.ts`、`sources.ts` 仍按
`<仓库根>/docs/QUEST_CATALOG.zh-CN.md` 读取该目录（QuestWiki manifest 记录其 sha256），
文档删除后 QuestWiki 生成链断档至今。

## 重建

生成器：`build_quest_catalog.py`（本目录）。运行：`python3 .agents/summary/quest-catalog-rebuild/build_quest_catalog.py`。

| 列 | 来源 |
|---|---|
| 任务 ID / 名称 / 限制 / 前置 / 接取方式 | QuestWiki 索引（`<工作区>/AionEmu-QuestWiki/public/data/quests.index.json`；迁移前目录 + 退役定义 XML + Aion 5.8 客户端字符串的派生） |
| 接取等级 | 真端表 `minlevel_permitted`（与索引全量交叉核对 6222/6222 一致） |
| 上限等级 | 真端表 `maxlevel_permitted`（0 = 无上限；998/999 哨兵按原值输出） |
| 道具物品 id | 真端表物品列经 `items/item/*.xml` 的 `name_desc → id` 解析（两通道：原名 → 去 `ITEM_` 前缀）；`%` 前缀随机组经 `<真端根>/Map/XML/quest_random_rewards.xml`（UTF-16，优先）+ 仓库内 legacy 副本解析；工作/检查/收集/掉落/背包/奖励/可选奖励/职业物品/职业奖励/随机组/未解 分组输出 |
| 定义文件 | 现行 `definitions/quests/<id>.xml` 直链；真端表驱动的任务标「已退役」并链历史路径 |
| 目标（击杀/采集/收集/交互） | 真端优先：击杀 = 仓库内零售族表副本 `quest/retail/Quest_SimpleHunt.xml`（monsterN+countN 组）/`Quest_SimpleSerialHunt.xml`/`data_driven_quest.xml` Hunt 进度，合并零售 `quest.xml` 的 `drop_monster_*`；真端无声明时回退客户端契约 `definitions/quest_monster/quest_monster.csv` 击杀类行。采集 = 客户端契约 gatherSource（经真端 `<真端根>/Map/XML/Objects.xml` harvest_source 表解析对象 id，与仓库 `gatherables/gatherable_templates.xml` 761 条 id 同空间：真端 400001 gb_source_vegetable_10a ↔ 仓库 400001 Kukuru，对拍一致）。收集 = SimpleCollectItem.objectN + data_driven CollectItem。交互 = SimpleTalk/SimpleItemPlay/SerialHunt 的 talk_npc*（中继 NPC）+ data_driven Talk/TalkFOBJ/EnterArea + 客户端契约 goodsList/itemUseArea。名字→npc_id = `npcs/npc_template_*.xml` 的 `name ∪ name_desc`（与 `NativeNpcNameResolver` 同规则，一名多 id 全列）；每分类最多 8 个名，超出 `…+k` |

当前产出：6222 行；现行 XML 链接 731；真端表驱动 5491；有物品声明 5745；未解物品符号 0；
有目标声明 3825（击杀 3147 / 采集 149 / 收集 545 / 交互 759）；未解目标符号 319。

## 目标列的两个口径

- **真端 vs 客户端优先级**：击杀/收集/交互的族表数据全部来自仓库内零售族表副本（与
  `<真端根>/Map/XML` 原件逐表行数对拍一致：SimpleCollectItem 262/262、data_driven 2527/2527）。
  客户端 `quest_monster.csv` 只在真端无声明时回退——它是客户端任务跟踪契约（含 `_t_`/副本变体），
  不是权威目标表；其 `num` 列与真端组计数不同源，故回退行不输出计数。
- **`?名字` 残差**：未解目标符号 319 处（181 个唯一名），主体是 `Ab1_1011_Boss_v58_Dr_*`、
  `Ab1_1131_Boss_Dr_Q1737` 一类真端刷怪名——不在客户端 npc 模板中（即
  `p1-prereqs/name-resolution-decision.md` 记录的「本服静态数据构建范围外的高地区域刷怪/赏金 NPC」
  残差类）；181 个唯一名与真端放置表导出 `<真端根>/Map/XML/npcs_npcs.xml`（24955 条）逐一交叉核对
  **0 命中**，按 `?名字` 显式输出（前缀分布 Ab1×66、DF6/LF6×16、LDF5×13、IDSeal×12…，另有 5 个
  `usearea_*` 使用区对象各表均无）。
- 自行校验锚点：1131 交互 = `Shugo_LF1a_01(799093)`——与
  `quest-acceptance/1131-2026-10-05-client-accepted.md` 的中继 NPC 799093 一致；5000 合成任务
  NPC（Anteros/Auminus=203788/830062）只出现在「接取方式」列，不重复进目标列。

## QuestWiki 兼容性约束

`scripts/sources.ts` 的 `loadCatalog` 解析规则：行首 `| <id> |`、第 7 格（`cells[6]`）必须含
markdown 链接、`cells.length >= 8`。因此**定义文件列必须保持第 7 格**，新增列只能追加在其后
（上限等级、道具物品 id 即按此追加）。

## 待办

- `docs/` 为 ignore 目录，入库需 `git add -f docs/QUEST_CATALOG.zh-CN.md`（历史上该文档是被跟踪的）。
- 建议把「仓外消费方清单」（当前已知：QuestWiki 读 `docs/QUEST_CATALOG.zh-CN.md`）补进
  CLEANUP-LEDGER，作为 docs/agents 清理前的检查项。
