package com.aionemu.gameserver.instance.handlers.scripts;

import java.util.Set;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

abstract class AbyssStoreroomInstance extends GeneralInstanceHandler {
	private static final long CHEST_STAGE_DURATION = 5 * 60_000L;
	private static final int[] BARRIERS = { 731580, 700545, 700546, 700547 };
	private static final float[][] CHEST_POSITIONS = {
			{ 478.56662f, 815.6565f, 199.76048f, 70 },
			{ 471.32745f, 834.5498f, 199.76048f, 63 },
			{ 470.52844f, 854.9471f, 199.76048f, 56 },
			{ 477.76843f, 873.94354f, 199.76036f, 50 },
			{ 490.90323f, 889.6053f, 199.76036f, 43 },
			{ 508.64328f, 899.91547f, 199.76036f, 36 },
			{ 528.42053f, 903.5909f, 199.76036f, 29 },
			{ 548.2363f, 900.31604f, 199.76036f, 23 },
			{ 565.53644f, 890.173f, 199.76036f, 16 },
			{ 578.9111f, 874.7958f, 199.76036f, 9 },
			{ 585.83545f, 855.7736f, 199.76036f, 3 },
			{ 586.7527f, 835.4556f, 199.76036f, 116 }
	};

	private final Config config;
	private final Npc[] chests = new Npc[CHEST_POSITIONS.length];

