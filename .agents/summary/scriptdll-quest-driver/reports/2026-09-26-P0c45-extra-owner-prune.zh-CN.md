# P0c-45 多余交付 owner 剪除：35010/35011 删去无背书的 Priamos（799806）+ `npc_check` 标记口径修复


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 车道 / 续片：**SimpleTalk 链车道 / 续片 28**（编号消歧：并发 lane 同轮亦自标 **P0c-45 / 续片 19**（链式接取 none 12 行退役，产物 `p0c45_retire_chain_acquire_rows.py`）；两个 lane 共享 `.agents/summary/scriptdll-quest-driver/` 目录 ⇒ 唯一键 = **lane + 续片号**，本片产物一律含 `xml-only`/`extra-owner`/`35010`/`blast-radius`，与 lane 的 `retire_chain_acquire` 零文件名冲突）（编号消歧：本片唯一键 = lane + 续片号；产物文件名一律含 `p0c45`/`35010`/`xml-only`）
- 轴（台账登记）：`XML_ONLY_NPC_CENSUS_PENDING` —— QE-073 指出的"审计对 owner **身份**是盲区"，用
  `npc_check=XML_ONLY:*` 做全登记表普查（接取侧 / 物件侧 / 检查侧此前未查）
- 日期：2026-09-26
- 状态：**已完成并验证**（干跑 / 保真 / 指纹 / 审计 / 契约门 / 净树 / T1 / T2 / T3 全部落地，见 §8）

## 1. 结论（TL;DR）

全登记表 `npc_check=XML_ONLY:*` 共 **104 行 / 10 个任务**，按"标记判据的声明集是否展开"分解后：

| 类别 | 行数 | 任务 | 处置 |
|---|---|---|---|
| **标记口径假阳性**（客户端通道已声明，判据未展开复合条目） | **93** | 35024/35025/35026/45024/45025/45026 | 生成器标记域修复（改记 `CLIENT_MATCH`） |
| **生产面真面**（RETAIL_TABLE 驱动、owner 无任何背书） | **6** | **35010/35011** | **剪除多余 owner 799806**（本片主交付） |
| XML 保留面（IR 属 XML，标记只是转写证据） | 5 | 2653/28805 | 登记 `RETAINED_SUBSTITUTE_OWNER`，**无生产改动** |

生产面 6 行的裁定依据是**五源一致**（真端表 ×2 + 客户端任务书 dic + 客户端交付登记 + 客户端模板索引），
对立面只有退役 XML 自己的一条注释「向 Palas / Priamos **任一**报告」——手工形便利，无真端/客户端背书。
按用户判据 ③（逐任务裁定"真端对 / XML 错"）裁 **真端 + 客户端胜**，并落进生成器（新裁定表 + 九轴 fail-closed）。

## 2. 轴定位与分解（先分解、再动手）

**标记来源**：生成器在 XML 逐字转写时按 `npc_check = 'RETAIL_MATCH' if npc in census_names[qid] else 'XML_ONLY:%s' % npc`
标注。`census_names` 取自 `p0c10e-talk-chain-census.tsv` 的 `names` 列，该列有三种形态：
纯 id（真端名解析）、`SENTINEL:_faction_`（**类别哨兵**，不是 NPC）、`CLIENT:799800,799801`（**复合条目**：
客户端交付登记 / 模板索引解析出的多 id）。

旧口径把复合条目当**整串**比较 ⇒ `'799800' not in {'SENTINEL:_faction_','798946','CLIENT:799800,799801'}`
**恒真** ⇒ 45024 一个任务的 33 行、45026 的 27 行全被误标。这是 QE-072「量级异常先当假阳性」的**第二次兑现**。

分解后按两个正交轴再切：
① **owner**（保留清单）：`RETAIL_TABLE` = 登记表驱动生产 IR（缺陷候选）；`XML_RETENTION` = XML 仍是 owner（标记只是证据）。
② **投影判形**（QE-072 口径，不用标签/动作名字面）：`served(npc)` = 该 NPC 在块内的（动作集, 下发页集），
与"同任务全部声明 NPC 的并集"比较——`served ⊆ union` ⇒ `EXTRA_DUPLICATE`（多余，删掉不丢页）；
否则 ⇒ `SUBSTITUTE_OWNER`（替身，真干活的人在声明集外，删掉会丢页）。结果：

