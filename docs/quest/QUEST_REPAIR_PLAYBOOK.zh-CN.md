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

本次修复使用 `AI2Engine.QUEST_SIDE_EFFECT_AI` 和 `AI2EngineRetailSelectionTest`，把证据明确的 37 个 AI 纳入保护范围。未来发现新任务时，必须补充“生命周期副作用 + 零售 pattern 无等价动作”的证据和测试。

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

本节按可复用的“问题模式”记录案例，不按任务 ID 重复写相同正文。后续任务的玩家可见症状、根因、修复层和验收合同均与已有案例相同时，仍需独立完成验收，但验收通过后只把任务 ID 追加到原案例的“已验收任务 ID”；不得新增重复案例。行为或修复层不同时才建立新案例。

| 提交 | 案例 | 可复用结论 |
|---|---|---|
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

### 8.1 升级自动登记弹出不存在的任务页

- 已验收任务 ID：38001、38002。
- 代表任务：38001「Radiant Ops Recruitment」。
- 玩家症状：升级自动登记任务时客户端弹出任务 HTML 的 `HtmlPageId 4 / load fail`。
- 根因：升级入口错误发送 `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`；NPC `START_DIALOG(31)` 又错误发送 Aion 5.8 客户端不存在的 `SELECT2(1352)`。旧 handler 的升级入口只启动任务并刷新状态，NPC 对话页为 `DEFAULT_SUCCESS(10002)`。
- 修改文件：`src/main/resources/aion/data/static_data/quest_definition/quests/38001.xml`、`src/test/java/com/aionemu/gameserver/questEngine/definition/Quest38001LevelUpDialogTest.java`。
- 验证命令和结果：`rtk mvn -q -Dtest=Quest38001LevelUpDialogTest,Quest38002LevelUpDialogTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestDialogOrderAuditTest test` 通过；生产 catalog 6200 条编译成功，失败 0，白名单违规 0；顺序审计显示 38001 的 31 -> 10002 为 `PAGE_ACTION_MATCHED`，奖励页 5 为 `TERMINAL_PAGE_REACHED`；玩家实测升级登记不再弹出加载失败页。
- 残余风险：同型任务在完成各自客户端运行验收前，不得追加到“已验收任务 ID”。
- commit：`d3b28d2af3a7a3085da461d96bb9dfe6118d4905`。

## 9. 提交和交接清单

修复完成后按顺序执行：

```bash
git diff --check
git status --short
git diff --stat
```

只暂存本次文件。`docs/*` 默认被忽略，提交本文档时要显式指定：

```bash
git add -f docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(quest): add quest repair playbook"
git status --short
```

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
8. 仅在用户明确要求提交时，暂存本次路径并本地提交；未经明确要求不 push。

最终报告必须列出：根因证据、改动、测试命令和结果、残余风险、commit hash。
```

这份 playbook 只规定排查和交付方法，不替代任务 XML 的字段合同；任何新任务仍必须回到客户端、旧 handler/正式模板和实际运行证据。
