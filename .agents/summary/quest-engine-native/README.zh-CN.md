# quest-engine-native 车道台账（README）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 真端表驱动引擎迁移车道。治理文档 = `2026-10-01-真端引擎迁移计划.zh-CN.md`（design-only，批次须逐批授权）。
> 本 README 是跨会话续跑入口：批次状态、产物索引、门态、下一步。更新纪律：每批收口同步本页。

## 一、批次状态（2026-10-01 收口时点）

| 批次 | 状态 | 产物目录 | 一句话 |
|---|---|---|---|
| P0a 只读审计 | ✅ 完成 | `p0a/` | 三语义矩阵 + 相机 2463 调用点 + owner-identity（name_desc 修正）+ raw vars/硬编码/缺表决策/启动量级；两处计划前提被推翻并归因 |
| P0b 数据基础 | ✅ 完成（8/8 绿） | `p0b/` | 4 张真端表 byte 级入仓 + 溯源清单 + `HtmlPagesRegistry` 启动装载 |
| P1 第一横切 | ✅ 完成（17/17 绿） | `p1/P1-TRANCHE1-REPORT` | RawQuestVarsCodec / ProgressCamera / CameraRegistry |
| P1 第二横切 | ✅ 完成（套件 41/41 绿） | `p1/P1-TRANCHE2-REPORT` | NativeNpcNameResolver（116,225 名）+ NativeQuestTableLoader（SimpleHunt 1863 行）+ 表↔脚本全量对拍 PASS（宽度规则实证、13912/23912 消解、休眠 51 行） |
| P1 第三横切 | ✅ 完成（套件 46/46 绿） | `p1/P1-TRANCHE3-REPORT` | NativeQuestXmlTable（quest.xml 10035 行数据层） |
| P1 切换批 | 📜 已声明未开工 | `p1/P1-SWITCH-BATCH-DECLARATION` | 四道门 G1..G4 见下；**§1.1 范围增补**（939 行内 4 组特殊形 110 行：talk 中继 21+1 / con_quest 85 / cutscene 3 / give_item 1） |
| P1 切换批（工作区落地） | 🟡 代码已落地、未提交 | `p1/P1-SWITCH-BATCH-DECLARATION` | `QuestEngine` 三入口原生分流 + 启动期 `installInterest`；`RetailQuestDriver` 切断 `compileSimpleHunt`（939 行移出旧 IR）；`SimpleHuntNativeFamilyGateTest` 3/3、`SimpleHuntHandlerTest` 2/2 绿；**同批删旧死码待与 QE-112 原子落地** |
| **P2 切换批（工作区落地）** | 🟡 代码已落地、未提交 | `p2-prereqs/serial-hunt-progression.md` | `NativeQuestTableLoader` 装载串行表 16 行 + `SimpleSerialHuntHandler`（严格串行/简报守卫位/满段翻 REWARD/页流 4-10-5-1008）+ QuestEngine 分流；`RetailQuestDriver` 切断 serial 注册；`SimpleSerialHuntNativeFamilyGateTest` 4/4、`NativeQuestTableLoaderTest` 7/7、启动与派发门禁全绿（tablelane+启动 61/61） |
| **红线基线（P2 收口实测）** | ❌ 324 红（门禁债） | `gates/2026-10-01-red-attribution.zh-CN.md` | 聚焦套件 1675 例 / 204F+120E / 118 类；归因=旧 IR 金标未重锚（非原生车道回归）；同批已重锚门禁全绿；`mvn clean` 复核排除缓存伪影 |
| P2 前置 | ✅ 证据齐（5/5 洞收口） | `p2-prereqs/serial-hunt-progression.md` §6-§7 | **codegen 生成脚本车道实证并收口全部分发问题**：16/16 serial id 有脚本条目；分发=生成函数直接注册为对象槽表槽 0x11（击杀）回调（23025 注册块全貌）；`FUN_180cb2450`=区间门相机 ⇒ count 真实强制、vars=跨阶段线性行进（23025/18911/30610/13918/23918 五任务实证）；event_quest.xml=活动任务子系（全域不存在且主线不依赖）；**§7 家族普查=codegen 全家族通用（P3-P6 前置证据）** |
| P7 前置 | ✅ 结构闭合 | `p7-prereqs/dd-dispatcher-and-handlers.md` + `dd-host-interface-detail.md` | DD 分发器 + 8 类 handler + 5 类接取全还原；IUserImp 133 槽两宿主全展开定名、expectedStep 落 hash 节点 +0x10/+0x14、tag 注册-分发全链路（含死代码区）；残缺=tag 9/0x13 零证据面不可还原（合法冻结态） |
| P3 步骤 1（表行模型） | ✅ 已落地（62/62 + 27/27 绿） | `p3/P3-STEP1-REPORT.zh-CN.md` | 真端 `Quest_SimpleTalk.xml` 3152 行进 `NativeQuestTableLoader`（`SimpleTalkRow` + 4 个访问面）；双 NPC 必填 fail-closed；长尾列按原文装载；**零切换、零触 QE-112 文件** |
| P3 前置 | ✅ 证据齐（表形状 + codegen 双报告） | `p3-prereqs/family-table-shapes.md` + `simple-talk-codegen.md` | 表侧：跨表相交门 PASS + item_check 63% 必建列；DLL 侧：**页 id 全是通用 select 页字面量（硬编码在共享分派器 cab520/cabb10，非每任务页）**，任务差异只在 thunk 立即数（questId/步进/con_quest/物品/movie）；页对拍 HtmlPages 全命中；0x37/0x38=活动任务子系槽；con_quest=链式接取窗双语义；残 6 洞均为运行期胶水（native 用自有事件总线替代，非阻塞） |
| **P3 步骤 2（切换批，工作区落地）** | 🟡 代码已落地（与步骤 3 同批提交） | `p3/P3-STEP2-REPORT.zh-CN.md` | `SimpleTalkHandler` 原生直驱 3152 行（cab520 接取 / cabb10 中继步进+按真端顺序 GiveItem·RemoveItem / 1009 报告门 / 领奖）；`NativeInventoryPort` 物品唯一出口；`RetailItemNameIndex.loadItemTemplates()` 两侧同源（旧车道去重迁移）；`SimpleTalkRow` 改真端分槽（`give_item` vs `give_item1..3`）；`QuestEngine` 三入口 + `installInterest`；`RetailQuestDriver` 切断 `retailOwnedTalk`（3152 行移出旧 IR）。门态：族门 7/7、tablelane 69/69、启动派发 27/27 |
| **P3 步骤 3（物品面 + 逐行门 + 双主收尾，工作区落地）** | 🟡 代码已落地、未提交 | `p3/P3-STEP3-REPORT.zh-CN.md` | ① 物品符号解析 = 真端两通道约定（原名 → 未命中再去 `ITEM_` 前缀重查）：表 give/remove 663 单元全 `ITEM_X` 形、quest.xml 交付列 3394 单元全原名形 ⇒ 3145 去重符号 0 冲突 0 未解，**白名单归零**；② 交付门判定由「全局集合增量」改**逐行**（修正 80669 fail-closed / 80670-72 放行的顺序相关缺陷）；③ 7 行门通道全缺 = 真端不可接取行（`client_level`/`minlevel_permitted=999`，证据 `Quest::CanAcquireQuest`），fail-closed + 逐行冻结；④ 系统发放（`_faction_` 54 行 + 阵营日常轮换资格直读 quest.xml 轴）/ cutscene（`NativeMoviePort`，67 行）/ 进世界旧存档自愈；⑤ `owns` 与 `routes` 分离（SimpleTalk 3152/3134、**SimpleHunt 939/936 双主收尾**、SimpleSerialHunt 同法泛化）且 `QuestEngine` 三入口改 `routes`。门态：族门 8/8、tablelane **70/70**、启动派发 + hunt 回归全绿 |
| **P3 步骤 4（同批删旧 + 旧金标重锚 + `con_quest` 接线）** | ✅ 已落地（与步骤 2/3 同批提交） | `p3/P3-STEP4-REPORT.zh-CN.md` | ① 同批删旧：`RetailSimpleTalkDefinitionCompiler` / `RetailSimpleTalkTable` / `RetailQuestDriver.compileSimpleTalk`（含字段/构造参数/族分支/`retailOwnedSimpleTalk`）/ `RetailSimpleTalkGateTest`（814 行）全部退场；`RetailSystemGrantDispatchTest` 的 talk 侧改读 native；② 旧金标按 §8.9 重锚（P3 新增 34 类全部归零；`SimpleTalkRowAlignmentGateTest` 独立逐行复算 3152 行）；③ `con_quest` 链式接取窗接线（真端 0x1e 槽 = 「下一环在本行交付 NPC 可接」；全表 492 行冻结 308/89/2/95；fail-closed 证据面 `unresolvedChainQuestIds()` 为空）；④ native 系统发放判定越权修复（`_challengetask_` 不得被 `grantSystemStart` 建档）。门态：tablelane 全绿、启动派发全绿、重锚 9 类 27 例全绿 |
| **P3 步骤 5（残余重锚，工作区落地）** | ✅ 已落地（未提交） | `p3/P3-STEP5-REPORT.zh-CN.md` | ① 10 个纯 talk 归属类按 §8.9 重锚真端表行 + 客户端页契约（含 1183/1483/1721/1724/2646/2692/2767/3966/3968/4501 十个真端行的 native 行锚与运行时走链；旧 IR 形状断言退场）；② 重锚暴露的三处实现缺口按真端修正：接取入口页由客户端任务页契约决定（`QuestDialogContract.acceptEntryPage` + 1012/1013 翻页原样回发）、职业轴 token 映射共享（`RetailQuestMetadataCompiler.permittedClassNames`）、`finished_quest_condN` 的 `:n` 奖励档后缀解析；③ 新增夹具 `NativeTalkFixture` + 证据导出器 `p3/tools/step5_anchor_evidence.py` + 50 行冻结 TSV。门态：A 组 10 类 58 例 / 2F+1E（全范围外）、聚焦套件 1688 例 / 110 类红（REMOVED 7 / ADDED 0） |
| **P3 步骤 5 门态（本次实测）** | ❌ 314 红（门禁债 110 类） | 门禁产物已清理（读数见本行） | 聚焦套件 1688 例 / 163F+151E / 110 类；对步骤 4 基线（1678 / 347 红 / 117 类）**REMOVED 7 / ADDED 0**；`missing production quest definition` 196 → 128；A 组红仅剩范围外三例（19673/80817 DD、1137 P4） |
| **跨族阻塞（步骤 5 新发现，待单批落地）** | ⏸ 取证完成 | `p3/P3-STEP5-REPORT.zh-CN.md` §5 | ① native 领奖/完成段对全部已切换族抛 NPE（已切换行无 typed 模板，`QuestService.finishQuest` 首段要求模板；族门零覆盖）——爆炸半径 SimpleHunt 939 + Serial 16 + Talk 3134；② `bm_restrict_category`（128 位地图位集）未坐实 ⇒ SimpleTalk 1383/3152 行不可接取 |
| **P3 未解析面冻结（步骤 3 归零）** | ✅ 白名单已归零 | `p3/simple-talk-unresolved-npcs.tsv` + `p3/simple-talk-unresolved-items.tsv` + `p3/P3-STEP3-REPORT.zh-CN.md` | NPC 39 名（接取列 207 行 / 交付列 86 行）仍为证据快照；**物品面 14 项白名单归零**——排查真端证实 14 项非静态数据缺失，而是沿用了老链路「无条件去 `ITEM_` 前缀」规则；1988 个 item_check 行 **1981 行门成立**，7 行门通道全缺=真端不可接取行 |
| **P3 步骤 4 门态（本次实测）** | ❌ 347 红（门禁债，117 类） | 门禁产物已清理（读数见本行） | 聚焦套件 1678 例 / 162F+185E / 117 类；对 P2 基线 **REMOVED 1（`Quest30314RetailAlignmentTest`）/ ADDED 0**；P3 新增 34 类全部归零；`missing production quest definition` 230 → 158（余下 158 属基线 117 类内的 talk 行迁移债）|
| P0a 重冻 | ⏸ 被 QE-112 门住 | — | QE-112 落地后重跑 `owner_identity.py` + `raw_vars_probe.py` |

