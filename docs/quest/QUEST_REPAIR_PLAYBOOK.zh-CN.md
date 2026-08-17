# 任务排查与修复 Playbook

本文档面向参与 AionEmu 任务修复的 coding agent 和开发者。目标是把“玩家描述的任务不能做”转换为可验证的状态、协议、客户端和运行时证据，再用最小改动修复，并留下可以阻止回归的测试。

适用仓库：当前 checkout 根目录，可用 `git rev-parse --show-toplevel` 获取。

相关文档：

- [任务 XML 编写指南](WRITING_GUIDE.zh-CN.md)：XML 状态图、领域积木和字段顺序。
- [客户端任务对话映射说明](client-dialog-mapping/README.zh-CN.md)：客户端 HTML、页面、动作和旧模板合同。
- [任务 XML 紧凑语法迁移规范](../QUEST_XML_COMPACT_MIGRATION_PLAN.zh-CN.md)：迁移时的 IR 等价、脏工作树和全量门禁。

## 1. Agent 合同

开始前必须遵守以下边界：

1. 生产任务的唯一执行 owner 是 `src/main/resources/aion/data/static_data/quest_definition/quests/<id>.xml` 加上 `quest_definition_catalog.xml` 中的 `EXECUTABLE` 注册。Java DSL 只用于测试和工具。
2. 旧 handler、`quest_data.xml`、客户端 5.8 数据和真实运行日志是行为证据，不是可以随手复制的生产 owner。缺少权威字段时，先标记为 `EVIDENCE_REQUIRED`，不要从候选 XML 或一次行为反推。
3. 事件、条件、事务动作和 `after-commit` 副作用职责分离。状态推进正确但页面、关闭、生成 NPC 或跟随动作缺失，仍然是未完成的修复。
4. 保留用户已有的脏工作区改动。禁止 `git reset --hard`、`git checkout --`、`git restore`、覆盖整文件或无范围的批量替换。
5. 项目命令使用标准系统入口；Maven/Javac writer 串行运行。不要让多个 agent 同时执行 Maven 或清理 `target`。
6. “提交”默认是本地 commit，不是 push。只暂存本次修改的明确路径。

## 2. 运行链和故障边界

任务定义不是一组孤立的页面，而是一张状态机图：

```text
XML
  -> XSD
  -> QuestDefinitionXmlCompiler
  -> QuestXmlBlockExpander
  -> QuestDefinitionCompiler
  -> CompiledQuestDefinition
  -> QuestProductionDispatcher
  -> QuestExecutionCoordinator
  -> QuestMutationPlanner
  -> 事务提交
  -> after-commit ports（协议、页面、生成、跟随、传送、动画）
```

客户端动作进入服务端后，关键顺序是：

```text
客户端 action/dialog
  -> 事件路由和条件匹配
  -> 事务内状态/物品/奖励变更
  -> 数据库 commit
  -> after-commit 副作用
```

因此排查时要区分：

- **状态错误**：节点、`QuestStatus`、`quest_vars`、条件或事务动作错误。
- **响应错误**：状态已推进，但没有发送正确页面、选择窗口或关闭包。
- **世界副作用错误**：NPC 没生成、生成后被错误 AI 接管、跟随目标错误或移动没有启动。
- **客户端表现错误**：服务端包正确，但客户端页面、字典、数据包或设置阻止显示。
- **性能错误**：一次交互重复刷新、重复遍历依赖任务或重复发送页面，而不是单纯“客户端慢”。

## 3. 先收集可复现证据

不要只记录“点了没反应”。先填写下面的最小记录：

```text
quest:       14112
character:   race/class/level（若与条件有关）
repro:       从哪个 NPC/物品开始，依次点击了什么，哪一步失败
npc/object:  template-id、object-id、interaction object、地图/实例
action:      QUEST_SELECT / USE_OBJECT / SELECT_QUEST_REWARD / ...
page:        客户端看到的页面或期望页面
state:       失败前后的 source/target/status/var 值
restart:     下线、重登、重启服务端后是否复现
log:         同一时间窗口的 WARN/ERROR，以及发送前后的任务日志
```

还要明确以下问题：

- 是第一次点击无响应，还是第二次点击才推进？这通常指向“状态已提交但 after-commit 页面缺失”。
- 是击杀后 NPC 不存在，还是 NPC 存在但不能对话？前者检查 spawn/AI，后者检查交互对象和对话路由。
- 是同一只 NPC 被对话后跟随，还是附近另一只同模板 NPC 被生成并跟随？这决定是否需要保留 interaction object 身份。
- 下线后是状态回退，还是状态正确但恢复 NPC 缺失？两者分别检查持久化和 `EnterWorld` 副作用。
- 日志中的“未找到 NPC 生成配置”是否来自本次 `spawn-npc-at-player`，还是静态地图 spawn/传送系统？先确认调用链，不能见到 WARN 就盲目补配置。

## 4. 症状到证据的快速分流

| 玩家症状 | 第一检查点 | 常见根因 | 推荐证明 |
|---|---|---|---|
| 点击奖励/“一堆物品”第一次没有反应 | `SELECT_QUEST_REWARD` 路由的 target 和 after-commit | 进入 `REWARD` 但只同步状态，没有页面/关闭响应 | `QuestDefinitionCatalogManifestTest.rewardSelectionTransitionsRespondInTheSameInteraction` |
| 实时奖励选择物品后点击领取无响应 | 无目标 `CM_DIALOG_SELECT` 的原始 action，以及 `QuestEvent.QuestDialog` 生产索引 | 客户端发送实时奖励动作 110..124，XML 却只注册普通奖励动作 8..22 | Aion 5.8 客户端 `client-hyperlinks.csv`、旧 `finishReportedQuest` 映射和任务专用 targetless reward 测试 |
| 杀怪后任务回到前一步 | progress 位域的 persistence、`LOG_OUT`/`ENTER_WORLD` 路由、是否有 reset action | 用临时状态覆盖了持久变量，或登出边把状态写回 START | `Quest14112LogoutPersistenceTest`，检查 `PERSISTENT` 和无回退边 |
| 杀怪后目标 NPC 没生成 | `after-commit` 的 spawn 动作、模板 ID、AI 选择 | 自定义 AI 的 `handleDied`/`handleSpawned` 被 `retail_pattern` 覆盖 | `AI2EngineRetailSelectionTest`，再做实际 NPC/任务路径测试 |
| 护送 NPC 不动、跟错 NPC、离玩家很远才追 | 对话时的 interaction object、follow action、跟随距离判断 | 重新生成同模板 NPC、跟随 slot 不对应交互对象，或 follow state 使用 15m 容差 | `Quest1149ClientDialogAlignmentTest`、`FollowManagerTest` |
| 交互物/物品点击没有页面 | client action、`can-act`、`QuestItemNpcAI2` 的 fallback | 只允许 `ACTION_ITEM_USE`，但任务实际注册的是 talk/start 路由；或只尝试错误 dialog | `QuestItemNpcAI2Test`、对应任务 XML 回归测试 |
| 对话下一页明显卡顿 | 一次请求产生的状态同步、页面和依赖重评估次数 | refresh amplification、重复遍历全部任务、重复发送相同页面 | `QuestDependencyIndexTest`、`QuestExecutionCoordinatorTest`、`QuestProductionDispatcherTest`；结合包/日志计数 |
| 页面编号或按钮不对 | 客户端 HTML/CSV、旧 handler 的页面顺序 | 把不同任务的 page/action 当成通用模板 | `QuestDialogOrderAuditTest`、任务专用 alignment test |
| 任务头顶标记没有出现 | `SM_NEARBY_QUESTS` 是否包含任务，客户端设置和数据包 | 服务端已正确发送，但 `show_acquirable_normal_quest` 关闭或客户端资源异常 | 先证明服务端包，再检查客户端设置/数据包 |

