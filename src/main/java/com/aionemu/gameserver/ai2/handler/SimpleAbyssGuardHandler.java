package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 欧比斯简易守卫处理器，对可攻击 / 敌对 NPC 做仇恨检查，其余委托 {@link CreatureEventHandler}。
 * aggressive NPCs, delegates others to {@link CreatureEventHandler}.
 */
public class SimpleAbyssGuardHandler {

	/**
	 * 生物移动时检查欧比斯守卫仇恨。
	 * Checks abyss-guard aggro when a creature moves.
	 *
	 * NPC AI instance
	 * @param creature 移动的生物 / moving creature
	 */
	public static void onCreatureMoved(NpcAI2 npcAI, Creature creature) {
		checkAggro(npcAI, creature);
	}

	/**
	 * 生物进入视野时检查欧比斯守卫仇恨。
	 * Checks abyss-guard aggro when a creature is seen.
	 *
	 * NPC AI instance
	 * @param creature 进入视野的生物 / creature that became visible
	 */
	public static void onCreatureSee(NpcAI2 npcAI, Creature creature) {
		checkAggro(npcAI, creature);
	}

	/**
	 * 欧比斯守卫仇恨检查：非 NPC 走通用逻辑；对可攻击/敌对且无目标的 NPC 在范围内触发仇恨。
	 * Abyss-guard aggro check: non-NPCs use common logic; attackable/aggressive target-less NPCs trigger aggro in range.
	 *
	 * NPC AI instance
	 * @param creature 待检查生物 / creature to evaluate
	 */
	protected static void checkAggro(NpcAI2 ai, Creature creature) {
		if (!(creature instanceof Npc)) {
			CreatureEventHandler.checkAggro(ai, creature);
			return;
		}
		Npc owner = ai.getOwner();
		if (creature.getLifeStats().isAlreadyDead() || !owner.canSee(creature)) {
			return;
		}
		Npc npc = ((Npc) creature);
		if (npc.getNpcType() != NpcType.ATTACKABLE && npc.getNpcType() != NpcType.AGGRESSIVE || npc.getLevel() < 2) {
			return;
		}
		if (creature.getTarget() != null) {
			return;
		}
		if (!owner.getActiveRegion().isMapRegionActive()) {
			return;
		}
		if (!ai.isInState(AIState.FIGHT)
				&& (MathUtil.isIn3dRange(owner, creature, owner.getObjectTemplate().getAggroRange()))) {
			if (GameWorldServices.geoService().canSee(owner, creature)) {
				if (!ai.isInState(AIState.RETURNING))
					ai.getOwner().getMoveController().storeStep();
				ai.onCreatureEvent(AIEventType.CREATURE_AGGRO, creature);
			}
		}
	}
}
