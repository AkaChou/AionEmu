package com.aionemu.gameserver.services.events;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
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
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static final BoostEventService getInstance() {
		ObjectProvider<BoostEventService> provider = instanceProvider;
		BoostEventService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("BoostEventService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
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
}
