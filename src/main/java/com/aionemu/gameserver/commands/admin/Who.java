package com.aionemu.gameserver.commands.admin;

import java.util.Collection;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 列出在线玩家（可按种族/会员过滤）的管理员命令。
 * Admin command to list online players, optionally filtered by race or membership.
 */
public class Who extends AdminCommand {

	/**
	 * 构造 who 命令。
	 * Creates the who command.
	 */
	public Who() {
		super("who");
	}

	/**
	 * 输出在线角色名、种族与账号；可选 ely/asmo/member 过滤。
	 * Prints online characters with race and account; optional ely/asmo/member filter.
	 *
	 * 执行 GM / Admin player
	 * Optional ely|asmo|member|premium。
	 */
	@Override
	public void execute(Player admin, String... params) {

		Collection<Player> players = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers();

		PacketSendUtility.sendMessage(admin, "Player :");

		for (Player player : players) {
			if (params != null && params.length > 0) {
				String cmd = params[0].toLowerCase();

				if (("ely").startsWith(cmd)) {
					if (player.getCommonData().getRace() == Race.ASMODIANS) {
						continue;
					}
				}

				if (("asmo").startsWith(cmd)) {
					if (player.getCommonData().getRace() == Race.ELYOS) {
						continue;
					}
				}

				if (("member").startsWith(cmd) || ("premium").startsWith(cmd)) {
					if (player.getPlayerAccount().getMembership() == 0) {
						continue;
					}
				}
			}

			PacketSendUtility.sendMessage(admin, "Char: " + player.getName() + " - Race: " + player.getCommonData().getRace().name() + " - Acc: " + player.getAcountName());
		}
	}
}
