package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：消耗欧比斯点数（AP）兑换荣耀点数（GP）。
 * Player command: exchanges Abyss Points (AP) for Glory Points (GP).
 *
 * @author Waii
 */
public class cmd_gp extends PlayerCommand {

	/**
	 * 注册命令别名 {@code gp}。
	 * Registers the command alias {@code gp}.
	 */
	public cmd_gp() {
		super("gp");
	}

	/**
	 * 若 AP 足够则扣除 150000 AP 并增加 200 GP。
	 * If AP is sufficient, deducts 150000 AP and grants 200 GP.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		int ap = 150000;
		int gp = 200;
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendMessage(player, "Vous n'avez pas asser de points abyssaux, point requis : " + ap);
			return;
		}
		AbyssPointsService.addGp(player, gp);
		AbyssPointsService.addAp(player, -ap);

	}
}
