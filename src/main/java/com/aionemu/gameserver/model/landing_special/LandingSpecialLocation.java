package com.aionemu.gameserver.model.landing_special;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.landing_special.LandingSpecialTemplate;
import com.aionemu.gameserver.services.abysslandingservice.landingspecialservice.SpecialLanding;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 特殊登陆点位置，用于特殊登陆系统相关逻辑。
 * Landing Special Location for landing special logic.
 */

@Getter
@Setter
@NoArgsConstructor
public class LandingSpecialLocation {
	/** 返回 ID / Returns the id */
	protected int id;
	/** 是否激活。 / Whether Active. */
	protected boolean isActive;
	/** 设置类型。 / Sets the type. */
	protected LandingSpecialStateType type;
	protected LandingSpecialTemplate template;
	/** 返回 active landing special / Returns the active landing special */
	protected SpecialLanding<LandingSpecialLocation> activeLandingSpecial;
	/** 返回玩家集合 / Returns the players */
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回是否已刷新 / Returns the spawned */
	private final List<VisibleObject> spawned = new ArrayList<>();

	public LandingSpecialLocation(LandingSpecialTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置 active landing / Sets the active landing */
	public void setActiveLanding(SpecialLanding<LandingSpecialLocation> landingSpecial) {
		isActive = landingSpecial != null;
		this.activeLandingSpecial = landingSpecial;
	}

	/** 获取模板。 / Returns the template. */
	public final LandingSpecialTemplate getTemplate() {
		return template;
	}
}
