package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@InstanceID(301380000)
public class DanuarSanctuaryInstance extends GeneralInstanceHandler
{
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
	/** 对象 / objects */
		private Map<Integer, VisibleObject> objects = new LinkedHashMap<Integer, VisibleObject>();
	
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
		spawnDanuarSanctuaryBoss();
    }
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onEnterInstance(Player player) {
		super.onInstanceCreate(instance);
		// 贝里特拉特别研究队指挥官正接近毁灭之室。 / The Beritran Special Research Team commanders are nearing The Chamber of Ruin.
		sendMsg(1401855, 0, false, 25, 300000);
		// 贝里特拉特别研究队指挥官发现了毁灭之室。 / The Beritran Special Research Team commanders have discovered The Chamber of Ruin.
		sendMsg(1401856, 0, false, 25, 600000);
		// 贝里特拉特别研究队指挥官已进入毁灭之室。 / The Beritran Special Research Team commanders have entered The Chamber of Ruin.
		sendMsg(1401857, 0, false, 25, 900000);
		// 贝里特拉特别研究队指挥官正在收集达努阿尔遗物。 / The Beritran Special Research Team commanders are collecting Danuar relics.
		sendMsg(1401858, 0, false, 25, 1200000);
		// 贝里特拉特别研究队指挥官已带着宝物离开。 / The Beritran Special Research Team commanders have departed with their treasures.
		sendMsg(1401859, 0, false, 25, 1500000);
		// 奇尔盗墓者几乎挖完了。 / The Chir Grave Robbers are almost finished digging.
		sendMsg(1401860, 0, false, 25, 1800000);
		// 奇尔盗墓者已离开。 / The Chir Grave Robbers have left.
		sendMsg(1401861, 0, false, 25, 2100000);
		switch (player.getRace()) {
		    case ELYOS:
			    sendMovie(player, 910);
			break;
			case ASMODIANS:
			    sendMovie(player, 911);
			break;
		} if (spawnRace == null) {
			spawnRace = player.getRace();
			SpawnDanuarRace();
		}
    }
	
	private void SpawnDanuarRace() {
		final int danuarGuard1 = spawnRace == Race.ASMODIANS ? 233126 : 233129;
        final int danuarGuard2 = spawnRace == Race.ASMODIANS ? 233127 : 233130;
		final int danuarGuard3 = spawnRace == Race.ASMODIANS ? 233128 : 233131;
		spawn(danuarGuard1, 911.333f, 904.6127f, 284.5891f, (byte) 110);
        spawn(danuarGuard1, 917.35785f, 901.0081f, 284.5891f, (byte) 50);
        spawn(danuarGuard1, 1025.9675f, 474.7492f, 290.26837f, (byte) 0);
        spawn(danuarGuard1, 1033.9897f, 474.7517f, 290.26837f, (byte) 61);
		spawn(danuarGuard2, 1029.233f, 484.0199f, 290.52118f, (byte) 31);
        spawn(danuarGuard2, 978.1413f, 1337.8359f, 335.875f, (byte) 34);
        spawn(danuarGuard2, 1019.45715f, 1367.1343f, 337.25f, (byte) 52);
        spawn(danuarGuard2, 881.45166f, 892.719f, 284.55508f, (byte) 109);
        spawn(danuarGuard2, 885.13104f, 898.88446f, 284.50986f, (byte) 109);
		spawn(danuarGuard3, 1103.6545f, 439.36285f, 284.61642f, (byte) 66);
        spawn(danuarGuard3, 833.283f, 961.50146f, 304.86777f, (byte) 79);
        spawn(danuarGuard3, 824.21826f, 967.07446f, 304.86777f, (byte) 79);
        spawn(danuarGuard3, 932.1827f, 876.7008f, 305.45746f, (byte) 92);
        spawn(danuarGuard3, 949.92975f, 903.508f, 299.75253f, (byte) 93);
    }
	@Override
	public void onDropRegistered(Npc npc) {
		int npcId = npc.getNpcId();
		if (npcId == 233391) {
			sendMsg(1401946, 0, false, 25, 0);
			return;
		}
		if (npcId != 235600) {
			return;
		}
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000254, 1)); //Seal Breaking Magic Cannonball.
	}
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * 玩家 / player
	 * npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 701859: //Metallic Mystic KeyStone.
				if (player.getInventory().isFull()) {
					sendMsg(1390149, 0, false, 25, 0);
				}
				despawnNpc(npc);
				ItemService.addItem(player, 188052613, 1); //Sanctuary Treasure Crate.
			break;
			case 701860: //Golden Mystic KeyStone.
				if (player.getInventory().isFull()) {
					sendMsg(1390149, 0, false, 25, 0);
				}
				despawnNpc(npc);
				ItemService.addItem(player, 188052613, 1); //Sanctuary Treasure Crate.
			break;
			case 701863: //Spherical Mystic KeyStone.
				// 某处有一扇门已打开。 / A door has opened somewhere.
				sendMsg(1401838, 0, false, 25, 0);
			break;
			case 701864: //Pyramidal Mystic KeyStone.
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsg(1401839, 0, false, 25, 0);
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
		    case 233084: //Ancien Danuar Coffin.
				despawnNpc(npc);
				switch (Rnd.get(1, 2)) {
					case 1:
					    spawn(233085, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading()); //Ancient Danuar Remains.
					break;
					case 2:
					break;
				}
			break;
		   /**
	 * 攻击岩石以激活上升气流 / Attack the rocks to activate the updraft
	 */
			case 233188: //Sturdy Boulder.
				despawnNpc(npc);
				spawnInfernalBoulder();
			break;
			case 235624: //Warmage Suyaroka.
			case 235625: //Chief Medic Tagnu.
			case 235626: //Virulent Ukahim.
