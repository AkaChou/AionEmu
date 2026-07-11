package com.aionemu.gameserver.controllers;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerPetsDAO;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PET;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 宠物控制器，管理宠物可见性与心情值更新任务。
 * Pet controller that manages pet visibility and mood-update tasks.
 *
 * @author ATracer
 */
public class PetController extends VisibleObjectController<Pet> {

	/**
	 * 宠物看到其他可见对象时的回调（当前无逻辑）。
	 * Callback when the pet sees another visible object (currently no-op).
	 *
	 * @param object 进入视野的对象 / the object entering sight
	 */
	@Override
	public void see(VisibleObject object) {

	}

	/**
	 * 宠物不再看到其他可见对象时的回调（当前无逻辑）。
	 * Callback when the pet no longer sees another visible object (currently no-op).
	 *
	 * @param object 离开视野的对象 / the object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
	}

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
		 *
		 * @param player 宠物所属玩家 / owner player of the pet
		 */
		public PetUpdateTask(Player player) {
			this.player = player;
		}

		/**
		 * 执行心情更新：满点时通知并落库，异常时取消任务。
		 * Runs mood update: notifies and persists at full points, cancels the task on error.
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
				int currentPoints = 0;
				boolean saved = false;

				if (pet.getCommonData().getMoodPoints(false) < 9000) {
					if (System.currentTimeMillis() - startTime >= 60 * 1000) {
						currentPoints = pet.getCommonData().getMoodPoints(false);
						if (currentPoints == 9000) {
							PacketSendUtility.sendPacket(player, new SM_PET(pet, 4, 0));
						}

						DAOManager.getDAO(PlayerPetsDAO.class).savePetMoodData(pet.getCommonData());
						saved = true;
						startTime = System.currentTimeMillis();
					}
				}

				if (currentPoints < 9000) {
					PacketSendUtility.sendPacket(player, new SM_PET(pet, 4, 0));
				} else {
					PacketSendUtility.sendPacket(player, new SM_PET(pet, 3, 0));
					// 玩家安抚宠物后达到 100% 时保存，而非由调度器。 / Save if it reaches 100% after player snuggles the pet, not by the scheduler
					// 自身 / itself
					if (!saved) {
						DAOManager.getDAO(PlayerPetsDAO.class).savePetMoodData(pet.getCommonData());
					}
				}
			} catch (Exception ex) {
				player.getController().cancelTask(TaskId.PET_UPDATE);
			}
		}
	}
}
