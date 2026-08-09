# Quest 编写指南（XML + Java DSL）

本指南面向任务编写者，说明如何用正式 XML 或 Java DSL 编写任务定义。两种写法最终都会编译为同一份不可变 IR（`CompiledQuestDefinition`），由统一编译器校验后进入生产执行链。

## 1. 两种写法

| | 正式 XML | Java DSL（QuestDsl） |
|---|---|---|
| 位置 | `src/main/resources/aion/data/static_data/quest_definition/quests/<id>.xml` | Java 代码（测试 fixture / 工具） |
| 生产使用 | ✅ 唯一生产 owner 来源 | ❌ 仅测试与工具 |
| 校验 | XSD + `QuestDefinitionCompiler` 语义校验 | 与 XML 走同一 compiler |
| 加载 | 经 schema v2 `quest_definition_catalog.xml` 按 mode 注册后由 QuestEngine 加载 | 编译期直接 `compile()` |

规则：**生产任务只写 XML**。DSL 用于在测试中构造等价定义（`QuestDefinitionCompilerTest` 等用 `assertEquivalent` 验证 DSL 与 XML 编译结果完全一致），并可作为设计任务的草稿原型。

## 2. 核心概念：状态机图

一个任务定义 = `metadata`（静态信息）+ `progress`（位域变量）+ `nodes`（状态节点）+ `transitions`（事件迁移）。

- **node**：一个命名状态，投影为 `status`（NONE / START / REWARD / COMPLETE / LOCKED）+ 一组变量值。例如 `unaccepted`（NONE, var0=0）、`started`（START, var0=0）、`reward`（REWARD, var0=1）、`complete`（COMPLETE, var0=0）。
- **transition**：`source → target` 的边，由事件触发；条件满足才生效；动作在提交事务中执行；after-commit 动作在状态持久化后执行（发对话窗口、传送、播放动画等副作用）。
- **bit-field**：进度变量，打包进 `quest_vars` 的位域。`offset`/`width` 决定占位，`min`/`max` 决定合法值域，`persistence`（PERSISTENT / MEMORY）、`scope`（LOCAL / SHARED）。击杀计数、收集计数都用它表达。

事件是"事实"，条件是"判定"，动作是"变更"，after-commit 是"副作用"——职责不混。

## 3. 编写 XML

### 3.1 步骤

1. 写 `quests/<id>.xml`；仅在完全符合下述标准模式时使用领域积木，其他流程继续写普通 transition。
2. 在 schema v2 `quest_definition_catalog.xml` 注册一行 `<definition id="<id>" resource="aion/data/static_data/quest_definition/quests/<id>.xml" mode="EXECUTABLE"/>`（每个 ID 只注册一次）。
3. 旧模板仍存在时，以 `quest_data.xml` 作为静态元数据迁移来源；旧模板缺失时必须从真端/客户端取得逐字段权威证据，禁止从现有候选 XML 或任务行为反推猜值。
4. 删除旧执行入口（`quest_script_data/*.xml` 中对应节点 / 旧 Java handler），同一改动完成 owner 交接。

Catalog 只有两种 mode：`EXECUTABLE` 同时提供 metadata 与唯一执行 Owner；`METADATA_ONLY` 只提供名称、分类、重复规则、历史状态解释等 canonical metadata，XML 中不得声明 `nodes` 或 `transitions`，事件索引和 dispatcher 永远不会装载它。只有旧 `item_collecting` Owner 的 `start_npc_ids`、`end_npc_ids`、`action_item_ids`、`next_npc_id` 全部为 0，且存在权威旧模板时，才可按“无任何可注册入口”迁为 `METADATA_ONLY`。缺任一权威字段或仍有执行入口时必须保持 BLOCKED，不能为了清零旧 Owner 而生成定义。

### 3.2 结构（顺序敏感，受 `quest_definition.xsd` 严格校验）

