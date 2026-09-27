# 2026-09-26 续片 27 / P0c-44：阶段/报告 NPC 身份漂移 —— 2482 改道 canonical（`XML_NPC_AXIS` 残留候选清零）

## 1. 结论

P0c-43 的残留普查把 `XML_NPC_AXIS` 的**唯一同签名候选**登记为 **2482 Moreinen**：真端声明
`acquired=Hakon(204333) / talk_npc1=Sereniti(204210) / talk_npc2=Neusa(204224) / reward=Moreinen(204211)`，
而登记表（= 遗留 XML 转写）把**阶段 2 与报告/完成**交给 **Andraste(278018) / Finne(278020)**——两个
**真端声明集之外**的 NPC。本片按三源取证裁定**真端胜**并改道 canonical：
**报告 NPC 未接线 1 → 0、阶梯缺失 0 → 0（`XML_NPC_AXIS` 在登记表内清零）**。

**这条缺陷的含义比"页未达"更重**：玩家按真端/客户端指示去找 Sereniti 与 Neusa、再找 Moreinen 报告，
而服务端只认 Andraste 与 Finne ⇒ **任务在阶段 2 卡死**（除非那两个 NPC 恰好在别处可交互且状态对得上）。
但它的**审计表现是全绿**（见 §3），因此在本片之前从未作为缺陷现形。

**本片第二个交付物是判据的可证伪性**：新轴 `LEGACY_STAGE_OWNER_DRIFT` 的四条 fail-closed 守卫用
**负测试**逐条证明会拦（`p0c44_guard_probe.py`：code 域 / 声明集内不得当第三方 / 真端 reward 名必须一致 /
与阶梯表互斥，4/4 拦下且裁定表 md5 还原、登记表零写入），生成器重跑仍与登记表**逐字节相同**
（`f3a99ac118736b0e096a3b10fb21b2f2`，430910 字节，8 张既有裁定表零漂移）。

## 2. 三源取证（判例 2482）

| 源 | 内容 | 指向 |
|---|---|---|
| 真端表 `Quest_SimpleTalk.xml` | `acquired=Hakon / talk_npc1=Sereniti / talk_npc2=Neusa / reward_npc_name=Moreinen` | K=2，Sereniti→Neusa→Moreinen |
| 客户端 `QUEST_Q2482.html` 正文 | 名字出现次数 **Sereniti 3 / Neusa 1 / Moreinen 2 / Andraste 0 / Finne 0**；任务书三行逐行点名「和塞雷尼提对话 / 和纽萨对话 / 和莫雷内恩对话」 | 与真端逐名一致 |
| 转写形自承认（census） | `p0c10e` 第 4 列 NPC 集 = `204333|204210|204224|204211`；第 7 列 `DEVIATION:SELECT3_1@204210;SELECT3_1@278018;SELECT_QUEST_REWARD@278020`；登记表 R 行 `npc_check=XML_ONLY:278018` / `XML_ONLY:278020` | 转写时已记「这两 NPC 在真端声明集之外服务了动作」 |

客户端页梯与 K=2 一致：`select2(1352)→select2_1(1353, SETPRO1)`、`select3(1693)→select3_1(1694, SETPRO2)`、
`select5(2375, SELECT_QUEST_REWARD)`；任务书 3 行 = K+1（QE-051）。

## 3. 审计盲区（本片最重要的判例）

改道**前后审计都是全绿**：

| 状态 | 审计 | 转换数 | 阶段 2 owner | 报告 owner |
|---|---|---|---|---|
| 改道前 | 12 行全 `PAGE_ACTION_MATCHED`/`TERMINAL_PAGE_REACHED` | 36 | **278018 Andraste** | **278020 Finne** |
| 改道后 | 13 行全达 | 39 | 204224 Neusa | 204211 Moreinen |

审计的 owner 判据是**自洽性**（下发页的路由与页按钮的路由同 owner、按钮有路由），**不是身份**
（owner 是否真端声明的那个人）。所以"页可达 + 按钮有路由 + owner 自洽"三件都成立时，绑错人**不会**
被审计、契约门或链指纹发现——只有三源（真端声明 / 客户端正文 / census DEVIATION·`XML_ONLY`）能现形。
另：`N reward REWARD 0` 与客户端末行 2 不符的**投影漂移**也随改道一并消除（2482 离开
`p0c34-chain-reward-row-divergence.tsv`）。

## 4. 生成器侧落地（第二个 code + 一条新取证通道）

裁定表 `p0c43-canonical-resynthesis.tsv` 的 code 域扩为两个已观测漂移形：

| code | 判例 | 转写形特征 |
|---|---|---|
| `LEGACY_REPORT_FLOW_ON_ACQUIRED` | 24123 | 报告/完成流**全绑接取 NPC** |
| `LEGACY_STAGE_OWNER_DRIFT` | 2482 | 阶段与报告 flow 绑**真端声明集之外的第三方 NPC** |