**tablelane 生产组件现况**（全部纯新增、零切换、零触碰 QE-112 文件）：
`HtmlPagesRegistry` / `RawQuestVarsCodec` / `ProgressCamera` / `CameraRegistry` /
`NativeNpcNameResolver` / `NativeQuestTableLoader` / `NativeQuestOwnerResolver` / `NativeQuestXmlTable`。
门测试 8 类 46 例全绿。启动接线目前只有 `HtmlPagesRegistry.ensureLoaded()`（QuestEngine.load 一行）。

## 二、P1 切换批门态（声明书 §8）

| 门 | 内容 | 状态 |
|---|---|---|
| G1 | QE-112 落地（`RetailSimpleHuntDefinitionCompiler.java` 是其脏文件，切换批要同批删改） | ❌ 外部 |
| G2 | 冻结行裁决：默认 A（`p1/simplehunt-frozen-rows.tsv` 172 行 + 不变式 2 修订表述），待用户追认；B 数据源 sql.rar 已证无 | ⏳ 默认 A 待追认 |
| G3 | PAGE-FLOW SPIKE：`p1/pageflow-spike.md`（页 id 全有 HP 出处；8 项家族级冻结约定登记；RETAIL_MULTI_TIER 文档漂移登记） | ✅ 已清 |
| G4 | 用户对声明书授权 | ⏳ 外部 |

