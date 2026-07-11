package com.aionemu.gameserver.services.events;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerPassportsDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.event.AtreianPassport;
import com.aionemu.gameserver.model.templates.event.AtreianPassportRewards;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_PASSPORT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 艾特利亚护照服务，处理登录签到与护照奖励。
 * Atreian passport service handling login stamps and passport rewards.
 *
 * @author Rinzler (Encom)
 */

@Slf4j
public class AtreianPassportService {

	private static volatile ObjectProvider<AtreianPassportService> instanceProvider;
	private Map<Integer, AtreianPassport> basic = new HashMap<Integer, AtreianPassport>(1);
	private Map<Integer, AtreianPassport> anny = new HashMap<Integer, AtreianPassport>(1);
	public Map<Integer, AtreianPassport> data = new HashMap<Integer, AtreianPassport>(1);

	/**
	 * getPlayerPassports 方法。
	 * getPlayerPassports method.
	 *
	 * accountId
	 * result
	 */
	public Map<Integer, AtreianPassport> getPlayerPassports(int accountId) {
		Map<Integer, AtreianPassport> passports = new HashMap<Integer, AtreianPassport>();
		List<Integer> ids = DAOManager.getDAO(PlayerPassportsDAO.class).getPassports(accountId);
		for (Integer i : ids) {
			passports.put(i, data.get(i));
		}
		return passports;
	}

	/**
	 * 玩家登录时同步状态。
	 * Syncs state when a player logs in.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogin(Player player) {
		if (player == null) {
			return;
		}
		int atreianId = 8;
		int accountId = player.getPlayerAccount().getId();
		PlayerPassportsDAO dao = DAOManager.getDAO(PlayerPassportsDAO.class);
		Map<Integer, AtreianPassport> playerPassports = getPlayerPassports(accountId);

		// 若全部印章已领取则添加重置 / Added reset if all Stamps are received
		if (dao.getStamps(accountId, atreianId) == 28) {
			dao.updatePassport(accountId, atreianId, 0, true, new Timestamp(System.currentTimeMillis() - 86400000L));
		}

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(player.getCreationDate());
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);

		if (!playerPassports.containsKey(atreianId)) {
			final Timestamp now = new Timestamp(System.currentTimeMillis() - 86400000L);
			dao.insertPassport(accountId, atreianId, 0, now);
			PacketSendUtility.sendPacket(player, new SM_ATREIAN_PASSPORT(atreianId, 0, 1, false, day, month + 1, year));
		} else {
			int stamps = dao.getStamps(accountId, atreianId);
			Timestamp now2 = new Timestamp(System.currentTimeMillis());
			Timestamp lastStamp = dao.getLastStamp(accountId, atreianId);
			if (now2.getTime() - lastStamp.getTime() >= 86400000L) {
				DAOManager.getDAO(PlayerPassportsDAO.class).updatePassport(accountId, atreianId, stamps, false,
						lastStamp);
				PacketSendUtility.sendPacket(player,
						new SM_ATREIAN_PASSPORT(atreianId, stamps, 1, false, day, month + 1, year));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NEW_PASSPORT_AVAIBLE);
			} else {
				PacketSendUtility.sendPacket(player,
						new SM_ATREIAN_PASSPORT(atreianId, stamps, 1, true, day, month + 1, year));
			}
		}
	}

	/**
	 * 启动服务。
	 * Starts the service.
	 */
	public void onStart() {
		Map<Integer, AtreianPassport> raw = DataManager.ATREIAN_PASSPORT_DATA.getAll();
		if (raw.size() != 0) {
			getPassports(raw);
		} else {
			log.warn(I18n.get("log.2ea564e44e72"));
		}
		log.info(I18n.get("log.9ae8def154d9"));
	}

	/**
	 * @param player
	 * @param atreianId
	 */
	public void getReward(Player player, int atreianId) {
		AtreianPassport loginRewardTemplate = DataManager.ATREIAN_PASSPORT_DATA.getAtreianPassportId(atreianId);
		ZonedDateTime currentTime = ZonedDateTime.now();
		if (loginRewardTemplate == null || loginRewardTemplate.getActive() != 1
				|| currentTime.isBefore(loginRewardTemplate.getPeriodStart()) || currentTime.isAfter(loginRewardTemplate.getPeriodEnd())) {
			return;
		}
		int accountId = player.getPlayerAccount().getId();
		PlayerPassportsDAO dao = DAOManager.getDAO(PlayerPassportsDAO.class);
		if (!dao.getPassports(accountId).contains(atreianId)) {
			return;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(player.getCreationDate());
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		int stamps = dao.getStamps(accountId, atreianId);
		for (AtreianPassportRewards component : loginRewardTemplate.getAtreianPassportRewards()) {
			Timestamp now = new Timestamp(System.currentTimeMillis());
			Timestamp lastStamp = dao.getLastStamp(accountId, atreianId);
			if (now.getTime() - lastStamp.getTime() >= 86400000L) {
				if (component.getRewardItemNum() == stamps + 1) {
					ItemService.addItem(player, component.getRewardItem(), component.getRewardItemCount());
					// PacketSendUtility.sendPacket(player, new SM_ATREIAN_PASSPORT(atreianId,
					// OLD
					PacketSendUtility.sendPacket(player,
							new SM_ATREIAN_PASSPORT(atreianId, stamps + 1, 1, true, day, month + 1, year));
					DAOManager.getDAO(PlayerPassportsDAO.class).updatePassport(accountId, atreianId, stamps + 1, true,
							now);
				}
			}
		}
	}

	/**
	 * getPassports 方法。
	 * getPassports method.
	 *
	 * @param AtreianPassport 阿特雷亚通行证 / AtreianPassport
	 */
	public void getPassports(Map<Integer, AtreianPassport> raw) {
		data.putAll(raw);
		for (AtreianPassport atp : data.values()) {
			switch (atp.getAttendType()) {
			case BASIC:
				getBasicPassports(atp.getId(), atp);
				break;
			case ANNIVERSARY:
				getAnniversaryPassports(atp.getId(), atp);
				break;
			default:
				break;
			}
		}
		log.info(I18n.get("log.a141967cbb98", basic.size()));
		log.info(I18n.get("log.7ada0c3d82d6", anny.size()));
	}

	/**
	 * getPassports 方法。
	 * getPassports method.
	 *
	 * @param id ID / id
	 * atp
	 */
	public void getPassports(int id, AtreianPassport atp) {
		if (data.containsValue(id)) {
			return;
		}
		data.put(id, atp);
	}

	/**
	 * getBasicPassports 方法。
	 * getBasicPassports method.
	 *
	 * @param id ID / id
	 * atp
	 */
	public void getBasicPassports(int id, AtreianPassport atp) {
		if (basic.containsValue(id)) {
			return;
		}
		basic.put(id, atp);
	}

	/**
	 * getAnniversaryPassports 方法。
	 * getAnniversaryPassports method.
	 *
	 * @param id ID / id
	 * atp
	 */
	public void getAnniversaryPassports(int id, AtreianPassport atp) {
		if (anny.containsValue(id)) {
			return;
		}
		anny.put(id, atp);
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final AtreianPassportService instance = new AtreianPassportService();
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static AtreianPassportService getInstance() {
		ObjectProvider<AtreianPassportService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<AtreianPassportService> provider) {
		instanceProvider = provider;
	}
}
