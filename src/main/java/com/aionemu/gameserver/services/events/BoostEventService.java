package com.aionemu.gameserver.services.events;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.bonus_service.BoostEventBonus;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.templates.event.BoostEvents;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BOOST_EVENTS;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 增益活动服务，按活动配置向玩家下发增益包。
 * Boost event service delivering configured boost packets to players.
 *
 * @author Rinzler (Encom)
 */

@Slf4j
public class BoostEventService implements StatOwner {

	private static BoostEventBonus bonus;

	private static volatile ObjectProvider<BoostEventService> instanceProvider;

	public Map<Integer, BoostEvents> data = new HashMap<Integer, BoostEvents>(1);

	/**
	 * 启动服务。
	 * Starts the service.
	 */
	public void onStart() {
		Map<Integer, BoostEvents> raw = DataManager.BOOST_EVENT_DATA.getAll();
		if (raw.size() != 0) {
			getBoostEvent(raw);
		}
	}

	/**
	 * 发送数据包。
	 * Sends a packet.
	 *
	 * @param player 玩家 / player
	 */
	public void sendPacket(Player player) {
		Map<Integer, BoostEvents> boost = getCurrentBoost();
		for (BoostEvents be : boost.values()) {
			long start = be.getStartDate().toInstant().toEpochMilli() / 1000;
			long end = be.getEndDate().toInstant().toEpochMilli() / 1000;
			PacketSendUtility.sendPacket(player, new SM_BOOST_EVENTS(be.getBuffId(), be.getBuffValue(), start, end));
		}
	}

	/**
	 * getCurrentBoost 方法。
	 * getCurrentBoost method.
	 * result
	 */
	public Map<Integer, BoostEvents> getCurrentBoost() {
		Map<Integer, BoostEvents> boost = new HashMap<Integer, BoostEvents>();
		ZonedDateTime now = ZonedDateTime.now();
		for (BoostEvents be : data.values()) {
			if (be.getStartDate().isBefore(now) && be.getEndDate().isAfter(now)) {
				boost.put(be.getId(), be);
			}
		}
		return boost;
	}

	/**
	 * getBoostEvent 方法。
	 * getBoostEvent method.
	 *
	 * @param id ID / id
	 * @param be 战斗事件 / be
	 */
	public void getBoostEvent(int id, BoostEvents be) {
		if (data.containsValue(id)) {
			return;
		}
		data.put(id, be);
	}

	/**
	 * getBoostEvent 方法。
	 * getBoostEvent method.
	 *
	 * BoostEvents
	 */
	public void getBoostEvent(Map<Integer, BoostEvents> raw) {
		data.putAll(raw);
		for (BoostEvents be : data.values()) {
			getBoostEvent(be.getId(), be);
		}
		log.info(I18n.get("log.91239132a81e", data.size()));
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static final BoostEventService getInstance() {
		ObjectProvider<BoostEventService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * @param instanceProvider 副本提供者 / instanceProvider
	 */
	public static void setInstanceProvider(ObjectProvider<BoostEventService> instanceProvider) {
		BoostEventService.instanceProvider = instanceProvider;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final BoostEventService instance = new BoostEventService();
	}
}
