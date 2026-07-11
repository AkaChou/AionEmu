package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步生物可见状态（visual/see state）的服务端包；常见用途是停止角色登入后的闪烁。
 * Server packet that synchronizes a creature's visual/see state; commonly used to stop the post-login blink.
 * <p>
 * states: 0 - normal char; 1 - crouched invisible char; 64 - standing blinking char; 128 - char is invisible
 *
 * @author Luno, Sweetkr
 */
public class SM_PLAYER_STATE extends AionServerPacket {

	private int playerObjId;
	private int visualState;
	private int seeState;

	/**
	 * @param creature 目标生物（通常为玩家） / target creature (usually a player)
	 */
	public SM_PLAYER_STATE(Creature creature) {
		this.playerObjId = creature.getObjectId();
		this.visualState = creature.getVisualState();
		this.seeState = creature.getSeeState();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjId);
		writeC(visualState);
		writeC(seeState);
		writeC(visualState == 64 ? 0x01 : 0x00);
	}
}
