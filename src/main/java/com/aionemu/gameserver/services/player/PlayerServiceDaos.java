package com.aionemu.gameserver.services.player;

import java.util.Objects;

import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.BlockListDAO;
import com.aionemu.gameserver.dao.CraftCooldownsDAO;
import com.aionemu.gameserver.dao.EventItemsDAO;
import com.aionemu.gameserver.dao.F2pDAO;
import com.aionemu.gameserver.dao.FriendListDAO;
import com.aionemu.gameserver.dao.HouseObjectCooldownsDAO;
import com.aionemu.gameserver.dao.HousesDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.ItemCooldownsDAO;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.MotionDAO;
import com.aionemu.gameserver.dao.OldNamesDAO;
import com.aionemu.gameserver.dao.PlayerABDAO;
import com.aionemu.gameserver.dao.PlayerAppearanceDAO;
import com.aionemu.gameserver.dao.PlayerBindPointDAO;
import com.aionemu.gameserver.dao.PlayerCooldownsDAO;
import com.aionemu.gameserver.dao.PlayerCreativityPointsDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerEffectsDAO;
import com.aionemu.gameserver.dao.PlayerEmotionListDAO;
import com.aionemu.gameserver.dao.PlayerEquipmentSettingDAO;
import com.aionemu.gameserver.dao.PlayerEventsWindowDAO;
import com.aionemu.gameserver.dao.PlayerLifeStatsDAO;
import com.aionemu.gameserver.dao.PlayerLunaShopDAO;
import com.aionemu.gameserver.dao.PlayerMacrossesDAO;
import com.aionemu.gameserver.dao.PlayerNpcFactionsDAO;
import com.aionemu.gameserver.dao.PlayerPunishmentsDAO;
import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.dao.PlayerRecipesDAO;
import com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO;
import com.aionemu.gameserver.dao.PlayerSettingsDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.dao.PlayerSkillSkinListDAO;
import com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO;
import com.aionemu.gameserver.dao.PlayerTitleListDAO;
import com.aionemu.gameserver.dao.PlayerVarsDAO;
import com.aionemu.gameserver.dao.PlayerWardrobeDAO;
import com.aionemu.gameserver.dao.PortalCooldownsDAO;

/**
 * 玩家核心服务的持久层依赖聚合：不可变 record，由 Spring 配置类声明式构造器注入装配，
 * 并对全部 40 个 DAO 依赖执行及早失败判空，彻底取代 {@code DAOManager.getDAO} 静态查表。
 * Aggregated persistence dependencies of the core player service: an immutable record wired
 * declaratively via Spring constructor injection, with fail-fast validation of all 40 DAO
 * dependencies, fully replacing {@code DAOManager.getDAO} static table lookups.
 */
