# minion 跟随对齐真端：服务端跟随子系统整体移除，移动归客户端本地模拟

```text
report: 高速下 minion 反复「瞬移到主人身后→离远→再瞬移」，用户要求同速跟随；随后裁定「要和真端一样」
status: ACCEPTED（2026-10-09 用户实机验证通过：高速/风之路无瞬拉无刷屏、正常速度跟随与宠物一致）+ 门禁全绿（MinionKnownListTest 3/3 + FlyTeleportMinionSuspendGateTest 3/3）
changed: MinionController（跟随子系统删除，空壳化）、MinionService（spawn/despawn 移除跟随调度）、MinionKnownListTest（新增客户端权威移动门禁）
working tree: dirty（18602 等并行改动严格保留未动）
```

## 1. 真端排查（用户指令「排查下真端是怎么做的」的证据链）

| # | 证据 | 结论 |
|---|---|---|
| E1 | 真端守护灵内部名 = **Familiar**（本仓系统消息前缀 `STR_FAMILIAR_*` 同源）：`<真端根>/MainServer_Server64/classes/Misc/Familiar.cpp`（762 行）、`classes/World/FamiliarMapMgr.cpp`（4311 行） | 权威实现定位 |
| E2 | 两文件中 **move/follow/position/speed 关键词零命中**；`renames.tsv` 的 `Familiar*`/`FamiliarMapMgr*` 方法全集 = 数据管理（契约/成长/合成/命名/锁定/拾取/Buff）+ `SummonFamiliar`/`AbandonFamiliar`（召唤/收回；`SummonFamiliar` 最终只调 `User_SetSummonFamiliar` 设状态） | 服务端**没有任何移动模拟**，也没有 Move/Follow/Position 方法 |
| E3 | `NPCServer_NPCSvr64` 中 minion 零命中 | minion 不是服务端 AI 移动体 |
| E4 | SM_MINIONS action 全集（0–13）与 CM_MINIONS 均无移动 action/位置上报通道 | 协议层没有 minion 移动通道——服务端想发也无处可发 |
| E5 | 本仓 `PetController` 同架构：无任何跟随调度/SM_MOVE（只有心情值任务），而宠物跟随一直正常 | **客户端对服务端 spawn 的附着物确实跑本地跟随 AI**——决定性活体佐证 |

**真端结论**：minion 同速跟随是每个客户端基于主人移动流**本地模拟**（服务端只发召唤/收回状态帧），因此零售不存在「追不上→掉队→瞬拉」。

## 2. 本模拟器瞬拉循环的真正来源

服务端自制跟随：`MinionFollowTask`（1s）/`MinionTeleportTask`（2s）+ `teleportToPlayer`（>25m 瞬拉）+ `SM_MOVE(0x40)` 走向包（起点是**只在瞬拉时才更新的服务端陈旧位置**）。客户端本地跟随本来在跑（正常速度跟随良好即为证明），被服务端瞬拉帧反复打断——高速下服务端陈旧位置与主人距离迅速超 25m，循环瞬拉。

## 3. 改动（真端对齐）

| 文件 | 变更 |
| --- | --- |
| `controllers/MinionController.java` | 跟随子系统整体删除（`MinionFollowTask`/`MinionTeleportTask`/`startFollowing`/`stopFollowing`/`teleportToPlayer`/`MOVE_MASK`），类退化为 see/notSee 空壳；类级双语注释写明真端证据与禁止回退契约 |
| `services/toypet/MinionService.java` | `spawnMinion` 移除 `startFollowing` 调用、`despawnMinion` 移除 `stopFollowing` 调用、`MinionController` import 移除 |
| `world/knownlist/MinionKnownListTest.java` | 新增 `minionMovementStaysClientAuthored`：MinionController 禁含 `scheduleAtFixedRate`/`SM_MOVE`/`teleportToPlayer`、MinionService 禁含 `startFollowing`（防回退门禁） |

服务端仍会触碰 minion 的事件（全部保留不变）：召唤/收回状态帧（SM_MINIONS 5/6）、跨图传送位置重置（`TeleportService2.changePosition`）、飞行传送收起/落地恢复（CL-001 上轮方案，已实机验收）、死亡/登出完整收回、进图自愈（CM_LEVEL_READY）。

`TaskId.MINION_UPDATE`/`MINION_TELEPORT_CHECK` 枚举常量保留（不再被调度；避免动共享枚举）。

## 4. 预期行为（与真端/宠物一致）

- 主人任意速度（跑/飞行/风之路/加速）下 minion 由客户端同速跟随，**无瞬拉、无循环消息**；
- 距离免疫（CL-001 MinionKnownList）保留——主人 KnownList 不再遗忘 minion，可见性/广播不破；
- 飞行传送仍是「开始一条收回、落地一条召唤」（上轮实机验收行为不变）。

## 5. 验证状态

- IDE inspections：3 文件 0 error；
- **门禁全绿（2026-10-09，IDEA MCP）**：`MinionKnownListTest` 3/3（距离免疫 + 装配契约 + 新增客户端权威移动门禁）+ `FlyTeleportMinionSuspendGateTest` 3/3 回归；首轮红灯为门禁断言命中控制器 Javadoc 中的「SM_MOVE」术语字样，断言已改为匹配 import/调用形态（`serverpackets.SM_MOVE`、`teleportToPlayer(Player`）；
- **实机 PENDING**：冷重启后复验 ①正常速度跟随不回退（与宠物一致）；②高速/风之路全程无瞬拉无刷屏；③飞行传送行为不变；④旁观者视角 minion 正常跟随可见。
- 若实机出现「minion 原地不动」（客户端未本地跟随的兜底假设不成立），回退方案：恢复本删掉的跟随子系统（git 历史可取回），改走服务端同速 chase 方案（200ms tick + 主人实测速度推进，设计已推演留档于会话）。