`metadata` 子元素顺序固定：`races` → `classes` → `gender` → `repeat` → `prerequisites` → `items` → `inventory-items` → `work-items` → `rewards`/`reward-groups` → `extended-rewards`/`extended-reward-groups` → `drops` → `bonuses` → `kills` → `start-conditions`/`start-condition-groups` → `class-rewards`。每对简写/分组写法只能二选一；**drops 必须在奖励之后**。

`<rewards>` 与 `<start-conditions>` 只是单组简写。多奖励组必须按旧顺序写 `<reward-groups><group>...</group></reward-groups>`；`complete-reward-index` 先选择 reward group，再在该组内解释 fixed/choice 索引。多开始条件组必须写 `<start-condition-groups>`：组内条件为 AND，组间为 OR，禁止展开成全局 AND。

transition 内部顺序固定：`event` → `conditions` → `actions` → `after-commit`。

`<transitions>` 内可以按任意顺序混写普通 `<transition>` 和八种领域积木。任务文件继续使用 `version="1"`。

### 3.3 编译期领域积木

`npc-start`、`counter`、`counter-grid`、`kill-chain`、`kill-routes`、`npc-item-report`、`npc-report`、`npc-complete` 是严格的 XML 编写简写。XML 前端先把它们展开为普通 `QuestTransition`、`QuestAction`、`AfterCommitAction`，再交给 `QuestDefinitionCompiler`。它们不增加运行时状态、IR 类型、分发分支、继承、include、模板参数或表达式语言。

标准 NPC 接取：

```xml
<npc-start npc-id="203110"
           source="unaccepted"
           target="started"
           selection-sources="unaccepted started">
  <accept-actions>
    <give-item item-id="182400001" count="1"/>
  </accept-actions>
</npc-start>
```

它按顺序展开为 dialog 31（查看）、1007（剧情）、1002/20000（带 `start-eligible` 的两种接受）、1003/1004/20001（关闭），最后为 `selection-sources` 中每个节点生成 dialog 1008 任务列表入口。`accept-actions` 可省略，提供时会复制到两条接受路径。`selection-sources` 也可省略，表示该 NPC 不提供任务列表入口。source 节点必须投影为 `NONE`，target 必须投影为 `START`，所有节点标签必须存在。

标准计数器：

```xml
<counter source="started" target="reward" field="var0" required="80">
  <event><kill-npc npc-ids="215094 215095"/></event>
  <conditions><world-is world-id="210010000"/></conditions>
</counter>
```

积木生成两条共享事件和可选条件的路径：

- priority 1 在 `field < required - 1` 时留在 source，计数加一并发送 `PACKET_ONLY`；
- priority 0 在 `field == required - 1` 时进入 target，同样计数加一并发送 `PACKET_ONLY`。

因此第 N 次事件会立即完成，不需要第 N+1 次。字段必须存在，位域上限必须容纳 `required`，并允许 `required - 1`。source 投影必须省略计数字段，否则会锁死或重置实时计数；target 可以省略该字段，或明确投影为 `required`。需要不同 delta、动作、优先级、阈值语义或多个字段协同的计数流程必须继续写显式 transition。

连续击杀节点链：

```xml
<kill-chain nodes="v1 v2 v3 v4 v5 v6">
  <event><kill-npc npc-id="210670"/></event>
  <conditions><world-is world-id="210010000"/></conditions>
</kill-chain>
```

积木按 `nodes` 的书写顺序，为每对相邻节点生成一条 transition。每条路径共享同一个 `kill-npc` 事件和可选条件，不含事务动作，提交后固定发送 `PACKET_ONLY`。`nodes` 至少包含三个互不重复且已声明的标签；事件只允许 `kill-npc`。因此它适合保留显式节点投影的线性击杀链，并保证展开后的 transition IR 与手写链完全一致。任一边带有不同条件、动作、优先级、after-commit，或链中存在分叉时，应拆分为多个积木或继续写显式 transition。

标准 NPC 领奖：

