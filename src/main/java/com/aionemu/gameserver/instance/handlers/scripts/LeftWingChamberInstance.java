package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

@InstanceID(300080000)
public class LeftWingChamberInstance extends GeneralInstanceHandler {
	private static final int[] TREASURE_BOX_IDS = { 700465, 700466, 700467, 700468, 701482, 701487,
		702796, 702797, 702798, 702799, 702803, 702804 };

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		if (runtimeState().getBoolean("leftwing.expired", false)) {
			deleteTreasureBoxes();
			return;
		}
		long deadline = runtimeState().getLong("leftwing.deadline", 0);
		if (deadline > 0) {
			scheduleDeadline("treasure", deadline, this::expireTreasure);
		}
	}

	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		syncTimer(player);
	}

	@Override
	public void onEnterZone(Player player, ZoneInstance zone) {
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("LEFT_WING_CHAMBER_TIMER_300080000")) {
			startTimer();
		}
	}

	private synchronized void startTimer() {
		if (runtimeState().getLong("leftwing.deadline", 0) != 0
				|| runtimeState().getBoolean("leftwing.expired", false)) {
			return;
		}
		long deadline = System.currentTimeMillis() + 900_000;
		runtimeState().put("leftwing.deadline", deadline);
		scheduleDeadline("treasure", deadline, this::expireTreasure);
		instance.doOnAllPlayers(player -> {
			if (player.isOnline()) {
				PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 900));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_START_IDABRE);
			}
		});
	}

	private void syncTimer(Player player) {
		long deadline = runtimeState().getLong("leftwing.deadline", 0);
		if (runtimeState().getBoolean("leftwing.expired", false)) {
			return;
		}
		if (deadline > 0 && deadline <= System.currentTimeMillis()) {
			expireTreasure();
		} else if (deadline > System.currentTimeMillis()) {
			PacketSendUtility.sendPacket(player,
				new SM_QUEST_ACTION(0, (int) ((deadline - System.currentTimeMillis()) / 1000)));
		}
	}

	private void expireTreasure() {
		if (runtimeState().getBoolean("leftwing.expired", false)) {
			return;
		}
		runtimeState().put("leftwing.expired", true);
		sendMsg(1400244);
		deleteTreasureBoxes();
	}

	private void deleteTreasureBoxes() {
		for (int npcId : TREASURE_BOX_IDS) {
			instance.getNpcs(npcId).forEach(npc -> npc.getController().onDelete());
		}
	}
}
