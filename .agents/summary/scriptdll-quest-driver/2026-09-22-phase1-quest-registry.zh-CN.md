# Phase 1：ScriptDLL64 任务注册表提取（真端驱动恢复第一步）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-22；性质：只读分析（未改任何生产代码），产物在本目录。
- 二进制：`/Users/mc/IdeaProjects/58Server/MainServer/ScriptDLL64.dll`（73.4 MB，与 NPCServer 副本同尺寸）
- 反编译产物：`/Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c`
  - 行数 2,719,399；函数 158,519（`status.txt: Exported 158519, failed 0`）
  - dump SHA256（脚本自动写入 TSV 头）：`dbcd96cc6a204912bf53be64021846c4ce18835afb2d44a8cc42750e2615441c`
- 工具：`extract_quest_registry.py`、`report_quest_registry_coverage.py`、`sample_topology_vs_xml.py`

## 1. 结论

真端任务脚本的**注册表可以机械提取**。每个任务在 DLL 里注册若干"步骤"，每个注册点把
`任务ID + 步骤参数(+ 描述符指针)` 交给一个注册辅助函数；提取这些调用点即得到
「任务 → 步骤序列（拓扑）」。这层不是手写逻辑，因此**不需要读懂 15.8 万个函数**。

| 指标 | 数值 |
|---|---:|
| 语句级、首实参为字面量任务 ID 的调用点 | 20,537 |
| 识别出的注册辅助函数（≥20 个不同任务 ID） | 22 |
| 提取到的注册行 | **20,380**（占调用点 99.2%） |
| 覆盖任务 ID | **6,814**（真端 10,035 的 67.9%；本仓库 6,224 的 72.9%） |

## 2. 按族覆盖（模板表 ←→ 注册表）

| 族 | 真端表行 | 注册表命中 |
|---|---:|---:|
| SimpleHunt | 1865 | 1863（100%） |
| SimpleTalk | 3152 | 3151（100%） |
| SimpleCollectItem | 262 | 262（100%） |
| SimpleUseItem | 160 | 160（100%） |
| SimpleItemPlay | 43 | 43（100%） |
| SimpleSerialHunt | 16 | 16（100%） |
| CombineTask | 574 | 574（100%） |
| **DataDriven** | **2510** | **0** |
| 无模板行（脚本型） | 1810 | 745 |

- **DataDriven 族没有 per-id 注册点** → 由通用 driver + `QuestProgressExtraInfo_*`（Hunt/Talk/CollectItem/EnterArea/EnterWorld/LevelUp/PvP/ItemPlay/TalkFOBJ）步骤处理器驱动；恢复这约 10 个类即可覆盖 2,510 个真端任务（本仓库 1,508 个）。
- 9 个注册辅助函数映射不到任何模板表（`FUN_180caf350/180caf6c0/180caf640/180caf3c0/180caf740/180caf5f0/180caa850/180cab150/180caa1b0/180cacbf0`）→ 这些就是**per-quest 脚本**的注册入口；本仓库 670 个无模板任务里 **494 个**有可用入口。

## 3. 与现有 XML 抽样对账（`sample_topology_vs_xml.txt`）

| 任务 | 族 | 注册步骤数 | 现有 XML 结构 |
|---|---:|---:|---|
| 1102 | SimpleHunt | 3（hunt 参数含 `3`，与表 `count1=3` 一致） | node=7, dialog=2, counter-grid=1 |
| 1517 | SimpleHunt | 4 | node=38, transition=3, dialog=8, counter-grid=1 |
| 2641 | SimpleTalk | 5（3 个 talk 步骤带 4 个描述符指针） | node=7, transition=9, dialog=17 |
| 21296 | SimpleTalk | 4 | node=4, transition=18, dialog=34 |
| 5000 | CombineTask | 3 | node=4, transition=7, dialog=12 |
| 10501 | DataDriven | 0（预期：无 per-id 注册） | node=10, transition=27, dialog=36 |
| 15551/15552/15563/16824 | SEEN_MARKER/SimpleTalk 行 | **0** | 有完整 XML |

结论：表驱动族可提取到步骤骨架；**步骤顺序 = 调用点偏移序**目前仍是假设（Phase 2 用客户端 HTML 与现有 XML 验证）。

## 4. 本轮新增的两个事实

1. **描述符参数存在**：注册调用除字面量外还传 `&DAT_xxx` 指针（如 2641 每个 talk 步骤 4 个指针）。
   其中 CombineTask 指向文件内静态块（首字段像 vtable 指针），Simple* 族指向 `.data` 的零填充区
   → 会在加载期由 `QuestDB_Load`（Server64，已重命名）从模板表填充。**即：per-quest 参数来自表，行为来自类**。
2. **SEEN_MARKER 一类任务没有注册点**（15551..15554、16824 等，`category=SEEN_MARKER`）
   → 它们的驱动是另一条路径（客户端标记/通用路由），Phase 1.1 需要单独定位。

## 5. 边界与未完成

- 提取覆盖 99.2% 的调用点；剩余为参数非字面量 ID、或 ID 在非首参位置的形式。
- 语义未验证：helper 名、步骤参数含义、顺序假设都需要 Phase 2（读 helper 体 + 与真端表/客户端 HTML/现有 XML 三方对齐）。
- 未做：Maven 测试（本目录仅分析脚本）；未修改本仓库任何生产文件。

## 6. 下一步（Phase 2 计划）

1. 取 3~5 个 helper（`FUN_180cb13b0` SimpleHunt、`FUN_180cab520/180cabb10` SimpleTalk、`FUN_180caac10` CombineTask、`FUN_180caf7c0/180cafa40`）读反编译体，确认真实语义与步骤顺序。
2. 用 RTTI 仍存的 `Simple*Quest` / `QuestProgressExtraInfo_*` 类 vftable 槽位对齐接口方法，建立"步骤种类 → 行为"字典。
3. DataDriven 族：定位通用 driver 如何按 `category_progress_` 取参（2510 个任务的性价比最高）。
4. 以 1102/1517/2641/21296/5000 为样板，产出「注册表 ↔ 现有 XML」逐步骤差异清单，作为生成器或 driver 的验收基线。
