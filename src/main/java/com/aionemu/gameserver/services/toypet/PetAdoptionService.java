package com.aionemu.gameserver.services.toypet;

import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PET;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 宠物领养服务，处理孵蛋领养与放弃宠物。
 * Pet adoption service handling egg-based adoption and pet surrender.
 */
public class PetAdoptionService {

	/**
	 * 使用宠物蛋领养宠物。
	 * Adopt a pet using a pet egg item.
	 *
	 * 玩家 / Player
	 * @param eggObjId 宠物蛋物品对象 ID / Egg item object id
	 * @param petId 宠物模板 ID / Pet template id
	 * @param name 宠物名称 / Pet name
	 * Decoration id
	 */
	public static void adoptPet(Player player, int eggObjId, int petId, String name, int decorationId) {
		int eggId = player.getInventory().getItemByObjId(eggObjId).getItemId();
		ItemTemplate template = DataManager.ITEM_DATA.getItemTemplate(eggId);
		if (!validateAdoption(player, template, petId)) {
			return;
		}
		if (!player.getInventory().decreaseByObjectId(eggObjId, 1)) {
			return;
		}
		int expireTime = template.getActions().getAdoptPetAction().getExpireMinutes() != 0
				? (int) ((System.currentTimeMillis() / 1000)
						+ template.getActions().getAdoptPetAction().getExpireMinutes() * 60)
				: 0;
		addPet(player, petId, name, decorationId, expireTime);
	}

	/**
	 * 向玩家宠物列表添加宠物，并在有限期时注册过期任务。
	 * Add a pet to the player's pet list and register expire task when limited.
	 *
	 * 玩家 / Player
	 * @param petId 宠物模板 ID / Pet template id
	 * @param name 宠物名称 / Pet name
	 * Decoration id
	 * @param expireTime 过期时间戳（秒，0 表示永久） / Expire unix time in seconds (0 = permanent)
	 */
	public static void addPet(Player player, int petId, String name, int decorationId, int expireTime) {
		PetCommonData petCommonData = player.getPetList().addPet(player, petId, decorationId, name, expireTime);
		if (petCommonData != null) {
			PacketSendUtility.sendPacket(player, new SM_PET(1, petCommonData));
			if (expireTime > 0) {
				GameTaskManagerServices.expireTimerTask().addTask(petCommonData, player);
			}
		}
	}

	/**
	 * 校验领养请求是否合法（模板、动作、是否已拥有等）。
	 * Validate whether the adoption request is legal (template, action, ownership, etc.).
	 *
	 * @param player 玩家 / Player
	 * @param template 宠物蛋物品模板 / Egg item template
	 * @param petId 宠物模板 ID / Pet template id
	 * @return 是否通过校验 / Whether validation passed
	 */
	private static boolean validateAdoption(Player player, ItemTemplate template, int petId) {
		if (template == null || template.getActions() == null || template.getActions().getAdoptPetAction() == null
				|| template.getActions().getAdoptPetAction().getPetId() != petId) {
			return false;
		}
		if (player.getPetList().hasPet(petId)) {
			return false;
		}
		if (DataManager.PET_DATA.getPetTemplate(petId) == null) {
			return false;
		}
		return true;
	}

	/**
	 * 放弃（删除）指定宠物；若当前已召唤则先解散。
	 * Surrender (delete) the given pet; dismiss first if currently summoned.
	 *
	 * @param player 玩家 / Player
	 * @param petId 宠物模板 ID / Pet template id
	 */
	public static void surrenderPet(Player player, int petId) {
		PetCommonData petCommonData = player.getPetList().getPet(petId);
		if (player.getPet() != null && player.getPet().getPetId() == petCommonData.getPetId()) {
			if (petCommonData.getFeedProgress() != null) {
				petCommonData.setCancelFeed(true);
			}
			PetSpawnService.dismissPet(player, false);
		}
		player.getPetList().deletePet(petCommonData.getPetId());
		PacketSendUtility.sendPacket(player, new SM_PET(2, petCommonData));
	}
}
