# 任务 15300 步骤 7「消灭盘龙巢穴的奥里萨」击杀不推进排查

- 日期：2026-09-19
- 范围：`immortalOrissanAI2`（本体修复）+ 同族 8 个阈值变身 AI（批量补死亡兜底）+ `DrakenspireDepthsQInstance` 奥里萨死亡场景（相邻缺陷）；
  证据与门禁：`ImmortalOrissanAI2Test`、`ThresholdTransformDeathFallbackGateTest`、`DrakenspireDepthsQOrissanSceneTest`。
- 状态：**第二轮**（下方「第二轮」章节）在本文件基础上追加了「真端 pattern 覆盖 + 实例层幂等兜底 + 副本销毁延迟任务收口」；聚焦测试 17 例全绿；奥里萨步骤客户端复测待执行。
- 关联沉淀：`IR-012`（副本销毁后延迟任务自行收口）、`IR-010` 边界补充（`spawnRetailActionNpc` 不支持 `SPAWN_LOCATION_MY_POINT`）。
- 关联：同任务步骤 4 的米西奥内问题见 `.agents/summary/quest-15300-masionel/2026-09-19-drakenspire-twin-instance-race.zh-CN.md`。

## 现象

天族任务 15300 推进到步骤 7（`var0=7`，客户端步骤文本为「消灭盘龙巢穴的奥里萨」）后，击杀奥里萨不再推进到步骤 8。
实测会话：2026-09-19 21:44:36 步数=4（双子）→ 21:44:42 步数=5（米西奥内）→ 21:45:03 步数=6 → 21:45:22 步数=7，此后到 21:47:50 登出无任何任务推进。

## 契约

- `15300.xml` `s7 -> s8`：`<kill-npc npc-id="237231"/>`；魔族 `25300.xml` 同款要求。任务只认 NPC **237231（虚脱的奥里萨 / Exhausted Orissan）**。
- `301520000` 的地图刷怪文件只生成 **237230（不灭之奥里萨 / Immortal Orissan，AI `immortal_orissan_quest`）**，237231 不在地图刷怪表里。
- 237231 由 237230 的 AI 变生产生：`immortalOrissanAI2` 在生命 ≤80% 时生成 237231 并删除自己（真端 `IDSeal_Q_Oritsa_01` 模式为 `on_die` / `on_enter_abnormal_state` → `spawn(IDSeal_Q_Oritsa_65_Al_02)` + `despawn_self`）。

## 根因

`CreatureController#onAttack` 的调用顺序是：

```java
getOwner().getAggroList().addDamage(attacker, damage, attackStatus); // 内部触发 ai.onAttacked -> handleAttack
getOwner().getLifeStats().reduceHp(damage, attacker);                // 之后才扣血
```

`AggroList#addDamageInternal` 会调用 `owner.getAi2().onAttacked(...)` → `AbstractAI#handleAttack`。因此 `immortalOrissanAI2#handleAttack` 里的
`checkPercentage(getLifeStats().getHpPercentage())` 读到的是**本次伤害结算之前**的 HP：

- 多段普通战斗：下一次命中会读到已 ≤80% 的 HP，变身照常触发（现状可玩）。
- 一击/爆发致死（从 >80% 直接打死）：阈值分支永远不执行，237230 直接死亡，237231 从未生成 → 任务永久停在步骤 7。
- 玩家侧观感正是「杀死了奥里萨但任务不更新，且刷出来的不是任务专属 NPC」：237230（不灭之奥里萨）不是任务击杀目标 237231（虚脱的奥里萨）。

### 佐证

- `error.log` 自 2026-09-19 12:17 起没有任何新记录；若 237231 真的死亡，`DrakenspireDepthsQInstance#onDie` 的 237231 分支会执行 `getNpc(209712)`（该 NPC 在 Q 实例从未生成）并抛 NPE。没有 NPE ⇒ 237231 从未死亡。
- `console.log` 21:44–21:47 无 ERROR/Exception，任务日志停在步数=7。
- `checkPercentage`/`getHpPercentage()` 阈值写法在全仓库约 136 个 AI 文件中复用，同一读序问题属同一族风险。

## 修复

`immortalOrissanAI2` 增加死亡兜底，与既有同类修复 `Betrayer_IcaronixAI2`（阿祖兰要塞：`handleDied` → `spawnFinalFormOnce()`）保持同一写法：

