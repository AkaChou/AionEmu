# P0b 实施报告：数据基础（入仓 + 来源 hash 门 + 页注册表 loader）

> 文档状态：**P0b 完成（待授权验证）**。授权依据：用户「继续完成计划」（2026-10-01，紧接 P0a 报告之后）。
> 批次口径：计划 §4.6/§7 批门表——只建数据基础，**不接任务路由、不翻转 owner、不删除资源、不跑服务器**。
> 构建纪律：按 AGENTS.md 规则 1，未跑任何 Maven/javac 命令；待授权命令见 §5。

## 1. 开工范围声明（§4.7 纪律）

| 项 | 内容 |
|---|---|
| 批次 | P0b 数据基础 |
| 新增资源 | `retail/HtmlPages.xml`、`retail/challenge_task.xml`、`retail/quest_random_rewards.xml`、`retail/npcfactions_quest.xml`、`retail/table-source-provenance.tsv`（均在 `src/main/resources/aion/data/static_data/quest/`） |
| 新增代码 | `questEngine/tablelane/HtmlPagesRegistry.java`（唯一新生产类） |
| 修改代码 | `questEngine/QuestEngine.java`（`load()` 内一行强制装载 + import） |
| 新增测试 | `tablelane/HtmlPagesRegistryTest.java`、`tablelane/TableSourceProvenanceGateTest.java` |
| 不触碰 | QE-112 全部在飞文件、legacy/ 两张旧表（现行消费者继续用旧副本）、XML 车道、任何路由/owner 代码 |

## 2. 已完成

### 2.1 表入仓（幂等工具 `tools/ingest_tables.py`，重跑输出一致已验证）

| 表 | 行数 | 入仓形态 | sha256（源=仓，byte 级一致） |
|---|---:|---|---|
| `HtmlPages.xml` | 5904 | UTF-16LE 原样 | `91ea9a0f…` |
| `challenge_task.xml` | 123 | UTF-16LE 原样 | `afbd0f50…` |
| `quest_random_rewards.xml` | 817 | UTF-16LE 原样 | `a8972172…` |
| `npcfactions_quest.xml` | 436 | UTF-16LE 原样 | `f1b63f6a…` |

- 4 张均为 **byte 级一致拷贝**（写出前后 sha256 强制相等，否则拒绝落盘）。
- `table-source-provenance.tsv`：13 行 = 9 张已转换表（P0a 已证 token 语义等价，记录源/仓两侧 hash 与转换标记）+ 4 张 byte 级新表。路径按 ENVIRONMENT.md 只写名称引用（`<真端根>/Map/XML/…`），无机器绝对路径。
- `quest_random_rewards.xml` 以真端源重新入仓为**新文件**；`legacy/quest_random_rewards.xml`（329 行陈旧副本）未动——其现行消费者随对应批次切换时再退役（P0b 禁删除资源）。

### 2.2 `HtmlPagesRegistry`（`tablelane` 包，计划 §6.2 组件第一件）

- 静态惰性装载（与 `ProductionQuestDefinitions` 同风格）；classpath 资源 `aion/data/static_data/quest/retail/HtmlPages.xml`。
- UTF-16 BOM 自动识别 + 内部 DTD 实体子集解析；`ACCESS_EXTERNAL_DTD/SCHEMA=""` 拒绝一切外部实体（安全处理开启）。
- fail-closed 语义错误码：`NATIVE_TABLE_PARSE_FAILED`（缺资源/根元素错/重复 id/重复 htmlpagename/缺字段/非数字 id/空表）、`NATIVE_PAGE_UNREGISTERED`（`require(int)` 未知页）。
- 空元素 `<htmlpagename></htmlpagename>` 归一为未声明（真端实测 5869 行全非空，无此形态，防御性处理）。
- 数据前提已实证：5904 行、id 零重复、htmlpagename 零重复（P0a/本批 Python 双重校验）。

### 2.3 生产装配

- `QuestEngine.load(CountDownLatch, PreparedProductionDefinitions)`（生产 boot 路径）开头 `HtmlPagesRegistry.ensureLoaded()`——页表缺失/损坏直接让启动失败。
- 刻意**不**放进 `prepareProductionDefinitions`（该路径被启动门禁测试注入复用，避免把页表依赖扩散进既有 76 例门禁基线）。

### 2.4 测试（已写、未跑）

- `HtmlPagesRegistryTest`：真端数据形状（5904 行、id 0/3 抽查、页名→id）+ 6 个负例合成夹具（重复 id/重复页名/缺字段/错根/非数字 id/空表）+ 内部实体展开正例 + 动态未知 id 的 `require` 负例（不在测试里发明 id）。
- `TableSourceProvenanceGateTest`：13 行溯源逐行校验仓内字节 hash；BYTE_IDENTICAL 行额外断言 仓 hash==源 hash + UTF-16 BOM 保留；P0b 四表存在性。
- 爆炸半径：`QuestReloadAtomicityTest` 是唯一直调 `QuestEngine.load` 的测试，将连带执行页表装载（依赖 parse 正确性，见 §5 待授权验证）。

## 3. 无构建验证（已完成）

1. 入仓 byte 一致性：Python sha256 源=仓相等（四表）。
2. git 归类：临时 index（已删，真实暂存区未动）验证 4 张 UTF-16 表被 git 判为 `i/-text w/-text`（binary）——`* text=auto eol=lf` 不会归一化其字节，commit 后 byte 一致性保持。
3. fail-fast 数据前提（重复/空值）已对真端源实证。
4. 溯源清单重跑幂等（两次运行输出一致）。

## 4. 验证结果（2026-10-01，用户授权后执行）

```bash
mvn -Dtest='HtmlPagesRegistryTest,TableSourceProvenanceGateTest,QuestReloadAtomicityTest' test
# Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS（main+test 全量编译通过）
#   HtmlPagesRegistryTest 5/5（真端 UTF-16 解析 5904 行、实体展开、6 负例、未知页 fail-closed）
#   TableSourceProvenanceGateTest 2/2（13 行 hash 门 + BYTE_IDENTICAL 行 BOM/源 hash 断言）
#   QuestReloadAtomicityTest 1/1（QuestEngine.load 爆炸半径无回归）
```

**P0b 验证收口：绿。** 提交仍需单独授权（按文档随同规则只 stage 本批路径）。

## 5. 批次边界自查（附录 D.4 收口段）

- [x] 未接任务路由 / 未翻 owner / 未写相机 / 未删除任何资源 / 未跑服务器；
- [x] `src/main` 未引入客户端派生 TSV/CSV/台账（provenance 清单是溯源元数据，非任务内容台账）；
- [x] 无 quest id 硬编码（registry 为通用页表，零任务常量）；
- [x] 未用 `git add -A`（本批未 stage/未 commit）；未 push；
- [x] QE-112 脏文件零触碰（`git status` 对照开工快照一致 + 仅新增本批路径）。

## 6. 下一步（须用户决策/授权）

1. **授权 §4 的两条聚焦测试命令**（或合并跑）；通过后 P0b 收口，可提交（提交需另行授权，提交时按文档随同规则 stage 本批路径）。
2. **P1 前置设计决策：名字解析器策略**（P0a §7.1：4623 行真名缺失的解析策略——规范化规则 / 刷怪点 join / 带出处映射数据表，三选一或组合）。
3. **80817 处置取向**：复刻真端「不可完成」还是显式禁用（P0a §7.4）。
4. P1 开工还需声明 SimpleHunt 批的 focused test 清册。
