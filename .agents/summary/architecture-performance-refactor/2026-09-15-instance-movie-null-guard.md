# 实例播片空玩家守卫（Dark Poeta NPE）修复记录 / Instance `sendMovie` null-player guard

日期 / Date: 2026-09-15 · 分支 / Branch: `quest` · HEAD: `4be81fe51` · 状态 / Status: **已实现并通过聚焦验证，未提交**
关联模式 / Related pattern: `IR-008`（`.agents/memory-bank/patterns/instance-runtime.md`）

## 一、现象 / Symptom

用户运行期日志（09-15 20:22:57、20:23:25，Dark Poeta 副本内 NPC 死亡）：

```
java.lang.NullPointerException: Cannot invoke "...Player.getClientConnection()" because "player" is null
  at com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(PacketSendUtility.java:191)
  at com.aionemu.gameserver.instance.handlers.scripts.DarkPoetaInstance.sendMovie(DarkPoetaInstance.java:871)
  at com.aionemu.gameserver.instance.handlers.scripts.DarkPoetaInstance.onDie(DarkPoetaInstance.java:392 / 438)
  at com.aionemu.gameserver.controllers.NpcController.onDie(NpcController.java:243)
  at com.aionemu.gameserver.model.stats.container.CreatureLifeStats.reduceHp(CreatureLifeStats.java:92)
  at com.aionemu.gameserver.controllers.CreatureController.onAttack(CreatureController.java:255)
  at com.aionemu.gameserver.skillengine.effect.DamageEffect.applyEffect(DamageEffect.java:51)
```

同一窗口内的任务日志（3502 状态 3）说明死亡处理已进入副本脚本，但**异常中断了 `onDie` 的剩余逻辑**（点位累加、`deleteNpc`、`spawn` 均不会执行），影响面大于“少播一个影片”。

## 二、根因链 / Root cause

1. `AggroList#getMostPlayerDamage()` 在 `aggroList` 为空时 `return null`（`AggroList.java:266`）；即使非空，循环后也可能因“无玩家正伤害”返回 `null`（`AggroList.java:278`）。NPC 被另一 NPC、环境伤害、GM 命令杀死，或玩家已离线/aggro 被清空时都会命中。
2. `DarkPoetaInstance.onDie` 把该结果直接当非空使用：`Player player = npc.getAggroList().getMostPlayerDamage();` → `sendMovie(player, 426/427)`（`DarkPoetaInstance.java:392`、`:438`）。
3. `sendMovie` 内先 `movies.add(movie)` 再 `PacketSendUtility.sendPacket(player, …)`（`DarkPoetaInstance.java:868` 起），因此**只加中央判空会留下语义错误**：影片被标记“已播”却从未发送。

## 三、修复 / Fix（两层防护）

| 层 | 位置 | 行为 |
|---|---|---|
| ① 中央兜底 | `PacketSendUtility.java:196` | `if (player != null && player.getClientConnection() != null)`：玩家或连接缺失时静默跳过，不再抛 NPE（覆盖全部 `getMostPlayerDamage()` 消费点的崩溃面） |
| ② 脚本语义 | 25 个实例脚本的 `private void sendMovie(Player, int)` | 在 `movies.contains/add` **之前** `if (player == null) return;`，保证“没播过”就不标记已播，后续玩家仍可收到影片 |

第二层由闸门测试守护，而非逐点人工记忆。

## 四、覆盖面与闸门 / Coverage and gate

- 全仓 `src/main/java` 共 **26** 个 `sendMovie(Player, int)` 声明（`grep -rnE '(private|protected|public)\s+(void|boolean)\s+sendMovie\(Player\s+\w+,'`）。
- 本次为其中 **25** 个补守卫；`HaramelInstance`（`private boolean sendMovie`）**本就带守卫**，未改动，避免无意义 diff。
- `src/test/java/com/aionemu/gameserver/instance/handlers/InstanceMovieNullGuardTest.java`：正则扫描全部实例脚本，要求
  1. 至少匹配到 **20** 个声明文件（防止包路径变动导致闸门静默失效）；
  2. 每个声明后 240 字符窗口内必须出现 `== null`。
- **负向验证**：临时移除 `DarkPoetaInstance` 的守卫 → 测试失败并在断言信息中点名该文件；恢复后转绿（证明闸门有判别力，而不是恒真）。
- `src/test/java/com/aionemu/gameserver/utils/PacketSendUtilityTest.java`：`sendPacket(null, packet)` 不抛异常。

## 五、消费点审计 / Consumer audit（只读）

