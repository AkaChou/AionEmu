# 任务 XML 紧凑语法全量迁移实施规范

状态：`READY_FOR_CLAUDE_CODE`

最后更新：2026-08-11

执行仓库：当前 checkout 根目录（可用 `git rev-parse --show-toplevel` 获取）

## 1. 文档用途

本文档是任务 XML 作者语法简化的实施合同，也是 Claude Code 执行、并行代理协作、迁移验收和后续
Codex 审查的唯一任务说明。实施者必须先完整阅读本文档，再检查当前工作树和代码；不能只根据摘要批量
替换 XML。

本次改动一次性完成以下事项：

1. 将任务节点切换为紧凑节点语法，最终生产解析器只接受新格式。
2. 新增严格的 `npc-dialog` 自循环领域块，并保持现有普通 transition 和八种领域块可用。
3. 新增保守、可审计、默认只扫描的迁移工具。
4. 迁移全部生产任务 XML、测试资源和 Java 内联测试 XML。
5. 证明每个改写文件迁移前后的完整 `QuestDefinition` IR 相等。
6. 保持运行时状态、数据库、协议、副作用顺序和根元素 `version` 含义不变。

任何行为修复、任务数据校正、客户端页面修正或奖励调整都不属于本次迁移。发现现有行为问题时，应记录到
报告或单独任务中，不能顺手混入语法迁移。

## 2. 已确认的仓库基线

实施前必须重新采样，以下数据仅是 2026-08-11 的已核实基线：

- 生产定义目录为
  `src/main/resources/aion/data/static_data/quest_definition/quests/`。
- 当前生产目录中有 6246 个 `<quest-definition>` XML。
- 旧 `<project>` / `<vars>` 包装仍出现在 5834 个生产或测试 XML 文件中。
- Java 测试中还存在 text block、单行字符串和拼接字符串形式的旧节点语法。
- 编译链为
  `QuestDefinitionXmlCompiler -> QuestXmlBlockExpander -> QuestDefinitionCompiler`。
- `QuestDefinition`、`QuestNode` 和 `QuestTransition` 都是不可变 record/value object，完整 definition
  的 `equals` 可以作为 IR 等价比较的基础。
- 当前已有八种领域块：`npc-start`、`npc-complete`、`npc-item-report`、`npc-report`、
  `counter-grid`、`counter`、`kill-chain`、`kill-routes`。
- `QuestDefinitionCatalogManifestTest.externalProductionCatalogCompiles()` 会通过生产 catalog 编译正式
  owner；全目录扫描能力位于 `QuestDefinitionDirectoryLoader`，两者不能互相替代。

### 2.1 当前 dirty 工作树不是迁移输入的默认授权

当前 checkout 已包含用户现有的 Java、XSD 和多个生产任务 XML 改动，而且这些改动在实施期间可能继续
变化。执行者必须把它们视为用户资产：

- 不得 `git reset --hard`、`git checkout --`、`git restore` 或覆盖整文件。
- 不得把当前 dirty 文件自动归类为本任务产生的改动。
- 实施开始、批量改写前、测试前和交付前都要重新运行 `rtk git status --short`。
- 必须检查重叠文件的 diff，并在最终交付中区分“原有改动”和“本任务改动”。
- 迁移工具自身必须在 `--apply` 前拒绝任何已 dirty 的目标文件；不能提供绕过该保护的 force 参数。

如果需要在当前 dirty 基线上完成全量迁移，使用第 10 节的隔离工作树流程。不能为了让工具通过而提交、
暂存、丢弃或隐藏用户改动。

本文件本身命中仓库现有的 `.gitignore` 规则 `/docs/*`。创建隔离 worktree 时必须从原工作树显式复制本文件，
不能假设 Git 会自动带入；不要为此修改 `.gitignore`。只有用户后续明确要求提交文档时，才可用显式路径
`git add -f docs/QUEST_XML_COMPACT_MIGRATION_PLAN.zh-CN.md` 纳入提交。

## 3. 不可改变的语义合同

### 3.1 IR 和运行时

迁移前后必须保持以下内容逐项相等：

- `QuestDefinition.id`
- `QuestDefinition.version`
- 完整 `QuestMetadata`
- 完整 `ProgressLayout`，包括字段顺序
- `QuestNode` 顺序、label、status 和 variables
- `QuestTransition` 顺序
- 每条 transition 的 event、conditions、actions、target、after-commit、priority 和 source

