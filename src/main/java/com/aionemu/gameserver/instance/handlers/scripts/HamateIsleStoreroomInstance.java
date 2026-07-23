package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

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

	@Override
	public void onEnterZone(Player player, ZoneInstance zone) {
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("HAMATE_ISLE_STOREROOM_TIMER_300070000")) {
			startHamateIsleStoreroomTimer();
		}
	}
	
	private synchronized void startHamateIsleStoreroomTimer() {
		if (runtimeState().getLong("hamate.deadline", 0) != 0
				|| runtimeState().getBoolean("hamate.expired", false)) {
			return;
		}
		long deadline = System.currentTimeMillis() + 900_000;
		runtimeState().put("hamate.deadline", deadline);
		scheduleDeadline("treasure", deadline, this::expireTreasure);
		instance.doOnAllPlayers(player -> {
			if (player.isOnline()) {
				PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 900));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_START_IDABRE);
			}
		});
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
