# Quest Writing Guide (XML + Java DSL)

This guide is for quest authors. It explains how to write quest definitions in production XML or as Java DSL. Both forms lower to the same immutable IR (`CompiledQuestDefinition`) through the same compiler, then enter the production execution chain.

## 1. The Two Ways to Write a Quest

| | Production XML | Java DSL (QuestDsl) |
|---|---|---|
| Location | `src/main/resources/aion/data/static_data/quest_definition/quests/<id>.xml` | Java code (test fixtures / tooling) |
| Production use | ✅ sole source of production owners | ❌ tests and tooling only |
| Validation | XSD + semantic checks in `QuestDefinitionCompiler` | same compiler as XML |
| Loading | registered with a mode in schema-v2 `quest_definition_catalog.xml`, loaded by QuestEngine | compiled directly via `compile()` |

Rule: **write production quests as XML only.** The DSL exists to build equivalent definitions in tests (`QuestDefinitionCompilerTest` and others assert `assertEquivalent` — the DSL and XML compile to exactly equal definitions) and to prototype a quest before writing the XML.

## 2. Core Model: A State-Machine Graph

A quest definition = `metadata` (static info) + `progress` (bit-field variables) + `nodes` (states) + `transitions` (event edges).

- **node**: a named state projecting to a `status` (NONE / START / REWARD / COMPLETE / LOCKED) plus variable values, e.g. `unaccepted` (NONE, var0=0), `started` (START, var0=0), `reward` (REWARD, var0=1), `complete` (COMPLETE, var0=0).
- **transition**: a directed edge `source → target` fired by an event; it only applies when its conditions hold; actions execute inside the committed transaction; after-commit actions run after the state is persisted (show dialog windows, teleports, movies, and other side effects).
- **bit-field**: a progress variable packed into the `quest_vars` bit field. `offset`/`width` define where it lives, `min`/`max` its legal range, `persistence` (PERSISTENT / MEMORY) and `scope` (LOCAL / SHARED) how it is stored. Kill counters and collection counters are expressed with these.

Events are facts, conditions are tests, actions are state changes, after-commit is side effects — keep the responsibilities separate.

## 3. Writing XML

### 3.1 Steps

1. Write `quests/<id>.xml`; use the domain blocks below only for their exact standard patterns, and write ordinary transitions for everything else.
2. Register it in the schema-v2 catalog: `<definition id="<id>" resource="aion/data/static_data/quest_definition/quests/<id>.xml" mode="EXECUTABLE"/>` (each ID exactly once).
3. While a legacy template exists, use `quest_data.xml` as the metadata migration source. If it is absent, obtain field-level authority from the retail server/client; never infer missing values from a candidate XML or behavior alone.
4. Delete the old execution entry (`quest_script_data/*.xml` node / old Java handler) in the same change — one owner per quest.

The catalog has two modes. `EXECUTABLE` exposes canonical metadata and the one execution owner. `METADATA_ONLY` exposes metadata needed for names, categories, repeat policy, and historical-state interpretation, but its XML must not declare nodes or transitions and it is never loaded by the event index or dispatcher. A legacy `item_collecting` owner may become `METADATA_ONLY` only when `start_npc_ids`, `end_npc_ids`, `action_item_ids`, and `next_npc_id` are all zero and an authoritative legacy template exists. Missing authority or any executable ingress keeps the owner BLOCKED.

### 3.2 Structure (order-sensitive, strictly validated by `quest_definition.xsd`)

`metadata` child order is fixed: `races` → `classes` → `gender` → `repeat` → `prerequisites` → `items` → `inventory-items` → `work-items` → `rewards`/`reward-groups` → `extended-rewards`/`extended-reward-groups` → `drops` → `bonuses` → `kills` → `start-conditions`/`start-condition-groups` → `class-rewards`. Each shorthand/grouped pair is mutually exclusive. **`drops` comes after rewards.**

`<rewards>` and `<start-conditions>` are single-group shorthands. Preserve multiple reward groups in declaration order with `<reward-groups>`; `complete-reward-index` selects a group before fixed/choice indices are interpreted within it. Preserve multiple start-condition groups with `<start-condition-groups>`: conditions are AND inside one group and groups are OR alternatives. Never flatten them into one global AND.

Transition child order is fixed: `event` → `conditions` → `actions` → `after-commit`.

Inside `<transitions>`, ordinary `<transition>` elements and the eight domain blocks can be mixed in any order. All task files remain `version="1"`.

### 3.3 Compile-Time Domain Blocks

