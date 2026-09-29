# 2026-09-29 DataDriven Talk 链最终收口（5 个，RETAIL_TALK_CHAIN_DEFERRED 清零）

## 范围

- 解决因静态页梯表未硬编码导致的最后 5 个 Talk 链任务挂起：`1888, 2888, 10033, 15550, 25550`。
- 本批从存量 XML 与生产 catalog 移除，转由真端规范形生命周期（Canonical Lifecycle）原生驱动。
- 关键突破：拒绝继续为任务手工添加微观页码阶梯（如 `p22`, `p23`），而是重构 `RetailClientTalkChainPages` 为通用动态派生器，基于客户端契约 `QuestDialogContract` 动态构建阶段梯。
- `RETAIL_TALK_CHAIN_DEFERRED` 拒绝码正式清零（5 -> 0）；DataDriven `ADOPTED` 升至 `1241`，当前生产宇宙剩余 `267`。
- 存量 XML 降至 `1141`，真端驱动覆盖升至 `5083`。

## 实现要点

1. **`QuestDialogContract` 暴露页面集查询**：
   - 增加 `public Map<Integer, String> pagesForQuest(int questId)`，为派生器提供不可变的页面字典。

2. **`RetailClientTalkChainPages` 规范动态派生器**：
   - 定义真端标准阶段页首数组 `STEP_PAGE_HEADS = [1011, 1352, 1693, 2034, 2375, 2716, 3057, 3398, 3739, 4080, 6500, 6841, 7182, 7523]`。
   - `deriveFromContract(int questId)`：逐步扫描客户端声明的 `select{k}` 首屏及其 `select{k}_*` 续页子页梯；中间步推进动作为 `SETPRO{k}` (`10000 + step`)，末步动作为 `SET_SUCCEED` (`10255`)，入口页规范化为 `4762`。
   - `find(questId, allowDerive)` 分流：默认调用保持对已登记项的纯静态检索，避免影响其他非纯 Talk 链；在 `RetailDataDrivenDefinitionCompiler` 的纯 Talk 分支中显式启用派生。

3. **门禁与测试规范对齐**：
   - `RetailDataDrivenGateTest.acceptedDefinitionsStayInCoveredScope`：将 `chainScope` 扩展为 `(talkAcquire || systemAcquire) && entry.allTalk()`，容纳 10033 的无主/系统发放（`none` 类别）。
   - 退役锁定旧 XML 专有路径的 `RetailDataDrivenTalkChainFamilyTest.java`（旧测试锁死了 1888/2888 的两页接取首屏与 `step0..step4` 节点名，违背规范形生命周期）。

4. **产物退役与登记同步**：
   - 删除 5 个存量 XML：`1888.xml, 2888.xml, 10033.xml, 15550.xml, 25550.xml`；从 `quest_definition_catalog.xml` 移除条目。
   - `retail-xml-retention.tsv`（主/测）翻为 `RETAIL_TABLE DataDriven OK basis=DD_TALK_CHAIN`。
   - `retail-data-driven-drift.tsv` 重新计算并登记，5 个任务全部转为 `ADOPTED`。
   - `retail-data-driven-ir-fingerprints.tsv` 冻结指纹增加此 5 个任务，总数达 1241。

## 验证

- `RetailDataDrivenGateTest`：`6/6` 全绿（涵盖漂移严格对齐、IR 冻结指纹精确覆盖 1241 行、保留清单所有权一致性、已覆盖范围校验）。
- 门禁全家桶全部通过：
  - `RetailOwnershipGateTest`：4/4 绿。
  - `RetailQuestCatalogTest`：2/2 绿。
  - `RetailTsvManifestGateTest`：3/3 绿。
  - `RetailQuestContractTest`：1/1 绿。
  - `RetailSimpleTalkChainGateTest` & `RetailSimpleTalkGateTest`：12/12 绿。
- `git diff --check` 无空白或换行异常。
- 内存库三步检验全部通过（146 个 Pattern，fresh 146 / stale 0）。
