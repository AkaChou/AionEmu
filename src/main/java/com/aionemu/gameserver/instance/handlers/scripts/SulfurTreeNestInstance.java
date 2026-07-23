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

@InstanceID(300060000)
public class SulfurTreeNestInstance extends GeneralInstanceHandler {
	private static final int[] TREASURE_BOX_IDS = { 214804, 700462, 700463, 700464, 701480, 701485,
		702793, 702794, 702795, 702807, 702808, 702809 };

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		if (runtimeState().getBoolean("sulfur.expired", false)) {
			deleteTreasureBoxes();
			return;
		}
		long deadline = runtimeState().getLong("sulfur.deadline", 0);
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
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("SULFUR_TREE_NEST_TIMER_300060000")) {
			startTimer();
		}
	}

	private synchronized void startTimer() {
		if (runtimeState().getLong("sulfur.deadline", 0) != 0
				|| runtimeState().getBoolean("sulfur.expired", false)) {
			return;
		}
		long deadline = System.currentTimeMillis() + 900_000;
		runtimeState().put("sulfur.deadline", deadline);
		scheduleDeadline("treasure", deadline, this::expireTreasure);
		instance.doOnAllPlayers(player -> {
			if (player.isOnline()) {
				PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 900));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_START_IDABRE);
			}
		});
	}

	private void syncTimer(Player player) {
		long deadline = runtimeState().getLong("sulfur.deadline", 0);
		if (runtimeState().getBoolean("sulfur.expired", false)) {
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
		if (runtimeState().getBoolean("sulfur.expired", false)) {
			return;
		}
		runtimeState().put("sulfur.expired", true);
		sendMsg(1400244);
		deleteTreasureBoxes();
	}

	private void deleteTreasureBoxes() {
		for (int npcId : TREASURE_BOX_IDS) {
			instance.getNpcs(npcId).forEach(npc -> npc.getController().onDelete());
		}
	}
}
