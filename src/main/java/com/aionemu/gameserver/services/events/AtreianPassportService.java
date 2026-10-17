package com.aionemu.gameserver.services.events;


import com.aionemu.boot.i18n.I18n;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.event.AtreianPassport;
import com.aionemu.gameserver.model.templates.event.AtreianPassportRewards;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_PASSPORT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.persistence.dao.PlayerPassportDao;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 亚特雷亚通行证服务，处理登录印章与通行证奖励。
 * Atreian passport service handling login stamps and passport rewards.
 *
 * @author Rinzler (Encom)
 */

@Slf4j
public class AtreianPassportService {

	private static volatile ObjectProvider<AtreianPassportService> instanceProvider;
	private static volatile AtreianPassportService resolvedInstance;
	private final PlayerPassportDao playerPassportsDAO;
	private final Map<Integer, AtreianPassport> basic = new HashMap<>(1);
	private final Map<Integer, AtreianPassport> anny = new HashMap<>(1);
	public Map<Integer, AtreianPassport> data = new HashMap<>(1);

	/**
	 * 使用指定的 PlayerPassportsDAO 构造通行证服务。
	 * Constructs the passport service with the given PlayerPassportsDAO.
	 *
	 * @param playerPassportsDAO 通行证数据访问对象 / passport DAO
	 */
	public AtreianPassportService(@NonNull PlayerPassportDao playerPassportsDAO) {
		this.playerPassportsDAO = playerPassportsDAO;
	}

	/**
	 * getPlayerPassports 方法。
	 * getPlayerPassports method.
	 * accountId
	 * result
	 */
	public Map<Integer, AtreianPassport> getPlayerPassports(int accountId) {
		Map<Integer, AtreianPassport> passports = new HashMap<>();
		List<Integer> ids = playerPassportsDAO.getPassports(accountId);
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
		int accountId = player.getPlayerAccount().getId();
		Map<Integer, AtreianPassport> playerPassports = getPlayerPassports(accountId);
		for (AtreianPassport template : activePassports()) {
			syncPassport(player, accountId, template, playerPassports);
		}
	}

	/**
	 * 返回全部激活中的签到模板（active=1，按 ID 升序）。
	 * All active passport templates (active=1, ordered by id).
	 */
	private List<AtreianPassport> activePassports() {
		return data.values().stream()
			.filter(p -> p.getActive() == 1)
			.sorted(java.util.Comparator.comparingInt(AtreianPassport::getId))
			.toList();
	}

