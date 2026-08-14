package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameStaticDataServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.*;
import java.util.concurrent.Future;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.commons.network.util.ThreadPoolManager;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.summons.*;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 塔洛克空洞副本事件处理器。
 * Instance event handler for Talocs Hollow.
 *
 * @author Encom
 */

@InstanceID(300190000)
public class TalocsHollowInstance extends GeneralInstanceHandler
{
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
		/** taloc 任务 / taloc task */
		private final List<Future<?>> talocTask = new ArrayList<Future<?>>();
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
		doors.get(49).setOpen(true);
		spawnHugeInsectEgg();
    }
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onEnterInstance(Player player) {
		switch (player.getRace()) {
			case ELYOS:
				if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
				addTalocFruitE(player);
				addTalocTearsE(player);
				sendMovie(player, 434);
			break;
			case ASMODIANS:
				if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
				addTalocFruitA(player);
				addTalocTearsA(player);
			    sendMovie(player, 438);
		    break;
		}
		// 你必须消灭塔洛克的敌人，才能获得强大物品。 / You must destroy the enemies of Taloc. It allows you to acquire objects with great power.
		sendMsgByRace(1400704, Race.PC_ALL, 5000);
		// 背包中有强大物品。使用塔洛克果实可变为强力形态。 / An object of great power waits in your cube. Transform into a mighty being with Taloc's Fruit.
		sendMsgByRace(1400752, Race.PC_ALL, 10000);
		// 背包中有强大物品。使用塔洛克之泪可发动强力空中攻击。 / An object of great power waits in your cube. Launch a powerful aerial attack with Taloc's Tears.
		sendMsgByRace(1400753, Race.PC_ALL, 15000);
		HTMLService.showHTML(player, GameStaticDataServices.htmlCache().getHTML("instances/talocHollow.xhtml"));
    }
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * @param npc NPC / npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		switch (npcId) {
			case 215456: //Shishir.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000088, 1)); //Shishir's Corrosive Fluid.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000137, 1)); //Shishir's Powerstone.
		    break;
			case 215478: //Neith.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000108, 1)); //Dorkin's Pocket Knife.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000139, 1)); //Neith's Sleepstone.
		    break;
			case 215482: //Gellmar.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000138, 1)); //Gellmar's Wardstone.
		    break;
			case 215488: //Celestius.
			    switch (Rnd.get(1, 5)) {
					case 1:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080005, 2)); //低级随从契约。 / Lesser Minion Contract.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080006, 2)); //高级随从契约。 / Greater Minion Contract.
					break;
					case 3:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080007, 2)); //大型随从契约。 / Major Minion Contract.
					break;
					case 4:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080008, 2)); //可爱随从契约。 / Cute Minion Contract.
					break;
					case 5:
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190200000, 50)); //Minium.
					break;
				}
			break;
		}
	}
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * @param player 玩家 / player
	 * @param npc NPC / npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 700940: //Healing Plant.
				despawnNpc(npc);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 20000);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.MP, 20000);
			break;
			case 700941: //Huge Healing Plant.
				despawnNpc(npc);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 30000);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.MP, 30000);
			break;
		}
	}
	
    /**
     * 处理死亡事件。
     * Handle a death event.
     *
     * @param npc NPC / npc
     */
    @Override
    public void onDie(Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 215456: //Shishir.
				// 希希尔尸体中有强大物品。获取后登记到技能窗口。 / An object of great power waits in Shishir's carcass. Obtain it, then register it in the skill window.
		        sendMsgByRace(1400754, Race.PC_ALL, 0);
            break;
			case 215457: //Ancient Octanus.
				// 你感觉塔洛克之根有动静。再不快点就见不到他了。 / You sense a movement in Taloc's Roots. You won't be able to meet him unless you hurry.
				sendMsgByRace(1400659, Race.PC_ALL, 0);
            break;
			case 215478: //Neith.
				// 奈斯尸体中有强大物品。获取后登记到技能窗口。 / An object of great power waits in Neith's carcass. Obtain it, then register it in the skill window.
		        sendMsgByRace(1400756, Race.PC_ALL, 0);
            break;
			case 215480: //Queen Mosqua.
                deleteNpc(700738); //Huge Insect Egg.
				sendMovie(player, 435);
				// 解除召唤：“恩盖乌斯与阿比拉” / Release Summon: "Engeius & Abyla"
				if (player.getSummon() != null) {
					SummonsService.release(player.getSummon(), UnsummonType.UNSPECIFIED, false);
				}
				sp(700739, 653.63f, 838.66998f, 1304.72f, (byte) 0, 11, 0, 0, null); //Cracked Huge Insect Egg.
            break;
			case 215482: //Gellmar.
				// 盖尔玛尸体中有强大物品。获取后登记到技能窗口。 / An object of great power waits in Gellmar's carcass. Obtain it, then register it in the skill window.
		        sendMsgByRace(1400755, Race.PC_ALL, 0);
            break;
            case 215488: //Celestius.
                deleteNpc(700740); //Contaminated Fragment Of Aion Tower.
				sendMovie(player, 437);
				ItemService.addItem(player, 188900011, 1); //Blessing Box Of Growth V.
				ItemService.addItem(player, 170170044, 1); //[Souvenir] Taloc's Komad Statue.
				sendMsg("[Congratulation]: you finish <Taloc's Hollow>");
                spawn(799503, 539.94135f, 813.3849f, 1377.4283f, (byte) 27); //Taloc's Mirage.
				sp(700741, 636.35999f, 769.53003f, 1387.38f, (byte) 0, 92, 0, 0, null); //Purified Fragment Of Aion Tower.
            break;
			case 700739: //Cracked Huge Insect Egg.
				despawnNpc(npc);
				// 卵所在处升起上升气流。 / An ascending air current is rising from the spot where the egg was.
				// 展开双翼乘气流可垂直飞升。 / You can fly vertically up by spreading your wings and riding the current.
				sendMsgByRace(1400477, Race.PC_ALL, 5000);
				sp(281817, 653.77478f, 838.88306f, 1303.8502f, (byte) 0, 1308, 0, 0, null); //Geyser.
            break;
			case 700942: //Bug Fluid.
			    despawnNpc(npc);
			break;
        }
    }
	
	private void spawnHugeInsectEgg() {
	    SpawnTemplate IDElim2FEntity = SpawnEngine.addNewSingleTimeSpawn(300190000, 700738, 653.63f, 838.66998f, 1304.72f, (byte) 0);
		IDElim2FEntity.setEntityId(90);
		objects.put(700738, SpawnEngine.spawnObject(IDElim2FEntity, instanceId));
	}
	
	/**
	 * 玩家进入区域时处理。
	 * Handle a player entering a zone.
	 *
	 * @param player 玩家 / player
	 * @param zone 区域 / zone
	 */
	@Override
    public void onEnterZone(Player player, ZoneInstance zone) {
        if (zone.getAreaTemplate().getZoneName() == ZoneName.get("KINQUIDS_DEN_300190000")) {
            sendMovie(player, 463);
			// 烟雾正在释放。接触烟雾将破坏金奎德的屏障。 / Smoke is being discharged. Exposure to smoke will destroy Kinquid's Barrier.
			sendMsgByRace(1400660, Race.PC_ALL, 0);
	    } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("MOSQUAS_NEST_300190000")) {
			sendMovie(player, 464);
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("COCCOONING_CHAMBER_300190000")) {
			// 茧在蠕动——里面有东西！ / The cocoons are wriggling--something's inside!
			sendMsgByRace(1400475, Race.PC_ALL, 2000);
			// 你可救出茧中两名雷安之一。 / You can save one of the two Reians imprisoned in the cocoon.
			sendMsgByRace(1400630, Race.PC_ALL, 8000);
		}
    }
	
    private void sendMovie(Player player, int movie) {
        if (!movies.contains(movie)) {
             movies.add(movie);
             PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
        }
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
		removeEffects(player);
		if (player.getSummon() != null) {
			SummonsService.release(player.getSummon(), UnsummonType.UNSPECIFIED, false);
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
		removeEffects(player);
		if (player.getSummon() != null) {
			SummonsService.release(player.getSummon(), UnsummonType.UNSPECIFIED, false);
		}
	}
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(182215618, storage.getItemCountByItemId(182215618)); //Taloc Fruit.
		storage.decreaseByItemId(182215593, storage.getItemCountByItemId(182215593)); //Taloc Fruit.
		storage.decreaseByItemId(182215619, storage.getItemCountByItemId(182215619)); //Taloc's Tears.
		storage.decreaseByItemId(182215592, storage.getItemCountByItemId(182215592)); //Taloc's Tears.
		storage.decreaseByItemId(164000137, storage.getItemCountByItemId(164000137)); //Shishir's Powerstone.
		storage.decreaseByItemId(164000138, storage.getItemCountByItemId(164000138)); //Gellmar's Wardstone.
		storage.decreaseByItemId(164000139, storage.getItemCountByItemId(164000139)); //Neith's Sleepstone.
	}
	
	private void addTalocFruitE(Player player) {
	    ItemService.addItem(player, 182215618, 1); //Taloc Fruit.
    }
	private void addTalocTearsE(Player player) {
        ItemService.addItem(player, 182215619, 1); //Taloc's Tears.
    }
	private void addTalocFruitA(Player player) {
		ItemService.addItem(player, 182215593, 1); //Taloc Fruit.
    }
	private void addTalocTearsA(Player player) {
        ItemService.addItem(player, 182215592, 1); //Taloc's Tears.
    }
	
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(10251); //Taloc Fruit.
		effectController.removeEffect(10252); //Taloc Fruit.
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	private void stopInstanceTask() {
        for (Future<?> task : talocTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * @param npcId NPC / NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * @param time 时间 / time
	 */
	
	protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
    /**
     * 处理 sp。
     * Handle sp.
     * 
     * @param npcId NPC / NPC
     * @param x X 坐标 / X
     * @param y Y 坐标 / Y
     * @param z Z 坐标 / Z
     * @param h 朝向 / h
     * @param time 时间 / time
     * @param msg 消息 / message
     * @param race 阵营 / race
     */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
    /**
     * 处理 sp。
     * Handle sp.
     * 
     * @param npcId NPC / NPC
     * @param x X 坐标 / X
     * @param y Y 坐标 / Y
     * @param z Z 坐标 / Z
     * @param h 朝向 / h
     * @param entityId 实体 ID / entity id
     * @param time 时间 / time
     * @param msg 消息 / message
     * @param race 阵营 / race
     */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        talocTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    spawn(npcId, x, y, z, h, entityId);
                    if (msg > 0) {
                        sendMsgByRace(msg, race, 0);
                    }
                }
            }
        }, time));
    }
    /**
     * 处理 sp。
     * Handle sp.
     * 
     * @param npcId NPC / NPC
     * @param x X 坐标 / X
     * @param y Y 坐标 / Y
     * @param z Z 坐标 / Z
     * @param h 朝向 / h
     * @param time 时间 / time
     * @param walkerId 寻路器 ID / walkerId
     */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        talocTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    Npc npc = (Npc) spawn(npcId, x, y, z, h);
                    npc.getSpawn().setWalkerId(walkerId);
                    WalkManager.startWalking((NpcAI2) npc.getAi2());
                }
            }
        }, time));
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
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
    public void onInstanceDestroy() {
		isInstanceDestroyed = true;
		movies.clear();
		doors.clear();
    }
}
