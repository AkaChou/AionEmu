package com.aionemu.gameserver.model.team.legion;

/**
 * 军团 Territory，用于团队相关逻辑。
 * Legion Territory for team logic.
 */

public class LegionTerritory {
	int territoryId = 0;
	int legionId = 0;
	String legionName = "";

	public LegionTerritory(int id) {
		this.territoryId = id;
	}

	public LegionTerritory() {
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return territoryId;
	}

	/** 设置 territory id / Sets the territory id */
	public void setTerritoryId(int terretoryId) {
		this.territoryId = terretoryId;
	}

	/** 返回军团 ID / Returns the legion id */
	public int getLegionId() {
		return legionId;
	}

	/** 设置军团 ID / Sets the legion id */
	public void setLegionId(int legionId) {
		this.legionId = legionId;
	}

	/** 获取军团名称。 / Returns the legion name. */
	public String getLegionName() {
		return legionName;
	}

	/** 设置军团名称。 / Sets the legion name. */
	public void setLegionName(String legionName) {
		this.legionName = legionName;
	}
}
