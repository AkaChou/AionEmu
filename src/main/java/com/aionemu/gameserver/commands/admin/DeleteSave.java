package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 删除当前目标 NPC 刷出并持久化的管理命令（{@code //deletes}）。
 * 在 {@code //delete} 的基础上把删除写入刷怪数据，服务端重启后不再刷出该点位。
 * Admin command that deletes the targeted NPC spawn and persists the change ({@code //deletes}).
 * On top of {@code //delete}, the removal is written to the spawn data so the spot no longer
 * respawns after a server restart.
 * @author Luno
 */
public class DeleteSave extends AdminCommand {

	/**
	 * 注册命令名为 {@code deletes}。
	 * Registers the command name {@code deletes}.
	 */
	public DeleteSave() {
		super("deletes");
	}

	/**
	 * 删除目标 NPC 刷出并持久化（不支持池化/攻城刷出）。
	 * Deletes the targeted NPC spawn and persists it (pooled/siege spawns are not allowed).
	 */
	@Override
	public void execute(Player player, String... params) {
		Delete.executeDelete(player, true);
	}

}
