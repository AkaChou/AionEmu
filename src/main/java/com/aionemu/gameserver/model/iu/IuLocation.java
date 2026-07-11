package com.aionemu.gameserver.model.iu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.iu.IuTemplate;
import com.aionemu.gameserver.services.iuservice.Iu;

/**
 * IU 活动位置模型。
 * Iu Location model.
 *
 * @author Rinzler (Encom)
 */

public class IuLocation {
	protected int id;
	protected boolean isActive;
	protected IuTemplate template;
	protected Iu<IuLocation> activeIu;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public IuLocation() {
	}

	public IuLocation(IuTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active iu / Sets the active iu */
	public void setActiveIu(Iu<IuLocation> iu) {
		isActive = iu != null;
		this.activeIu = iu;
	}

	/** 返回 active iu / Returns the active iu */
	public Iu<IuLocation> getActiveIu() {
		return activeIu;
	}

	/** 获取模板。 / Returns the template. */
	public final IuTemplate getTemplate() {
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
