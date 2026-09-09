package com.aionemu.gameserver.model.base;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.base.BaseTemplate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 基础位置模型。
 * Base Location model.
 *
 * @author Rinzler
 */

@NoArgsConstructor
public class BaseLocation {
	protected BaseTemplate template;
	/** 获取种族。 / Returns the race. */
	@Getter
	@Setter
	protected Race race = Race.NPC;

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
}