| quest | owner | 标记 NPC | 行数 | 判形 | 备注 |
|---|---|---|---|---|---|
| 35010 | RETAIL_TABLE | 799806 | 4 | **EXTRA_DUPLICATE** | 与声明 owner Palas(799805) 同动作同页 |
| 35011 | RETAIL_TABLE | 799806 | 2 | **EXTRA_DUPLICATE** | 同上 |
| 2653 | XML_RETENTION | 204655 | 3 | SUBSTITUTE_OWNER | `SETPRO1` 阶段推进在 204655；声明集 {204650,212314,204775} 无人服务该推进 |
| 28805 | XML_RETENTION | 730525 | 2 | SUBSTITUTE_OWNER | `SETPRO2`（s1→reward）在 730525；声明 `talk_npc2=Housing_FOBJ_Recycle_Box`=730522 |
| 35024/35025/35026/45024/45025/45026 | RETAIL_TABLE | 799800/799801/799842/799843 | 93 | MARKER_ARTIFACT | 客户端通道已声明（见 §2 首段） |

普查脚本 `p0c45_xml_only_npc_census.py`（v1→v4 判据沿革写在文件头，QE-072 要求），
pre/post 快照 `p0c45-xml-only-npc-census-{pre,post}.tsv`：
**pre = 16 个 flagged NPC 行（12 假阳性 + 2 生产面 + 2 保留面）；post = 2 行（只剩两个保留面）**。

## 3. 取证与裁定（判例 35010/35011：五源一致裁真端+客户端胜）

任务形状：`_faction_` = **类别哨兵**（阵营日常轮换，系统发放、无 NPC 接取），`talk_npc1` = 简报人
（Trou/Sofne），`reward_npc_name` = 交付人。遗留 XML 把交付窗铺给了 **Palas(799805) 与 Priamos(799806) 两人**。

| 源 | 内容（35010/35011） | 结论 |
|---|---|---|
| ① 真端表 `Quest_SimpleTalk.xml` | `reward_npc_name=Palas` → 唯一解析 799805 | 单一交付人 |
| ①′ 真端表同族（`Quest_SimpleCollectItem.xml`） | 35007/35008 `reward=Palas`；**35014/35015 `reward=Priamos`** | 系列内每任务**各自点名**交付人 |
| ② 客户端任务书正文 | `QUEST_Q35010.html` / `QUEST_Q35011.html` → `[%dic:STR_DIC_E_35007]`（=Palas）；`QUEST_Q35014.html` → `STR_DIC_E_35014`（=Priamos） | 任务书只点名 Palas |
| ③ 客户端交付登记 `quest_client_reward_npcs.tsv` | 35010/35011 **无行**；成对形有行：35021+→`799800,799801`、45024+→`799842,799843`、35014→`799806` | 无多 NPC 背书 |
| ④ 客户端模板索引 `legacy-quest-dialog-template-index.csv` | 35010/35011 `end_npc_ids=799805` **单值**；同 `template_type=report_to_many` 的 35024/35025/35026/45024+ 均**成对** | 单值是实测，非解析失败 |
| ⑤ census `DEVIATION` 列 | `DEVIATION:...@799806`（转写形自己承认该 NPC 在声明集外服务） | 佐证 |
| ✗ 对立面：退役 XML 注释 | 「第二步：向 Palas (799805) / Priamos (799806) **任一**报告」 | 手工形便利，无背书 → 裁 XML 错 |

**裁定**：剪除 35010/35011 里 799806 的全部记录（35010 4 条 R + 1 条 `B NPC_COMPLETE`；35011 2 条 R）。

## 4. 判例

**QE-074 标记判据的声明集必须分通道展开**：生成物里的"成员校验"列（`npc_check` 这类）在比较前必须把
**复合条目**（`CLIENT:a,b`）与**哨兵字面量**（`SENTINEL:x`）展开/剔除——整串比较会造 93/104 假阳性，
把 6 个任务误报成缺陷面。标记域同时升级为三值：`RETAIL_MATCH`（真端通道）/ `CLIENT_MATCH`（客户端通道）/
`XML_ONLY:<id>`（两者皆非），守卫用**两者并集**（声明证据 = 真端 ∪ 客户端）。

