package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端技能皮肤/动画切换请求包。
 * Client packet for activating or deactivating a skill skin animation.
 *
 * @author FrozenKiller
 */
public class CM_SKILL_ANIMATION extends AionClientPacket {

	private int SkillId;
	private int SkillSkinId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SKILL_ANIMATION(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	protected void readImpl() {
		SkillId = readH();
		SkillSkinId = readH();
	}

	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (SkillSkinId > 0) {
			player.getSkillSkinList().setActive(SkillSkinId);
		} else {
			player.getSkillSkinList().setDeactive(SkillId);
		}
	}
}