节点顺序不是格式细节。`QuestDefinitionCompiler` 的可达性和初始节点检查会使用定义顺序，因此不得排序、
去重或按 label 重建节点。

本次不改变：

- `QuestStatus + quest_vars` 的数据库持久化模型
- packed variable 的 bit layout、范围或恢复规则
- 任务协议包类型、action 编号和发送时序
- transaction / after-commit 边界
- 任务奖励、物品、NPC、页面、条件或动作
- 生产 owner、catalog 模式或 legacy owner 状态
- 根元素 `quest-definition version` 的含义和数值

### 3.2 只做作者语法和编译期降低

新节点语法和 `npc-dialog` 都只存在于 XML 前端。它们必须降低为现有 canonical IR，不能新增运行时节点、
事件、condition、action、after-commit action 或持久化字段。

禁止借本次迁移：

- 删除或合并枚举状态节点
- 压缩 `counter-grid` 的节点笛卡尔积
- 根据 label 推断 status 或变量值
- 为缺失变量补 `var0=0` 或 `var0=1`
- 将特殊页面统一成常见页面
- 将不连续 transition 移到一起以便匹配
- 调整 priority 或 after-commit 顺序

## 4. 紧凑节点 XML 合同

### 4.1 唯一合法格式

有变量节点：

```xml
<node label="started" status="START">
  <var name="var0" value="0"/>
</node>
```

无变量节点可以自闭合：

```xml
<node label="unaccepted" status="NONE"/>
```

约束：

- `node.label` 必填，语义与当前实现相同。
- `node.status` 必填，类型仍为现有 `status` enum。
- `node/var` 可出现 0 到 32 次。
- `var.name` 必填，继续引用 `<progress><bit-field name="..."/>`。
- `var.value` 必填，仍为 `xs:int`。
- 变量顺序按 XML 原顺序进入 `LinkedHashMap`，不得排序。
- 所有现有未知字段、字段范围、重复节点、重复 projection 和可达性校验保持不变。

### 4.2 最终必须拒绝的旧格式

最终 XSD 和 `QuestDefinitionXmlCompiler` 不得接受以下包装：

```xml
<node label="started">
  <project status="START">
    <vars>
      <var name="var0" value="0"/>
    </vars>
  </project>
</node>
```

也不得接受混合格式，例如 node 同时有 `status` 和 `<project>`、直接 `<var>` 外再包 `<vars>`、缺少
`status`、动态属性 `var0="1"` 或根据 label 省略 projection。

最终拒绝应发生在严格 XML/XSD 前端，测试至少断言 `QuestCompilationException.code()` 为
`INVALID_XML`。生产 compiler 中不得留下 legacy fallback、双格式分支或静默兼容。

### 4.3 编译器实现边界

`QuestDefinitionXmlCompiler.parseNodes` 应直接读取 `node.status` 和 node 的直接 `var` 子元素，构造与当前
完全相同的：

```text
QuestNode(label, NodeProjection(status, LinkedHashMap<String, Integer>))
```

临时双格式支持只允许用于开发阶段生成 before/after 证据，最终 diff 中必须删除。永久迁移工具如需读取旧
节点，应在工具或测试辅助层先把旧节点结构归一化为新节点，再交给最终 production compiler；不得把旧格式
重新放回生产 parser。

## 5. `npc-dialog` 领域块合同

### 5.1 语法

```xml
<npc-dialog source="started"
    npc-ids="203097 799093"
    dialog-ids="31">
  <show-quest-dialog dialog-id="1352"/>
</npc-dialog>
```

允许的唯一响应子元素为三选一：

```xml
<show-quest-dialog dialog-id="1352"/>
<show-quest-selection-dialog dialog-id="10"/>
<close-dialog/>
```

### 5.2 属性和结构约束

- `source` 必填，必须引用现有节点。
- 不暴露 `target`；每条展开 transition 的 target 固定等于 source。
- `npc-ids` 必填，至少包含两个唯一正整数。
- `npc-ids` 保留书写顺序；重复、0、负数、非整数都必须拒绝。
- `dialog-ids` 必填，复用普通 `talk-to-npc.dialog-ids` 的空格、逗号和 `a..b` 范围语义。
- dialog ID 的去重、范围方向、范围长度和总数量限制必须与普通 talk event 一致。
- 恰好有一个响应子元素；缺失、两个响应、嵌套 wrapper 或其他 after-commit action 都必须拒绝。
- 不允许 conditions、actions、priority、额外 after-commit wrapper 或未知属性。
- 块只能表达同一 source 上的纯自循环对话。

