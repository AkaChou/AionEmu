package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.CubeExpandService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 管理员扩展魔立方命令：为指定在线玩家增加一档背包格子。
 * Admin cube-expand command: expands inventory slots for a target online player.
 *
 * @author Kamui
 */
public class AddCube extends AdminCommand {

	/**
	 * 注册 {@code //addcube} 命令。
	 * Registers the {@code //addcube} command.
	 */
	public AddCube() {
		super("addcube");
	}

	/**
	 * 执行魔立方扩展：定位在线玩家并调用扩展服务。
	 * Executes cube expand: finds the online player and calls the expand service.
	 *
	 * @param params 参数：玩家名 / player name
	 */
	@Override
	public void execute(Player admin, String... params) {

		if (params.length != 1) {
			PacketSendUtility.sendMessage(admin, "Syntax: //addcube <player name>");
			return;
		}

		Player receiver = null;

		receiver = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));

		if (receiver == null) {
			PacketSendUtility.sendMessage(admin, "The player "+ Util.convertName(params[0]) +" is not online.");
			return;
		}

		if (receiver != null) {
			if (receiver.getNpcExpands() < 9) {
				CubeExpandService.expand(receiver, true);
				PacketSendUtility.sendMessage(admin, "9 cube slots successfully added to player "+receiver.getName()+"!");
				PacketSendUtility.sendMessage(receiver, "Admin "+admin.getName()+" gave you a cube expansion!");
			}
			else {
				PacketSendUtility.sendMessage(admin, "Cube expansion cannot be added to "+receiver.getName()+"!\nReason: player cube already fully expanded.");
				return;
			}
		}
	}
	
	/**
	 * 参数错误时输出 {@code //addcube} 用法。
	 * Prints {@code //addcube} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player admin, String message) {
		PacketSendUtility.sendMessage(admin, "Syntax: //addcube <player name>");
	}
}