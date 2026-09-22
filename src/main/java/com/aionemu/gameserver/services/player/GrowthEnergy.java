package com.aionemu.gameserver.services.player;


import com.aionemu.boot.i18n.I18n;
import lombok.Setter;
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

/**
 * 成长能量服务，管理成长能量计时与发放。
 * Growth energy service managing growth-energy timers and grants.
 */

@Slf4j

public class GrowthEnergy {
    /**
     * -- SETTER --
     *  设置实例提供者（Spring 注入）。
     *  Sets the instance provider (Spring injection).
     *
     * @param provider 实例提供者 / instance provider
     */
    @Setter
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
		/**
		 * visit 方法。
		 * visit method.
		 *
		 * @param player 玩家 / player
		 */com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			 player.getCommonData().setAuraOfGrowth(0);
			 PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
			 DAOManager.getDAO(PlayerDAO.class).storePlayer(player);
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
	public static GrowthEnergy getInstance() {
		ObjectProvider<GrowthEnergy> provider = instanceProvider;
		GrowthEnergy provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("GrowthEnergy 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

}
