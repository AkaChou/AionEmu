package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.main.CacheConfig;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.LegionMemberDAO;
import com.aionemu.gameserver.dao.PlayerAppearanceDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerPunishmentsDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.AccountTime;
import com.aionemu.gameserver.model.account.CharacterBanInfo;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.collections.cachemap.CacheMap;
import com.aionemu.gameserver.utils.collections.cachemap.CacheMapFactory;
import com.aionemu.gameserver.world.World;

/**
 * 账号服务：作为 DAO 前端，负责加载与组装 Account 对象。
 * Account service front-end for DAOs responsible for loading and assembling Account objects.
 *
 * @author Luno
 * @modified cura
 */
@Slf4j
public class AccountService {


	/** 账号软引用缓存。 / Soft-reference account cache. */
	private static CacheMap<Integer, Account> accountsMap = CacheMapFactory.createSoftCacheMap("Account", "account");

	/**
	 * 按 ID 获取账号，缓存未命中时从数据库加载，并刷新会话相关字段。
	 * Returns the account for the given id, loading from DB on cache miss and refreshing session fields.
	 *
	 * 账号 ID / account id
	 * account name
	 * account time
	 * access level
	 * membership
	 * toll
	 * luna
	 *
	 * @return 账号 / account
	 */
	public static Account getAccount(int accountId, String accountName, AccountTime accountTime, byte accessLevel,
			byte membership, long toll, long luna, byte vipLevel, long vipExp, long vipExpireTime) {
		log.debug("[AS] request for account: " + accountId);

		Account account = accountsMap.get(accountId);
		if (account == null) {
			account = loadAccount(accountId);
			if (CacheConfig.CACHE_ACCOUNTS) {
				accountsMap.put(accountId, account);
			}
		}
		account.setName(accountName);
		account.setAccountTime(accountTime);
		account.setAccessLevel(accessLevel);
		account.setMembership(membership);
		account.setToll(toll);
		account.setLuna(luna);
		account.setVipLevel(vipLevel);
		account.setVipExp(vipExp);
		account.setVipExpireTime(vipExpireTime);
		removeDeletedCharacters(account);
		if (account.isEmpty()) {
			removeAccountWH(accountId);
		}
		return account;
	}

	/**
	 * 删除到期待删角色，并在启用阵营比例限制时更新比例。
	 * Removes characters whose deletion time has passed and updates race ratio when limited.
	 *
	 * @param account 账号 / account
	 */
	static void removeDeletedCharacters(Account account) {
		/* Removes chars that should be removed */
		Iterator<PlayerAccountData> it = account.iterator();
		while (it.hasNext()) {
			PlayerAccountData pad = it.next();
			Race race = pad.getPlayerCommonData().getRace();
			if (isDeletionDue(pad.getDeletionDate(), System.currentTimeMillis())) {
				it.remove();
				account.decrementCountOf(race);
				PlayerService.deletePlayerFromDB(pad.getPlayerCommonData().getPlayerObjId());
				if (GSConfig.ENABLE_RATIO_LIMITATION
						&& pad.getPlayerCommonData().getLevel() >= GSConfig.RATIO_MIN_REQUIRED_LEVEL) {
					if (account.getNumberOf(race) == 0) {
						GameServer.updateRatio(pad.getPlayerCommonData().getRace(), -1);
					}
				}
			}
		}
	}

	static boolean isDeletionDue(Timestamp deletionDate, long currentTimeMillis) {
		return deletionDate != null && deletionDate.getTime() <= currentTimeMillis;
	}

	/**
	 * 删除空账号的账号仓库。
	 * Deletes the account warehouse for an empty account.
	 *
	 * @param accountId 账号 ID / account id
	 */
	private static void removeAccountWH(int accountId) {
		DAOManager.getDAO(InventoryDAO.class).deleteAccountWH(accountId);
	}

	/**
	 * 从数据库加载账号下角色、外观、装备、军团与仓库数据。
	 * Loads account characters, appearance, equipment, legion and warehouse from the database.
	 *
	 * @param accountId 账号 ID / account id
	 * @return 账号 / account
	 */
	public static Account loadAccount(int accountId) {
		Account account = new Account(accountId);

		PlayerDAO playerDAO = DAOManager.getDAO(PlayerDAO.class);
		PlayerAppearanceDAO appereanceDAO = DAOManager.getDAO(PlayerAppearanceDAO.class);

		List<Integer> playerIdList = playerDAO.getPlayerOidsOnAccount(accountId);

		for (int playerId : playerIdList) {
			PlayerCommonData playerCommonData = playerDAO.loadPlayerCommonData(playerId);
			CharacterBanInfo cbi = DAOManager.getDAO(PlayerPunishmentsDAO.class).getCharBanInfo(playerId);
			if (playerCommonData.isOnline()) {
				if (com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId) == null) {
					playerCommonData.setOnline(false);
					log.warn(I18n.get("log.530f2a71906a", playerCommonData.getName()));
				}
			}
			PlayerAppearance appereance = appereanceDAO.load(playerId);

			LegionMember legionMember = DAOManager.getDAO(LegionMemberDAO.class).loadLegionMember(playerId);

			/**
			 * 仅加载角色选择界面展示用的装备及其镶嵌石。
			 * Load only equipment and its stones to display on character selection screen.
			 */
			List<Item> equipment = DAOManager.getDAO(InventoryDAO.class).loadEquipment(playerId);

			PlayerAccountData acData = new PlayerAccountData(playerCommonData, cbi, appereance, equipment,
					legionMember);
			playerDAO.setCreationDeletionTime(acData);

			account.addPlayerAccountData(acData);

			if (account.getAccountWarehouse() == null) {
				Storage accWarehouse = DAOManager.getDAO(InventoryDAO.class).loadStorage(playerId,
						StorageType.ACCOUNT_WAREHOUSE);
				ItemService.loadItemStones(accWarehouse.getItems());
				account.setAccountWarehouse(accWarehouse);
			}
		}

		// 新账号：创建空账号仓库。 / For new accounts - create empty account warehouse
		if (account.getAccountWarehouse() == null) {
			account.setAccountWarehouse(new PlayerStorage(StorageType.ACCOUNT_WAREHOUSE));
		}
		return account;
	}
}
