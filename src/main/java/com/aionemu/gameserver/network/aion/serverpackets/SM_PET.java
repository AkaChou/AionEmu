package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.PetAction;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.templates.pet.PetDopingEntry;
import com.aionemu.gameserver.model.templates.pet.PetFunctionType;
import com.aionemu.gameserver.model.templates.pet.PetTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 宠物动作/状态同步服务端包（多模式：列表、领养、召唤、解散、喂食、心情、功能操作等）。
 * Multi-mode server packet that synchronizes pet actions and state
 * (list, adopt, spawn, despawn, feed, mood, functional ops, etc.).
 * <p>
 * actionId 主要分支：0=宠物列表，1=领养/新增，2=删除，3=召唤，4=解散，
 * 9=喂食进度，10=改名，12=心情，13=功能操作（药剂/拾取/欢呼等）。
 * Primary actionId branches: 0=list, 1=adopt/add, 2=remove, 3=spawn, 4=despawn,
 * 9=feed progress, 10=rename, 12=mood, 13=function ops (dope/loot/cheer, etc.).
 */
public class SM_PET extends AionServerPacket {
	private int actionId;
	private Pet pet;
	private PetCommonData commonData;
	private int itemObjectId;
	private Collection<PetCommonData> pets;
	private int count;
	private int subType;
	private int shuggleEmotion;
	private boolean isActing;
	private int lootNpcId;
	private int dopeAction;
	private int dopeSlot;

	/**
	 * 通用构造：指定子类型、动作、物品与数量。
	 * General constructor with subtype, action, item and count.
	 *
	 * subtype
	 * action id
	 * item object id
	 * count
	 * @param pet 宠物实例 / pet instance
	 */
	public SM_PET(int subType, int actionId, int objectId, int count, Pet pet) {
		this.subType = subType;
		this.actionId = actionId;
		this.count = count;
		this.itemObjectId = objectId;
		this.pet = pet;
		this.commonData = pet.getCommonData();
	}

	/**
	 * 仅动作 ID 的构造（无宠物载荷）。
	 * Action-id-only constructor (no pet payload).
	 *
	 * action id
	 */
	public SM_PET(int actionId) {
		this.actionId = actionId;
	}

	/**
	 * 指定动作与宠物的构造。
	 * Constructor for an action against a single pet.
	 *
	 * action id
	 * @param pet 宠物实例 / pet instance
	 */
	public SM_PET(int actionId, Pet pet) {
		this(0, actionId, 0, 0, pet);
	}

	/**
	 * 拾取功能开关（actionId=13, subType=3）。
	 * Loot-function toggle (actionId=13, subType=3).
	 *
	 * @param isLooting 是否正在拾取 / whether looting is active
	 */
	public SM_PET(boolean isLooting) {
		this.actionId = 13;
		this.isActing = isLooting;
		this.subType = 3;
	}

	/**
	 * 对指定 NPC 的拾取同步。
	 * Loot sync for a specific NPC corpse.
	 *
	 * @param isLooting 是否正在拾取 / whether looting is active
	 * target NPC id
	 */
	public SM_PET(boolean isLooting, int npcId) {
		this(isLooting);
		this.lootNpcId = npcId;
	}

	/**
	 * 药剂/Buff 功能开关（actionId=13, subType=2）。
	 * Dope/buff function toggle (actionId=13, subType=2).
	 *
	 * @param dopeAction 药剂子动作 / dope sub-action
	 * @param isBuffing 是否处于 Buff 中 / whether buffing is active
	 */
	public SM_PET(int dopeAction, boolean isBuffing) {
		this.actionId = 13;
		this.dopeAction = dopeAction;
		this.isActing = isBuffing;
		this.subType = 2;
	}

	/**
	 * 欢呼功能开关（actionId=13, subType=5）。
	 * Cheer function toggle (actionId=13, subType=5).
	 *
	 * @param isCheering 是否欢呼中 / whether cheering is active
	 * @param what 保留参数 / reserved
	 * reserved
	 */
	public SM_PET(boolean isCheering, int what, int wahtwaht) {
		this.actionId = 13;
		this.isActing = isCheering;
		this.subType = 5;
	}

