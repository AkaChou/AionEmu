package com.aionemu.gameserver.services.siegeservice;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BATTLEFIELD_UNION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BATTLEFIELD_UNION_REGISTER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 战场联盟服务，管理攻城期间联盟注册与进出。
 * Battlefield union service managing union register and enter/leave during sieges.
 */


public class BattlefieldUnionService {
	private static final BattlefieldUnionService instance = new BattlefieldUnionService();
	private static volatile ObjectProvider<BattlefieldUnionService> instanceProvider;

	public int size = 0;
	public int maxSize = 24;
	public int requestId = 0;
	public int activeSiegeId;

	/**
	 * 玩家进入世界时处理。
	 * Handles player entering the world.
	 *
	 * @param player 玩家 / player
	 */
	public void onEnterWorld(Player player) {
		PacketSendUtility.sendPacket(player, new SM_BATTLEFIELD_UNION(getSiegeActive(), true, getSize(), getMaxSize()));
	}

	/**
	 * 返回当前进行的攻城 ID（无则保持上次值）。
	 * Returns the currently active siege id (keeps last value when none).
	 *
	 * @return 活跃攻城 ID / active siege id
	 */
	public int getSiegeActive() {
		if (GameFeatureServices.siegeService().isSiegeInProgress(1011)) {
			activeSiegeId = 1011;
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1131)) {
			activeSiegeId = 1131;
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1132)) {
			activeSiegeId = 1132;
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1141)) {
			activeSiegeId = 1141;
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1221)) {
			activeSiegeId = 1221;
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1231)) {
			activeSiegeId = 1231;
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1241)) {
			activeSiegeId = 1241;
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(7011)) {
			activeSiegeId = 7011;
		}
		return activeSiegeId;
	}

	/**
	 * 攻城开始回调。
	 * Callback when siege starts.
	 *
	 * @param fortressId 要塞 ID / fortress id
	 */
	public void onSiegeStart(final int fortressId) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			/**
			 * visit 方法。
			 * visit method.
			 *
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
	 *
	 * @param fortressId 要塞 ID / fortress id
	 */
	public void onSiegeFinish(final int fortressId) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			/**
			 * visit 方法。
			 * visit method.
			 *
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
	 *
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
	 * 返回当前已注册人数。
	 * Returns the current registered size.
	 *
	 * @return 已注册人数 / registered size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * 返回联盟人数上限。
	 * Returns the maximum registered size.
	 *
	 * @return 人数上限 / max size
	 */
	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * 返回当前请求 ID。
	 * Returns the current request id.
	 *
	 * @return 请求 ID / request id
	 */
	public int getrequestId() {
		return requestId;
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 *
	 * @return 服务实例 / service instance
	 */
	public static BattlefieldUnionService getInstance() {
		ObjectProvider<BattlefieldUnionService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> instance);
		}
		return instance;
	}

	/**
	 * 注入 Spring 单例提供者。
	 * Injects the Spring singleton provider.
	 *
	 * @param provider Spring 提供者 / spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<BattlefieldUnionService> provider) {
		instanceProvider = provider;
	}
}
