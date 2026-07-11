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
	 * getSiegeActive 方法。
	 * getSiegeActive method.
	 * result
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
			activeSiegeId = 1231;
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
	 * fortressId
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
	 * fortressId
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
	 * 玩家 / player
	 * requestId
	 * activeSiegeId
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
	 * getSize 方法。
	 * getSize method.
	 * result
	 */
	public int getSize() {
		return size;
	}

	/**
	 * getMaxSize 方法。
	 * getMaxSize method.
	 * result
	 */
	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * getrequestId 方法。
	 * getrequestId method.
	 * result
	 */
	public int getrequestId() {
		return requestId;
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static BattlefieldUnionService getInstance() {
		ObjectProvider<BattlefieldUnionService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> instance);
		}
		return instance;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<BattlefieldUnionService> provider) {
		instanceProvider = provider;
	}
}
