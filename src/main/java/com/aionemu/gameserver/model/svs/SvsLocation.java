package com.aionemu.gameserver.model.svs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.svs.SvsTemplate;
import com.aionemu.gameserver.services.svsservice.Panesterra;

/**
 * 势力战位置模型。
 * Svs Location model.
 *
 * @author Rinzler (Encom)
 */

public class SvsLocation {
	protected int id;
	protected boolean isActive;
	protected SvsTemplate template;
	protected Panesterra<SvsLocation> activeSvs;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public SvsLocation() {
	}

	public SvsLocation(SvsTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active svs / Sets the active svs */
	public void setActiveSvs(Panesterra<SvsLocation> svs) {
		isActive = svs != null;
		this.activeSvs = svs;
	}

	/** 返回当前 svs / Returns the active svs */
	public Panesterra<SvsLocation> getActiveSvs() {
		return activeSvs;
	}

	/** 获取模板。 / Returns the template. */
	public final SvsTemplate getTemplate() {
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
