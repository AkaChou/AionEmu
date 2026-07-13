package com.aionemu.gameserver.ai2;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.poll.AIAnswer;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;

/**
 * AI2 核心接口，定义 NPC/召唤物等生物的人工智能行为契约。
 * Core AI2 interface defining the artificial intelligence contract for creatures such as NPCs and summons.
 *
 * @author ATracer
 */
public interface AI2 {

	/**
	 * 处理与生物相关的 AI 事件。
	 * Handles a creature-related AI event.
	 *
	 * @param event 事件类型 / event type
	 * related creature
	 */
	void onCreatureEvent(AIEventType event, Creature creature);

	/** 处理带真实命中状态的受击事件。 */
	default void onAttacked(Creature attacker, AttackStatus status) {
		onCreatureEvent(AIEventType.ATTACK, attacker);
	}

	/**
	 * 处理技能成功作用事件。
	 *
	 * @param caster 施法者
	 * @param skillId 技能 ID
	 * @param skillLevel 技能等级
	 */
	default void onSpelled(Creature caster, int skillId, int skillLevel) {
	}

	/** 处理玩家实际治疗 NPC 事件。 */
	default void onHealedByUser(Player player) {
	}

	/** 处理实际伤害事件。 */
	default void onDamaged(Creature attacker, int skillId) {
	}

	/** 处理看见附近生物参与攻击事件。 */
	default void onSeeAttack(Creature attacker, Creature attacked) {
	}

	/** 处理看见附近生物开始施法事件。 */
	default void onSeeSkill(Creature caster, Creature target, int skillId, int skillLevel) {
	}

	/** 处理附近友军被技能命中事件。 */
	default void onFriendSpelled(Creature caster, Creature friend, int skillId, int skillLevel) {
	}

	/** 处理附近友军被玩家击杀事件。 */
	default void onFriendKilledByUser(Creature friend, Player killer) {
	}

	/** 处理附近友军首次进入攻击状态事件。 */
	default void onFriendEnterAttackState(Creature friend, Creature target) {
	}

	/** 处理技能成功施放到自身事件。 */
	default void onCasted(Creature caster, int skillId, int skillLevel) {
	}

	/** 处理看见附近技能完成事件。 */
	default void onSeeSpell(Creature caster, Creature target, int skillId, int skillLevel) {
	}

	/** 处理玩家结束过场动画事件。 */
	default void onQuitCutscene(Player player, int cutsceneId) {
	}

	/** 处理玩家在该 NPC 处完成并领取任务奖励事件。 */
	default void onQuestFinished(Player player, int questId) {
	}

	/** 处理最高仇恨对象变化事件。 */
	default void onMostHatingUpdated(Creature creature) {
	}

	/** 处理进入异常状态事件。 */
	default void onEnterAbnormalState(Creature caster, int abnormalState) {
	}

	/** 处理离开异常状态事件。 */
	default void onLeaveAbnormalState(Creature caster, int abnormalState) {
	}

	/**
	 * 处理自定义 AI 事件。
	 * Handles a custom AI event.
	 *
	 * @param eventId 自定义事件 ID / custom event id
	 * @param args 事件参数 / event arguments
	 */
	void onCustomEvent(int eventId, Object... args);

	/**
	 * 处理通用（无目标）AI 事件。
	 * Handles a general (target-less) AI event.
	 *
	 * @param event 事件类型 / event type
	 */
	void onGeneralEvent(AIEventType event);

	/**
	 * 处理玩家对话框选择；若已处理返回 true。
	 * Handles player dialog selection; returns true if already handled.
	 *
	 * 玩家 / player
	 * dialog id
	 * quest id
	 * @param extendedRewardIndex 扩展奖励索引 / extended reward index
	 * @return 是否已处理 / whether the dialog was handled
	 */
	boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex);

	/**
	 * 执行一次 AI 思考/决策循环。
	 * Runs one AI think/decision cycle.
	 */
	void think();

	/**
	 * 判断当前是否允许思考。
	 * Returns whether thinking is currently allowed.
	 *
	 * @return 是否可思考 / whether the AI can think
	 */
	boolean canThink();

	/**
	 * 获取当前主状态。
	 * Returns the current main AI state.
	 *
	 * AI main state
	 */
	AIState getState();

	/**
	 * 获取当前子状态。
	 * Returns the current AI sub-state.
	 *
	 * AI sub-state
	 */
	AISubState getSubState();

	/**
	 * 获取 AI 名称（通常来自 {@link AIName} 注解）。
	 * Returns the AI name (usually from the {@link AIName} annotation).
	 *
	 * AI name
	 */
	String getName();

	/**
	 * 对指定问题进行投票式查询，返回是否肯定。
	 * Polls the given question and returns whether the answer is positive.
	 *
	 * AI question
	 * whether the answer is positive
	 */
	boolean poll(AIQuestion question);

	/**
	 * 询问指定问题并返回完整答案对象。
	 * Asks the given question and returns a full answer object.
	 *
	 * AI question
	 * AI answer
	 */
	AIAnswer ask(AIQuestion question);

	/**
	 * 是否开启 AI 调试日志。
	 * Returns whether AI debug logging is enabled.
	 *
	 * @return 是否记录日志 / whether logging is enabled
	 */
	boolean isLogging();

	/**
	 * 获取剩余时间（例如限时 AI 行为）。
	 * Returns remaining time (e.g. for timed AI behaviors).
	 *
	 * remaining time
	 */
	long getRemainigTime();

	/**
	 * 修改对自身造成的伤害值。
	 * Modifies incoming damage dealt to this AI's owner.
	 *
	 * original damage
	 * @return 修正后伤害 / modified damage
	 */
	int modifyDamage(int damage);

	/**
	 * 修改所有者造成的伤害值。
	 * Modifies outgoing damage dealt by this AI's owner.
	 *
	 * original damage
	 * @return 修正后伤害 / modified damage
	 */
	int modifyOwnerDamage(int damage);

	/**
	 * 处理来自其他 NPC 的个体事件通知。
	 * Handles an individual event notification from another NPC.
	 *
	 * @param npc 触发事件的生物 / creature that raised the event
	 */
	void onIndividualNpcEvent(Creature npc);

	/**
	 * 修改治疗数值。
	 * Modifies a heal value.
	 *
	 * @param value 原始治疗量 / original heal value
	 * @return 修正后治疗量 / modified heal value
	 */
	int modifyHealValue(int value);

	/**
	 * 修改命中/精准相关数值。
	 * Modifies a maccuracy (hit/accuracy) related value.
	 *
	 * original value
	 * modified value
	 */
	int modifyMaccuracy(int value);

	/**
	 * 修改感知范围。
	 * Modifies sensory/aggro range.
	 *
	 * @param value 原始范围 / original range
	 * @return 修正后范围 / modified range
	 */
	int modifySensoryRange(int value);

	/**
	 * 修改攻击类型。
	 * Modifies the item attack type.
	 *
	 * @param type 原始攻击类型 / original attack type
	 * @return 修正后攻击类型 / modified attack type
	 */
	ItemAttackType modifyAttackType(ItemAttackType type);
}
