package com.aionemu.gameserver.model.base;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.base.BaseTemplate;

/**
 * 基础位置模型。
 * Base Location model.
 *
 * @author Rinzler
 */

public class BaseLocation {
	protected BaseTemplate template;
	protected Race race = Race.NPC;

	public BaseLocation() {
	}

	public BaseLocation(BaseTemplate template) {
		this.template = template;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return template.getId();
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return template.getWorldId();
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return template.getName();
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 设置种族。 / Sets the race. */
	public void setRace(Race race) {
		this.race = race;
	}
}
