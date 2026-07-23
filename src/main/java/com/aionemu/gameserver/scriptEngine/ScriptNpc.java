package com.aionemu.gameserver.scriptEngine;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 脚本 NPC 回调契约，对应真端 ScriptDLL64 的 {@code IAIScriptNpc}。
 * Script-NPC callback contract, mirroring the retail {@code IAIScriptNpc}.
 *
 * <p>由数据绑定实例化，引擎按事件回调；默认空实现便于逐副本渐进迁移，
 * 最终替代 {@code ai/instance/<副本>} 下硬编码坐标与分支的手写 AI2 桥接。
 */
public interface ScriptNpc {

	/**
	 * 返回该脚本绑定的 NPC 模板 ID。
	 * Return the NPC template id this script is bound to.
	 *
	 * @return NPC 模板 ID / NPC template id
	 */
	int getNpcId();

	/**
	 * 玩家发起对话。
	 * Player starts a dialog with the NPC.
	 *
	 * @param player 发起玩家 / initiating player
	 */
	default void onDialogStart(Player player) {
	}

	/**
	 * 玩家选择对话框选项；返回是否接管。
	 * Player selects a dialog option; return {@code true} when handled.
	 *
	 * @param player 发起玩家 / initiating player
	 * @param dialogId 对话框 ID / dialog id
	 * @param questId 关联任务 ID / related quest id
	 * @param extendedRewardIndex 扩展奖励索引 / extended reward index
	 * @return 是否由本脚本接管 / whether this script handled it
	 */
	default boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		return false;
	}

	/**
	 * NPC 发现玩家进入感知范围。
	 * The NPC notices a player entering sensory range.
	 *
	 * @param player 被发现的玩家 / noticed player
	 */
	default void onSeePlayer(Player player) {
	}

	/**
	 * 玩家离开感知范围。
	 * A player leaves sensory range.
	 *
	 * @param player 离开的玩家 / leaving player
	 */
	default void onLeavePlayer(Player player) {
	}

	/**
	 * NPC 被玩家击杀。
	 * The NPC is killed by a player.
	 *
	 * @param player 击杀者 / killer player
	 */
	default void onKilledByPlayer(Player player) {
	}

	/**
	 * NPC 死亡。
	 * The NPC dies.
	 *
	 * @param killer 击杀者 / killer
	 */
	default void onDied(Creature killer) {
	}

	/**
	 * NPC 脚本计时器到期。
	 * A script timer expires on the NPC.
	 *
	 * @param timerId 计时器标识 / timer id
	 */
	default void onTimerEnd(String timerId) {
	}

	/**
	 * 玩家进入区域。
	 * A player enters a zone.
	 *
	 * @param player 进入的玩家 / entering player
	 * @param zoneName 区域名 / zone name
	 */
	default void onEnterZone(Player player, ZoneName zoneName) {
	}

	/**
	 * 玩家离开区域。
	 * A player leaves a zone.
	 *
	 * @param player 离开的玩家 / leaving player
	 * @param zoneName 区域名 / zone name
	 */
	default void onLeaveZone(Player player, ZoneName zoneName) {
	}

	/**
	 * 影片播放结束。
	 * A movie finishes playing.
	 *
	 * @param player 观影玩家 / watching player
	 * @param movieId 影片 ID / movie id
	 */
	default void onMovieEnd(Player player, int movieId) {
	}

	/**
	 * 玩家使用物品。
	 * A player uses an item.
	 *
	 * @param player 使用者 / using player
	 * @param item 使用的物品 / used item
	 */
	default void onItemUse(Player player, Item item) {
	}

	/**
	 * 玩家完成 NPC 交互；返回 {@code true} 时由脚本接管，不再进入副本 Handler。
	 * Player finishes an NPC interaction; return {@code true} to own the event.
	 */
	default boolean onItemUseFinish(Player player, Npc npc) {
		return false;
	}
}
