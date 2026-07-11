package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.TimeUnit;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：麻痹 10 秒后传送回绑定石位置。
 * Player command: after a 10-second paralyze cast, teleports to bind location.
 *
 * @author Nemiroff
 * @rework Eloann
 */
public class cmd_unstuck extends PlayerCommand {

	/**
	 * 注册命令别名 {@code unstuck}。
	 * Registers the command alias {@code unstuck}.
	 */
	public cmd_unstuck() {
		super("unstuck");
	}

	/**
	 * 校验状态后施加麻痹，并在延时后传送到绑定点。
	 * Validates state, applies paralyze, and teleports to bind after delay.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
	@Override
	public void execute(final Player player, String... params) {
		if (player.getLifeStats().isAlreadyDead()) {
			PacketSendUtility.sendMessage(player, "You dont have execute this command. You die");
			return;
		}
		if (player.isInPrison()) {
			PacketSendUtility.sendMessage(player, "You can't use the unstuck command when you are in Prison");
			return;
		}

		PacketSendUtility.sendMessage(player, "You are now freeze for 10 secondes before unstuck.");
		player.getEffectController().setAbnormal(AbnormalState.PARALYZE.getId());
		player.getEffectController().updatePlayerEffectIcons();
		player.getEffectController().broadCastEffects();
		PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), 0, 0, 0, (int) TimeUnit.SECONDS.toMillis(10), 0));
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				player.getEffectController().unsetAbnormal(AbnormalState.PARALYZE.getId());
				player.getEffectController().updatePlayerEffectIcons();
				player.getEffectController().broadCastEffects();
				player.getController().cancelUseItem();
				PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), 0, 0, 0, 0, 1));
				TeleportService2.moveToBindLocation(player, true);
			}
		}, (int) TimeUnit.SECONDS.toMillis(10));
	}

}