	/**
	 * 药剂槽位操作（装入/使用等）。
	 * Dope-slot operation (insert/use, etc.).
	 *
	 * @param dopeAction 药剂子动作 / dope sub-action
	 * item id
	 * slot index
	 */
	public SM_PET(int dopeAction, int itemId, int slot) {
		this(dopeAction, true);
		itemObjectId = itemId;
		dopeSlot = slot;
	}

	/**
	 * 心情/抚摸相关同步（PetAction.MOOD）。
	 * Mood/cuddle sync (PetAction.MOOD).
	 *
	 * @param pet 宠物实例 / pet instance
	 * @param subType 心情子类型 / mood subtype
	 * @param shuggleEmotion 抚摸情绪值 / shuggle emotion value
	 */
	public SM_PET(Pet pet, int subType, int shuggleEmotion) {
		this(0, PetAction.MOOD.getActionId(), 0, 0, pet);
		this.shuggleEmotion = shuggleEmotion;
		this.subType = subType;
	}

	/**
	 * 使用宠物公共数据的构造。
	 * Constructor backed by pet common data.
	 *
	 * action id
	 * @param commonData 宠物公共数据 / pet common data
	 */
	public SM_PET(int actionId, PetCommonData commonData) {
		this.actionId = actionId;
		this.commonData = commonData;
	}

