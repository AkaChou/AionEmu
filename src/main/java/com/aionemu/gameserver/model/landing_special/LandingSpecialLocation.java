package com.aionemu.gameserver.model.landing_special;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.landing_special.LandingSpecialTemplate;
import com.aionemu.gameserver.services.abysslandingservice.landingspecialservice.SpecialLanding;

/**
 * 登陆 Special 位置，用于登陆 special 相关逻辑。
 * Landing Special Location for landing special logic.
 */

public class LandingSpecialLocation {
	protected int id;
	protected boolean isActive;
	protected LandingSpecialStateType type;
	protected LandingSpecialTemplate template;
	protected SpecialLanding<LandingSpecialLocation> activeLandingSpecial;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public LandingSpecialLocation() {
	}

	public LandingSpecialLocation(LandingSpecialTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active landing / Sets the active landing */
	public void setActiveLanding(SpecialLanding<LandingSpecialLocation> landingSpecial) {
		isActive = landingSpecial != null;
		this.activeLandingSpecial = landingSpecial;
	}

	/** 返回 active landing special / Returns the active landing special */
	public SpecialLanding<LandingSpecialLocation> getActiveLandingSpecial() {
		return activeLandingSpecial;
	}

	/** 获取模板。 / Returns the template. */
	public final LandingSpecialTemplate getTemplate() {
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

	/** 设置类型。 / Sets the type. */
	public void setType(LandingSpecialStateType type) {
		this.type = type;
	}

	/** 获取类型。 / Returns the type. */
	public LandingSpecialStateType getType() {
		return this.type;
	}
}