	/**
	 * 同步单个签到（插入新行、跨自然日解锁、下发状态包）。
	 * Syncs a single passport (insert row, unlock on a new calendar day, send state packet).
	 */
	private void syncPassport(Player player, int accountId, AtreianPassport template,
			Map<Integer, AtreianPassport> playerPassports) {
		int atreianId = template.getId();
		// 实测结论：客户端 BASIC(0) 路径按服务器年月取本地月历数据（5.8 客户端仅 2017 年 → 2026 年下发全黑无图标）；
		// ANNIVERSARY(1) 路径按 eventId 索引本地数据填 28 槽网格（格数=本地该期奖励数），标题亦取本地该期。
		// 故固定下发 1，由 eventId 选择期数 / Send ANNIVERSARY(1): the BASIC path reads local data by calendar
		// month (empty for 2026), while ANNIVERSARY indexes by eventId and fills the 28-slot grid from it.
		int attendTypeId = 1;

		// 多格月历型签到印章集满则重置（满额 = 该签到的奖励格数）；单格型（如 passport 9）领完即止不重置
		// Multi-stamp passports reset once full; single-reward passports (e.g. passport 9) end when claimed out
		int maxStamps = template.getAtreianPassportRewards().size();
		if (maxStamps > 1 && playerPassportsDAO.getStamps(accountId, atreianId) >= maxStamps) {
			playerPassportsDAO.updatePassport(accountId, atreianId, 0, true, new Timestamp(System.currentTimeMillis() - 86400000L));
		}

		// 真端下发的是服务器当前日期，客户端据此渲染当月月历
		// Retail sends the current server date; the client renders the current month from it
		LocalDate today = LocalDate.now();
		int day = today.getDayOfMonth();
		int month = today.getMonthValue();
		int year = today.getYear();

		if (!playerPassports.containsKey(atreianId)) {
			final Timestamp now = new Timestamp(System.currentTimeMillis() - 86400000L);
			playerPassportsDAO.insertPassport(accountId, atreianId, 0, now);
			PacketSendUtility.sendPacket(player,
				new SM_ATREIAN_PASSPORT(atreianId, 0, attendTypeId, false, day, month, year));
		} else {
			int stamps = playerPassportsDAO.getStamps(accountId, atreianId);
			Timestamp lastStamp = playerPassportsDAO.getLastStamp(accountId, atreianId);
			if (isNewDay(lastStamp)) {
				playerPassportsDAO.updatePassport(accountId, atreianId, stamps, false, lastStamp);
				PacketSendUtility.sendPacket(player,
					new SM_ATREIAN_PASSPORT(atreianId, stamps, attendTypeId, false, day, month, year));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NEW_PASSPORT_AVAIBLE);
			} else {
				PacketSendUtility.sendPacket(player,
					new SM_ATREIAN_PASSPORT(atreianId, stamps, attendTypeId, true, day, month, year));
			}
		}
	}

	/**
	 * 判定上次盖章是否落在今天之前的自然日（本地时区），即今日是否可再领一次。
	 * Whether the last stamp falls on a previous calendar day (local zone), i.e. another claim is allowed today.
	 */
	private static boolean isNewDay(Timestamp lastStamp) {
		return lastStamp == null || !LocalDate.now().equals(lastStamp.toLocalDateTime().toLocalDate());
	}

	/**
	 * 启动服务。
	 * Starts the service.
	 */
	public void onStart() {
		Map<Integer, AtreianPassport> raw = DataManager.ATREIAN_PASSPORT_DATA.getAll();
		if (!raw.isEmpty()) {
			getPassports(raw);
		} else {
			log.warn(I18n.get("log.2ea564e44e72"));
		}
		log.info(I18n.get("log.9ae8def154d9"));
	}

	public void getReward(Player player, int atreianId) {
		AtreianPassport loginRewardTemplate = DataManager.ATREIAN_PASSPORT_DATA.getAtreianPassportId(atreianId);
		ZonedDateTime currentTime = ZonedDateTime.now();
		if (loginRewardTemplate == null || loginRewardTemplate.getActive() != 1
			|| currentTime.isBefore(loginRewardTemplate.getPeriodStart()) || currentTime.isAfter(loginRewardTemplate.getPeriodEnd())) {
			return;
		}
		int accountId = player.getPlayerAccount().getId();
		if (!playerPassportsDAO.getPassports(accountId).contains(atreianId)) {
			return;
		}
		// 真端下发的是服务器当前日期，客户端据此渲染当月月历
		// Retail sends the current server date; the client renders the current month from it
		LocalDate today = LocalDate.now();
		int day = today.getDayOfMonth();
		int month = today.getMonthValue();
		int year = today.getYear();
		int attendTypeId = 1; // 见 syncPassport 注释 / see note in syncPassport
		int stamps = playerPassportsDAO.getStamps(accountId, atreianId);
		Timestamp lastStamp = playerPassportsDAO.getLastStamp(accountId, atreianId);
		if (!isNewDay(lastStamp)) {
			return;
		}
		for (AtreianPassportRewards component : loginRewardTemplate.getAtreianPassportRewards()) {
			// 至多一个格子的序号等于 stamps+1，命中即发 / at most one slot equals stamps+1
			if (component.getRewardItemNum() == stamps + 1) {
				ItemService.addItem(player, component.getRewardItem(), component.getRewardItemCount());
				PacketSendUtility.sendPacket(player,
					new SM_ATREIAN_PASSPORT(atreianId, stamps + 1, attendTypeId, true, day, month, year));
				playerPassportsDAO.updatePassport(accountId, atreianId, stamps + 1, true, new Timestamp(System.currentTimeMillis()));
			}
		}
	}

	/**
	 * getPassports 方法。
	 * getPassports method.
	 *
	 * @param raw 原始通行证映射 / raw passport data
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
	 *           atp
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
	 *           atp
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
	 *           atp
	 */
	public void getAnniversaryPassports(int id, AtreianPassport atp) {
		if (anny.containsValue(id)) {
			return;
		}
		anny.put(id, atp);
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *                               when no provider or bean is available
	 */
	public static AtreianPassportService getInstance() {
		AtreianPassportService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<AtreianPassportService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("AtreianPassportService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<AtreianPassportService> instanceProvider) {
		AtreianPassportService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

}