```xml
<npc-complete npc-id="203123"
              source="reward"
              target="complete"
              fixed-reward-indices="0 1"
              dialog-ids="8..23"
              complete-reward-index="0"
              preview-dialog-ids="-1 1009"
              finish="SELECTION_DIALOG"/>
```

奖励索引对应 `complete-reward-index` 选中的有序 reward group；单组 `<rewards>` 等价于 group 0。固定奖励索引不得指向 `SELECTABLE_ITEM`。N 选 1 奖励应省略 `dialog-ids`，逐项声明客户端选择映射；`fallback` 表示只发固定奖励：

```xml
<npc-complete npc-id="203123" source="reward" target="complete"
              fixed-reward-indices="0 1" complete-reward-index="0"
              preview-dialog-ids="-1 1009" finish="CLOSE_DIALOG">
  <choice dialog-id="8" reward-index="2"/>
  <choice dialog-id="9" reward-index="3"/>
  <fallback dialog-ids="23"/>
</npc-complete>
```

choice 索引必须指向 `SELECTABLE_ITEM`，编译器会把该 metadata 条目降为具体 `ITEM` 奖励。preview、普通领取、choice、fallback 的 dialog ID 不能重复。source 必须投影为 `REWARD`，target 必须投影为 `COMPLETE`。每条完成路径的动作顺序固定为：固定奖励、可选的 choice 奖励、`complete-quest`；提交后始终执行 `refresh-player-stats`、`sync-quest-state mode="COMPLETION"`，最后按 `SELECTION_DIALOG`、`CLOSE_DIALOG`、`NONE` 三选一结束。预览路径固定显示 page 5。普通 NPC 默认 `SELECTION_DIALOG`；`useitem`、`quest_use_item`、`quest_start_use_item` 交互物默认 `CLOSE_DIALOG`。`NONE` 只允许有旧包序列或真端行为证据的白名单任务。

只有整个展开结果都正确时才使用积木。接取附加条件、非标准 dialog/page、领奖事务动作或额外 after-commit 副作用都必须写显式 `<transition>`。编译失败使用稳定的 `QuestCompilationException` code，并指出任务、积木和出错属性。

标准多 NPC 击杀路由：

```xml
<kill-routes source="started" target="k1"
             npc-ids="215468 215469 215470"/>
```

积木按 `npc-ids` 的书写顺序展开为多条独立的 `kill-npc` transition。每条边均无条件、无事务动作、无 priority，并在提交后发送 `sync-quest-state mode="PACKET_ONLY"`；每个 NPC 都是独立事件，不能合并为 `KillNpcSet`。source 和 target 只要求是已声明节点，因此允许 START 自环等已有合法投影。NPC ID 必须为至少两个互不重复的正整数。需要条件、额外动作、不同 after-commit 或非连续字节布局时继续写显式 transition。

标准 NPC 报告：

```xml
<npc-report npc-id="203941"
            source="started" target="reward" page="1352"/>
```

固定展开为两条边：dialog 31 在 START 节点自环并显示显式 `page`，dialog 1009 进入 REWARD，依次执行 `PACKET_ONLY` 同步和页面 5。`page` 只允许真端协议页面 `1352`、`2375`、`10002`；source 必须投影 START，target 必须投影 REWARD。成长任务中的 10000/10001、4762 或其他特殊页面协议不使用此积木。

标准带物品报告：

```xml
<npc-item-report npc-id="800937"
                 source="started" target="reward"
                 item-id="182215285" required="1"
                 remove-count="ALL"/>
```

固定展开为 dialog 39 和 20002 的成功/失败四条边。成功 priority 为 0，检查 `required` 数量、扣除相同数量或 `ALL`、进入 REWARD，随后依次同步 `PACKET_ONLY` 并显示页面 5；失败 priority 为 1，分别显示 2716 和关闭对话。`remove-count` 省略时等于 `required`，显式数字也必须相同，避免检查与扣除数量分离。source/target 必须分别投影 START/REWARD；额外条件、动作、页面或不同扣除协议继续使用显式 transition。成长任务的特殊页面不会被批量 matcher 捕获。

