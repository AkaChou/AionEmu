package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AttackIntention;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.handler.*;
import com.aionemu.gameserver.ai2.manager.SkillAttackManager;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.NpcSkillEntry;
import com.aionemu.gameserver.model.templates.npcshout.ShoutEventType;

/**
 * 通用 NPC AI：派发思考、攻击、对话、归位、移动与目标等标准事件，并选择普攻/技能攻击意图。
 * General NPC AI that dispatches think/attack/talk/return/move/target events and chooses simple or skill attack intention.
 *
 * @author Encom
 */
@AIName("general")
public class GeneralNpcAI2 extends NpcAI2
{
	/**
	 * 执行 AI 思考循环（状态机 tick）。
	 * Run one AI think cycle (state-machine tick).
	 */
	@Override
	public void think() {
		ThinkEventHandler.onThink(this);
	}
	
	/**
	 * 处理死亡事件。
	 * Handle death.
	 */
	@Override
	protected void handleDied() {
		DiedEventHandler.onDie(this);
	}
	
	/**
	 * 处理受到攻击事件。
	 * Handle being attacked.
	 *
	 * creature
	 */
	@Override
	protected void handleAttack(Creature creature) {
		AttackEventHandler.onAttack(this, creature);
	}
	
	/**
	 * 处理盟友需要支援事件。
	 * Handle ally needs-support.
	 *
	 * creature
	 */
	@Override
	protected boolean handleCreatureNeedsSupport(Creature creature) {
		return AggroEventHandler.onCreatureNeedsSupport(this, creature);
	}
	
	/**
	 * 玩家开始与本 NPC 对话/交互。
	 * Player starts dialog/interaction with this NPC.
	 *
	 * 玩家 / player
	 */
	@Override
	protected void handleDialogStart(Player player) {
		TalkEventHandler.onTalk(this, player);
	}
	
	/**
	 * 玩家结束与本 NPC 对话。
	 * Player finishes dialog with this NPC.
	 *
	 * creature
	 */
	@Override
	protected void handleDialogFinish(Player creature) {
		TalkEventHandler.onFinishTalk(this, creature);
	}
	
	/**
	 * 处理结束攻击事件。
	 * Handle finish-attack.
	 */
	@Override
	protected void handleFinishAttack() {
		AttackEventHandler.onFinishAttack(this);
	}
	
	/**
	 * 处理单次攻击完成事件。
	 * Handle attack-complete.
	 */
	@Override
	protected void handleAttackComplete() {
		AttackEventHandler.onAttackComplete(this);
	}
	
	/**
	 * 处理到达目标事件。
	 * Handle target reached.
	 */
	@Override
	protected void handleTargetReached() {
		TargetEventHandler.onTargetReached(this);
	}
	
	/**
	 * 处理不在出生点事件（开始归位）。
	 * Handle not-at-home (start returning).
	 */
	@Override
	protected void handleNotAtHome() {
		ReturningEventHandler.onNotAtHome(this);
	}
	
	/**
	 * 处理归位完成事件。
	 * Handle back-home.
	 */
	@Override
	protected void handleBackHome() {
		ReturningEventHandler.onBackHome(this);
	}
	
	/**
	 * 处理目标过远事件。
	 * Handle target-too-far.
	 */
	@Override
	protected void handleTargetTooFar() {
		TargetEventHandler.onTargetTooFar(this);
	}
	
	/**
	 * 处理放弃目标事件。
	 * Handle target give-up.
	 */
	@Override
	protected void handleTargetGiveup() {
		TargetEventHandler.onTargetGiveup(this);
	}
	
	/**
	 * 处理目标变更事件。
	 * Handle target changed.
	 *
	 * creature
	 */
	@Override
	protected void handleTargetChanged(Creature creature) {
		super.handleTargetChanged(creature);
		TargetEventHandler.onTargetChange(this, creature);
	}

	/**
	 * 处理看不见生物事件。
	 * Handle creature-not-see.
	 *
	 * creature
	 */
	@Override
	protected void handleCreatureNotSee(Creature creature) {
		if (creature.equals(getTarget())) {
			getOwner().getController().abortCast();
			onGeneralEvent(AIEventType.TARGET_TOOFAR);
		}
	}
	
	/**
	 * 校验当前移动是否仍有效。
	 * Validate whether current movement is still valid.
	 */
	@Override
	protected void handleMoveValidate() {
		MoveEventHandler.onMoveValidate(this);
	}
	
	/**
	 * 处理移动到达事件。
	 * Handle move-arrived.
	 */
	@Override
	protected void handleMoveArrived() {
		super.handleMoveArrived();
		MoveEventHandler.onMoveArrived(this);
	}
	
	/**
	 * 处理生物移动事件。
	 * Handle creature-moved.
	 *
	 * creature
	 */
	@Override
	protected void handleCreatureMoved(Creature creature) {
		CreatureEventHandler.onCreatureMoved(this, creature);
	}
	
	/**
	 * 处理消失事件。
	 * Handle despawn.
	 */
	@Override
	protected void handleDespawned() {
		super.handleDespawned();
	}
	
	/**
	 * 判断是否可处理指定 AI 事件类型。
	 * Whether the given AI event type can be handled.
	 *
	 * AI event type
	 */
	@Override
	protected boolean canHandleEvent(AIEventType eventType) {
		boolean canHandle = super.canHandleEvent(eventType);
		switch (eventType) {
			case CREATURE_MOVED:
				return canHandle || DataManager.NPC_SHOUT_DATA.hasAnyShout(getOwner().getWorldId(), getOwner().getNpcId(), ShoutEventType.SEE);
			case CREATURE_NEEDS_SUPPORT:
				return canHandle && isNonFightingState() && DataManager.TRIBE_RELATIONS_DATA.hasSupportRelations(getOwner().getTribe());
		}
		return canHandle;
	}
	
	/**
	 * 选择下一次攻击意图（普攻/技能/换目标/结束）。
	 * Choose the next attack intention (simple/skill/switch/finish).
	 */
	@Override
	public AttackIntention chooseAttackIntention() {
		VisibleObject currentTarget = getTarget();
		Creature mostHated = getAggroList().getMostHated();
		if (mostHated == null || mostHated.getLifeStats().isAlreadyDead()) {
			return AttackIntention.FINISH_ATTACK;
		} if (currentTarget == null || !currentTarget.getObjectId().equals(mostHated.getObjectId())) {
			onCreatureEvent(AIEventType.TARGET_CHANGED, mostHated);
			return AttackIntention.SWITCH_TARGET;
		} if (getOwner().getObjectTemplate().getAttackRange() == 0) {
			NpcSkillEntry skill = getOwner().getSkillList().getRandomSkill();
			if (skill != null) {
				skillId = skill.getSkillId();
				skillLevel = skill.getSkillLevel();
				return AttackIntention.SKILL_ATTACK;
			}
		} else {
			NpcSkillEntry skill = SkillAttackManager.chooseNextSkill(this);
			if (skill != null) {
				skillId = skill.getSkillId();
				skillLevel = skill.getSkillLevel();
				return AttackIntention.SKILL_ATTACK;
			}
		}
		return AttackIntention.SIMPLE_ATTACK;
	}
}
