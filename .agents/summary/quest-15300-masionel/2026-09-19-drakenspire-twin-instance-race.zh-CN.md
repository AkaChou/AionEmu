# 任务 15300 与黑暗龙帝的对决：米西奥内未出现排查

- 日期：2026-09-19
- 范围：`src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenspireDepthsQInstance.java`
- 状态：代码已修复；聚焦测试、运行时和客户端验收待授权/待执行。

## 现象

任务 15300 进行到步骤 4“和黎明匕首派遣队长米西奥内对话”时，副本 `301520000` 中没有可对话的米西奥内（NPC `209863`）。

## 证据

- `log/console.log` 21:07:54 创建 `301520000` 实例；21:08:46 任务 15300 进入步骤 4；21:08:52 玩家进入 `301390000`；21:08:56 因离开任务副本回退到步骤 2。随后 21:09、21:10 又重复了同样的 `s3 -> s4 -> s2` 循环。
- 该时段 `log/error.log`、`log/warn.log` 没有对应的 NPE/ERROR，说明不是异常中断，而是死亡事件中的条件分支被静默跳过。
- `DrakenspireDepthsQInstance#onDie` 原先对 `237228/237229`（Lava/Heatvent Protector）双子场景使用：
  - `if (player != null)`
  - `switch (player.getRace())`
  - 只有进入该分支后才会 `spawn(209863, ...)`（天族米西奥内）/ `spawn(209883, ...)`（魔族帕西娅）。
- `AggroList#getMostPlayerDamage()` 在无玩家伤害、击杀者为 NPC/环境、或 aggro 已不可用时可能返回 `null`（既有模式 IR-008）。此时旧代码不会报错，但会跳过整个双子后置场景，包括任务 NPC 和开门 `731580` 的延迟清理。
- 奖励链路的执行顺序还会放大这个问题：`NpcController#doReward` 先通过 `PlayerTeamDistributionService` 给同队/临时队伍成员发任务击杀事件，之后才调用实例处理器的 `onDie`。因此任务步骤可能已经推进到 `s4`，但实例处理器重新读取 `getMostPlayerDamage()` 时仍可能得到 `null`，于是只推进任务、不生成米西奥内。
- 即使击杀者不为空，使用击杀者阵营选择副本剧情分支也不稳定；实例在首次进入时已经记录 `sealSceneRaceQ`，它才是副本剧情的阵营来源。

## 修复

- `onDie` 中 12 处阵营分支统一改为：
  - `if (sealSceneRaceQ != null || player != null)`
  - `switch (sealSceneRaceQ != null ? sealSceneRaceQ : player.getRace())`
- 这样只要实例已经记录阵营，双子死亡场景就会执行；击杀者缺失只影响回退阵营，不再阻断任务 NPC 生成。
- 新增 `src/test/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenspireDepthsQTwinSceneTest.java`，静态闸门要求：
  - 双子分支包含 `spawn(209863, ...)` 和 `spawn(209883, ...)`；
  - 双子分支不再包含 `if (player != null) {`；
  - `onDie` 中所有 12 处阵营分支都使用实例阵营表达式。

## 待验证

- 未执行 Maven（项目规则要求显式授权）。建议首个聚焦命令：
  - `mvn -B test -Dtest=DrakenspireDepthsQTwinSceneTest`
- 运行时需重新构建并重启后，以天族角色重做双子战斗，确认：
  - 任务 15300 进入步骤 4 后 NPC `209863` 出现在 `301520000`；
  - 对话可正常推进到后续步骤；
  - 魔族对应任务分支仍生成 `209883`。

## 运行期佐证（2026-09-19 21:44，服务于 21:44:00 重启新字节码）

- `log/console.log`：21:44:22 创建 301520000 实例；21:44:36 双子死亡后任务进入步数=4；21:44:41 客户端与 `209863`（米西奥内）对话；21:44:42 动作=10004 → 步数=5。
- 结论：双子死亡分支在实例阵营来源下正常执行，米西奥内如期生成并可对话推进。用户随后继续测试到步骤 7。
- 边界：这是运行期证据；整任务客户端验收仍以用户明确确认为准。
