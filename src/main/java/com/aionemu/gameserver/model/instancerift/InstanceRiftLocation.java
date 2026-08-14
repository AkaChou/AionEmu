package com.aionemu.gameserver.model.instancerift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.instancerift.InstanceRiftTemplate;
import com.aionemu.gameserver.services.instanceriftservice.RiftInstance;

/**
 * 副本裂隙位置模型。
 * Instance Rift Location model.
 *
 * @author Rinzler (Encom)
 */

public class InstanceRiftLocation {
	protected int id;
	protected boolean isActive;
	protected InstanceRiftTemplate template;
	protected RiftInstance<InstanceRiftLocation> activeInstanceRift;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public InstanceRiftLocation() {
	}

	public InstanceRiftLocation(InstanceRiftTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active instance rift / Sets the active instance rift */
	public void setActiveInstanceRift(RiftInstance<InstanceRiftLocation> instanceRift) {
		isActive = instanceRift != null;
		this.activeInstanceRift = instanceRift;
	}

	/** 返回 active instance rift / Returns the active instance rift */
	public RiftInstance<InstanceRiftLocation> getActiveInstanceRift() {
		return activeInstanceRift;
	}

	/** 获取模板。 / Returns the template. */
	public final InstanceRiftTemplate getTemplate() {
		return template;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回已生成的对象列表 / Returns the spawned objects */
	public List<VisibleObject> getSpawned() {
		return spawned;
	}

	/** 返回玩家集合 / Returns the players */
	public Map<Integer, Player> getPlayers() {
		return players;
	}
}
