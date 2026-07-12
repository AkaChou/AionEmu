package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.lang.reflect.Field;

/**
 * 阵营频道（.faction）开关命令（{@code //channel}）。
 * Admin command that toggles the faction channel ({@code //channel}).
 *
 * @author SheppeR
 */
public class Channel extends AdminCommand {

	/**
	 * 注册命令名为 {@code channel}。
	 * Registers the command name {@code channel}.
	 */
	public Channel() {
		super("channel");
	}

	/**
	 * 开关 {@code FACTION_CMD_CHANNEL} 配置。
	 * Toggles the {@code FACTION_CMD_CHANNEL} config flag.
	 *
	 * admin
	 * on|off。
	 */
	@Override
	public void execute(Player player, String... params) {
		Class<?> classToMofify = CustomConfig.class;
		Field someField;
		try {
			someField = classToMofify.getDeclaredField("FACTION_CMD_CHANNEL");
			if (params[0].equalsIgnoreCase("on") && !CustomConfig.FACTION_CMD_CHANNEL) {
				someField.set(null, Boolean.valueOf(true));
				PacketSendUtility.sendMessage(player, "The command .faction is ON.");
			}
			else if (params[0].equalsIgnoreCase("off") && CustomConfig.FACTION_CMD_CHANNEL) {
				someField.set(null, Boolean.valueOf(false));
				PacketSendUtility.sendMessage(player, "The command .faction is OFF.");
			}
		}
		catch (Exception e) {
			PacketSendUtility.sendMessage(player, "Error! Wrong property or value.");
			return;
		}
	}

	/**
	 * 执行失败时的语法提示。
	 * Syntax hint on failure.
	 *
	 * admin
	 * error message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //channel <On | Off>");
	}
}
