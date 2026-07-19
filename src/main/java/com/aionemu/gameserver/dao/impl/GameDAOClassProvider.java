package com.aionemu.gameserver.dao.impl;

import com.aionemu.commons.database.dao.DAOClassProvider;

/**
 * 游戏服 MySQL 8 DAO 类提供器。
 * Game-server MySQL 8 DAO class provider.
 */
public class GameDAOClassProvider implements DAOClassProvider {

	/**
	 * 返回 DAO 上下文名称。
	 * Returns the DAO context name.
	 *
	 * context name
	 */
	@Override
	public String contextName() {
		return "game";
	}

	/**
	 * 返回本提供器注册的全部 MySQL 8 DAO 类。
	 * Returns all MySQL 8 DAO classes registered by this provider.
	 *
	 * array of DAO classes
	 */
	@Override
	public Class<?>[] daoClasses() {
		return new Class<?>[] {
			AbyssLandingDAO.class,
			AbyssRankDAO.class,
			AbyssSpecialLandingDAO.class,
			AnnouncementsDAO.class,
			BaseDAO.class,
			BlockListDAO.class,
			BrokerDAO.class,
			ChallengeTasksDAO.class,
			CraftCooldownsDAO.class,
			DynamicInstancesDAO.class,
			EventItemsDAO.class,
			F2pDAO.class,
			FriendListDAO.class,
			GuideDAO.class,
			HouseBidsDAO.class,
			HouseObjectCooldownsDAO.class,
			HouseScriptsDAO.class,
			HousesDAO.class,
			InGameShopDAO.class,
			InstanceRewardLedgerDAO.class,
			InventoryDAO.class,
			ItemCooldownsDAO.class,
			ItemStoneListDAO.class,
			LadderDAO.class,
			LegionDAO.class,
			LegionMemberDAO.class,
			LimitedQuestDAO.class,
			MailDAO.class,
			MotionDAO.class,
			OldNamesDAO.class,
			OutpostDAO.class,
			PetitionDAO.class,
			PlayerAppearanceDAO.class,
			PlayerAtreianBestiaryDAO.class,
			PlayerBindPointDAO.class,
			PlayerCooldownsDAO.class,
			PlayerCreativityPointsDAO.class,
			PlayerDAO.class,
			PlayerEffectsDAO.class,
			PlayerEquipmentSettingDAO.class,
			PlayerEmotionListDAO.class,
			PlayerEventsWindowDAO.class,
			PlayerInstanceLimitsDAO.class,
			PlayerLifeStatsDAO.class,
			PlayerLunaShopDAO.class,
			PlayerMacrossesDAO.class,
			PlayerMinionsDAO.class,
			PlayerNpcFactionsDAO.class,
			PlayerPasskeyDAO.class,
			PlayerPassportsDAO.class,
			PlayerPetsDAO.class,
			PlayerPunishmentsDAO.class,
			PlayerQuestListDAO.class,
			PlayerRecipesDAO.class,
			PlayerRegisteredItemsDAO.class,
			PlayerSettingsDAO.class,
			PlayerShugoSweepDAO.class,
			PlayerSkillListDAO.class,
			PlayerSkillSkinListDAO.class,
			PlayerStigmasEquippedDAO.class,
			PlayerThievesDAO.class,
			PlayerTitleListDAO.class,
			PlayerTransfoDAO.class,
			PlayerVarsDAO.class,
			PlayerWardrobeDAO.class,
			RewardServiceDAO.class,
			SeasonRankingDAO.class,
			ServerVariablesDAO.class,
			SiegeDAO.class,
			SurveyControllerDAO.class,
			TaskFromDBDAO.class,
			TownDAO.class,
			VeteranRewardsDAO.class,
		};
	}
}
