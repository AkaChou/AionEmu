package com.aionemu.gameserver.services.siegeservice;

import com.aionemu.gameserver.services.SiegeService;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BATTLEFIELD_UNION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BATTLEFIELD_UNION_REGISTER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;
import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;
import lombok.Getter;

/**
 * 战场联盟服务，管理攻城期间联盟注册与进出。
 * Battlefield union service managing union register and enter/leave during sieges.
 */


@Getter
public class BattlefieldUnionService {
	/** 注入 Spring 实例提供者。 / Injects the Spring instance provider. */
	@Setter
	private static volatile ObjectProvider<BattlefieldUnionService> instanceProvider;

	/**
	 * 返回当前已注册人数。
	 * Returns the current registered size.
	 */
	public int size = 0;
	/**
	 * 返回联盟人数上限。
	 * Returns the maximum registered size.
	 * 人数上限 / max size
	 */
	public int maxSize = 24;
	public int requestId = 0;
	public int activeSiegeId;

	/**
	 * 玩家进入世界时处理。
	 * Handles player entering the world.
	 * @param player 玩家 / player
	 */
	public void onEnterWorld(Player player) {
		PacketSendUtility.sendPacket(player, new SM_BATTLEFIELD_UNION(getSiegeActive(), true, getSize(), getMaxSize()));
	}

	/**
	 * 返回当前进行的攻城 ID（无则保持上次值）。
	 * Returns the currently active siege id (keeps last value when none).
	 * @return 活跃攻城 ID / active siege id
	 */
	public int getSiegeActive() {
		if (SiegeService.getInstance().isSiegeInProgress(1011)) {
			activeSiegeId = 1011;
		} else if (SiegeService.getInstance().isSiegeInProgress(1131)) {
			activeSiegeId = 1131;
		} else if (SiegeService.getInstance().isSiegeInProgress(1132)) {
			activeSiegeId = 1132;
		} else if (SiegeService.getInstance().isSiegeInProgress(1141)) {
			activeSiegeId = 1141;
		} else if (SiegeService.getInstance().isSiegeInProgress(1221)) {
			activeSiegeId = 1221;
		} else if (SiegeService.getInstance().isSiegeInProgress(1231)) {
			activeSiegeId = 1231;
		} else if (SiegeService.getInstance().isSiegeInProgress(1241)) {
			activeSiegeId = 1241;
		} else if (SiegeService.getInstance().isSiegeInProgress(7011)) {
			activeSiegeId = 7011;
		}
		return activeSiegeId;
	}

	/**
	 * 攻城开始回调。
	 * Callback when siege starts.
	 * @param fortressId 要塞 ID / fortress id
	 */
	public void onSiegeStart(final int fortressId) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<>() {
			/**
			 * visit 方法。
			 * visit method.
			 * @param player 玩家 / player
			 */
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player,
					new SM_BATTLEFIELD_UNION(fortressId, true, getSize(), getMaxSize()));
			}
		});
	}

	/**
	 * 攻城结束回调。
	 * Callback when siege finishes.
	 * @param fortressId 要塞 ID / fortress id
	 */
	public void onSiegeFinish(final int fortressId) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<>() {
			/**
			 * visit 方法。
			 * visit method.
			 * @param player 玩家 / player
			 */
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player,
					new SM_BATTLEFIELD_UNION(fortressId, false, getSize(), getMaxSize()));
			}
		});
	}

	/**
	 * 注册处理。
	 * Handles registration.
	 * @param player 玩家 / player
	 * @param requestId 请求 ID / request id
	 * @param activeSiegeId 活跃攻城 ID / active siege id
	 */
	public void onRegister(Player player, int requestId, int activeSiegeId) {
		boolean register = false;
		PacketSendUtility.sendPacket(player,
				new SM_BATTLEFIELD_UNION(activeSiegeId, true, getSize() + 1, getMaxSize()));
		PacketSendUtility.sendPacket(player, new SM_BATTLEFIELD_UNION_REGISTER(requestId, true));
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1404004));
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1404005));
	}

	/**
	 * 返回当前请求 ID。
	 * Returns the current request id.
	 * @return 请求 ID / request id
	 */
	public int getrequestId() {
		return requestId;
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
	public static BattlefieldUnionService getInstance() {
		ObjectProvider<BattlefieldUnionService> provider = instanceProvider;
		BattlefieldUnionService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("BattlefieldUnionService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

}
