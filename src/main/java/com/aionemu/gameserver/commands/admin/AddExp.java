package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员增加经验命令：给目标玩家增加指定经验值。
 * Admin add-exp command: grants a specified amount of experience to the target player.
 *
 * @author Wakizashi
 */
public class AddExp extends AdminCommand {

	/**
	 * 注册 {@code //addexp} 命令。
	 * Registers the {@code //addexp} command.
	 */
	public AddExp() {
		super("addexp");
	}

	/**
	 * 执行增加经验：解析经验值并叠加到目标玩家。
	 * Executes add-exp: parses the amount and adds it to the target player.
	 *
	 * @param params 参数：经验值 / experience amount
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length != 1) {
			onFail(player, null);
			return;
		}

		Player target = null;
		VisibleObject creature = player.getTarget();

		if (player.getTarget() instanceof Player) {
			target = (Player) creature;
		}

		String paramValue = params[0];
		long exp;
		try {
			exp = Long.parseLong(paramValue);
		}
		catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(player, "<exp> must be an Integer");
			return;
		}

		exp += target.getCommonData().getExp();
		target.getCommonData().setExp(exp, false);
		PacketSendUtility.sendMessage(player, "You added " + params[0] + " exp points to the target.");
	}

	/**
	 * 参数错误时输出 {@code //addexp} 用法。
	 * Prints {@code //addexp} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //addexp <exp>");
	}
}