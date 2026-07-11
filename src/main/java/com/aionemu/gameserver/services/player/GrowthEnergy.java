package com.aionemu.gameserver.services.player;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 成长能量服务，管理成长能量计时与发放。
 * Growth energy service managing growth-energy timers and grants.
 */

@Slf4j

public class GrowthEnergy {
	private static volatile ObjectProvider<GrowthEnergy> instanceProvider;
	private boolean dailyGenerated = true;

	/**
	 * 初始化。
	 * Initializes.
	 */
	public void init() {
		log.info(I18n.get("log.bfa6a6e66239"));
		String daily = "0 0 9 1/1 * ? *";
		GameCronServices.cronService().schedule(new Runnable() {
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				dailyGenerated = false;
				updateGrowthEnergy();
			}
		}, daily);
	}

	private void updateGrowthEnergy() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			/**
			 * visit 方法。
			 * visit method.
			 *
			 * @param player 玩家 / player
			 */
			public void visit(final Player player) {
				player.getCommonData().setAuraOfGrowth(0);
				PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
				DAOManager.getDAO(PlayerDAO.class).storePlayer(player);
			}
		});
	}

	/**
	 * 玩家登录时同步状态。
	 * Syncs state when a player logs in.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogin(Player player) {
		if (player.getCommonData().getAuraOfGrowth() != 0) {
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		}
	}

	/**
	 * 增加成长能量。
	 * Adds growth energy.
	 *
	 * @param player 玩家 / player
	 */
	public void addGrowthEnergy(Player player) {
		PlayerCommonData pcd = player.getCommonData();
		if (pcd.isReadyForAuraOfGrowth()) {
			long auraOfGrowthpercent = pcd.getAuraOfGrowthPoints();
			pcd.addAuraOfGrowth(auraOfGrowthpercent);
			DAOManager.getDAO(PlayerDAO.class).storePlayer(player);
			sendMessage(player);
		}
	}

	/**
	 * 发送提示消息。
	 * Sends a notice message.
	 *
	 * @param player 玩家 / player
	 */
	public void sendMessage(Player player) {
		long points = player.getCommonData().getAuraOfGrowthPoints();
		if (player.getCommonData().getAuraOfGrowth() == points) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(1));
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		} else if (player.getCommonData().getAuraOfGrowth() == points * 10) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(10));
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		} else if (player.getCommonData().getAuraOfGrowth() == points * 20) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(20));
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		} else if (player.getCommonData().getAuraOfGrowth() == points * 30) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(30));
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		} else if (player.getCommonData().getAuraOfGrowth() == points * 40) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(40));
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		} else if (player.getCommonData().getAuraOfGrowth() == points * 50) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(50));
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		} else if (player.getCommonData().getAuraOfGrowth() == points * 60) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(60));
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		} else if (player.getCommonData().getAuraOfGrowth() == points * 70) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(70));
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		} else if (player.getCommonData().getAuraOfGrowth() == points * 80) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(80));
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		} else if (player.getCommonData().getAuraOfGrowth() == points * 90) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(90));
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		} else if (player.getCommonData().getAuraOfGrowth() == points * 100) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CHARGE_EXP_POINT_NORMAL(100));
			updateGrowthEnergy();
		}
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static GrowthEnergy getInstance() {
		ObjectProvider<GrowthEnergy> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<GrowthEnergy> provider) {
		instanceProvider = provider;
	}

	private static class SingletonHolder {
		protected static final GrowthEnergy instance = new GrowthEnergy();
	}
}
