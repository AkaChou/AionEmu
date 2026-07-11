package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：消耗欧比斯点数兑换指定道具。
 * Player command: spends Abyss Points to exchange for a fixed item reward.
 *
 * @author Maestross
 */
public class cmd_exchange extends PlayerCommand {

	/**
	 * 注册命令别名 {@code exchange}。
	 * Registers the command alias {@code exchange}.
	 */
	public cmd_exchange() {
		super("exchange");
	}

	/**
	 * 若 AP 足够则扣除 15000 AP 并发放兑换道具。
	 * If AP is sufficient, deducts 15000 AP and grants the exchange items.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		int ap = 15000;
		int derived = 186000147;
		int derived_q = 5;
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendMessage(player, "Du hast nicht genug AP, du hast nur: " + ap);
			return;
		}
		ItemService.addItem(player, derived, derived_q);
		AbyssPointsService.addAp(player, -ap);

	}
}
