package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员添加掉落命令（当前未实现，仅提示）。
 * Admin add-drop command (currently unimplemented; prints a notice only).
 * @author ATracer
 */
public class AddDrop extends AdminCommand {

	/**
	 * 注册 {@code //adddrop} 命令。
	 * Registers the {@code //adddrop} command.
	 */
	public AddDrop() {
		super("adddrop");
	}

	/**
	 * 执行添加掉落：当前功能未实现。
	 * Executes add-drop: currently not implemented.
	 * @param params 参数：怪物/物品/数量/几率 / mob, item, min/max, chance
	 */
	@Override
	public void execute(Player player, String... params) {
		PacketSendUtility.sendMessage(player, "Now this is not implemented.");
	}

	/**
	 * 参数错误时输出 {@code //adddrop} 用法。
	 * Prints {@code //adddrop} usage on invalid arguments.
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //adddrop <mobid> <itemid> <min> <max> <chance>");
	}
}
