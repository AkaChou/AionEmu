package com.aionemu.gameserver.model.moltenus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.moltenus.MoltenusTemplate;
import com.aionemu.gameserver.services.moltenusservice.MoltenusFight;

/**
 * 熔岩魔位置模型。
 * Moltenus Location model.
 *
 * @author Rinzler (Encom)
 */

public class MoltenusLocation {
	protected int id;
	protected boolean isActive;
	protected MoltenusTemplate template;
	protected MoltenusFight<MoltenusLocation> activeMoltenus;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public MoltenusLocation() {
	}

	public MoltenusLocation(MoltenusTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active moltenus / Sets the active moltenus */
	public void setActiveMoltenus(MoltenusFight<MoltenusLocation> moltenus) {
		isActive = moltenus != null;
		this.activeMoltenus = moltenus;
	}

	/** 返回 active moltenus / Returns the active moltenus */
	public MoltenusFight<MoltenusLocation> getActiveMoltenus() {
		return activeMoltenus;
	}

	/** 获取模板。 / Returns the template. */
	public final MoltenusTemplate getTemplate() {
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
