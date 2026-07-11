package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 卡普斯岛储藏室副本事件处理器。
 * Instance event handler for Carpus Isle Storeroom.
 *
 * @author Encom
 */

@InstanceID(300050000)
public class CarpusIsleStoreroomInstance extends GeneralInstanceHandler
{
	/** carpus 岛储藏室任务 / carpus isle storeroom task */
		private Future<?> carpusIsleStoreroomTask;
	/** 是否启动计时器 / is start timer */
		private boolean isStartTimer = false;
	/** carpus isle storeroom treasure box suscess / carpus isle storeroom treasure box suscess */
		private List<Npc> CarpusIsleStoreroomTreasureBoxSuscess = new ArrayList<Npc>();
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
        spawnCarpusIsleStoreroomRings();
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
			case 214762: //Dakaer Tactician.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 185000033, 1)); //Golden Abyss Key.
					}
				}
			break;
			case 214766: //Dakaer Chanter.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 185000034, 1)); //Jeweled Abyss Key.
					}
				}
			break;
			case 215444: //Ebonlord Kiriel.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 185000035, 1)); //Magic Abyss Key.
					}
				}
			break;
		}
	}
	
	private void spawnCarpusIsleStoreroomRings() {
        FlyRing f1 = new FlyRing(new FlyRingTemplate("CARPUS_ISLE_STOREROOM", mapId,
        new Point3D(479.24, 572.57, 202.72),
        new Point3D(477.95, 567.64, 212.9),
        new Point3D(477.97, 563.35, 202.12), 10), instanceId);
        f1.spawn();
    }
	
	/**
	 * 玩家通过飞行环时处理。
	 * Handle a player passing a flying ring.
	 *
	 * 玩家 / player
	 * @param flyingRing 飞行环标识 / flying-ring id
	 * result
	 */
	@Override
    public boolean onPassFlyingRing(Player player, String flyingRing) {
        if (flyingRing.equals("CARPUS_ISLE_STOREROOM")) {
		    if (!isStartTimer) {
			    isStartTimer = true;
			    System.currentTimeMillis();
			    instance.doOnAllPlayers(new Visitor<Player>() {
			        /**
			         * 处理 visit。
			         * Handle visit.
			         *
			         * @param player 玩家 / player
			         */
			        @Override
			        public void visit(Player player) {
						if (player.isOnline()) {
							startCarpusIsleStoreroomChamberTimer();
							PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 900));
							// 龙族防护魔法结界已激活。 / The Balaur protective magic ward has been activated.
							PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_START_IDABRE);
						}
					}
				});
			}
		}
		return false;
	}
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(final Player player) {
		super.onInstanceCreate(instance);
		CarpusIsleStoreroomTreasureBoxSuscess.add((Npc) spawn(700475, 524.4908f, 706.2591f, 191.8985f, (byte) 90));
		CarpusIsleStoreroomTreasureBoxSuscess.add((Npc) spawn(700476, 522.22754f, 421.55646f, 199.75935f, (byte) 29));
		CarpusIsleStoreroomTreasureBoxSuscess.add((Npc) spawn(700477, 671.581f, 565.1735f, 206.14534f, (byte) 60));
	}
	
	private void startCarpusIsleStoreroomChamberTimer() {
		carpusIsleStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				// 所有龙族宝箱已消失。 / All Balaur treasure chests have disappeared.
				sendMsg(1400244);
				CarpusIsleStoreroomTreasureBoxSuscess.get(0).getController().onDelete();
				CarpusIsleStoreroomTreasureBoxSuscess.get(1).getController().onDelete();
				CarpusIsleStoreroomTreasureBoxSuscess.get(2).getController().onDelete();
			}
		}, 900000); //15 Minutes.
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
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(185000033, storage.getItemCountByItemId(185000033)); //Golden Abyss Key.
		storage.decreaseByItemId(185000034, storage.getItemCountByItemId(185000034)); //Jeweled Abyss Key.
		storage.decreaseByItemId(185000035, storage.getItemCountByItemId(185000035)); //Magic Abyss Key.
	}
}