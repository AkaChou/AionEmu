package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 玩家命令：向全服世界频道发送付费消息。
 * Player command: sends a paid message on the global world channel.
 *
 * @author Maestross
 */
public class cmd_world_channel extends PlayerCommand {

	public cmd_world_channel() {
		super("world");
	}

	/**
	 * 向在线玩家广播世界频道消息并扣费。
	 * Broadcasts a world-channel message to online players and charges a fee.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 命令参数 / command parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		int i = 1;
		int ap = CustomConfig.WORLD_CHANNEL_AP_COSTS;
		boolean check = true;
		String adminTag = "";

		if (params.length < 1) {
			PacketSendUtility.sendMessage(player, "syntax : .world <message>");
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
			sbMessage = new StringBuilder("[World-Chat]" + " " + adminTag);
		}
		else {
			sbMessage = new StringBuilder("[World-Chat]" + " " + player.getName() + " : ");
		}

		for (String s : params) {
			if (i++ != 0 && (check)) {
				sbMessage.append(s).append(" ");
			}
		}

		String message = sbMessage.toString().trim();
		int messageLenght = message.length();

		final String sMessage = message.substring(0, CustomConfig.MAX_CHAT_TEXT_LENGHT > messageLenght ? messageLenght : CustomConfig.MAX_CHAT_TEXT_LENGHT);
		if (player.isGM()) {

			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {

				@Override
				public void visit(Player player) {
					PacketSendUtility.sendMessage(player, sMessage);
				}
			});
		}
		else if (!player.isGM() && !player.isInPrison()) {
			if (player.getAbyssRank().getAp() < ap) {
				PacketSendUtility.sendMessage(player, "You dont have enough ap, you only have:" + player.getAbyssRank().getAp());
			}
			else {
				AbyssPointsService.addAp(player, -ap);
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {

					@Override
					public void visit(Player player) {
						PacketSendUtility.sendMessage(player, sMessage);
					}
				});
			}
		}
		else {
			PacketSendUtility.sendMessage(player, "You dont have enough ap, you only have:" + player.getAbyssRank().getAp());
		}
	}

	/**
	 * 参数错误时提示用法。
	 * Shows usage when arguments are invalid.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param message 失败提示消息 / failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax : .world <message>");
	}
}