	protected AbyssStoreroomInstance(Config config) {
		this.config = config;
	}

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		restoreSelections();
		restoreBarriers();
		int stage = runtimeState().getInt("storeroom.stage", 0);
		if (runtimeState().getBoolean("storeroom.started", false) && stage < chests.length) {
			spawnChests(stage);
			if (!runtimeState().getBoolean("storeroom.stopped", false)) {
				long deadline = runtimeState().getLong("storeroom.next_deadline", 0);
				if (deadline > 0) {
					scheduleDeadline("chest", deadline, this::expireNextChest);
				}
			}
		}
	}

	private void restoreSelections() {
		int treasurerId = runtimeState().getInt("storeroom.treasurer", 0);
		if (treasurerId == 0) {
			treasurerId = config.treasurerIds()[Rnd.get(0, config.treasurerIds().length - 1)];
			runtimeState().put("storeroom.treasurer", treasurerId);
		}
		if (!runtimeState().getBoolean("storeroom.treasurer_dead", false)) {
			spawn(treasurerId, 527.769f, 212.12146f, 178.46744f, (byte) 90);
		}
		int bossId = runtimeState().getInt("storeroom.boss", 0);
		if (bossId == 0) {
			int bossIndex = Rnd.get(0, config.bossIds().length - 1);
			bossId = config.bossIds()[bossIndex];
			runtimeState().put("storeroom.boss", bossId);
			sendCenterMessage('<' + config.bossNames()[bossIndex] + "> appear!!!");
		}
		if (!runtimeState().getBoolean("storeroom.boss_dead", false)) {
			spawn(bossId, 526.6656f, 845.7792f, 199.44875f, (byte) 90);
		}
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		if (npc.getNpcId() == config.artifactId()) {
			sendCenterMessage("You win effect <Shield Of Compassion>");
			GameEngineServices.skillEngine().getSkill(npc, 276, 10, player).useNoAnimationSkill();
		}
	}

	@Override
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		if (contains(config.treasurerIds(), npcId)) {
			dropItems.add(GameWorldServices.dropRegistrationService()
					.regDropItem(1, 0, npcId, config.keyStartId() + Rnd.get(0, 2), 1));
		} else if (contains(config.bossIds(), npcId)) {
			dropItems.add(GameWorldServices.dropRegistrationService()
					.regDropItem(1, 0, npcId, config.keyStartId() + 4, 1));
		}
	}

	@Override
	public void onDie(Npc npc) {
		int npcId = npc.getNpcId();
		if (contains(config.treasurerIds(), npcId)) {
			runtimeState().put("storeroom.treasurer_dead", true);
			return;
		}
		int barrierIndex = indexOf(config.barrierTriggerIds(), npcId);
		if (barrierIndex >= 0) {
			startBarrierRemoval(barrierIndex);
			return;
		}
		if (contains(config.bossIds(), npcId)) {
			finishBoss();
		}
	}

	private void startBarrierRemoval(int index) {
		String prefix = "storeroom.barrier." + index;
		if (runtimeState().getBoolean(prefix + ".removed", false)
				|| runtimeState().getLong(prefix + ".deadline", 0) > 0) {
			return;
		}
		long deadline = System.currentTimeMillis() + 5_000;
		runtimeState().put(prefix + ".deadline", deadline);
		scheduleDeadline("barrier_" + index, deadline, () -> removeBarrier(index));
	}

	private void restoreBarriers() {
		for (int index = 0; index < BARRIERS.length; index++) {
			String prefix = "storeroom.barrier." + index;
			if (runtimeState().getBoolean(prefix + ".removed", false)) {
				deleteNpc(BARRIERS[index]);
				continue;
			}
			long deadline = runtimeState().getLong(prefix + ".deadline", 0);
			if (deadline > 0) {
				int barrierIndex = index;
				scheduleDeadline("barrier_" + index, deadline, () -> removeBarrier(barrierIndex));
			}
		}
	}

	private void removeBarrier(int index) {
		String prefix = "storeroom.barrier." + index;
		if (runtimeState().getBoolean(prefix + ".removed", false)) {
			return;
		}
		runtimeState().put(prefix + ".removed", true);
		deleteNpc(BARRIERS[index]);
		sendSystemMessage(1401839);
	}

	private void finishBoss() {
		if (runtimeState().getBoolean("storeroom.boss_dead", false)) {
			return;
		}
		runtimeState().put("storeroom.boss_dead", true);
		runtimeState().put("storeroom.stopped", true);
		cancelDeadline("chest");
		for (int doorId : config.completionDoors()) {
			setDoorState(doorId, true);
		}
		instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0)));
	}

	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		if (!runtimeState().getBoolean("storeroom.started", false)) {
			startChestStages();
			return;
		}
		long deadline = runtimeState().getLong("storeroom.next_deadline", 0);
		if (!runtimeState().getBoolean("storeroom.stopped", false) && deadline > System.currentTimeMillis()) {
			PacketSendUtility.sendPacket(player,
					new SM_QUEST_ACTION(0, (int) ((deadline - System.currentTimeMillis()) / 1000)));
		}
	}

	private void startChestStages() {
		runtimeState().put("storeroom.started", true);
		runtimeState().put("storeroom.stage", 0);
		runtimeState().put("storeroom.stopped", false);
		spawnChests(0);
		long deadline = System.currentTimeMillis() + CHEST_STAGE_DURATION;
		runtimeState().put("storeroom.next_deadline", deadline);
		sendCountdown(deadline);
		scheduleDeadline("chest", deadline, this::expireNextChest);
	}

	private void spawnChests(int firstStage) {
		for (int stage = firstStage; stage < CHEST_POSITIONS.length; stage++) {
			float[] position = CHEST_POSITIONS[stage];
			chests[stage] = (Npc) spawn(254574, position[0], position[1], position[2], (byte) position[3]);
		}
	}

	private void expireNextChest() {
		if (runtimeState().getBoolean("storeroom.stopped", false)) {
			return;
		}
		int stage = runtimeState().getInt("storeroom.stage", 0);
		if (stage >= chests.length) {
			return;
		}
		int nextStage = stage + 1;
		runtimeState().put("storeroom.stage", nextStage);
		if (nextStage == chests.length) {
			sendMsg(1400244);
		} else {
			long deadline = runtimeState().getLong("storeroom.next_deadline", 0) + CHEST_STAGE_DURATION;
			runtimeState().put("storeroom.next_deadline", deadline);
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

	private void sendSystemMessage(int messageId) {
		instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(messageId)));
	}

	private void sendCenterMessage(String message) {
		instance.doOnAllPlayers(player -> PacketSendUtility.sendWhiteMessageOnCenter(player, message));
	}

	@Override
	public void onEnterZone(Player player, ZoneInstance zone) {
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get(config.artifactZone())) {
			sendCenterMessage("Use <" + config.artifactName() + "> to receive a skill");
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
		for (int itemId = config.keyStartId(); itemId <= config.keyStartId() + 4; itemId++) {
			storage.decreaseByItemId(itemId, storage.getItemCountByItemId(itemId));
		}
	}

	private void deleteNpc(int npcId) {
		Npc npc = getNpc(npcId);
		if (npc != null) {
			npc.getController().onDelete();
		}
	}

	private static boolean contains(int[] values, int value) {
		return indexOf(values, value) >= 0;
	}

	private static int indexOf(int[] values, int value) {
		for (int index = 0; index < values.length; index++) {
			if (values[index] == value) {
				return index;
			}
		}
		return -1;
	}

	protected record Config(int artifactId, String artifactName, String artifactZone, int[] treasurerIds,
			int[] bossIds, String[] bossNames, int[] barrierTriggerIds, int keyStartId, int[] completionDoors) {
	}
}