多维计数网格：

```xml
<counter-grid>
  <dimension field="var0" required="7"
             npc-ids="212600 212601"/>
  <dimension field="var1" required="7"
             npc-ids="212603 212604"
             source-order="VALUE_THEN_NODE"/>
</counter-grid>
```

每个 dimension 按 NPC 声明顺序生成原有的无动作击杀边。matcher 只接受“恰好当前字段增加 1”的目标节点，并要求全部 START 节点恰好构成各 dimension `0..required` 的笛卡尔积；投影字段集合必须与 dimension 集合完全一致，NPC ID 在维度内及维度间不得重复。`source-order="NODE"`（默认）按 nodes 文档顺序展开；`VALUE_THEN_NODE` 先按字段值 `0..required-1`，再按 nodes 文档顺序展开，用于精确复现旧生成器顺序。字段必须存在且能表示 `0..required`，每条网格 transition 必须连续相邻且只有标准击杀协议。缺少投影、缺少唯一目标、网格不完整、注释/额外动作或复杂行为时保留显式 transition；本积木不删除节点，也不改变运行时状态。

### 3.4 完整示例：1138「A Mother's Worry」（真实任务，无 work item 的 report_to 模板）

文件 `quests/1138.xml`（11 级 ELYOS 任务，NPC 203110 接取、203123 报告，奖励 1440 金币 + 5730 经验）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="1138" version="1">
  <metadata name="A Mother's Worry" display-name-id="1102308" min-level="11" max-level="2147483647" category="QUEST">
    <races>
      <race id="ELYOS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="1440"/>
      <reward kind="EXP" id="0" amount="5730"/>
    </rewards>
  </metadata>
  <progress>
    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>
  <nodes>
    <node label="unaccepted">
      <project status="NONE">
        <vars>
          <var name="var0" value="0"/>
        </vars>
      </project>
    </node>
    <node label="started">
      <project status="START">
        <vars>
          <var name="var0" value="0"/>
        </vars>
      </project>
    </node>
    <node label="reward">
      <project status="REWARD">
        <vars>
          <var name="var0" value="1"/>
        </vars>
      </project>
    </node>
    <node label="complete">
      <project status="COMPLETE">
        <vars>
          <var name="var0" value="0"/>
        </vars>
      </project>
    </node>
  </nodes>
  <transitions>
    <!-- 接取 NPC 203110 -->
    <transition source="unaccepted" target="unaccepted">
      <event>
        <talk-to-npc npc-id="203110" dialog-id="31"/>
      </event>
      <after-commit>
        <show-quest-dialog dialog-id="1011"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <talk-to-npc npc-id="203110" dialog-id="1007"/>
      </event>
      <after-commit>
        <show-quest-dialog dialog-id="4"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="started">
      <event>
        <talk-to-npc npc-id="203110" dialog-id="1002"/>
      </event>
      <conditions>
        <start-eligible/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
        <show-quest-dialog dialog-id="1003"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="started">
      <event>
        <talk-to-npc npc-id="203110" dialog-id="20000"/>
      </event>
      <conditions>
        <start-eligible/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <talk-to-npc npc-id="203110" dialog-ids="1003 1004 20001"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <talk-to-npc npc-id="203110" dialog-id="1008"/>
      </event>
      <after-commit>
        <show-quest-selection-dialog dialog-id="10"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <talk-to-npc npc-id="203110" dialog-id="1008"/>
      </event>
      <after-commit>
        <show-quest-selection-dialog dialog-id="10"/>
      </after-commit>
    </transition>
    <!-- 报告 NPC 203123 -->
    <transition source="started" target="started">
      <event>
        <talk-to-npc npc-id="203123" dialog-id="31"/>
      </event>
      <after-commit>
        <show-quest-dialog dialog-id="2375"/>
      </after-commit>
    </transition>
    <transition source="started" target="reward">
      <event>
        <talk-to-npc npc-id="203123" dialog-id="1009"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <show-quest-dialog dialog-id="5"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <talk-to-npc npc-id="203123" dialog-ids="-1 1009"/>
      </event>
      <after-commit>
        <show-quest-dialog dialog-id="5"/>
      </after-commit>
    </transition>
    <transition source="reward" target="complete">
      <event>
        <talk-to-npc npc-id="203123" dialog-ids="8..23"/>
      </event>
      <actions>
        <grant-reward kind="GOLD" id="0" amount="1440" amount-mode="QUEST_BASE"/>
        <grant-reward kind="EXP" id="0" amount="5730" amount-mode="QUEST_BASE"/>
        <complete-quest reward-index="0"/>
      </actions>
      <after-commit>
        <refresh-player-stats/>
        <sync-quest-state mode="COMPLETION"/>
        <show-quest-selection-dialog dialog-id="10"/>
      </after-commit>
    </transition>
  </transitions>