## 5. 标准排查流程

### 5.1 阶段 0：建立基线

```bash
git status --short
git branch --show-current
git log -20 --oneline --decorate
git diff --stat
```

记录当前分支和 dirty 文件。若任务 XML 或相关 Java 已被用户修改，先读 diff，后续只能在其基础上工作。不要为了让工具通过而隐藏、提交或丢弃这些改动。

### 5.2 阶段 1：找到生产 owner 和实际路由

已知任务 ID、NPC 或动作时直接精确搜索。以下 shell 命令仅为示例，可替换为当前 agent 和执行环境提供的等价搜索与读取能力：

```bash
rg -n 'id="14112"|npc-id="203195"|SELECT_QUEST_REWARD' \
  src/main/resources/aion/data/static_data/quest_definition \
  src/test/java src/main/java
sed -n '1,230p' src/main/resources/aion/data/static_data/quest_definition/quests/14112.xml
rg -n '14112|203195|Poisonous_Bubblegut' src/main/java src/test/java
```

检查 `quest_definition_catalog.xml`：

- 是否只有一个同 ID 注册；
- 是否为 `EXECUTABLE`；
- resource 路径是否指向当前 XML；
- 是否仍有旧 XML/Java owner 可能抢占执行入口。

不知道代码位置时，使用当前环境可用的语义、符号或结构化搜索定位一次；得到相关目录后改用精确搜索和符号附近的局部读取。不要反复对整个仓库做宽泛搜索。

### 5.3 阶段 2：画出状态和协议合同

对失败动作至少写出一条完整合同：

```text
source node/status/vars
  + event（NPC/object/action/dialog）
  + conditions
  -> target node/status/vars
  + transaction actions
  -> after-commit 顺序（sync/page/close/spawn/follow/...）
```

重点核对：

1. `source` 是否真的是玩家失败时的状态，而不是只看了 XML 中最常见的 `started`。
2. `target` 的 status 和所有 packed variables 是否保持任务语义。
3. `priority` 是否让正确分支先匹配；多个相同 action 不能只看第一条。
4. `after-commit` 是否有且只有客户端/世界需要的副作用；顺序通常是 `sync` 后再发页面，完成时使用正确的 `COMPLETION` 同步。
5. 非 `TalkToNpc` 事件不要猜 dialog target；只有带权威 interaction object 的对话事件可以驱动对象相关副作用。

### 5.4 阶段 3：交叉验证三类权威

**当前 XML/编译 IR**

- 用 `QuestDefinitionXmlCompiler` 或生产 catalog 测试确认 XML 能编译。
- 不要只读源码文本；检查实际 `CompiledQuestDefinition` 的 transition 顺序和 after-commit。

**旧 handler/正式模板**

如果有 `origin/history` ref，可用：

```bash
git show origin/history:<path/to/legacy-handler.java>
git log --all --oneline -- <path/to/legacy-handler.java>
```

对照旧 handler 的 `setStatus`、`updateQuestStatus`、`sendQuestDialog`、spawn、follow、logout/enter-world 行为。旧 handler 是时序和副作用证据，不是让新代码重新绕过 typed dispatcher 的理由。

**客户端/零售数据**

Aion 5.8 客户端是客户端页面、动作、字典和数据包的权威来源。当前任务缺少所需客户端文件、解包产物或抓包时，明确列出缺失项并向用户请求提供，在取得证据前不要猜测。优先使用：

- `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv`：页面上的实际按钮动作；
- `quest-dialog-pages.csv`：页面存在性和页面名；
- `legacy-quest-dialog-contracts.csv`：旧正式模板的 NPC、页面和状态合同；
- `client-lifecycle-alignment.csv`、`quest-order-audit.csv`：当前 IR 与客户端路径的审计结果。

客户端页面只证明客户端可见合同，不能单独证明服务端状态和奖励副作用；服务端 IR 也不能单独证明页面按钮真的可达。

### 5.5 阶段 4：选择最小修复层

- **单个任务的页面/状态错配**：优先修 XML transition，并新增任务专用回归测试。
- **多个任务共享同一协议缺陷**：修 dispatcher/after-commit/runtime，再增加生产目录级审计测试。
- **AI 生命周期副作用被覆盖**：修 `AI2Engine.selectNpcAi` 的保留规则，只对有明确任务副作用证据的 AI 保留 fallback，并测试集合。
- **数据包/客户端字典问题**：修生成/打包流程或客户端资源；不要用服务器 XML 掩盖客户端资源错误。
- **静态数据缺失**：确认调用路径确实使用静态 spawn 后再补配置；玩家位置生成、任务 slot 生成和地图静态生成是不同机制。

### 5.6 阶段 5：修复后立即添加回归

测试至少锁定以下合同：

- source、target、status、变量值；
- event 的 NPC/object/action/dialog ID；
- conditions、priority、事务动作；
- after-commit 的完整顺序；
- 登录、登出、死亡、重复点击等边界；
- 不能只断言“最终 status 正确”，因为页面、生成和跟随可能仍然错误。

测试命名优先使用 `Quest<id>...Test` 或共享行为测试，便于以后从玩家反馈直接定位。

## 6. 高频修复配方

### 6.1 奖励选择第一次点击无响应

典型错误是：`SELECT_QUEST_REWARD` 已把 `started`/`k1` 推到 `reward`，after-commit 只有 `sync-quest-state`。客户端第一次点击后状态发生变化，但没有新的页面，所以看起来像按钮失效；第二次点击才触发 reward 自循环。

