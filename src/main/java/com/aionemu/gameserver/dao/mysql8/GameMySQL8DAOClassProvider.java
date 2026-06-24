package com.aionemu.gameserver.dao.mysql8;

import com.aionemu.commons.database.dao.DAOClassProvider;

public class GameMySQL8DAOClassProvider implements DAOClassProvider {

	@Override
	public String contextName() {
		return "game";
	}

	@Override
	public Class<?>[] daoClasses() {
		return new Class<?>[] {
			MySQL8AbyssLandingDAO.class,
			MySQL8AbyssRankDAO.class,
			MySQL8AbyssSpecialLandingDAO.class,
			MySQL8AnnouncementsDAO.class,
			MySQL8BaseDAO.class,
			MySQL8BlockListDAO.class,
			MySQL8BrokerDAO.class,
			MySQL8ChallengeTasksDAO.class,
			MySQL8CraftCooldownsDAO.class,
			MySQL8EventItemsDAO.class,
			MySQL8F2pDAO.class,
			MySQL8FriendListDAO.class,
			MySQL8GuideDAO.class,
			MySQL8HouseBidsDAO.class,
			MySQL8HouseObjectCooldownsDAO.class,
			MySQL8HouseScriptsDAO.class,
			MySQL8HousesDAO.class,
			MySQL8InGameShopDAO.class,
			MySQL8InventoryDAO.class,
			MySQL8ItemCooldownsDAO.class,
			MySQL8ItemStoneListDAO.class,
			MySQL8LadderDAO.class,
			MySQL8LegionDAO.class,
			MySQL8LegionMemberDAO.class,
			MySQL8MailDAO.class,
			MySQL8MotionDAO.class,
			MySQL8OldNamesDAO.class,
			MySQL8OutpostDAO.class,
			MySQL8PetitionDAO.class,
			MySQL8PlayerAppearanceDAO.class,
			MySQL8PlayerAtreianBestiaryDAO.class,
			MySQL8PlayerBindPointDAO.class,
			MySQL8PlayerCooldownsDAO.class,
			MySQL8PlayerCreativityPointsDAO.class,
			MySQL8PlayerDAO.class,
			MySQL8PlayerEffectsDAO.class,
			MySQL8PlayerEquipmentSettingDAO.class,
			MySQL8PlayerEmotionListDAO.class,
			MySQL8PlayerEventsWindowDAO.class,
			MySQL8PlayerLifeStatsDAO.class,
			MySQL8PlayerLunaShopDAO.class,
			MySQL8PlayerMacrossesDAO.class,
			MySQL8PlayerMinionsDAO.class,
			MySQL8PlayerNpcFactionsDAO.class,
			MySQL8PlayerPasskeyDAO.class,
			MySQL8PlayerPassportsDAO.class,
			MySQL8PlayerPetsDAO.class,
			MySQL8PlayerPunishmentsDAO.class,
			MySQL8PlayerQuestListDAO.class,
			MySQL8PlayerRecipesDAO.class,
			MySQL8PlayerRegisteredItemsDAO.class,
			MySQL8PlayerSettingsDAO.class,
			MySQL8PlayerShugoSweepDAO.class,
			MySQL8PlayerSkillListDAO.class,
			MySQL8PlayerSkillSkinListDAO.class,
			MySQL8PlayerStigmasEquippedDAO.class,
			MySQL8PlayerThievesDAO.class,
			MySQL8PlayerTitleListDAO.class,
			MySQL8PlayerTransfoDAO.class,
			MySQL8PlayerVarsDAO.class,
			MySQL8PlayerWardrobeDAO.class,
			MySQL8PortalCooldownsDAO.class,
			MySQL8RewardServiceDAO.class,
			MySQL8SeasonRankingDAO.class,
			MySQL8ServerVariablesDAO.class,
			MySQL8SiegeDAO.class,
			MySQL8SurveyControllerDAO.class,
			MySQL8TaskFromDBDAO.class,
			MySQL8TownDAO.class,
			MySQL8VeteranRewardsDAO.class,
			MySQL8WeddingDAO.class,
		};
	}
}