`npc-start`, `counter`, `counter-grid`, `kill-chain`, `kill-routes`, `npc-item-report`, `npc-report`, and `npc-complete` are strict XML authoring shorthands. The XML front end expands them to ordinary `QuestTransition`, `QuestAction`, and `AfterCommitAction` values before `QuestDefinitionCompiler` runs. They add no runtime state, IR type, dispatch branch, inheritance, include, template parameter, or expression language.

Standard NPC acquisition:

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

This expands, in order, to dialog 31 (view), 1007 (story), 1002 and 20000 (accept with `start-eligible`), 1003/1004/20001 (close), then dialog 1008 for every label in `selection-sources`. `accept-actions` is optional and copied to both accept routes. `selection-sources` is optional; omit it when this NPC must not open the quest list. The source node must project `NONE`, the target must project `START`, and every named node must exist.

Standard counter:

```xml
<counter source="started" target="reward" field="var0" required="80">
  <event><kill-npc npc-ids="215094 215095"/></event>
  <conditions><world-is world-id="210010000"/></conditions>
</counter>
```

The block emits two routes with the same event and optional conditions:

- priority 1 stays at `source` while `field < required - 1`, increments by one, and sends `PACKET_ONLY`;
- priority 0 enters `target` when `field == required - 1`, also increments by one, and sends `PACKET_ONLY`.

Therefore the Nth event completes the counter, not event N+1. The field must exist, fit `required`, and have room for `required - 1`. The source projection must omit the counter field so it does not lock or reset live progress. The target may omit it or project exactly `required`. Counters needing different deltas, actions, priorities, threshold semantics, or multiple coordinated fields must stay as explicit transitions.

Linear kill-node chain:

```xml
<kill-chain nodes="v1 v2 v3 v4 v5 v6">
  <event><kill-npc npc-id="210670"/></event>
  <conditions><world-is world-id="210010000"/></conditions>
</kill-chain>
```

The block emits one transition for each adjacent node pair in document order. Every route shares the same `kill-npc` event and optional conditions, has no transactional actions, and sends a fixed `PACKET_ONLY` sync after commit. `nodes` must contain at least three distinct declared labels, and the event must be `kill-npc`. This preserves explicit node projections while producing exactly the same transition IR as the handwritten linear chain. Split the chain or keep explicit transitions when any edge has different conditions, actions, priority, after-commit behavior, or branching.

Standard NPC reward settlement:

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

Reward indices refer to the ordered reward group selected by `complete-reward-index`; shorthand `<rewards>` is group 0. Fixed indices must not point to `SELECTABLE_ITEM`. For N-choose-1 rewards, omit `dialog-ids` and map each client choice explicitly; `fallback` completes with fixed rewards only:

```xml
<npc-complete npc-id="203123" source="reward" target="complete"
              fixed-reward-indices="0 1" complete-reward-index="0"
              preview-dialog-ids="-1 1009" finish="CLOSE_DIALOG">
  <choice dialog-id="8" reward-index="2"/>
  <choice dialog-id="9" reward-index="3"/>
  <fallback dialog-ids="23"/>
</npc-complete>
```

Choice indices must point to `SELECTABLE_ITEM`; the compiler lowers that metadata entry to the concrete `ITEM` reward. Dialog IDs must be unique across preview, generic, choice, and fallback routes. The source must project `REWARD`, the target `COMPLETE`. Every completion route orders actions as fixed rewards, optional choice reward, `complete-quest`; after commit it always runs `refresh-player-stats`, `sync-quest-state mode="COMPLETION"`, then exactly one finish policy. Ordinary NPCs default to `SELECTION_DIALOG`; `useitem`, `quest_use_item`, and `quest_start_use_item` interaction objects default to `CLOSE_DIALOG`. `NONE` requires an evidence-backed whitelist. Preview routes display page 5.

Use these blocks only when the entire expansion is correct. Extra conditions on acquisition, nonstandard dialogs/pages, reward-side mutations, or any additional after-commit effect require explicit `<transition>` elements. Compiler failures use stable `QuestCompilationException` codes and identify the quest, block, and offending attribute.

Standard multi-NPC kill routes:

```xml
<kill-routes source="started" target="k1"
             npc-ids="215468 215469 215470"/>
```

The block emits independent `kill-npc` transitions in declared `npc-ids` order. Every edge has no conditions, transactional actions, or priority and sends `sync-quest-state mode="PACKET_ONLY"` after commit. Each NPC remains a separate event; the block never lowers to `KillNpcSet`. Source and target only need to be declared nodes, so legal projections such as a START self-loop remain possible. The list must contain at least two distinct positive NPC IDs. Keep explicit transitions when the route needs conditions, extra actions, different after-commit effects, or non-contiguous XML layout.

