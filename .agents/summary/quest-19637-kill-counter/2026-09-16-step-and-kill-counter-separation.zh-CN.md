# 任务 19637「Onboard for One」阶段变量与击杀计数分离修复记录

## 1. 任务背景与玩家症状

- **任务 ID**: 19637（`Onboard for One`，中文名「特别任务1」）
- **阵营/等级**: 天族（ELYOS），50 级可接，IMPORTANT 类别
- **起始与报告 NPC**: 798926「凯西内尔主神代理人 Cainus」（位于英吉斯温 `210050000`）
- **击杀目标**: 215500, 215501, 215502, 215503（包含 215502 绿地斯科拉姆，共需消灭 10 只）
- **玩家症状**:
  1. 玩家击杀了 1 只怪（215502）后，客户端任务界面直接显示“10 个击杀任务已做完”，提示去找凯西内尔对话；
  2. 玩家跑回凯西内尔处对话，NPC 对话框中根本没有该任务，无法推进也无法领奖；
  3. 截图显示：`Status: START`，`Vars: 1 0 0 0 0 0`。

---

## 2. 根因分析（客户端合同与服务端 XML 错位）

### 2.1 客户端 step 与变量映射（SECTION_0 与 SECTION_1）
1. 查阅 Aion 5.8 客户端解包文件 `quest_monster.csv`：
   ```csv
   19637,Progress(SECTION_0==0; SECTION_1<10),,simpleQuest,,4,lf4_a_slime_snail_51_an,lf4_a_slime_snail_52_an,lf4_a_foam_wisp_51_n,lf4_a_foam_wisp_52_n
   ```
   客户端在 `SECTION_0 == 0`（即第一阶段 step 0）且 `SECTION_1 < 10`（击杀数小于 10）时，才认为任务处于“杀怪阶段”。
2. 查阅 Aion 5.8 客户端 HTML `quest_q19637.html` 的 `quest_summary`：
   ```xml
   <steps>
     <step><p visible="[%0]"><font color="[%1]">在英吉斯温消灭怪物([%2]/10)</font></p></step>
     <step><p visible="[%3]"><font color="[%4]">在英吉斯温主神代理人处和凯西内尔对话</font></p></step>
   </steps>
   ```
   客户端的 `[%0]` 对应 `SECTION_0 == 0`，`[%2]` 对应 `SECTION_1`（击杀数量）；当 `SECTION_0 == 1` 时，客户端直接判定第一步已完成，激活第二步 `[%3]`（与凯西内尔对话）。

### 2.2 服务端旧 XML 的错误实现
1. 之前的 `19637.xml` 仅声明了一个变量 `var0`，并将 `counter-grid` 直接绑定在 `var0` 上（`field="var0" required="10"`）。
2. 当玩家击杀第 1 只怪物时，`counter-grid` 将 `var0` 从 0 累加为 1，服务端下发：
   ```
   SM_QUEST_ACTION 任务=19637 状态=3 步数=1
   ```
3. 协议中的步数低 6 位即为 `var0`。客户端接收到 `步数=1` 后，解析出 `SECTION_0 = 1`，误认为第一步（10 杀）已经完成，立刻在界面上勾选第一步并切换到第二步（找凯西内尔对话）。
4. 但在服务端内部，当前节点却仅仅是 `k1`（`var0=1`，仅代表击杀了 1 只怪物）。服务端在 `k1` 节点根本没有对 NPC 798926 的对话响应；同时旧 XML 底部还存在无门槛的 `started -> reward` 错误后门，导致逻辑严重错乱。

### 2.3 历史 Java Handler 对照（Commit 911440146）
查阅权威历史基准 `_19637Onboard_For_One.java`：
```java
if (qs.getQuestVarById(1) < 10) {
    qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
    updateQuestStatus(env);
} if (qs.getQuestVarById(1) >= 10) {
    qs.setQuestVarById(0, 1);
    qs.setStatus(QuestStatus.REWARD);
    updateQuestStatus(env);
}
```
历史旧代码明确证明：
- `var1`（`questVars[1]` / `SECTION_1`）才是击杀数量计数器（0..10）；
- `var0`（`questVars[0]` / `SECTION_0`）是步骤控制变量；只有在击杀满 10 只后，`var0` 才置 1，同时任务进入 `REWARD` 状态！

---

## 3. 修复方案设计

1. **变量定义扩展（progress）**:
   - `var0`（offset=0, width=6, min=0, max=63）：任务步骤（0=杀怪阶段，1=报告阶段）；
   - `var1`（offset=6, width=6, min=0, max=10）：击杀计数（0..10）。
2. **节点声明（nodes）**:
   - `unaccepted`: `status=NONE`, `var0=0, var1=0`；
   - `started`: `status=START`，遵循 `COUNTER_SOURCE_PROJECTION_NO_LOCK` 模式，不锁定实时计数字段；
   - `reward`: `status=REWARD`, `var0=1, var1=10`；
   - `complete`: `status=COMPLETE`, `var0=0, var1=0`。
3. **击杀转换拆分**:
   - `priority 1`（0..8 杀）：事件覆盖目标怪 `215500 215501 215502 215503`，条件 `<variable-below field="var1" value="9"/>`，动作 `<set-variable field="var0" value="0"/>` 与 `<increment-variable field="var1" delta="1"/>`，提交后 `PACKET_ONLY` 同步。显式 set `var0=0` 可在击杀时自动修复玩家当前 `var0=1, var1=0` 的历史脏数据！
   - `priority 0`（第 10 杀）：条件 `<variable-at-least field="var1" value="9"/>`，动作 `<set-variable field="var0" value="1"/>` 与 `<set-variable field="var1" value="10"/>`，目标节点 `reward`，提交后 `LEVEL_AND_VISIBILITY_REFRESH`，凯西内尔头顶亮起黄色问号。
4. **满计数恢复路线**:
   - 针对可能存在的历史满计数存档，在 `started` 且 `<variable-at-least field="var1" value="10"/>` 时，允许对话报告并迁移到 `reward`。
5. **对话与领奖闭环**:
   - 接取：`unaccepted` 下发 4762（`SELECT_NONE`），接受后进入 `started`；
   - 领奖：`reward` 节点收到 `QUEST_SELECT(31)` 下发 `DEFAULT_SUCCESS(10002)`（即客户端 `select_success`）；
   - 奖励预览：收到 `SELECT_QUEST_REWARD(1009)` 下发奖励窗口 5（`select_quest_reward1`）；
   - 完成：通过 `npc-complete` 提供 6 选 1 装备奖励与固定经验值发放。

---

## 4. 影响范围与验证边界

- **改动文件**:
  - `src/main/resources/aion/data/static_data/quest_definition/quests/19637.xml`
  - `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest19637ClientDialogAlignmentTest.java`
- **静态验证**:
  - `xmllint --noout --schema ... 19637.xml` 校验通过。
  - `git diff --check` 无空白或格式违规。
- **待执行构建/测试（严格遵守 AGENTS.md 不擅自运行构建规则，保持 PENDING 状态）**:
  - `mvn test -Dtest=Quest19637ClientDialogAlignmentTest`
  - `mvn test -Dtest=QuestProductionJourneyTest`
