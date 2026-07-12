package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerPetsDAO;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 宠物列表。
 * Pet List game object.
 *
 * @author ATracer
 */
public class PetList {

	private final Player player;
	private int lastUsedPetId;

	private Map<Integer, PetCommonData> pets = new LinkedHashMap<Integer, PetCommonData>();

	PetList(Player player) {
		this.player = player;
		loadPets();
	}

	/**
	 * 从数据库加载宠物。 / Load pets from the database.
	 */
	public void loadPets() {
		List<PetCommonData> playerPets = DAOManager.getDAO(PlayerPetsDAO.class).getPlayerPets(player);
		PetCommonData lastUsedPet = null;
		for (PetCommonData pet : playerPets) {
			if (pet.getExpireTime() > 0) {
				GameTaskManagerServices.expireTimerTask().addTask(pet, player);
			}
			pets.put(pet.getPetId(), pet);
			if (lastUsedPet == null || pet.getDespawnTime().after(lastUsedPet.getDespawnTime())) {
				lastUsedPet = pet;
			}
		}

		if (lastUsedPet != null) {
			lastUsedPetId = lastUsedPet.getPetId();
		}
	}

	/** 返回 pets / Returns the pets */
	public Collection<PetCommonData> getPets() {
		return pets.values();
	}

	/**
	 * @param petId
	 * @return
	 */
	public PetCommonData getPet(int petId) {
		return pets.get(petId);
	}

	/** 返回 last used pet / Returns the last used pet */
	public PetCommonData getLastUsedPet() {
		return getPet(lastUsedPetId);
	}

	/** 设置 last used pet id / Sets the last used pet id */
	public void setLastUsedPetId(int lastUsedPetId) {
		this.lastUsedPetId = lastUsedPetId;
	}

	/**
	 * @param player
	 * @param petId
	 * @param decorationId
	 * @param name
	 * @return
	 */
	public PetCommonData addPet(Player player, int petId, int decorationId, String name, int expireTime) {
		return addPet(player, petId, decorationId, System.currentTimeMillis(), name, expireTime);
	}

	/** 添加宠物。 / Adds pet. */
	public PetCommonData addPet(Player player, int petId, int decorationId, long birthday, String name,
			int expireTime) {
		PetCommonData petCommonData = new PetCommonData(petId, player.getObjectId(), expireTime);
		petCommonData.setDecoration(decorationId);
		petCommonData.setName(name);
		petCommonData.setBirthday(new Timestamp(birthday));
		petCommonData.setDespawnTime(new Timestamp(System.currentTimeMillis()));
		DAOManager.getDAO(PlayerPetsDAO.class).insertPlayerPet(petCommonData);
		pets.put(petId, petCommonData);
		return petCommonData;
	}

	/**
	 * @param petId
	 * @return
	 */
	public boolean hasPet(int petId) {
		return pets.containsKey(petId);
	}

	/**
	 * @param petId
	 */
	public void deletePet(int petId) {
		if (hasPet(petId)) {
			pets.remove(petId);
			DAOManager.getDAO(PlayerPetsDAO.class).removePlayerPet(player, petId);
		}
	}
}
