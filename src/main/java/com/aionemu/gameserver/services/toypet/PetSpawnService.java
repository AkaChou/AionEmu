package com.aionemu.gameserver.services.toypet;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.sql.Timestamp;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.PeriodicSaveConfig;
import com.aionemu.gameserver.controllers.PetController;
import com.aionemu.gameserver.dao.PlayerPetsDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.pet.PetDopingBag;
import com.aionemu.gameserver.model.templates.pet.PetFunction;
import com.aionemu.gameserver.model.templates.pet.PetTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PET;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_INFO;
import com.aionemu.gameserver.spawnengine.VisibleObjectSpawner;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 宠物生成服务，管理宠物召唤与周期存盘。
 * Pet spawn service managing pet summoning and periodic persistence.
 *
 * @author ATracer
 */
public class PetSpawnService {

	/**
	 * 召唤宠物；若已有其他宠物则先解散。
	 * Summon a pet; dismiss the current one if different.
	 *
	 * @param player 玩家 / Player
	 * @param petId 宠物模板 ID / Pet template id
	 * @param isManualSpawn 是否手动召唤 / Whether manually summoned
	 */
	public static final void summonPet(Player player, int petId, boolean isManualSpawn) {
		PetCommonData lastPetCommonData;

		if (player.getPet() != null) {
			if (player.getPet().getPetId() == petId) {
				PacketSendUtility.broadcastPacket(player, new SM_PET(3, player.getPet()), true);
				return;
			}

			lastPetCommonData = player.getPet().getCommonData();
			dismissPet(player, isManualSpawn);
		} else {
			lastPetCommonData = player.getPetList().getLastUsedPet();
		}

		if (lastPetCommonData != null) {
			// 若生成其他宠物则重置心情 / reset mood if other pet is spawned
			if (petId != lastPetCommonData.getPetId()) {
				lastPetCommonData.clearMoodStatistics();
			}
		}
		reschedulePeriodicSaveTask(player);

		Pet pet = VisibleObjectSpawner.spawnPet(player, petId);
		// 这意味着严重错误或作弊——为何只显示“null”？ / It means serious error or cheater - why its just nothing say "null"?
		if (pet != null) {
			sendWhInfo(player, petId);

			if (System.currentTimeMillis() - pet.getCommonData().getDespawnTime().getTime() > 10 * 60 * 1000) {
				// 若宠物取消生成超过 10 分钟则重置心情。 / reset mood if pet was despawned for longer than 10 mins.
				player.getPet().getCommonData().clearMoodStatistics();
			}
			lastPetCommonData = pet.getCommonData();
			player.getPetList().setLastUsedPetId(petId);
		}
	}

	/**
	 * 重新调度宠物数据的周期存盘任务。
	 * Reschedule the periodic pet data save task.
	 *
	 * @param player 玩家 / Player
	 */
	public static void reschedulePeriodicSaveTask(Player player) {
		player.getController().addTask(TaskId.PET_UPDATE,
				GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new PetController.PetUpdateTask(player),
						PeriodicSaveConfig.PLAYER_PETS * 1000, PeriodicSaveConfig.PLAYER_PETS * 1000));
	}

	/**
	 * 若宠物具备仓库功能，向客户端发送仓库信息。
	 * Send warehouse info to client when the pet has warehouse function.
	 *
	 * @param player 玩家 / Player
	 * @param petId 宠物模板 ID / Pet template id
	 */
	private static void sendWhInfo(Player player, int petId) {
		PetTemplate petTemplate = DataManager.PET_DATA.getPetTemplate(petId);
		PetFunction pf = petTemplate.getWarehouseFunction();
		if (pf != null && pf.getSlots() != 0) {
			int itemLocation = StorageType.getStorageId(pf.getSlots(), 6);
			if (itemLocation != -1) {
				PacketSendUtility.sendPacket(player, new SM_WAREHOUSE_INFO(
						player.getStorage(itemLocation).getItemsWithKinah(), itemLocation, 0, true, player));
				PacketSendUtility.sendPacket(player, new SM_WAREHOUSE_INFO(null, itemLocation, 0, false, player));
			}
		}
	}

	/**
	 * 解散当前宠物并保存喂养/增益/心情数据。
	 * Dismiss the current pet and persist feed/doping/mood data.
	 *
	 * 玩家 / Player
	 * @param isManualDespawn 是否手动解散 / Whether manually despawned
	 */
	public static final void dismissPet(Player player, boolean isManualDespawn) {
		Pet toyPet = player.getPet();
		if (toyPet != null) {
			PetFeedProgress progress = toyPet.getCommonData().getFeedProgress();
			if (progress != null) {
				toyPet.getCommonData().setCancelFeed(true);
				DAOManager.getDAO(PlayerPetsDAO.class).saveFeedStatus(player, toyPet.getPetId(),
						progress.getHungryLevel().getValue(), progress.getDataForPacket(),
						toyPet.getCommonData().getCurentTime());
			}
			PetDopingBag bag = toyPet.getCommonData().getDopingBag();
			if (bag != null && bag.isDirty()) {
				DAOManager.getDAO(PlayerPetsDAO.class).saveDopingBag(player, toyPet.getPetId(), bag);
			}
			player.getController().cancelTask(TaskId.PET_UPDATE);

			// 传送会暂时解散宠物，且不应启动手动再召唤冷却。 / Teleportation dismisses the pet temporarily and must not start the manual re-summon cooldown.
			if (isManualDespawn) {
				toyPet.getCommonData().setDespawnTime(new Timestamp(System.currentTimeMillis()));
			}
			toyPet.getCommonData().savePetMoodData();

			player.setToyPet(null);
			toyPet.getController().delete();
		}
	}
}
