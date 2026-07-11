package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

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
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 被夺取的达努亚尔圣所副本事件处理器。
 * Instance event handler for Seized Danuar Sanctuary.
 *
 * @author Encom
 */

@InstanceID(301140000)
public class SeizedDanuarSanctuaryInstance extends GeneralInstanceHandler
{
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
	/** seized danuar sanctuary boss / seized danuar sanctuary boss */
		private int seizedDanuarSanctuaryBoss;
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
		sendMsgByRace(1401855, Race.PC_ALL, 300000);
		// 贝里特拉特别研究队指挥官发现了毁灭之室。 / The Beritran Special Research Team commanders have discovered The Chamber of Ruin.
		sendMsgByRace(1401856, Race.PC_ALL, 600000);
		// 贝里特拉特别研究队指挥官已进入毁灭之室。 / The Beritran Special Research Team commanders have entered The Chamber of Ruin.
		sendMsgByRace(1401857, Race.PC_ALL, 900000);
		// 贝里特拉特别研究队指挥官正在收集达努阿尔遗物。 / The Beritran Special Research Team commanders are collecting Danuar relics.
		sendMsgByRace(1401858, Race.PC_ALL, 1200000);
		// 贝里特拉特别研究队指挥官已带着宝物离开。 / The Beritran Special Research Team commanders have departed with their treasures.
		sendMsgByRace(1401859, Race.PC_ALL, 1500000);
		// 奇尔盗墓者几乎挖完了。 / The Chir Grave Robbers are almost finished digging.
		sendMsgByRace(1401860, Race.PC_ALL, 1800000);
		// 奇尔盗墓者已离开。 / The Chir Grave Robbers have left.
		sendMsgByRace(1401861, Race.PC_ALL, 2100000);
		switch (player.getRace()) {
		    case ELYOS:
			    sendMovie(player, 910);
			break;
			case ASMODIANS:
			    sendMovie(player, 911);
			break;
		} if (spawnRace == null) {
			spawnRace = player.getRace();
			SpawnSeizedRace();
		}
    }
	
	private void SpawnSeizedRace() {
		final int seizedGuard1 = spawnRace == Race.ASMODIANS ? 233126 : 233129;
        final int seizedGuard2 = spawnRace == Race.ASMODIANS ? 233127 : 233130;
		final int seizedGuard3 = spawnRace == Race.ASMODIANS ? 233128 : 233131;
		spawn(seizedGuard1, 911.333f, 904.6127f, 284.5891f, (byte) 110);
        spawn(seizedGuard1, 917.35785f, 901.0081f, 284.5891f, (byte) 50);
        spawn(seizedGuard1, 1025.9675f, 474.7492f, 290.26837f, (byte) 0);
        spawn(seizedGuard1, 1033.9897f, 474.7517f, 290.26837f, (byte) 61);
		spawn(seizedGuard2, 1029.233f, 484.0199f, 290.52118f, (byte) 31);
        spawn(seizedGuard2, 978.1413f, 1337.8359f, 335.875f, (byte) 34);
        spawn(seizedGuard2, 1019.45715f, 1367.1343f, 337.25f, (byte) 52);
        spawn(seizedGuard2, 881.45166f, 892.719f, 284.55508f, (byte) 109);
        spawn(seizedGuard2, 885.13104f, 898.88446f, 284.50986f, (byte) 109);
		spawn(seizedGuard3, 1103.6545f, 439.36285f, 284.61642f, (byte) 66);
        spawn(seizedGuard3, 833.283f, 961.50146f, 304.86777f, (byte) 79);
        spawn(seizedGuard3, 824.21826f, 967.07446f, 304.86777f, (byte) 79);
        spawn(seizedGuard3, 932.1827f, 876.7008f, 305.45746f, (byte) 92);
        spawn(seizedGuard3, 949.92975f, 903.508f, 299.75253f, (byte) 93);
    }
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
			case 235574: //Shulack Mercenary Cannon Chief.
			    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000254, 1)); //Seal Breaking Magic Cannonball.
			break;
			case 235655: //Bodyguard Yatakin.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000174, 1)); //Sentry Post Of Eternity Key.
			break;
			case 235619: //Warmage Suyaroka.
			case 235620: //Chief Medic Tagnu.
			case 235621: //Virulent Ukahim.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053495, 1)); //Remodeled Ancient Danuar Weapon Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					}
				}
			break;
			case 233391: //Sanctuary Keybox.
				// 请谨慎选择。钥匙一经选定无法更改。 / Be careful in your selection. The key cannot be changed once it is chosen.
				sendMsgByRace(1401946, Race.PC_ALL, 0);
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 185000181, 1)); //The Catacombs Key.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 185000182, 1)); //The Crypts Key.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 185000183, 1)); //The Charnels Key.
					}
				}
			break;
			case 233185: //Danuar Sanctuary Jar.
			case 233190: //Stone Treasure Box I.
			case 233191: //Stone Treasure Box II.
			case 233192: //Stone Treasure Box III.
				switch (Rnd.get(1, 5)) {
				    case 1:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 169405254, 2)); //Earth Trace.
				    break;
					case 2:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012592, 2)); //Earth Scrap.
				    break;
					case 3:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012613, 2)); //Burning Vitality.
				    break;
					case 4:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 169405267, 2)); //Flame Vitality.
				    break;
					case 5:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 169405268, 2)); //Lightning Vitality.
				    break;
				} switch (Rnd.get(1, 12)) {
				    case 1:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012580, 2)); //Fire Mote.
				    break;
					case 2:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012581, 2)); //Fire Breath.
				    break;
					case 3:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012582, 2)); //Fire Fragment.
					break;
					case 4:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012583, 2)); //Fire Source.
				    break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012584, 2)); //Water Source.
				    break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012585, 2)); //Wind Mote.
				    break;
					case 7:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012586, 2)); //Wind Breath.
				    break;
					case 8:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012587, 2)); //Wind Eternity.
				    break;
					case 9:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012589, 2)); //Wind Source.
					break;
					case 10:
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012588, 2)); //Wind Fragment.
					break;
					case 11:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012590, 2)); //Wind Origin.
					break;
					case 12:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152012591, 2)); //Water Fragment.
				    break;
				}
			break;
		}
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
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
				despawnNpc(npc);
				ItemService.addItem(player, 188052613, 1); //Sanctuary Treasure Crate.
			break;
			case 701860: //Golden Mystic KeyStone.
				if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
				despawnNpc(npc);
				ItemService.addItem(player, 188052613, 1); //Sanctuary Treasure Crate.
			break;
			case 701863: //Spherical Mystic KeyStone.
				// 某处有一扇门已打开。 / A door has opened somewhere.
				sendMsgByRace(1401838, Race.PC_ALL, 0);
			break;
			case 701864: //Pyramidal Mystic KeyStone.
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 0);
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
				spawnSturdyBoulder();
			break;
			case 235619: //Warmage Suyaroka.
			case 235620: //Chief Medic Tagnu.
			case 235621: //Virulent Ukahim.
				seizedDanuarSanctuaryBoss ++;
				if (seizedDanuarSanctuaryBoss == 3) {
					spawnAbbeyNobleBox();
					// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Seized Danuar Sanctuary>");
					spawn(701876, 1057.1633f, 557.6902f, 284.73123f, (byte) 30); //Seized Danuar Sanctuary Exit.
				}
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
	
	private void spawnSturdyBoulder() {
		SpawnTemplate sturdyBoulder = SpawnEngine.addNewSingleTimeSpawn(301140000, 233187, 906.1991f, 859.88177f, 278.64731f, (byte) 37);
		sturdyBoulder.setEntityId(1699);
		objects.put(233187, SpawnEngine.spawnObject(sturdyBoulder, instanceId));
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
	 * message
	 * 阵营 / race
	 * time
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