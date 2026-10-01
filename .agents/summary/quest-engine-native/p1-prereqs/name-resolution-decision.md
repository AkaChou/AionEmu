# P1 前置决策备忘录：native 名字解析器（NativeNpcNameResolver）

> 2026-10-01 · 只读研究产物（`tools/name_resolution_feasibility.py` + `name-resolution-coverage.tsv`）。
> 本文把 P0a 的"名字解析轴"从开放问题收敛为一个**小而明确的决策**，供用户裁决后才进 P1。

## 1. 已解决的主体（无需决策）

真端任务表引用的 NPC/怪名 = 本服 npcTemplates 的 **`name_desc` 属性**（真端式全名；`name` 属性是短名）。
**按 `name ∪ name_desc` 精确解析（零发明规则、零别名表）即覆盖：**

| 家族 | READY/行数 | 残差（缺失+歧义） |
|---|---|---|
| SimpleSerialHunt | 16/16 | 0 |
| CombineTask | 574/574 | 0 |
| SimpleHunt | 1691/1863 | **172**（150 缺失 + 22 歧义）|
| SimpleTalk | 2939/3152 | **213**（100 + 113）|
| SimpleUseItem | 156/160 | 9 |
| SimpleCollectItem | 238/262 | 24 |
| SimpleItemPlay | 38/43 | 5 |
| DataDriven | 1895/2492 | 572（+337 客户端缺失另列）|

规范化类桥接（子串/词元超集）实测覆盖率 ≤3% 且多为 `test_` 前缀变体 ⇒ **否决**；不建别名台账（D.3-13）。

## 2. 残差全貌（决策对象）

| 桶 | 规模 | 构成 |
|---|---|---|
| 名字完全不存在 | 146 个唯一名 / 623 引用 | LF4/DF4/DF6/ldf5a/ldf5b 高地区域刷怪、赏金 NPC（本服静态数据构建范围外）|
| 跨模板同名歧义 | 57 个唯一名 / 246 引用 | 同名命中多个 npc_id，需消歧依据 |
| DD 客户端缺失 | 337 行 | id 不在客户端 quest.xml（`_challengetask_`/活动/内部行）|

## 3. 供裁决的方案

**方案 A（推荐）：残差行 fail-closed 冻结 + 家族级"冻结行清单"作为切换批的一部分**
- resolver 对残差名抛 `NATIVE_NAME_UNRESOLVED`；这些表行在启动期注册为 **DISABLED(UNRESOLVED_IDENTITY)** 并计入启动报告（显式、可数、无兜底）。
- 计划不变式 2 需要一条**用户批准的修订**：家族切换允许携带"显式冻结行清单"（清单内行 = 玩家不可达或本服无对应刷怪，保留于旧车道或禁用），清单随 P0a owner-identity 工具重算、逐批收敛、终态随台账删除。
- 优点：不阻塞 88% 已闭环行的迁移；残差显式可见。缺点：需改计划不变式 2 的表述。

**方案 B：先补数据再切换（不修订计划）**
- 残差多数是本服静态数据缺失的 65+ 区域刷怪——若用户能提供真端 NPC 模板表（`sql.rar` 内游戏 DB dump；本机无 unrar/7z，需你解压或另给数据），resolver 增加一条"真端模板表 name→id 绑定"后残差大概率大幅归零，且 id 宇宙与真端同源。
- 优点：不违反不变式 2、身份轴更强（真端权威数据）。缺点：迁移被数据获取阻塞；337 个 DD 客户端缺失行此路也不通（客户端侧无正文）。

**方案 C：A+B 组合（推荐顺序）**
- 你先提供真端模板表 → 重算残差 → 剩余量小到可逐行核阅后再批准方案 A 的冻结清单。

## 4. 决策点清单（请逐项裁决）

1. 残差处置：**A / B / C** 三选一（影响 P1 能否开工及家族切换的表述）。
2. 若选 A 或 C：批准"显式冻结行清单"作为不变式 2 的修订表述（清单 = 迁移期临时工件，随批收敛，不进 `src/main` 运行时）。
3. DD 337 客户端缺失行：维持 `IDENTITY_EVIDENCE_REQUIRED` 冻结（P7 前逐行核），还是现在就按"内部/活动行"整类冻结？
4. P1 批次开工授权：SimpleHunt 批文件清单与 focused test 清册将在决策后声明。

## 5. 追记（2026-10-01 续）：方案 B 数据源探测结果

- `sql.rar` 可用 bsdtar 解包，内容为 4 个 MSSQL `.bak`（Aion_gm 317MB / AionWorldLive 59MB / AionAccountDB / CacheDB 日志）。
- 二进制探针：`bountyhunter`/`soglo`/`utisda`/`NpcTemplate` 等在 Aion_gm 与 AionWorldLive 中 **零命中**（UTF-16LE 与 ASCII 双试）——真端 NPC 模板表不在这些备份里（可能在 CacheD 私有数据文件或未随包）。**方案 B 数据源暂不可得，需用户提供真端 NPC 模板表位置。**
- 残差处置在获得数据前按方案 A 的表述推进（冻结行清单），若后续拿到真端模板表则按 C 收敛。
