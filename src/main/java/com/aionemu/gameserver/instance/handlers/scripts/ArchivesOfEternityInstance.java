package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.Map;

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
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(Player player) {
		super.onInstanceCreate(instance);
		player.getController().updateNearbyQuests();
		// 与代理人交谈。 / Talk with the Agent.
		sendMsg(1403340, 0, false, 25, 5000);
		// 须摧毁奥德封印才能进入。 / You must destroy the Aether seals to enter.
		sendMsg(1403210, 0, false, 25, 30000);
		// 古物学家已开始激活永恒遗物。 / The Antiquarian has begun activating the Eternity Relics.
		sendMsg(1403212, 0, false, 25, 60000);
		// 阿特雷亚古物学家已激活全部永恒遗物。 / The Antiquarian of Atreia has activated all Eternity Relics.
		sendMsg(1403213, 0, false, 25, 120000);
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
		} switch (Rnd.get(1, 8)) {
			case 1:
			    deleteNpc(220334); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
				spawn(806139, 345.74078f, 392.68344f, 469.52179f, (byte) 0); //密码背包。 / Cryptograph Cube.
			break;
			case 2:
			    deleteNpc(220334); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
				spawn(806139, 345.26672f, 631.75073f, 469.52179f, (byte) 0); //密码背包。 / Cryptograph Cube.
			break;
			case 3:
			    deleteNpc(220334); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
				spawn(806139, 668.62073f, 630.28986f, 469.52179f, (byte) 0); //密码背包。 / Cryptograph Cube.
			break;
			case 4:
			    deleteNpc(220334); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			    spawn(806139, 414.77441f, 352.00488f, 469.52179f, (byte) 0); //密码背包。 / Cryptograph Cube.
			break;
			case 5:
			    deleteNpc(220334); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			    spawn(806139, 599.04608f, 352.67654f, 469.52179f, (byte) 0); //密码背包。 / Cryptograph Cube.
			break;
			case 6:
			    deleteNpc(220334); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			    spawn(806139, 414.85263f, 671.28998f, 469.52179f, (byte) 0); //密码背包。 / Cryptograph Cube.
			break;
			case 7:
			    deleteNpc(220334); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			    spawn(806139, 668.36761f, 392.10706f, 469.52179f, (byte) 0); //密码背包。 / Cryptograph Cube.
			break;
			case 8:
			    deleteNpc(220334); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			    spawn(806139, 598.98456f, 672.07361f, 469.52179f, (byte) 0); //密码背包。 / Cryptograph Cube.
			break;
		} switch (Rnd.get(1, 8)) {
			case 1:
			    deleteNpc(806139); //密码背包。 / Cryptograph Cube.
				spawn(220334, 345.74078f, 392.68344f, 469.52179f, (byte) 0); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			break;
			case 2:
			    deleteNpc(806139); //密码背包。 / Cryptograph Cube.
				spawn(220334, 345.26672f, 631.75073f, 469.52179f, (byte) 0); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			break;
			case 3:
			    deleteNpc(806139); //密码背包。 / Cryptograph Cube.
				spawn(220334, 668.62073f, 630.28986f, 469.52179f, (byte) 0); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			break;
			case 4:
			    deleteNpc(806139); //密码背包。 / Cryptograph Cube.
			    spawn(220334, 414.77441f, 352.00488f, 469.52179f, (byte) 0); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			break;
			case 5:
			    deleteNpc(806139); //密码背包。 / Cryptograph Cube.
			    spawn(220334, 599.04608f, 352.67654f, 469.52179f, (byte) 0); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			break;
			case 6:
			    deleteNpc(806139); //密码背包。 / Cryptograph Cube.
			    spawn(220334, 414.85263f, 671.28998f, 469.52179f, (byte) 0); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			break;
			case 7:
			    deleteNpc(806139); //密码背包。 / Cryptograph Cube.
			    spawn(220334, 668.36761f, 392.10706f, 469.52179f, (byte) 0); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			break;
			case 8:
			    deleteNpc(806139); //密码背包。 / Cryptograph Cube.
			    spawn(220334, 598.98456f, 672.07361f, 469.52179f, (byte) 0); //神器拟态；箱中拟态。 / Artifact Mimic; Mimic-In-The-Box.
			break;
		}
    }
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
	public void onDie(Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 701432: //IDEternity_01_Secret_Door_01.
			    despawnNpc(npc);
			break;
			case 703009: //Shedim Eternity Relic.
			    despawnNpc(npc);
				// 谢迪姆封印已被摧毁。 / Shedim Seal has been destroyed.
				sendMsg(1403269, 0, false, 25, 0);
			break;
			case 703010: //Seraphim Eternity Relic.
			    despawnNpc(npc);
				// 炽天使封印已被摧毁。 / Seraphim Seal has been destroyed.
				sendMsg(1403270, 0, false, 25, 0);
				deleteNpc(703017);
			break;
			case 703011: //Shedim Eternity Relic.
			    despawnNpc(npc);
				// 谢迪姆封印已被摧毁。 / Shedim Seal has been destroyed.
				sendMsg(1403269, 0, false, 25, 0);
			break;
			case 703012: //Seraphim Eternity Relic.
			    despawnNpc(npc);
				// 炽天使封印已被摧毁。 / Seraphim Seal has been destroyed.
				sendMsg(1403270, 0, false, 25, 0);
				deleteNpc(703018);
			break;
			case 703013: //Shedim Eternity Relic.
			    despawnNpc(npc);
				// 谢迪姆封印已被摧毁。 / Shedim Seal has been destroyed.
				sendMsg(1403269, 0, false, 25, 0);
			break;
			case 703014: //Seraphim Eternity Relic.
				despawnNpc(npc);
				// 炽天使封印已被摧毁。 / Seraphim Seal has been destroyed.
				sendMsg(1403270, 0, false, 25, 0);
				deleteNpc(703019);
			break;
			case 703015: //Shedim Eternity Relic.
			    despawnNpc(npc);
				// 谢迪姆封印已被摧毁。 / Shedim Seal has been destroyed.
				sendMsg(1403269, 0, false, 25, 0);
			break;
			case 703016: //Seraphim Eternity Relic.
			    despawnNpc(npc);
				// 炽天使封印已被摧毁。 / Seraphim Seal has been destroyed.
				sendMsg(1403270, 0, false, 25, 0);
				deleteNpc(703020);
			break;
			case 857460: //Ancient Relic Techgolem.
			case 857462: //Fleshgolem Captain.
			case 857464: //Mountainous Shardgolem.
			    doors.get(33).setOpen(true);
				// 阿特雷亚古物学家被击败，永恒遗物停止运作。 / The Antiquarian of Atreia is defeated and the Eternity Relics ceased functioning.
				sendMsg(1403214, 0, false, 25, 0);
				final int ArchivesExit = spawnRace == Race.ASMODIANS ? 806192 : 806191;
				spawn(ArchivesExit, 222.88667f, 511.78955f, 468.80215f, (byte) 0);
				final int ArchivesToCradle = spawnRace == Race.ASMODIANS ? 806057 : 806055;
				spawn(ArchivesToCradle, 256.28693f, 512.5591f, 468.84964f, (byte) 118);
				spawn(806153, 245.83438f, 512.4957f, 468.80215f, (byte) 119); //密码背包。 / Cryptograph Cube.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Archives Of Eternity>");
			break;
		}
	}
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
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
        doors.clear();
    }
}
