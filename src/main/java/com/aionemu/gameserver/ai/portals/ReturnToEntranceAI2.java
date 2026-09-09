package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * 副本返回入口 AI：完成交互后将玩家传送到当前副本的配置出口。
 * Instance return-entrance AI: teleports the player to the configured exit after the interaction completes.
 *
 * <p>该 AI 复用 {@link ActionItemNpcAI2} 的交互进度条，并委托现有实例出口服务处理出口配置、离场和兜底行为。
 * This AI reuses the interaction progress bar from {@link ActionItemNpcAI2} and delegates exit configuration,
 * instance departure, and fallback behavior to the existing instance-exit service.</p>
 */
@AIName("ReturnToEntrance")
public class ReturnToEntranceAI2 extends ActionItemNpcAI2 {

	/**
	 * 完成交互后离开当前副本。
	 * Leaves the current instance after the interaction completes.
	 *
	 * @param player 完成交互的玩家 / player who completed the interaction
	 */
	@Override
	protected void handleUseItemFinish(Player player) {
		if (getOwner().isInInstance()) {
			TeleportService2.moveToInstanceExit(player, player.getWorldId(), player.getRace());
		}
	}
}
