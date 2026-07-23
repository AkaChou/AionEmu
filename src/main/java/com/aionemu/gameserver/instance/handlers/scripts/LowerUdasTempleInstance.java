package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(300160000)
public class LowerUdasTempleInstance extends GeneralInstanceHandler {
	private static final long CHEST_STAGE_DURATION = 5 * 60_000L;
	private static final int[] CHEST_IDS = {
			216149, 216149, 216149, 216149, 216149, 216149,
			216150, 216150, 216150, 216150, 216150, 216150
	};
	private static final float[][] CHEST_POSITIONS = {
			{ 445.99957f, 1178.3578f, 193.02937f, 21 },
			{ 448.85532f, 1205.2148f, 191.59023f, 15 },
			{ 452.71637f, 1180.77f, 190.47333f, 85 },
			{ 440.6775f, 1198.4562f, 191.70049f, 50 },
			{ 449.19788f, 1197.8282f, 190.50172f, 24 },
			{ 436.17404f, 1185.6791f, 190.22073f, 13 },
			{ 442.38748f, 1186.572f, 190.88919f, 14 },
			{ 433.22824f, 1198.147f, 192.34004f, 0 },
			{ 462.2652f, 1180.8121f, 191.70518f, 85 },
			{ 455.50082f, 1176.3575f, 192.6768f, 34 },
			{ 436.63177f, 1192.1348f, 190.88254f, 119 },
			{ 438.38586f, 1202.9849f, 192.8323f, 105 }
	};
	private final Npc[] chests = new Npc[CHEST_IDS.length];

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		if (!runtimeState().getBoolean("lower_udas.started", false)) {
			return;
		}
		int stage = runtimeState().getInt("lower_udas.stage", 0);
		if (stage >= CHEST_IDS.length) {
			return;
		}
		spawnChests(stage);
		if (!runtimeState().getBoolean("lower_udas.stopped", false)) {
			long deadline = runtimeState().getLong("lower_udas.next_deadline", 0);
			if (deadline > 0) {
				scheduleDeadline("chest", deadline, this::expireNextChest);
			}
		}
	}

	@Override
	public void onDie(Npc npc) {
		if (npc.getNpcId() == 215795) {
			runtimeState().put("lower_udas.stopped", true);
			cancelDeadline("chest");
			instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0)));
		}
	}

	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		if (!runtimeState().getBoolean("lower_udas.started", false)) {
			startChestStages();
			return;
		}
		long deadline = runtimeState().getLong("lower_udas.next_deadline", 0);
		if (!runtimeState().getBoolean("lower_udas.stopped", false) && deadline > System.currentTimeMillis()) {
			PacketSendUtility.sendPacket(player,
					new SM_QUEST_ACTION(0, (int) ((deadline - System.currentTimeMillis()) / 1000)));
		}
	}

	private void startChestStages() {
		runtimeState().put("lower_udas.started", true);
		runtimeState().put("lower_udas.stage", 0);
		runtimeState().put("lower_udas.stopped", false);
		spawnChests(0);
		long deadline = System.currentTimeMillis() + CHEST_STAGE_DURATION;
		runtimeState().put("lower_udas.next_deadline", deadline);
		sendCountdown(deadline);
		scheduleDeadline("chest", deadline, this::expireNextChest);
	}

	private void spawnChests(int firstStage) {
		for (int stage = firstStage; stage < CHEST_IDS.length; stage++) {
			float[] position = CHEST_POSITIONS[stage];
			chests[stage] = (Npc) spawn(CHEST_IDS[stage], position[0], position[1], position[2],
					(byte) position[3]);
		}
	}

	private void expireNextChest() {
		if (runtimeState().getBoolean("lower_udas.stopped", false)) {
			return;
		}
		int stage = runtimeState().getInt("lower_udas.stage", 0);
		if (stage >= CHEST_IDS.length) {
			return;
		}
		int nextStage = stage + 1;
		runtimeState().put("lower_udas.stage", nextStage);
		if (nextStage == CHEST_IDS.length) {
			sendMsg(1400244);
		} else {
			long deadline = runtimeState().getLong("lower_udas.next_deadline", 0) + CHEST_STAGE_DURATION;
			runtimeState().put("lower_udas.next_deadline", deadline);
			sendCountdown(deadline);
			scheduleDeadline("chest", deadline, this::expireNextChest);
		}
		sendMsg(1400245);
		Npc chest = chests[stage];
		if (chest != null) {
			chest.getController().onDelete();
			chests[stage] = null;
		}
	}

	private void sendCountdown(long deadline) {
		long remaining = deadline - System.currentTimeMillis();
		if (remaining > 0) {
			instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player,
					new SM_QUEST_ACTION(0, (int) (remaining / 1000))));
		}
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
	}

	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(185000086, storage.getItemCountByItemId(185000086));
		storage.decreaseByItemId(185000087, storage.getItemCountByItemId(185000087));
	}
}
