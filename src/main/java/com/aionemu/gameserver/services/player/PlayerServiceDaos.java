package com.aionemu.gameserver.services.player;

import java.util.Objects;

import com.aionemu.gameserver.persistence.dao.AbyssRankDao;
import com.aionemu.gameserver.persistence.dao.BlockDao;
import com.aionemu.gameserver.persistence.dao.CraftCooldownDao;
import com.aionemu.gameserver.persistence.dao.EventItemDao;
import com.aionemu.gameserver.persistence.dao.F2paccountDao;
import com.aionemu.gameserver.persistence.dao.FriendDao;
import com.aionemu.gameserver.persistence.dao.HouseDao;
import com.aionemu.gameserver.persistence.dao.HouseObjectCooldownDao;
import com.aionemu.gameserver.persistence.dao.HouseScriptDao;
import com.aionemu.gameserver.persistence.dao.InventoryDao;
import com.aionemu.gameserver.persistence.dao.ItemCooldownDao;
import com.aionemu.gameserver.persistence.dao.ItemStoneDao;
import com.aionemu.gameserver.persistence.dao.MailDao;
import com.aionemu.gameserver.persistence.dao.OldNameDao;
import com.aionemu.gameserver.persistence.dao.PlayerAppearanceDao;
import com.aionemu.gameserver.persistence.dao.PlayerAtreianBestiaryDao;
import com.aionemu.gameserver.persistence.dao.PlayerBindPointDao;
import com.aionemu.gameserver.persistence.dao.PlayerCooldownDao;
import com.aionemu.gameserver.persistence.dao.PlayerCpDao;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.persistence.dao.PlayerEffectDao;
import com.aionemu.gameserver.persistence.dao.PlayerEmotionDao;
import com.aionemu.gameserver.persistence.dao.PlayerEquipmentSettingDao;
import com.aionemu.gameserver.persistence.dao.PlayerEventsWindowDao;
import com.aionemu.gameserver.persistence.dao.PlayerLifeStatDao;
import com.aionemu.gameserver.persistence.dao.PlayerLunaShopDao;
import com.aionemu.gameserver.persistence.dao.PlayerMacrosseDao;
import com.aionemu.gameserver.persistence.dao.PlayerMinionDao;
import com.aionemu.gameserver.persistence.dao.PlayerMotionDao;
import com.aionemu.gameserver.persistence.dao.PlayerNpcFactionDao;
import com.aionemu.gameserver.persistence.dao.PlayerPasskeyDao;
import com.aionemu.gameserver.persistence.dao.PlayerPetDao;
import com.aionemu.gameserver.persistence.dao.PlayerPunishmentDao;
import com.aionemu.gameserver.persistence.dao.PlayerQuestDao;
import com.aionemu.gameserver.persistence.dao.PlayerRecipeDao;
import com.aionemu.gameserver.persistence.dao.PlayerRegisteredItemDao;
import com.aionemu.gameserver.persistence.dao.PlayerSettingDao;
import com.aionemu.gameserver.persistence.dao.PlayerShugoSweepDao;
import com.aionemu.gameserver.persistence.dao.PlayerSkillDao;
import com.aionemu.gameserver.persistence.dao.PlayerSkillSkinDao;
import com.aionemu.gameserver.persistence.dao.PlayerStigmasEquippedDao;
import com.aionemu.gameserver.persistence.dao.PlayerTitleDao;
import com.aionemu.gameserver.persistence.dao.PlayerTransformDao;
import com.aionemu.gameserver.persistence.dao.PlayerVarDao;
import com.aionemu.gameserver.persistence.dao.PlayerWardrobeDao;
import com.aionemu.gameserver.persistence.dao.PortalCooldownDao;


/**
 * 玩家核心服务的持久层依赖聚合：不可变 record，由 Spring 配置类声明式构造器注入装配，
 * 并对全部 DAO 依赖执行及早失败判空。
 * Aggregated persistence dependencies of the core player service: an immutable record wired
 * declaratively via Spring constructor injection, with fail-fast validation of every DAO dependency.
 */
public record PlayerServiceDaos(
		PlayerDao playerDAO,
		OldNameDao oldNamesDAO,
		PlayerAppearanceDao playerAppearanceDAO,
		PlayerSkillDao playerSkillListDAO,
		InventoryDao inventoryDAO,
		PlayerStigmasEquippedDao playerStigmasEquippedDAO,
		PlayerEquipmentSettingDao playerEquipmentSettingDAO,
		PlayerSettingDao playerSettingsDAO,
		PlayerQuestDao playerQuestListDAO,
		AbyssRankDao abyssRankDAO,
		PlayerPunishmentDao playerPunishmentsDAO,
		HouseDao housesDAO,
		PlayerRegisteredItemDao playerRegisteredItemsDAO,
		ItemStoneDao itemStoneListDAO,
		MailDao mailDAO,
		PortalCooldownDao portalCooldownsDAO,
		CraftCooldownDao craftCooldownsDAO,
		PlayerNpcFactionDao playerNpcFactionsDAO,
		PlayerLunaShopDao playerLunaShopDAO,
		EventItemDao eventItemsDAO,
		PlayerMacrosseDao playerMacrossesDAO,
		FriendDao friendListDAO,
		BlockDao blockListDAO,
		PlayerTitleDao playerTitleListDAO,
		PlayerCpDao playerCreativityPointsDAO,
		PlayerEventsWindowDao playerEventsWindowDAO,
		PlayerAtreianBestiaryDao playerABDAO,
		PlayerWardrobeDao playerWardrobeDAO,
		F2paccountDao f2pDAO,
		PlayerMotionDao motionDAO,
		PlayerVarDao playerVarsDAO,
		PlayerRecipeDao playerRecipesDAO,
		PlayerSkillSkinDao playerSkillSkinListDAO,
		PlayerEffectDao playerEffectsDAO,
		PlayerCooldownDao playerCooldownsDAO,
		ItemCooldownDao itemCooldownsDAO,
		HouseObjectCooldownDao houseObjectCooldownsDAO,
		PlayerBindPointDao playerBindPointDAO,
		PlayerLifeStatDao playerLifeStatsDAO,
		PlayerEmotionDao playerEmotionListDAO,
		PlayerPetDao playerPetsDAO,
		PlayerMinionDao playerMinionsDAO,
		PlayerShugoSweepDao playerShugoSweepDAO,
		HouseScriptDao houseScriptsDAO,
		PlayerPasskeyDao playerPasskeyDAO,
		PlayerTransformDao playerTransformDAO) {

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
		Objects.requireNonNull(playerPetsDAO, "playerPetsDAO must not be null");
		Objects.requireNonNull(playerMinionsDAO, "playerMinionsDAO must not be null");
		Objects.requireNonNull(playerShugoSweepDAO, "playerShugoSweepDAO must not be null");
		Objects.requireNonNull(houseScriptsDAO, "houseScriptsDAO must not be null");
		Objects.requireNonNull(playerPasskeyDAO, "playerPasskeyDAO must not be null");
		Objects.requireNonNull(playerTransformDAO, "playerTransformDAO must not be null");
	}
}
