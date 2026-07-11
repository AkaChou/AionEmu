package com.aionemu.gameserver.model.legiondominion;

import com.aionemu.gameserver.model.templates.legiondominion.LegionDominionTemplate;

/**
 * 军团领地位置，用于 legiondominion 相关逻辑。
 * Legion Dominion Location for legiondominion logic.
 */

public class LegionDominionLocation {
	protected LegionDominionTemplate template;
	protected LegionDominionRace legionDominionRace = LegionDominionRace.BALAUR;

	public LegionDominionLocation() {
	}

	public LegionDominionLocation(LegionDominionTemplate template) {
		this.template = template;
	}

	/** 获取模板。 / Returns the template. */
	public LegionDominionTemplate getTemplate() {
		return template;
	}

	/** 返回军团领地 ID / Returns the legion dominion id */
	public int getLegionDominionId() {
		return template.getLegionDominionId();
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return template.getName();
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return template.getWorldId();
	}

	/** 获取种族。 / Returns the race. */
	public LegionDominionRace getRace() {
		return this.legionDominionRace;
	}

	/** 设置种族。 / Sets the race. */
	public void setRace(LegionDominionRace legionDominionRace) {
		this.legionDominionRace = legionDominionRace;
	}
}