加载器把**真端轴**抽成两 code 共用（acquired 唯一解析 / `reward_npc_name` 与裁定表一致、唯一且 ≠ acquired /
每个 `talk_npcK` ≠ acquired 防自环 / 客户端任务书行数 == K+1 / 客户端页册含 `SELECT{i+2}`），
**形判轴**按 code 分派；新增 `census_deviations` 取证通道（census DEVIATION 列必须记过该第三方 NPC——
"转写形自己承认漂移"才是可裁定证据，否则 fail-closed）。

## 5. 落地物

| 文件 | 说明 |
|---|---|
| `src/main/resources/…/quest_client_talk_chain_steps.tsv`（+`target/classes` 副本） | 2482 块 18→20 行；md5 `f3a99ac118736b0e096a3b10fb21b2f2`（5110→5112 行） |
| `.agents/…/build_quest_client_talk_chain_steps.py` | 加载器泛化为双 code + `census_deviations` 通道；真端轴共用、形判轴分派 |
| `.agents/…/p0c43-canonical-resynthesis.tsv` | 新增 2482 行（`LEGACY_STAGE_OWNER_DRIFT`）+ 表头 code 域说明 |
| `.agents/…/p0c44_guard_probe.py` / `p0c44-guard-probe.txt` | 四条守卫的**负测试**（M1 code 域 / M2 声明集内 / M3 reward 名不符 / M4 与阶梯表互斥），4/4 拦下 + 还原断言 |
| `.agents/…/p0c44_refreeze_fingerprints.py` | 指纹外科重冻（`EXPECTED_CHANGED=("2482",)` + PRE 值守卫 + whipsaw 守卫） |
| `.agents/…/p0c44-registry-pre.tsv` | 改道前登记表快照（md5 `322f4d1a…`，复现用） |
| `.agents/…/p0c44-2482-audit-{pre,post}.tsv` | 审计逐行前后对照（12 行 / 13 行，两态均全绿——审计盲区证据） |
| `.agents/…/p0c44-2482-ir-{pre,post}.tsv` | 编译 IR 前后对照（36 / 39 条转换，owner 逐条可见） |
| `.agents/…/p0c43-residue-census.tsv` | 普查重跑：**报告 NPC 未接线 0 / 阶梯缺失 0**（2482 已出表） |
| `src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv`（+target 副本） | 外科重冻 2482 一行（288 行布局逐字节保留；双副本 md5 `d995c6861b9cf34cf306a169dbb243ea`） |
| `.agents/…/P0c44StageOwnerDriftProbeTest.java.txt` | 探针源码归档（树内 `.java` 与 `.class` 已删除） |

## 6. 验证

| 门 | 结果 |
|---|---|
| 生成器忠实性 | `FIDELITY_OK`（430910 字节）+ `REGISTRY_MD5 f3a99ac118736b0e096a3b10fb21b2f2` + `DECISION_TABLES_STABLE: 8 张裁定表零漂移` |
| 守卫负测试 | `GUARD_PROBE_OK: 4/4 守卫按预期拦下；裁定表还原；登记表未被写入` |
| 干跑比对（安装前） | `CHANGED [2482] / ADDED [] / REMOVED []`；块差异 = 18→20 行（`reward REWARD 0→2`、`B NPC_REPORT 204211`、阶段/报告 owner 全换） |
| 审计（2482） | 12 行 → **13 行全达**（`PAGE_ACTION_MATCHED` ×11 + `TERMINAL_PAGE_REACHED` ×2），owner 逐行 = 真端 NPC |
| 链门（校验轮，无 `-D`） | `RetailSimpleTalkChainGateTest` 2/2 绿；指纹 `changed={2482}`（`b2e52f47…` → `f616ccc9…`） |
| 契约门 | `QuestClientContractGateTest` 1/1 绿，fatal 0 |
| 审计全表 | `QuestDialogOrderAuditTest` 17/17 绿 |
| 净树复跑 | 链门 2 + 审计 17 + 契约门 1 = **20/20**（探针删除后另跑 21/21 含探针） |
| T1（18 类 63 例） | **3F**（`gates/T1-165546.log`）与 P0c-43 `T1-162409.log` **逐条相同**（DataDriven×2 + CollectItem×1）⇒ 零新增 |
| T2（2482，60 例） | **3F+1E**（`gates/T2-170200.log`）：3F 同上；**1E = `RetailOwnershipGateTest` `catalog must not keep retired quests: [23758, 23759, …]`** —— 并发 lane 正在翻 13758–13769/23758–23769 族，其 `retail-xml-retention.tsv` mtime **17:08:09 落在本片 T2 窗口（17:02–17:08）内** ⇒ 翻窗中间态，非本片 |
| T3（全树） | `gates/T3-170837.log` 2015 例 / 121F+23E+1S。与 P0c-43 基线 `T3-163754`（2006 例 / 121F+21E）失败身份集对拍：**`only-now = 2`、`only-pre = 0`**；两条新增均属上述并发族——`Quest13765RetailAlignmentTest.preservesWeeklyMetadataKillTargetAndThreeGuardNpcs`（`NoSuchFile …/quests/13765.xml`）+ `QuestKillCounterRetailGateTest.simulatorReproducesTheFixedOverkillDrift`（该测试断言对象即 13758/13765）⇒ **零新增归因本片**；T3 日志中提及 2482 的失败 **0 条** |