</quest-definition>
```

关键约定：

- **dialog 语义**（客户端对话协议，勿随意改）：`31`=查看任务信息、`1007`=任务剧情说明、`1002`/`20000`=接受任务（需 `start-eligible` 条件）、`1003 1004 20001`=接受后的常规关闭、`1008`=打开任务列表、`1009`=报告完成（START→REWARD）、`8..23`=领取奖励（REWARD→COMPLETE）、`-1`=关闭对话。
- **`start-eligible`**：接受类过渡的条件，由服务端检查等级/前置/阵营等接取资格。
- **同步**：状态变化后必须 `sync-quest-state`（VISIBILITY_REFRESH / LEVEL_AND_VISIBILITY_REFRESH / COMPLETION / PACKET_ONLY），让客户端刷新。
- **奖励结算**：`grant-reward` 与 metadata `rewards` 一一对应；GOLD/EXP 用 `amount-mode="QUEST_BASE"`（金额受任务等级加成），ITEM/TITLE 用默认 `EXACT`。
- **`dialog-ids="8..23"`**：区间写法，等于逐个列出 8 到 23。

### 3.5 进阶示例：1002「Request Of The Elim」（真实任务，选择奖励 + drops + 前置）

文件 `quests/1002.xml` 的 metadata（3 级 ELYOS MISSION，前置 1100，需收集 3 个任务物品 182200003，6 个可选武器奖励）：

```xml
<metadata name="Request Of The Elim" display-name-id="1102002" min-level="3" max-level="2147483647" category="MISSION" cannot-share="true" cannot-giveup="true">
  <races>
    <race id="ELYOS"/>
  </races>
  <prerequisites>
    <quest id="1100"/>
  </prerequisites>
  <items>
    <item id="182200003" count="3"/>
  </items>
  <rewards>
    <reward kind="EXP" id="0" amount="5943"/>
    <reward kind="TITLE" id="4" amount="1"/>
    <reward kind="SELECTABLE_ITEM" id="100200613" amount="1"/>
    <reward kind="SELECTABLE_ITEM" id="100000651" amount="1"/>
    <reward kind="SELECTABLE_ITEM" id="100100505" amount="1"/>
    <reward kind="SELECTABLE_ITEM" id="100600544" amount="1"/>
    <reward kind="SELECTABLE_ITEM" id="101800514" amount="1"/>
    <reward kind="SELECTABLE_ITEM" id="102000535" amount="1"/>
  </rewards>
  <drops>
    <drop npc-id="210677" item-id="182200003" chance="100" each-member="true" collecting-step="6"/>
    <!-- ... 其余 6 个掉落 NPC -->
  </drops>