正确的进入奖励选择路径通常类似：

```xml
<transition source="started" target="reward">
  <event>
    <dialog type="TALK_TO_NPC" npc-id="203195" action="SELECT_QUEST_REWARD"/>
  </event>
  <after-commit>
    <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
    <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
  </after-commit>
</transition>
```

如果 `source=reward,target=reward` 的重复选择路由存在，通常只需按旧协议显示选择窗口，不要无证据增加额外刷新。修改后用生产目录审计确保所有“进入 `REWARD` 的 `SELECT_QUEST_REWARD`”都有页面、选择窗口或关闭响应；不能只测一个任务。

实时奖励确认是另一类协议问题。无目标奖励包会保留客户端原始 action，并由 typed owner 构造成 `QuestEvent.QuestDialog`；普通奖励槽使用 8..22，实时奖励槽使用 110..124。任务已在 `REWARD`、页面也正常显示，但 XML 只注册普通动作时，点击“领取”不会命中任何完成迁移。修复时必须根据客户端可见奖励槽注册对应的实时动作，并锁定职业条件、奖励索引、事务动作及 `after-commit` 关闭顺序；不能把所有 110..124 全局改写成 8..22。

### 6.2 击杀后、重登后任务状态回退或目标 NPC 消失

以 14112「了解污染的原因」为基准检查：

1. 击杀 210318 的 transition 应从 `started` 到 `k1`，进度位域为 `PERSISTENT`。
2. 击杀提交后生成 Kato 203195，并同步任务状态。
3. `k1` 和 `reward` 的 `ENTER_WORLD` 都要重新生成 Kato；否则下线重登后状态正确但任务无法继续。
4. 不应存在 `k1 -> started` 的登出回退边或会把持久变量清零的动作。
5. Kato 的 dialog action、首次奖励窗口和完成奖励路由必须各自验证。

日志出现“未找到 NPC 生成配置”时，先确认 XML 使用的是 `spawn-npc-at-player` 还是地图静态 spawn。对玩家位置生成的任务 NPC，不要仅凭 Teleport/静态 spawn 警告去修改地图配置。

### 6.3 自定义 AI 生成 NPC 被 retail pattern 覆盖

`AI2Engine.setupAI` 会在创建 NPC AI 时调用 `selectNpcAi`。如果 fallback AI 在 `handleDied` 或 `handleSpawned` 中生成任务目标，而同 NPC 有零售 pattern，直接切成 `retail_pattern` 可能让任务目标永远不出现。

排查步骤：

1. 找源 NPC 的 `@AIName`、自定义 AI 的生命周期方法和生成目标模板 ID。
2. 查 `npc-ai.xml`/零售 pattern 是否确实覆盖该 NPC。
3. 查 pattern 是否有等价的 spawn/变身/任务副作用；只有没有等价动作时才保留自定义 fallback。
4. 将保留名单作为明确集合和回归测试，不要按“所有自定义 AI”全局禁用 retail pattern。

当前使用 `AI2Engine.QUEST_SIDE_EFFECT_AI` 和 `AI2EngineRetailSelectionTest` 保护 38 个具有任务副作用的 AI；初始代表提交覆盖 37 个，任务 14047 又补充了战斗阶段变身 AI `betrayer_icaronix`。未来发现新任务时，必须补充“生命周期或战斗阶段副作用 + 零售 pattern 无等价动作”的证据和测试。

### 6.4 护送 NPC 跟错对象、启动不动或距离过远

护送任务至少要回答“跟随的是哪一个对象”：模板 ID 相同不等于 object ID 相同。

- 对话动作应使用权威 interaction object；1149「消失的波比」使用 `start-follow-current-target-npc npc-id="203145"`，不再重新生成另一只 Poppy。
- 开始跟随时必须立即调用 `FollowManager.startMoving`，而不是等目标先移动到阈值外。
- 普通满血跟随 NPC 的当前贴身判定在 `FollowEventHandler` 为 3 米；副本和残血 NPC 使用不同规则。修改距离前先确认实际生效的 AI、HP、实例和 3D 距离判断。
- 跟随丢失、死亡、登出时要验证删除/重生和任务状态同步，避免只修追击距离。

回归测试至少断言：交互 NPC ID、after-commit 顺序、没有重复 spawn、follow target 绑定，以及 2 米保持/4 米触发追击之类的边界。

### 6.5 交互物或“一堆物品”点击无反应

先区分三层：客户端发的是 `USE_OBJECT` 还是 `START_DIALOG`；NPC 是否通过 `ACTION_ITEM_USE`；任务 XML 是否注册了 `onTalkEvent`/`onQuestStart`。

`QuestItemNpcAI2` 的安全策略是：

1. `ACTION_ITEM_USE` 允许时继续；
2. 即使 action gate 不允许，只要任务注册了 talk/start 路由也允许进入；
3. 使用完成时先尝试 `USE_OBJECT`，再回退 `START_DIALOG`；
4. 两者都没有成功时才发送通用选择/关闭响应。

1156「消失的村落印章」还要求：未接取状态不能使用 700003，接取后才接受 `USE_OBJECT -> SELECT2 -> SETPRO1`，完成 NPC 为 798003。不要把 `QUEST_SELECT` 当成 object action，也不要只改 NPC 模板而不补任务路由。

### 6.6 对话页面卡顿或重复点击

先统计一次交互的事件、事务和 after-commit 数量，重点搜索：

- 同一 transition 同时发送多次状态刷新；
- 一个提交触发全量任务依赖重评估；
- 页面显示后又被另一个通用 handler 重发或关闭；
- 重试/重复点击没有幂等条件。

`a5e7fba5a` 的性能修复把状态变化后的自动接取评估改为 `QuestDependencyIndex` 反向索引，只重评估显式依赖的 owner。修复性能时要证明“少做了不必要的工作”，不能把必要的 `VISIBILITY_REFRESH` 或页面包删除来掩盖问题。

### 6.7 页面、任务标记或文本只在客户端异常

当服务端已经证明发送了正确 `SM_QUEST_ACTION`、dialog page 或 `SM_NEARBY_QUESTS`，再检查客户端：

- `show_acquirable_normal_quest` 是否开启；
- `5.8客户端` 中对应 page/action 是否存在；
- data pak 编码是否保持原始格式；
- 本地化字典的 display-name ID 是否匹配。

客户端修复和服务端任务修复要分开提交，避免用 XML page 改动掩盖数据包编码或客户端设置问题。

## 7. 验证门禁

