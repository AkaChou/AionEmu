package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端召唤物施放技能请求包。
 * Client packet for summon skill cast requests.
 */
@Slf4j
public class CM_SUMMON_CASTSPELL extends AionClientPacket {
	private int summonObjId;
	private int targetObjId;
	private int skillId;
	@SuppressWarnings("unused")
	private int skillLvl;
	@SuppressWarnings("unused")
	private float unk;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SUMMON_CASTSPELL(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		summonObjId = readD();
		skillId = readH();
		skillLvl = readC();
		targetObjId = readD();
		unk = readF();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		long currentTime = System.currentTimeMillis();
		if (player.getNextSummonSkillUse() > currentTime) {
			return;
		}
		Summon summon = player.getSummon();
		if (summon == null) {
			return;
		}
		if (summon.getObjectId() != summonObjId) {
			return;
		}
		Creature target = null;
		if (targetObjId != summon.getObjectId()) {
			VisibleObject obj = summon.getKnownList().getObject(targetObjId);
			if (obj instanceof Creature) {
				target = (Creature) obj;
			}
		} else {
			target = summon;
		}
		if (target != null) {
			player.setNextSummonSkillUse(currentTime + 1100);
			summon.getController().useSkill(skillId, target);
		}
	}
}
