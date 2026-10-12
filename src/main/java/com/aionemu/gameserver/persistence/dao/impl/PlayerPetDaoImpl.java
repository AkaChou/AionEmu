package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.pet.PetDopingBag;
import com.aionemu.gameserver.persistence.dao.PlayerPetDao;
import com.aionemu.gameserver.persistence.entity.PlayerPetPo;
import com.aionemu.gameserver.persistence.mapper.PlayerPetMapper;
import com.aionemu.gameserver.services.toypet.PetHungryLevel;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家宠物数据访问实现。
 * Data access implementation for player pets.
 */
@Slf4j
@Service
@Lazy
public class PlayerPetDaoImpl extends ServiceImpl<PlayerPetMapper, PlayerPetPo> implements PlayerPetDao {

	/**
	 * 插入玩家宠物记录。
	 * Inserts a player pet record.
	 * @param petCommonData 宠物通用数据 / pet common data
	 */
	@Override
	public void insertPlayerPet(PetCommonData petCommonData) {
		try {
			PlayerPetPo po = new PlayerPetPo();
			po.setPlayerId(petCommonData.getMasterObjectId());
			po.setPetId(petCommonData.getPetId());
			po.setDecoration(petCommonData.getDecoration());
			po.setName(petCommonData.getName());
			po.setDespawnTime(petCommonData.getDespawnTime());
			po.setExpireTime(petCommonData.getExpireTime());
			// 仅写入上述列，其余列沿用库内默认值（等价原 INSERT 的列清单）/ other columns keep their DB defaults
			mapper.insertSelective(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.470c078aecdc", petCommonData.getPetId(), petCommonData.getName()), e);
		}
	}

	/**
	 * 删除玩家宠物记录。
	 * Removes a player pet record.
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 */
	@Override
	public void removePlayerPet(Player player, int petId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerPetPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerPetPo::getPetId).eq(petId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c0204a470d98", petId), e);
		}
	}

