package com.aionemu.gameserver.taskmanager.tasks;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import com.aionemu.gameserver.lifecycle.GameShutdownRequest;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.tasks.TaskFromDBHandler;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 数据库调度的自动重启任务处理器。
 * DB-scheduled automatic server-restart task handler.
 *
 * @author Divinity
 */
@Slf4j
public class RestartTask extends TaskFromDBHandler {

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
	 * 返回任务名 {@code restart}。
	 * Return the task name {@code restart}.
	 *
	 * Task name
	 */
	@Override
	public String getTaskName() {
		return "restart";
	}

	/**
	 * 校验参数个数是否为 3。
	 * Whether parameter count is exactly 3.
	 *
	 * @return 配置有效时为 {@code true} / {@code true} if valid
	 */
	@Override
	public boolean isValid() {
		return params.length == 3;
	}

	/**
	 * 广播重启预警，并在预警结束后触发 {@link ShutdownMode#RESTART}。
	 * Broadcast a restart warning, then trigger {@link ShutdownMode#RESTART} after the warning delay.
	 */
	@Override
	public void run() {
		log.info(I18n.get("log.92ee53efe389", id));
		setLastActivation();

		countDown = Integer.parseInt(params[0]);
		announceInterval = Integer.parseInt(params[1]);
		warnCountDown = Integer.parseInt(params[2]);

		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {

			@Override
			public void visit(Player player) {
				PacketSendUtility.sendBrightYellowMessageOnCenter(player, "Automatic Task: The server will restart in "
						+ warnCountDown + " seconds ! Please find a safe place and disconnect your character.");
			}
		});

		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				GameShutdownRequest.doShutdown(countDown, announceInterval, ShutdownMode.RESTART);
			}
		}, warnCountDown * 1000);
	}
}