另：不变式 7（raw vars 存档）已闭环 CLEAN（`p1/simplehunt-rawvars-archive.md`：213 行存档 100% 可达值内，零迁移）。

## 三、关键判例与事实（跨批次复用）

- **npcTemplates 必须按 name ∪ name_desc 双属性索引**（P0a 修正判例）；836025 同分片定义两次；
  `name=" "` 占位 375 处不入键。
- **SimpleHunt 行 id 是行元素自身的 id 属性**（`<id id="N">`）；quest.xml 行 id 是 `<id>` 子元素；
  行内直接子元素实测全单值，重复字段全在嵌套容器。
- **宽度规则「任一 count>63 ⇒ 10 位」= 表自身判据**（对拍 1812 任务零失败）；表推导 fullValue 与
  脚本字面全等；13912/23912 无需特例。
- **80817 是 DD 任务**，不在 SimpleHunt 表（旧口径错置已纠）。
- **typed 车道已有完整事务/发布/同步机制**（QuestExecutionCoordinator + PlayerQuestStatePort +
  PlayerQuestStateSyncPort；`QuestState.setQuestVar(int)`=原始整字口；QuestVars 槽位 API 钳 63）⇒
  native 不新建持久化路径。
- **编译器规范形 helper 被 9 个他族编译器复用**；`RetailSimpleHuntPlan/Table`、
  `RetailHuntCounterLayout`、狩猎 TSV 被 P2/P7 家族消费 ⇒ P1 切换批只删网格专有部分（对计划 §7
  P1 行的修正已登记）。
