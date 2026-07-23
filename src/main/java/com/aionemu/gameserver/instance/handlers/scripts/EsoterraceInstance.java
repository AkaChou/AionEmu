package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 秘境露台副本事件处理器。
 * Instance event handler for Esoterrace.
 *
 * @author Encom
 */

@InstanceID(300250000)
public class EsoterraceInstance extends GeneralInstanceHandler
{
	/** lab manager killed / lab manager killed */
		private int labManagerKilled;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onEnterInstance(Player player) {
		super.onInstanceCreate(instance);
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
			case 282295: //Command Gate Control.
				doors.get(39).setOpen(true);
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
			/**
	 * 从主入口可进入伊索特拉斯秘密实验室较大半区；实验室中央有…… / From the main entrance, players have access to the larger half of the Esoterrace Secret Laboratory. In the middle of the Laboratory is a Surkana Feeder. Destroy this to face Warden Surama, the Hard Mode? final Named Monster. Leave it alone to face Kexkra, the normal final Named Monster
	 */
			case 282291: //Surkana Feeder.
				despawnNpc(npc);
				deleteNpc(217204); //Kexkra.
				deleteNpc(283173); //Drana FX.
				// 苏卡纳供应器已过载。 / The Surkana Supplier has overloaded.
				sendMsg(1400996, 0, false, 25, 0);
				// 苏卡纳供应器已损坏。 / The Surkana Supplier has been broken.
				sendMsg(1401037, 0, false, 25, 4000);
				spawn(217205, 1315.99f, 1170.77f, 51.8004f, (byte) 87); //Kexkra Prototype.
			break;
			/**
	 * “达莉亚·查兰兹”是伊索特拉斯首个命名怪；开战前请清理周边巡逻。 / "Dalia Charlands" is the first Named Monster of Esoterrace. Before engaging the boss, be sure to clear out the surrounding area of patrolling monsters. Dalia is a straightforward encounter that doesn't have a lot of gimmicks, just be sure to watch out for the occasional area of effect attack. When Dalia is defeated, Dalia's Watcher will appear, follow it to find the path to the next area. Be sure each player in the Group loots the Dalia Key from "Dalia Charlands" Like a Quest item, each player can loot this key, and earn access to one of the "Entwined Treasure Chest" nearby which contain Abyss relics! In addition to the "Entwined Treasure Chests" & "Huge Entwined Chest" will spawn beneath Dalia once it is defeated, which can be opened with the Swirl Key. Should the Group befall certain death after defeating Dalia, the Windstream found at the beginning of the Instanced Dungeon will now transport players directly to the Dalia Garden
	 */
			case 217185: //Dalia Charlands.
			    // 达莉亚·查兰兹已消失。 / Dalia Charlands has vanished.
				sendMsg(1401036, 0, false, 25, 0);
				// 苏卡纳蒸汽喷射产生了上升气流。 / The Surkana Steam Jet has generated an updraft.
				sendMsg(1400997, 0, false, 25, 5000);
				// 击败所有德拉纳生产实验室区段管理员以打开实验室院子门。 / Defeat all Drana Production Lab Section Managers to open the Laboratory Yard door.
				sendMsg(1400919, 0, false, 25, 120000);
				spawn(703052, 392.27563f, 543.89026f, 318.3265f, (byte) 18); //Windstream A
				spawn(703054, 392.27563f, 543.89026f, 318.3265f, (byte) 18); //Windstream B
				spawn(701023, 1264.862061f, 644.995178f, 296.831818f, (byte) 0, 112); //Large Entwined Chest.
            break;
			case 217282: //Esoterrace Investigator.
			case 217283: //Senior Lab Researcher.
			case 217284: //Lab Supervisor.
				labManagerKilled++;
				if (labManagerKilled == 1) {
					doors.get(367).setOpen(false);
				} else if (labManagerKilled == 2) {
					doors.get(69).setOpen(false);
				} else if (labManagerKilled == 3) {
					doors.get(111).setOpen(true);
					// 通往实验室院子的门现已打开。 / The door to the Laboratory Yard is now open.
					sendMsg(1400920, 0, false, 25, 0);
					// 德拉纳生产实验室通道现已开放。 / The Drana Production Lab walkway is now open.
					sendMsg(1400923, 0, false, 25, 6000);
				}
			break;
			case 217281: //Lab Gatekeeper.
				doors.get(70).setOpen(true);
				// 通往实验室空调室的门现已打开。 / The door to the Laboratory Air Conditioning Room is now open.
				sendMsg(1400921, 0, false, 25, 0);
            break;
			case 286930: //Esoterrace Mage.
                despawnNpc(npc);
				spawn(799580, 1034.11f, 985.01f, 327.35095f, (byte) 105); //Keening Sirokin.
				spawn(701025, 1038.636963f, 987.741455f, 328.356415f, (byte) 0, 725); //Sundries Box.
            break;
		   /**
	 * 实验室空调室内将遭遇第二个命名怪“穆鲁甘队长”。 / Inside the Laboratory Air Conditioning Room, players will encounter the second Named Monster of Esoterrace, "Captain Murugan" Be wary of "Captain Murugan's" deadly combo skills, expect the primary target to take massive damage throughout the encounter! When Captain Murugan is defeated, two doors will open in the Laboratory Air Conditioning Room, granting access to Chilled Treasure chests which contain Abyss relics
	 */
			case 217195: //Captain Murugan.
				switch (Rnd.get(1, 2)) {
				    case 1:
						doors.get(45).setOpen(true);
						// 守门人倒下，左侧门已打开！ / With the gatekeeper down, the door on the left is open!
						sendMsg(1401229, 0, false, 25, 0);
					break;
			        case 2:
						doors.get(67).setOpen(true);
						// 守门人倒下，右侧门已打开！ / With the gatekeeper down, the door on the right is open!
						sendMsg(1401230, 0, false, 25, 0);
					break;
				}
				doors.get(52).setOpen(true);
				doors.get(70).setOpen(true);
				// 苏卡纳蒸汽喷射产生了上升气流。 / The Surkana Steam Jet has generated an updraft.
				sendMsg(1400997, 0, false, 25, 6000);
				spawn(703056, 392.27563f, 543.89026f, 318.3265f, (byte) 18); //Windstream C
				spawn(703058, 392.27563f, 543.89026f, 318.3265f, (byte) 18); //Windstream D
				spawn(701024, 751.67f, 1136.08f, 365.031f, (byte) 105, 41); //Chilled Treasure.
				spawn(701024, 827.596f, 1136.16f, 365.031f, (byte) 73, 77); //Chilled Treasure.
            break;
			case 282293: //Esoterrace Ventilator.
			    despawnNpc(npc);
				// 实验室通风口现已打开。 / The Laboratory Ventilator is now open.
				sendMsg(1400922, 0, false, 25, 0);
			break;
			case 217289: //Esoterrace Biolab Watchman.
				doors.get(122).setOpen(true);
				// 生物实验室外墙已坍塌。 / The outer wall of the Bio Lab has collapsed.
				sendMsg(1400924, 0, false, 25, 0);
            break;
		   /**
	 * 击败“凯克斯克拉”后刷新宝箱，含欧比斯遗物与白金勋章等。 / When "Kexkra" is defeated, a treasure chest will spawn containing Abyss relics and Platinum Medals. In addition, the treasure chest has a chance to contain Fabled armor from the Surama set
	 */
			case 217204: //Kexkra.
			    despawnNpc(npc);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Esoterrace>");
				spawn(701044, 1341.19f, 1181.25f, 51.515f, (byte) 67); //Esoterrace Dimensional Rift Exit.
				spawn(701027, 1326.7705f, 1173.1145f, 51.493996f, (byte) 70, 726); //Laboratory Treasure Chest.
				spawn(701027, 1321.9897f, 1179.5394f, 51.493996f, (byte) 79, 727); //Laboratory Treasure Chest.
            break;
			case 217205: //Kexkra Prototype.
			    despawnNpc(npc);
				sendMovie(player, 472);
				spawn(217206, 1315.99f, 1170.77f, 51.8004f, (byte) 87); //Warden Surama.
				spawn(701047, 1316.5045f, 1171.0127f, 52.589924f, (byte) 0, 180); //Flame Wall.
            break;
		   /**
	 * 开战面对“凯克斯克拉原型”；随后典狱长苏拉玛会加入战斗。 / Players will start this encounter facing the "Kexkra Prototype" As the encounter wears on, an event will cause Warden Surama to join the battle. When Warden Surama is defeated, two treasure chests will spawn, one of which has a chance to contain Fabled armor from the Surama series, and the other Fabled weapons from the Surama series
	 */
            case 217206: //Warden Surama.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Esoterrace>");
				spawn(701044, 1341.19f, 1181.25f, 51.515f, (byte) 67); //Esoterrace Dimensional Rift Exit.
				spawn(701027, 1326.7705f, 1173.1145f, 51.493996f, (byte) 70, 726); //Laboratory Treasure Chest.
				spawn(701027, 1321.9897f, 1179.5394f, 51.493996f, (byte) 79, 727); //Laboratory Treasure Chest.
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
	
	private void sendMovie(Player player, int movie) {
		if (!movies.contains(movie)) {
			movies.add(movie);
			PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
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
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(185000111, storage.getItemCountByItemId(185000111)); //Dalia Key.
	}
}
