package com.aionemu.gameserver.model.templates.ai;

import com.aionemu.gameserver.model.ai.Ai;
import com.aionemu.gameserver.model.ai.Bombs;
import com.aionemu.gameserver.model.ai.Summons;

/**
 * AI 模板（静态数据/XML）。
 * AI Template (static data/XML).
 *
 * @author xTz
 */
public class AITemplate {

	private int npcId;
	private Summons summons;
	private Bombs bombs;

	public AITemplate() {
	}

	public AITemplate(Ai template) {
		this.summons = template.getSummons();
		this.bombs = template.getBombs();
		this.npcId = template.getNpcId();
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 返回召唤物 / Returns the summons */
	public Summons getSummons() {
		return summons;
	}

	/** 返回炸弹 / Returns the bombs */
	public Bombs getBombs() {
		return bombs;
	}
}
