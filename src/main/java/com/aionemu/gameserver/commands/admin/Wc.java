package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 管理员世界频道广播（按种族或全体）。
 * Admin world-channel broadcast by race or to all.
 *
 * @author -Evilwizard-, Wakizashi World Channel, only for GM/Admins
 */
public class Wc extends AdminCommand {

	/**
	 * 构造 wc 命令。
	 * Creates the wc command.
	 */
	public Wc() {
		super("wc");
	}

	/**
	 * 向天族/魔族/全体或默认本阵营发送世界频道消息。
	 * Broadcasts a world-channel message to Elyos, Asmodians, all, or default own race.
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params ELY|ASM|ALL|default and message。
	 */
	@Override
	public void execute(Player admin, String... params) {
		int i = 1;
		boolean check = true;
		Race adminRace = admin.getRace();

		if (params.length < 2) {
			PacketSendUtility.sendMessage(admin, "syntax : //wc <ELY | ASM | ALL | default> <message>");
			return;
		}

		StringBuilder sbMessage;
		if (params[0].equals("ELY")) {
			sbMessage = new StringBuilder("[World-Elyos]" + admin.getName() + ": ");
			adminRace = Race.ELYOS;
		}
		else if (params[0].equals("ASM")) {
			sbMessage = new StringBuilder("[World-Asmodian]" + admin.getName() + ": ");
			adminRace = Race.ASMODIANS;
		}
		else if (params[0].equals("ALL"))
			sbMessage = new StringBuilder("[World-All]" + admin.getName() + ": ");
		else {
			check = false;
			if (adminRace == Race.ELYOS)
				sbMessage = new StringBuilder("[World-Elyos]" + admin.getName() + ": ");
			else
				sbMessage = new StringBuilder("[World-Asmodian]" + admin.getName() + ": ");
		}

		for (String s : params)
			if (i++ != 1 && (check))
				sbMessage.append(s + " ");

		String message = sbMessage.toString().trim();
		int messageLenght = message.length();

		final String sMessage = message.substring(0, CustomConfig.MAX_CHAT_TEXT_LENGHT > messageLenght ? messageLenght : CustomConfig.MAX_CHAT_TEXT_LENGHT);
		final boolean toAll = params[0].equals("ALL");
		final Race race = adminRace;

		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {

			@Override
			public void visit(Player player) {
				if (toAll || player.getRace() == race || player.getAccessLevel() >= getLevel()) {
					PacketSendUtility.sendMessage(player, sMessage);
				}
			}
		});
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax : //wc <ELY | ASM | ALL | default> <message>");
	}
}