### 5.3 精确降低顺序

展开顺序必须是 NPC 在外层、dialog ID 在内层：

```text
for npcId in npc-ids document order:
  for dialogId in expanded dialog-ids order:
    transition(
      event = TalkToNpc(npcId, dialogId),
      conditions = [],
      actions = [],
      source = source,
      target = source,
      priority = null,
      afterCommit = [the single declared response]
    )
```

`QuestXmlBlockExpander.expand` 继续按 `<transitions>` 子元素的文档顺序处理。`npc-dialog` 与普通
`transition`、现有八种领域块混排时，前后 transition 的相对位置必须保持不变。

### 5.4 实现复用要求

- 尽量复用 `QuestDefinitionXmlCompiler.parseAfterCommitAction` 解析三个响应。
- dialog ID 列表/范围解析应抽取或复用现有严格逻辑，避免普通 talk event 与 `npc-dialog` 出现两套不同
  行为。
- 抽取公共 helper 时，普通 `talk-to-npc.dialog-ids` 的错误码和行为必须保持不变。
- `QuestXmlBlockExpander.Context` 的 node 引用校验和异常上下文格式保持一致。

## 6. 代码改动范围

预期核心文件：

- `src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd`
- `src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDefinitionXmlCompiler.java`
- `src/main/java/com/aionemu/gameserver/questEngine/definition/QuestXmlBlockExpander.java`
- `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDefinitionCompilerTest.java`
- `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestXmlDomainBlocksTest.java`
- `scripts/quest_xml_compact_migration.py`
- 迁移 verifier 所需的最小 Java 测试/工具辅助类
- 所有含旧节点语法的生产 XML、测试 XML 和 Java 测试 XML 字符串

除非验证证明必要，不修改 runtime、DAO、network、quest protocol、catalog 结构或业务测试断言。

若触碰已有文件，遵守仓库约定：删除非必需的文件头说明注释，但保留法律、生成器和构建要求的头部。

## 7. 迁移工具合同

新增：

```text
scripts/quest_xml_compact_migration.py
```

### 7.1 CLI

默认行为必须只扫描：

```bash
rtk python3 scripts/quest_xml_compact_migration.py
```

只有显式 `--apply` 才允许写文件：

```bash
rtk python3 scripts/quest_xml_compact_migration.py --apply
```

默认扫描范围：

- `src/main/resources/aion/data/static_data/quest_definition/quests/**/*.xml`
- `src/test/resources/**/*.xml`
- `src/test/java/**/*.java` 中的 quest-definition 内联 XML

工具不得修改 XSD、生产 Java compiler 或非测试 Java 源码。路径参数如有提供，只能缩小范围，不能绕过
dirty 检查或 IR 验证。

### 7.2 结构解析和最小文本替换

语义识别必须使用 XML 结构解析器，不能用正则表达式判断 XML 结构。为了保留原始格式，可以另用词法扫描
建立元素的 byte/character span，但该扫描器只负责定位已由结构 parser 识别的元素，不能负责语义匹配。

改写规则：

- 只替换完整 `<node>...</node>` 或自闭合 node span。
- 只替换完整、连续、顺序严格匹配的普通 transition span。
- 由文件尾向文件头应用 span replacement，避免 offset 漂移。
- 不重排未触碰元素的属性。
- 不格式化整个文档。
- 不改变 XML declaration、编码、换行风格、尾部换行、注释或无关空白。
- 新生成行沿用被替换元素的缩进和换行风格。
- 无变量节点优先生成自闭合 `<node .../>`；有变量节点保留每个 var 的原相对顺序。

对于 Java 内联 XML：

- 必须正确处理 Java text block 的共同缩进。
- 单行或拼接字符串不能靠不安全的全局字符串替换；可以由 coordinator 逐处改写，或给工具增加受测试的
  Java 字符串抽取/重建能力。
- 工具遇到无法可靠还原的内联 XML 时必须报告 `unsupported_inline_xml`，不能猜测。
- 最终验收仍要求测试源码中的旧 node wrapper 为零，因此报告出的 unsupported 项必须由 coordinator
  安全迁移并验证。