以下 Maven、Javac、测试和脚本命令仅在用户明确授权后执行；未获授权时只记录待执行验收项。不会触发构建的 `git diff --check`、状态和 diff 检查可直接执行。

### 7.1 先跑 focused tests

根据改动范围选择测试；当前任务相关修复可从以下命令开始：

```bash
mvn -q -Dtest=AI2EngineRetailSelectionTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,Quest14112LogoutPersistenceTest,Quest1149ClientDialogAlignmentTest,QuestItemNpcAI2Test,FollowManagerTest test
```

涉及早期 Elyos 交互、1156/1158、页面映射时加入：

```bash
mvn -q -Dtest=EarlyElyosQuestRegressionTest,Quest1163ClientDialogAlignmentTest,QuestDialogOrderAuditTest test
```

不要把 targeted pass 当成全目录证明。生产 catalog 必须实际编译；上面的 `QuestDefinitionCatalogManifestTest` 会覆盖目录级 owner 和奖励选择审计。

### 7.2 结构、目录和 diff 门禁

```bash
git diff --check
mvn -q -Dtest=QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test
git status --short
git diff --stat
```

预期至少包含：

- `PRODUCTION_COMPILE_FAILURES=0`；
- `PRODUCTION_WHITELIST_VIOLATIONS=0`；
- 没有 XML 编译异常、路径重复或 ambiguous transition；
- 没有将无关 dirty 文件带入 diff。

如果需要最终全量证明，确认无 Maven/Javac 后串行执行：

```bash
mvn clean verify
```

增量构建出现匿名类、内部类或 `NoClassDefFoundError` 时，先确认没有并发 writer，再做一次串行 clean verify；不要用被并发构建污染的 `target/classes` 启动服务器。

### 7.3 客户端映射和顺序审计

只有修改页面/动作合同或需要重新生成报告时才执行：

```bash
python3 scripts/quest/generate_client_dialog_mapping.py --check
python3 scripts/quest/extract_legacy_quest_dialog_contracts.py --check
python3 scripts/quest/align_client_quest_dialog_lifecycle.py --check
python3 scripts/quest/generate_quest_dialog_enums.py --check
```

顺序审计应在测试编译完成后执行，命令和字段说明见 `client-dialog-mapping/README.zh-CN.md`。`EVIDENCE_REQUIRED` 不是“已修复”，不能为了清零报告而猜测 page/action。

## 8. 近期修复提交索引

下面的提交不是互相独立的技巧，而是一条从客户端证据到状态、协议、AI 和性能的排查链。新 agent 遇到相似症状时，先找对应案例，再读取完整 diff 和测试。

本节只按可复用的“问题模式”记录一个代表任务，目的是为后续修复提供证据和实现参考，不维护任务清单、验收名单或覆盖数量。后续任务与已有案例的症状、根因、修复层和修复合同相同时，不追加任务 ID、不修改案例正文，也不新增重复案例；问题或修复模式实质不同时才建立新案例。

| 提交 | 案例 | 可复用结论 |
|---|---|---|
| `8b058d4b4` | 14047 两段飞行传送、不可达副本恢复、伊卡罗尼斯变身和完整客户端链 | 状态必须先提交并同步再传送；飞行专属地点必须覆盖崩溃、断线和实例重建后的可重入恢复；血量阈值变身必须以幂等死亡路径覆盖直接秒杀；同名 NPC 的 GM 寻找传送只能按任务阶段限域 |
| `4a23cf0a0` | 13830 实时奖励选择后点击领取无响应 | 无目标实时奖励使用 110..124 独立动作空间；任务 XML 必须注册实际可见槽位并保留完整完成合同 |
| `906c08e92` | 24 个奖励选择路由首次点击无响应；37 个任务副作用 AI 被 retail pattern 覆盖 | 用生产目录审计捕获 `SELECT_QUEST_REWARD -> REWARD` 无响应；有生命周期任务副作用的 AI 必须有证据化 fallback 集合 |
| `7a6ad8eca` | 14112 击杀剧毒斯拉希后生成 Kato、重登恢复、首次奖励对话 | 任务 NPC 生成、登录恢复、页面响应和旧 AI 清理必须作为同一任务合同验证 |
| `c25db02d5` | 14112 下线后击杀进度回退 | 持久位域不能被登出/恢复流程写回 START；用专用测试锁定 logout/enter-world |
| `598deb98f` | 1163 对话页面和状态时序对齐 | 页面 ID、动作顺序和状态迁移要结合客户端/旧 handler 验证，不能套通用 page |
| `56009f7f5` | 1149 跟随与玩家对话的 Poppy，而非另一只同模板 NPC | 保留 interaction object 身份，使用 `start-follow-current-target-npc`，不要重复 spawn |
| `de7e6ebe1` | 护送开始后 follower 不立即移动 | 进入 FOLLOWING 状态时立即启动移动，并用 FollowManager 测试证明 |
| `138e5c57e` | 护送 NPC 超过 15 米才追、追到 15 米停 | 跟随容差属于 AI 状态判定；当前普通满血贴身距离为 3 米，必须测试边界而不是只改一个常量 |
| `c02c8722e` | 1156 消失的村落印章 object flow | `USE_OBJECT`、`can-act`、中间变量和完成 NPC 必须按客户端动作链分段验证 |
| `0786d4126` | 1158 村落印章对象交互 | object action 与 `QUEST_SELECT` 是两个 ID 空间，接取前不能开放 object route |
| `9abdf9433` | 交互物 dialog fallback | action gate 失败不代表没有任务 talk/start 路由；按 `USE_OBJECT -> START_DIALOG` 顺序尝试 |
| `5c7a2eb68` | 1157 Mimiti 到达目标后才继续 | 护送/诱导要用周期检查任务和 `NpcReachTarget`，不能在攻击事件里提前播放下一段电影 |
| `a5e7fba5a` | 对话刷新放大导致卡顿 | 用依赖反向索引限制重评估；不要删除必要的状态可见性刷新 |
| `1f4139b56` | 早期 Elyos 多个交互物任务 | 多任务共享协议缺陷应在 runtime/回归测试层修复，并保留各任务的页面/动作差异 |
| `15a20225c` | 多 NPC 顺序报告流 | 先画完整 state/var 时序，再实现显式 route；不要将 report self-loop 和完成路由合并 |
| `e5a25fd9b` | Belbua 酒桶交互 | 从客户端 object action 和接取状态证明 route，避免未接取时误触发任务副作用 |
| `cc7aabea5` | 9550 装备事件物品后仍无法接取任务 | 元数据 `equipped` 起始条件必须下沉为正式 `EquippedItem` 条件并使用已捕获装备事实求值；装备事实未知时必须 fail closed |

