package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameBattlefieldServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.CountDownLatch;

import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;
import com.aionemu.gameserver.services.instance.KamarBattlefieldService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员副本引擎管理命令：加载/关闭/重启 InstanceEngine，或开启 HOT/Kamar 报名。
 * Admin instance-engine manager: load/stop/restart InstanceEngine, or start HOT/Kamar registration.
 */
public class InstanceEngineManager extends AdminCommand {

	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";
	private static final String COMMAND_RESTART = "restart";

	private static final String COMMAND_STARTHOT = "hot";
	private static final String COMMAND_STARTKAR = "karma";

	public InstanceEngineManager() {
		super("instance_manager");
	}

	/**
	 * 处理 start/stop/restart/hot/karma 子命令。
	 * Handle start/stop/restart/hot/karma subcommands.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 */
	@Override
	public void execute(final Player player, String... params) {
		final GameEngine[] parallelEngines = { GameEngineServices.instanceEngine() };
		final CountDownLatch progressLatch = new CountDownLatch(parallelEngines.length);
		if (params.length == 0) {
			showHelp(player);
			return;
		}
		if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0]) || COMMAND_RESTART.equalsIgnoreCase(params[0]) || COMMAND_STARTHOT.equalsIgnoreCase(params[0]) || COMMAND_STARTKAR.equalsIgnoreCase(params[0])) {
			if (COMMAND_START.equalsIgnoreCase(params[0])) {
				GameEngineServices.instanceEngine().load(progressLatch);
				PacketSendUtility.sendMessage(player, "InstanceEngine loaded successfully!");
			}
			if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
				GameEngineServices.instanceEngine().shutdown();
				PacketSendUtility.sendMessage(player, "InstanceEngine shutdown successfully!");
			}
			if (COMMAND_RESTART.equalsIgnoreCase(params[0])) {
				GameEngineServices.instanceEngine().shutdown();
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						GameEngineServices.instanceEngine().load(progressLatch);
						PacketSendUtility.sendMessage(player, "InstanceEngine reloaded successfully!");
					}
				}, 5000);
			}
			if (COMMAND_STARTHOT.equalsIgnoreCase(params[0])) {
				GameBattlefieldServices.hallOfTenacityService().startHallOfTenacityRegistration();
			}
			if (COMMAND_STARTKAR.equalsIgnoreCase(params[0])) {
				GameBattlefieldServices.kamarBattlefieldService().startKamarRegistration();
			}
		}
	}

	/**
	 * 显示命令帮助。
	 * Show command help.
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 */
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //instance start|stop");
	}
}