</metadata>
```

- `prerequisites`：`<quest id="1100"/>` 表示必须先完成 1100。
- `items`：接取时自动发给玩家的物品（等价于 work-item 的 give 语义）。
- `SELECTABLE_ITEM`：N 选 1 奖励。**运行时不自动发**，由 reward→complete 的过渡拆分表达——每个选项一条 transition，dialog 从 8 起递增，公共奖励每条重复，选项不同：

```xml
<transition source="reward" target="complete">
  <event>
    <talk-to-npc npc-id="203067" dialog-id="8"/>
  </event>
  <actions>
    <grant-reward kind="EXP" id="0" amount="5943" amount-mode="QUEST_BASE"/>
    <grant-reward kind="TITLE" id="4" amount="1"/>
    <grant-reward kind="ITEM" id="100200613" amount="1"/>
    <complete-quest reward-index="0"/>
  </actions>
  <after-commit>
    <refresh-player-stats/>
    <sync-quest-state mode="COMPLETION"/>
    <show-quest-selection-dialog dialog-id="10"/>
  </after-commit>
</transition>
<!-- dialog-id 9、10、11、12、13 各一条，ITEM 换成其余五个武器 -->
```

- `drops`：**必填，勿省略**——省略会导致任务物品不掉落、任务无法推进。`collecting-step` 对应击杀计数步进（见下），`chance` 默认 100，`each-member="true"` 表示队伍每人独立掉落。

1002 的击杀推进（杀死 7 个目标 NPC 之一使收集步进）：

```xml
<transition source="s6" target="s7">
  <event>
    <kill-npc npc-ids="210677 210678 210679 210680 210681 210701 210702"/>
  </event>
  <after-commit>
    <sync-quest-state mode="PACKET_ONLY"/>
  </after-commit>
</transition>
```

（节点 `sN` 的 var0=N；每个击杀过渡 `source=sN target=sN+1`。1002 中还用了 `can-act` 交互物、`enter-world` + `world-is` 条件做升天变形 `morph`，见文件注释。）

### 3.6 work item 任务

`quest_data.xml` 有 `<quest_work_items>` 时（如 1106）：

- 接受过渡（dialog 1002/20000）：actions 加 `<give-item item-id="<id>" count="<n>"/>`。
- 报告过渡（dialog 1009，started→reward）：conditions 加 `<has-item item-id="<id>" count="<n>"/>`，actions 加 `<remove-item item-id="<id>" count="<n>"/>`。
- 追加一条 `priority="1"` 的拒绝分支：同 dialog 无物品时保持 started，`show-quest-selection-dialog dialog-id="10"`（参考 `quests/1142.xml`）。**运行时不会自动移除 work item，必须显式 remove。**

## 4. 编写 Java DSL

`QuestDsl`（`com.aionemu.gameserver.questEngine.definition.QuestDsl`）是与 XML 等价的 Java 表达，经同一 `QuestDefinitionCompiler` 编译。静态工厂：事件 `talkToNpc/killNpc/collectItem/useItem/...`，条件 `statusIs/hasItem/variableIs/...`，动作 `giveItem/removeItem/setVariable/...`，after-commit `showQuestDialog/syncQuestState/...`，以及 `quest(id)` 构建器。

### 4.1 真实任务 1138 的 DSL 等价写法

```java
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.*;

