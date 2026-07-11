package com.aionemu.gameserver.model.rvr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.rvr.RvrTemplate;
import com.aionemu.gameserver.services.rvrservice.Rvrlf3df3;

/**
 * 阵营战位置模型。
 * Rvr Location model.
 *
 * @author Rinzler (Encom)
 */

public class RvrLocation {
	protected int id;
	protected boolean isActive;
	protected RvrTemplate template;
	protected Rvrlf3df3<RvrLocation> activeRvr;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public RvrLocation() {
	}

	public RvrLocation(RvrTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active rvr / Sets the active rvr */
	public void setActiveRvr(Rvrlf3df3<RvrLocation> rvr) {
		isActive = rvr != null;
		this.activeRvr = rvr;
	}

	/** 返回 active rvr / Returns the active rvr */
	public Rvrlf3df3<RvrLocation> getActiveRvr() {
		return activeRvr;
	}

	/** 获取模板。 / Returns the template. */
	public final RvrTemplate getTemplate() {
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
