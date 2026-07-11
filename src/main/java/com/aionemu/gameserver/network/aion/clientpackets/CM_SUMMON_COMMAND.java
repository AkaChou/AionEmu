package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.summons.SummonMode;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.summons.SummonsService;

/**
 * 客户端召唤物模式指令请求包（跟随、攻击、解散等）。
 * Client packet for summon mode commands (follow, attack, dismiss, etc.).
 */
public class CM_SUMMON_COMMAND extends AionClientPacket {
	private int mode;
	private int targetObjId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SUMMON_COMMAND(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		mode = readC();
		readD();
		readD();
		targetObjId = readD();
	}

	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();
		Summon summon = activePlayer.getSummon();
		SummonMode summonMode = SummonMode.getSummonModeById(mode);
		if (summon != null && summonMode != null) {
			SummonsService.doMode(summonMode, summon, targetObjId, UnsummonType.COMMAND);
		}
	}
}