- `getMostPlayerDamage()` 共 **96** 处调用（实例脚本占 78 处）。
- 形如 `Player x = …getMostPlayerDamage();` 的赋值点：**全部**在首次解引用 `x.` 之前存在 `x == null` 判空（脚本化扫描，0 例遗漏）。
- 内联（非赋值）用法仅 6 处：`PacketSendUtility` 文档注释 2 处、`RetailPatternAI2.resolveUser` 的 `eventTarget != null ? … : getMostPlayerDamage()` 回退 1 处（null 为该分支既有可接受值）、`HarmonyArenaInstance` / `PvPArenaInstance` 各 1 处（**已自带** `if (… == null) return;`）、`AggroList` 定义本身 1 处。
- 结论：本次崩溃面已封闭，未发现需要追加改动的裸露消费点。

## 六、验证记录 / Validation

| 项 | 命令 | 结果 |
|---|---|---|
| 聚焦回归 | `mvn test -Dtest='InstanceMovieNullGuardTest,PacketSendUtilityTest,BeshmundirTempleInstanceTest,KromedesTrialInstanceTest,HaramelInstanceTest,ArchivesOfEternityMechanicsTest,AncientBoxInstanceTest,ArenaOfTenacityInstanceTest,FissureOfOblivionInstanceTest'` | **29 例 / 0 失败 / 0 错误**（BUILD SUCCESS，45s） |
| 格式 | `git diff --check` | 通过（无尾随空白、无空格缩进混入 tab 文件） |
| 沉淀校验 | `python3 .agents/memory-bank/sync_memory_bank.py`、`check_memory_bank.py` | `MEMORY_BANK_OK ROUTER_IDS=38 PATTERNS=38 SYMPTOM_INDEX=38` |
| 全量（本修复前一次，同批工作区） | `mvn test` | 3262 例 / 13 失败 / 0 错误；13 例均为分支既有 quest/AI 审计欠账（已在 HEAD 干净副本复现），**本次非既有失败 = 0** |

实现细节修正：批量插入的守卫最初在 13 个以 tab 缩进的脚本中写了空格缩进，已统一改为 `\t`；`TalocsHollowInstance` 全文件为空格缩进，保持原样。

## 七、运行态验收（2026-09-15 20:58 重启后）/ Runtime acceptance

| 证据 / Evidence | 值 / Value |
|---|---|
| 服务重启 | 20:58:08 启动（pid 82164/82165，IDEA 运行），20:58:26 `game 服务启动已返回` |
| 修复是否真的加载 | `target/classes/com/aionemu/gameserver/utils/PacketSendUtility.class` 于 20:58 重新编译；`javap -c` 显示 `ifnull` 判空分支（等价于源码 `player != null && …`） |
| 进入暗黑波伊塔 | 20:58:48 `c.a.g.s.instance.InstanceService - <副本进行中>300040000 ID=2，所有者=0`（`DarkPoetaInstance` 标注 `@InstanceID(300040000)`） |
| NPE / ERROR | 重启后至 21:05+：**NPE 0 次、ERROR 0 次**（`log/console.log` 第 14460 行之后） |
| 修复前对照 | 同一副本、同一任务 3502：20:22:57 与 20:23:25 两次 NPE（`log/console.log` 第 14215 / 14242 行，`DarkPoetaInstance.java:392` / `:438`） |
| 业务是否继续 | 任务 3502 由修复前的 `状态=3` 推进到 `状态=4`（21:04:56），说明 `onDie` 之后不会再被异常中断 |

用户实机确认（2026-09-15）：已重启并验证通过。

边界：本记录不含实机操作录像，**具体触发路径（NPC 来源 vs 已知列表过滤）仍无日志级定论**；两层修复对两条路径都生效，因此不影响结论。

## 八、边界与未验证项 / Boundaries

- **未提交**：全部改动仍在工作区（30 M + 1 AM + 1 A），需要用户授权后按精确路径提交。
- **运行期验收已完成**（见第七节），但覆盖的是“同副本、同路线、单人”回归；`//kill` / `//damage` 无法制造“无玩家伤害”场景（伤害记在 GM 玩家身上），NPC 来源击杀路径未单独构造。
- 中央判空只保证“不再崩”，**不会补发封包**；`sendMovie` 仍保持“播给伤害最高的玩家”语义，若将来需要“播给副本内所有玩家”，属独立需求。
- 单机、单客户端环境；组队/攻城/多客户端未覆盖。

## 九、后续建议 / Next steps

1. ~~重启后复现验证~~ → 已完成（第七节）。
2. 可选：把 `getMostPlayerDamage()` 的判空要求升级为静态闸门（扩大现有正则扫描范围），防止新脚本再引入同类写法。
3. 性能侧仍待办见 `2026-09-15-gameplay-jfr-hotspots.md` 第七节末的新 TOP 分配候选。
