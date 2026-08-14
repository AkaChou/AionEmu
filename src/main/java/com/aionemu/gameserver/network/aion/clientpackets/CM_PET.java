package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEventBootstrapServices;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.PetAction;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PET;
import com.aionemu.gameserver.services.NameRestrictionService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.services.toypet.PetAdoptionService;
import com.aionemu.gameserver.services.toypet.PetMoodService;
import com.aionemu.gameserver.services.toypet.PetService;
import com.aionemu.gameserver.services.toypet.PetSpawnService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 宠物收养、召唤、喂养等操作的客户端包。
 * Client packet for pet adopt/summon/feed and related actions.
 *
 * @author M@xx, xTz
 */
@Slf4j
public class CM_PET extends AionClientPacket {

	private int actionId;
	private PetAction action;
	private int petId;
	private String petName;
	private int decorationId;
	private int eggObjId;
	private int objectId;
	private int count;
	private int subType;
	private int emotionId;
	private int actionType;
	private int dopingItemId;
	private int dopingAction;
	private int dopingSlot1;
	private int dopingSlot2;
	private int activateLoot;
	@SuppressWarnings("unused")
	private int unk2;
	@SuppressWarnings("unused")
	private int unk3;
	@SuppressWarnings("unused")
	private int unk5;
	@SuppressWarnings("unused")
	private int unk6;

	// 增益 / Buff
	private int activateCheering;
	@SuppressWarnings("unused")
	private int unkCheer2;
	@SuppressWarnings("unused")
	private int unkCheer3;

	// 商人 / Merchand
	private int activateAutoSell;
	@SuppressWarnings("unused")
	private int unkMerchand2;
	@SuppressWarnings("unused")
	private int unkMerchand3;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_PET(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 按宠物动作类型读取操作参数。
	 * Reads pet action parameters by action type.
	 */
	@Override
	protected void readImpl() {
		actionId = readH();
		action = PetAction.getActionById(actionId);
		switch (action) {
		case ADOPT:
			eggObjId = readD();
			petId = readD();
			unk2 = readC();
			unk3 = readD();
			decorationId = readD();
			unk5 = readD();
			unk6 = readD();
			petName = readS();
			break;
		case SURRENDER:
		case SPAWN:
		case DISMISS:
			petId = readD();
			break;
		case FOOD:
			actionType = readD();
			if (actionType == 3) {
				activateLoot = readD();
			} else if (actionType == 2) {
				dopingAction = readD();
				if (dopingAction == 0) { // 添加物品 / add item
					dopingItemId = readD();
					dopingSlot1 = readD();
				} else if (dopingAction == 1) { // 移除物品 / remove item
					dopingSlot1 = readD();
					dopingItemId = readD();
				} else if (dopingAction == 2) { // 移动物品 / move item
					dopingSlot1 = readD();
					dopingSlot2 = readD();
				} else if (dopingAction == 3) { // 使用宠物药 / use doping
					dopingItemId = readD();
					dopingSlot1 = readD();
				}
			} else if (actionType == 4) {
				activateAutoSell = readD();
				unkMerchand2 = readD();
				unkMerchand3 = readD();
			} else if (actionType == 5) {
				activateCheering = readD();
				unkCheer2 = readD();
				unkCheer3 = readD();
			} else {
				objectId = readD();
				count = readD();
				unk2 = readD();
			}
			break;
		case RENAME:
			petId = readD();
			petName = readS();
			break;
		case MOOD:
			subType = readD();
			emotionId = readD();
			break;
		default:
			break;
		}
	}
	/**
	 * 执行宠物收养、召唤、喂养等动作。
	 * Executes pet adopt, summon, feed, and related actions.
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		Pet pet = player.getPet();
		switch (action) {
		case ADOPT:
			if (NameRestrictionService.isForbiddenWord(petName)) {
				PacketSendUtility.sendMessage(player, "You are trying to use a forbidden name. Choose another one!");
			} else {
				PetAdoptionService.adoptPet(player, eggObjId, petId, petName, decorationId);
			}
			break;
		case SURRENDER:
			PetAdoptionService.surrenderPet(player, petId);
			break;
		case SPAWN:
			if (player.getMinion() != null) {
				GameEventBootstrapServices.minionService().despawnMinion(player, 0);
			}
			GameFeatureServices.petService().switchOffBuff(player);
			PetSpawnService.summonPet(player, petId, true);
			break;
		case DISMISS:
			GameFeatureServices.petService().switchOffBuff(player);
			PetSpawnService.dismissPet(player, true);
			break;
		case FOOD:
			if (actionType == 2) {
				// 宠物用药 / Pet doping
				if (dopingAction == 2) {
					GameFeatureServices.petService().relocateDoping(player, dopingSlot1, dopingSlot2);
				} else {
					GameFeatureServices.petService().useDoping(player, dopingAction, dopingItemId, dopingSlot1);
				}
			} else if (actionType == 3) {
				// 宠物拾取 / Pet looting
				GameFeatureServices.petService().activateLoot(player, activateLoot != 0);
			} else if (actionType == 4) {
				if (activateAutoSell == 1) {
					GameFeatureServices.petService().activeAutoSell(player, true);
				} else if (activateAutoSell == 0) {
					GameFeatureServices.petService().activeAutoSell(player, false);
				}
			} else if (actionType == 5) {
				if (activateCheering == 1) {
					GameFeatureServices.petService().activateBuff(player, true);
				} else if (activateCheering == 0) {
					GameFeatureServices.petService().activateBuff(player, false);
				}
			} else if (pet != null) {
				if (objectId == 0) {
					pet.getCommonData().setCancelFeed(true);
					PacketSendUtility.sendPacket(player, new SM_PET(4, actionId, 0, 0, player.getPet()));
					PacketSendUtility.sendPacket(player,
							new SM_EMOTION(player, EmotionType.END_FEEDING, 0, player.getObjectId()));
				} else if (!pet.getCommonData().isFeedingTime()) {
					PacketSendUtility.sendPacket(player, new SM_PET(8, actionId, objectId, count, player.getPet()));
				} else {
					GameFeatureServices.petService().removeObject(objectId, count, actionId, player);
				}
			}
			break;
		case RENAME:
			if (NameRestrictionService.isForbiddenWord(petName)) {
				PacketSendUtility.sendMessage(player, "You are trying to use a forbidden name. Choose another one!");
			} else {
				GameFeatureServices.petService().renamePet(player, petName);
			}
			break;
		case MOOD:
			if (pet != null && (subType == 0 && pet.getCommonData().getMoodRemainingTime() == 0
					|| (subType == 3 && pet.getCommonData().getGiftRemainingTime() == 0) || emotionId != 0)) {
				PetMoodService.checkMood(pet, subType, emotionId);
			}
		default:
			break;
		}
	}
}
