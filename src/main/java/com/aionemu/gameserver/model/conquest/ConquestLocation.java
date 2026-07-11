package com.aionemu.gameserver.model.conquest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.conquest.ConquestTemplate;
import com.aionemu.gameserver.services.conquestservice.ConquestOffering;

/**
 * 征服位置模型。
 * Conquest Location model.
 *
 * @author Rinzler (Encom)
 */

public class ConquestLocation {
	protected int id;
	protected boolean isActive;
	protected ConquestTemplate template;
	protected ConquestOffering<ConquestLocation> activeConquest;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public ConquestLocation() {
	}

	public ConquestLocation(ConquestTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active conquest / Sets the active conquest */
	public void setActiveConquest(ConquestOffering<ConquestLocation> conquest) {
		isActive = conquest != null;
		this.activeConquest = conquest;
	}

	/** 返回当前征服 / Returns the active conquest */
	public ConquestOffering<ConquestLocation> getActiveConquest() {
		return activeConquest;
	}

	/** 获取模板。 / Returns the template. */
	public final ConquestTemplate getTemplate() {
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
