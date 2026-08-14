package com.aionemu.gameserver.model.towerofeternity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.towerofeternity.TowerOfEternityTemplate;
import com.aionemu.gameserver.services.towerofeternityservice.TowerOfEternity;

/**
 * 永恒之塔位置，用于 towerofeternity 相关逻辑。
 * Tower Of Eternity Location for towerofeternity logic.
 */

public class TowerOfEternityLocation {
	protected int id;
	protected boolean isActive;
	protected TowerOfEternityTemplate template;
	protected TowerOfEternity<TowerOfEternityLocation> activeTowerOfEternity;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public TowerOfEternityLocation() {
	}

	public TowerOfEternityLocation(TowerOfEternityTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置当前的永恒之塔 / Sets the active tower of eternity */
	public void setActiveTowerOfEternity(TowerOfEternity<TowerOfEternityLocation> towerOfEternity) {
		isActive = towerOfEternity != null;
		this.activeTowerOfEternity = towerOfEternity;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return template.getWorldId();
	}

	/** 返回当前永恒之塔 / Returns the active tower of eternity */
	public TowerOfEternity<TowerOfEternityLocation> getActiveTowerOfEternity() {
		return activeTowerOfEternity;
	}

	/** 获取模板。 / Returns the template. */
	public final TowerOfEternityTemplate getTemplate() {
		return template;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回是否已刷新 / Returns the spawned */
	public List<VisibleObject> getSpawned() {
		return spawned;
	}

	/** 返回玩家集合 / Returns the players */
	public Map<Integer, Player> getPlayers() {
		return players;
	}
}
