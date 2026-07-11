package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.bonus_service.F2pBonus;
import com.aionemu.gameserver.model.bonus_service.ServiceBuff;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.f2p.F2pAccount;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ACCOUNT_PROPERTIES;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PACKAGE_INFO_NOTIFY;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * F2P/会员礼包服务，处理进服加成、续期与客户端属性包通知。
 * F2P/membership pack service handling enter-world bonuses, renewals, and client property packets.
 *
 * Created by wanke on 11/02/2017.
 */
public class F2pService {
	private static volatile ObjectProvider<F2pService> instanceProvider;
	private static F2pBonus f2p;
	private static ServiceBuff boost;

	/**
	 * 玩家进服时同步 F2P 状态、注册过期任务并下发属性包。
	 * Syncs F2P state on enter world, registers expire tasks, and sends property packets.
	 *
	 * @param player 玩家 / player
	 */
	public void onEnterWorld(Player player) {
		boolean isGM = player.getAccessLevel() >= AdminConfig.GM_PANEL;
		if (player.getF2p().getF2pAccount() != null) {
			playerBoostPack(player);
			GameTaskManagerServices.expireTimerTask().addTask(player.getF2p().getF2pAccount(), player);
			PacketSendUtility.sendPacket(player,
					new SM_ACCOUNT_PROPERTIES(isGM, 0, 8, player.getF2p().getF2pAccount().getRemainingTime()));
			PacketSendUtility.sendPacket(player,
					new SM_PACKAGE_INFO_NOTIFY(1, 3, player.getF2p().getF2pAccount().getRemainingTime()));
		} else {
			PacketSendUtility.sendPacket(player, new SM_ACCOUNT_PROPERTIES(isGM, 4, 0, 0));
			PacketSendUtility.sendPacket(player, new SM_PACKAGE_INFO_NOTIFY(1, 0, 0));
		}
	}

	/**
	 * 为玩家施加 F2P/会员相关增益效果。
	 * Applies F2P/membership boost effects to the player.
	 *
	 * 玩家 / player
	 */
	public void playerBoostPack(Player player) {
		// 会员相关 / MEMBERSHIP_BASE_TW_07
		boost = new ServiceBuff(2000007);
		boost.applyEffect(player, 2000007);
		// 会员相关 / MEMBERSHIP_PK_A_TW_07
		boost = new ServiceBuff(2000014);
		boost.applyEffect(player, 2000014);
		// 会员相关 / MEMBERSHIP_PK_B_TW_04
		boost = new ServiceBuff(2000018);
		boost.applyEffect(player, 2000018);
		// 黄金包。 / Gold Pack.
		f2p = new F2pBonus(1);
		f2p.applyEffect(player, 1);
	}

	/**
	 * 为玩家新增或续期 F2P 时长并立即生效。
	 * Adds or renews F2P duration for the player and applies effects immediately.
	 *
	 * @param player 玩家 / player
	 * @param minutes 续期分钟数，null 表示即时过期 / minutes to add; null means expire now
	 */
	public void onAddF2p(Player player, Integer minutes) {
		boolean isGM = player.getAccessLevel() >= AdminConfig.GM_PANEL;
		F2pAccount f2pAccount = new F2pAccount(
				minutes == null ? 0 : (int) (System.currentTimeMillis() / 1000 + minutes.intValue() * 60));
		player.getF2p().add(f2pAccount, true);
		PacketSendUtility.sendPacket(player,
				new SM_ACCOUNT_PROPERTIES(isGM, 0, 8, player.getF2p().getF2pAccount().getRemainingTime()));
		GameTaskManagerServices.expireTimerTask().addTask(player.getF2p().getF2pAccount(), player);
		playerBoostPack(player);
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static F2pService getInstance() {
		ObjectProvider<F2pService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<F2pService> provider) {
		instanceProvider = provider;
	}

	private static class SingletonHolder {
		protected static final F2pService instance = new F2pService();
	}
}
