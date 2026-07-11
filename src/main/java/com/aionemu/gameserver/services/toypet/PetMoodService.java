package com.aionemu.gameserver.services.toypet;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PET;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import lombok.extern.slf4j.Slf4j;

/**
 * 宠物心情服务，处理抚摸互动与心情礼物领取。
 * Pet mood service handling cuddle interaction and mood gift claims.
 */
@Slf4j
public class PetMoodService {

	/**
	 * 按类型处理宠物心情相关客户端请求。
	 * Handle pet-mood client requests by type.
	 *
	 * Pet
	 * @param type 请求类型（0 查看 / 1 互动 / 3 领礼） / Request type (0 check / 1 interact / 3 gift)
	 * Shuggle emotion id
	 */
	public static void checkMood(Pet pet, int type, int shuggleEmotion) {
		switch (type) {
		case 0:
			startCheckingMood(pet);
			break;
		case 1:
			interactWithPet(pet, shuggleEmotion);
			break;
		case 3:
			requestPresent(pet);
			break;
		}
	}

	/**
	 * 在心情达标且冷却结束时发放礼物。
	 * Grant a gift when mood threshold is met and cooldown is over.
	 *
	 * Pet
	 */
	private static void requestPresent(Pet pet) {
		if (pet.getCommonData().getMoodPoints(false) < 9000) {
			log.warn(I18n.get("log.d24173d153f7", pet.getMaster().getName()));
			return;
		}
		if (pet.getCommonData().getGiftRemainingTime() > 0) {
			AuditLogger.info(pet.getMaster(), "Trying to get gift during CD for pet " + pet.getPetId());
			return;
		}
		if (pet.getMaster().getInventory().isFull()) {
			// 背包已满。请腾出空间后再请求礼物。 / Your cube is full. Wait before asking for a gift.
			PacketSendUtility.sendPacket(pet.getMaster(), SM_SYSTEM_MESSAGE.STR_PET_CONDITION_REWARD_FULL_INVEN);
			return;
		}
		pet.getCommonData().clearMoodStatistics();
		PacketSendUtility.sendPacket(pet.getMaster(), new SM_PET(pet, 4, 0));
		PacketSendUtility.sendPacket(pet.getMaster(), new SM_PET(pet, 3, 0));
		int itemId = pet.getPetTemplate().getConditionReward();
		if (itemId != 0) {
			ItemService.addItem(pet.getMaster(), pet.getPetTemplate().getConditionReward(), 1);
		}
	}

	/**
	 * 与宠物互动（抚摸）并提升心情。
	 * Interact (shuggle) with the pet and raise mood.
	 *
	 * Pet
	 * Shuggle emotion id
	 */
	private static void interactWithPet(Pet pet, int shuggleEmotion) {
		if (pet.getCommonData() != null) {
			if (pet.getCommonData().increaseShuggleCounter()) {
				PacketSendUtility.sendPacket(pet.getMaster(), new SM_PET(pet, 2, shuggleEmotion));
				PacketSendUtility.sendPacket(pet.getMaster(), new SM_PET(pet, 4, 0));
			}
		}
	}

	/**
	 * 向客户端发送当前心情状态。
	 * Send current mood state to the client.
	 *
	 * Pet
	 */
	private static void startCheckingMood(Pet pet) {
		PacketSendUtility.sendPacket(pet.getMaster(), new SM_PET(pet, 0, 0));
	}
}