### 7.3 原子性、备份和回滚

`--apply` 前必须先完成全批次 preflight：

1. 解析所有目标。
2. 计算所有候选 replacement。
3. 获取 Git dirty 文件集合。
4. 只要任一将被改写的目标已 dirty，整个批次拒绝写入。
5. 确认 Java verifier 可运行。

写入前，把每个将改写的原文件按仓库相对路径复制到：

```text
target/quest-xml-migration/before/<relative-path>
```

每个文件采用临时文件 + 原子 replace。写入后立即执行该文件的 before/after Java IR 比较：

- 编译失败：恢复该文件原内容，记录 `compile_failure`，批次失败。
- IR 不相等：恢复该文件原内容，记录首个差异路径，批次失败。
- 任一文件失败后，不得继续把剩余候选当成成功批次；已经写入的本批次文件必须整体回滚，或明确实现为
  预验证全部成功后统一原子提交。

Python 自己重新实现 IR 只能作为交叉检查，不能作为等价证明。

### 7.4 最终 production compiler 拒绝旧格式时的 before 编译

永久 verifier 不得依赖 production compiler 的 legacy 分支。推荐流程：

```text
before legacy XML
  -> migration-only node normalizer in memory
  -> compact-node XML with explicit transitions unchanged
  -> final QuestDefinitionXmlCompiler

after migrated XML
  -> final QuestDefinitionXmlCompiler

compare full QuestDefinition equality
```

这样能用最终严格 compiler 证明旧节点投影和新节点投影相等，同时允许最终生产 parser 拒绝 `<project>` 和
`<vars>`。normalizer 必须只做第 4 节规定的一对一结构改写，不能识别或重写领域块。

为性能考虑，Java verifier 应支持单 JVM 批量输入；禁止为 6246 个文件分别启动 Maven。Maven/Javac 只能由
coordinator 串行执行。

## 8. 普通 transition 到领域块的严格匹配

完成所有 node 转换后，按以下优先级扫描连续普通 transition：

1. `npc-start`
2. `npc-complete`
3. `npc-item-report`
4. `npc-report`
5. `counter-grid`
6. `counter`
7. `kill-chain`
8. `kill-routes`
9. `npc-dialog`

优先级是合同。大模式必须先于可能吞掉其局部 transition 的小模式。

匹配器应以现有 `QuestXmlBlockExpander` 的精确展开结果为模板，而不是只比较标签或事件种类。只有同时满足
以下条件才可替换：

- 候选 transition 在原文中完整且连续。
- 数量与目标领域块展开数量完全相等。
- source/target、event、conditions、actions、after-commit 和 priority 全部相等。
- XML 中的 transition 顺序与领域块展开顺序完全相等。
- 替换 span 不跨越注释、已有领域块或其他元素。
- 替换后完整文件通过 Java IR equality。

任何特殊页面、额外 condition、额外 action、不同同步模式、显式 priority、非标准 finish、阈值差异、分支、
恢复路径、不连续路径或未知结构都保留为普通 transition，并记录为 `no_strict_match`。不以迁移率作为强制
改写理由。

### 8.1 `npc-dialog` matcher 的额外条件

只有一个完整 NPC x dialog 的矩形序列可压缩为 `npc-dialog`：

- 至少两个不同 NPC。
- 所有 transition 的 source 与 target 是同一个 label。
- event 只能是 `TalkToNpc`。
- conditions/actions 为空，priority 为 null。
- after-commit 恰好一个，且所有 transition 响应完全相同，并属于允许的三种类型。
- 每个 NPC 拥有完全相同、顺序完全相同的 dialog ID 列表。
- transition 顺序为 NPC-major、dialog-minor。
- NPC ID 和 dialog ID 都不能重复。

1131 中 `started` 上 NPC `203097`、`799093` 的 dialog `31`、响应页面 `1352` 是正向代表。1131 中
`shugo` 上不同 NPC 打开不同页面的两条 transition 不能合并。

## 9. 迁移报告

每次扫描和 apply 都写入：

```text
target/quest-xml-migration/report.json
```

报告至少包含：

