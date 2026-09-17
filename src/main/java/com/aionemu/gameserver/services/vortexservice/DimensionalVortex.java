package com.aionemu.gameserver.services.vortexservice;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.vortex.VortexLocation;
import com.aionemu.gameserver.model.vortex.VortexStateType;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;

/**
 * 次元漩涡入侵活动抽象基类。
 * Abstract base for dimensional-vortex invasion events.
 *
 * <p>管理启动/停止幂等、生成器监听与攻防双方玩家列表契约。
 * Manages idempotent start/stop, generator listeners, and the defender/invader player-list contract.</p>
 *
 * @author Rinzler (Encom)
 * @param <VL> 漩涡地点类型 / vortex location type
 */
@Getter
@Setter
@RequiredArgsConstructor
public abstract class DimensionalVortex<VL extends VortexLocation> {

	/**
	 * 获取绑定的漩涡地点。
	 * Returns the bound vortex location.
	 *
	 * @return 绑定的漩涡地点 / location
	 */
	private final VL vortexLocation;
	/**
	 * 获取生成器摧毁监听器。
	 * Returns the generator destroy listener.
	 *
	 * @return 生成器摧毁监听器 / listener
	 */
	private final GeneratorDestroyListener generatorDestroyListener = new GeneratorDestroyListener(this);
	private final AtomicBoolean finished = new AtomicBoolean();
	/**
	 * 生成器是否已被摧毁。
	 * Whether the generator has been destroyed.
	 *
	 * @return 已摧毁则为 true / true if destroyed
	 */
	private boolean generatorDestroyed;
	/**
	 * 获取裂隙生成器 NPC。
	 * Returns the rift generator NPC.
	 *
	 * @return 生成器 NPC / generator
	 */
	private Npc generator;
	private boolean started;

	/**
	 * 启动入侵的具体实现。
	 * Concrete start-invasion logic.
	 */
	protected abstract void startInvasion();

	/**
	 * 停止入侵的具体实现。
	 * Concrete stop-invasion logic.
	 */
	protected abstract void stopInvasion();

	/**
	 * 将玩家加入攻方或守方。
	 * Adds a player as invader or defender.
	 *
	 * @param player 玩家 / player
	 * @param isInvader 是否攻方 / true for invader
	 */
	public abstract void addPlayer(Player player, boolean isInvader);

	/**
	 * 将玩家踢出攻方或守方。
	 * Kicks a player from invader or defender side.
	 *
	 * @param player 玩家 / player
	 * @param isInvader 是否攻方 / true for invader
	 */
	public abstract void kickPlayer(Player player, boolean isInvader);

	/**
	 * 尝试将玩家登记为守方（可含确认弹窗）。
	 * Tries to register a player as defender (may prompt confirmation).
	 *
	 * @param defender 守方玩家 / defender
	 */
	public abstract void updateDefenders(Player defender);

	/**
	 * 将玩家登记为攻方。
	 * Registers a player as invader.
	 *
	 * @param invader 攻方玩家 / invader
	 */
	public abstract void updateInvaders(Player invader);

	/**
	 * 守方玩家表。
	 * Defender player map.
	 *
	 * @return 守方玩家表 / defenders
	 */
	public abstract Map<Integer, Player> getDefenders();

	/**
	 * 攻方玩家表。
	 * Invader player map.
	 *
	 * @return 攻方玩家表 / invaders
	 */
	public abstract Map<Integer, Player> getInvaders();

	/**
	 * 启动入侵（幂等）。
	 * Starts the invasion (idempotent).
	 */
	public final void start() {
		boolean doubleStart = false;
		synchronized (this) {
			if (started) {
				doubleStart = true;
			} else {
				started = true;
			}
		}
		if (doubleStart) {
			return;
		}
		startInvasion();
	}

	/**
	 * 停止入侵（仅首次生效）。
	 * Stops the invasion (first call only).
	 */
	public final void stop() {
		if (finished.compareAndSet(false, true)) {
			stopInvasion();
		}
	}

	/**
	 * 在已刷出对象中定位裂隙生成器并注册死亡监听。
	 * Locates the rift generator among spawned objects and registers its death listener.
	 */
	protected void initRiftGenerator() {
		Npc gen = null;
		for (VisibleObject obj : new ArrayList<VisibleObject>(getVortexLocation().getSpawned())) {
			int npcId = ((Npc) obj).getNpcId();
			if (npcId == 209486 || npcId == 209487) {
				gen = (Npc) obj;
			}
		}
		if (gen == null) {
			throw new NullPointerException("No generator was found in loc:" + getVortexLocationId());
		}
		setGenerator(gen);
		registerSiegeBossListeners();
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by vortex state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(VortexStateType type) {
		GameLocationBootstrapServices.vortexService().spawn(getVortexLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.vortexService().despawn(getVortexLocation());
	}

	/**
	 * 为生成器 AI 注册死亡回调。
	 * Registers the death callback on the generator AI.
	 */
	protected void registerSiegeBossListeners() {
		AbstractAI ai = (AbstractAI) getGenerator().getAi2();
		ai.addAiDeathListener(getGeneratorDestroyListener());
	}

	/**
	 * 移除生成器 AI 上的死亡回调。
	 * Removes the death callback from the generator AI.
	 */
	protected void unregisterSiegeBossListeners() {
		AbstractAI ai = (AbstractAI) getGenerator().getAi2();
		ai.removeAiDeathListener(getGeneratorDestroyListener());
	}

	/**
	 * 是否已结束。
	 * Whether the event has finished.
	 *
	 * @return 已结束则为 true / true if finished
	 */
	public boolean isFinished() {
		return finished.get();
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * @return 地点 ID / location id
	 */
	public int getVortexLocationId() {
		return vortexLocation.getId();
	}
}