```java
@Override
protected void handleDied() {
    spawnExhaustedOrissanOnce();
    super.handleDied();
}

private void checkPercentage(int hpPercentage) {
    if (hpPercentage <= 80 && spawnExhaustedOrissanOnce()) {
        AI2Actions.deleteOwner(this);
    }
}

private boolean spawnExhaustedOrissanOnce() {
    if (!exhaustedOrissanSpawned.compareAndSet(false, true)) {
        return false;
    }
    spawn(EXHAUSTED_ORISSAN_NPC_ID, getOwner().getX(), getOwner().getY(), getOwner().getZ(),
        getOwner().getHeading());
    return true;
}
```

- 阈值路径与死亡路径共用同一个 CAS，保证 237231 只生成一次。
- 生成改为同步（原实现延迟 1 秒），去掉生成空窗，并与 `Betrayer_IcaronixAI2` 的写法一致；阈值路径仍在生成后删除本体。
- 新增 `src/test/java/com/aionemu/gameserver/ai/instance/drakenspireDepths/ImmortalOrissanAI2Test.java`：
  - 阈值 + 死亡兜底同时触发时只生成一次 `237231`；
  - 未过阈值直接死亡（一击致死）时仍然生成 `237231`。

## 同族批量补齐（已改 8 个文件 + 门禁）

同一族（「阈值分支 → spawn 替代形态 + `AI2Actions.deleteOwner`」，且阈值检查读到的是本次伤害前的 HP）共 10 个 AI，全部按 `handleDied()` → 与阈值路径共用同一个 `*Once()` CAS 闸门补齐：

| AI | 阈值 | 替代形态 NPC |
| --- | --- | --- |
| `azoturanFortress/Betrayer_IcaronixAI2` | - | 214599（原本已有死亡兜底，作为参考实现，未改） |
| `darkPoeta/Crazy_ScarAI2` | 75% | 281116 |
| `drakenspireDepths/Fountless_Heatvent_ProtectorAI2` | 30% | 236228 |
| `drakenspireDepths/Fountless_Lava_ProtectorAI2` | 30% | 236227 |
| `drakenspireDepths/immortalOrissanAI2` | 80% | 237231 |
| `brusthonin/Unfaithful_NtuamuAI2` | 50% | 214583 |
| `tiamaranta_eye/Aide_IranatiAI2` | 50% | 218555 |
| `tiamaranta_eye/Master_At_Arms_RaniganAI2` | 50% | 218558 |
| `tiamaranta_eye/TDown_M_Drakan_Pagati_Named_60_AeAI2` | 50% | 249099 |
| `tiamaranta_eye/TDown_M_Drakan_Sikara_Named_60_AeAI2` | 50% | 249102 |

- `Fountless_*` 两个文件在阈值路径保留原有 `AI2Actions.scheduleRespawn(this)`（Fountless 语义：本体重生），死亡兜底只补生成，不额外排定重生。
- 同目录的 `Lava_ProtectorAI2`（236227）与 `Heatvent_ProtectorAI2`（236228）经复核**不属于**该族：它们的 `checkPercentage` 只启动 5 分钟牺牲/事件任务、不生成替代形态，各自 `handleDied` 只掉宝箱，因此未改动、也不进门禁名单。
- 新增门禁 `src/test/java/com/aionemu/gameserver/ai/ThresholdTransformDeathFallbackGateTest.java`：每个成员必须覆写 `handleDied()`、使用 `compareAndSet`、保留 `AI2Actions.deleteOwner(this)` 与目标 NPC id，且死亡路径调用的 `*Once()` 必须与阈值路径复用同一个生成闸门。

## 相邻问题（已按选项 1 修复：喊话挂到实际生成的爆破手）

Q 实例 301520000 的奥里萨死亡场景（`DrakenspireDepthsQInstance#onDie` case 237231）原实现引用 `getNpc(209712)`（天族）/ `getNpc(209777)`（魔族）——即非任务副本脚本 `DrakenspireDepthsInstance` 的 Scene 14 任务 NPC（Masionel/Parsia）。Q 分支实际生成的是 `805363`（Killios，15300 步骤 8/9 对话 NPC）/ `805366`（Aimah，25300 同款）与守卫 `209713`/`209778`，从未生成 209712/209777，所以原代码必然 NPE + 爆破手喊话静默。