```json
{
  "mode": "scan",
  "started_at": "ISO-8601",
  "repository": "/absolute/path",
  "git_head": "sha",
  "scan_count": 6246,
  "changed_file_count": 0,
  "node_migration_count": 0,
  "domain_block_counts": {
    "npc-start": 0,
    "npc-complete": 0,
    "npc-item-report": 0,
    "npc-report": 0,
    "counter-grid": 0,
    "counter": 0,
    "kill-chain": 0,
    "kill-routes": 0,
    "npc-dialog": 0
  },
  "no_strict_match": [],
  "unsupported_inline_xml": [],
  "dirty": [],
  "parse_failures": [],
  "compile_failures": [],
  "ir_mismatches": [],
  "remaining_legacy_wrappers": []
}
```

数组项要含文件相对路径、行号或 span、分类和简短原因。IR mismatch 至少给出第一个不同字段，例如
`transitions[17].afterCommit[1]`，不能只写 `not equal`。

完成门禁：

- `dirty` 为空
- `parse_failures` 为空
- `compile_failures` 为空
- `ir_mismatches` 为空
- `remaining_legacy_wrappers` 为空
- 所有实际改写文件都有 before/after Java IR 等价证据

`no_strict_match` 可以非空，因为普通 transition 继续是合法作者语法。它表示保守保留，不表示失败。

## 10. dirty 工作树上的安全执行流程

由于当前 checkout 已有重叠改动，Claude Code coordinator 应采用隔离流程：

1. 在原工作树记录 `git status --short`、`git diff --name-only` 和重叠文件 diff。
2. 在仓库外创建一次性 detached worktree；路径必须具体且不能是项目根或用户 home。
3. 将原工作树的 tracked/untracked 用户改动复制到隔离 worktree，建立“包含用户改动的基线”。
4. 仅在隔离 worktree 内创建临时本地 baseline commit，使迁移工具能够从 clean 状态执行。该 commit 不得推送，
   也不得改变原分支历史。
5. 在隔离 worktree 实现 compiler、XSD、测试和迁移器，串行测试。
6. 从 baseline commit 计算“仅本任务新增变化”的 patch；其中不能重新携带用户原有 diff。
7. 把该 task-only patch 应用回原工作树。若任何 hunk 与用户新改动冲突，立即停止并报告，不能覆盖。
8. 在原工作树重新运行结构扫描、focused tests 和最终 serial verify。
9. 保留 `target/quest-xml-migration/report.json` 作为审查证据；`target` 内容不提交。

若 coordinator 无法可靠建立 task-only delta，应停在扫描和实现阶段并报告阻塞，不得用整文件复制解决冲突。

## 11. Claude Code 最多 50 子代理协作协议

### 11.1 所有权

Claude Code 主进程是唯一 coordinator，也是唯一允许写共享工作树、运行 Maven/Javac、执行迁移 apply、解决
冲突和形成最终交付的参与者。

子代理全部只读：

- 不修改文件。
- 不运行 Maven、Javac 或迁移 apply。
- 不创建 commit、stash、worktree 或锁文件。
- 不声称 Python 报告等同于 Java IR 证明。
- 返回文件路径、行号、候选模式、风险和建议测试给 coordinator。

每个子代理必须收到本文档的完整共同合同，再附一个互斥的具体分片。不得只给一句模糊任务，也不得让两个
写代理竞争同一文件。

### 11.2 建议分配，合计不超过 50

第一阶段，8 个只读架构代理：

1. XSD 和 node parser 合同。
2. `npc-dialog` lowering 和非法输入。
3. 普通 dialog ID 范围语义复用。
4. 现有八种领域块的精确展开合同。
5. Java IR verifier 设计。
6. XML span 保真改写设计。
7. Java 内联 XML 盘点。
8. dirty worktree 和隔离交付风险。

第二阶段，在 coordinator 完成 dry-run 后，最多 32 个只读数据代理：

- 按确定性的 Quest ID 范围拆分生产 XML。
- 每个 Quest ID 只归属于一个代理。
- 审查 dry-run 报告中的候选、`no_strict_match`、特殊页面、priority、同步模式和顺序。
- 只返回审查结果，不手工改 XML。

第三阶段，最多 10 个只读验证代理：

- 按互斥路径检查最终 diff。
- 分别审查 node、九种 block、测试资源、Java text blocks、报告、1131、残留 wrapper、非任务行为变化、
  测试覆盖和工作树边界。

不要求为了达到 50 而创建无工作量代理。代理数量由实际分片决定，但任何时刻和总计划都不得超过用户给出的
50 上限。Maven 仍由 coordinator 串行运行。

## 12. 实施阶段和门禁