**QE-075 形状裁定的第三条通道 = 剪除（prune）**：三条通道的分工是——
**改写**（局部搬行，`apply_talk_ladder`）/ **改道**（整块弃转写按真端重合成，`canonical_resynthesis`）/
**剪除**（删去真端与客户端都不声明的多余 owner 记录，本片新增）。剪除的判据不是"看着多余"，而是两条守卫：
**覆盖守卫**（多余 owner 的（动作, 页）投影 ⊆ 声明 owner 的 ⇒ 删了不丢页）+ **越界守卫**（多余 owner 只许出现在
`<dialog>` 与 `<npc-complete>`，出现在别的元素即越界，**不得剪除**，须另行裁定）。非 EXTRA 形（替身）一律不剪。

## 5. 落地

**生成器 `build_quest_client_talk_chain_steps.py`（两处）**

1. **census 装载分通道**：`census_retail` / `census_client` / `census_names`（= 并集，供守卫用）；标记域改三值。
2. **剪除通道**：裁定表 `p0c45-extra-owner-decisions.tsv`（code `LEGACY_EXTRA_DELIVERY_OWNER`）+ **九轴 fail-closed**：
   ①code 域；②与阶梯/接取入口/改道三表**互斥**；③该行 owner 必须 `RETAIL_TABLE`（XML 保留行不得进表）；
   ④多余 NPC ∉ 声明集（真端 ∪ 客户端）；⑤census `DEVIATION` 记过该 NPC；⑥客户端交付登记未为它背书；
   ⑦声明 owner 名 == 真端 `reward_npc_name` 且唯一解析、≠ 多余 NPC；
   ⑧**覆盖守卫**（转写循环内、按 XML 投影计算）；⑨**越界守卫**。
   转写循环对多余 NPC 的 `R`/`B`/`I`/`C` 记录逐条跳过（并按 action 计数 => 记录数），块尾对剪除任务**重编 R 序号**
   （保持 1..N 连续，不留空洞）。

**工具口径升级**：`p0c43_dryrun_diff.py` 与 `p0c42_builder_fidelity_check.py` 的输入表清单从 8 张扩到 **19 张**
（= `grep "HERE / '*.tsv'"` 提取的生成器**全部**输入表）——"裁定表零漂移"的证明面覆盖全输入，不再只是抽查 8 张。

**裁定表**：`p0c45-extra-owner-decisions.tsv`（2 行，逐行带五源 basis；表头写明九轴与取证）。

## 6. 效果（修复面 = 缺陷面）

- **登记表**：5112 → **5105** 行（-7 条记录：35010 的 4R+1B、35011 的 2R），
  md5 `1a3386b26a84c34f975e1da7725dbc51`（双副本 src + target/classes 一致）。
- **爆炸半径**（`p0c45-blast-radius.txt`，pre = `p0c45-registry-pre.tsv`）：
  `BLOCKS 295 -> 295`、`CHANGED [35010, 35011, 35024, 35025, 35026, 45024, 45025, 45026]`、
  **`ADDED []` / `REMOVED []`**（无任务块增减）。
- **链式指纹**（`retail-simple-talk-chain-ir-fingerprints.tsv`，双副本 md5 `a6300d13bc90426b2eb2f09f0e308d95`）：
  `set-diff: added=[] removed=[] changed=['35010','35011']` ⇒ ①剪除的 IR 影响面 = 2 个任务；
  ②**6 个任务的标记改动（93 行）零 IR 影响**——这是"标记列不参与编译"的机器证明（不是声明，是实测）。
  重冻前链门**红**（`链式 IR 指纹偏离冻结值`）⇒ 指纹确为契约；外科重冻后绿。
- **审计**（`p0c45-35010-35011-audit-post.txt`，探针已归档删除）：35010 **13 行**、35011 **6 行**全部
  `PAGE_ACTION_MATCHED` / `TERMINAL_PAGE_REACHED`（IR 引用 `799806` = **0**）。
  唯一非致命未达 = 页 4（`SHOW_ASK_QUEST_ACCEPT_WINDOW`）：其发射集 = `35010 R2@204560` + `35011 B NPC_START@799805`，
  与剪除集**不相交**（逐行核对 blast radius，剪除集里该页发射记录 **0** 条）⇒ 该行前后同值，
  属 `_faction_` 系统发放接取形状（非致命，既有轴）。**边界**：为免在并发 lane 活跃期替换生产副本，
  本片**未做** classpath 交换取"剪除前"审计快照——该结论由"发射记录集合在两侧完全相同"的逐行核对给出。
