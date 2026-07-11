package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 玩家命令：向魔族频道发送付费世界消息。
 * Player command: sends a paid world message to the Asmodian channel.
 *
 * @author Maestros
 */
public class cmd_asmo_channel extends PlayerCommand {

	public cmd_asmo_channel() {
		super("asmo");
	}

	/**
	 * 向同种族在线玩家广播频道消息并扣费。
	 * Broadcasts a channel message to same-race online players and charges a fee.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * command parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		if (player.getRace() == Race.ASMODIANS && !player.isInPrison()) {
			int i = 1;
			boolean check = true;
			String adminTag = "";

			if (params.length < 1) {
				PacketSendUtility.sendMessage(player, "syntax : .asmo <message>");
				return;
			}

			StringBuilder sb = new StringBuilder(adminTag);
			if (AdminConfig.ADMIN_TAG_ENABLE) {
				if (player.getAccessLevel() == 1) {
					adminTag = AdminConfig.ADMIN_TAG_1.replace("%s", sb.toString());
				}
				else if (player.getAccessLevel() == 2) {
					adminTag = AdminConfig.ADMIN_TAG_2.replace("%s", sb.toString());
				}
				else if (player.getAccessLevel() == 3) {
					adminTag = AdminConfig.ADMIN_TAG_3.replace("%s", sb.toString());
				}
				else if (player.getAccessLevel() == 4) {
					adminTag = AdminConfig.ADMIN_TAG_4.replace("%s", sb.toString());
				}
				else if (player.getAccessLevel() == 5) {
					adminTag = AdminConfig.ADMIN_TAG_5.replace("%s", sb.toString());
				}
			}

			adminTag += player.getName() + " : ";

			StringBuilder sbMessage;
			if (player.isGM()) {
				sbMessage = new StringBuilder("[Asmodians]" + " " + adminTag);
			}
			else {
				sbMessage = new StringBuilder("[Asmodians]" + " " + player.getName() + " : ");
			}
			Race adminRace = Race.ASMODIANS;

			for (String s : params) {
				if (i++ != 0 && (check)) {
					sbMessage.append(s).append(" ");
				}
			}

			String message = sbMessage.toString().trim();
			int messageLenght = message.length();

			final String sMessage = message.substring(0, CustomConfig.MAX_CHAT_TEXT_LENGHT > messageLenght ? messageLenght : CustomConfig.MAX_CHAT_TEXT_LENGHT);
			final boolean toAll = params[0].equals("ALL");
			final Race race = adminRace;

			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {

				@Override
				public void visit(Player player) {
					if (toAll || player.getRace() == race || (player.getAccessLevel() > 0)) {
						PacketSendUtility.sendMessage(player, sMessage);
					}
				}
			});
		}
		else {
			PacketSendUtility.sendMessage(player, "You are Elyos! You can't use this chat. Please use .ely <message> to use you're faction chat!");

		}
	}

	/**
	 * 参数错误时提示用法。
	 * Shows usage when arguments are invalid.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax : .asmo <message>");
	}
}