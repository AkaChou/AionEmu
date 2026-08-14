package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.AEInfos;
import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import com.aionemu.gameserver.lifecycle.GameShutdownRequest;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.util.List;

/**
 * 服务器系统信息与关机/重启控制管理员命令。
 * Admin command for server system info and shutdown/restart control.
 * <p>
 * //sys info - 系统信息 / System informations<br>
 * //sys memory - 内存信息 / Memory informations<br>
 * //sys gc - 垃圾回收 / Garbage collector<br>
 * //sys shutdown - 关机 / Call shutdown<br>
 * //sys restart - 重启 / Call restart<br>
 * //sys threadpool - 线程池信息 / Thread pools info
 * </p>
 *
 * @author lord_rex
 */
public class Sys extends AdminCommand {

	/**
	 * 构造 sys 命令。
	 * Creates the sys command.
	 */
	public Sys() {
		super("sys");
	}

	/**
	 * 执行系统信息、内存、GC、关机、重启或线程池查询。
	 * Runs system info, memory, GC, shutdown, restart, or thread-pool dump.
	 *
	 * @param player 执行 GM / Admin player
	 * @param params 子命令与参数 / Subcommand and args
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility
				.sendMessage(
					player,
					"Usage: //sys info | //sys memory | //sys gc | //sys restart <countdown time> <announce delay> | //sys shutdown <countdown time> <announce delay>");
			return;
		}

		if (params[0].equals("info")) {
			// 时间 / Time
			PacketSendUtility.sendMessage(player, "System Informations at: " + AEInfos.getRealTime().toString());

			// OS 信息 / OS Infos
			for (String line : AEInfos.getOSInfo())
				PacketSendUtility.sendMessage(player, line);

			// CPU 信息 / CPU Infos
			for (String line : AEInfos.getCPUInfo())
				PacketSendUtility.sendMessage(player, line);

			// JRE 信息 / JRE Infos
			for (String line : AEInfos.getJREInfo())
				PacketSendUtility.sendMessage(player, line);

			// JVM 信息 / JVM Infos
			for (String line : AEInfos.getJVMInfo())
				PacketSendUtility.sendMessage(player, line);
		}

		else if (params[0].equals("memory")) {
			// 内存信息 / Memory Infos
			for (String line : AEInfos.getMemoryInfo())
				PacketSendUtility.sendMessage(player, line);
		}

		else if (params[0].equals("gc")) {
			long time = System.currentTimeMillis();
			PacketSendUtility.sendMessage(player, "RAM Used (Before): "
				+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576));
			System.gc();
			PacketSendUtility.sendMessage(player, "RAM Used (After): "
				+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576));
			PacketSendUtility.sendMessage(player,
				"Garbage Collection finished in: " + (System.currentTimeMillis() - time) + " milliseconds...");
		}
		else if (params[0].equals("shutdown")) {
			try {
				int val = Integer.parseInt(params[1]);
				int announceInterval = Integer.parseInt(params[2]);
				GameShutdownRequest.doShutdown(val, announceInterval, ShutdownMode.SHUTDOWN);
				PacketSendUtility.sendMessage(player, "Server will shutdown in " + val + " seconds.");
			}
			catch (ArrayIndexOutOfBoundsException e) {
				PacketSendUtility.sendMessage(player, "Numbers only!");
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(player, "Numbers only!");
			}
		}
		else if (params[0].equals("restart")) {
			try {
				int val = Integer.parseInt(params[1]);
				int announceInterval = Integer.parseInt(params[2]);
				GameShutdownRequest.doShutdown(val, announceInterval, ShutdownMode.RESTART);
				PacketSendUtility.sendMessage(player, "Server will restart in " + val + " seconds.");
			}
			catch (ArrayIndexOutOfBoundsException e) {
				PacketSendUtility.sendMessage(player, "Numbers only!");
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(player, "Numbers only!");
			}
		}
		else if (params[0].equals("threadpool")) {
			List<String> stats = GameThreadPoolServices.threadPoolManager().getStats();
			for (String stat : stats) {
				PacketSendUtility.sendMessage(player, stat.replaceAll("\t", ""));
			}
		}
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Usage: //sys info | //sys memory | //sys gc | //sys restart <countdown time> <announce delay> | //sys shutdown <countdown time> <announce delay>");
	}
}