- **契约门**：`QuestClientContractGateTest` 1/1 绿（fatal 0，空基线）。
- **净树复跑**：链门 2 + 审计 17 + 契约门 1 = **20/20 绿**。

## 7. 耐久性与可证伪性

- **生成器保真**：`p0c42_builder_fidelity_check.py`（重定向 `OUT` 重跑）→
  `FIDELITY_OK`（**429549 字节**，`REGISTRY_MD5 1a3386b26a84c34f975e1da7725dbc51`）+ **19 张输入表零漂移**。
- **干跑**：`DRYRUN_OK`（变化面 == 期望 8 个任务；`HEADERS 10 -> 10`）。
- **普查可证伪**：判据 v1（只数标记）→ v2（声明集展开）→ v3（投影判形）→ v4（owner 分层），
  每次修正理由留在 `p0c45_xml_only_npc_census.py` 文件头；**双向校准**：2482 的**修复前快照**里
  278018/278020 必须判 `LIVE_SUBSTITUTE_OWNER`、现状必须无标记（`CALIBRATION_OK`）。
- **剪除守卫可拦性**：九轴里 ①②③④⑤⑥⑦ 在装载期即 fail-closed；⑧⑨ 在转写期按 XML 投影计算。
  本片**只**授权"多余"形；替身形（2653/28805）与越界形一律拦下（未剪）。

## 8. 门禁与归因

| 档 | 结果 | 归因 |
|---|---|---|
| 净树 20 例 | **20/20 绿** | 链门 verify 2 + 审计 17 + 契约门 1 |
| T1（63 例） | **1F** | `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（lane 20035 码位：`RETAIL_TALK_HUNT_CHAIN_DEFERRED` → `RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED`）＝**在册 lane 项**；对 P0c-44 基线 `T1-162409`（63 例 3F：CollectItem 冻结 + DataDriven 冻结 + 20035）**减少 2 条**（两条 lane 项自愈），**零新增**。日志 `gates/T1-173432.log` |
| T2（79 例，ids 35010 35011 35024 35025 35026 45024 45025 45026 2653 28805） | **3F** | ① 20035（同上 lane）；② `ReportToManyDialogRouteRegressionTest.migratedReportNpcsOpenTheirPageFromStartDialog`（quest 3914/npc 203752/page 1352）；③ `LegacyTemplateMirrorRouteRegressionTest.legacyTemplateCloseControlsDoNotChangeQuestState`（quest 1131）——② ③ 在**本片改动前**的 T3 全树日志（`T3-170837` lane / `T3-163754` 本车道）里**同消息在册**（3914 属 SimpleUseItem 族、登记表内 0 行 ⇒ 本片不可达），= 慢性项。日志 `gates/T2-174150.log` |
| T3（全 `questEngine.**`） | **2011 例 116F + 21E + 1S** | 与 lane 最近全量 `T3-170837`（17:08）身份对拍：`only-in-right` 7 条（13765/KillCounter 4 条、CombineTask/CollectItem/DataDriven 冻结覆盖 3 条）**全部溯源到 lane 在 17:38–17:43 改动的文件**（`RetailSimpleHuntPlan.java` 17:38:15、`QuestKillCounterRetailGateTest.java` 17:39:22、`RetailHuntClientCountGateTest.java` 17:39:11、`retail-data-driven-ir-fingerprints.tsv` 17:42:03、`Quest13765RetailAlignmentTest.java` 17:43:30——都在其 17:08 基线之后、我的 T3 起点 17:52:59 之前）；`only-in-left` 4 类 7 方法（lane 自愈）；**我的 T3 窗口内（17:52:59–18:00）零源/数据文件改动**（仅 surefire 报告输出）⇒ **本片零新增**。日志 `gates/T3-175259.log` |

### 8.1 T3 全树对拍

`gates/T3-175259.log` = **2011 例 / 116F / 21E / 1S**（396s）。`t3_failure_diff.py T3-170837 T3-175259`：

- `only-in-right`（新增）7 条：`Quest13765RetailAlignmentTest.preservesWeeklyMetadataKillTargetAndThreeGuardNpcs`、
  `QuestKillCounterRetailGateTest.{killDeclarationsStayInsideKillTransitions, simulatorReproducesTheFixedOverkillDrift, singleCounterQuestsRequireExactlyTheClientGate}`、
  `RetailCombineTaskGateTest.frozenFingerprintsMatch`、`RetailDataDrivenGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`、
  `RetailSimpleCollectItemGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`。
- `only-in-left`（消失）4 类 7 方法（13765 旧形、KillCounter 旧形、CombineTask/CollectItem/DataDriven 冻结覆盖旧形）= lane 自愈。

**归因三步法（QE-069 次序）**：①失败行的冻结产物 mtime —— lane 侧 17:25（collect/combine/data-driven）与 `retail-data-driven-ir-fingerprints.tsv` 17:42:03；
②运行内算值无法直读（lane 未提供）；③**逐文件 mtime 对窗口**——上述 7 条对应的 lane 源/测试/指纹文件全部落在
**17:38:15–17:43:30**（= 我的 T2 窗口内、我的 T3 起点之前），而**我的 T3 窗口内（17:52:59–18:00）无任何源/数据文件改动**；
本片改动面（IR 只有 35010/35011，标记改动不参与编译）与这 7 条零交集 ⇒ **零新增归因本片**。

> 补充观察：我的 T1（`T1-173432`，17:34:32 启动）里这些类还是绿的——因为 lane 的这批编辑落在 **T1 启动之后**
> （17:38–17:43）。**T1 与 T3 的失败数差异是 lane 窗口效应**，不是回归；T1 的价值在"与同期基线 `T1-162409` 逐条比"（本片 −2、零新增）。

## 9. 命令索引

```
python3 -B .agents/summary/scriptdll-quest-driver/p0c45_xml_only_npc_census.py \
    --calibrate-pre .agents/summary/scriptdll-quest-driver/p0c44-registry-pre.tsv
