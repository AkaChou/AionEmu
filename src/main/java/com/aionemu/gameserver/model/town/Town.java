package com.aionemu.gameserver.model.town;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TOWNS_LIST;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 城镇模型。
 * Town model.
 */

public class Town {
	private int id;
	private int nameId;
	private int level;
	private int points;
	private Timestamp levelUpDate;
	private Race race;
	private PersistentState persistentState;
	private List<Npc> spawnedNpcs;

	public Town(int id, int level, int points, Race race, Timestamp levelUpDate) {
		this.id = id;
		this.level = level;
		this.points = points;
		this.levelUpDate = levelUpDate;
		this.race = race;
		this.persistentState = PersistentState.UPDATED;
		this.spawnedNpcs = new ArrayList<Npc>();
		spawnNewObjects();
		updateTownToLevel();
	}

	public Town(int id, Race race) {
		this(id, 1, 0, race, new Timestamp(60000));
		this.persistentState = PersistentState.NEW;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回名称 ID / Returns the name id */
	public int getNameId() {
		return nameId;
	}

	/** 获取等级。 / Returns the level. */
	public int getLevel() {
		return level;
	}

	/** 获取点。 / Returns the points. */
	public int getPoints() {
		return points;
	}

	/** 增加点。 / Increase points. */
	public synchronized void increasePoints(int amount) {
		switch (this.level) {
		case 1:
			if (this.points + amount >= 1000)
				increaseLevel();
			break;
		case 2:
			if (this.points + amount >= 2000)
				increaseLevel();
			break;
		case 3:
			if (this.points + amount >= 3000)
				increaseLevel();
			break;
		case 4:
			if (this.points + amount >= 4000)
				increaseLevel();
			break;
		}
		this.points += amount;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	private void increaseLevel() {
		this.level++;
		this.levelUpDate.setTime(System.currentTimeMillis());
		broadcastUpdate();
		despawnOldObjects();
		spawnNewObjects();
		updateTownToLevel();
	}

	private void updateTownToLevel() {
		GameWorldServices.geoService().updateTownToLevel(DataManager.TOWN_SPAWNS_DATA.getWorldIdForTown(id), id, level);
	}

	private void broadcastUpdate() {
		Map<Integer, Town> data = new HashMap<Integer, Town>(1);
		data.put(this.id, this);
		final SM_TOWNS_LIST packet = new SM_TOWNS_LIST(data);
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			/** 访问 / visit. */
			@Override
			public void visit(Player player) {
				if (player.getRace() == race) {
					PacketSendUtility.sendPacket(player, packet);
				}
			}
		});
	}

	private void spawnNewObjects() {
		List<Spawn> newSpawns = DataManager.TOWN_SPAWNS_DATA.getSpawns(this.id, this.level);
		int worldId = DataManager.TOWN_SPAWNS_DATA.getWorldIdForTown(this.id);
		for (Spawn spawn : newSpawns) {
			for (SpawnSpotTemplate sst : spawn.getSpawnSpotTemplates()) {
				SpawnTemplate spawnTemplate = SpawnEngine.addNewSpawn(worldId, spawn.getNpcId(), sst.getX(), sst.getY(),
						sst.getZ(), sst.getHeading(), spawn.getRespawnTime());
				spawnTemplate.setEntityId(sst.getEntityId());
				spawnTemplate.setRandomWalk(0);
				VisibleObject object = SpawnEngine.spawnObject(spawnTemplate, 1);
				if (object instanceof Npc) {
					((Npc) object).setTownId(this.id);
					spawnedNpcs.add((Npc) object);
				}
			}
		}
	}

	private void despawnOldObjects() {
		for (Npc npc : spawnedNpcs)
			npc.getController().delete();
		spawnedNpcs.clear();
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return this.race;
	}

	/** 返回等级日期 / Returns the level up date */
	public Timestamp getLevelUpDate() {
		return levelUpDate;
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState state) {
		if (this.persistentState == PersistentState.NEW && state == PersistentState.UPDATE_REQUIRED) {
			return;
		} else {
			this.persistentState = state;
		}
	}
}
