package com.aionemu.gameserver.ai.instance.beshmundirTemple;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.model.actions.NpcActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Beshmundir Temple 副本 NPC AI：Sacrificial Soul（@AIName "templeSoul"），继承 AggressiveNpcAI2。
 * Beshmundir Temple instance NPC AI: Sacrificial Soul (@AIName "templeSoul"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("templeSoul")
public class SacrificialSoulAI2 extends AggressiveNpcAI2
{
	private Npc boss;
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		AI2Actions.useSkill(this, 18901); // 时间褶皱 / Time Wrinkle.
		this.setStateIfNot(AIState.FOLLOWING);
		boss = getPosition().getWorldMapInstance().getNpc(216263); // Boss：Isbariya The Resolute / Isbariya The Resolute.
		if (boss != null && !NpcActions.isAlreadyDead(boss)) {
			AI2Actions.targetCreature(this, boss);
			getMoveController().moveToTargetObject();
		}
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (creature.getEffectController().hasAbnormalEffect(18959)) { // 第六感 / Sixth Sense.
			getMoveController().abortMove();
			AI2Actions.deleteOwner(this);
		}
	}
	
	@Override
	protected void handleMoveArrived() {
		if (boss != null && !NpcActions.isAlreadyDead(boss)) {
			GameEngineServices.skillEngine().getSkill(getOwner(), 18960, 55, boss).useNoAnimationSkill(); // 死亡召唤 / Call Of The Grave.
			AI2Actions.deleteOwner(this);
		}
	}
	
	@Override
	public boolean canThink() {
		return false;
	}
}