### 8.1 升级自动登记弹出不存在的任务页

- 代表任务：38001「Radiant Ops Recruitment」。
- 玩家症状：升级自动登记任务时客户端弹出任务 HTML 的 `HtmlPageId 4 / load fail`。
- 根因：升级入口错误发送 `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`；NPC `START_DIALOG(31)` 又错误发送 Aion 5.8 客户端不存在的 `SELECT2(1352)`。旧 handler 的升级入口只启动任务并刷新状态，NPC 对话页为 `DEFAULT_SUCCESS(10002)`。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/38001.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest38001LevelUpDialogTest.java`。
- 验证命令和结果：`rtk mvn -q -Dtest=Quest38001LevelUpDialogTest,Quest38002LevelUpDialogTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestDialogOrderAuditTest test` 通过；生产 catalog 6200 条编译成功，失败 0，白名单违规 0；顺序审计显示 38001 的 31 -> 10002 为 `PAGE_ACTION_MATCHED`，奖励页 5 为 `TERMINAL_PAGE_REACHED`；玩家实测升级登记不再弹出加载失败页。
- 复用边界：仅适用于升级入口不应显示任务页，且 NPC `START_DIALOG(31)` 应显示 `DEFAULT_SUCCESS(10002)` 的同型任务；页面、状态或副作用合同不同的任务必须重新取证。
- commit：`d3b28d2af3a7a3085da461d96bb9dfe6118d4905`。

### 8.2 升级自动登记与双 NPC 阶段对话链错配

- 代表任务：1920「Testing Your Mettle」。
- 玩家症状：升级自动登记任务时客户端弹出任务 HTML 的 `HtmlPageId 4 / load fail`；修复升级提示后，还必须保证第一个 NPC 的 `1011 -> 1012 -> 1013 -> 10000`、第二个 NPC 的 `1352 -> 1353 -> 10255` 和最终领奖页链可达。
- 根因：升级入口错误发送 `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`；原 XML 将两个 NPC 的多阶段客户端动作压缩成通用 `FINISH_DIALOG`、`SELECT_QUEST` 和错误的奖励入口，丢失了 `var0=1` 中间状态以及客户端页面/动作顺序。旧 handler 与 Aion 5.8 客户端页面证据共同证明：第一个 NPC 完成第一段后关闭对话，第二个 NPC 才能进入成功状态，最终只由第一个 NPC 领奖。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/1920.xml`、`src/main/resources/aion/data/static_data/quest_definition/quests/2945.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1920And2945ClientDialogAlignmentTest.java`。
- 验证命令和结果：`rtk mvn -q -Dtest=Quest1920And2945ClientDialogAlignmentTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestDialogOrderAuditTest test` 通过；生产 catalog 6200 条编译成功，失败 0，白名单违规 0；两个 XML 均通过 XSD；客户端实测升级登记及双 NPC 对话流程通过。
- 复用边界：仅适用于升级入口不应显示 page 4，且任务存在由客户端页面动作驱动的双 NPC 或多阶段状态链的同型任务；单 NPC `DEFAULT_SUCCESS(10002)` 合同复用 8.1，页面、状态或奖励归属不同的任务必须重新取证。
- commit：`76b0894`。

### 8.3 实时奖励确认使用独立动作导致领取无响应

- 代表任务：13830「Stigma 101」。同批修复的 13831..13834 共享同一问题模式，不重复建案例。
- 玩家症状：任务进入实时奖励界面并可选择职业奖励，但点击“领取”没有反应，任务不完成、奖励不到背包、界面也不关闭。
- 根因：Aion 5.8 客户端对第一个普通奖励槽发送 `HACTION_SELECTED_QUEST_REWARD1(8)`，对第一个实时奖励槽发送 `HACTION_SELECTED_QUEST_AUTO_REWARD1(110)`。无目标 `CM_DIALOG_SELECT` 会把原始 action 交给 typed dispatcher，后者按 `QuestEvent.QuestDialog(110)` 查询生产索引；原 XML 只有普通奖励动作 8 的完成路由，因此实时奖励确认没有候选迁移。旧 `finishReportedQuest` 将 110..124 映射到普通奖励槽 8..22，且正式任务数据将这五个任务标记为 `can_report=true`，共同证明两个动作空间应落到等价的奖励完成合同。
- 修复层：任务 XML + 由客户端字典和活动 XML 引用生成的 typed dialog action 枚举。五个任务的 11 个互斥职业分支同时注册普通动作 8 和实际可见的实时动作 110；事务内发放职业物品与经验、回收工作物品并完成任务，提交后按 `refresh-player-stats -> COMPLETION sync -> close-dialog` 执行。不在共享 runtime 中全局重写动作。
- 修改文件：`src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogAction.java`、`src/main/resources/aion/data/static_data/quest_definition/quests/13830.xml`、`13831.xml`、`13832.xml`、`13833.xml`、`13834.xml`，以及 `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest13830To13834TargetlessRewardTest.java`。
- 验证命令和结果：`rtk mvn -Dtest=Quest13830To13834TargetlessRewardTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test` 通过，共 8 个测试，失败 0、错误 0、跳过 0；五个 XML 均通过 XSD；`rtk python3 scripts/quest/generate_quest_dialog_enums.py --check` 返回 `changed=0`；Aion 5.8 客户端实测实时奖励可领取并正常完成任务。
- 复用边界：仅适用于权威数据允许实时报告、无目标奖励包确实发送 110..124，且普通与实时槽位应共享奖励完成语义的任务。必须按客户端实际可见槽位逐一映射：单一职业奖励通常只需 110；多槽奖励要分别证明 111..124 与奖励索引。动作 108、NPC 目标领奖、不同奖励索引、额外页面或副作用合同必须单独取证，不能套用本案例或做全局 remap。
- commit：`4a23cf0a0f531182e195bfa0f662513da50d170a`。

### 8.4 两段飞行传送、不可达副本恢复与血量阈值变身兜底

