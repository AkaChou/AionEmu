package com.aionemu.gameserver.model.team.legion;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 军团 Territory，用于团队相关逻辑。
 * Legion Territory for team logic.
 */

@Getter
@Setter
@NoArgsConstructor
public class LegionTerritory {
	/** 设置领地 ID。 / Sets the territory id. */
	int territoryId = 0;
	/** 返回军团 ID。 / Returns the legion id. */
	int legionId = 0;
	/** 获取军团名称。 / Returns the legion name. */
	String legionName = "";

	public LegionTerritory(int id) {
		this.territoryId = id;
	}

	/** 返回 ID。 / Returns the id. */
	public int getId() {
		return territoryId;
	}
}
