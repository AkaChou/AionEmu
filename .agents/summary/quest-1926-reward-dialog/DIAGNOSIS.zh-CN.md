# 任务 1926/2938 对话双重结束与任务书步骤空白诊断与修复报告

- 日期：2026-10-07
- 任务：1926（天族 20级秘密书库出入许可 / Secret Library Access）及其魔族同形镜像 2938
- 状态：**FIXED_PENDING_CLIENT_VERIFY**

---

## 1. 现场故障现象与 Trace 分析

### 1.1 玩家报障
与 NPC 203701（拉比怖托斯 / Lavirintos）对话交接后：
1. 对话窗口弹出了拒绝台词，并出现了 2 个“结束对话”；
2. 任务书（J键）打开后，任务说明下没有任何步骤（原应显示引导去见拉特里），且没有指引下一步做什么。

### 1.2 现场 Trace 数据
```text
10-07 16:29:55 INFO  [multiThreadIoEventLoopGroup-3-4] quest - [QUEST-TRACE][S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=2800 questId=0 下发页=10
10-07 16:29:56 INFO  [PacketProcessor:0] quest - [QUEST-TRACE][C->S] CM_DIALOG_SELECT 玩家=Kk npcId=203701 targetObj=2800 questId=1926 上一页=10 动作=31
10-07 16:29:56 INFO  [multiThreadIoEventLoopGroup-3-4] quest - [QUEST-TRACE][S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=2800 questId=1926 下发页=1011
10-07 16:29:57 INFO  [PacketProcessor:1] quest - [QUEST-TRACE][C->S] CM_DIALOG_SELECT 玩家=Kk npcId=203701 targetObj=2800 questId=1926 上一页=1011 动作=1012
10-07 16:29:57 INFO  [multiThreadIoEventLoopGroup-3-4] quest - [QUEST-TRACE][S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=2800 questId=1926 下发页=1012
10-07 16:29:58 INFO  [PacketProcessor:3] quest - [QUEST-TRACE][C->S] CM_DIALOG_SELECT 玩家=Kk npcId=203701 targetObj=2800 questId=1926 上一页=1012 动作=10255
10-07 16:29:58 INFO  [multiThreadIoEventLoopGroup-3-4] quest - [QUEST-TRACE][S->C] SM_QUEST_ACTION 任务=1926 状态=4 步数=1
10-07 16:29:58 INFO  [multiThreadIoEventLoopGroup-3-4] quest - [QUEST-TRACE][S->C] SM_DIALOG_WINDOW 玩家=Kk targetObj=2800 questId=1926 下发页=1097
```

---

## 2. 根因剖析

### 2.1 缺陷一：对话出现 2 个“结束对话”与拒绝台词
- **客户端 HTML 页面事实**（`QUEST_Q1926.html`）：
  - `select1_1`（页 1012）：拉比怖托斯写好推荐信并交出的台词（“（다 쓴 추천장을 건네며） 여기 있네. 비밀서고에서 필요한 자료를 잘 찾길 바라네.”），按钮为 `<Act href="HACTION_SET_SUCCEED">대화를 마친다.</Act>`（结束对话，动作号 10255）。
  - `select1_2`（页 1097）：前置任务未完成时的**拒信台词**（“흐음... 자네가 열심히 임무를 수행한 건 사실이지만 아직 이 추천서를 받을 정도는 아닌 것 같군... 내가 준 임무를 완수하면 다시 오게.”），按钮为 `<Act href="HACTION_FINISH_DIALOG">대화를 마친다.</Act>`（结束对话）。
- **真端行为对比**：
  - 查真端 `MainServer_ScriptDLL64` 反编译 `FUN_180f80190` 与通用对话口 `FUN_180caf150`：
    当玩家触发 `0x280f (HACTION_SET_SUCCEED = 10255)` 时，真端推进任务状态并显式调用 `*(param_2 + 0x5d8)`（关窗），**零发页**（不发任何新页面）。
- **模拟器错位**：
  - `1926.xml` 在 `SET_SUCCEED` 后的 after-commit 写了 `<dialog type="SHOW_QUEST_PAGE" page="SELECT1_2"/>`，导致玩家刚点击完 1012 页的“结束对话”，服务端又错误下发了拒信页 1097，产生多余页面和第 2 个“结束对话”。

### 2.2 缺陷二：任务书上步骤完全空白
- **客户端 HTML 步骤定义**：
  ```xml
  <steps>
     <step><p visible="[%0]"><font color="[%1]">[%dic:STR_DIC_N_Lavirintos]에게 가서 추천장을 받아라</font></p></step>
     <step><p visible="[%3]"><font color="[%4]">[%dic:STR_DIC_N_Latri]와 대화하라</font></p></step>
  </steps>
  ```
- **既有模式与同型判例**：
  - 与 `QE-045` / `QE-056`（如 1123、1466、10528、15300、25300）完全一致：
    对于此类 2 步任务，客户端在 `REWARD` 状态下会自动叠加领奖态的偏移量。
    若服务端在 `REWARD` 态下发 `var0=1`（步数=1），客户端计算出的步骤索引超出实际步骤范围（0..1），导致两行 `<p visible>` 都不满足，任务书步骤区域**整块空白**。
  - 查真端 `FUN_180f80190` / `FUN_180caf150`：在 `SET_SUCCEED` 时调用 `0x100(player, 1926, 0)`，所传步数就是 `0`。
  - 查迁移前 legacy Java handler `_1926Secret_Library_Access.java` 与 `_2938Secret_Library_Access.java`：
    在 203701 处仅调用 `qs.setStatus(QuestStatus.REWARD); updateQuestStatus(env);`，从未修改 `var0`，进入 REWARD 时的 packed step 始终为 `var0=0`。
  - 错误来源：commit `579e3475b` 将 1926/2938 机械套用 QE-051 将 reward 投影由 0 改为 1，直接引发了实机步骤消失。

---

## 3. 修复方案

1. **修正任务定义**：
   - `1926.xml` 与 `2938.xml`：
     - `reward` 节点投影还原为 `<var name="var0" value="0"/>`；
     - 增加无 source 的 `enter-world` 恢复边：`status=REWARD && var0=1 -> set var0=0`，使当前已处于错误状态的玩家登录/切图即可自动纠正；
     - 在 203701 / 203557 的 `SET_SUCCEED` 转换中，将 `<dialog type="SHOW_QUEST_PAGE" page="SELECT1_2"/>` 改为 `<close-dialog/>`。
2. **测试与契约锁定**：
   - `Quest1926And2938ClientDialogAlignmentTest`：更新 reward 投影断言为 0、恢复边断言为 `1 -> 0`、交接 after-commit 断言为 `CloseDialog()`。
   - `RewardRowTwoRowTalkFamilyContractTest`：将 1926/2938 移入 `QE045_LOCKED_SIBLINGS`（保持 packed step 0 锁定）。

---

## 4. 验证与复测建议

- 静态 Schema 校验：`xmllint --noout --schema quest_definition.xsd` 针对 1926.xml 与 2938.xml 校验通过。
- 实机复测步骤：
  1. 重启服务端使 XML 生效；
  2. 角色 Kk 进入游戏后，enter-world 边会自动将当前处于 REWARD 态的 1926 任务步数从 1 自愈为 0；
  3. 打开任务书（J 键），步骤列表应正常显示，且指向下一步“和拉特里对话”；
  4. 找拉特里（203894）对话，应正常下发 10002 交付页并顺利完成任务领奖。
