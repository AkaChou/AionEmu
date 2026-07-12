package com.aionemu.gameserver.questEngine.handlers;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.rewards.BonusType;
import com.aionemu.gameserver.questEngine.model.QuestActionType;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 任务处理器抽象基类：为各类游戏事件提供默认 no-op 钩子，子类按需覆写。
 * Abstract quest-handler base providing default no-op hooks for game events;
 * subclasses override only the events they care about.
 */
public abstract class AbstractQuestHandler {

	/**
	 * 向 {@link com.aionemu.gameserver.questEngine.QuestEngine} 注册本任务关心的事件。
	 * Register the events this quest cares about with {@link com.aionemu.gameserver.questEngine.QuestEngine}.
	 */
	public abstract void register();

	/**
	 * NPC 对话事件。
	 * NPC dialog event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onDialogEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 玩家进入世界事件。
	 * Player entered the world.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onEnterWorldEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 玩家进入区域事件。
	 * Player entered a zone.
	 *
	 * Quest environment
	 * Zone name
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onEnterZoneEvent(QuestEnv questEnv, ZoneName zoneName) {
		return false;
	}

	/**
	 * 玩家离开区域事件。
	 * Player left a zone.
	 *
	 * Quest environment
	 * Zone name
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onLeaveZoneEvent(QuestEnv questEnv, ZoneName zoneName) {
		return false;
	}

	/**
	 * 使用物品事件。
	 * Item-use event.
	 *
	 * Quest environment
	 * @param item 使用的物品 / Used item
	 * Handler result
	 */
	public HandlerResult onItemUseEvent(QuestEnv questEnv, Item item) {
		return HandlerResult.UNKNOWN;
	}

	/**
	 * 使用房屋物品事件。
	 * House-item use event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Item template id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onHouseItemUseEvent(QuestEnv env, int itemId) {
		return false;
	}

	/**
	 * 获得物品事件。
	 * Item-obtained event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onGetItemEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 使用技能事件。
	 * Skill-use event.
	 *
	 * Quest environment
	 * Skill id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onUseSkillEvent(QuestEnv questEnv, int skillId) {
		return false;
	}

	/**
	 * 击杀事件。
	 * Kill event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onKillEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 攻击事件。
	 * Attack event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onAttackEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 升级事件。
	 * Level-up event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onLvlUpEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 区域任务结束事件。
	 * Zone-mission end event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onZoneMissionEndEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 玩家死亡事件。
	 * Player-death event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onDieEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 玩家登出事件。
	 * Player logout event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onLogOutEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 跟随 NPC 到达目标事件。
	 * Escort NPC reached its target.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onNpcReachTargetEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 跟随 NPC 丢失目标事件。
	 * Escort NPC lost its target.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onNpcLostTargetEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 过场动画结束事件。
	 * Movie-end event.
	 *
	 * Quest environment
	 * Movie id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onMovieEndEvent(QuestEnv questEnv, int movieId) {
		return false;
	}

	/**
	 * 任务计时器结束事件。
	 * Quest-timer end event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onQuestTimerEndEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 隐形计时器结束事件。
	 * Invisible-timer end event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onInvisibleTimerEndEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 穿过飞行环事件。
	 * Passed a flying ring.
	 *
	 * Quest environment
	 *
	 * @param flyingRing 飞行环标识 / Flying-ring key
	 * @param flyingRing
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onPassFlyingRingEvent(QuestEnv questEnv, String flyingRing) {
		return false;
	}

	/**
	 * 击杀指定欧比斯军衔玩家事件。
	 * Kill of a ranked (abyss) player.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onKillRankedEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 进入风道事件。
	 * Entered a wind stream.
	 *
	 * Quest environment
	 * 世界 ID / World id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onEnterWindStreamEvent(QuestEnv questEnv, int worldId) {
		return false;
	}

	/**
	 * 骑乘动作事件。
	 * Ride action event.
	 *
	 * Quest environment
	 * Ride item id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean rideAction(QuestEnv questEnv, int rideItemId) {
		return false;
	}

	/**
	 * 世界内击杀事件。
	 * Kill-in-world event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onKillInWorldEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 制作失败事件。
	 * Craft-fail event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Item id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onFailCraftEvent(QuestEnv env, int itemId) {
		return false;
	}

	/**
	 * 装备物品事件。
	 * Equip-item event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Item id
	 *
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onEquipItemEvent(QuestEnv env, int itemId) {
		return false;
	}

	/**
	 * 判断玩家是否可对目标执行指定任务动作（默认要求任务处于 START）。
	 * Whether the player may perform the given quest action (default: quest must be START).
	 *
	 * @param env 任务环境 / Quest environment
	 * Action type
	 * Extra arguments
	 * Whether allowed
	 */
	public boolean onCanAct(QuestEnv env, QuestActionType questEventType, Object... objects) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(env.getQuestId());
		return (qs != null) && (qs.getStatus() == QuestStatus.START);
	}

	/**
	 * 被加入仇恨列表事件。
	 * Added-to-aggro-list event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onAddAggroListEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 靠近目标距离事件。
	 * At-distance (proximity) event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onAtDistanceEvent(QuestEnv questEnv) {
		return false;
	}

	/**
	 * 挖掘号奖励事件。
	 * Dredgion reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onDredgionRewardEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 卡玛尔奖励事件。
	 * Kamar reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onKamarRewardEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 欧菲丹奖励事件。
	 * Ophidan reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onOphidanRewardEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 堡垒奖励事件。
	 * Bastion reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onBastionRewardEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 奖励加成应用事件。
	 * Bonus-apply event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Bonus type
	 * @param rewardItems 奖励物品列表 / Reward items
	 * Handler result
	 */
	public HandlerResult onBonusApplyEvent(QuestEnv env, BonusType bonusType, List<QuestItems> rewardItems) {
		return HandlerResult.UNKNOWN;
	}

	/**
	 * 保护任务成功结束事件。
	 * Protect-mission success event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onProtectEndEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 保护任务失败事件。
	 * Protect-mission failure event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onProtectFailEvent(QuestEnv env) {
		return false;
	}

	/**
	 * 创造力点数事件。
	 * Creativity-point event.
	 *
	 * Quest environment
	 *
	 * @param questEnv
	 * @return 是否已处理 / Whether handled
	 */
	public boolean onCreativityPointEvent(QuestEnv questEnv) {
		return false;
	}
}
