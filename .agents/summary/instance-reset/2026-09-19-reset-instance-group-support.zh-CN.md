# //reset instance 组队与单人进入组队本重置支持 (方案 A + B)

## 1. 现象与问题

- 用户反馈：`//reset instance` 可以重置个人的副本，但是没有重置个人进入的组队副本。
- 历史实现根因：
  1. **方案 A 缺失（个人独立进组队本）**：历史版本在 `isPlayerSoloInstance` 内部通过 `isSoloInstance` 对副本地图模板的 `maxPlayers == 1` 进行了硬编码限定。当玩家单人或以个人身份进入组队副本（如火神殿、帕休曼迪尔寺院，地图最大人数大于 1），即使未组队且无队伍登记（`registeredGroup == null && soloPlayerObj == playerObjectId`），仍因地图人数上限大于 1 被判定为非单人副本而拒绝重置。
  2. **方案 B 缺失（组队状态进组队本）**：当玩家处于队伍/联盟/军团（`isInGroup2` / `isInAlliance2` / `isInLeague`）中时，`//reset instance` 仅检索单人个人副本，未将玩家当前所在团队注册的副本实例纳入重置范围。

## 2. 修复方案 (方案 A + B)

### 方案 A：个人独立进组队本支持
- 在 `InstanceService.isPlayerSoloInstance(instance, playerObjectId)` 中，解除对地图模板 `maxPlayers == 1` 的硬编码约束。
- 判定原则：凡无队伍、联盟、军团登记（`registeredGroup == null && registredAlliance == null && registredLeague == null`），且所有者为该玩家（`isPersonal() && ownerId == playerObjectId`，或 `soloPlayerObj == playerObjectId`）的副本实例，均视为该玩家拥有的副本予以重置。
- 修复 `isSoloInstance(instance)` 中的布尔运算条件，使无队伍登记的单人占有副本在空副本延时销毁时，正确调度单人销毁延迟（`SOLO_DESTROY_DELAY_SECONDS`）而非队伍延迟。

### 方案 B：组队状态下团队副本重置支持
- 新增 `InstanceService.isPlayerInstance(WorldMapInstance instance, Player player)`，统一整合判定：
  1. 个人单人副本及个人独立进入的组队副本（方案 A）；
  2. 玩家处于队伍中且副本登记了当前队伍（`player.isInGroup2() && instance.getRegisteredGroup()` 匹配）；
  3. 玩家处于联盟中且副本登记了当前联盟（`player.isInAlliance2() && instance.getRegistredAlliance()` 匹配）；
  4. 玩家处于军团联盟中且副本登记了当前军团联盟（`player.isInLeague() && instance.getRegistredLeague()` 匹配）。
- 新增 `InstanceService.resetPlayerInstances(Player player)`，遍历全部活跃副本实例，重置属于该玩家个人或其当前团队的副本；原 `resetPlayerSoloInstances(Player player)` 保留作为向后兼容别名。
- 更新 `Reset.java` 命令处理器与 `commands.xhtml` 帮助文档，扩展提示为 `owned by you or your team`。

## 3. 测试与验证

- 聚焦测试：`mvn test -Dtest=InstanceServiceTest`
- 测试用例覆盖：
  - `identifiesOnlyOwnedSoloInstances`：验证个人进入组队本（300030000）正确判定为属于所有者；
  - `doesNotResetGroupRegisteredInstances`：验证当副本已登记 Team 时不作为 Solo 副本重置；
  - `identifiesPlayerInstancesForSoloAndTeams`：全链路覆盖个人单人本、独立进组队本、队伍副本、联盟副本、军团联盟副本、异队隔离及空值防护；
  - `usesSoloDestroyDelayForSoloEnteredGroupInstance`：验证单人进入组队本使用单人延时销毁配置。
- 测试结果：8 tests passed, 0 failures, 0 errors.
