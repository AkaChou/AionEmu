# 任务 10032/20032 真机修正：阶段必须留在 SECTION_0

- 时间：2026-09-15
- 玩家：Ww（objectId 153807）
- 相关：`quests/10032.xml`、`quests/20032.xml`、`Quest10032ItemPlayClientCounterProductionFlowTest`、commit `0823653a7`

## 一、现象（实机日志 + 客户端截图）

```text
23:06:19 SM_QUEST_ACTION 任务=10032 状态=0 步数=0          # 重置
23:06:55 SM_QUEST_ACTION 任务=10031 状态=5 步数=10         # 10031 领奖完成
23:06:55 SM_QUEST_ACTION 任务=10032 状态=3 步数=0          # zone-mission-end 自动接取（10031 广播修复生效）
23:07:05 SM_QUEST_ACTION 任务=10032 状态=3 步数=64         # 与 798952 完成 SETPRO1 后
23:07:05 SM_DIALOG_WINDOW  questId=0 下发页=0               # 对话关闭
```

服务端 `步数=64`（当前交换布局：`var1=1` → s1），但客户端任务说明仍显示第 0 行“前往卡斯帕调查营地，和克罗希亚对话”；再点 798952 无响应，798954 才会接。

## 二、判别证据

1. 执行 `//quest set 10032 START 65`（低槽 SECTION_0=1、高槽 SECTION_1=1）后，客户端任务说明**立刻切到下一行**，服务端仍在 s1（var1=1）。这证明**客户端任务说明行索引读取低位 SECTION_0（var0）**，而不是 09-14 修复假设的 var1。
2. 已提交的 `QuestPacketOrderRegressionTest#quest10032SynchronizesConsumedItemBeforeSuccessPage` 早已断言 `s7` 节点投影与条件为 `var0=7`；与 09-14 的交换布局冲突，此前属于既有失败例。
3. 同族客户端已验收的 10031 使用 `var0=阶段`、计数放 `var1/var2@6/12`；11468 的客户端脚本同样要求进行中 `SECTION_0==0`、计数在 `SECTION_1+`。

## 三、修正

- `10032.xml`、`20032.xml`：
  - `var0` = 阶段索引（`offset=0 width=6 max=8`，客户端 SECTION_0 / 任务说明行索引）。
  - `var1` = 眼泪使用次数（`offset=6 width=6 max=20`，客户端 SECTION_1）。
  - 节点投影、阶段条件与 `set-variable` 改回 var0；眼泪计数（`variable-below`/`variable-at-least`/`increment-variable`/满 20）改到 var1。
  - 回退边：`var1=0`（清计数）+ `var0=2`（回 s2）。
  - `10032.xml` 掉落门禁恢复 `collecting-step="6"`（门禁比较 `getQuestVarById(0)` 的阶段 6；满 20 次只是进入 s6 的前置条件）。
- `Quest10032ItemPlayClientCounterProductionFlowTest`：断言低槽=阶段、SECTION_1=次数、回退与掉落门禁，锁定上述合同。

## 四、验证

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 任务专用回归 | `mvn -q -Dtest='Quest10032ItemPlayClientCounterProductionFlowTest' test` | PASS |
| 相关回归 + 生产门禁 | `mvn -q -Dtest='Quest10032ItemPlayClientCounterProductionFlowTest,QuestPacketOrderRegressionTest,QuestDependencyIndexTest,CompletedQuestPrerequisiteRegressionTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test` | PASS；`PRODUCTION_COMPILE_OK=6193`、`FAILURES=0`、`WHITELIST_VIOLATIONS=0` |

`QuestPacketOrderRegressionTest` 同时跑 10032 的 E2E 协议环，确认提交状态先于成功页。

## 五、旧存档与测试角色

- 09-14 之后写入的旧 packed 值按新布局会误读（例如 `var1<<6`）。测试角色需要 `//quest delete 10032` 后由 10031 完成广播重新接取，或按新布局 `//quest set 10032 START <stage>`：
  - s1 = `1`，s5/眼泪起点 = `5`，眼泪第 c 次 = `5 + 64*c`，s6 = `6`，reward = `8`。
- 真机待复验：接取后说明行应随 798952→798954→799022 前进；副本内使用眼泪应显示 `1/20…20/20`，第 20 次切到击杀行。

## 六、追加修正：s6 完成 20 次后不得回退

### 现象

- 完成 20 次眼泪（s6）后离开副本，20 次计数被清空并回退到 s2（罗特亚斯）。

### 权威边界（commit 911440146 的旧 handler）

```java
if (player.getWorldId() != 300190000) {
    int var = qs.getQuestVarById(0);
    if (var >= 4 && var < 6) { removeQuestItem(...); qs.setQuestVar(2); }      // 只有 s4/s5 回退
    else if (var == 7) { removeQuestItem(...); qs.setQuestVar(8); qs.setStatus(REWARD); }
}
```

`onDieEvent`/`onLogOutEvent` 同样只有 `var >= 4 && var < 6`；魔族 `20032.xml` 本来也只保留 s4/s5 回退，s6 没有回退边。

### 修正

- 删除 `10032.xml` 中 `s6 -> s2` 的 `enter-world` / `die` / `log-out` 三条回退边；保留 s4/s5 回退与 `s7 -> reward`。
- `Quest10032ItemPlayClientCounterProductionFlowTest` 新增两条合同断言：
  - s6（`var0=6, var1=20`）时离副本/死亡/下线没有任何匹配计划；
  - s7（`var0=7, var1=20`）离副本转入 reward（回收两件道具 + `var0=8`）。

### 验证

| 门禁 | 结果 |
| --- | --- |
| `mvn -q -Dtest='Quest10032ItemPlayClientCounterProductionFlowTest' test` | PASS |
| 相关回归 + 生产门禁（10032 回归、QuestPacketOrder、QuestDependencyIndex、CompletedQuestPrerequisite、ClientQuestSectionAlignment、ProductionCatalogWhitelist、QuestDefinitionCatalogManifest） | PASS；`PRODUCTION_COMPILE_OK=6193`、`FAILURES=0`、`WHITELIST_VIOLATIONS=0` |

### 真机待复验

- 完成 20 次后出副本应保留进度：`//quest show 10032` 期望 `START, Vars: 6 20 0 0 0 0`（wire = `6 + 64*20 = 1286`）。
- 击杀 215488 拿到心脏后出副本应转 reward：`START -> REWARD, Vars: 8 20 0 0 0 0`。