- 代表任务：14047「Chaining Memories」。
- 玩家症状：佩托 802052 的电影 421 会重复播放，任务页面不能继续；同一位置同时出现任务佩托 802052 和普通佩托 204653。任务先通过飞行 71001 到达第一处玩家无法自行返回的副本区域，再通过飞行 72001 进入下一处区域；如果客户端崩溃、断线，或服务端重启导致副本实例重建，持久化的 `s4/s5` 会把玩家留在无法重新执行任务动作的位置。GM 点击“寻找”还会因同名模板传送到普通阿凯斯泰斯 204652，而不是任务 NPC 802051。最后战斗中入口形态 233877 不会可靠生成任务监听的最终形态 214599，直接秒杀还会跳过 75% 血量检查；即使击杀推进，278500 奖励对话也会因服务端发送不存在的 `HtmlPageId 10002` 显示 load fail。
- 根因：电影 421 后缺少客户端实际存在的 `SELECT5_1(2376)` 页面，且把 `SETPRO10/SETPRO11(10009/10010)` action ID 当成页面 ID；副本静态数据重复生成普通佩托。原迁移先执行飞行副作用、后同步已提交状态，传送过程发生断线时客户端和任务进度可能不同步。`s4(var0=4)` 是飞行 71001 后的阶段，`s5(var0=5)` 是飞行 72001 后的阶段，两者都只能由前置飞行进入，却没有 `ENTER_WORLD` 恢复边；单纯回到上一个 `s4` 仍然无法从普通世界重新到达 802052。Aion 5.8 客户端寻找链接提交同名普通模板 204652，服务端没有结合任务阶段解析为 802051。副本静态出生 233877，而旧 handler 和任务合同只监听 214599；零售空 pattern 又绕过 `betrayer_icaronix` 脚本 AI，原阈值逻辑也没有死亡兜底。奖励预览最后把 `DEFAULT_SUCCESS(10002)` 当作 Q14047 页面发送，而旧 handler 的 `sendQuestEndDialog` 合同是奖励窗口 page 5。
- 修复层：任务 XML 将两段飞行都固定为 `commit -> PACKET_ONLY sync -> close-dialog -> flight-teleport`，分别使用 71001 和 72001；电影 421 后返回 2376 页面，错误阶段点击明确关闭。`s4/s5` 在 `ENTER_WORLD` 统一回退到 `s3(var0=3)`，让玩家重新与 802051 对话并再次触发飞行 71001；不在 `LOG_OUT` 回退，避免正常登出和重登各执行一次，也不回退已经可继续的 `s6`、`REWARD` 或 `COMPLETE`。GM 寻找只在 Q14047 `START + var0=3/6` 时把 204652 限域解析为 802051。副本删除重复 204653，保留静态入口形态 233877；AI 选择保护 `betrayer_icaronix`，并用共享 `AtomicBoolean` 让 75% 阈值和 `handleDied()` 最多生成一次 214599。最终击杀只监听 214599，提交后按 `PACKET_ONLY sync -> movie 422` 推进；278500 的 `USE_OBJECT` 显示 `SHOW_SELECT_QUEST_REWARD_WINDOW1`。
- 传送合同：`s3 + 802051 + SETPRO10 -> s4` 必须先同步 `var0=4` 再关闭窗口并执行 71001；`s4 + 802052 + SETPRO11 -> s5` 必须先同步 `var0=5` 再关闭窗口并执行 72001。传送是 commit 后副作用，事务失败时不得启动飞行，客户端也不能在旧任务状态下进入新区域。
- 副本崩溃回退合同：无论玩家在 71001 后尚未完成 802052 对话，还是在 72001 后尚未击杀 214599，只要重新进入世界时仍为 `s4/s5`，都回到 `s3`。恢复目标不是机械地减一阶段，而是回到普通世界中仍可交互、且能重建整段副本路径的最近节点；该合同同时覆盖客户端崩溃重连、网络断线重登、服务端重启和副本实例丢失后的重新进入。
- GM 寻找传送合同：客户端同名链接请求 204652 时，只有 GM 且 Q14047 为 `START + var0=3` 或 `START + var0=6` 才解析到任务 NPC 802051 并直接传送；其他任务、其他状态、其他阶段、空任务状态以及已经请求 802051 的情况均保持原 ID，不能全局改写同名 NPC。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/14047.xml`、`src/main/resources/aion/data/static_data/spawns/Instances/310100000_Azoturan_Fortress.xml`、`src/main/java/com/aionemu/gameserver/ai/instance/azoturanFortress/Betrayer_IcaronixAI2.java`、`src/main/java/com/aionemu/gameserver/ai2/AI2Engine.java`、`src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_OBJECT_SEARCH.java`，以及 `Betrayer_IcaronixAI2Test`、`AI2EngineRetailSelectionTest`、`CMObjectSearchTest`、`Quest14047ClientDialogAlignmentTest`。
- 验证命令和结果：索引快照运行 `rtk mvn -q -Dtest=Quest14047ClientDialogAlignmentTest,Betrayer_IcaronixAI2Test,AI2EngineRetailSelectionTest,CMObjectSearchTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test` 通过；生产 catalog 6200 条编译成功，失败 0，白名单违规 0。全量 E2E 生成 396,797 条 transition、390,082 条 PASS，Q14047 为 58/58 PASS，`PAGE_NOT_IN_CLIENT`、`INVALID_PACKET_ORDER`、`STATE_CHANGED_WITHOUT_RESPONSE`、`AFTER_COMMIT_FAILURE`、`RUNTIME_REQUIRED`、`TRANSACTION_FAILURE` 均为 0。用户使用重新打包的服务端完成真实客户端端到端验收，确认页面、两段飞行、断线恢复、唯一佩托、214599 击杀、电影 422 和 278500 奖励完成流程均可继续。
- 复用边界：传送前必须证明目标状态已经事务提交，状态同步、关闭窗口和飞行的先后顺序不能照搬旧 handler 中先改内存再发包的实现。只有后续阶段所在位置确实无法由玩家自行返回、实例重建也不能恢复交互对象，且旧 handler/客户端流程证明必须重新执行前置传送时，才能在 `ENTER_WORLD` 回退到最近的可重入阶段；普通持久进度不能借此清零，也不能把 `LOG_OUT` 和 `ENTER_WORLD` 同时作为回退入口。只有任务目标由入口形态的阈值变身生成时才添加死亡兜底，并必须用同一幂等门覆盖阈值和死亡竞争。GM 搜索别名必须同时限定任务 ID、状态和阶段，不得影响普通玩家地图标记或全局替换同名模板。页面 ID 与 action ID 仍是独立空间，每条电影后续页和奖励窗口都必须由 Aion 5.8 客户端与旧 handler 分别证明。
- commit：`8b058d4b4de747d12df9e9af63617619d5eefcf5`。

### 8.5 高阶守护者任务完成后未直接升到 66 级

- 代表任务：10520「遗失的记忆」。20520「Lost Destiny」为同一合同的魔族任务，不重复建立案例。
- 玩家症状：任务领奖后经验奖励和任务完成状态可以提交，但角色没有稳定地持久化高阶守护者身份，也不会在提交成功后立即升到 66 级；重试或重登还可能暴露数据库与在线角色状态不一致。
- 根因：标准 `npc-complete` 只覆盖普通奖励结算，任务 XML 没有声明高阶守护者晋升；经验奖励本身受当前经验和 65 级上限影响，不能替代 `is_archdaeva` 持久化及在线角色升级。
- 修复层：新增 `QuestAction.PromoteArchDaeva` 与 `promote-archdaeva` XML/XSD/DSL 合同；`PlayerQuestProgressionPort` 在奖励和任务状态相同的 JDBC 事务中执行 `GREATEST(exp, level-66-start-exp)` 与 `is_archdaeva=true`，提交后才调用在线角色 `setArchDaeva()`；事务快照同时恢复晋升标记。10520/20520 使用显式 `reward -> complete` 路由，顺序固定为经验奖励、晋升、完成任务，再刷新属性和完成状态。
- 修改文件：`src/main/java/com/aionemu/gameserver/dao/PlayerDAO.java`、`src/main/java/com/aionemu/gameserver/dao/impl/PlayerDAO.java`、`src/main/java/com/aionemu/gameserver/model/gameobjects/player/PlayerCommonData.java`、`src/main/java/com/aionemu/gameserver/questEngine/definition/QuestAction.java`、`QuestDefinitionCompiler.java`、`QuestDefinitionXmlCompiler.java`、`QuestDsl.java`、`src/main/java/com/aionemu/gameserver/questEngine/runtime/CompositeQuestActionPort.java`、`PlayerQuestProgressionPort.java`、`QuestProgressionPort.java`、`QuestMutationPlanner.java`、`QuestRuntimeComposition.java`、`quest_definition.xsd`、`quests/10520.xml`、`quests/20520.xml`，以及对应晋升、事务顺序和快照回归测试。
- 验证命令和结果：两个任务 XML 均通过 `xmllint --noout --schema .../quest_definition.xsd`；`git diff --check` 通过；生产 catalog/whitelist 报告为 6200 条任务编译成功、失败 0、白名单违规 0；用户确认真实客户端领奖后角色直接升到 66 级并完成验收。
- 复用边界：仅适用于任务完成本身代表高阶守护者晋升，且必须原子持久化身份标记、最低经验和任务完成状态的任务。普通经验奖励、普通等级奖励或仅更新客户端等级显示的任务不得复用该动作；晋升动作必须与恰好一个 `complete-quest` 和 `COMPLETE` 投影绑定。
- commit：`7cd670ffb22ddd080b550f6100b09932efe2c7d8`。

### 8.6 状态变化后先发页面、后同步任务状态

- 代表任务：1573「Some Tasty Mushrooms」。1607、2392、2533、10032、24153 为同一协议顺序问题，不重复建立案例。
- 玩家可见症状：一次交互已经把任务推进到奖励或新进度，但服务端先发送新页面、后发送新任务状态；客户端可能用旧 `status/step` 解释新页面，表现为成功页、奖励页或物品确认页与任务进度不同步，或需要重复交互。协议回环可稳定观察到错误的 `SM_DIALOG_WINDOW -> SM_QUEST_ACTION` 顺序。
- 根因：这些 transition 的事务状态和物品动作本身正确，`after-commit` 却把 `SHOW_QUEST_PAGE` 声明在 `sync-quest-state` 之前。两者都在 commit 后执行不代表顺序可以交换；页面消费的是刚提交的状态，必须先让客户端收到对应的 `SM_QUEST_ACTION`。
- 修复层：仅调整六个任务 XML 的 `after-commit` 顺序为 `sync-quest-state -> SHOW_QUEST_PAGE`，不改变 source、target、条件、priority、事务动作、页面 ID 或奖励。`QuestPacketOrderRegressionTest` 同时锁定完整 IR 合同，并通过真实 `CM_DIALOG_SELECT -> QuestEngine -> QuestProductionDispatcher -> SM_QUEST_ACTION/SM_DIALOG_WINDOW` 回环校验 objectId、questId 和包顺序。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/1573.xml`、`1607.xml`、`2392.xml`、`2533.xml`、`10032.xml`、`24153.xml`，以及 `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestPacketOrderRegressionTest.java`。
- 验证命令和结果：`rtk mvn -q -Dtest=QuestPacketOrderRegressionTest test` 为 7/7 通过；`rtk mvn -q -Dtest=QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestE2eInfrastructureTest test` 通过，生产 catalog 6200 条编译成功、失败 0、白名单违规 0，通用 E2E infrastructure 为 37/37 通过。Aion 5.8 客户端资源确认六个任务引用的成功页、奖励页、报告页和物品确认页存在且 action 可继续；全量 E2E 快照中 `INVALID_PACKET_ORDER=0`。本案例的客户端验收来自资源与真实协议包回环，不声称已逐任务完成人工客户端点击。
- 复用边界：仅适用于同一次已提交状态变化后立即显示依赖新状态页面的 transition。事务内状态或物品动作仍保持原顺序；没有状态同步、页面必须展示旧状态、关闭/电影/传送等副作用合同不同，或客户端页面本身不存在时，不能只交换两行掩盖根因。任何 `sync -> page` 修复都必须同时证明页面/action 存在、目标 objectId 权威、questId 正确且 commit 失败时两种包都不会发送。
- commit：`6a77337dbfd8cfec60f9daeb1125e51b976d56a3`。

