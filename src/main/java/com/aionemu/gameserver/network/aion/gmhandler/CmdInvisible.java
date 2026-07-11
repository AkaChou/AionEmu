package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令：使管理员进入隐身状态。
 * GM command handler that makes the admin invisible.
 *
 * @author Alcapwnd
 */
public class CmdInvisible extends AbstractGMHandler {

	/**
	 * 创建处理器并立即进入隐身。
	 * Creates the handler and immediately applies invisibility.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 指令参数（当前未使用） / command parameters (currently unused)
	 */
	public CmdInvisible(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 设置隐身异常状态与视觉状态并广播。
	 * Sets hide abnormal/visual state and broadcasts the player state.
	 */
	private void run() {
		admin.getEffectController().setAbnormal(AbnormalState.HIDE.getId());
		admin.setVisualState(CreatureVisualState.HIDE20);
		PacketSendUtility.broadcastPacket(admin, new SM_PLAYER_STATE(admin), true);
		PacketSendUtility.sendMessage(admin, "You are invisible.");
	}
}
