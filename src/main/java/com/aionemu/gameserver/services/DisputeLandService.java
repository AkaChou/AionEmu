package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DISPUTE_LAND;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneAttributes;

/**
 * 纷争之地服务：按计划开启/关闭，并在指定地图广播与同步 PvP 状态。
 * Dispute Land service: schedules open/close and broadcasts/syncs PvP state for listed maps.
 * @author Rinzler (Encom)
 */
@Slf4j

public class DisputeLandService {
	private static volatile ObjectProvider<DisputeLandService> instanceProvider;
	private static volatile DisputeLandService resolvedInstance;
    /**
     * -- GETTER --
     *  是否处于激活状态。
     *  Whether Dispute Land is currently active.
     * 若 active 则为 true / true if active
     */
    @Getter
    private boolean active;
	private final List<Integer> worlds = new ArrayList<>();

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public DisputeLandService() {
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static DisputeLandService getInstance() {
		DisputeLandService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<DisputeLandService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("DisputeLandService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<DisputeLandService> instanceProvider) {
		DisputeLandService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

	/**
	 * 初始化纷争之地：注册世界 ID，并在启用时按 cron 调度启停。
	 * Initializes Dispute Land: registers world IDs and schedules open/close when enabled.
	 */
	public void initDisputeLand() {
		if (CustomConfig.DISPUTE_LAND_ENABLED) {
			log.info(I18n.get("log.87d65384cfca"));
			CronService.requireCurrent().schedule(() -> {
				if (isActive()) {
					ThreadPoolManager.getInstance().schedule(() -> setActive(false), (long) CustomConfig.DISPUTE_LAND_DURATION * 3600 * 1000);
				}
			}, () -> CustomConfig.DISPUTE_LAND_SCHEDULE);
		}
		worlds.add(210020000); // Eltnen.
		worlds.add(210040000); // Heiron.
		worlds.add(210050000); // Inggison.
		worlds.add(210060000); // Theobomos.
		worlds.add(220020000); // Morheim.
		worlds.add(220040000); // Beluslan.
		worlds.add(220050000); // Brusthonin.
		worlds.add(220070000); // Gelkmaros.
		worlds.add(220140000); // Gelkmaros [Master Server].
		// 4.7
		worlds.add(600090000); // Kaldor.
		worlds.add(600100000); // Levinshor.
		// 4.8
		worlds.add(210070000); // Cygnea.
		worlds.add(220080000); // Enshar.
		// 5.0
		worlds.add(210100000); // Iluma.
		worlds.add(220110000); // Norsvold.
	}

    /**
	 * 设置激活状态，同步世界 PvP 选项并向全体玩家广播。
	 * Sets active state, syncs world PvP options, and broadcasts to all players.
	 * @param value 是否激活 / whether active
	 */
	public void setActive(boolean value) {
		active = value;
		syncState();
		broadcast();
	}

	private void syncState() {
		for (int world : worlds) {
			if (world == 210020000 || // Eltnen.
					world == 210040000 || // Heiron.
					world == 210050000 || // Inggison.
					world == 210060000 || // Theobomos.
					world == 210070000 || // Cygnea.
					world == 220020000 || // Morheim.
					world == 220040000 || // Beluslan.
					world == 220050000 || // Brusthonin.
					world == 220070000 || // Gelkmaros.
					world == 220140000 || // Gelkmaros [Master Server].
					world == 220080000 || // Enshar.
					world == 210100000 || // Iluma.
					world == 220110000 || // Norsvold.
					world == 600090000 || // Kaldor.
					world == 600100000) { // Levinshor.
				continue;
			}
			if (active) {
				World.getInstance().getWorldMap(world).setWorldOption(ZoneAttributes.PVP_ENABLED);
			} else {
				World.getInstance().getWorldMap(world).removeWorldOption(ZoneAttributes.PVP_ENABLED);
			}
		}
	}

	private void broadcast(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DISPUTE_LAND(worlds, active));
	}

	private void broadcast() {
		World.getInstance().doOnAllPlayers(this::broadcast);
	}

	/**
	 * 玩家登录时下发当前纷争之地状态。
	 * Sends current Dispute Land state to the player on login.
	 * logging-in player
	 */
	public void onLogin(Player player) {
		broadcast(player);
	}
}
