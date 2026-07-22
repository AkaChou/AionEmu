package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(300080000)
public class LeftWingChamberInstance extends GeneralInstanceHandler {
	private static final long CHEST_STAGE_DURATION = 5 * 60_000L;
	private static final int CHEST_STAGE_COUNT = 6;
	private static final int[] TIMED_CRATE_IDS = { 700465, 700466, 700467, 700468,
		702796, 702797, 702798, 702799 };
	private static final int[] TREASURE_BOX_IDS = { 700465, 700466, 700467, 700468,
		701482, 701487, 702796, 702797, 702798, 702799, 702803, 702804 };

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		if (!runtimeState().getBoolean("leftwing.started", false)
				|| runtimeState().getBoolean("leftwing.complete", false)) {
			if (runtimeState().getBoolean("leftwing.complete", false)) {
				deleteTreasureBoxes();
			}
			return;
		}
		int stage = runtimeState().getInt("leftwing.stage", 0);
		if (stage >= CHEST_STAGE_COUNT) {
			runtimeState().put("leftwing.complete", true);
			deleteTreasureBoxes();
			return;
		}
		long deadline = runtimeState().getLong("leftwing.next_deadline", 0);
		if (deadline > 0) {
			scheduleDeadline("chest", deadline, this::expireNextChest);
		}
	}

	@Override
	public void onEnterInstance(Player player) {
		long deadline = runtimeState().getLong("leftwing.next_deadline", 0);
		if (runtimeState().getBoolean("leftwing.started", false)
				&& !runtimeState().getBoolean("leftwing.complete", false)
				&& deadline > System.currentTimeMillis()) {
			PacketSendUtility.sendPacket(player,
					new SM_QUEST_ACTION(0, (int) ((deadline - System.currentTimeMillis()) / 1000)));
		}
	}

	@Override
	public void onDie(Npc npc) {
		switch (npc.getNpcId()) {
			case 219617 -> npc.getController().onDelete();
			case 215424 -> startChestStages();
		}
	}

	private void startChestStages() {
		if (runtimeState().getBoolean("leftwing.started", false)) {
			return;
		}
		runtimeState().put("leftwing.started", true);
		runtimeState().put("leftwing.stage", 0);
		runtimeState().put("leftwing.complete", false);
		long deadline = System.currentTimeMillis() + CHEST_STAGE_DURATION;
		runtimeState().put("leftwing.next_deadline", deadline);
		sendCountdown(deadline);
		scheduleDeadline("chest", deadline, this::expireNextChest);
	}

	private void expireNextChest() {
		if (runtimeState().getBoolean("leftwing.complete", false)) {
			return;
		}
		int stage = runtimeState().getInt("leftwing.stage", 0);
		if (stage >= CHEST_STAGE_COUNT) {
			runtimeState().put("leftwing.complete", true);
			deleteTreasureBoxes();
			return;
		}
		Npc chest = nextTimedCrate();
		if (chest != null) {
			chest.getController().onDelete();
		}
		int nextStage = stage + 1;
		runtimeState().put("leftwing.stage", nextStage);
		if (nextStage == CHEST_STAGE_COUNT) {
			runtimeState().put("leftwing.complete", true);
			sendMsg(1400244);
			deleteTreasureBoxes();
		} else {
			long deadline = runtimeState().getLong("leftwing.next_deadline", 0) + CHEST_STAGE_DURATION;
			runtimeState().put("leftwing.next_deadline", deadline);
			sendCountdown(deadline);
			scheduleDeadline("chest", deadline, this::expireNextChest);
		}
		sendMsg(1400245);
	}

	private Npc nextTimedCrate() {
		for (int npcId : TIMED_CRATE_IDS) {
			for (Npc npc : instance.getNpcs(npcId)) {
				if (npc.isSpawned()) {
					return npc;
				}
			}
		}
		return null;
	}

	private void deleteTreasureBoxes() {
		for (int npcId : TREASURE_BOX_IDS) {
			instance.getNpcs(npcId).forEach(npc -> npc.getController().onDelete());
		}
	}

	private void sendCountdown(long deadline) {
		long remaining = deadline - System.currentTimeMillis();
		if (remaining > 0) {
			instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player,
					new SM_QUEST_ACTION(0, (int) (remaining / 1000))));
		}
	}
}
