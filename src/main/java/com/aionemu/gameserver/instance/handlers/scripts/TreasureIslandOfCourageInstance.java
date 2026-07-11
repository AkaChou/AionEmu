package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Map;
import java.util.concurrent.Future;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 勇气宝藏岛副本事件处理器。
 * Instance event handler for Treasure Island Of Courage.
 *
 * @author (Encom)
 */

@InstanceID(301700000)
public class TreasureIslandOfCourageInstance extends GeneralInstanceHandler {

    /** 开始时间 / start time */
    @SuppressWarnings("unused")
	private long startTime;
    /** 门映射 / door map */
    private Map<Integer, StaticDoor> doors;
    /** 副本计时器 / instance timer */
        private Future<?> instanceTimer;
    /** 副本是否已销毁 / whether the instance is destroyed */
    protected boolean isInstanceDestroyed = false;

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
	public void onEnterInstance(final Player player) {
		super.onInstanceCreate(instance);
		if (instanceTimer == null) {
			startTime = System.currentTimeMillis();
			instanceTimer = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					openFirstDoors();
				}
			}, 60000);
		}
	}
/**
 * 处理 openFirstDoors。
 * Handle openFirstDoors.
 */

	protected void openFirstDoors() {
		openDoor(8);
		openDoor(93);
	}
/**
 * 打开指定门。
 * Open the given door.
 *
 * doorId
 */

	protected void openDoor(int doorId) {
		StaticDoor door = doors.get(doorId);
		if (door != null) {
			door.setOpen(true);
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
			case 835544: // Ancient Hero's Shoe
			case 835592:
				GameEngineServices.skillEngine().applyEffectDirectly(11277, player, player, 4000 * 1);
				ItemService.addItem(player, 190100295, 1); // Unicorn
				ItemService.addItem(player, 169300017, 1); // Hero`s Might
				break;
			case 835545: // Ancient Hero's Shield
			case 835593:
				GameEngineServices.skillEngine().applyEffectDirectly(11278, player, player, 4000 * 1);
				ItemService.addItem(player, 190100295, 1); // Unicorn
				ItemService.addItem(player, 169300017, 1); // Hero`s Might
				break;
			case 835546: // Ancient Hero's Trap
			case 835594:
				GameEngineServices.skillEngine().applyEffectDirectly(11279, player, player, 4000 * 1);
				ItemService.addItem(player, 190100295, 1); // Unicorn
				ItemService.addItem(player, 169300017, 1); // Hero`s Might
				break;
			case 835547: // Ancient Hero's Hook
			case 835794:
				GameEngineServices.skillEngine().applyEffectDirectly(11280, player, player, 4000 * 1);
				ItemService.addItem(player, 190100295, 1); // Unicorn
				ItemService.addItem(player, 169300017, 1); // Hero`s Might
				break;
		}
	}

	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(190100295, 1);
		storage.decreaseByItemId(169300017, 1);
		player.getEffectController().removeEffect(11277);
		player.getEffectController().removeEffect(11278);
		player.getEffectController().removeEffect(11279);
		player.getEffectController().removeEffect(11280);
	}

	private void stopInstanceTask() {
		if (instanceTimer != null) {
			instanceTimer.cancel(true);
		}
	}

	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
		doors.clear();
		stopInstanceTask();
	}

	/**
	 * 玩家请求退出副本时处理。
	 * Handle a player exit request.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onExitInstance(Player player) {
		removeItems(player);
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
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
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}

	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * 玩家 / player
	 * @param lastAttacker 最后攻击者 / last attacker
	 * result
	 */
	@Override
	public boolean onDie(final Player player, Creature lastAttacker) {
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.DIE, 0, player.equals(lastAttacker) ? 0 : lastAttacker.getObjectId()), true);
		PacketSendUtility.sendPacket(player, new SM_DIE(player.haveSelfRezEffect(), player.haveSelfRezItem(), 0, 8));
		return true;
	}
}
