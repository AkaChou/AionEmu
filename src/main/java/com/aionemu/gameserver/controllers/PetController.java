package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.services.player.PlayerService;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PET;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 宠物控制器，管理宠物可见性与心情值更新任务。
 * Pet controller that manages pet visibility and mood-update tasks.
 * @author ATracer
 */
public class PetController extends VisibleObjectController<Pet> {

	/**
	 * 宠物心情值周期性更新任务。
	 * Periodic task that updates the pet's mood points.
	 */
	public static class PetUpdateTask implements Runnable {

		/** 宠物所属玩家。 / Owner player of the pet. */
		private final Player player;
		/** 本轮计时起点时间戳。 / Start timestamp of the current timing window. */
		private long startTime = 0;

		/**
		 * 构造宠物更新任务。
		 * Constructs a pet update task.
		 * @param player 宠物所属玩家 / owner player of the pet
		 */
		public PetUpdateTask(Player player) {
			this.player = player;
		}

		/**
		 * 执行心情更新：心情未满时每 60 秒落库一次并向客户端同步心情状态；
		 * 心情满后的礼物发放与落库由 PetMoodService 在玩家互动/领礼时处理。异常时取消任务。
		 * Runs mood update: persists mood data every 60 seconds and syncs the mood state
		 * to the client while below the full threshold; the gift and persistence at full
		 * mood are handled by PetMoodService on player interaction. Cancels the task on error.
		 */
		@Override
		public void run() {
			if (startTime == 0) {
				startTime = System.currentTimeMillis();
			}
			try {
				Pet pet = player.getPet();
				if (pet == null) {
					throw new IllegalStateException("Pet is null");
				}
				if (pet.getCommonData().getMoodPoints(false) < 9000
					&& System.currentTimeMillis() - startTime >= 60 * 1000) {
					PlayerService.daos().playerPetsDAO().savePetMoodData(pet.getCommonData());
					startTime = System.currentTimeMillis();
				}
				PacketSendUtility.sendPacket(player, new SM_PET(pet, 4, 0));
			} catch (Exception ex) {
				player.getController().cancelTask(TaskId.PET_UPDATE);
			}
		}
	}
}