CompiledQuestDefinition motherWorry1138() {
    return quest(1138)
        // QuestMetadata 是不可变 record：简单任务用 minimal，完整字段用全构造器
        // （name/displayNameId/minLevel/maxLevel/races/category/repeatPolicy/prerequisites/...）
        .metadata(QuestMetadata.minimal("A Mother's Worry", 1102308, "QUEST"))
        .progress(bitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT))
        .node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
        .node("started",    project(QuestStatus.START, vars("var0", 0)))
        .node("reward",     project(QuestStatus.REWARD, vars("var0", 1)))
        .node("complete",   project(QuestStatus.COMPLETE, vars("var0", 0)))
        // 接取 NPC 203110：dialog 1002 接受
        .on(talkToNpc(203110, QuestDialog.ACCEPT_QUEST))
        .when(startEligible())
        .afterCommit(syncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH))
        .afterCommit(showQuestDialog(1003))
        .goTo("started")
        // 报告 NPC 203123：dialog 1009 进入奖励
        .on(talkToNpc(203123, QuestDialog.SELECT_REWARD))
        .afterCommit(syncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH))
        .afterCommit(showQuestDialog(5))
        .goTo("reward")
        // 领奖：dialog 8..23 结算，每档一条过渡
        .on(talkToNpc(203123, QuestDialog.SELECTED_QUEST_REWARD1))  // dialog 8
        .then(grantQuestBaseReward("GOLD", 0, 1440))
        .then(grantQuestBaseReward("EXP", 0, 5730))
        .then(completeQuest(0))
        .afterCommit(refreshPlayerStats())
        .afterCommit(syncQuestState(QuestStateSyncMode.COMPLETION))
        .goTo("complete")
        .compile();
}
```

### 4.2 真实 DSL 示例（测试 fixture：简单收集任务 1103）

来自 `SimpleQuestFamilyDefinitionTest`（与 `quests/1103.xml` 等价性验证）：

```java
private static QuestDsl.QuestBuilder simpleCollect1103() {
    QuestDsl.QuestBuilder builder = base(1103, "SimpleCollect 1103", "IMPORTANT")
        .progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
        .node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
        .node("started", project(QuestStatus.START, vars("var0", 0)))
        .node("object-collected", project(QuestStatus.START, vars("var0", 1)));
    builder.on(talkToNpc(203057)).from("unaccepted").goTo("started");
    builder.on(collectItem(700105, 1)).from("started").when(statusIs(QuestStatus.START))
        .when(variableIs("var0", 0)).then(setVariable("var0", 1)).goTo("object-collected");
    return builder;
}
```

要点：

- `quest(id)` → `QuestBuilder`，`.on(event)` → `TransitionBuilder`：`.when(条件)`、`.then(动作)`、`.afterCommit(副作用)`、`.from(源节点)`、`.goTo(目标节点)`、`.priority(n)`、`.compile()`。
- `QuestDialog` 枚举封装了客户端 dialog 常量（如 `QuestDialog.ACCEPT`=1002、`REPORT`=1009），也可直接传 dialog int。
- DSL 与 XML 走同一 compiler；测试用 `assertEquivalent` 断言 `dsl.compile().definition()` 与 `QuestDefinitionXmlCompiler.compile(xml)` 完全相等。**新增 XML 能力时若 DSL 缺工厂方法，两者必须同步补。**

## 5. 常用模式速查（真实任务参考）

| 模式 | 结构 | 权威示例 |
|---|---|---|
| report_to（无 work item） | 见 §3.4 | `quests/1138.xml` |
| report_to（有 work item） | give/has/remove-item + priority=1 拒绝分支 | `quests/1106.xml` |
| monster_hunt（击杀计数） | var0 逐级推进，每 NPC 一条 kill transition，`source=k{i} target=k{i+1}`，after-commit `sync PACKET_ONLY`；终态报告 dialog 1009 → reward | `quests/1120.xml`（单组）、`quests/1112.xml`（双组，var0/var1 交叉，offset 0/6） |
| item_collecting | end NPC dialog 39 上交检查：has-item（每个 collect_item）+ remove-item；无物品时 priority=1 fallback `show-quest-dialog 2716`；metadata 必须有 drops | `quests/1129.xml` |
| item_order | start_item_id 接取时 give-item，talk_npc 对话推进 var，end_npc 报告 | `quests/2146.xml`、`quests/2210.xml` |
| xml_quest（复杂） | 每 var 值一个 node，每 dialog 分支一条 transition | `quests/1115.xml`、`quests/1127.xml` |
| 选择奖励（N 选 1） | metadata `SELECTABLE_ITEM` × N；reward→complete 按 dialog 8、9、10... 拆 N 条，公共奖励重复 + 各一条 ITEM | `quests/1002.xml`（6 选 1）、`quests/1686.xml`（2 选 1） |
| 任务交互物 | `can-act`/`action-item-use` route + catalog drops；NPC 模板、AI 与 TALK route 必须一起校验 | `quests/1109.xml`（wine barrel 700106） |
| 普通宝箱 | `ChestAI2`、钥匙/使用进度、死亡与实例掉落链；不创建 Quest Owner | Ancient Box 702700/702701 |
| 延迟跨任务活动刷新 | 来源 Owner 检查外部活动/种族并调度；目标 Owner 用 `event-quest-refresh` 声明实时背包阈值和状态恢复 | `quests/80030.xml`、`quests/80033.xml`、`quests/80034.xml`–`80039.xml` |
| 定时失败 | after-commit `start-quest-timer`，事件 `quest-timer-end` → 失败节点 | — |

双 NPC 规则：start_npc_ids 每个 NPC 各一份接取过渡；end_npc_ids 每个 NPC 各一份报告/奖励过渡。

延迟跨任务活动刷新示例：

```xml
<conditions>
  <event-active quest-id="80029"/>
  <player-race-is race="ELYOS"/>
