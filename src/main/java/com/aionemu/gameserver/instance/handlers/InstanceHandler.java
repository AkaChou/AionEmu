package com.aionemu.gameserver.instance.handlers;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.StageType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 副本事件处理器接口：定义副本生命周期与玩家/NPC 交互回调。
 * Instance event-handler interface: lifecycle and player/NPC interaction callbacks.
 *
 * @author ATracer
 */
public interface InstanceHandler {

	/**
	 * 副本创建时执行（刷怪加载完成后）。
	 * Executed during instance creation (after spawns are loaded).
	 *
	 * @param instance 新建的世界地图实例 / created world-map instance
	 */
	void onInstanceCreate(WorldMapInstance instance);

	/**
	 * 副本销毁时执行（全部刷怪卸载后）；应清理类级共享对象。
	 * Executed during instance destroy (after all spawns unloaded); clean class-shared objects here.
	 */
	void onInstanceDestroy();

	/**
	 * 玩家登录到该副本时回调。
	 * Called when a player logs in while in this instance.
	 *
	 * 玩家 / player
	 */
	void onPlayerLogin(Player player);

	/**
	 * 玩家从该副本登出时回调。
	 * Called when a player logs out from this instance.
	 *
	 * 玩家 / player
	 */
	void onPlayerLogOut(Player player);

	/**
	 * 玩家进入副本时回调。
	 * Called when a player enters the instance.
	 *
	 * 玩家 / player
	 */
	void onEnterInstance(Player player);

	/**
	 * 玩家离开副本时回调。
	 * Called when a player leaves the instance.
	 *
	 * 玩家 / player
	 */
	void onLeaveInstance(Player player);

	/**
	 * 玩家打开门时回调。
	 * Called when a player opens a door.
	 *
	 * 玩家 / player
	 * door id
	 */
	void onOpenDoor(Player player, int door);

	/** 持久化并应用副本门状态。 */
	void setDoorState(int entityId, boolean open);

	/**
	 * 玩家进入区域时回调。
	 * Called when a player enters a zone.
	 *
	 * 玩家 / player
	 * zone
	 */
	void onEnterZone(Player player, ZoneInstance zone);

	/**
	 * 玩家离开区域时回调。
	 * Called when a player leaves a zone.
	 *
	 * 玩家 / player
	 * zone
	 */
	void onLeaveZone(Player player, ZoneInstance zone);

	/**
	 * 玩家播放过场动画结束时回调。
	 * Called when a player finishes playing a cutscene movie.
	 *
	 * 玩家 / player
	 * movie id
	 */
	void onPlayMovieEnd(Player player, int movieId);

	/**
	 * 处理玩家复活事件。
	 * Handle a player revive event.
	 *
	 * 玩家 / player
	 * @return 已处理则为 {@code true} / {@code true} if handled
	 */
	boolean onReviveEvent(Player player);

	/**
	 * 玩家请求退出副本时回调。
	 * Called when a player requests to exit the instance.
	 *
	 * 玩家 / player
	 */
	void onExitInstance(Player player);

	/**
	 * 结算并发放玩家奖励。
	 * Settle and grant rewards to the player.
	 *
	 * 玩家 / player
	 */
	void doReward(Player player);

	/**
	 * 玩家死亡时回调。
	 * Called when a player dies.
	 *
	 * dead player
	 * @param lastAttacker 最后攻击者 / last attacker
	 * @return 已处理则为 {@code true} / {@code true} if handled
	 */
	boolean onDie(Player player, Creature lastAttacker);

	/**
	 * 玩家停止训练时回调。
	 * Called when a player stops training.
	 *
	 * 玩家 / player
	 */
	void onStopTraining(Player player);

	/**
	 * NPC 死亡时回调。
	 * Called when an NPC dies.
	 *
	 * dead NPC
	 */
	void onDie(Npc npc);

	/**
	 * 副本阶段变更时回调。
	 * Called when the instance stage changes.
	 *
	 * @param type 新阶段类型 / new stage type
	 */
	void onChangeStage(StageType type);

	/**
	 * 返回当前副本阶段。
	 * Return the current instance stage.
	 *
	 * stage type
	 */
	StageType getStage();

	/**
	 * NPC 掉落表注册时回调。
	 * Called when an NPC's drop table is registered.
	 *
	 * related NPC
	 */
	void onDropRegistered(Npc npc);

	/**
	 * 玩家采集完成时回调。
	 * Called when a player finishes gathering.
	 *
	 * 玩家 / player
	 * gatherable
	 */
	void onGather(Player player, Gatherable paramGatherable);

	/**
	 * 返回本副本的奖励对象。
	 * Return this instance's reward object.
	 *
	 * @return 奖励；无则为 {@code null} / reward, or {@code null}
	 */
	InstanceReward<?> getInstanceReward();

	/**
	 * 玩家通过飞行环时回调。
	 * Called when a player passes a flying ring.
	 *
	 * 玩家 / player
	 * @param flyingRing 飞行环标识 / flying-ring id
	 * @return 已处理则为 {@code true} / {@code true} if handled
	 */
	boolean onPassFlyingRing(Player player, String flyingRing);

	/**
	 * 玩家对 NPC 使用物品完成时回调。
	 * Called when a player finishes using an item on an NPC.
	 *
	 * 玩家 / player
	 * target NPC
	 */
	void handleUseItemFinish(Player player, Npc npcId);

	/** 返回当前副本是否允许通用真端 Pattern 接管该 NPC。 */
	boolean supportsRetailPattern(int npcId);

	/** 返回当前副本是否能按真端语义消费该 NPC 的计分动作。 */
	boolean supportsRetailNpcScore(int npcId, int scoreApplyType);

	/** 将真端 NPC 分值交给当前副本的既有计分模型。 */
	boolean onRetailNpcScore(Player player, Npc npc, int scoreApplyType, int points);
}