- 删除对 209712/209777 的引用。
- 开场台词 `1501314/1501312/1501310`（`STR_CHAT_IDSeal_Bomber_Gossip_*`，归属分遣队爆破手）移入**实际生成 209711/209776 的同一个排定任务**内，消除原先 0ms 并发任务的取用竞态。
- 收尾台词 `1501311/1501313` 移到 `killNpc(getNpcs(700546))` 之后延迟 `10000` 执行（与双子场景收尾节奏一致），并同样取 `getNpc(209711)`/`getNpc(209776)`；原重复的 Masionel/Parsia 收尾喊话已删。
- 任务推进不受影响：步骤 8/9 的对话 NPC 是 805363/805366，仍在同一场景生成。
- 新增门禁 `src/test/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenspireDepthsQOrissanSceneTest.java`：该 case 不得引用 209712/209777；喊话必须与生成同一任务；两族收尾延迟必须为 10000。

## 验证

- Maven（用户授权范围内，聚焦命令）：
  `mvn -B test -Dtest='ImmortalOrissanAI2Test,Betrayer_IcaronixAI2Test,ThresholdTransformDeathFallbackGateTest,DrakenspireDepthsQOrissanSceneTest,DrakenspireDepthsQTwinSceneTest'`
  - 2026-09-19 22:12 结果：11 tests, 0 failures, 0 errors, BUILD SUCCESS（全量编译 4716 主源 + 1023 测试源）；
    注释/缩进/import 归位后 22:17:58 复跑同一命令再次 BUILD SUCCESS（11 例，0 失败 0 错误）。
- 运行期已确认上一轮双子/米西奥内修复：双子死亡 → 步数=4，209863 生成并可对话 → 步数=5（见 `quest-15300-masionel` 文档）。
- 客户端复测（需重新构建、重启加载新字节码）：
  - 攻击 237230：无论是否一击致死，都应出现 237231（虚脱的奥里萨）；
  - 击杀 237231 后 15300 应由步骤 7 推进到步骤 8，并可与 805363（Killios）对话；
  - 魔族 25300 同款链路。

## 沉淀
- 同批次的出生表问题（237228/237229 各刷 2 个）见 `.agents/summary/spawn-duplicate-spots/2026-09-19-drakenspire-q-twin-duplicate.zh-CN.md`。

- 复用性不变量已提炼为 `AIM-007`（阈值变身必须共用一次性生成闸门并在死亡路径兜底），见 `.agents/memory-bank/patterns/ai-movement.md`。


## 第二轮：客户端复测后（2026-09-19 22:33–22:39）

### 运行期现状（`log/quests.log` / `log/error.log`）

- 22:33:35 进入 301520000 ID=2（`SpawnEngine` 生成 116 个 NPC）：22:34:36 步数=5（米西奥内）→ 22:35:03 步数=6（237224）→ 22:35:23 步数=7。
- 22:36:53 副本销毁 → 步数回退=2，22:37:09 重新接到步数=3 → 22:38:08 再次到步数=7，之后依旧卡死，22:39:12 副本销毁。
- 这一轮里 `error.log` 新增的全部是**副本销毁后延迟任务继续补刷**导致的 NPE：
  - `22:37:13 生成 NPC 209679/209680/209681 时出错，世界 301390000`（`DrakenspireDepthsInstance` 的 `spawnIDSealScene01`，排定于进副本 20 秒后）；
  - `22:39:13/22:39:15 生成 NPC 237219/237232 时出错，世界 301520000`（`startRaidSeal2/2_1`，排定于 237216 死亡后 65–67 秒）；
  - `22:39:35 生成 NPC 237217 时出错，世界 301520000`（`startRaidSeal3/4`，87–90 秒）。
- 没有任何与 237231 直接相关的报错或任务推进；237231 在这次会话里同样没有死亡记录。

### 新证据：237230 的真端 pattern 覆盖模板 AI

