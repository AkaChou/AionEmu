package com.aionemu.gameserver.model.assemblednpc;

import com.aionemu.gameserver.model.templates.assemblednpc.AssembledNpcTemplate.AssembledNpcPartTemplate;

import lombok.Getter;

/**
 * 组装 NPCPart，用于 assemblednpc 相关逻辑。
 * Assembled Npc Part for assemblednpc logic.
 *
 * @author xTz
 */
public class AssembledNpcPart {

	@Getter
	private Integer object;
	private AssembledNpcPartTemplate template;

	public AssembledNpcPart(Integer object, AssembledNpcPartTemplate template) {
		this.object = object;
		this.template = template;
	}

	/** 返回 assembled npc part template / Returns the assembled npc part template */
	public AssembledNpcPartTemplate getAssembledNpcPartTemplate() {
		return template;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return template.getNpcId();
	}

	/** 返回 entity id / Returns the entity id */
	public int getEntityId() {
		return template.getEntityId();
	}
}
