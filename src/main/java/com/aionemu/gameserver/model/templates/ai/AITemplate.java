package com.aionemu.gameserver.model.templates.ai;

import com.aionemu.gameserver.model.ai.Ai;
import com.aionemu.gameserver.model.ai.Bombs;
import com.aionemu.gameserver.model.ai.Summons;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 模板（静态数据/XML）。
 * AI Template (static data/XML).
 *
 * @author xTz
 */
@NoArgsConstructor
public class AITemplate {

	/** 返回 NPC ID / Returns the npc id */
	@Getter
	private int npcId;
	/** 返回召唤物 / Returns the summons */
	@Getter
	private Summons summons;
	/** 返回炸弹 / Returns the bombs */
	@Getter
	private Bombs bombs;

	public AITemplate(Ai template) {
		this.summons = template.getSummons();
		this.bombs = template.getBombs();
		this.npcId = template.getNpcId();
	}
}