- **串行狩猎（P2）codegen 实证（2026-10-01 深夜+次轮勘误）**：**16/16 serial id 有 codegen 生成脚本条目**
  （首轮"3/16 零命中"是人工 hex 换算伪影，被家族普查揭穿后修正——id→hex 必须程序化换算）；
  **分发收口**：生成函数直接经 `FUN_180cb2ac0`（fun_731.cpp:6214 槽表写入器）注册为对象槽表槽 0x11（击杀）
  回调，系统自含（名字字面量+静态节点）；`FUN_180cb2450` = 区间门 6 位相机变体（state==3 ∧ low≤vars<high ⇒
  槽+1；flag ∧ target==high ∧ vars==target ⇒ +0x100=SetQuestSuccess）——**count 列由生成脚本真实强制，
  serial vars = 跨阶段线性行进**（23025 count_first=9↔[0,9)、count_second=1↔fullValue 0x49=9+64×1；
  18911/30610/13918/23918 同型实证）；「对象相机 bits26-31 步指针」=event_quest.xml 活动任务子系
  （该表全域不存在且主线不依赖）。**家族普查（P2 报告 §7）：SimpleHunt/SimpleTalk/CollectItem/UseItem/
  ItemPlay/CombineTask 全部有 codegen 条目** ⇒ 计划 §2.3「离线生成每任务脚本」全家族成立，P3-P6 前置
  可复用「程序化 hex 抽样 → helper 分类」方法。
