package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Calendar;
import java.util.concurrent.Future;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dao.PlayerPunishmentsDAO;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CAPTCHA;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUIT_RESPONSE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapType;

/**
 * 惩罚服务，处理角色封禁、监狱与采集限制（验证码）相关逻辑。
 * Punishment service handling character bans, prison, and gather restrictions (captcha).
 *
 * @author lord_rex, Cura, nrg
 */
public class PunishmentService {

	/**
	 * 解除角色封禁。
	 * Unbans a character.
	 *
	 * character id
	 */
	public static void unbanChar(int playerId) {
		DAOManager.getDAO(PlayerPunishmentsDAO.class).unpunishPlayer(playerId, PunishmentType.CHARBAN);
	}

	/**
	 * 封禁角色；若在线则立即踢下线。
	 * Bans a character and kicks them if currently online.
	 *
	 * character id
	 * @param dayCount 封禁天数，0 表示永久 / ban days; 0 means permanent
	 * ban reason
	 */
	public static void banChar(int playerId, int dayCount, String reason) {
		DAOManager.getDAO(PlayerPunishmentsDAO.class).punishPlayer(playerId, PunishmentType.CHARBAN,
				calculateDuration(dayCount), reason);

		// 若玩家在线——踢出 / if player is online - kick him
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
		if (player != null) {
			player.getClientConnection().close(new SM_QUIT_RESPONSE(), false);
		}
	}

	/**
	 * 将天数换算为剩余秒数时间戳；0 天返回 {@link Integer#MAX_VALUE}。
	 * Converts day count to remaining seconds; 0 days returns {@link Integer#MAX_VALUE}.
	 *
	 * day count
	 * duration in seconds
	 */
	public static long calculateDuration(int dayCount) {
		if (dayCount == 0) {
			return Integer.MAX_VALUE; // int because client handles this with seconds timestamp in int
		}
		Calendar cal = Calendar.getInstance();
		cal.add(5, dayCount);

		return (cal.getTimeInMillis() - System.currentTimeMillis()) / 1000;
	}

	/**
	 * 将玩家送入或放出监狱。
	 * Sends a player into prison or releases them.
	 *
	 * target player
	 * false 出狱 / true imprison / false release。
	 * @param delayInMinutes 监禁分钟数 / prison minutes
	 * reason
	 */
	public static void setIsInPrison(Player player, boolean state, long delayInMinutes, String reason) {
		stopPrisonTask(player, false);
		if (state) {
			long prisonTimer = player.getPrisonTimer();
			if (delayInMinutes > 0) {
				prisonTimer = delayInMinutes * 60000L;
				schedulePrisonTask(player, prisonTimer);
				PacketSendUtility.sendMessage(player, "You have been teleported to prison for a time of "
						+ delayInMinutes
						+ " minutes.\n If you disconnect the time stops and the timer of the prison'll see at your next login.");
			}

			if (GSConfig.ENABLE_CHAT_SERVER) {
				com.aionemu.gameserver.lifecycle.GameServerNetworkServices.chatServer().sendPlayerLogout(player);
			}
			player.setStartPrison(System.currentTimeMillis());
			TeleportService2.teleportToPrison(player);
			DAOManager.getDAO(PlayerPunishmentsDAO.class).punishPlayer(player, PunishmentType.PRISON, reason);
		} else {
			PacketSendUtility.sendMessage(player, "You come out of prison.");

			if (GSConfig.ENABLE_CHAT_SERVER) {
				PacketSendUtility.sendMessage(player, "To use global chats again relog!");
			}
			player.setPrisonTimer(0);

			TeleportService2.moveToBindLocation(player, true);

			DAOManager.getDAO(PlayerPunishmentsDAO.class).unpunishPlayer(player.getObjectId(), PunishmentType.PRISON);
		}
	}

	/**
	 * 停止监狱倒计时任务，可选保存剩余时间。
	 * Stops the prison countdown task, optionally saving remaining time.
	 *
	 * target player
	 * @param save 是否保存剩余计时 / whether to persist remaining timer
	 */
	public static void stopPrisonTask(Player player, boolean save) {
		Future<?> prisonTask = player.getController().getTask(TaskId.PRISON);
		if (prisonTask != null) {
			if (save) {
				long delay = player.getPrisonTimer();
				if (delay < 0) {
					delay = 0;
				}
				player.setPrisonTimer(delay);
			}
			player.getController().cancelTask(TaskId.PRISON);
		}
	}

