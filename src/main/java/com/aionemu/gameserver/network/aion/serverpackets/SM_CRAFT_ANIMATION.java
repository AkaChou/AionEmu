package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 制作动画包：广播施法者、工作台目标、技能与动作阶段。
 * Server packet for craft animation: caster, workbench target, skill and action phase.
 *
 * @author Mr. Poke
 */
@AllArgsConstructor
public class SM_CRAFT_ANIMATION extends AionServerPacket {

	private final int senderObjectId;
	private final int targetObjectId;
	private final int skillId;
	private final int action;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(senderObjectId);
		writeD(targetObjectId);
		writeH(skillId);
		writeC(action);
	}
}
