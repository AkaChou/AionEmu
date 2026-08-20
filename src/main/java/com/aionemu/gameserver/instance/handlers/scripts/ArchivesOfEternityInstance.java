package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 永恒档案库副本事件处理器。
 * Instance event handler for Archives Of Eternity.
 *
 * @author Encom
 */

@InstanceID(301540000)
public class ArchivesOfEternityInstance extends GeneralInstanceHandler
{
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 进入提示是否已为本副本安排 / whether entry messages were scheduled for this instance */
	private boolean entryMessagesScheduled;
	/** 最终 Boss 出口是否已生成 / whether final-boss exits were spawned */
	private boolean finalBossRewardSpawned;
	/**
	 * 永恒档案库秘密房间的密码背包位置。
	 * Cryptograph Cube positions in the Archives Of Eternity secret rooms.
	 *
	 * <p>这些坐标与零售 condition-spawn 数据一致；每次副本只生成其中一个密码背包。
	 * These coordinates match the retail condition-spawn data; one cube is spawned per instance.</p>
	 */
	private static final List<SecretCubeSpot> SECRET_CUBE_SPOTS = List.of(
		new SecretCubeSpot(345.740784f, 392.683441f, 469.52179f, 120),
		new SecretCubeSpot(345.266724f, 631.750732f, 469.52179f, 240),
		new SecretCubeSpot(668.620728f, 630.289856f, 469.52179f, 300),
		new SecretCubeSpot(414.774414f, 352.004883f, 469.52179f, 0),
		new SecretCubeSpot(599.046082f, 352.676544f, 469.52179f, 180),
		new SecretCubeSpot(414.852631f, 671.289978f, 469.52179f, 0),
		new SecretCubeSpot(668.367615f, 392.107056f, 469.52179f, 60),
		new SecretCubeSpot(598.984558f, 672.073608f, 469.52179f, 180));

	private record SecretCubeSpot(float x, float y, float z, float heading) {
	}
	
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * @param npc NPC / npc
	 */
	@Override
	public void onDropRegistered(Npc npc) {
		if (npc == null || instance == null) {
			return;
		}
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		if (dropItems == null) {
			return;
		}
		int npcObjectId = npc.getObjectId();
		switch (npc.getNpcId()) {
			case 857452, 857456, 857459, 857460, 857462, 857464 -> registerNamedBossDrops(dropItems, npcObjectId);
		}
	}

	private void registerNamedBossDrops(Set<DropItem> dropItems, int npcObjectId) {
		addDrop(dropItems, 0, npcObjectId, 188058413, 1);
		for (Player player : instance.getPlayersInside()) {
			if (!player.isOnline()) {
				continue;
			}
			int playerObjectId = player.getObjectId();
			addDrop(dropItems, playerObjectId, npcObjectId, 166040001, 1);
			addDrop(dropItems, playerObjectId, npcObjectId, 188100344, 1);
			addDrop(dropItems, playerObjectId, npcObjectId, 188100345, 1);
			addDrop(dropItems, playerObjectId, npcObjectId, 188100346, 1);
			addDrop(dropItems, playerObjectId, npcObjectId, 188100347, 1);
		}
		int randomReward = switch (Rnd.get(1, 5)) {
			case 1 -> 190080005;
			case 2 -> 190080006;
			case 3 -> 190080007;
			case 4 -> 190080008;
			default -> 190200000;
		};
		addDrop(dropItems, 0, npcObjectId, randomReward, randomReward == 190200000 ? 50 : 3);
	}

