package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@InstanceID(300560000)
public class ShugoImperialTombInstance extends GeneralInstanceHandler {
	private static final long[] RAID_WAVE_OFFSETS = {
		10_000L, 30_000L, 50_000L, 70_000L, 90_000L,
		110_000L, 130_000L, 150_000L, 170_000L, 190_000L
	};
	private static final int[] RAID_SPAWN_DELAYS = {1_000, 5_000, 9_000};

	private static final int[] A_NPCS = {219508, 219509, 219510};
	private static final float[][] A_SPOTS = {
		{199.53075f, 270.43457f, 550.5646f},
		{209.68540f, 263.57240f, 550.5646f}
	};
	private static final byte[] A_HEADINGS = {77, 78};
	private static final String[] A_WALKERS = {"ImperialTombUnderpath1", "ImperialTombUnderpath2"};

	private static final int[] B_NPCS = {219514, 219515, 219516};
	private static final float[][] B_SPOTS = {
		{307.80344f, 434.2390f, 298.31903f},
		{307.02597f, 433.8582f, 298.31903f},
		{359.85450f, 421.5649f, 292.48206f},
		{359.61240f, 421.5032f, 292.48206f}
	};
	private static final byte[] B_HEADINGS = {25, 88, 30, 82};
	private static final String[] B_WALKERS = {
		"ImperialTombUnderpath3", "ImperialTombUnderpath4",
		"ImperialTombUnderpath5", "ImperialTombUnderpath6"
	};

	private static final int[] C1_NPCS = {219521, 219522, 219523};
	private static final float[][] C1_SPOTS = {
		{398.80435f, 81.94784f, 223.16089f},
		{398.66214f, 81.80799f, 223.16089f},
		{419.37616f, 90.95251f, 214.33856f}
	};
	private static final byte[] C1_HEADINGS = {8, 8, 8};
	private static final String[] C1_WALKERS = {
		"ImperialTombUnderpath7", "ImperialTombUnderpath8", "ImperialTombUnderpath9"
	};

	private static final int[] C2_NPCS = {219527, 219528, 219529};
	private static final float[][] C2_SPOTS = C1_SPOTS;
	private static final byte[] C2_HEADINGS = C1_HEADINGS;
	private static final String[] C2_WALKERS = C1_WALKERS;

