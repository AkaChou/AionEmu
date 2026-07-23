package com.aionemu.gameserver.instance.handlers.scripts;

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
 * 卡普斯岛储藏室副本事件处理器。
 * Instance event handler for Carpus Isle Storeroom.
 *
 * @author Encom
 */

@InstanceID(300050000)
public class CarpusIsleStoreroomInstance extends GeneralInstanceHandler
{
	private static final int[] TREASURE_BOX_IDS = { 700475, 700476, 700477, 701483, 701488,
		702850, 702851, 702852, 702853, 702855 };
	
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
		if (runtimeState().getBoolean("carpus.expired", false)) {
			deleteTreasureBoxes();
			return;
		}
		long deadline = runtimeState().getLong("carpus.deadline", 0);
		if (deadline > 0) {
			scheduleDeadline("treasure", deadline, this::expireTreasure);
		}
    }

	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		long deadline = runtimeState().getLong("carpus.deadline", 0);
		if (runtimeState().getBoolean("carpus.expired", false)) {
			return;
		}
		if (deadline > 0 && deadline <= System.currentTimeMillis()) {
			expireTreasure();
		} else if (deadline > System.currentTimeMillis()) {
			PacketSendUtility.sendPacket(player,
				new SM_QUEST_ACTION(0, (int) ((deadline - System.currentTimeMillis()) / 1000)));
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
		    if (runtimeState().getLong("carpus.deadline", 0) == 0) {
				startCarpusIsleStoreroomChamberTimer();
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
	
	private void startCarpusIsleStoreroomChamberTimer() {
		long deadline = System.currentTimeMillis() + 900_000;
		runtimeState().put("carpus.deadline", deadline);
		scheduleDeadline("treasure", deadline, this::expireTreasure);
    }

	private void expireTreasure() {
		if (runtimeState().getBoolean("carpus.expired", false)) {
			return;
		}
		runtimeState().put("carpus.expired", true);
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
		storage.decreaseByItemId(185000033, storage.getItemCountByItemId(185000033)); //Golden Abyss Key.
		storage.decreaseByItemId(185000034, storage.getItemCountByItemId(185000034)); //Jeweled Abyss Key.
		storage.decreaseByItemId(185000035, storage.getItemCountByItemId(185000035)); //Magic Abyss Key.
	}
}