### 阶段 A：基线冻结

- 完整读取本文档和当前 AGENTS/RTK 指令。
- 重新采样 Git 状态、6246 数量、wrapper 残留和测试内联 XML。
- 保存初始 dry-run 报告。
- 明确当前用户改动与任务改动边界。

门禁：没有路径所有权歧义，隔离方案可执行。

### 阶段 B：先加测试，再改 compiler

- 给新 node 语法添加无变量、单变量、多变量正向测试。
- 添加旧 wrapper、混合格式、缺 status、未知字段、越界和重复 projection 负向测试。
- 给 `npc-dialog` 添加完整正向和负向矩阵。
- 添加 blocks 与 ordinary transitions 混排顺序测试。

门禁：新测试在旧实现上按预期失败，且没有改业务断言。

### 阶段 C：实现最终 XML 前端

- 更新 XSD。
- 更新 `parseNodes`。
- 实现 `npc-dialog` lowering。
- 保持普通 transition 和八种 block 行为不变。

门禁：node/compiler/domain focused tests 通过；旧格式拒绝测试通过。

### 阶段 D：实现迁移器和 verifier

- 完成只扫描模式、报告、dirty preflight、备份、span rewrite、rollback 和 Java IR compare。
- 为工具增加 Python 单元测试或等价的自动测试，覆盖格式保真、注释、CRLF、失败回滚和 dirty 拒绝。
- 默认 scan 不写输入文件。

门禁：在小型临时 fixture 上证明 scan 无写入、apply 等价、失败可恢复。

### 阶段 E：全量迁移

- 在 clean 隔离基线上运行 `--apply`。
- 迁移生产 XML、测试 XML 和所有 Java 内联测试 XML。
- 对每个改写文件完成 Java IR 比较。
- 生成最终 report。

门禁：dirty、parse failure、compile failure、IR mismatch 和 legacy wrapper 均为零。

### 阶段 F：测试与人工复核

- 运行第 13 节测试。
- 人工检查 1131。
- 子代理只读审查最终 diff 和 report。
- 将 task-only delta 安全带回原工作树并重新验证。

门禁：没有无法解释的业务 diff，没有后台 Maven/Javac 进程，串行 clean verify 成功。

## 13. 自动测试和命令

所有 shell 命令使用 `rtk`。不要并行启动 Maven，也不要让子代理运行 Maven。

### 13.1 结构门禁

```bash
rtk rg -n '<project(?:[ >])|<vars(?:[ >])' \
  src/main/resources/aion/data/static_data/quest_definition \
  src/test/java src/test/resources
```

预期：零匹配。

生产定义数量：

```bash
rtk rg -l '<quest-definition\b' \
  src/main/resources/aion/data/static_data/quest_definition/quests -g '*.xml' | rtk wc -l
```

预期：仍为 6246；若执行期间仓库合法新增或删除定义，必须解释新基线，不能静默更新数字。

### 13.2 focused tests

至少串行运行：

```bash
rtk mvn -Dtest=QuestXmlDomainBlocksTest,QuestDefinitionCompilerTest,QuestDefinitionCatalogManifestTest,QuestDialog31RegressionTest,QuestStepDialogResponseRegressionTest,EarlyElyosQuestRegressionTest test
```

迁移工具自己的测试也必须加入 focused 命令。若测试类名不同，在交付中写出实际命令。

### 13.3 全目录和生产 catalog

- 用 `QuestDefinitionDirectoryLoader` 或对应测试编译全部 6246 个 quest 文件，包括 metadata-only 文件。
- 用 `QuestDefinitionCatalogManifestTest` 验证正式生产 catalog owner。
- 两项都要成功，不能只运行其一。

### 13.4 最终全量验证

确认没有 Maven/Javac writer 后，串行运行：

```bash
rtk mvn clean verify
```

不能以非 clean、并行或被其他构建污染的 `target/classes` 结果作为最终证明。

## 14. 必须覆盖的测试矩阵

### 14.1 node

- 无 var：label/status 完整保留。
- 单 var：name/value 完整保留。
- 多 var：XML 顺序、字段和值完整保留。
- 旧 `<project>` 拒绝。
- 旧 `<vars>` 拒绝。
- 新旧混合拒绝。
- 缺 label、缺 status、缺 var name/value 拒绝。
- 未知 progress field 保持 `UNKNOWN_PROGRESS_FIELD`。
- 超出 bit-field 范围保持现有失败。
- 两个 node 投影到同一 status + packed vars 保持 `DUPLICATE_NODE_PROJECTION`。
- 节点顺序不变，首节点相关可达性行为不变。

