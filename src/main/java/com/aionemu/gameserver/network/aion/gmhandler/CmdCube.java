package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.CubeExpandService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令：扩展目标玩家背包（Cube）格子。
 * GM command handler that expands the target player's cube inventory slots.
 *
 * @author Waii
 */
public final class CmdCube extends AbstractGMHandler {

	/**
	 * 创建处理器并立即扩展背包。
	 * Creates the handler and immediately expands the cube.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 指令参数（当前未使用） / command parameters (currently unused)
	 */
	public CmdCube(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 为当前目标玩家扩展 9 格 Cube 背包。
	 * Expands the current target player's cube by 9 slots.
	 */
	public void run() {
		@SuppressWarnings("unused")
		Player t = target != null ? target : admin;
		CubeExpandService.expand(target, true);
		PacketSendUtility.sendMessage(admin, "9 cube slots successfully added");
	}
}
