package com.aionemu.gameserver.model.dynamicrift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.dynamicrift.DynamicRiftTemplate;
import com.aionemu.gameserver.services.dynamicriftservice.DynamicRift;

/**
 * 动态裂隙位置模型。
 * Dynamic Rift Location model.
 *
 * @author Rinzler (Encom)
 */

public class DynamicRiftLocation {
	protected int id;
	protected boolean isActive;
	protected DynamicRiftTemplate template;
	protected DynamicRift<DynamicRiftLocation> activeDynamicRift;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public DynamicRiftLocation() {
	}

	public DynamicRiftLocation(DynamicRiftTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置激活的动态裂隙 / Sets the active dynamic rift */
	public void setActiveDynamicRift(DynamicRift<DynamicRiftLocation> dynamicRift) {
		isActive = dynamicRift != null;
		this.activeDynamicRift = dynamicRift;
	}

	/** 返回激活的动态裂隙 / Returns the active dynamic rift */
	public DynamicRift<DynamicRiftLocation> getActiveDynamicRift() {
		return activeDynamicRift;
	}

	/** 获取模板。 / Returns the template. */
	public final DynamicRiftTemplate getTemplate() {
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
