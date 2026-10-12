package com.aionemu.gameserver.persistence.dao;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.pet.PetDopingBag;
import com.aionemu.gameserver.persistence.entity.PlayerPetPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家宠物数据访问对象。
 * Player pets data access object.
 */
public interface PlayerPetDao extends IService<PlayerPetPo> {

	/**
	 * 插入一只玩家宠物。
	 * Inserts a player pet.
	 * @param petCommonData 宠物公共数据 / pet common data
	 */
	void insertPlayerPet(PetCommonData petCommonData);

	/**
	 * 移除玩家的指定宠物。
	 * Removes the given pet from the player.
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 */
	void removePlayerPet(Player player, int petId);

	/**
	 * 更新宠物名称。
	 * Updates the pet name.
	 * @param petCommonData 宠物公共数据 / pet common data
	 */
	void updatePetName(PetCommonData petCommonData);

	/**
	 * 获取玩家全部宠物。
	 * Returns all pets owned by the player.
	 * @param player 玩家 / player
	 * @return 宠物列表 / list of pets
	 */
	List<PetCommonData> getPlayerPets(Player player);

	/**
	 * 设置宠物相关时间。
	 * Sets a pet-related time value.
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 * @param time 时间 / time
	 */
	void setTime(Player player, int petId, long time);

	/**
	 * 保存宠物喂养状态。
	 * Saves the pet feed status.
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 * @param hungryLevel 饥饿度 / hungry level
	 * @param feedProgress 喂食进度 / feed progress
	 * @param reuseTime 冷却时间 / reuse time
	 */
	void saveFeedStatus(Player player, int petId, int hungryLevel, int feedProgress, long reuseTime);

	/**
	 * 保存宠物心情数据。
	 * Saves pet mood data.
	 * @param petCommonData 宠物公共数据 / pet common data
	 */
	void savePetMoodData(PetCommonData petCommonData);

	/**
	 * 保存宠物增益包。
	 * Saves the pet doping bag.
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 * @param bag 补给包 / doping bag
	 */
	void saveDopingBag(Player player, int petId, PetDopingBag bag);
}
