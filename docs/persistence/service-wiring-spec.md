# 服务侧 DAO 接线规约（Batch 3 共享接线）

> 每个持有遗留 DAO 字段的服务类，按本规约改类型并删离线兜底。**只改字段/构造/import/兜底方法，不改业务逻辑与调用点**。

## 改造步骤（每个服务类）

1. **import**：`com.aionemu.gameserver.dao.XxxDAO` → `com.aionemu.gameserver.persistence.dao.XxxDao`（按 `docs/persistence/legacy-dao-to-flex-mapping.md` 映射，注意名字可能不同：`HousesDAO→HouseDao`、`PlayerABDAO→PlayerAtreianBestiaryDao`、`PlayerCreativityPointsDAO→PlayerCpDao`、`MotionDAO→PlayerMotionDao`、`PlayerTransformDAO→PlayerTransformDao`、`F2pDAO→F2paccountDao`、`SurveyControllerDAO→SurveyDao`、`TaskFromDBDAO→TaskDao`、`RewardServiceDAO→WebRewardDao`、`LimitedQuestDAO→LimitedQuestCounterDao`、`SeasonRankingDAO→CompetitionRankingDao`、`AbyssSpecialLandingDAO→SpecialLandingDao`、`BaseDAO→BaseLocationDao`、`OutpostDAO→OutpostLocationDao`、`LadderDAO→LadderPlayerDao`、`TownDAO→TownDao`、`GuideDAO→GuideDao`、`PetitionDAO→PetitionDao`、`AnnouncementsDAO→AnnouncementDao`、`InGameShopDAO→IngameshopDao`、`ChallengeTasksDAO→ChallengeTaskDao`、`HouseBidsDAO→HouseBidDao`、`PlayerPassportsDAO→PlayerPassportDao`、`PlayerEventsWindowDAO→PlayerEventsWindowDao`、`PlayerSkillSkinListDAO→PlayerSkillSkinDao`、`PlayerEmotionListDAO→PlayerEmotionDao`、`PlayerMacrossesDAO→PlayerMacrosseDao`、`PlayerRegisteredItemsDAO→PlayerRegisteredItemDao`、`PlayerLifeStatsDAO→PlayerLifeStatDao`、`PlayerEffectsDAO→PlayerEffectDao`、`PlayerMinionsDAO→PlayerMinionDao`、`PlayerWardrobeDAO→PlayerWardrobeDao`、`PlayerRecipesDAO→PlayerRecipeDao`、`PlayerPetsDAO→PlayerPetDao`、`PlayerShugoSweepDAO→PlayerShugoSweepDao`、`HouseScriptsDAO→HouseScriptDao`、`ItemCooldownsDAO→ItemCooldownDao`、`PortalCooldownsDAO→PortalCooldownDao`、`CraftCooldownsDAO→CraftCooldownDao`、`HouseObjectCooldownsDAO→HouseObjectCooldownDao`、`EventItemsDAO→EventItemDao`、`PlayerSkillListDAO→PlayerSkillDao`、`PlayerStigmasEquippedDAO→PlayerStigmasEquippedDao`、`PlayerEquipmentSettingDAO→PlayerEquipmentSettingDao`、`PlayerSettingsDAO→PlayerSettingDao`、`PlayerPunishmentsDAO→PlayerPunishmentDao`、`OldNamesDAO→OldNameDao`、`PlayerAppearanceDAO→PlayerAppearanceDao`、`PlayerTitleListDAO→PlayerTitleDao`、`PlayerBindPointDAO→PlayerBindPointDao`、`PlayerQuestListDAO→PlayerQuestDao`、`FriendListDAO→FriendDao`、`BlockListDAO→BlockDao`、`AbyssLandingDAO→AbyssLandingDao`、`AbyssRankDAO→AbyssRankDao`、`InventoryDAO→InventoryDao`、`ItemStoneListDAO→ItemStoneDao`、`MailDAO→MailDao`、`BrokerDAO→BrokerDao`、`HousesDAO→HouseDao`、`LegionDAO→LegionDao`、`LegionMemberDAO→LegionMemberDao`、`ServerVariablesDAO→ServerVariableDao`、`VeteranRewardsDAO→VeteranRewardDao`、`PlayerDAO→PlayerDao`）。

2. **字段与构造参数类型**：同名替换为新 Dao 类型（字段名、参数名、方法名**全部不变**）。

3. **删除无参兜底构造**：删掉 `public XxxService() { this(safeDao(...)); }` 这类构造器，以及 `private static <T extends DAO> T safeDao(...)` 方法；删掉因此不再使用的 `DAOManager` import。

4. **保留**：`getInstance()` / `instanceProvider`（ObjectProvider 模式）**不动**；业务方法、调用点、Javadoc 一律不动（DAO 方法签名未变）。

5. 若服务内直接 `new com.aionemu.gameserver.dao.impl.XxxDAO(...)`（无参兜底路径），一并删除该路径。

## 铁律

- **不改** `CoreRuntimeServiceBeans` / `EngineBeans` / `EventBeans` / `TaskAndStatBeans` / `SiegeBattlefieldBeans` / `NetworkBeans`（这些由 lead 集中改）。
- **不改** `PlayerServiceDaos` / `Player.java` / `IDFactory` / 测试。
- 只改你被分配的 `services/**` 文件。
- 不要运行构建命令。
