package com.aionemu.gameserver.services.events;


import com.aionemu.boot.i18n.I18n;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
import com.aionemu.gameserver.persistence.entity.PlayerPassportPo;
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
		Map<Integer, PlayerPassportPo> progress = playerPassportsDAO.getPassportProgress(accountId);
		List<SM_ATREIAN_PASSPORT.Record> records = new ArrayList<>();
		boolean newDay = false;
		for (AtreianPassport template : activePassports()) {
			newDay |= syncPassport(accountId, template, progress, records);
		}
		if (!records.isEmpty()) {
			LocalDate today = LocalDate.now();
			PacketSendUtility.sendPacket(player, new SM_ATREIAN_PASSPORT(records, today.getDayOfMonth(),
				today.getMonthValue(), today.getYear()));
			if (newDay) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NEW_PASSPORT_AVAIBLE);
			}
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
	 * 同步单个签到（插入新行、跨自然日解锁、追加下发记录）。
	 * Syncs a single passport (insert row, unlock on a new calendar day, append the outgoing record).
	 * @return 本次是否跨自然日解锁 / whether the passport unlocked on a new calendar day
	 */
	private boolean syncPassport(int accountId, AtreianPassport template,
			Map<Integer, PlayerPassportPo> progress, List<SM_ATREIAN_PASSPORT.Record> records) {
		int atreianId = template.getId();
		// 真端下发的是签到类型枚举（0=anniversary, 1=basic, 2=pc_basic），由 eventId 选择期数。
		// 历史实测（旧实现留下的结论，尚未用本轮数据复核）：客户端 BASIC 路径按服务器年月取本地月历数据，
		// 5.8 客户端只有 2017 年数据，故非 2017 年份下发 BASIC 可能全黑无图标；ANNIVERSARY 路径按 eventId
		// 索引本地数据填槽位，标题同源。旧实现因此对所有签到固定下发 1，但它把 1 记作 ANNIVERSARY，
		// 与真端枚举（anniversary=0 / basic=1）矛盾——id=8 在两种口径下都发 1 故无差异，
		// 风险集中在本轮新激活的 id=9（anniversary，下发 0）。上线前须实机确认 id=9 面板渲染。
		// Retail sends the attendance-type enum (0=anniversary, 1=basic, 2=pc_basic); eventId selects the
		// period. The old implementation always sent 1 and labelled it ANNIVERSARY, which contradicts the
		// retail enum, so id=8 is unaffected either way but the newly activated id=9 (anniversary, sends 0)
		// needs a live check before shipping.
		int attendTypeId = template.getAttendType().getId();

		// 多格月历型签到印章集满则重置（满额 = 该签到的奖励格数）；单格型（如 passport 9）领完即止不重置
		// Multi-stamp passports reset once full; single-reward passports (e.g. passport 9) end when claimed out
		int maxStamps = template.getAtreianPassportRewards().size();
		PlayerPassportPo row = progress.get(atreianId);
		int stamps = row == null || row.getStamps() == null ? 0 : row.getStamps();
		if (maxStamps > 1 && stamps >= maxStamps) {
			playerPassportsDAO.updatePassport(accountId, atreianId, 0, true, new Timestamp(System.currentTimeMillis() - 86400000L));
			stamps = 0;
		}

		if (row == null) {
			final Timestamp now = new Timestamp(System.currentTimeMillis() - 86400000L);
			playerPassportsDAO.insertPassport(accountId, atreianId, 0, now);
			records.add(new SM_ATREIAN_PASSPORT.Record(atreianId, 0, attendTypeId, false));
			return false;
		}
		if (isNewDay(row.getLastStamp())) {
			playerPassportsDAO.updatePassport(accountId, atreianId, stamps, false, row.getLastStamp());
			records.add(new SM_ATREIAN_PASSPORT.Record(atreianId, stamps, attendTypeId, false));
			return true;
		}
		records.add(new SM_ATREIAN_PASSPORT.Record(atreianId, stamps, attendTypeId, true));
		return false;
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
		log.debug(I18n.get("log.9ae8def154d9"));
	}

	public void getReward(Player player, int atreianId) {
		AtreianPassport loginRewardTemplate = DataManager.ATREIAN_PASSPORT_DATA.getAtreianPassportId(atreianId);
		ZonedDateTime currentTime = ZonedDateTime.now();
		if (loginRewardTemplate == null || loginRewardTemplate.getActive() != 1
			|| currentTime.isBefore(loginRewardTemplate.getPeriodStart()) || currentTime.isAfter(loginRewardTemplate.getPeriodEnd())) {
			return;
		}
		int accountId = player.getPlayerAccount().getId();
		Map<Integer, PlayerPassportPo> progress = playerPassportsDAO.getPassportProgress(accountId);
		PlayerPassportPo row = progress.get(atreianId);
		if (row == null) {
			return;
		}
		// 真端下发的是服务器当前日期，客户端据此渲染当月月历
		// Retail sends the current server date; the client renders the current month from it
		LocalDate today = LocalDate.now();
		int stamps = row.getStamps() == null ? 0 : row.getStamps();
		if (!isNewDay(row.getLastStamp())) {
			return;
		}
		for (AtreianPassportRewards component : loginRewardTemplate.getAtreianPassportRewards()) {
			// 至多一个格子的序号等于 stamps+1，命中即发 / at most one slot equals stamps+1
			if (component.getRewardItemNum() == stamps + 1) {
				ItemService.addItem(player, component.getRewardItem(), component.getRewardItemCount());
				playerPassportsDAO.updatePassport(accountId, atreianId, stamps + 1, true, new Timestamp(System.currentTimeMillis()));
			}
		}
		// 真端领奖后一次性回发全部签到状态（S_LOGIN_EVENT_LIST），客户端据此刷新整个面板
		// Retail replies with the whole attendance list after a claim, so the client can refresh the panel
		List<SM_ATREIAN_PASSPORT.Record> records =
			buildRecords(activePassports(), playerPassportsDAO.getPassportProgress(accountId));
		PacketSendUtility.sendPacket(player, new SM_ATREIAN_PASSPORT(records, today.getDayOfMonth(),
			today.getMonthValue(), today.getYear()));
	}

	/**
	 * 按库内真实进度构建下发记录：计数与「今日已领」一律取库内值，不按领取意图推算。
	 * Builds the outgoing records from stored progress; a miss writes nothing, so the panel always matches the DB.
	 * @param templates 激活中的签到模板 / active passport templates
	 * @param progress 账号进度行 / stored progress rows by passport id
	 * @return 下发记录 / outgoing records
	 */
	static List<SM_ATREIAN_PASSPORT.Record> buildRecords(List<AtreianPassport> templates,
			Map<Integer, PlayerPassportPo> progress) {
		List<SM_ATREIAN_PASSPORT.Record> records = new ArrayList<>(templates.size());
		for (AtreianPassport template : templates) {
			int id = template.getId();
			PlayerPassportPo row = progress.get(id);
			int count = row == null || row.getStamps() == null ? 0 : row.getStamps();
			boolean claimed = row != null && !isNewDay(row.getLastStamp());
			records.add(new SM_ATREIAN_PASSPORT.Record(id, count, template.getAttendType().getId(), claimed));
		}
		return records;
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
				case BASIC, PC_BASIC -> getBasicPassports(atp.getId(), atp);
				case ANNIVERSARY -> getAnniversaryPassports(atp.getId(), atp);
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
		data.putIfAbsent(id, atp);
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