### 8.7 装备物品起始条件未进入生产求值

- 代表任务：9550「[Event] Solorius Donations」。9553「[Event] Solorius Romance」使用同一装备物品和接取合同，不重复建立案例。
- 玩家可见症状：任务元数据要求装备物品 125040015；Aion 5.8 客户端页面和接取按钮均存在，但玩家即使已装备该物品，点击接受也无法由生产 dispatcher 完成 `NONE -> START`，任务状态和页面不推进。
- 根因：XML 编译器已把 `<condition type="equipped" quest-id="125040015"/>` 保留到 `QuestMetadata.startConditionGroups`，E2E 场景也能捕获装备事实；`QuestMutationPlanner` 将元数据起始条件转换为正式条件时却只支持 `finished`、`unfinished`、`acquired` 和 `noacquired`，遗漏 `equipped`，因此实际接受路由不能完成生产求值。
- 修复层：共享 production planner 将 `equipped` 映射为 `QuestCondition.EquippedItem`，继续复用 `QuestConditionEvaluator` 和 `QuestEquipmentFacts`，不修改任务 XML。装备物品数量满足时允许接取；明确未装备或装备事实未捕获时都不匹配，不用背包事实代替装备事实。独立生产流测试让 9550/9553 的 `QUEST_ACCEPT_1` 经过真实 `CM_DIALOG_SELECT -> QuestEngine -> QuestProductionDispatcher -> QuestExecutionCoordinator -> after-commit`，并校验状态包先于接取页面、objectId/questId/page 字段正确。
- 修改文件：`src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java`、`src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlannerTest.java`、`QuestE2eRuntime.java`，以及 `src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestEquippedStartProductionFlowTest.java`。
- 验证命令和结果：`rtk mvn -q -Dtest=QuestMutationPlannerTest,QuestEquippedStartProductionFlowTest test` 通过，共 22 个测试；正向场景证明两族任务均进入 `START`，反向场景证明未装备和装备事实未知时状态保持 `NONE` 且不发送任务状态/页面包。`rtk mvn -q -Dtest=QuestE2eInfrastructureTest,QuestPacketOrderRegressionTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test` 通过，生产 catalog 6200 条编译成功、失败 0、白名单违规 0。Aion 5.8 客户端资源确认 9550/9553 的接受路径页面和 action 存在；真实 CM 回环确认协议字段和包顺序，本案例不声称已完成人工客户端点击。
- 复用边界：仅适用于元数据起始条件的 `equipped` 类型，其中 `quest-id` 字段按旧数据合同承载物品模板 ID。装备套装、背包持有、任务工作物品或 transition 自身的装备条件已有独立合同，不能改写为本规则；任何新元数据条件都必须显式映射并在事实未知时 fail closed，不能用默认通过掩盖未支持类型。
- commit：`cc7aabea521af6b27bab129dc4be5ed63c0f3e07`。

