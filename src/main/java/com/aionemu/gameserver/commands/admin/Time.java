package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GAME_TIME;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 设置游戏内时间（时段或小时）的管理员命令。
 * Admin command to set in-game time by period name or hour.
 */
public class Time extends AdminCommand
{
	/**
	 * 构造 time 命令。
	 * Creates the time command.
	 */
	public Time() {
		super("time");
	}

	/**
	 * 将游戏时间切换为 dawn/day/dusk/night 或指定小时，并广播给全体在线玩家。
	 * Switches game time to dawn/day/dusk/night or a given hour and broadcasts to all online players.
	 *
	 * 执行 GM / Admin player
	 * @param params 时段名或小时 / Period name or hour
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			onFail(admin, null);
			return;
		}
		int time = GameTimeManager.getGameTime().getHour();
		int min = GameTimeManager.getGameTime().getMinute();
		int hour;
		if (params[0].equals("night")) {
			hour = 22;
		} else if (params[0].equals("dusk")) {
			hour = 18;
		} else if (params[0].equals("day")) {
			hour = 9;
		} else if (params[0].equals("dawn")) {
			hour = 4;
		} else {
			try {
				hour = Integer.parseInt(params[0]);
			} catch (NumberFormatException e) {
				onFail(admin, null);
				return;
			} if (hour < 0 || hour > 23) {
				onFail(admin, null);
				PacketSendUtility.sendMessage(admin, "A day have only 24 hours!\n" + "Min value : 0 - Max value : 23");
				return;
			}
		}
		time = hour - time;
		time = GameTimeManager.getGameTime().getTime() + (60 * time) - min;
		GameTimeManager.reloadTime(time);
		GameTimeManager.getGameTime().checkDayTimeChange();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_GAME_TIME());
			}
		});
		PacketSendUtility.sendMessage(admin, "You changed the time to " + params[0].toString() + ".");
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		String syntax = "Syntax: //time < dawn | day | dusk | night | desired hour (number) >";
		PacketSendUtility.sendMessage(player, syntax);
	}
}
