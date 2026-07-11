package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 制作动画包：广播施法者、工作台目标、技能与动作阶段。
 * Server packet for craft animation: caster, workbench target, skill and action phase.
 *
 * @author Mr. Poke
 */
public class SM_CRAFT_ANIMATION extends AionServerPacket {

	private int senderObjectId;
	private int targetObjectId;
	private int skillId;
	private int action;

	/**
	 * @param senderObjectId 施法者对象 ID / caster object id
	 * @param targetObjectId 工作台/目标对象 ID / workbench target object id
	 * craft skill id
	 * @param action         动画动作阶段 / animation action phase
	 */
	public SM_CRAFT_ANIMATION(int senderObjectId, int targetObjectId, int skillId, int action) {
		this.senderObjectId = senderObjectId;
		this.targetObjectId = targetObjectId;
		this.skillId = skillId;
		this.action = action;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(senderObjectId);
		writeD(targetObjectId);
		writeH(skillId);
		writeC(action);
	}
}
