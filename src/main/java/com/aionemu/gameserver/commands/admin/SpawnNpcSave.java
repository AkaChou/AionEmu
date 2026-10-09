package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * NPC 生成指令（持久化版本，{@code //spawns}）；行为等同于写入刷怪数据的 {@code //spawn}：
 * 非批量默认重生时间，重生时间大于 0 时把刷出点位写入 {@code spawns} 自定义 XML。
 * Persisting spawn command ({@code //spawns}); behaves like the legacy saving form of {@code //spawn}:
 * a non-batch spawn uses the default respawn time, and spots with a positive respawn time are written
 * to the custom {@code spawns} XML.
 * @author Luno
 */
public class SpawnNpcSave extends AdminCommand {

	/**
	 * 注册命令名为 {@code spawns}。
	 * Registers the command name {@code spawns}.
	 */
	public SpawnNpcSave() {
		super("spawns");
	}

	/**
	 * 执行该管理指令（持久化版本）。
	 * Executes this admin command (persisting variant).
	 * @param admin 执行指令的管理员 / admin executing the command
	 */
	@Override
	public void execute(Player admin, String... params) {
		SpawnNpc.executeSpawn(admin, true, params);
	}

	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 * @param player 接收提示的玩家 / player receiving the message
	 */
	@Override
	public void onFail(Player player, String message) {
		SpawnNpc.printSpawnUsage(player, true);
	}
}
