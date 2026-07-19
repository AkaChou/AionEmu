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
	private static final float[][] CHEST_POSITIONS = {
			{ 212.09007f, 741.0567f, 366.20367f, 10 },
			{ 239.12955f, 755.24274f, 365.43304f, 102 },
			{ 210.2166f, 697.1134f, 365.69165f, 99 },
			{ 188.26668f, 675.905f, 365.71332f, 7 },
			{ 182.42268f, 631.6112f, 366.24146f, 111 },
			{ 181.081f, 608.83777f, 365.52753f, 99 },
			{ 181.32057f, 561.24915f, 365.01053f, 113 },
			{ 181.63654f, 539.41473f, 365.01053f, 15 },
			{ 191.39304f, 495.07608f, 366.49414f, 65 },
			{ 197.46051f, 471.78418f, 365.32578f, 82 },
			{ 223.41487f, 409.03143f, 365.01053f, 26 },
			{ 213.39343f, 425.5012f, 366.57892f, 8 }
	};
	private final Npc[] treasureBoxes = new Npc[CHEST_POSITIONS.length];

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		if (!runtimeState().getBoolean("leftwing.started", false)
				|| runtimeState().getBoolean("leftwing.complete", false)) {
			return;
		}
		int stage = runtimeState().getInt("leftwing.stage", 0);
		if (stage >= CHEST_POSITIONS.length) {
			runtimeState().put("leftwing.complete", true);
			return;
		}
		spawnTreasureBoxes(stage);
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
		spawnTreasureBoxes(0);
		long deadline = System.currentTimeMillis() + CHEST_STAGE_DURATION;
		runtimeState().put("leftwing.next_deadline", deadline);
		sendCountdown(deadline);
		scheduleDeadline("chest", deadline, this::expireNextChest);
	}

	private void spawnTreasureBoxes(int firstStage) {
		for (int stage = firstStage; stage < CHEST_POSITIONS.length; stage++) {
			float[] position = CHEST_POSITIONS[stage];
			treasureBoxes[stage] = (Npc) spawn(700465, position[0], position[1], position[2], (byte) position[3]);
		}
	}

	private void expireNextChest() {
		if (runtimeState().getBoolean("leftwing.complete", false)) {
			return;
		}
		int stage = runtimeState().getInt("leftwing.stage", 0);
		if (stage >= CHEST_POSITIONS.length) {
			runtimeState().put("leftwing.complete", true);
			return;
		}
		int nextStage = stage + 1;
		runtimeState().put("leftwing.stage", nextStage);
		if (nextStage == CHEST_POSITIONS.length) {
			runtimeState().put("leftwing.complete", true);
			sendMsg(1400244);
		} else {
			long deadline = runtimeState().getLong("leftwing.next_deadline", 0) + CHEST_STAGE_DURATION;
			runtimeState().put("leftwing.next_deadline", deadline);
			sendCountdown(deadline);
			scheduleDeadline("chest", deadline, this::expireNextChest);
		}
		sendMsg(1400245);
		Npc chest = treasureBoxes[stage];
		if (chest != null) {
			chest.getController().onDelete();
			treasureBoxes[stage] = null;
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
