package com.aionemu.gameserver.services.teleport;


import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.boot.i18n.I18n;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.TransportConfig;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOTSPOT_TELEPORT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_OBJECT;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 热点传送服务，处理地图热点（快捷传送点）的读条、打断与扣费传送。
 * Hotspot teleport service handling cast, interrupt and paid travel via map hotspots.
 *
 * @author Ranastic
 */
@Slf4j
public class HotspotTeleportService {

	private static final int ELYOS_INGGISON_WORLD_ID = 210050000;
	private static final int ELYOS_INGGISON_MASTER_WORLD_ID = 210130000;
	private static final int ASMODIAN_GELKMAROS_WORLD_ID = 220070000;
	private static final int ASMODIAN_GELKMAROS_MASTER_WORLD_ID = 220140000;

    /**
     * -- SETTER --
     *  设置 Spring 实例提供者。
     *  Sets the Spring instance provider.
     *
     * @param provider 实例提供者 / Instance provider
     */
    @Setter
    private static volatile ObjectProvider<HotspotTeleportService> instanceProvider;

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
	public static HotspotTeleportService getInstance() {
		ObjectProvider<HotspotTeleportService> provider = instanceProvider;
		HotspotTeleportService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("HotspotTeleportService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

    /**
	 * 初始化并记录热点位置模板数量。
	 * Initializes and logs the number of hotspot location templates.
	 */
	public HotspotTeleportService() {
		int hotspotList = DataManager.HOTSPOT_LOCATION_DATA.size();
		log.info(I18n.get("log.162c2b724010", hotspotList));
	}

	/**
	 * 执行热点传送：按配置读条后传送并扣费，期间受击/异常/DoT 可打断。
	 * Performs hotspot teleport after the configured cast time, then travels and charges the fee; interruptible by attack/abnormal/DoT.
	 *
	 * @param player 玩家 / Player
	 * @param teleportId 热点传送点 ID / Hotspot teleport id
	 * @param price 基纳费用 / Kinah price
	 */
	public void doTeleport(final Player player, final int teleportId, final int price) {
		final int worldId = resolveLiveWorldId(
				DataManager.HOTSPOT_LOCATION_DATA.getHotspotlocationTemplate(teleportId).getMapId());
		final float getX = DataManager.HOTSPOT_LOCATION_DATA.getHotspotlocationTemplate(teleportId).getX();
		final float getY = DataManager.HOTSPOT_LOCATION_DATA.getHotspotlocationTemplate(teleportId).getY();
		final float getZ = DataManager.HOTSPOT_LOCATION_DATA.getHotspotlocationTemplate(teleportId).getZ();
		final int cooldown = TransportConfig.HOTSPOT_COOLDOWN_SECONDS;
		final int castTimeMillis = castTimeMillis(TransportConfig.HOTSPOT_CAST_TIME_SECONDS);
		final AtomicReference<Future<?>> castTask = new AtomicReference<>();
		Future<?> scheduledCast = GameThreadPoolServices.threadPoolManager().schedule(() -> {
			Future<?> teleportTask = GameThreadPoolServices.threadPoolManager().schedule(() -> {
				TeleportService2.teleportTo(player, worldId, getX, getY, getZ, player.getHeading(), TeleportAnimation.NO_ANIMATION);
				player.getInventory().decreaseKinah(price);
				PacketSendUtility.sendPacket(player,
						new SM_HOTSPOT_TELEPORT(player, 3, teleportId, cooldown));
			}, 1000);
			if (!player.getController().replaceTask(TaskId.HOTSPOT_TELEPORT, castTask.get(), teleportTask)) {
				return;
			}
			finishCastBar(player, 0);
			PacketSendUtility.broadcastPacketAndReceive(player,
					new SM_HOTSPOT_TELEPORT(player, 3, teleportId, cooldown));
			ActionObserver attackedObserver = new ActionObserver(ObserverType.ATTACKED) {
				@Override
				public void attacked(Creature creature) {
					if (player.getController().cancelTask(TaskId.HOTSPOT_TELEPORT) != null) {
						finishCastBar(player, 2);
					}
				}
			};
			player.getObserveController().addObserver(attackedObserver);
			player.setHotTeleObservers(attackedObserver);
			ActionObserver rideObserver = new ActionObserver(ObserverType.ABNORMALSETTED) {
				@Override
				public void abnormalsetted(AbnormalState state) {
					if (state.getId() > 0) {
						PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402444));
						if (player.getController().cancelTask(TaskId.HOTSPOT_TELEPORT) != null) {
							finishCastBar(player, 2);
						}
					}
				}
			};
			player.getObserveController().addObserver(rideObserver);
			player.setHotTeleObservers(rideObserver);
			ActionObserver dotAttackedObserver = new ActionObserver(ObserverType.DOT_ATTACKED) {
				@Override
				public void dotattacked(Creature creature, Effect dotEffect) {
					if (player.getController().cancelTask(TaskId.HOTSPOT_TELEPORT) != null) {
						finishCastBar(player, 2);
					}
				}
			};
			player.getObserveController().addObserver(dotAttackedObserver);
			player.setHotTeleObservers(dotAttackedObserver);
		}, castTimeMillis);
		castTask.set(scheduledCast);
		player.getController().addTask(TaskId.HOTSPOT_TELEPORT, scheduledCast);
		PacketSendUtility.broadcastPacket(player, new SM_HOTSPOT_TELEPORT(1, player.getObjectId(), teleportId));
		if (castTimeMillis > 0) {
			PacketSendUtility.sendPacket(player,
					new SM_USE_OBJECT(player.getObjectId(), player.getObjectId(), castTimeMillis, 1));
		}
	}

	/**
	 * 将热点配置中的镜像服世界 ID 归一为实际可玩的龙界世界。
	 * Normalizes mirror-server world ids from hotspot data to the live Balaurea worlds.
	 *
	 * @param worldId 原始目标世界 ID / Configured target world id
	 * @return 实际可玩世界 ID / Live world id
	 */
	static int resolveLiveWorldId(int worldId) {
        return switch (worldId) {
            case ELYOS_INGGISON_MASTER_WORLD_ID -> ELYOS_INGGISON_WORLD_ID;
            case ASMODIAN_GELKMAROS_MASTER_WORLD_ID -> ASMODIAN_GELKMAROS_WORLD_ID;
            default -> worldId;
        };
	}

	static int castTimeMillis(int castTimeSeconds) {
		return Math.multiplyExact(castTimeSeconds, 1000);
	}

	/**
	 * 关闭当前玩家的据点传送辅助读条。
	 * Closes the current player's hotspot teleport helper cast bar.
	 *
	 * @param player 玩家 / Player
	 */
	public static void cancelCastBar(Player player) {
		finishCastBar(player, 2);
	}

	private static void finishCastBar(Player player, int actionType) {
		PacketSendUtility.sendPacket(player,
				new SM_USE_OBJECT(player.getObjectId(), player.getObjectId(), 0, actionType));
	}
}
