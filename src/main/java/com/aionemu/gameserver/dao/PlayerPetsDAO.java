package com.aionemu.gameserver.dao;

import java.util.List;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.pet.PetDopingBag;

/**
 * 玩家宠物数据访问对象。
 * Player pets data access object.
 *
 * @author Xitanium, Kamui, Rolandas
 */
public abstract class PlayerPetsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public final String getClassName() {
		return PlayerPetsDAO.class.getName();
	}

	/**
	 * 插入一只玩家宠物。
	 * Inserts a player pet.
	 *
	 * @param petCommonData 宠物公共数据 / pet common data
	 */
	public abstract void insertPlayerPet(PetCommonData petCommonData);

	/**
	 * 移除玩家的指定宠物。
	 * Removes the given pet from the player.
	 *
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 */
	public abstract void removePlayerPet(Player player, int petId);

	/**
	 * 更新宠物名称。
	 * Updates the pet name.
	 *
	 * @param petCommonData 宠物公共数据 / pet common data
	 */
	public abstract void updatePetName(PetCommonData petCommonData);

	/**
	 * 获取玩家全部宠物。
	 * Returns all pets owned by the player.
	 *
	 * @param player 玩家 / player
	 * @return 宠物列表 / list of pets
	 */
	public abstract List<PetCommonData> getPlayerPets(Player player);

	/**
	 * 设置宠物相关时间。
	 * Sets a pet-related time value.
	 *
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 * @param time 时间 / time
	 */
	public abstract void setTime(Player player, int petId, long time);

	/**
	 * 保存宠物喂养状态。
	 * Saves the pet feed status.
	 *
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 * @param hungryLevel 饥饿度 / hungry level
	 * @param feedProgress 喂食进度 / feed progress
	 * @param reuseTime 冷却时间 / reuse time
	 */
	public abstract void saveFeedStatus(Player player, int petId, int hungryLevel, int feedProgress, long reuseTime);

	/**
	 * 保存宠物心情数据。
	 * Saves pet mood data.
	 *
	 * @param petCommonData 宠物公共数据 / pet common data
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean savePetMoodData(PetCommonData petCommonData);

	/**
	 * 保存宠物增益包。
	 * Saves the pet doping bag.
	 *
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 * @param bag 补给包 / doping bag
	 */
	public abstract void saveDopingBag(Player player, int petId, PetDopingBag bag);
}
