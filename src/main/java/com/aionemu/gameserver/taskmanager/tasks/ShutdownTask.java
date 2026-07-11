package com.aionemu.gameserver.taskmanager.tasks;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import com.aionemu.gameserver.lifecycle.GameShutdownRequest;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.tasks.TaskFromDBHandler;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据库调度的自动关机任务处理器。
 * DB-scheduled automatic server-shutdown task handler.
 *
 * @author Divinity
 */
@Slf4j
public class ShutdownTask extends TaskFromDBHandler {

	/**
	 * 正式倒计时秒数。
	 * Formal countdown seconds.
	 */
	private int countDown;

	/**
	 * 公告间隔秒数。
	 * Announce interval in seconds.
	 */
	private int announceInterval;

	/**
	 * 预警倒计时秒数（预警后再进入正式关机）。
	 * Warning countdown seconds (before formal shutdown starts).
	 */
	private int warnCountDown;

	/**
	 * 返回任务名 {@code shutdown}。
	 * Return the task name {@code shutdown}.
	 *
	 * Task name
	 */
	@Override
	public String getTaskName() {
		return "shutdown";
	}

	/**
	 * 校验参数个数是否为 3。
	 * Whether parameter count is exactly 3.
	 *
	 * @return 若 valid 则为 true / True if valid
	 */
	@Override
	public boolean isValid() {
		return params.length == 3;
	}

	/**
	 * 广播关机预警，并在预警结束后触发 {@link ShutdownMode#SHUTDOWN}。
	 * Broadcast a shutdown warning, then trigger {@link ShutdownMode#SHUTDOWN} after the warning delay.
	 */
	@Override
	public void run() {
		log.info(I18n.get("log.8f7fa68333a8", id));
		setLastActivation();

		countDown = Integer.parseInt(params[0]);
		announceInterval = Integer.parseInt(params[1]);
		warnCountDown = Integer.parseInt(params[2]);

		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {

			@Override
			public void visit(Player player) {
				PacketSendUtility.sendBrightYellowMessageOnCenter(player, "Automatic Task: The server will shutdown in "
						+ warnCountDown + " seconds ! Please find a peace place and disconnect your character.");
			}
		});

		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				GameShutdownRequest.doShutdown(countDown, announceInterval, ShutdownMode.SHUTDOWN);
			}
		}, warnCountDown * 1000);
	}
}