</conditions>
<after-commit>
  <schedule-event-quest-refresh seconds="10"
      quest-ids="80030 80034 80035 80036"/>
</after-commit>
```

延迟到期后，运行时重新查找在线玩家，并通过统一 dispatcher、事务与 publish 链向每个目标发送内部 `event-quest-refresh`。每个目标 Owner 必须自行声明实时 `has-item` 阈值，以及 NONE/START/REWARD/COMPLETE 各状态应保持、启动或重启的规则；COMPLETE → START 还必须带 `start-eligible`，由 canonical metadata 和 `QuestState.canRepeat(metadata)` 决定能否重复。禁止改成立即广播、直接调用旧 `QuestService.startEventQuest` 或按任务 ID 写特例 callback。当前调度是进程内、session-scoped、at-most-once 风格，服务器重启不会恢复未执行任务；只有权威行为要求跨重启持久化时才应另行设计持久化机制。

## 6. 编写检查清单

1. XML 通过 XSD 校验；`metadata` 子元素与 transition 内部顺序正确（drops 在 rewards 之后）。
2. catalog schema v2 注册一次且 mode 正确；`EXECUTABLE` 的旧入口（quest_script_data / Java handler）已删除，无双 owner；`METADATA_ONLY` 无节点、无 transition、无事件 route。
3. 静态元数据逐字段有权威来源；旧模板缺失时查真端（58Server/Map/XML/quest.xml 等）或客户端，不能核对就标记 BLOCKED，禁止猜测。
4. 接受类过渡带 `start-eligible`；状态变更后带 `sync-quest-state`。
5. `grant-reward` 与选中的有序 reward group 一一对应（GOLD/EXP 用 QUEST_BASE）；多组不扁平化。
6. 有 drops 的任务 metadata 已写 `<drops>` 段；有 work item 的任务 give/has/remove 齐全且无物品拒绝分支存在。
7. 连续重复动作（连续 refresh/sync/show-quest-selection-dialog）视为错误，除非旧逻辑明确要求。
8. 奖励结算使用合法 `npc-complete`，或保持 `source="reward" target="complete"` + `complete-quest` + `refresh-player-stats` + `sync COMPLETION` 结尾，并按 NPC/交互物规则收尾窗口。
9. 开始条件保持组内 AND、组间 OR；任务交互物走 catalog route/drop 索引，`ChestAI2` 宝箱保持在宝箱/实例掉落域。

## 7. 校验命令

```bash
# schema 校验（任一合法 XML 解析器，例）
python3 -c "import xml.dom.minidom,sys; xml.dom.minidom.parse('src/main/resources/aion/data/static_data/quest_definition/quests/<id>.xml')"

# catalog 重复 ID 检查
grep -c 'id="<id>"' src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml  # 应为 1

# 旧入口残留检查
grep -rn '<report_to id="<id>"\|<monster_hunt id="<id>"' src/main/resources/aion/data/static_data/quest_script_data/  # 应为空
grep -rn 'questId\s*=\s*<id>\b' src/main/java  # 应为空
```

生产验证：`mvn compile` + questEngine 相关测试（`QuestDefinitionCompilerTest`、各定义等价性测试）。
