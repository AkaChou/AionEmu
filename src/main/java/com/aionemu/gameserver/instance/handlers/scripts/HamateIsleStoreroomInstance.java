package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
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
 * 哈马特岛储藏室副本事件处理器。
 * Instance event handler for Hamate Isle Storeroom.
 *
 * @author Encom
 */

@InstanceID(300070000)
public class HamateIsleStoreroomInstance extends GeneralInstanceHandler
{
	/** 哈马特岛储藏室任务 / hamate isle storeroom task */
		private Future<?> hamateIsleStoreroomTask;
	/** 是否启动计时器 / is start timer */
		private boolean isStartTimer = false;
	/** hamate isle storeroom treasure box suscess / hamate isle storeroom treasure box suscess */
		private List<Npc> HamateIsleStoreroomTreasureBoxSuscess = new ArrayList<Npc>();
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
		spawnHamateIsleStoreroomRings();
		switch (Rnd.get(1, 2)) {
			case 1:
				spawn(214780, 381.35986f, 510.61307f, 102.618126f, (byte) 111); //Dakaer Diabolist.
			break;
			case 2:
				spawn(214781, 381.35986f, 510.61307f, 102.618126f, (byte) 111); //Dakaer Bloodmender.
			break;
		} switch (Rnd.get(1, 2)) {
			case 1:
				spawn(214782, 625.4933f, 455.0907f, 102.63267f, (byte) 47); //Dakaer Adjutant.
			break;
			case 2:
				spawn(214784, 625.4933f, 455.0907f, 102.63267f, (byte) 47); //Dakaer Physician.
			break;
		} switch (Rnd.get(1, 2)) {
			case 1:
				spawn(215449, 503.947f, 623.82227f, 103.695724f, (byte) 90); //Relic Protector Kael.
			break;
			case 2:
				spawn(215450, 503.947f, 623.82227f, 103.695724f, (byte) 90); //Ebonlord Vasana.
			break;
		}
		HamateIsleStoreroomTreasureBoxSuscess.add((Npc) spawn(700472, 377.06046f, 512.4419f, 102.618126f, (byte) 114));
		HamateIsleStoreroomTreasureBoxSuscess.add((Npc) spawn(700473, 628.6996f, 451.98642f, 102.63267f, (byte) 48));
		HamateIsleStoreroomTreasureBoxSuscess.add((Npc) spawn(700474, 503.7779f, 630.8419f, 104.54881f, (byte) 90));
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
			case 214780: //Dakaer Diabolist.
			case 214781: //Dakaer Bloodmender.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 185000036, 1)); //Golden Ruins Of Roah Key.
					}
				}
			break;
			case 214782: //Dakaer Adjutant.
			case 214784: //Dakaer Physician.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 185000037, 1)); //Jeweled Ruins Of Roah Key.
					}
				}
			break;
			case 215449: //Relic Protector Kael.
			case 215450: //Ebonlord Vasana.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 185000038, 1)); //Magic Ruins Of Roah Key.
					}
				}
			break;
		}
	}
	
	private void spawnHamateIsleStoreroomRings() {
        FlyRing f1 = new FlyRing(new FlyRingTemplate("HAMATE_ISLE_STOREROOM", mapId,
        new Point3D(501.77, 409.53, 94.12),
        new Point3D(503.93, 409.65, 98.9),
        new Point3D(506.26, 409.7, 94.15), 10), instanceId);
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
        if (flyingRing.equals("HAMATE_ISLE_STOREROOM")) {
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
							startHamateIsleStoreroomTimer();
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
	
	private void startHamateIsleStoreroomTimer() {
		hamateIsleStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				// 所有龙族宝箱已消失。 / All Balaur treasure chests have disappeared.
				sendMsg(1400244);
				HamateIsleStoreroomTreasureBoxSuscess.get(0).getController().onDelete();
				HamateIsleStoreroomTreasureBoxSuscess.get(1).getController().onDelete();
				HamateIsleStoreroomTreasureBoxSuscess.get(2).getController().onDelete();
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
        storage.decreaseByItemId(185000036, storage.getItemCountByItemId(185000036)); //Golden Ruins Of Roah Key.
        storage.decreaseByItemId(185000037, storage.getItemCountByItemId(185000037)); //Jeweled Ruins Of Roah Key.
        storage.decreaseByItemId(185000038, storage.getItemCountByItemId(185000038)); //Magic Ruins Of Roah Key.
    }
}
