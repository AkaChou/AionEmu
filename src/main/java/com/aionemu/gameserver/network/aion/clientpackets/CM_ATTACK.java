package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 普通攻击目标的客户端包。
 * Client packet to perform a basic attack on a target.
 *
 * @author alexa026, Avol, ATracer, KID
 */
@Slf4j
public class CM_ATTACK extends AionClientPacket {

	/**
	 * 客户端要对话的目标对象 ID，0 表示取消选择 / Target object id that client wants to TALK WITH or 0 if wants to unselect
	 */
	private int targetObjectId;
	private int time;

	public CM_ATTACK(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		targetObjectId = readD();
		readC();
		time = readH();
		readC();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player.getLifeStats().isAlreadyDead()) {
			return;
		}

		if (player.isProtectionActive()) {
			player.getController().stopProtectionActiveTask();
		}

		VisibleObject obj = player.getKnownList().getObject(targetObjectId);
		if (obj != null && obj instanceof Creature) {
			player.getController().attackTarget((Creature) obj, time);
		} else {
			if (obj != null) {
				log.warn(I18n.get("log.78ca4b765739", obj, obj.getObjectTemplate().getTemplateId()));
			}
		}
	}
}