- **IUserImp 槽位定名（P7 宿主接口考古）**：+0xd0 GetQuestState / +0xd8 SetQuestAcquired /
  +0xf0 SetQuestProgress / +0xf8 SetQuestProgressMemoryOnly / +0x100 SetQuestSuccess /
  +0x118 ShareProgressMultiple；DD expectedStep 落 hash 节点 +0x10(key)/+0x14(value)；tag 语义
  10=CutScene、0xD=Movie、0x15=复活（确定），9/0x13 零证据面不可还原。
- **8 项家族级冻结约定**（pageflow-spike §8）：页 4 直跳、回页 10、确认区间 8..23 + 108/110+k
  双协议、档位→窗映射、class 优先级、select_none 集、简报位 slot6、满段杀刷新——全族共形，不阻切换。
- XML id 权威枚举 = `QuestDefinitionCatalogManifest`（742 行强校验）；owner resolver 的目录扫描是
  同值过渡，go-live 时切 manifest 源。
- owner 交叠 3 id：14112/16961（READY 但 XML 残留，切换批同批删）+ 14123（冻结行）。

## 四、待用户输入

1. G1：QE-112 落地（或裁决其基线）；
2. G2：追认 A 方案表述（172 行冻结清单 + 不变式 2 修订）；
3. G4：授权 P1 切换批按声明书执行；
4. DD-337 客户端缺失行处置、80817（DD）复刻 vs 显禁用（P7 前）；
5. 真端 NPC 模板表位置（若倾向 B/C；当前默认 A 不需要）；
6. 每族代表任务的真机客户端验收（PENDING_CLIENT 项）。

> 原 §四 第 7 项（30600/30610/23918 三任务推进裁决）已撤销：勘误重扫证明 16/16 全部走 codegen
> 线性行进，无待裁决项（P2 报告 §6.8）。

## 五、产物索引

- 计划：`2026-10-01-真端引擎迁移计划.zh-CN.md`；聚焦基线：`gates/2026-10-01-focused-baseline.md`
- P0a：`p0a/P0A-REPORT`（总）+ 三语义矩阵 + `camera-params.tsv`(2463) + `owner-identity.tsv`(9232)
  + `raw-vars-archive` + `hardcode-audit` + `missing-table-decisions` + `startup-scale-estimate`
  + `qe112-inflight-status` + `tools/`（6 只读工具）
- P0b：`p0b/P0B-REPORT` + `tools/ingest_tables.py`
- P1 前置：`p1-prereqs/name-resolution-decision.md`（含 sql.rar 探针 §5）+ coverage TSV
- P1：`p1/P1-SWITCH-BATCH-DECLARATION`（切换批唯一入口）+ 三横切报告 + `integration-surface.md`
  （运行时集成面事实）+ `pageflow-spike.md`（G3）+ `simplehunt-frozen-rows.tsv`(172) +
  `simplehunt-rawvars-archive.md` + `tools/`（对拍与 rawvars 工具）
- P2 前置：`p2-prereqs/serial-hunt-progression.md`（§6-§8 = codegen 实证 + 勘误 + 家族普查）
- P3 前置：`p3-prereqs/family-table-shapes.md`（P3-P6 表形状 + 跨表相交门）+
  `p3-prereqs/simple-talk-codegen.md`（talk 页流/槽位/phase 门/物品解码 + §8 设计输入）
- P7 前置：`p7-prereqs/dd-dispatcher-and-handlers.md` + `dd-host-interface-detail.md`（IUserImp 133 槽 /
  hash 节点 / 事件 tag 全链路）