## 7. 残留与下一步

`XML_NPC_AXIS` 在登记表内**清零**（117 行真端多阶段行：`REWARD_NPC_UNWIRED` 0 / `LADDER_MISSING` 0），
普查表只剩信息性 2 行（35010/35011 共享交付 NPC，非缺陷）。

其他承轴不变：`SELECT6_CLOSE_EXIT_OTHER_FAMILIES`（62 行）、44 行 `XML_RETENTION` 的 1011 未达
（QE-066 双侧对拍后采纳）、25070 的 DataDriven 族形资质、`REWARD 投影` 余量
（`p0c34-chain-reward-row-divergence.tsv` 现 15 行：24123/2482 已随改道闭合）、
真端数据缺口桶（`ACQUIRE_NPC_UNRESOLVED` 62 / `MONSTER_UNRESOLVED` 56 / `SENTINEL_AREA_PENDING` 64）。

**下一个可用轴（本片新增）**：既然审计对 owner **身份**是盲区，可用 `npc_check=XML_ONLY:*` 做一次
**全登记表普查**（不限于真端多阶段行），把"服务了动作但不在真端声明集内"的 NPC 全部列出——
本片只覆盖了 `XML_NPC_AXIS` 那一类（阶段/报告漂移），其余动作类型（接取侧、物件侧、检查侧）未查。
登记为 `XML_ONLY_NPC_CENSUS_PENDING`（下一切片候选）。

## 8. PENDING（需授权）

- **运行时/客户端目检**（2482）：Hakon(204333) 接取 → Sereniti(204210) 两页 → **Neusa(204224)** →
  Moreinen(204211) 报告 → 领奖窗。按约束**未启服**，本轮未执行任何服务器生命周期操作。
- 前片遗留目检清单不变（24123、1323、21460/29070/29071、2458/4905/4906、P0c-39/40/41/42 各行）。

## 9. 命令索引

```
python3 -B .agents/summary/scriptdll-quest-driver/p0c43_dryrun_diff.py --expect 2482
python3 -B .agents/summary/scriptdll-quest-driver/p0c42_builder_fidelity_check.py /tmp/p0c44-fidelity.tsv
python3 -B .agents/summary/scriptdll-quest-driver/p0c44_guard_probe.py            # 守卫负测试（4/4）
python3 -B .agents/summary/scriptdll-quest-driver/p0c43_residue_census.py         # 残留清零复核
mvn -o -B test -Dtest=RetailSimpleTalkChainGateTest -Dretail.talkChain.fingerprintOut=/tmp/p0c44-fp.tsv
python3 -B .agents/summary/scriptdll-quest-driver/p0c44_refreeze_fingerprints.py /tmp/p0c44-fp.tsv --apply
mvn -o -B test -Dtest='RetailSimpleTalkChainGateTest,QuestClientContractGateTest,QuestDialogOrderAuditTest'
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 2482
.agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
```

## 10. 并发车道注记

并发 lane 在本片全窗口处于**活跃翻窗**：17:08:09 重写 `retail-xml-retention.tsv`（13758–13769 + 镜像
23758–23769 族退役），导致 T2 的 `RetailOwnershipGateTest` 与 T3 的两条新失败（均指向 13758/13765）。
**本片未触碰该 lane 的任何文件**；其族与我方改动面（2482）零交集。

### 10.1 切片编号撞车（唯一键 = lane + 续片号）

本片编号 "P0c-44" 与并发 lane 同轮发布的 "P0c-44（续片 18，hunt 家族代表形 18996/28996）" **编号相同**——
两个 lane 共享 `.agents/summary/scriptdll-quest-driver/` 目录，且各自按自己的续片序推进。唯一键因此是
**lane + 续片号**（本片 = SimpleTalk 链车道 / 续片 27），不是 P0c 序号。文件级无冲突：本片产物一律带任务号
（`p0c44-2482-*`、`p0c44-guard-probe.txt`、`p0c44_refreeze_fingerprints.py`），lane 的产物为
`p0c44_retire_hunt_family_rows.py`（hunt 族）；`p0c43_retire_sensory_area_rows.py` 同理属 lane（P0c-43 报告
§9 已记同一现象）。**引用本片时以任务号 2482 为准。**
