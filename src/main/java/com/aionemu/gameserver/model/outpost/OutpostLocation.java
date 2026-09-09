package com.aionemu.gameserver.model.outpost;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.outpost.OutpostTemplate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 前哨位置模型。
 * Outpost Location model.
 */
@NoArgsConstructor
public class OutpostLocation {
	protected OutpostTemplate template;
	/** 获取种族。 / Returns the race. */
	@Getter
	@Setter
	protected Race race = Race.NPC;

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

	/** 返回 artifact id / Returns the artifact id */
	public int getArtifactId() {
		return template.getArtifactId();
	}
}
