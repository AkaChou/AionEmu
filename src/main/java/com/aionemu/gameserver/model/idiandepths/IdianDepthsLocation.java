package com.aionemu.gameserver.model.idiandepths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.idiandepths.IdianDepthsTemplate;
import com.aionemu.gameserver.services.idiandepthsservice.IdianDepths;

/**
 * 伊迪安深渊位置，用于 idiandepths 相关逻辑。
 * Idian Depths Location for idiandepths logic.
 *
 * @author Rinzler (Encom)
 */

public class IdianDepthsLocation {
	protected int id;
	protected boolean isActive;
	protected IdianDepthsTemplate template;
	protected IdianDepths<IdianDepthsLocation> activeIdianDepths;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public IdianDepthsLocation() {
	}

	public IdianDepthsLocation(IdianDepthsTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置当前的伊迪安深渊 / Sets the active idian depths */
	public void setActiveIdianDepths(IdianDepths<IdianDepthsLocation> idianDepths) {
		isActive = idianDepths != null;
		this.activeIdianDepths = idianDepths;
	}

	/** 返回当前伊迪安深渊 / Returns the active idian depths */
	public IdianDepths<IdianDepthsLocation> getActiveIdianDepths() {
		return activeIdianDepths;
	}

	/** 获取模板。 / Returns the template. */
	public final IdianDepthsTemplate getTemplate() {
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
