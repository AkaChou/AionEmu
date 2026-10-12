# 遗留 gs DAO → 生成 Flex 四件套 映射表 / Legacy DAO → Flex mapping

> 生成器对表名做单数化，故遗留 DAO 名与生成 Dao 名**多数不同**。接线时按本表逐条核对，禁止正则批量替换。
> The generator singularizes table names, so legacy DAO names and generated Dao names usually differ.

| 遗留 DAO (dao/XxxDAO) | 主表 | Po | 新 Dao (persistence/dao/) | 备注 |
|---|---|---|---|---|
| AbyssLandingDAO | abyss_landing | AbyssLandingPo | AbyssLandingDao | |
| AbyssRankDAO | abyss_rank | AbyssRankPo | AbyssRankDao | 另读 legion_members/legions/players；有 storeInTransaction |
| AbyssSpecialLandingDAO | special_landing | SpecialLandingPo | SpecialLandingDao | |
| AnnouncementsDAO | announcements | AnnouncementPo | AnnouncementDao | |
| BaseDAO | base_location | BaseLocationPo | BaseLocationDao | |
| BlockListDAO | blocks | BlockPo | BlockDao | 依赖 PlayerDAO |
| BrokerDAO | broker | BrokerPo | BrokerDao | 有 storeInTransaction；依赖 ItemStoneListDAO |
| ChallengeTasksDAO | challenge_tasks | ChallengeTaskPo | ChallengeTaskDao | |
| CraftCooldownsDAO | craft_cooldowns | CraftCooldownPo | CraftCooldownDao | |
| EventItemsDAO | event_items | EventItemPo | EventItemDao | |
| F2pDAO | f2paccount | F2paccountPo | F2paccountDao | |
| FriendListDAO | friends | FriendPo | FriendDao | 依赖 PlayerDAO |
| GuideDAO | guides | GuidePo | GuideDao | |
| HouseBidsDAO | house_bids | HouseBidPo | HouseBidDao | |
| HouseObjectCooldownsDAO | house_object_cooldowns | HouseObjectCooldownPo | HouseObjectCooldownDao | |
| HouseScriptsDAO | house_scripts | HouseScriptPo | HouseScriptDao | |
| HousesDAO | houses | HousePo | HouseDao | |
| InGameShopDAO | ingameshop | IngameshopPo | IngameshopDao | |
| InventoryDAO | inventory | InventoryPo | InventoryDao | 有 storeInTransaction |
| ItemCooldownsDAO | item_cooldowns | ItemCooldownPo | ItemCooldownDao | |
| ItemStoneListDAO | item_stones | ItemStonePo | ItemStoneDao | |
| LadderDAO | ladder_player | LadderPlayerPo | LadderPlayerDao | |
| LegionDAO | legions | LegionPo | LegionDao | 多表（emblems/history/join_requests/announcement_list/siege_locations/inventory） |
| LegionMemberDAO | legion_members | LegionMemberPo | LegionMemberDao | |
| LimitedQuestDAO | limited_quest_counters | LimitedQuestCounterPo | LimitedQuestCounterDao | |
| MailDAO | mail | MailPo | MailDao | 依赖 ItemStoneListDAO |
| MotionDAO | player_motions | PlayerMotionPo | PlayerMotionDao | |
| OldNamesDAO | old_names | OldNamePo | OldNameDao | |
| OutpostDAO | outpost_location | OutpostLocationPo | OutpostLocationDao | |
| PetitionDAO | petitions | PetitionPo | PetitionDao | |
| PlayerABDAO | player_atreian_bestiary | PlayerAtreianBestiaryPo | PlayerAtreianBestiaryDao | |
| PlayerAppearanceDAO | player_appearance | PlayerAppearancePo | PlayerAppearanceDao | |
| PlayerBindPointDAO | player_bind_point | PlayerBindPointPo | PlayerBindPointDao | |
| PlayerCooldownsDAO | player_cooldowns | PlayerCooldownPo | PlayerCooldownDao | |
| PlayerCreativityPointsDAO | player_cp | PlayerCpPo | PlayerCpDao | |
| PlayerDAO | players | PlayerPo | PlayerDao | 有 storeInTransaction/promoteArchDaevaInTransaction |
| PlayerEffectsDAO | player_effects | PlayerEffectPo | PlayerEffectDao | |
| PlayerEmotionListDAO | player_emotions | PlayerEmotionPo | PlayerEmotionDao | |
| PlayerEquipmentSettingDAO | player_equipment_setting | PlayerEquipmentSettingPo | PlayerEquipmentSettingDao | |
| PlayerEventsWindowDAO | player_events_window | PlayerEventsWindowPo | PlayerEventsWindowDao | |
| PlayerLifeStatsDAO | player_life_stats | PlayerLifeStatPo | PlayerLifeStatDao | |
| PlayerLunaShopDAO | player_luna_shop | PlayerLunaShopPo | PlayerLunaShopDao | |
| PlayerMacrossesDAO | player_macrosses | PlayerMacrossePo | PlayerMacrosseDao | |
| PlayerMinionsDAO | player_minions | PlayerMinionPo | PlayerMinionDao | |
| PlayerNpcFactionsDAO | player_npc_factions | PlayerNpcFactionPo | PlayerNpcFactionDao | |
| PlayerPasskeyDAO | player_passkey | PlayerPasskeyPo | PlayerPasskeyDao | |
| PlayerPassportsDAO | player_passports | PlayerPassportPo | PlayerPassportDao | |
| PlayerPetsDAO | player_pets | PlayerPetPo | PlayerPetDao | |
| PlayerPunishmentsDAO | player_punishments | PlayerPunishmentPo | PlayerPunishmentDao | |
| PlayerQuestListDAO | player_quests | PlayerQuestPo | PlayerQuestDao | |
| PlayerRecipesDAO | player_recipes | PlayerRecipePo | PlayerRecipeDao | |
| PlayerRegisteredItemsDAO | player_registered_items | PlayerRegisteredItemPo | PlayerRegisteredItemDao | |
| PlayerSettingsDAO | player_settings | PlayerSettingPo | PlayerSettingDao | |
| PlayerShugoSweepDAO | player_shugo_sweep | PlayerShugoSweepPo | PlayerShugoSweepDao | |
| PlayerSkillListDAO | player_skills | PlayerSkillPo | PlayerSkillDao | |
| PlayerSkillSkinListDAO | player_skill_skins | PlayerSkillSkinPo | PlayerSkillSkinDao | |
| PlayerStigmasEquippedDAO | player_stigmas_equipped | PlayerStigmasEquippedPo | PlayerStigmasEquippedDao | |
| PlayerTitleListDAO | player_titles | PlayerTitlePo | PlayerTitleDao | 有 storeInTransaction |
| PlayerTransformDAO | player_transform | PlayerTransformPo | PlayerTransformDao | |
| PlayerVarsDAO | player_vars | PlayerVarPo | PlayerVarDao | ✅ 已完成 |
| PlayerWardrobeDAO | player_wardrobe | PlayerWardrobePo | PlayerWardrobeDao | |
| PortalCooldownsDAO | portal_cooldowns | PortalCooldownPo | PortalCooldownDao | |
| RewardServiceDAO | web_reward | WebRewardPo | WebRewardDao | |
| SeasonRankingDAO | competition_ranking | CompetitionRankingPo | CompetitionRankingDao | |
| ServerVariablesDAO | server_variables | ServerVariablePo | ServerVariableDao | |
| SiegeDAO | siege_locations | SiegeLocationPo | SiegeLocationDao | |
| SurveyControllerDAO | surveys | SurveyPo | SurveyDao | |
| TaskFromDBDAO | tasks | TaskPo | TaskDao | |
| TownDAO | towns | TownPo | TownDao | |
| VeteranRewardsDAO | veteran_rewards | VeteranRewardPo | VeteranRewardDao | |
| IDFactoryAwareDAO | （无表，纯接口） | — | — | getUsedIDs 契约，终批删除 |
