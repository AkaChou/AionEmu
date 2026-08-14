package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.CubeExpandService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 将管理员背包扩展到最大格数的命令（{@code //cube}）。
 * Admin command that expands the inventory cube to maximum ({@code //cube}).
 *
 * @author Kamui
 */
public class Cube extends AdminCommand {

	/**
	 * 注册命令名为 {@code cube}。
	 * Registers the command name {@code cube}.
	 */
	public Cube() {
		super("cube");
	}

	/**
	 * 循环扩展背包直至满级（9 次）。
	 * Expands the inventory cube up to the maximum (9 expansions).
	 *
	 */
	@Override
	public void execute(Player player, String... params) {
		if (player.getNpcExpands() >= 9) {
			PacketSendUtility.sendMessage(player, "Aucune extension n'est disponible pour votre inventaire.");
			return;
		}
        while (player.getNpcExpands() < 9) {
            CubeExpandService.expand(player, true);
        }
		PacketSendUtility.sendMessage(player, "Vous venez de recevoir toutes les extensions de votre inventaire.");
	}

	/**
	 * 执行失败时的语法提示。
	 * Syntax hint on failure.
	 *
	 */
	@Override
	public void onFail(Player admin, String message) {
		PacketSendUtility.sendMessage(admin, "Syntaxe : .cube");
	}
}