python3 -B .agents/summary/scriptdll-quest-driver/p0c43_dryrun_diff.py \
    --expect 35010,35011,35024,35025,35026,45024,45025,45026
python3 -B .agents/summary/scriptdll-quest-driver/p0c42_builder_fidelity_check.py /tmp/p0c45-fidelity.tsv
python3 -B .agents/summary/scriptdll-quest-driver/p0c45_blast_radius.py
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c45-fp.tsv
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest                     # 重冻前：红（指纹契约）
python3 -B .agents/summary/scriptdll-quest-driver/p0c45_refreeze_fingerprints.py /tmp/p0c45-fp.tsv --apply
mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest,QuestClientContractGateTest,QuestDialogOrderAuditTest'
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 35010 35011 35024 35025 35026 45024 45025 45026 2653 28805
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
```

产物：普查 `p0c45-xml-only-npc-census-{pre,post}.tsv`；爆炸半径 `p0c45-blast-radius.txt`（前端快照
`p0c45-registry-pre.tsv`）；审计逐行 `p0c45-35010-35011-audit-post.txt`；裁定表 `p0c45-extra-owner-decisions.tsv`；
探针 `P0c45ExtraOwnerPruneProbeTest.java.txt`（树内 `.java`/`.class` 均已删）。

## 10. 残留与下一步

- **2653 / 28805（`RETAINED_SUBSTITUTE_OWNER`）**：两条都在 XML 保留面（IR 属 XML，标记只是转写证据），
  本片**无生产改动**。它们**不是**"多余"形——`SETPRO1`/`SETPRO2` 的阶段推进只有这两个 NPC 在做，
  删掉会丢页 ⇒ 若日后该族采纳，必须走**改道/重绑**（QE-075 的边界），不得套用本片剪除规则。
- **标记的边界**：`npc_check` 只判"集合成员"，**不判角色**——同一 NPC 在声明集内、但服务了真端没给它的角色
  （例如把 `talk_npc1` 的页服务在 `reward` NPC 上）不会被标记。登记候选 `XML_ONLY_ROLE_AXIS_PENDING`。
- 遗留候选不变：`SELECT6_CLOSE_EXIT_OTHER_FAMILIES` 62 行、`XML_RETENTION` 1011 未达 44 行（需 QE-066
  双侧对拍）、REWARD 投影余量 15 行、真端数据缺口桶（`ACQUIRE_NPC_UNRESOLVED` 62 / `MONSTER_UNRESOLVED` 56 /
  `SENTINEL_AREA_PENDING` 64）。
- **并发 lane**：本片全窗口 lane 处活跃翻窗（`retail-xml-retention.tsv` 17:08:09 重写 13758–13769 族）；
  T2/T3 的追加失败项全部按"是否在 lane 日志在册 + 是否可达本片改动面"两步归因（§8），本片未触碰其文件。
