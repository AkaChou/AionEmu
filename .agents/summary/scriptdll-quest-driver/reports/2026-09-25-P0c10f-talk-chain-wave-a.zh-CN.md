# P0c-10f：SimpleTalk wave A 链式合成落地（83 行 ADOPT 退役 + 40 行 KEEP 变体登记）

- 日期：2026-09-25
- 切片：P0c-10f（wave A 链式合成 + 裁定 + 退役落地；retired 3918 → **4209 实测**，其中本切片 +83，
  并发批同窗口 +208 其自行落地）
- 状态：**落地与验证完成；T1 收敛中（剩余失败全归并发车道）**

## 1. 交付

1. **链路由登记表** `quest_client_talk_chain_steps.tsv`（生成器 `build_quest_client_talk_chain_steps.py`，
   词汇 fail-closed）：N 节点 673 + R TalkToNpc 路由 837（含 `actions` 复数拆分、can-act 事件 C 记录、
   teleport 坐标、dialog type 保真）+ B 规范块参数（NPC_START/NPC_REPORT/NPC_COMPLETE）+
   P 布局逐字；页面交叉核验 675/675 全过（CLIENT 信件页 / GLOBAL 引擎常页）。
2. **编译器链式路径**：`RetailClientTalkChainSteps` 加载器 + `RetailSimpleTalkDefinitionCompiler`
   增 `buildChain`（登记表逐字回放 + npc-start/npc-report 块参数化合成 + npc-complete 按存在性
   合成 + 统一显式覆盖过滤，与 `QuestXmlBlockExpander` 的 `explicitDialogRoutes` 同口径）+
   precheck 细分码（`RETAIL_TALK_CHAIN_COMPOUND`/`NO_START`/`NO_ROUTES`/`START_CONFLICT`/
   `RETAIL_TALK_NPC_*`）。
3. **对拍三轮迭代**（探针 `RetailTalkChainProbeTest`，已归档 `.java.txt`）：18 → 65 → **83 EQUIVALENT**。
4. **裁定** `p0c10f-talk-chain-decisions.tsv`：**83 ADOPT_RETAIL/CHAIN_REPLAY**（IR 等价 +
   冻结指纹 `retail-simple-talk-chain-ir-fingerprints.tsv`，重算稳定）+ **40 KEEP**
   （NPC_COMPLETE_VARIANT 15 / CHAIN_SHAPE_UNSUPPORTED 15 / NO_START 6 / START_CONFLICT 2 /
   ENTER_WORLD_EDGE 1 / SHAPE_VARIANT 1）。
5. **落地**：83 个生产 XML 删除 + catalog 同步（余 **2015**）；**`verify_retirement` =
   `catalog=2015 directory=2015 retired=4209 sum=6224 — OK`**；classpath 预对齐（含残组单清）。
6. **保留清单**：生成器接入 `TALK_CHAIN_DECISIONS`；SimpleTalk RETAIL_TABLE **1631**；
   drift 登记全量细分码刷新（COMPOUND 177 / NO_START / NO_ROUTES 等，经 surefire 报告迭代收敛）。

## 2. 门禁

| 门禁 | 结果 |
|---|---|
| SimpleTalk 族门禁 | **3/3 绿**（新增链式回放保真不变量：定义节点须与登记表 N 记录逐一相等） |
| T1 | 60 例 2F+6E **全归并发车道**（15548/25548 NO_NODES、DD 冻结集 926/930 差一系、CatalogManifest 级联）；本切片自身面绿 |

## 3. 教训（沉淀）

- 临时探针 + `-D` dump + surefire 完整报告反哺登记表的"对拍-收敛"循环高效可行；
  断言消息截断 20 条时须读 surefire 全文。
- 多轮 python 补丁叠加易失配（转义/缩进/锚点）——同区域 ≥2 次修改应直接整体重写。
- 共享主树三重干扰实测：并发半成品测试挡 testCompile（轮询恢复）、并发 maven 竞态删
  target 类文件（NoClassDef，重编译恢复）、并发落地窗口使全局计数漂移（±208，verify 以
  全局恒等式为准）。

## 4. 未验证 / 下一步

- **未验证**：83 行运行时行为、客户端抽检（P3 终局统一做）。
- **下一步**：① 探针转正为 `RetailSimpleTalkChainGateTest`（冻结指纹 + retention 分区 +
  回放保真三不变量，进 T1）；② wave B 复合 177 行（链+物品轴）；③ 残组/变体 40 行的
  逐机制归零评估。
