package com.aionemu.gameserver.services.instance;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.services.AutoGroupService;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;


/**
 * 大竞技场训练营报名服务，管理开启窗口与冷却。
 * Grand Arena Training Camp registration service managing open windows and cooldowns.
 */

@Slf4j

public class GrandArenaTrainingCampService {
	private final AutoGroupService autoGroupService;

	/**
	 * 使用 Spring 提供的 AutoGroupService 构造服务。
	 * Constructs the service with the Spring-provided AutoGroupService.
	 *
	 * @param autoGroupService AutoGroupService 实例 / AutoGroupService instance
	 */
	public GrandArenaTrainingCampService(@NonNull AutoGroupService autoGroupService) {
		this.autoGroupService = Objects.requireNonNull(autoGroupService, "autoGroupService must not be null");
	}
	private static volatile ObjectProvider<GrandArenaTrainingCampService> instanceProvider;
	private static volatile GrandArenaTrainingCampService resolvedInstance;
	private boolean registerAvailable;
	private final List<Integer> playersWithCooldown = new ArrayList<>();
	public static final byte minLevel = 66, capLevel = 76;
	public static final int maskId = 127;

	/**
	 * 初始化大竞技场训练营报名：按配置调度开启报名。
	 * Initializes Grand Arena Training Camp registration by scheduling open windows per config.
	 */
	public void initGrandArenaTrainingCamp() {
		if (AutoGroupConfig.GRAND_ARENA_TRAINING_CAMP_ENABLED) {
			log.info(I18n.get("log.5e476c3f89e2"));
			// IDTM_LobbyP01 SAT-SUN "6PM-0AM"
            CronService.requireCurrent().schedule(this::startGrandArenaTrainingCampRegistration, AutoGroupConfig.GRAND_ARENA_TRAINING_CAMP_SCHEDULE_EVENING);
		}
	}

	private void startUregisterGrandArenaTrainingCampTask() {
        ThreadPoolManager.getInstance().schedule(() -> {
			 registerAvailable = false;
			 playersWithCooldown.clear();
			 autoGroupService.unRegisterInstance(maskId);
			 Iterator<Player> iter = World.getInstance().getPlayersIterator();
			 while (iter.hasNext()) {
				 Player player = iter.next();
				 if (player.getLevel() > minLevel) {
					 int instanceMaskId = getInstanceMaskId(player);
					 if (instanceMaskId > 0) {
						 PacketSendUtility.sendPacket(player,
								 new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon, true));
					 }
				 }
			 }
		 }, AutoGroupConfig.GRAND_ARENA_TRAINING_CAMP_TIMER * 60 * 1000);
	}

	private void startGrandArenaTrainingCampRegistration() {
		this.registerAvailable = true;
		startUregisterGrandArenaTrainingCampTask();
		Iterator<Player> iter = World.getInstance().getPlayersIterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			if (player.getLevel() > minLevel && player.getLevel() < capLevel) {
				int instanceMaskId = getInstanceMaskId(player);
				if (instanceMaskId > 0) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					// 황금의 템페르 훈련소 파티전에 참가할 수 있습니다.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDTM_Lobbyp_01);
				}
			}
		}
	}

	/**
	 * 报名是否可用。
	 * Whether registration is available.
	 * @return 结果 / result
	 */
	public boolean isGrandArenaTrainingCampAvailable() {
		return this.registerAvailable;
	}

	/**
	 * 获取玩家的报名掩码 ID；等级不符时返回 0。
	 * Returns the registration mask id for the player, or 0 if level mismatch.
	 * 玩家 / player
	 * @return 结果 / result
	 */
	public byte getInstanceMaskId(Player player) {
		int level = player.getLevel();
		if (level < minLevel || level >= capLevel) {
			return 0;
		}
		return maskId;
	}

	/**
	 * 添加冷却。
	 * Adds a cooldown.
	 * @param player 玩家 / player
	 */
	public void addCoolDown(Player player) {
		this.playersWithCooldown.add(player.getObjectId());
	}

	/**
	 * 是否处于冷却。
	 * Whether cooldown is active.
	 * 玩家 / player
	 * @return 结果 / result
	 */
	public boolean hasCoolDown(Player player) {
		return this.playersWithCooldown.contains(player.getObjectId());
	}

	/**
	 * 显示报名窗口。
	 * Shows the registration window.
	 * @param player 玩家 / player
	 * @param instanceMaskId 副本掩码 ID / instance mask id
	 */
	public void showWindow(Player player, byte instanceMaskId) {
		if (getInstanceMaskId(player) != instanceMaskId) {
			return;
		}
		if (!this.playersWithCooldown.contains(player.getObjectId())) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId));
		}
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static GrandArenaTrainingCampService getInstance() {
		GrandArenaTrainingCampService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<GrandArenaTrainingCampService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("GrandArenaTrainingCampService 未由 Spring 提供："
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
	public static void setInstanceProvider(ObjectProvider<GrandArenaTrainingCampService> instanceProvider) {
		GrandArenaTrainingCampService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

}