	private final Map<Integer, String> raidSpawnKeys = new ConcurrentHashMap<>();
	private boolean instanceDestroyed;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		restoreRaid("a", A_NPCS, A_SPOTS, A_HEADINGS, A_WALKERS);
		restoreRaid("b", B_NPCS, B_SPOTS, B_HEADINGS, B_WALKERS);
		restoreRaid("c1", C1_NPCS, C1_SPOTS, C1_HEADINGS, C1_WALKERS);
		restoreRaid("c2", C2_NPCS, C2_SPOTS, C2_HEADINGS, C2_WALKERS);
		restoreDelayedSpawn("boss.letu", 219530, 398.80435f, 81.94784f, 223.16089f, (byte) 8,
			"ImperialTombUnderpath7");
		restoreDelayedSpawn("boss.captain", 219531, 398.66214f, 81.80799f, 223.16089f, (byte) 8,
			"ImperialTombUnderpath8");
		restoreFinishedSpawns();
	}

	@Override
	public void onDie(Npc npc) {
		String spawnKey = raidSpawnKeys.remove(npc.getObjectId());
		if (spawnKey != null) {
			runtimeState().put(spawnKey + ".dead", true);
		}
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 219508 -> {
				int kills = increment("tomb.kills.diligent");
				if (kills == 6) {
					beginRaid("a", A_NPCS, A_SPOTS, A_HEADINGS, A_WALKERS);
					scheduleStartMessages("a");
				} else if (kills == 20) {
					completeRaid("a");
					spawnOnce("tomb.finish.a", this::spawnRaidAFinish);
				}
			}
			case 219514 -> {
				int kills = increment("tomb.kills.strong");
				if (kills == 6) {
					beginRaid("b", B_NPCS, B_SPOTS, B_HEADINGS, B_WALKERS);
					scheduleStartMessages("b");
				} else if (kills == 40) {
					completeRaid("b");
					spawnOnce("tomb.finish.b", this::spawnFairyGuardian);
					sendMsgByRaceNow(1402832, Race.PC_ALL);
				}
			}
			case 219521 -> {
				int kills = increment("tomb.kills.swift");
				if (kills == 6) {
					beginRaid("c1", C1_NPCS, C1_SPOTS, C1_HEADINGS, C1_WALKERS);
					scheduleStartMessages("c1");
				} else if (kills == 30) {
					completeRaid("c1");
					scheduleBossSpawn("boss.letu", 219530, 398.80435f, 81.94784f, 223.16089f, (byte) 8,
							"ImperialTombUnderpath7");
					scheduleMessage("tomb.message.c1.finish.0", System.currentTimeMillis(), 1402833, Race.PC_ALL);
					scheduleMessage("tomb.message.c1.finish.1", System.currentTimeMillis() + 5_000L, 1402834, Race.PC_ALL);
				}
			}
			case 219528 -> {
				int kills = increment("tomb.kills.commander");
				if (kills == 30) {
					completeRaid("c2");
					scheduleBossSpawn("boss.captain", 219531, 398.66214f, 81.80799f, 223.16089f, (byte) 8,
							"ImperialTombUnderpath8");
					scheduleMessage("tomb.message.c2.finish.0", System.currentTimeMillis(), 1402833, Race.PC_ALL);
					scheduleMessage("tomb.message.c2.finish.1", System.currentTimeMillis() + 5_000L, 1402834, Race.PC_ALL);
				}
			}
			case 219530 -> {
				runtimeState().put("tomb.boss.letu.killed", true);
				beginRaid("c2", C2_NPCS, C2_SPOTS, C2_HEADINGS, C2_WALKERS);
				scheduleMessage("tomb.message.letu.0", System.currentTimeMillis(), 1402833, Race.PC_ALL);
				scheduleMessage("tomb.message.letu.1", System.currentTimeMillis() + 5_000L, 1402834, Race.PC_ALL);
			}
			case 219531 -> {
				runtimeState().put("tomb.boss.captain.killed", true);
				spawnOnce("tomb.finish.captain", this::spawnCaptainFinish);
			}
			case 219544 -> spawnOnce("tomb.finish.guardian", this::spawnGuardianFinish);
			default -> {
			}
		}
	}

	private int increment(String key) {
		int value = runtimeState().getInt(key, 0) + 1;
		runtimeState().put(key, value);
		return value;
	}

	private void beginRaid(String key, int[] npcs, float[][] spots, byte[] headings, String[] walkers) {
		if (runtimeState().getLong(raidKey(key, "started_at"), 0) > 0) {
			return;
		}
		long startedAt = System.currentTimeMillis();
		runtimeState().put(raidKey(key, "started_at"), startedAt);
		runtimeState().put(raidKey(key, "completed"), false);
		scheduleRaid(key, startedAt, npcs, spots, headings, walkers);
	}

	private void restoreRaid(String key, int[] npcs, float[][] spots, byte[] headings, String[] walkers) {
		long startedAt = runtimeState().getLong(raidKey(key, "started_at"), 0);
		if (startedAt > 0 && !runtimeState().getBoolean(raidKey(key, "completed"), false)) {
			scheduleRaid(key, startedAt, npcs, spots, headings, walkers);
			if (!key.equals("c2")) {
				scheduleStartMessages(key);
			}
		}
	}

	private void scheduleRaid(String key, long startedAt, int[] npcs, float[][] spots, byte[] headings,
			String[] walkers) {
		long now = System.currentTimeMillis();
		for (int wave = 0; wave < RAID_WAVE_OFFSETS.length; wave++) {
			long waveAt = startedAt + RAID_WAVE_OFFSETS[wave];
			for (int spot = 0; spot < spots.length; spot++) {
				for (int mob = 0; mob < npcs.length; mob++) {
					String spawnKey = raidKey(key, "wave." + wave + ".spot." + spot + ".mob." + mob);
					int npcId = npcs[mob];
					float[] position = spots[spot];
					byte heading = headings[spot];
					String walkerId = walkers[spot];
					long spawnAt = waveAt + RAID_SPAWN_DELAYS[mob];
					if (spawnAt <= now) {
						spawnRaidNpc(key, spawnKey, npcId, position, heading, walkerId);
					} else {
						scheduleDeadline(spawnKey, spawnAt,
							() -> spawnRaidNpc(key, spawnKey, npcId, position, heading, walkerId));
					}
				}
			}
			if ((wave & 1) == 1) {
				scheduleDeadline(raidKey(key, "wave." + wave + ".message"), waveAt,
						() -> sendRaidMessage(key));
			}
		}
	}

	private void spawnRaidNpc(String key, String spawnKey, int npcId, float[] position, byte heading, String walkerId) {
		if (instanceDestroyed || runtimeState().getBoolean(raidKey(key, "completed"), false)
				|| runtimeState().getBoolean(spawnKey + ".dead", false)) {
			return;
		}
		Npc npc = (Npc) spawn(npcId, position[0], position[1], position[2], heading);
		raidSpawnKeys.put(npc.getObjectId(), spawnKey);
		npc.getSpawn().setWalkerId(walkerId);
		WalkManager.startWalking((NpcAI2) npc.getAi2());
	}

	private void sendRaidMessage(String key) {
		if (!instanceDestroyed && !runtimeState().getBoolean(raidKey(key, "completed"), false)) {
			sendMsgByRaceNow(1401607, Race.PC_ALL);
		}
	}

	private void completeRaid(String key) {
		runtimeState().put(raidKey(key, "completed"), true);
	}

	private String raidKey(String raid, String suffix) {
		return "tomb.raid." + raid + "." + suffix;
	}

	private void scheduleStartMessages(String raid) {
		long startedAt = runtimeState().getLong(raidKey(raid, "started_at"), 0);
		scheduleMessage("tomb.message." + raid + ".start.0", startedAt, 1401586, Race.PC_ALL);
		scheduleMessage("tomb.message." + raid + ".start.1", startedAt + 5_000L, 1402833, Race.PC_ALL);
		scheduleMessage("tomb.message." + raid + ".start.2", startedAt + 10_000L, 1402834, Race.PC_ALL);
	}

	private void scheduleMessage(String key, long deadline, int messageId, Race race) {
		scheduleDeadline(key, deadline, () -> {
			if (!instanceDestroyed) {
				sendMsgByRaceNow(messageId, race);
			}
		});
	}

	private void sendMsgByRaceNow(int messageId, Race race) {
		instance.doOnAllPlayers(player -> {
			if (race == Race.PC_ALL || player.getRace() == race) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(messageId));
			}
		});
	}

	private void scheduleBossSpawn(String key, int npcId, float x, float y, float z, byte heading, String walkerId) {
		String atKey = "tomb.spawn." + key + ".at";
		String spawnedKey = "tomb.spawn." + key + ".spawned";
		if (runtimeState().getBoolean(spawnedKey, false) || bossKilled(key)) {
			return;
		}
		long deadline = runtimeState().getLong(atKey, 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + 2_000L;
			runtimeState().put(atKey, deadline);
		}
		scheduleDeadline("tomb.spawn." + key, deadline, () -> {
			if (instanceDestroyed || runtimeState().getBoolean(spawnedKey, false) || bossKilled(key)) {
				return;
			}
			spawnWalking(npcId, x, y, z, heading, walkerId);
			runtimeState().put(spawnedKey, true);
		});
	}

	private void restoreDelayedSpawn(String key, int npcId, float x, float y, float z, byte heading, String walkerId) {
		if (bossKilled(key)) {
			return;
		}
		if (runtimeState().getBoolean("tomb.spawn." + key + ".spawned", false)) {
			spawnWalking(npcId, x, y, z, heading, walkerId);
		} else if (runtimeState().getLong("tomb.spawn." + key + ".at", 0) > 0) {
			scheduleBossSpawn(key, npcId, x, y, z, heading, walkerId);
		}
	}

	private boolean bossKilled(String key) {
		return runtimeState().getBoolean("tomb." + key + ".killed", false);
	}

	private void spawnWalking(int npcId, float x, float y, float z, byte heading, String walkerId) {
		Npc npc = (Npc) spawn(npcId, x, y, z, heading);
		npc.getSpawn().setWalkerId(walkerId);
		WalkManager.startWalking((NpcAI2) npc.getAi2());
	}

	private void restoreFinishedSpawns() {
		if (runtimeState().getBoolean("tomb.finish.a", false)) {
			spawnRaidAFinish();
		}
		if (runtimeState().getBoolean("tomb.finish.guardian", false)) {
			spawnGuardianFinish();
		} else if (runtimeState().getBoolean("tomb.finish.b", false)) {
			spawnFairyGuardian();
		}
		if (runtimeState().getBoolean("tomb.finish.captain", false)) {
			spawnCaptainFinish();
		}
	}

	private void spawnRaidAFinish() {
		spawn(831095, 344.28635f, 425.418f, 294.75867f, (byte) 56);
		spawn(831114, 183.95969f, 237.51074f, 536.16974f, (byte) 71);
		spawn(831111, 340.27893f, 426.2435f, 294.7574f, (byte) 56);
	}

	private void spawnCaptainFinish() {
		deleteNpc(831130);
		spawn(831116, 443.322f, 110.39832f, 212.20023f, (byte) 92);
		spawn(831119, 440.2393f, 109.80865f, 212.20023f, (byte) 94);
		spawn(831350, 452.43765f, 106.14462f, 212.20023f, (byte) 68);
	}

	private void spawnGuardianFinish() {
		spawn(831095, 465.13556f, 111.26043f, 214.702f, (byte) 8);
		spawn(831115, 329.33588f, 432.96265f, 294.76144f, (byte) 100);
		spawn(831112, 452.43765f, 106.14462f, 212.20023f, (byte) 68);
	}

	private void spawnFairyGuardian() {
		spawn(219544, 315.94565f, 431.73035f, 294.58875f, (byte) 116);
		spawn(219505, 314.94418f, 428.22006f, 294.58875f, (byte) 115);
		spawn(219505, 318.11328f, 427.66050f, 294.58875f, (byte) 115);
		spawn(219505, 319.69778f, 434.42917f, 294.58875f, (byte) 115);
		spawn(219505, 316.08636f, 435.35806f, 294.58875f, (byte) 115);
		spawn(219505, 321.05954f, 430.44263f, 294.58875f, (byte) 115);
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		if (npc.getNpcId() == 831095) {
			GameEngineServices.skillEngine().getSkill(npc, 21096, 60, player).useNoAnimationSkill();
		}
	}

	public void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(182006989, storage.getItemCountByItemId(182006989));
		storage.decreaseByItemId(182006990, storage.getItemCountByItemId(182006990));
		storage.decreaseByItemId(182006991, storage.getItemCountByItemId(182006991));
		storage.decreaseByItemId(182006999, storage.getItemCountByItemId(182006999));
	}

	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(21096);
	}

	private void deleteNpc(int npcId) {
		Npc npc = getNpc(npcId);
		if (npc != null) {
			npc.getController().onDelete();
		}
	}

	private void spawnOnce(String key, Runnable action) {
		if (runtimeState().getBoolean(key, false)) {
			return;
		}
		runtimeState().put(key, true);
		action.run();
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
		removeEffects(player);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
		removeEffects(player);
	}

	@Override
	public void onInstanceDestroy() {
		instanceDestroyed = true;
	}
}
