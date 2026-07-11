package com.aionemu.gameserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.teleport.TeleporterTemplate;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 客户端传送点 NPC 目的地选择请求包。
 * Client packet for selecting a teleport destination from a teleporter NPC.
 */
@Slf4j
public class CM_TELEPORT_SELECT extends AionClientPacket {
	public int targetObjectId;
	public int locId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_TELEPORT_SELECT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		targetObjectId = readD();
		locId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player.getLifeStats().isAlreadyDead()) {
			return;
		}
		AionObject obj = player.getKnownList().getObject(targetObjectId);
		if (obj != null && obj instanceof Npc) {
			Npc npc = (Npc) obj;
			int npcId = npc.getNpcId();
			if (!MathUtil.isInRange(npc, player, npc.getObjectTemplate().getTalkDistance() + 2)) {
				return;
			}
			TeleporterTemplate teleport = DataManager.TELEPORTER_DATA.getTeleporterTemplateByNpcId(npcId);
			if (teleport != null) {
				TeleportService2.teleport(teleport, locId, player, npc, TeleportAnimation.JUMP_ANIMATION);
			} else {
				log.warn(I18n.get("log.aa4841d68464", locId, npcId));
			}
		} else {
			log.debug("player " + player.getName() + " requested npc "
					+ targetObjectId + " for teleportation " + locId + ", but he doesnt have such npc in knownlist");
		}
	}
}