	private void addDrop(Set<DropItem> dropItems, int playerObjectId, int npcObjectId, int itemId, long count) {
		int nextIndex = dropItems.stream().mapToInt(DropItem::getIndex).max().orElse(0) + 1;
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(
			nextIndex, playerObjectId, npcObjectId, itemId, count));
	}
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		player.getController().updateNearbyQuests();
		if (!entryMessagesScheduled) {
			entryMessagesScheduled = true;
			// 与代理人交谈。 / Talk with the Agent.
			sendMsgByRace(1403340, Race.PC_ALL, 5000);
			// 须摧毁奥德封印才能进入。 / You must destroy the Aether seals to enter.
			sendMsgByRace(1403210, Race.PC_ALL, 30000);
			// 古物学家已开始激活永恒遗物。 / The Antiquarian has begun activating the Eternity Relics.
			sendMsgByRace(1403212, Race.PC_ALL, 60000);
			// 阿特雷亚古物学家已激活全部永恒遗物。 / The Antiquarian of Atreia has activated all Eternity Relics.
			sendMsgByRace(1403213, Race.PC_ALL, 120000);
		}
		if (spawnRace == null) {
			spawnRace = player.getRace();
			spawnHistoriesOfAtreia();
			spawnEmpyreanHistories();
			spawnLibraryGuardianRace();
			spawnRecordsFromTheEraOfMen();
		}
	}
	
	private void spawnLibraryGuardianRace() {
        final int libraryGuardian = spawnRace == Race.ASMODIANS ? 806151 : 806150;
		spawn(libraryGuardian, 737.3133f, 490.04956f, 468.99835f, (byte) 31);
        spawn(libraryGuardian, 718.2463f, 501.0919f, 468.99835f, (byte) 9);
        spawn(libraryGuardian, 718.2918f, 522.9931f, 468.99835f, (byte) 111);
        spawn(libraryGuardian, 737.1272f, 533.95374f, 468.99835f, (byte) 90);
        spawn(libraryGuardian, 756.2984f, 523.1093f, 468.99835f, (byte) 71);
        spawn(libraryGuardian, 756.3234f, 501.1647f, 468.99835f, (byte) 51);
    }
	private void spawnHistoriesOfAtreia() {
        final int historiesOfAtreia = spawnRace == Race.ASMODIANS ? 703149 : 703131;
		spawn(historiesOfAtreia, 625.27313f, 500.36285f, 468.95096f, (byte) 0, 133);
		spawn(historiesOfAtreia, 619.94202f, 422.01804f, 468.95096f, (byte) 0, 137);
		spawn(historiesOfAtreia, 620.38477f, 600.65179f, 468.95096f, (byte) 0, 220);
		spawn(historiesOfAtreia, 569.55731f, 526.27197f, 469.02530f, (byte) 0, 229);
    }
	private void spawnRecordsFromTheEraOfMen() {
        final int recordsFromTheEraOfMen = spawnRace == Race.ASMODIANS ? 703150 : 703132;
		spawn(recordsFromTheEraOfMen, 570.76123f, 337.31241f, 468.95096f, (byte) 0, 343);
		spawn(recordsFromTheEraOfMen, 443.34570f, 341.27530f, 469.01694f, (byte) 0, 355);
		spawn(recordsFromTheEraOfMen, 394.60165f, 443.42435f, 468.95096f, (byte) 0, 360);
		spawn(recordsFromTheEraOfMen, 480.38297f, 678.60730f, 469.01431f, (byte) 0, 394);
		spawn(recordsFromTheEraOfMen, 387.74930f, 500.32230f, 468.95096f, (byte) 0, 396);
		spawn(recordsFromTheEraOfMen, 319.92542f, 568.84387f, 468.95096f, (byte) 0, 404);
    }
	private void spawnEmpyreanHistories() {
        final int empyreanHistories = spawnRace == Race.ASMODIANS ? 703151 : 703133;
		spawn(empyreanHistories, 502.64456f, 454.79669f, 468.95096f, (byte) 0, 268);
		spawn(empyreanHistories, 413.44009f, 568.73181f, 468.95096f, (byte) 0, 371);
		spawn(empyreanHistories, 528.64270f, 599.86584f, 468.95096f, (byte) 0, 372);
		spawn(empyreanHistories, 549.72137f, 648.74438f, 468.95096f, (byte) 0, 373);
		spawn(empyreanHistories, 439.38571f, 504.14023f, 468.95096f, (byte) 0, 399);
    }
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
        doors = instance.getDoors();
		entryMessagesScheduled = false;
		finalBossRewardSpawned = false;
		spawnRace = null;
		switch (Rnd.get(1, 3)) {
			case 1:
				spawn(857452, 552.1911f, 511.7292f, 468.97675f, (byte) 0); //Relic Techgolem.
				spawn(857456, 460.161f, 672.068f, 468.97745f, (byte) 92); //Augmented Fleshgolem.
				spawn(857459, 460.66083f, 351.61194f, 468.9799f, (byte) 21); //Crystalized Shardgolem.
			break;
			case 2:
				spawn(857456, 552.1911f, 511.7292f, 468.97675f, (byte) 0); //Augmented Fleshgolem.
				spawn(857459, 460.161f, 672.068f, 468.97745f, (byte) 92); //Crystalized Shardgolem.
				spawn(857452, 460.66083f, 351.61194f, 468.9799f, (byte) 21); //Relic Techgolem.
			break;
			case 3:
				spawn(857459, 552.1911f, 511.7292f, 468.97675f, (byte) 0); //Crystalized Shardgolem.
				spawn(857452, 460.161f, 672.068f, 468.97745f, (byte) 92); //Relic Techgolem.
				spawn(857456, 460.66083f, 351.61194f, 468.9799f, (byte) 21); //Augmented Fleshgolem.
			break;
		} switch (Rnd.get(1, 3)) {
			case 1:
				spawn(857460, 255.67651f, 512.3747f, 468.84964f, (byte) 0); //Ancient Relic Techgolem.
			break;
			case 2:
				spawn(857462, 255.67651f, 512.3747f, 468.84964f, (byte) 0); //Fleshgolem Captain.
			break;
			case 3:
				spawn(857464, 255.67651f, 512.3747f, 468.84964f, (byte) 0); //Mountainous Shardgolem.
			break;
		}
		SecretCubeSpot cubeSpot = SECRET_CUBE_SPOTS.get(Rnd.get(1, SECRET_CUBE_SPOTS.size()) - 1);
		spawn(806139, cubeSpot.x(), cubeSpot.y(), cubeSpot.z(), MathUtil.convertDegreeToHeading(cubeSpot.heading()));
    }
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * @param npc NPC / npc
	 */
	@Override
	public void onDie(Npc npc) {
			switch (npc.getObjectTemplate().getTemplateId()) {
			case 701432: //IDEternity_01_Secret_Door_01.
			    despawnNpc(npc);
			break;
			case 857460: //Ancient Relic Techgolem.
			case 857462: //Fleshgolem Captain.
			case 857464: //Mountainous Shardgolem.
				if (finalBossRewardSpawned) {
					break;
				}
				finalBossRewardSpawned = true;
				if (doors != null && doors.get(33) != null) {
					doors.get(33).setOpen(true);
				}
				// 阿特雷亚古物学家被击败，永恒遗物停止运作。 / The Antiquarian of Atreia is defeated and the Eternity Relics ceased functioning.
				sendMsgByRace(1403214, Race.PC_ALL, 0);
				final int ArchivesExit = spawnRace == Race.ASMODIANS ? 806192 : 806191;
				spawn(ArchivesExit, 222.88667f, 511.78955f, 468.80215f, (byte) 0);
				final int ArchivesToCradle = spawnRace == Race.ASMODIANS ? 806057 : 806055;
				spawn(ArchivesToCradle, 256.28693f, 512.5591f, 468.84964f, (byte) 118);
				spawn(806153, 245.83438f, 512.4957f, 468.80215f, (byte) 119); //密码背包。 / Cryptograph Cube.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Archives Of Eternity>");
			break;
		}
	}
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
	/**
	 * 处理 sendMsgByRace。
	 * Handle sendMsgByRace.
	 *
	 * @param msg 消息 / message
	 * @param race 阵营 / race
	 * @param time 时间 / time
	 */
	
	protected void sendMsgByRace(final int msg, final Race race, int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				instance.doOnAllPlayers(new Visitor<Player>() {
					/**
					 * 处理 visit。
					 * Handle visit.
					 *
					 * @param player 玩家 / player
					 */
					@Override
					public void visit(Player player) {
						if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
						}
					}
				});
			}
		}, time);
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
    public void onInstanceDestroy() {
		if (doors != null) {
			doors.clear();
		}
		doors = null;
		spawnRace = null;
		entryMessagesScheduled = false;
		finalBossRewardSpawned = false;
    }
}