## 9. 提交和交接清单

### 9.1 验收状态和代表案例门禁

任何 focused test、生产 catalog/whitelist、客户端实测或 runtime 证据到达后，之前的 `pending acceptance` 或案例去重判断立即失效。每次验收状态变化后，以及任何任务修复执行 `git add` 前，都必须重新完成以下检查：

- [ ] 当前是“实现完成，待验收”还是“验收完成”？缺少的证据及负责人是否已列出？
- [ ] 是否按玩家症状、根因、修复层、修复合同四项与第 8 节已有案例逐一比较？
- [ ] 判断结果是否明确记录为 `PENDING`、`ACCEPTED_EXISTING_PATTERN` 或 `ACCEPTED_NEW_PATTERN`？
- [ ] 若为 `ACCEPTED_EXISTING_PATTERN`，是否指出复用的代表案例，且没有重复追加任务 ID？
- [ ] 若为 `ACCEPTED_NEW_PATTERN`，Playbook 是否已准备记录代表任务、症状、根因、修复层、修改文件、验证结果、复用边界和稳定的修复 commit？

`PENDING` 可以在用户明确要求时提交实现，但不得添加代表案例或报告“验收完成”；最后一项证据到达后，必须在下一次提交/完整交接前重新运行本门禁。`ACCEPTED_NEW_PATTERN` 必须继续执行 9.2 的两提交批次，不能直接做单个源码提交后结束交付。

### 9.2 稳定哈希的两提交批次

新代表案例使用两个连续的本地提交。第一提交只包含修复源码、数据和测试；取得稳定哈希后写入 Playbook，第二提交只包含 Playbook。两者共同构成同一交付批次：中间不得夹入无关提交、push，或发送“验收完成”的最终交接。

先提交修复：

```bash
git diff --check
git status --short
git diff --stat
git add -- path/to/changed-quest.xml path/to/regression-test.java
git diff --cached --name-status
git diff --cached --check
git diff --cached --stat
git commit -m "fix(quest): describe repaired behavior"
git rev-parse HEAD
```

将上一步稳定哈希写入第 8 节代表案例后，再单独提交 Playbook。不要把文档 amend 进它所记录的修复提交，否则 commit 哈希变化后会形成失效的自引用：

```bash
git add -f docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md
git diff --cached --name-status
git diff --cached --check
git diff --cached --stat
git commit -m "docs(quest): record representative repair"
git log -2 --oneline
git status --short
```

如果实现是在 `PENDING` 状态下先提交，最终客户端/runtime 证据稍后才到达，保留原修复 commit 的稳定哈希，并把 Playbook 作为验收完成后的第一个提交；验收等待期间若已有其他历史，不为追求拓扑相邻而重写。只有修复提交、Playbook 提交及其验收结果都交接后，才算新代表案例交付完成。

### 9.3 交接内容

交接报告至少包含：

```text
症状和任务：
根因分类：状态 / 协议 / 客户端 / AI / 性能
权威证据：当前 XML/IR、旧 handler、客户端、runtime 日志
修改文件：
回归测试和实际命令：
未覆盖风险或 EVIDENCE_REQUIRED：
commit：
是否 push：否（除非用户明确要求）
```

## 10. 可复制给任意 coding agent 的任务提示词

```text
你在当前 checkout 的 quest 分支工作。
请先阅读 docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md、docs/quest/WRITING_GUIDE.zh-CN.md、
当前 checkout 的 AGENTS.md 和 `.agent/rules/` 规则。使用当前环境可用的搜索、读取和编辑能力。

任务：<quest-id>，症状：<玩家可复现步骤>。

按以下顺序工作：
1. 记录 git status/branch，并保留已有 dirty 改动。
2. 找到 catalog owner、当前 XML、编译后的 transition、旧 handler/正式模板和客户端 page/action 证据。
3. 明确 source/target/status/vars、条件、事务动作和 after-commit 顺序。
4. 判断是 XML 单任务问题、共享 runtime 问题、AI 副作用问题、客户端资源问题还是性能放大问题。
5. 只做最小修复；不要用候选 XML 或通用 page 猜值，不要删除必要的 visibility refresh。
6. 同时增加能证明完整行为合同的回归测试；涉及共享逻辑时增加生产目录级审计。
7. 仅在用户明确要求运行构建或测试时，串行执行 focused tests、生产 catalog/whitelist，必要时 clean verify；否则列出未执行的验收项。可执行不触发构建的 diff 检查。
8. 每次验收证据变化后以及提交前，重新执行 Playbook 代表案例门禁；旧的 `pending acceptance` 或去重判断不得沿用。
9. 仅在用户明确要求提交时，暂存本次路径并本地提交；若形成新代表案例，按“修复 commit -> 引用其稳定哈希的 Playbook commit”连续提交，期间不得夹入无关提交或 push。

最终报告必须列出：根因证据、改动、测试命令和结果、残余风险、commit hash。
```

这份 playbook 只规定排查和交付方法，不替代任务 XML 的字段合同；任何新任务仍必须回到客户端、旧 handler/正式模板和实际运行证据。