Standard NPC report:

```xml
<npc-report npc-id="203941"
            source="started" target="reward" page="1352"/>
```

This always emits two edges: dialog 31 self-loops at the START node and shows the explicit `page`; dialog 1009 enters REWARD, then sends `PACKET_ONLY` and shows page 5. `page` is restricted to the retail protocol pages `1352`, `2375`, and `10002`; source must project START and target must project REWARD. Growth quests using 10000/10001, 4762, or another special page protocol must remain explicit.

Standard item-backed NPC report:

```xml
<npc-item-report npc-id="800937"
                 source="started" target="reward"
                 item-id="182215285" required="1"
                 remove-count="ALL"/>
```

This emits the fixed four-route dialog 39/20002 success and failure protocol. Success has priority 0, checks `required`, removes the same count or `ALL`, enters REWARD, then synchronizes with `PACKET_ONLY` and shows page 5. Failure has priority 1 and respectively shows 2716 or closes the dialog. Omitting `remove-count` means `required`; an explicit number must match it so checking and removal cannot diverge. Source and target must project START and REWARD. Extra conditions, actions, pages, or removal behavior require explicit transitions; growth-quest special pages are excluded from the batch matcher.

Multi-dimensional counter grid:

```xml
<counter-grid>
  <dimension field="var0" required="7"
             npc-ids="212600 212601"/>
  <dimension field="var1" required="7"
             npc-ids="212603 212604"
             source-order="VALUE_THEN_NODE"/>
</counter-grid>
```

Each dimension emits the existing action-free kill edges in declared NPC order. The matcher accepts only a target whose current field increases by exactly one, and requires all START nodes to form the complete Cartesian product across every dimension from `0..required`. Projected fields must equal the dimension fields exactly, and NPC IDs must be unique within and across dimensions. `source-order="NODE"` (the default) follows node document order; `VALUE_THEN_NODE` groups by field value `0..required-1` first and then follows node document order, allowing exact reproduction of legacy generator order. Fields must exist and represent `0..required`; each grid transition must be contiguous and use only the standard kill protocol. Missing projections, missing unique targets, incomplete products, comments/extra actions, or complex behavior stay explicit. The block does not delete nodes or change runtime state.

### 3.4 Full Example: 1138 "A Mother's Worry" (real quest, report_to template without work items)

File `quests/1138.xml` (level-11 ELYOS quest, accept from NPC 203110, report to 203123, rewards 1440 gold + 5730 XP):

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
    <!-- Accept NPC 203110 -->
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
    <!-- Report NPC 203123 -->
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

Key conventions:

