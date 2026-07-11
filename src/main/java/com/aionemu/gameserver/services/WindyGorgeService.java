package com.aionemu.gameserver.services;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EVERGALE_CANYON;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 风谷（永恒峡谷）服务，处理玩家登录时相关数据包。
 * Windy gorge (Evergale Canyon) service that handles related packets on player login.
 *
 * @author Wnkrz
 */
public class WindyGorgeService {
	private static volatile ObjectProvider<WindyGorgeService> instanceProvider;

	/**
	 * 玩家登录时发送风谷相关数据包。
	 * Sends windy gorge related packets on player login.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogin(Player player) {
		PacketSendUtility.sendPacket(player, new SM_EVERGALE_CANYON(2));
		PacketSendUtility.sendPacket(player, new SM_EVERGALE_CANYON(4));
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 */
	public static final WindyGorgeService getInstance() {
		ObjectProvider<WindyGorgeService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<WindyGorgeService> provider) {
		instanceProvider = provider;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final WindyGorgeService instance = new WindyGorgeService();
	}
}
