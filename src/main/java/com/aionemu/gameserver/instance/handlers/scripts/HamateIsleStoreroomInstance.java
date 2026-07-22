package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 哈马特岛储藏室副本事件处理器。
 * Instance event handler for Hamate Isle Storeroom.
 *
 * @author Encom
 */

@InstanceID(300070000)
public class HamateIsleStoreroomInstance extends GeneralInstanceHandler
{
	private static final int[] TREASURE_BOX_IDS = { 700472, 700473, 700474, 701484, 701489,
		702847, 702848, 702849, 702854, 702856 };
	
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
		if (runtimeState().getBoolean("hamate.expired", false)) {
			deleteTreasureBoxes();
			return;
		}
		long deadline = runtimeState().getLong("hamate.deadline", 0);
		if (deadline > 0) {
			scheduleDeadline("treasure", deadline, this::expireTreasure);
		}
    }

	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		long deadline = runtimeState().getLong("hamate.deadline", 0);
		if (runtimeState().getBoolean("hamate.expired", false)) {
			return;
		}
		if (deadline > 0 && deadline <= System.currentTimeMillis()) {
			expireTreasure();
		} else if (deadline > System.currentTimeMillis()) {
			PacketSendUtility.sendPacket(player,
				new SM_QUEST_ACTION(0, (int) ((deadline - System.currentTimeMillis()) / 1000)));
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
		if (runtimeState().getBoolean("hamate.expired", false)) {
			return;
		}
		runtimeState().put("hamate.expired", true);
		sendMsg(1400244);
		deleteTreasureBoxes();
	}

	private void deleteTreasureBoxes() {
		for (int npcId : TREASURE_BOX_IDS) {
			instance.getNpcs(npcId).forEach(npc -> npc.getController().onDelete());
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
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
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
