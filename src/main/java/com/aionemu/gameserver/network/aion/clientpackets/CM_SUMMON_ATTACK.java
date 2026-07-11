package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端召唤物攻击请求包。
 * Client packet for summon attack requests.
 */
@Slf4j
public class CM_SUMMON_ATTACK extends AionClientPacket {

	private int summonObjId;
	private int targetObjId;
	@SuppressWarnings("unused")
	private int unk1;
	private int time;
	@SuppressWarnings("unused")
	private int unk3;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SUMMON_ATTACK(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		summonObjId = readD();
		targetObjId = readD();
		unk1 = readC();
		time = readH();
		unk3 = readC();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		Summon summon = player.getSummon();
		if (summon == null) {
			return;
		}
		if (summon.getObjectId() != summonObjId) {
			return;
		}
		VisibleObject obj = summon.getKnownList().getObject(targetObjId);
		if (obj != null && obj instanceof Creature) {
			summon.getController().attackTarget((Creature) obj, time);
		}
	}
}
