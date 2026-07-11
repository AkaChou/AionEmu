package com.aionemu.gameserver.ai2.poll;

import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * NPC AI 的默认投票回答工具，提供腐朽与重生等通用决策的默认肯定结果。
 * Default poll-answer helpers for NPC AI, providing positive defaults for decay and respawn decisions.
 *
 * @author ATracer
 */
public class NpcAIPolls {

	/**
	 * 判断 NPC 死亡后是否应当腐朽消失；默认返回肯定。
	 * Decide whether the NPC should decay after death; defaults to positive.
	 *
	 * NPC AI instance
	 * Positive answer
	 */
	public static AIAnswer shouldDecay(NpcAI2 npcAI) {
		return AIAnswers.POSITIVE;
	}

	/**
	 * 判断 NPC 是否应当重生；默认返回肯定。
	 * Decide whether the NPC should respawn; defaults to positive.
	 *
	 * NPC AI instance
	 * Positive answer
	 */
	public static AIAnswer shouldRespawn(NpcAI2 npcAI) {
		return AIAnswers.POSITIVE;
	}
}
