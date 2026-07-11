package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.ArmsfusionService;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 解除武器融合的客户端包。
 * Client packet to break (unfuse) weapons.
 *
 * @author zdead
 */
public class CM_BREAK_WEAPONS extends AionClientPacket {

	public CM_BREAK_WEAPONS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	private int npcObjId;
	private int weaponToBreakUniqueId;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		npcObjId = readD();
		weaponToBreakUniqueId = readD();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		VisibleObject target = player.getTarget();
		if (target instanceof Npc && target.getObjectId() == npcObjId) {
			ArmsfusionService.breakWeapons(player, weaponToBreakUniqueId);
		} else {
			AuditLogger.info(player, "tried to defuse a weapon without targeting the requested NPC");
		}
	}
}