- **dialog semantics** (client dialog protocol, don't change freely): `31`=view quest info, `1007`=quest story text, `1002`/`20000`=accept quest (requires the `start-eligible` condition), `1003 1004 20001`=normal close after accept, `1008`=open quest list, `1009`=report completion (START→REWARD), `8..23`=claim reward (REWARD→COMPLETE), `-1`=close dialog.
- **`start-eligible`**: the condition on accept transitions; the server checks level/prerequisites/race eligibility.
- **Sync**: every status change must be followed by `sync-quest-state` (VISIBILITY_REFRESH / LEVEL_AND_VISIBILITY_REFRESH / COMPLETION / PACKET_ONLY) so the client refreshes.
- **Reward settlement**: `grant-reward` must mirror the metadata `rewards` one-to-one; GOLD/EXP use `amount-mode="QUEST_BASE"` (amount scales with quest level), ITEM/TITLE use the default `EXACT`.
- **`dialog-ids="8..23"`**: range shorthand, equivalent to listing 8 through 23 individually.

### 3.5 Advanced Example: 1002 "Request Of The Elim" (real quest: selectable rewards + drops + prerequisites)

Metadata from file `quests/1002.xml` (level-3 ELYOS MISSION, prerequisite 1100, collect 3 quest items 182200003, six selectable weapon rewards):

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
    <!-- ... the other 6 drop NPCs -->
  </drops>
</metadata>
```

- `prerequisites`: `<quest id="1100"/>` means quest 1100 must be completed first.
- `items`: items handed to the player on accept (equivalent to the work-item give semantics).
- `SELECTABLE_ITEM`: N-choose-1 rewards. **Not granted automatically at runtime** — expressed by splitting the reward→complete transitions, one per option with dialog starting at 8, repeating the shared rewards and granting a different ITEM each time:

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
<!-- one more transition each for dialog-id 9, 10, 11, 12, 13, swapping the ITEM for the other five weapons -->
```

- `drops`: **mandatory, never omit** — omitting it means the quest item never drops and the quest cannot progress. `collecting-step` ties into the kill-count step (below), `chance` defaults to 100, `each-member="true"` means every group member gets an independent drop.

1002's kill progression (killing any of the 7 target NPCs advances the collection step):

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

(Nodes `sN` project var0=N; each kill transition is `source=sN target=sN+1`. 1002 also uses `can-act` interaction objects and `enter-world` + `world-is` conditions for the ascension `morph` — see comments in the file.)

### 3.6 Quest Work Items

When `quest_data.xml` has `<quest_work_items>` (e.g. 1106):

- Accept transitions (dialog 1002/20000): add `<give-item item-id="<id>" count="<n>"/>` to actions.
- Report transition (dialog 1009, started→reward): add `<has-item item-id="<id>" count="<n>"/>` to conditions and `<remove-item item-id="<id>" count="<n>"/>` to actions.
- Add a `priority="1"` rejection branch: same dialog without the item stays in started and shows `show-quest-selection-dialog dialog-id="10"` (see `quests/1142.xml`). **Work items are never removed automatically — remove them explicitly.**

## 4. Writing Java DSL

`QuestDsl` (`com.aionemu.gameserver.questEngine.definition.QuestDsl`) is the Java equivalent of the XML, compiled by the same `QuestDefinitionCompiler`. Static factories: events `talkToNpc/killNpc/collectItem/useItem/...`, conditions `statusIs/hasItem/variableIs/...`, actions `giveItem/removeItem/setVariable/...`, after-commit `showQuestDialog/syncQuestState/...`, and the `quest(id)` builder.

### 4.1 DSL Equivalent of Real Quest 1138

```java
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.*;

CompiledQuestDefinition motherWorry1138() {
    return quest(1138)
        // QuestMetadata is an immutable record: use minimal() for simple quests,
        // the full constructor for complete fields
        // (name/displayNameId/minLevel/maxLevel/races/category/repeatPolicy/prerequisites/...)
        .metadata(QuestMetadata.minimal("A Mother's Worry", 1102308, "QUEST"))
        .progress(bitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT))
        .node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
        .node("started",    project(QuestStatus.START, vars("var0", 0)))
        .node("reward",     project(QuestStatus.REWARD, vars("var0", 1)))
        .node("complete",   project(QuestStatus.COMPLETE, vars("var0", 0)))
        // Accept NPC 203110: dialog 1002
        .on(talkToNpc(203110, QuestDialog.ACCEPT_QUEST))
        .when(startEligible())
        .afterCommit(syncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH))
        .afterCommit(showQuestDialog(1003))
        .goTo("started")
        // Report NPC 203123: dialog 1009 enters reward
        .on(talkToNpc(203123, QuestDialog.SELECT_REWARD))
        .afterCommit(syncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH))
        .afterCommit(showQuestDialog(5))
        .goTo("reward")
        // Claim reward: dialog 8..23, one transition per slot
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

### 4.2 Real DSL Example (test fixture: simple collection quest 1103)

From `SimpleQuestFamilyDefinitionTest` (equivalence-verified against `quests/1103.xml`):

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

Key points:

- `quest(id)` → `QuestBuilder`; `.on(event)` → `TransitionBuilder`: `.when(condition)`, `.then(action)`, `.afterCommit(side effect)`, `.from(sourceNode)`, `.goTo(targetNode)`, `.priority(n)`, `.compile()`.
- `QuestDialog` wraps the client dialog constants (`QuestDialog.ACCEPT_QUEST`=1002, `SELECT_REWARD`=1009, `SELECTED_QUEST_REWARD1`=8, ...); you can also pass a raw dialog int where the API allows.
- The DSL and XML share one compiler; tests assert `dsl.compile().definition()` equals `QuestDefinitionXmlCompiler.compile(xml)` exactly via `assertEquivalent`. **When a new XML capability is added, the DSL factory must be added in the same change.**

## 5. Common Pattern Quick Reference (real quests)

| Pattern | Structure | Authoritative example |
|---|---|---|
| report_to (no work item) | see §3.4 | `quests/1138.xml` |
| report_to (with work item) | give/has/remove-item + priority=1 rejection branch | `quests/1106.xml` |
| monster_hunt (kill counter) | var0 advances step by step, one kill transition per NPC, `source=k{i} target=k{i+1}`, after-commit `sync PACKET_ONLY`; final report dialog 1009 → reward | `quests/1120.xml` (single group), `quests/1112.xml` (two groups, var0/var1 interleaved, offsets 0/6) |
| item_collecting | end NPC dialog 39 turn-in check: has-item (per collect_item) + remove-item; priority=1 fallback `show-quest-dialog 2716` when items are missing; metadata must carry `drops` | `quests/1129.xml` |
| item_order | start_item_id given on accept, talk_npc dialog advances var, end_npc report | `quests/2146.xml`, `quests/2210.xml` |
| xml_quest (complex) | one node per var value, one transition per dialog branch | `quests/1115.xml`, `quests/1127.xml` |
| selectable rewards (N-choose-1) | metadata `SELECTABLE_ITEM` × N; reward→complete split into N transitions by dialog 8, 9, 10..., shared rewards repeated + one ITEM each | `quests/1002.xml` (6 options), `quests/1686.xml` (2 options) |
| quest interaction object | `can-act`/`action-item-use` route plus catalog drops; validate NPC template, AI, and TALK route together | `quests/1109.xml` (wine barrel 700106) |
| ordinary chest | `ChestAI2`, key/use progress, death, and instance-drop chain; do not create a Quest Owner | Ancient Box 702700/702701 |
| delayed cross-quest event refresh | the source owner checks external event/race facts and schedules; target owners declare live inventory thresholds and state recovery with `event-quest-refresh` | `quests/80030.xml`, `quests/80033.xml`, `quests/80034.xml`–`80039.xml` |
| timed failure | after-commit `start-quest-timer`, event `quest-timer-end` → failure node | — |

Multi-NPC rule: every NPC in start_npc_ids gets its own accept transitions; every NPC in end_npc_ids gets its own report/reward transitions.

Delayed cross-quest event refresh example:

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

When the delay expires, the runtime resolves the live player again and sends the internal `event-quest-refresh` to every target through the shared dispatcher, transaction, and publish chain. Every target owner must declare its own live `has-item` threshold and the keep/start/restart rule for NONE, START, REWARD, and COMPLETE. A COMPLETE → START route must also carry `start-eligible`, so canonical metadata and `QuestState.canRepeat(metadata)` decide whether repetition is legal. Do not replace this with an immediate broadcast, a direct call to legacy `QuestService.startEventQuest`, or a quest-ID-specific callback. The current scheduler is in-process, session-scoped, and at-most-once; pending refreshes are not restored after a server restart. Add persistence only when authoritative behavior proves cross-restart recovery is required.

## 6. Writing Checklist

1. XML passes XSD validation; `metadata` child order and transition child order are correct (drops after rewards).
2. Registered exactly once in schema-v2 catalog with the correct mode. An `EXECUTABLE` entry has no old quest_script_data/Java owner; a `METADATA_ONLY` entry has no nodes, transitions, or event route.
3. Every metadata field has an authority source. If the legacy template is absent, check the retail server/client; if it cannot be verified, mark the migration BLOCKED instead of guessing.
4. Accept transitions carry `start-eligible`; status changes carry `sync-quest-state`.
5. `grant-reward` mirrors the selected ordered reward group one-to-one (GOLD/EXP as QUEST_BASE); do not flatten groups.
6. Quests with drops have a `<drops>` section; quests with work items have give/has/remove and the missing-item rejection branch.
7. Consecutive duplicate actions (repeated refresh/sync/show-quest-selection-dialog) are an error unless the old logic explicitly required both.
8. Reward settlement uses a valid `npc-complete` block or ends with `source="reward" target="complete"` + `complete-quest` + `refresh-player-stats` + `sync COMPLETION`, followed by the correct NPC/interaction-object window policy.
9. Start conditions retain AND-within-group and OR-between-groups. Quest interaction objects use catalog route/drop indices; `ChestAI2` chests remain in the chest/instance-drop domain.

## 7. Verification Commands

```bash
# schema check (any XML parser works, e.g.)
python3 -c "import xml.dom.minidom,sys; xml.dom.minidom.parse('src/main/resources/aion/data/static_data/quest_definition/quests/<id>.xml')"

# catalog duplicate-ID check
grep -c 'id="<id>"' src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml  # must be 1

# stale legacy-entry check
grep -rn '<report_to id="<id>"\|<monster_hunt id="<id>"' src/main/resources/aion/data/static_data/quest_script_data/  # must be empty
grep -rn 'questId\s*=\s*<id>\b' src/main/java  # must be empty
```

Production verification: `mvn compile` + the questEngine tests (`QuestDefinitionCompilerTest`, the definition equivalence tests).