	/**
	 * 登录/状态刷新时恢复监狱计时，并确保玩家在监狱地图。
	 * On login/status refresh, restores prison timer and ensures the player is on a prison map.
	 *
	 * target player
	 */
	public static void updatePrisonStatus(final Player player) {
		if (player.isInPrison()) {
			long prisonTimer = player.getPrisonTimer();
			if (prisonTimer > 0) {
				schedulePrisonTask(player, prisonTimer);
				int timeInPrison = (int) (prisonTimer / 60000);

				if (timeInPrison <= 0) {
					timeInPrison = 1;
				}
				PacketSendUtility.sendMessage(player, "You are still in prison for " + timeInPrison + " minute"
						+ (timeInPrison > 1 ? "s" : "") + ".");

				player.setStartPrison(System.currentTimeMillis());
			}

			if (player.getWorldId() != WorldMapType.DF_PRISON.getId()
					&& player.getWorldId() != WorldMapType.DE_PRISON.getId()) {
				PacketSendUtility.sendMessage(player, "You will be teleported to prison in one minute!");
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

					@Override
					public void run() {
						TeleportService2.teleportToPrison(player);
					}
				}, 60000);
			}
		}
	}

	/**
	 * 调度监狱释放任务。
	 * Schedules the prison release task.
	 *
	 * target player
	 * remaining milliseconds
	 */
	private static void schedulePrisonTask(final Player player, long prisonTimer) {
		player.setPrisonTimer(prisonTimer);
		player.getController().addTask(TaskId.PRISON, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				setIsInPrison(player, false, 0, "");
			}
		}, prisonTimer));
	}

	/**
	 * 设置或解除采集限制（含验证码流程）。
	 * Enables or clears gather restriction (including captcha flow).
	 *
	 * target player
	 * @param captchaCount 验证码次数 / captcha attempt count
	 * @param state true 禁止采集 / false 解除 / true restrict / false clear
	 * @param delay 限制毫秒 / restriction delay in ms
	 * @author Cura
	 */
	public static void setIsNotGatherable(Player player, int captchaCount, boolean state, long delay) {
		stopGatherableTask(player, false);

		if (state) {
			if (captchaCount < 3) {
				PacketSendUtility.sendPacket(player, new SM_CAPTCHA(captchaCount + 1, player.getCaptchaImage()));
			} else {
				player.setCaptchaWord(null);
				player.setCaptchaImage(null);
			}

			player.setGatherableTimer(delay);
			player.setStopGatherable(System.currentTimeMillis());
			scheduleGatherableTask(player, delay);
			DAOManager.getDAO(PlayerPunishmentsDAO.class).punishPlayer(player, PunishmentType.GATHER,
					"Possible gatherbot");
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400269));
			player.setCaptchaWord(null);
			player.setCaptchaImage(null);
			player.setGatherableTimer(0);
			player.setStopGatherable(0);
			DAOManager.getDAO(PlayerPunishmentsDAO.class).unpunishPlayer(player.getObjectId(), PunishmentType.GATHER);
		}
	}

	/**
	 * 停止采集限制任务，可选保存剩余时间。
	 * Stops the gather-restriction task, optionally saving remaining time.
	 *
	 * target player
	 * @param save 是否保存剩余计时 / whether to persist remaining timer
	 * @author Cura
	 */
	public static void stopGatherableTask(Player player, boolean save) {
		Future<?> gatherableTask = player.getController().getTask(TaskId.GATHERABLE);

		if (gatherableTask != null) {
			if (save) {
				long delay = player.getGatherableTimer();
				if (delay < 0) {
					delay = 0;
				}
				player.setGatherableTimer(delay);
			}
			player.getController().cancelTask(TaskId.GATHERABLE);
		}
	}

	/**
	 * 登录/状态刷新时恢复采集限制计时。
	 * On login/status refresh, restores gather-restriction timer.
	 *
	 * target player
	 * @author Cura
	 */
	public static void updateGatherableStatus(Player player) {
		if (player.isNotGatherable()) {
			long gatherableTimer = player.getGatherableTimer();

			if (gatherableTimer > 0) {
				scheduleGatherableTask(player, gatherableTimer);
				player.setStopGatherable(System.currentTimeMillis());
			}
		}
	}

	/**
	 * 调度采集限制解除任务。
	 * Schedules the gather-restriction release task.
	 *
	 * target player
	 * remaining milliseconds
	 * @author Cura
	 */
	private static void scheduleGatherableTask(final Player player, long gatherableTimer) {
		player.setGatherableTimer(gatherableTimer);
		player.getController().addTask(TaskId.GATHERABLE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				setIsNotGatherable(player, 0, false, 0);
			}
		}, gatherableTimer));
	}

	/**
	 * 惩罚类型枚举。
	 * Punishment type enum.
	 *
	 * @author Cura
	 */
	public enum PunishmentType {
		/** 监狱 / Prison */
		PRISON,
		/** 采集限制 / Gather restriction */
		GATHER,
		/** 角色封禁 / Character ban */
		CHARBAN
	}
}
