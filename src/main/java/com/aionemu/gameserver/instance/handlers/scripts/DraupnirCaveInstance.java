package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 德劳普尼尔洞穴副本事件处理器。
 * Instance event handler for Draupnir Cave.
 *
 * @author Encom
 */

@InstanceID(320080000)
public class DraupnirCaveInstance extends GeneralInstanceHandler
{
	//** NPC 4.9 / NPC 4.9 *//
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
	/** bakarma charger / bakarma charger */
		private int bakarmaCharger;
	/** adjutants killed / adjutants killed */
		private int adjutantsKilled;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		bakarmaCharger = runtimeState().getInt("draupnir.chargers", 0);
		adjutantsKilled = runtimeState().getInt("draupnir.adjutants", 0);
		String race = runtimeState().get("draupnir.race");
		spawnRace = race == null ? null : Race.valueOf(race);
		if (spawnRace != null) {
			SpawnIDDF3DragonSP();
		}
		if (adjutantsKilled >= 4 && !runtimeState().getBoolean("draupnir.bakarma_dead", false)) {
			spawnCommanderBakarma();
		}
		restoreDeadlines();
	}
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(final Player player) {
		if (spawnRace != null) {
			return;
		}
		spawnRace = player.getRace();
		runtimeState().put("draupnir.race", spawnRace.name());
		// 须击杀阿弗兰、萨拉斯瓦蒂、拉克希米与宁巴卡，指挥官巴卡尔玛才会出现。 / You must kill Afrane, Saraswati, Lakshmi, and Nimbarka to make Commander Bakarma appear.
		sendMsg(1400757, 0, false, 25, 10000);
		long deadline = System.currentTimeMillis() + 10_000;
		runtimeState().put("draupnir.phantasm_deadline", deadline);
		scheduleDeadline("phantasm", deadline,
				() -> spawn(237276, 495.48535f, 392.0867f, 616.5717f, (byte) 89));
		SpawnIDDF3DragonSP();
	}
	
	private void SpawnIDDF3DragonSP() {
		final int npc1 = spawnRace == Race.ASMODIANS ? 805737 : 805736;
		spawn(npc1, 498.74973f, 379.33267f, 621.2866f, (byte) 54);
    }
    /**
     * 处理死亡事件。
     * Handle a death event.
     *
     * npc
     */
    @Override
    public void onDie(Npc npc) {
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 213776: //Instructor Afrane.
			case 237264:
			case 213778: //Beautiful Lakshmi.
			case 237265:
			case 213779: //Commander Nimbarka.
			case 237266:
			case 213802: //Kind Saraswati.
			case 237267:
				adjutantsKilled++;
				runtimeState().put("draupnir.adjutants", adjutantsKilled);
				if (adjutantsKilled == 1) {
					// 还须再击杀 3 名副官，指挥官巴卡尔玛才会出现。 / You must kill 3 more Adjutants to make Commander Bakarma appear.
				    sendMsg(1400758, 0, false, 25, 0);
				} else if (adjutantsKilled == 2) {
					// 还须再击杀 2 名副官，指挥官巴卡尔玛才会出现。 / You must kill 2 more Adjutants to make Commander Bakarma appear.
				    sendMsg(1400759, 0, false, 25, 0);
				} else if (adjutantsKilled == 3) {
					// 还须再击杀 1 名副官，指挥官巴卡尔玛才会出现。 / You must kill 1 more Adjutant to make Commander Bakarma appear.
				    sendMsg(1400760, 0, false, 25, 0);
				} else if (adjutantsKilled == 4) {
					spawnCommanderBakarma();
					// 指挥官巴卡尔玛已出现在贝里特拉神谕处。 / Commander Bakarma has appeared at Beritra's Oracle.
				    sendMsg(1400751, 0, false, 25, 0);
					deleteNpc(214026); //Deputy Brigade General Yavant.
				}
			break;
			case 236929: //Commander Bakarma.
				runtimeState().put("draupnir.bakarma_dead", true);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Draupnir Cave>");
				long akhalDeadline = System.currentTimeMillis() + 60_000;
				runtimeState().put("draupnir.akhal_deadline", akhalDeadline);
				scheduleDeadline("akhal", akhalDeadline, this::spawnAkhalStage);
			break;
			case 236900: //Bakarma Charger.
			    bakarmaCharger++;
				runtimeState().put("draupnir.chargers", bakarmaCharger);
				if (bakarmaCharger == 18) {
					runtimeState().put("draupnir.gate_neutralized", true);
					cancelDeadline("gate_raid_1");
					cancelDeadline("gate_raid_2");
					// 欧比斯之门增强器已被中和。 / The Abyss Gate Enhancer has been neutralized.
					sendMsg(1403065, 0, false, 25, 0);
				}
			break;
        }
    }
	
   /**
	 * Central Control Room Raid
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 702857: //Balaur Abyss Gate Enhancer.
				if (runtimeState().getBoolean("draupnir.gate_started", false)) {
					return;
				}
				runtimeState().put("draupnir.gate_started", true);
				despawnNpc(npc);
				// 龙族蜂拥而至，保卫欧比斯之门增强器。 / Balaur are swarming to defend the Abyss Gate Enhancer.
				sendMsg(1403063, 0, false, 25, 0);
				// 龙族已察觉入侵者的存在。 / The Balaur have been alerted to the presence of intruders.
				sendMsg(1403064, 0, false, 25, 4000);
				long raid1 = System.currentTimeMillis() + 5_000;
				long raid2 = System.currentTimeMillis() + 60_000;
				runtimeState().put("draupnir.gate_raid_1", raid1);
				runtimeState().put("draupnir.gate_raid_2", raid2);
				restoreDeadlines();
			break;
			case 702858: //Balaur Abyss Gate Booster.
			    despawnNpc(npc);
				// 在中央控制室找到并过载欧比斯之门增强器。 / Find and overload the Abyss Gate Enhancer in the Central Control Room.
				sendMsg(1403058, 0, false, 25, 0);
				// 龙族的欧比斯之门增强器已激活。 / The Balaur's Abyss Gate Enhancer is active.
				// 增强器防护装置将在 3 分钟后激活，防止被摧毁。 / The enhancer protection device will activate in 3 minutes, preventing it from being destroyed.
				sendMsg(1403081, 0, false, 25, 5000);
				spawn(702857, 469.00000f, 563.0000f, 510.49686f, (byte) 29); //Balaur Abyss Gate Enhancer.
				spawn(702857, 511.36166f, 591.0183f, 510.60300f, (byte) 60); //Balaur Abyss Gate Enhancer.
				spawn(702857, 466.00000f, 617.0000f, 511.22543f, (byte) 96); //Balaur Abyss Gate Enhancer.
			break;
		}
	}
	
	private void spawnCommanderBakarma() {
		spawn(236929, 777.46985f, 431.09888f, 321.7541f, (byte) 62); //Commander Bakarma.
	}
	
	private void spawnAkhal() {
		spawn(237275, 777.46985f, 431.09888f, 321.7541f, (byte) 62); //Akhal.
	}

	private void spawnAkhalStage() {
		if (runtimeState().getBoolean("draupnir.akhal_spawned", false)) {
			return;
		}
		runtimeState().put("draupnir.akhal_spawned", true);
		spawnAkhal();
		sendMsg(1403068, 0, false, 25, 0);
	}

	private void restoreDeadlines() {
		long phantasm = runtimeState().getLong("draupnir.phantasm_deadline", 0);
		if (phantasm > 0) {
			scheduleDeadline("phantasm", phantasm,
					() -> spawn(237276, 495.48535f, 392.0867f, 616.5717f, (byte) 89));
		}
		long akhal = runtimeState().getLong("draupnir.akhal_deadline", 0);
		if (akhal > 0) {
			scheduleDeadline("akhal", akhal, this::spawnAkhalStage);
		}
		if (runtimeState().getBoolean("draupnir.gate_neutralized", false)) {
			return;
		}
		long raid1 = runtimeState().getLong("draupnir.gate_raid_1", 0);
		if (raid1 > 0) {
			scheduleDeadline("gate_raid_1", raid1, this::startAbyssGateRaid1);
		}
		long raid2 = runtimeState().getLong("draupnir.gate_raid_2", 0);
		if (raid2 > 0) {
			scheduleDeadline("gate_raid_2", raid2, () -> {
				sendMsg(1403063, 0, false, 25, 0);
				startAbyssGateRaid2();
			});
		}
	}
	/**
	 * 处理 startAbyssGateRaid1。
	 * Handle startAbyssGateRaid1.
	 */
	
	public void startAbyssGateRaid1() {
	    abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
		abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
		abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
	}
	/**
	 * 处理 startAbyssGateRaid2。
	 * Handle startAbyssGateRaid2.
	 */
	
	public void startAbyssGateRaid2() {
	    abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
		abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
		abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
	}
	
	private void abyssGateRaid(final Npc npc) {
		for (Player player: instance.getPlayersInside()) {
			npc.setTarget(player);
			((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
			npc.setState(1);
			npc.getMoveController().moveToTargetObject();
			PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
		}
	}
	/**
	 * 移除指定 NPC。
	 * Despawn the given NPC.
	 *
	 * npc
	 */
	
	protected void despawnNpc(Npc npc) {
        if (npc != null) {
            npc.getController().onDelete();
        }
    }
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
}