public record PlayerServiceDaos(
		PlayerDAO playerDAO,
		OldNamesDAO oldNamesDAO,
		PlayerAppearanceDAO playerAppearanceDAO,
		PlayerSkillListDAO playerSkillListDAO,
		InventoryDAO inventoryDAO,
		PlayerStigmasEquippedDAO playerStigmasEquippedDAO,
		PlayerEquipmentSettingDAO playerEquipmentSettingDAO,
		PlayerSettingsDAO playerSettingsDAO,
		PlayerQuestListDAO playerQuestListDAO,
		AbyssRankDAO abyssRankDAO,
		PlayerPunishmentsDAO playerPunishmentsDAO,
		HousesDAO housesDAO,
		PlayerRegisteredItemsDAO playerRegisteredItemsDAO,
		ItemStoneListDAO itemStoneListDAO,
		MailDAO mailDAO,
		PortalCooldownsDAO portalCooldownsDAO,
		CraftCooldownsDAO craftCooldownsDAO,
		PlayerNpcFactionsDAO playerNpcFactionsDAO,
		PlayerLunaShopDAO playerLunaShopDAO,
		EventItemsDAO eventItemsDAO,
		PlayerMacrossesDAO playerMacrossesDAO,
		FriendListDAO friendListDAO,
		BlockListDAO blockListDAO,
		PlayerTitleListDAO playerTitleListDAO,
		PlayerCreativityPointsDAO playerCreativityPointsDAO,
		PlayerEventsWindowDAO playerEventsWindowDAO,
		PlayerABDAO playerABDAO,
		PlayerWardrobeDAO playerWardrobeDAO,
		F2pDAO f2pDAO,
		MotionDAO motionDAO,
		PlayerVarsDAO playerVarsDAO,
		PlayerRecipesDAO playerRecipesDAO,
		PlayerSkillSkinListDAO playerSkillSkinListDAO,
		PlayerEffectsDAO playerEffectsDAO,
		PlayerCooldownsDAO playerCooldownsDAO,
		ItemCooldownsDAO itemCooldownsDAO,
		HouseObjectCooldownsDAO houseObjectCooldownsDAO,
		PlayerBindPointDAO playerBindPointDAO,
		PlayerLifeStatsDAO playerLifeStatsDAO,
		PlayerEmotionListDAO playerEmotionListDAO) {

	/**
	 * 紧凑构造器：对全部持久层依赖执行及早失败判空。
	 * Compact constructor: fail-fast null validation for every persistence dependency.
	 */
	public PlayerServiceDaos {
		Objects.requireNonNull(playerDAO, "playerDAO must not be null");
		Objects.requireNonNull(oldNamesDAO, "oldNamesDAO must not be null");
		Objects.requireNonNull(playerAppearanceDAO, "playerAppearanceDAO must not be null");
		Objects.requireNonNull(playerSkillListDAO, "playerSkillListDAO must not be null");
		Objects.requireNonNull(inventoryDAO, "inventoryDAO must not be null");
		Objects.requireNonNull(playerStigmasEquippedDAO, "playerStigmasEquippedDAO must not be null");
		Objects.requireNonNull(playerEquipmentSettingDAO, "playerEquipmentSettingDAO must not be null");
		Objects.requireNonNull(playerSettingsDAO, "playerSettingsDAO must not be null");
		Objects.requireNonNull(playerQuestListDAO, "playerQuestListDAO must not be null");
		Objects.requireNonNull(abyssRankDAO, "abyssRankDAO must not be null");
		Objects.requireNonNull(playerPunishmentsDAO, "playerPunishmentsDAO must not be null");
		Objects.requireNonNull(housesDAO, "housesDAO must not be null");
		Objects.requireNonNull(playerRegisteredItemsDAO, "playerRegisteredItemsDAO must not be null");
		Objects.requireNonNull(itemStoneListDAO, "itemStoneListDAO must not be null");
		Objects.requireNonNull(mailDAO, "mailDAO must not be null");
		Objects.requireNonNull(portalCooldownsDAO, "portalCooldownsDAO must not be null");
		Objects.requireNonNull(craftCooldownsDAO, "craftCooldownsDAO must not be null");
		Objects.requireNonNull(playerNpcFactionsDAO, "playerNpcFactionsDAO must not be null");
		Objects.requireNonNull(playerLunaShopDAO, "playerLunaShopDAO must not be null");
		Objects.requireNonNull(eventItemsDAO, "eventItemsDAO must not be null");
		Objects.requireNonNull(playerMacrossesDAO, "playerMacrossesDAO must not be null");
		Objects.requireNonNull(friendListDAO, "friendListDAO must not be null");
		Objects.requireNonNull(blockListDAO, "blockListDAO must not be null");
		Objects.requireNonNull(playerTitleListDAO, "playerTitleListDAO must not be null");
		Objects.requireNonNull(playerCreativityPointsDAO, "playerCreativityPointsDAO must not be null");
		Objects.requireNonNull(playerEventsWindowDAO, "playerEventsWindowDAO must not be null");
		Objects.requireNonNull(playerABDAO, "playerABDAO must not be null");
		Objects.requireNonNull(playerWardrobeDAO, "playerWardrobeDAO must not be null");
		Objects.requireNonNull(f2pDAO, "f2pDAO must not be null");
		Objects.requireNonNull(motionDAO, "motionDAO must not be null");
		Objects.requireNonNull(playerVarsDAO, "playerVarsDAO must not be null");
		Objects.requireNonNull(playerRecipesDAO, "playerRecipesDAO must not be null");
		Objects.requireNonNull(playerSkillSkinListDAO, "playerSkillSkinListDAO must not be null");
		Objects.requireNonNull(playerEffectsDAO, "playerEffectsDAO must not be null");
		Objects.requireNonNull(playerCooldownsDAO, "playerCooldownsDAO must not be null");
		Objects.requireNonNull(itemCooldownsDAO, "itemCooldownsDAO must not be null");
		Objects.requireNonNull(houseObjectCooldownsDAO, "houseObjectCooldownsDAO must not be null");
		Objects.requireNonNull(playerBindPointDAO, "playerBindPointDAO must not be null");
		Objects.requireNonNull(playerLifeStatsDAO, "playerLifeStatsDAO must not be null");
		Objects.requireNonNull(playerEmotionListDAO, "playerEmotionListDAO must not be null");
	}
}
