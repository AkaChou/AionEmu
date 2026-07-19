package com.aionemu.gameserver.instance.handlers.scripts;

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

/**
 * 哈马特岛储藏室副本事件处理器。
 * Instance event handler for Hamate Isle Storeroom.
 *
 * @author Encom
 */

@InstanceID(300070000)
public class HamateIsleStoreroomInstance extends GeneralInstanceHandler
{
	/** hamate isle storeroom treasure box suscess / hamate isle storeroom treasure box suscess */
		private final List<Npc> treasureBoxes = new ArrayList<>();
	
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
		spawnSelectedNpc("hamate.guard.1", 214780, 214781, 381.35986f, 510.61307f, 102.618126f, (byte) 111);
		spawnSelectedNpc("hamate.guard.2", 214782, 214784, 625.4933f, 455.0907f, 102.63267f, (byte) 47);
		spawnSelectedNpc("hamate.guard.3", 215449, 215450, 503.947f, 623.82227f, 103.695724f, (byte) 90);
		if (!runtimeState().getBoolean("hamate.expired", false)) {
			treasureBoxes.add((Npc) spawn(700472, 377.06046f, 512.4419f, 102.618126f, (byte) 114));
			treasureBoxes.add((Npc) spawn(700473, 628.6996f, 451.98642f, 102.63267f, (byte) 48));
			treasureBoxes.add((Npc) spawn(700474, 503.7779f, 630.8419f, 104.54881f, (byte) 90));
			long deadline = runtimeState().getLong("hamate.deadline", 0);
			if (deadline > 0) {
				scheduleDeadline("treasure", deadline, this::expireTreasure);
			}
		}
    }

	private void spawnSelectedNpc(String key, int firstNpcId, int secondNpcId, float x, float y, float z, byte heading) {
		int npcId = runtimeState().getInt(key, 0);
		if (npcId == 0) {
			npcId = Rnd.get(1, 2) == 1 ? firstNpcId : secondNpcId;
			runtimeState().put(key, npcId);
		}
		spawn(npcId, x, y, z, heading);
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
		    if (runtimeState().getLong("hamate.deadline", 0) == 0) {
				startHamateIsleStoreroomTimer();
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
		long deadline = System.currentTimeMillis() + 900_000;
		runtimeState().put("hamate.deadline", deadline);
		scheduleDeadline("treasure", deadline, this::expireTreasure);
    }

	private void expireTreasure() {
		runtimeState().put("hamate.expired", true);
		sendMsg(1400244);
		treasureBoxes.forEach(box -> box.getController().onDelete());
		treasureBoxes.clear();
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
