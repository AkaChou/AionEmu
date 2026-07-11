package com.aionemu.gameserver.model.beritra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.beritra.BeritraTemplate;
import com.aionemu.gameserver.services.beritraservice.BeritraInvasion;

/**
 * 贝里特拉位置模型。
 * Beritra Location model.
 *
 * @author Rinzler (Encom)
 */

public class BeritraLocation {
	protected int id;
	protected boolean isActive;
	protected BeritraTemplate template;
	protected BeritraInvasion<BeritraLocation> activeBeritra;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public BeritraLocation() {
	}

	public BeritraLocation(BeritraTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active beritra / Sets the active beritra */
	public void setActiveBeritra(BeritraInvasion<BeritraLocation> beritra) {
		isActive = beritra != null;
		this.activeBeritra = beritra;
	}

	/** 返回 active beritra / Returns the active beritra */
	public BeritraInvasion<BeritraLocation> getActiveBeritra() {
		return activeBeritra;
	}

	/** 获取模板。 / Returns the template. */
	public final BeritraTemplate getTemplate() {
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