/* 				spawnAbbeyNobleBox(); */
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Danuar Sanctuary>");
				spawn(701876, 1057.1633f, 557.6902f, 284.73123f, (byte) 30); //Danuar Sanctuary Exit.
			break;
        }
    }
	
	private void spawnAbbeyNobleBox() {
	    switch (Rnd.get(1, 2)) {
		    case 1:
				spawn(702658, 1053.4221f, 565.259f, 282.28778f, (byte) 19); //修道院箱子。 / Abbey Box.
			break;
			case 2:
				spawn(702659, 1060.8652f, 565.46436f, 282.2873f, (byte) 41); //高级修道院箱子。 / Noble Abbey Box.
			break;
		}
	}
	
	private void spawnDanuarSanctuaryBoss() {
	    switch (Rnd.get(1, 3)) {
		    case 1:
				spawn(235624, 1056.5698f, 693.86584f, 282.0391f, (byte) 30); //Warmage Suyaroka.
			break;
			case 2:
				spawn(235625, 1045.4534f, 682.2679f, 282.0391f, (byte) 60); //Chief Medic Tagnu.
			break;
			case 3:
				spawn(235626, 1056.4889f, 670.9826f, 282.0391f, (byte) 91); //Virulent Ukahim.
			break;
		}
	}
	
	private void spawnInfernalBoulder() {
		SpawnTemplate sturdyInfernalBoulder = SpawnEngine.addNewSingleTimeSpawn(301380000, 233187, 906.1991f, 859.88177f, 278.64731f, (byte) 37);
		sturdyInfernalBoulder.setEntityId(1699);
		objects.put(233187, SpawnEngine.spawnObject(sturdyInfernalBoulder, instanceId));
	}
	/**
	 * 移除相关物品。
	 * Remove related items.
	 *
	 * @param player 玩家 / player
	 */
	
	public void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(185000181, storage.getItemCountByItemId(185000181)); //The Catacombs Key.
		storage.decreaseByItemId(185000182, storage.getItemCountByItemId(185000182)); //The Crypts Key.
        storage.decreaseByItemId(185000183, storage.getItemCountByItemId(185000183)); //The Charnels Key.
    }
	
	private void sendMovie(Player player, int movie) {
        if (!movies.contains(movie)) {
             movies.add(movie);
             PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
        }
    }
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
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
		movies.clear();
    }
}
