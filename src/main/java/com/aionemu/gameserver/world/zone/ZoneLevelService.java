package com.aionemu.gameserver.world.zone;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 区域高度/水位服务：低于死亡高度立即死亡，低于水位则开始溺水。
 * Zone height/water service: die below death level, start drowning below water level.
 *
 * @author ATracer
 */
public class ZoneLevelService {

	/** 溺水伤害周期（毫秒）/ drowning damage period in milliseconds */
	private static final long DROWN_PERIOD = 2000;

	/**
	 * 检查玩家 Z 高度：低于死亡高度则死亡，低于水位则开始溺水，否则停止溺水。
	 * Check player Z: die below death level, start drowning below water level, otherwise stop drowning.
	 *
	 * @param player 玩家 / player
	 */
	public static void checkZoneLevels(Player player) {
		World world = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		float z = player.getZ();

		if (player.getLifeStats().isAlreadyDead()) {
			return;
		}
		if (z < world.getWorldMap(player.getWorldId()).getDeathLevel()) {
			player.getController().die();
			return;
		}

		float noseHeight = player.getPlayerAppearance().getBoundHeight() - 0.1f;
		if (z + noseHeight < world.getWorldMap(player.getWorldId()).getWaterLevel()) {
			startDrowning(player);
		} else {
			stopDrowning(player);
		}
	}

	/**
	 * 若尚未溺水则启动溺水任务。
	 * Start drowning task if not already drowning.
	 *
	 * @param player 玩家 / player
	 */
	private static void startDrowning(Player player) {
		if (!isDrowning(player)) {
			scheduleDrowningTask(player);
		}
	}

	/**
	 * 若正在溺水则取消溺水任务。
	 * Cancel drowning task if currently drowning.
	 *
	 * @param player 玩家 / player
	 */
	private static void stopDrowning(Player player) {
		if (isDrowning(player)) {
			player.getController().cancelTask(TaskId.DROWN);
		}
	}

	/**
	 * 玩家是否正在溺水。
	 * Whether the player is currently drowning.
	 *
	 * @param player 玩家 / player
	 * @return 是否溺水中 / whether drowning
	 */
	private static boolean isDrowning(Player player) {
		return player.getController().getTask(TaskId.DROWN) == null ? false : true;
	}

	/**
	 * 调度固定频率的溺水伤害任务。
	 * Schedule a fixed-rate drowning damage task.
	 *
	 * @param player 玩家 / player
	 */
	private static void scheduleDrowningTask(final Player player) {
		player.getController().addTask(TaskId.DROWN,
				GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {

					@Override
					public void run() {
						int value = Math.max(1, Math.round(player.getLifeStats().getMaxHp() / 10f));
						if (!player.getLifeStats().isAlreadyDead()) {
							if (!player.isInvul()) {
								int previousHp = player.getLifeStats().getCurrentHp();
								int currentHp = player.getLifeStats().reduceHp(value, player);
								PacketSendUtility.broadcastPacketAndReceive(player,
										new SM_ATTACK_STATUS(player, player, TYPE.DROWNING, 0, previousHp - currentHp, LOG.REGULAR));
								if (currentHp == 0) {
									stopDrowning(player);
								}
							}
						} else {
							stopDrowning(player);
						}
					}
				}, 0, DROWN_PERIOD));
	}
}
