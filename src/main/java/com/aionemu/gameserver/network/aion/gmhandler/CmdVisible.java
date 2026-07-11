package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令：取消管理员隐身，恢复可见。
 * GM command handler that makes the admin visible again.
 *
 * @author Alcapwnd
 */
public class CmdVisible extends AbstractGMHandler {

	/**
	 * 创建处理器并立即取消隐身。
	 * Creates the handler and immediately removes invisibility.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 指令参数（当前未使用） / command parameters (currently unused)
	 */
	public CmdVisible(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 清除隐身异常与视觉状态并广播。
	 * Clears hide abnormal/visual state and broadcasts the player state.
	 */
	private void run() {
		admin.getEffectController().unsetAbnormal(AbnormalState.HIDE.getId());
		admin.unsetVisualState(CreatureVisualState.HIDE20);
		PacketSendUtility.broadcastPacket(admin, new SM_PLAYER_STATE(admin), true);
		PacketSendUtility.sendMessage(admin, "You are visible.");
	}
}
