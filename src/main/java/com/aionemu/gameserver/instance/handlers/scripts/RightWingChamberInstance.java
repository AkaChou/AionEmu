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

@InstanceID(300090000)
public class RightWingChamberInstance extends GeneralInstanceHandler {
	private static final int[] TREASURE_BOX_IDS = { 700469, 700470, 700471, 701481, 701486,
		702800, 702801, 702802, 702805, 702806 };

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		if (runtimeState().getBoolean("rightwing.expired", false)) {
			deleteTreasureBoxes();
			return;
		}
		long deadline = runtimeState().getLong("rightwing.deadline", 0);
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
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("RIGHT_WING_CHAMBER_TIMER_300090000")) {
			startTimer();
		}
	}

	private synchronized void startTimer() {
		if (runtimeState().getLong("rightwing.deadline", 0) != 0
				|| runtimeState().getBoolean("rightwing.expired", false)) {
			return;
		}
		long deadline = System.currentTimeMillis() + 900_000;
		runtimeState().put("rightwing.deadline", deadline);
		scheduleDeadline("treasure", deadline, this::expireTreasure);
		instance.doOnAllPlayers(player -> {
			if (player.isOnline()) {
				PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 900));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_START_IDABRE);
			}
		});
	}

	private void syncTimer(Player player) {
		long deadline = runtimeState().getLong("rightwing.deadline", 0);
		if (runtimeState().getBoolean("rightwing.expired", false)) {
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
		if (runtimeState().getBoolean("rightwing.expired", false)) {
			return;
		}
		runtimeState().put("rightwing.expired", true);
		sendMsg(1400244);
		deleteTreasureBoxes();
	}

	private void deleteTreasureBoxes() {
		for (int npcId : TREASURE_BOX_IDS) {
			instance.getNpcs(npcId).forEach(npc -> npc.getController().onDelete());
		}
	}
}