- `npc-ai` 映射（现为分片）`npc-ai-parts/npc-ai_*_247606.xml` 中 `237230` 的 `ai="IDSeal_Q_Oritsa_01"`；`AI2Engine.selectNpcAi` 在 `RetailPatternAI2.supports` 通过时返回 `retail_pattern`，**覆盖** NPC 模板里的 `ai="immortal_orissan_quest"`（即上一轮改动的 `immortalOrissanAI2`）。
- 真端 `IDSeal_Q_Oritsa_01` 有两条等价变身链：
  - `on_enter_abnormal_state`（`is_in_abnormal_state = ABNSTATEI_SANCTUARY`）：`spawn BIDSeal_Q_ShapeChange_Flash` + `spawn IDSeal_Q_Oritsa_65_Al_02` + `despawn_self`；
  - `on_die`：同两条 spawn + `despawn_self`。
  两者都用 `spawn_id=SPAWN_ID_NONE`、`live_time=0`、`SPAWN_LOCATION_MY_POINT`。
- 本轮复核的引擎事实（修正上一轮的推断）：
  1. `RetailPatternAI2#releaseTrackedSpawns()` 只遍历 `spawned`（按 `spawn_id` 登记）表，`SPAWN_ID_NONE` 的子对象**根本不在该表**，不会被重置删除；
  2. `on_die` 属于 `SPAWNER_END_EVENTS`，即使 `live_time=0` 也会进 `selfManagedSpawns`，同样被放过。
  因此「237231 被同一次死亡处理删掉」不成立；静态层面**无法判定** pattern 事件是否真正送达（`supports` 门禁、`is_in_abnormal_state` 条件与终端事件投递都只能在运行期观测）。

### 本轮的修复（保证任务击杀目标一定存在）

1. `DrakenspireDepthsQInstance#onDie` 新增 `case IMMORTAL_ORISSAN_NPC_ID (237230)`：记录死亡坐标后延迟 1.5 秒做**幂等兜底**——
   `isInstanceDestroyed` 或实例内已有存活的 237231 时跳过，否则在死亡点 `spawn(237231, x, y, z, heading)`。
   延迟一拍是为了让真端 pattern / 模板 AI 的死亡链先执行；已死亡但未消退的残骸不算“目标存在”。
2. 副本销毁后的延迟任务收口（对应上面的 NPE）：
   - 两个处理器（`DrakenspireDepthsQInstance` 301520000、`DrakenspireDepthsInstance` 301390000）都覆写
     `spawn(int,float,float,float,byte)`，`isInstanceDestroyed` 为真时返回 `null`；
   - `raidSeal` / `moveToSealForward` / `killNpc` 对 `null` 判空，`spawnEternalAltarOfTormentEntrance` / `spawnAgonyWell` / `spawnWaveDoor` 在销毁后直接返回。
3. 门禁：`DrakenspireDepthsQOrissanSceneTest#immortalDeathBacksUpTheQuestKillTarget`（兜底调用、目标 ID、幂等判据、延迟量）、
   `DrakenspireDepthsInstanceTeardownGuardTest`（两个处理器销毁后不再 spawn、`killNpc(null)` 不抛）。

### 验证

- `mvn -B test -Dtest='DrakenspireDepthsQOrissanSceneTest,DrakenspireDepthsQTwinSceneTest,DrakenspireDepthsQTwinSpawnSurfaceTest,DrakenspireDepthsInstanceTeardownGuardTest,ImmortalOrissanAI2Test,ThresholdTransformDeathFallbackGateTest,Betrayer_IcaronixAI2Test'`
  → 2026-09-19 23:09:38，17 tests, 0 failures, 0 errors, BUILD SUCCESS。
- 客户端复测（需重新构建重启）：步骤 7 击杀 237230 → 1.5 秒内必须出现可击杀的 237231 → 击杀后推进到步骤 8；副本销毁后 `error.log` 不再出现 `生成 NPC 209679/237219/237232/237217 时出错`。
- 若复测仍失败，下一步在 `AIConfig`/GM 打开 237230 的 AI2 日志，确认 `Terminal event on_die rule priority=200` 是否打印，从而区分「pattern 未接管」与「pattern 事件未送达」。

### 沉淀

- 新增 `IR-012`（副本销毁后延迟任务必须自行收口）；`IR-010` 边界补充：`RetailPatternAI2#spawnRetailActionNpc` 按 owner 坐标 (0,0,0) 解析动作坐标，无法覆盖 `SPAWN_LOCATION_MY_POINT`，需要实例用死亡者实际坐标补刷。
- memory-bank：`sync_memory_bank.py` + `verify_memory_bank.py` 通过（84 条，EVIDENCE_REFS=439）。