### 14.2 `npc-dialog`

- `show-quest-dialog`、`show-quest-selection-dialog`、`close-dialog` 三种响应。
- 至少两个 NPC。
- 多 dialog ID，包括空格列表、逗号和合法范围。
- NPC-major、dialog-minor 展开顺序。
- 与普通 transition 和其他 block 混排时保持文档顺序。
- source 不存在。
- 缺 source、缺 npc-ids、缺 dialog-ids。
- 只有一个 NPC。
- NPC 重复、0、负数、非整数。
- 空 dialog 集合、重复 ID、反向范围、过长范围、非法 token。
- 无响应、多个响应、非法响应。
- 非法 target、priority、conditions、actions、after-commit wrapper 或未知属性。

### 14.3 migration tool

- 默认 scan 输入文件 hash 完全不变。
- `--apply` 对 clean fixture 正确改写。
- 任一目标 dirty 时全批次零写入。
- unrelated dirty 文件不阻止 clean 目标批次，也不被修改。
- XML declaration、注释、属性顺序、CRLF/LF 和尾部换行保持。
- 只匹配连续完整 transition。
- 大模式优先，不被小模式拆分。
- 不严格匹配进入 `no_strict_match`。
- compile failure 和 IR mismatch 回滚。
- before 备份路径稳定。
- Java text block 缩进和源码可编译。
- report 字段完整且计数与文件明细一致。

## 15. 1131 人工验收

迁移后打开 `quests/1131.xml` 并结合 `EarlyElyosQuestRegressionTest` 检查：

1. `unaccepted` 仍投影 `NONE/var0=0`。
2. 接取后 `started` 仍投影 `START/var0=0`。
3. 交付后 `shugo` 仍投影 `START/var0=1`。
4. 上交后 `reward` 仍投影 `REWARD/var0=1`。
5. 完成后 `complete` 仍投影 `COMPLETE/var0=0`。
6. `started` 上 NPC 203097 和 799093 的 dialog 31 可合并为一个 `npc-dialog`，页面仍为 1352。
7. `started -> shugo` 的 dialog 10000、物品发放/移除、PACKET_ONLY 和 close 顺序不变。
8. `shugo` 上 NPC 799093 的页面 1353 与 NPC 203101 的页面 2375 不得错误合并。
9. `shugo -> reward` 的物品条件、移除动作、LEVEL_AND_VISIBILITY_REFRESH 和页面 5 顺序不变。
10. completion block 和奖励索引不变。

## 16. 完成交付物

Claude Code 完成后必须提供：

- 最终 `git status --short`。
- task-only `git diff --stat` 和完整 diff 的审查说明。
- 变更文件列表，区分原有用户改动与本任务改动。
- 最终 `target/quest-xml-migration/report.json` 摘要。
- 扫描数、node 迁移数、九种领域块迁移数、`no_strict_match` 数。
- dirty、parse failure、compile failure、IR mismatch、legacy wrapper 的零值证据。
- 全部实际测试命令、退出结果和测试统计。
- 6246 全目录编译证据。
- `rtk mvn clean verify` 结果。
- 1131 人工验收结果。
- 子代理分工、每个分片结论和未解决风险。
- 明确说明没有 commit/push；除非用户在后续另行要求提交。

不得只给“实现完成”或“测试通过”的摘要。后续 Codex 会根据以上证据重新检查 diff、报告、重点 XML 和
focused/full tests。

## 17. 必须停止并报告的条件

遇到以下任一情况时，停止写入并向用户报告，不得自行放宽合同：

- 无法区分用户现有改动与本任务改动。
- task-only patch 与用户新改动冲突。
- 任一文件不能证明 Java IR 相等。
- 为提高迁移率必须改变 transition 顺序或语义。
- 最终 production parser 仍需 legacy 分支才能工作。
- 迁移工具必须格式化整文件或破坏注释才能完成。
- 全量编译数量与基线不同且无法解释。
- serial clean verify 重复失败且原因不属于本任务可安全修复范围。
- 报告存在 dirty、compile failure 或 IR mismatch。

保守保留普通 transition 是允许的；猜测性压缩不是。
