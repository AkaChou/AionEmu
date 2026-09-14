# 注释质量修复总结（2026-09-09）

范围：`../../src/main/java` 全部 Java 源文件，**仅修改注释，未触碰任何代码**。
基准：`../rules/i18n.md`（注释须中英双语同义）、`../../docs/aion-game-terms-en-zh.md`（术语对照）。

## 修复类别与数量（约 90 处，跨约 34 个文件）

### 1. 空壳 / 装饰性注释（约 35 处）
- 删除空 Javadoc：`Polygon2D`、`cmd_shop`（serialVersionUID 上空注释块）。
- 删除纯装饰行：`ThreadPoolManager` 5 处 `// ===…`、`CradleOfEternity` 的 `//****//`。
- 区域横幅规范化：`Influence`（14）、`SM_FORTRESS_STATUS`（6）、`SM_INFLUENCE_RATIO`（2）中
  `// ======[欧比斯]… / ======[ABYSS]…` → `// 欧比斯 / ABYSS` 简洁双语。
- `LinkgateFoundryInstance` 头部畸形块 `/****/ /** Author (Encom) /****/` → 正规类级双语 Javadoc。

### 2. 注释内嵌残留内容 / XML 残留（约 13 处）
- `NightmareCircusInstance`：`/*Nightmare Lord Heiramune. <Nightmare Lord>*/` 两处 → `/* case 233161：… / … */`。
- `DrakenspireDepthsInstance`：英文残句 + `<Beritra>` 残留整块重写为规范双语。
- `AbyssRank`：8 处 `The daily <Abyss Point> count` 去除尖括号。
- `Equipment`：7 处残缺 Javadoc（`@return 已装备物品列表 / List<Item>`、非法 `@usage` 标签）重写为带方法职责说明的双语 Javadoc。

### 3. 单语 / 重复 / 残缺注释（约 40 处）
- `Base64`：删除迁移时与新增双语注释重复堆叠的 3 处旧中文注释，行尾注释补英文。
- 纯中文注释补英文（约 30 处）：`RetailPatternAI2`、`NpcMoveController`、`NpcFactions`、
  `PlayerQuestStatePort`、`MathUtil`、`PathService`、`LimitedQuestDAO/Service`、`WalkerData`、
  `Skill`、`Properties`、`WalkManager`、`cmd_setexp`、`PlayerCommonData`、`MotionLoggingService`、
  `TallocsSummonAI2`、`CM_QUESTION_RESPONSE`、`ContaminatedUnderpathInstance`（4）、
  `SM_SYSTEM_MESSAGE`（俄语残留 2 处）、`ItemGroupsData`（“唯一采集物 / 唯一采集物”中英重复）。
- `PortalDialogAI2`：16 个传送点 case 注释补英文，与同文件已有命名保持术语一致
  （符文安息处 = Danuar Sanctuary、达努阿尔秘境 = Sealed Danuar Mysticarium、石矛地域 = Stonespear Reach、
  提亚马特要塞 = Tiamat Stronghold、龙主避难所 = Dragon Lord's Refuge）。

## 校验
- 单元级扫描（按注释单元判定双语完整性）剩余问题：0。
- 行级扫描剩余均为「中文行 + 相邻英文行」成对误报。
- 词法健全性检查（块注释闭合、引号配对）全部通过；`git diff --check` 无本次改动引入的空白错误。

## 后续建议 / 有意保留
- `LinkgateFoundry` 等文件的 `//\\//\\//***怪物组名***//` 阶段横幅（18 处）与 `AbyssService` 的
  `//// ***////` 地图分区标记（7 处）**已处理**：去除装饰字符，规范为标准双语注释
  （如 `// Thecynon Bruiser 阶段 / Thecynon Bruiser phase`、`// 天族领地 / Elyos territories`）。
- 全库约 **1.0 万+ 条行尾英文名称标签**（`spawn(...); //NPC Name.` 一类，散布 884 个历史文件）：
  属上游原始注释（多为 NPC/地名标识），大部分非本次迁移引入。全部双语化需可核对的
  NPC/地名/物品译名词典（术语表仅覆盖核心条目），盲目机器翻译会引入错误译名；
  建议作为独立专项，基于客户端字符串建立名称→中文映射后统一处理。

## 专项二：SM_SYSTEM_MESSAGE 机翻注释（已完成，2026-09-09）
该文件存在 1492 条「中英逐词交错」的历史机翻注释（典型 `你：can see again。`、
`你的掉落率 has increased because you used…`、`Yousummon%0Spirit.Cooldowntimebegins 当其为…`）。
已按 13 批全部重写中文侧：以英文侧为权威底本逐句翻译，保留 `%0`/`%num0`/`[%SkillName]` 等占位符，
移除误用的 `@param skillname`/`@return`/`@usage` 前缀；术语对齐（欧比斯点数、基纳、魔石/神石、
烙印之石与碎片、背包、结界石、方尖碑、导师等）。
最终审计仅剩 4 处为占位符误报（`[%subzone]`、`%num0sp`、`[/Recruit Mentor …]` 指令参数），属合法保留。
词法健全性检查通过，`git diff --check` 无新增空白错误。
工具与进度：`comment_i18n`（`sm_i18n_audit.py` 按行检测中文段残留英文、
`sm_i18n_apply.py` 按行替换注释中文段、`sm_progress.json`）。批量译文映射 JSON 存档于
`/tmp/comment_audit/batch_*_fix.json`。

## 专项三：行尾名称注释双语化（术语表驱动，2026-09-09）
以 `../../docs/aion-game-terms-en-zh.md`（10937 个唯一键）为词典，对全库 8983 条纯英文行尾注释做
规范化精确匹配：命中 **871 处**并改写为 `// 中文 / English`（79 个文件）；同一文件内重复出现、
带等级后缀（I–V）的技能译文 **427 处归一为通名**（如 `气魄 I`→`气魄`），避免多技能 ID case 误标单一级。
全库精确词法检查 PASS；`git diff --check` 无新增空白错误。
匹配率约 **9.7%**：术语表按"至少出现 3 次"过滤，且不含系统消息与内部对象；未命中的 8112 条构成：
内部 ID（`ID*` 前缀）约 284、含数字 463、其余约 7.3k 为自定义对象/实体名/非实体说明。
若要更高覆盖，需基于服务端模板 `name` 全量 + 客户端 STR 中文重建词典（需服务端-客户端 key 关联规则）。
工具：`comment_i18n/tail_coverage.py`、`comment_i18n/tail_bilingual_apply.py`、`comment_i18n/tail_special_progress.json`。