	/**
	 * 宠物列表同步构造。
	 * Constructor for a collection of pets (list sync).
	 *
	 * action id
	 * @param pets 宠物公共数据集合 / collection of pet common data
	 */
	public SM_PET(int actionId, Collection<PetCommonData> pets) {
		this.actionId = actionId;
		this.pets = pets;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		PetTemplate petTemplate = null;
		writeH(actionId);
		switch (actionId) {
		case 0:
			writeC(0);
			writeH(pets.size());
			for (PetCommonData petCommonData : pets) {
				petTemplate = DataManager.PET_DATA.getPetTemplate(petCommonData.getPetId());
				int expireTime = petCommonData.getExpireTime();
				writeS(petCommonData.getName());
				writeD(petCommonData.getPetId());
				writeD(petCommonData.getObjectId());
				writeD(petCommonData.getMasterObjectId());
				writeD(0);
				writeD(0);
				writeD((int) petCommonData.getBirthday());
				writeD(expireTime != 0 ? expireTime - (int) (System.currentTimeMillis() / 1000) : 0);
				int specialtyCount = 0;
				if (petTemplate.ContainsFunction(PetFunctionType.WAREHOUSE)) {
					writeH(PetFunctionType.WAREHOUSE.getId());
					specialtyCount++;
				}
				if (petTemplate.ContainsFunction(PetFunctionType.LOOT)) {
					writeH(PetFunctionType.LOOT.getId());
					writeC(0);
					specialtyCount++;
				}
				if (petTemplate.ContainsFunction(PetFunctionType.CHEER)) {
					writeH(PetFunctionType.CHEER.getId());
					short cheer = (short) petTemplate.getPetFunction(PetFunctionType.CHEER).getId();
					writeH(cheer);
					specialtyCount++;
				}
				if (petTemplate.ContainsFunction(PetFunctionType.MERCHAND)) {
					writeH(PetFunctionType.MERCHAND.getId());
					short merchant = (short) petTemplate.getPetFunction(PetFunctionType.MERCHAND).getId();
					writeH(merchant);
					writeC(0x00);
					specialtyCount++;
				}
				if (petTemplate.ContainsFunction(PetFunctionType.DOPING)) {
					writeH(PetFunctionType.DOPING.getId());
					short dopeId = (short) petTemplate.getPetFunction(PetFunctionType.DOPING).getId();
					PetDopingEntry dope = DataManager.PET_DOPING_DATA.getDopingTemplate(dopeId);
					writeD(dope.isUseFood() ? petCommonData.getDopingBag().getFoodItem() : 0);
					writeD(dope.isUseDrink() ? petCommonData.getDopingBag().getDrinkItem() : 0);
					int[] scrollBag = petCommonData.getDopingBag().getScrollsUsed();
					if (scrollBag.length == 0) {
						writeQ(0);
						writeQ(0);
						writeQ(0);
					} else {
						writeD(scrollBag[0]);
						writeD(scrollBag.length > 1 ? scrollBag[1] : 0);
						writeD(scrollBag.length > 2 ? scrollBag[2] : 0);
						writeD(scrollBag.length > 3 ? scrollBag[3] : 0);
						writeD(scrollBag.length > 4 ? scrollBag[4] : 0);
						writeD(scrollBag.length > 5 ? scrollBag[5] : 0);
					}
					specialtyCount++;
				}
				if (petTemplate.ContainsFunction(PetFunctionType.FOOD)) {
					writeH(PetFunctionType.FOOD.getId());
					writeD(petCommonData.getFeedProgress().getDataForPacket());
					writeD((int) petCommonData.getTime() / 1000);
					specialtyCount++;
				}
				if (specialtyCount == 0) {
					writeH(PetFunctionType.NONE.getId());
					writeH(PetFunctionType.NONE.getId());
				} else if (specialtyCount == 1) {
					writeH(PetFunctionType.NONE.getId());
				}
				writeH(PetFunctionType.APPEARANCE.getId());
				writeC(0);
				writeC(0);
				writeC(0);
				writeD(petCommonData.getDecoration());
				writeD(0);
				writeD(0);
			}
			break;
		case 1:
			writeS(commonData.getName());
			writeD(commonData.getPetId());
			writeD(commonData.getObjectId());
			writeD(commonData.getMasterObjectId());
			writeD(0);
			writeD(0);
			writeD(commonData.getBirthday());
			writeD(commonData.getExpireTime() != 0
					? commonData.getExpireTime() - (int) (System.currentTimeMillis() / 1000)
					: 0);
			petTemplate = DataManager.PET_DATA.getPetTemplate(commonData.getPetId());
			int specialtyCount = 0;
			if (petTemplate.ContainsFunction(PetFunctionType.WAREHOUSE)) {
				writeH(PetFunctionType.WAREHOUSE.getId());
				specialtyCount++;
			}
			if (petTemplate.ContainsFunction(PetFunctionType.LOOT)) {
				writeH(PetFunctionType.LOOT.getId());
				writeC(0);
				specialtyCount++;
			}
			if (petTemplate.ContainsFunction(PetFunctionType.DOPING)) {
				writeH(PetFunctionType.DOPING.getId());
				writeQ(0);
				writeQ(0);
				writeQ(0);
				writeQ(0);
				specialtyCount++;
			}
			if (petTemplate.ContainsFunction(PetFunctionType.FOOD)) {
				writeH(PetFunctionType.FOOD.getId());
				writeQ(0);
				specialtyCount++;
			}
			if (specialtyCount == 0) {
				writeH(PetFunctionType.NONE.getId());
				writeH(PetFunctionType.NONE.getId());
			} else if (specialtyCount == 1) {
				writeH(PetFunctionType.NONE.getId());
			}
			writeH(PetFunctionType.APPEARANCE.getId());
			writeC(0);
			writeC(0);
			writeC(0);
			writeD(commonData.getDecoration());
			writeD(0);
			writeD(0);
			break;
		case 2:
			writeD(commonData.getPetId());
			writeD(commonData.getObjectId());
			writeD(0);
			writeD(0);
			break;
		case 3:
			writeS(pet.getName());
			writeD(pet.getPetId());
			writeD(pet.getObjectId());
			if (pet.getPosition().getX() == 0 && pet.getPosition().getY() == 0 && pet.getPosition().getZ() == 0) {
				writeF(pet.getMaster().getX());
				writeF(pet.getMaster().getY());
				writeF(pet.getMaster().getZ());
				writeF(pet.getMaster().getX());
				writeF(pet.getMaster().getY());
				writeF(pet.getMaster().getZ());
				writeC(pet.getMaster().getHeading());
			} else {
				writeF(pet.getPosition().getX());
				writeF(pet.getPosition().getY());
				writeF(pet.getPosition().getZ());
				writeF(pet.getMoveController().getTargetX2());
				writeF(pet.getMoveController().getTargetY2());
				writeF(pet.getMoveController().getTargetZ2());
				writeC(pet.getHeading());
			}
			writeD(pet.getMaster().getObjectId());
			writeC(1);
			writeD(0);
			writeD(pet.getCommonData().getDecoration());
			writeD(0);
			writeD(0);
			break;
		case 4:
			writeD(pet.getObjectId());
			writeC(0x01);
			break;
		case 9:
			writeH(1);
			writeC(1);
			writeC(subType);
			switch (subType) {
			case 1:
				writeD(commonData.getFeedProgress().getDataForPacket());
				writeD(0);
				writeD(itemObjectId);
				writeD(count);
				break;
			case 2:
				writeD(commonData.getFeedProgress().getDataForPacket());
				writeD(0);
				writeD(itemObjectId);
				writeD(count);
				writeC(0);
				break;
			case 3:
			case 4:
			case 5:
				writeD(commonData.getFeedProgress().getDataForPacket());
				writeD((int) commonData.getTime() / 1000);
				break;
			case 6:
				writeD(commonData.getFeedProgress().getDataForPacket());
				writeD(0);
				writeD(itemObjectId);
				writeC(0);
				break;
			case 7:
				writeD(commonData.getFeedProgress().getDataForPacket());
				writeD((int) commonData.getTime() / 1000);
				writeD(itemObjectId);
				writeD(0);
				break;
			case 8:
				writeD(commonData.getFeedProgress().getDataForPacket());
				writeD((int) commonData.getTime() / 1000);
				writeD(itemObjectId);
				writeD(count);
				break;
			}
			break;
		case 10:
			writeD(pet.getObjectId());
			writeS(pet.getName());
			break;
		case 12:
			switch (subType) {
			case 0:
				writeC(subType);
				if (commonData.getLastSentPoints() < commonData.getMoodPoints(true)) {
					writeD(commonData.getMoodPoints(true) - commonData.getLastSentPoints());
				} else {
					writeD(0);
					commonData.setLastSentPoints(commonData.getMoodPoints(true));
				}
				break;
			case 2:
				writeC(subType);
				writeD(0);
				writeD(pet.getCommonData().getMoodPoints(true));
				writeD(shuggleEmotion);
				commonData.setLastSentPoints(pet.getCommonData().getMoodPoints(true));
				commonData.setMoodCdStarted(System.currentTimeMillis());
				break;
			case 3:
				writeC(subType);
				writeD(pet.getPetTemplate().getConditionReward());
				commonData.setGiftCdStarted(System.currentTimeMillis());
				break;
			case 4:
				writeC(subType);
				writeD(commonData.getMoodPoints(true));
				writeD(commonData.getMoodRemainingTime());
				writeD(commonData.getGiftRemainingTime());
				commonData.setLastSentPoints(pet.getCommonData().getMoodPoints(true));
				break;
			}
			break;
		case 13:
			writeC(subType);
			if (subType == 2) {
				writeC(dopeAction);
				switch (dopeAction) {
				case 0:
					writeD(itemObjectId);
					writeD(dopeSlot);
					break;
				case 1:
					writeD(0);
					break;
				case 2:
					break;
				case 3:
					writeD(itemObjectId);
					break;
				}
			} else if (subType == 3) {
				if (lootNpcId > 0) {
					writeC(isActing ? 1 : 2);
					writeD(lootNpcId);
				} else {
					writeC(0);
					writeC(isActing ? 1 : 0);
				}
			} else if (subType == 4) {
				writeC(0);
				writeC(isActing ? 1 : 0);
			} else if (subType == 5) {
				writeC(isActing ? 0 : 1);
			}
			break;
		default:
			break;
		}
	}
}
