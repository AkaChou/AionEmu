package com.aionemu.gameserver.model.nightmarecircus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.nightmarecircus.NightmareCircusTemplate;
import com.aionemu.gameserver.services.nightmarecircusservice.CircusInstance;

/**
 * 梦魇马戏团位置，用于 nightmarecircus 相关逻辑。
 * Nightmare Circus Location for nightmarecircus logic.
 *
 * @author Rinzler (Encom)
 */

public class NightmareCircusLocation {
	protected int id;
	protected boolean isActive;
	protected NightmareCircusTemplate template;
	protected CircusInstance<NightmareCircusLocation> activeNightmareCircus;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public NightmareCircusLocation() {
	}

	public NightmareCircusLocation(NightmareCircusTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置激活的梦魇马戏团实例。 / Sets the active nightmare circus. */
	public void setActiveNightmareCircus(CircusInstance<NightmareCircusLocation> nightmareCircus) {
		isActive = nightmareCircus != null;
		this.activeNightmareCircus = nightmareCircus;
	}

	/** 返回激活的梦魇马戏团实例。 / Returns the active nightmare circus. */
	public CircusInstance<NightmareCircusLocation> getActiveNightmareCircus() {
		return activeNightmareCircus;
	}

	/** 获取模板。 / Returns the template. */
	public final NightmareCircusTemplate getTemplate() {
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
