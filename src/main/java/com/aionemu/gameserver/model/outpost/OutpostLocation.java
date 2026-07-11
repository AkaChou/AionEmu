package com.aionemu.gameserver.model.outpost;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.outpost.OutpostTemplate;

/**
 * 前哨位置模型。
 * Outpost Location model.
 */

public class OutpostLocation {
	protected OutpostTemplate template;
	protected Race race = Race.NPC;

	public OutpostLocation() {
	}

	public OutpostLocation(OutpostTemplate template) {
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

	/** 返回 artifact id / Returns the artifact id */
	public int getArtifactId() {
		return template.getArtifactId();
	}
}
