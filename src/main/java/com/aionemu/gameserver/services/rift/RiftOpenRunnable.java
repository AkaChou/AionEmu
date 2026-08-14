package com.aionemu.gameserver.services.rift;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import java.util.Map;

import com.aionemu.gameserver.model.rift.RiftLocation;
import com.aionemu.gameserver.services.RiftService;

/**
 * 裂隙开启定时任务，按世界 ID 打开对应裂隙并向玩家广播状态。
 * Runnable that opens rifts for a world id and broadcasts rift state to players.
 *
 * @author Rinzler (Encom)
 */
public class RiftOpenRunnable implements Runnable {
	private final int worldId;

	/**
	 * 构造指定世界的裂隙开启任务。
	 * Create an open-rift task for the given world.
	 *
	 * @param worldId 世界地图 ID / World map id
	 */
	public RiftOpenRunnable(int worldId) {
		this.worldId = worldId;
	}

	/**
	 * 打开该世界全部裂隙位置并发送裂隙信息。
	 * Opens all rift locations in the world and sends rift info.
	 */
	@Override
	public void run() {
		Map<Integer, RiftLocation> locations = GameLocationBootstrapServices.riftService().getRiftLocations();
		for (final RiftLocation loc : locations.values()) {
			if (loc.getWorldId() == worldId) {
				GameLocationBootstrapServices.riftService().openRifts(loc);
			}
		}
		RiftInformer.sendRiftsInfo(worldId);
	}
}