	/**
	 * 更新宠物名称。
	 * Updates the pet name.
	 * @param petCommonData 宠物通用数据 / pet common data
	 */
	@Override
	public void updatePetName(PetCommonData petCommonData) {
		try {
			UpdateChain.of(PlayerPetPo.class)
				.set(PlayerPetPo::getName, petCommonData.getName())
				.where(PlayerPetPo::getPlayerId).eq(petCommonData.getMasterObjectId())
				.and(PlayerPetPo::getPetId).eq(petCommonData.getPetId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8faeec608d56", petCommonData.getPetId()), e);
		}
	}

	/**
	 * 加载玩家全部宠物数据。
	 * Loads all pets for the player.
	 * @param player 玩家 / player
	 * @return 宠物数据列表 / list of pet common data
	 */
	@Override
	public List<PetCommonData> getPlayerPets(Player player) {
		List<PetCommonData> pets = new ArrayList<>();

		try {
			List<PlayerPetPo> rows = mapper.selectListByQuery(
				QueryWrapper.create().where(PlayerPetPo::getPlayerId).eq(player.getObjectId()));
			for (PlayerPetPo po : rows) {
				pets.add(toPetCommonData(po, player));
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.2e256611ad4a", player.getObjectId()), e);
		}
		return pets;
	}

	/**
	 * 设置宠物复用时间。
	 * Sets the pet reuse time.
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 * @param time 复用时间 / reuse time
	 */
	@Override
	public void setTime(Player player, int petId, long time) {
		try {
			UpdateChain.of(PlayerPetPo.class)
				.set(PlayerPetPo::getReuseTime, time)
				.where(PlayerPetPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerPetPo::getPetId).eq(petId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8faeec608d56", petId), e);
		}
	}

	/**
	 * 保存宠物喂养状态。
	 * Saves the pet feed status.
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 * @param hungryLevel hungry level
	 * @param feedProgress feed progress
	 * @param reuseTime 复用时间 / reuse time
	 */
	@Override
	public void saveFeedStatus(Player player, int petId, int hungryLevel, int feedProgress, long reuseTime) {
		try {
			UpdateChain.of(PlayerPetPo.class)
				.set(PlayerPetPo::getHungryLevel, hungryLevel)
				.set(PlayerPetPo::getFeedProgress, feedProgress)
				.set(PlayerPetPo::getReuseTime, reuseTime)
				.where(PlayerPetPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerPetPo::getPetId).eq(petId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8faeec608d56", petId), e);
		}
	}

	/**
	 * 保存宠物心情相关数据。
	 * Saves pet mood related data.
	 * @param petCommonData 宠物通用数据 / pet common data
	 */
	@Override
	public void savePetMoodData(PetCommonData petCommonData) {
		try {
			UpdateChain.of(PlayerPetPo.class)
				.set(PlayerPetPo::getMoodStarted, petCommonData.getMoodStartTime())
				.set(PlayerPetPo::getCounter, petCommonData.getShuggleCounter())
				.set(PlayerPetPo::getMoodCdStarted, petCommonData.getMoodCdStarted())
				.set(PlayerPetPo::getGiftCdStarted, petCommonData.getGiftCdStarted())
				.set(PlayerPetPo::getDespawnTime, petCommonData.getDespawnTime())
				.where(PlayerPetPo::getPlayerId).eq(petCommonData.getMasterObjectId())
				.and(PlayerPetPo::getPetId).eq(petCommonData.getPetId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.44d7671006ae", petCommonData.getPetId()), e);
		}
	}

	/**
	 * 保存宠物增益包（食品/饮料/卷轴）。
	 * Saves the pet doping bag (food/drink/scrolls).
	 * @param player 玩家 / player
	 * @param petId 宠物 ID / pet id
	 * @param bag doping bag
	 */
	@Override
	public void saveDopingBag(Player player, int petId, PetDopingBag bag) {
		try {
			StringBuilder itemIds = new StringBuilder();
			itemIds.append(bag.getFoodItem()).append(",").append(bag.getDrinkItem());

			for (int itemId : bag.getScrollsUsed()) {
				itemIds.append(",").append(itemId);
			}

			UpdateChain.of(PlayerPetPo.class)
				.set(PlayerPetPo::getDopings, itemIds.toString())
				.where(PlayerPetPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerPetPo::getPetId).eq(petId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b3af66365ffc", petId), e);
		}
	}

	/**
	 * 将持久化实体还原为宠物模型（字段与读取顺序与原 ResultSet 映射一致）。
	 * Restores the pet model from the persistence entity (same field order as the original ResultSet mapping).
	 * @param po 宠物持久化实体 / pet persistence entity
	 * @param player 玩家 / player
	 * @return 宠物数据 / pet common data
	 */
	private static PetCommonData toPetCommonData(PlayerPetPo po, Player player) {
		PetCommonData petCommonData = new PetCommonData(po.getPetId(), player.getObjectId(), po.getExpireTime());

		petCommonData.setName(po.getName());
		petCommonData.setDecoration(po.getDecoration());

		if (petCommonData.getFeedProgress() != null) {
			petCommonData.getFeedProgress().setHungryLevel(PetHungryLevel.fromId(po.getHungryLevel()));
			petCommonData.getFeedProgress().setData(po.getFeedProgress());
			petCommonData.setCurentTime(po.getReuseTime());
		}

		if (petCommonData.getDopingBag() != null) {
			String dopings = po.getDopings();
			if (dopings != null) {
				String[] ids = dopings.split(",");
				for (int i = 0; i < ids.length; i++) {
					if (!ids[i].isEmpty()) {
						petCommonData.getDopingBag().setItem(Integer.parseInt(ids[i]), i);
					}
				}
			}
		}

		petCommonData.setBirthday(po.getBirthday());

		if (petCommonData.getTime() != 0) {
			petCommonData.setIsFeedingTime(false);
			petCommonData.setReFoodTime(petCommonData.getTime());
		}

		petCommonData.setStartMoodTime(po.getMoodStarted());
		petCommonData.setShuggleCounter(po.getCounter());
		petCommonData.setMoodCdStarted(po.getMoodCdStarted());
		petCommonData.setGiftCdStarted(po.getGiftCdStarted());

		Timestamp ts = po.getDespawnTime();
		if (ts == null) {
			ts = new Timestamp(System.currentTimeMillis());
		}
		petCommonData.setDespawnTime(ts);

		return petCommonData;
	}
}
