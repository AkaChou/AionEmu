package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员隐身切换命令：在 HIDE3 隐身与可见状态间切换。
 * Admin invisibility toggle command: switch between HIDE3 invisibility and visible state.
 *
 * @author Divinity
 */
public class Invis extends AdminCommand {

	public Invis() {
		super("invis");
	}

	/**
	 * 切换执行者的隐身/可见状态并广播玩家状态包。
	 * Toggle the invoker's invisibility/visibility and broadcast player state.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * Unused
	 */
	@Override
	public void execute(Player player, String... params) {
		if (player.getVisualState() < 3) {
			player.getEffectController().setAbnormal(AbnormalState.HIDE.getId());
			player.setVisualState(CreatureVisualState.HIDE3);
			PacketSendUtility.broadcastPacket(player, new SM_PLAYER_STATE(player), true);
			PacketSendUtility.sendMessage(player, "You are invisible.");
		}
		else {
			player.getEffectController().unsetAbnormal(AbnormalState.HIDE.getId());
			player.unsetVisualState(CreatureVisualState.HIDE3);
			PacketSendUtility.broadcastPacket(player, new SM_PLAYER_STATE(player), true);
			PacketSendUtility.sendMessage(player, "You are visible.");
		}
	}

}
