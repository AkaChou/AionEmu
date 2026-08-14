package com.aionemu.gameserver.services.conquestservice;

import com.aionemu.commons.callbacks.EnhancedObject;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.conquest.ConquestLocation;
import com.aionemu.gameserver.model.conquest.ConquestStateType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

/**
 * 征服/供奉活动抽象基类。
 * Abstract base for Conquest/Offering world events.
 *
 * <p>管理启动/停止幂等、供奉 BOSS 定位与死亡监听。
 * Manages idempotent start/stop, offering-boss location and death listeners.</p>
 *
 * @author Rinzler (Encom)
 * @param <CL> 征服地点类型 / conquest location type
 */
@Slf4j(topic = "com.aionemu.gameserver.services.conquestservice.ConquestOffering")
public abstract class ConquestOffering<CL extends ConquestLocation> {

	private boolean started;
	/**
	 * -- SETTER --
	 * 设置供奉 BOSS NPC。
	 * Sets the offering boss NPC.
	 * boss
	 * -- GETTER --
	 * 获取供奉 BOSS NPC。
	 * Returns the offering boss NPC.
	 * boss
	 */
	@Getter
	@Setter
	private Npc conquestBoss;
	/**
	 * -- GETTER --
	 * 获取绑定地点。
	 * Returns the bound location.
	 * location
	 */
	@Getter
	private final CL conquestLocation;
	/**
	 * -- SETTER --
	 * 设置供奉 BOSS 摧毁状态。
	 * Sets the offering boss destroyed flag.
	 * state
	 * -- GETTER --
	 * 供奉 BOSS 是否已被摧毁。
	 * Whether the offering boss has been destroyed.
	 *
	 * @return 已摧毁则为 true / true if destroyed
	 */
	@Getter
	@Setter
	private boolean conquestBossDestroyed;
	private final AtomicBoolean finished = new AtomicBoolean();
    /**
     * -- GETTER --
	 *  获取 BOSS 死亡监听器。
	 *  Returns the boss destroy listener.
	 *  listener
	 */
	@Getter
	private final ConquestBossDestroyListener conquestBossDestroyListener = new ConquestBossDestroyListener(this);

	/**
	 * 停止活动的具体实现。
	 * Concrete stop logic.
	 */
	protected abstract void stopConquest();

	/**
	 * 启动活动的具体实现。
	 * Concrete start logic.
	 */
	protected abstract void startConquest();

	/**
	 * 绑定征服地点。
	 * Binds the conquest location.
	 *
	 * location
	 */
	public ConquestOffering(CL conquestLocation) {
		this.conquestLocation = conquestLocation;
	}

	/**
	 * 启动活动（幂等）。
	 * Starts the event (idempotent).
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
		startConquest();
	}

	/**
	 * 停止活动（仅首次生效）。
	 * Stops the event (first call only).
	 */
	public final void stop() {
		if (finished.compareAndSet(false, true)) {
			stopConquest();
		}
	}

	/**
	 * 在已刷出对象中定位供奉 BOSS 并注册死亡监听。
	 * Locates the offering boss among spawned objects and registers its death listener.
	 */
	protected void initConquestBoss() {
		Npc cb = null;
		for (VisibleObject obj : new ArrayList<VisibleObject>(getConquestLocation().getSpawned())) {
			int npcId = ((Npc) obj).getNpcId();
			// 征服/供奉 英吉斯温。 / Conquest/Offering Inggison.
			if (npcId >= 236530 && npcId <= 236553) {
				cb = (Npc) obj;
				break;
			}
			// 征服/供奉 格尔克马洛斯。 / Conquest/Offering Gelkmaros.
			if (npcId >= 236586 && npcId <= 236609) {
				cb = (Npc) obj;
				break;
			}
		}
		if (cb == null) {
			// id 3-14 为限时副本入口通知（术古宝库/火神殿/库穆奇/提亚之眼等），本身无征服 BOSS，
			// 仅靠 spawn(CONQUEST) 刷出入口 NPC 即可，不应中断活动。
			log.warn("No <Conquest/Offering Boss> in loc:{} — 入口型活动，跳过 BOSS 初始化", getConquestLocationId());
			return;
		}
		setConquestBoss(cb);
		addConquestBossListeners();
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(ConquestStateType type) {
		GameLocationBootstrapServices.conquestService().spawn(getConquestLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.conquestService().despawn(getConquestLocation());
	}

	/**
	 * 为供奉 BOSS AI 注册死亡回调。
	 * Registers the death callback on the offering boss AI.
	 */
	protected void addConquestBossListeners() {
		AbstractAI ai = (AbstractAI) getConquestBoss().getAi2();
		EnhancedObject eo = (EnhancedObject) ai;
		eo.addCallback(getConquestBossDestroyListener());
	}

	/**
	 * 移除供奉 BOSS AI 上的死亡回调。
	 * Removes the death callback from the offering boss AI.
	 */
	protected void rmvConquestBossListener() {
		if (getConquestBoss() == null) {
			return;
		}
		AbstractAI ai = (AbstractAI) getConquestBoss().getAi2();
		EnhancedObject eo = (EnhancedObject) ai;
		eo.removeCallback(getConquestBossDestroyListener());
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
	 * <p>
	 * location id
	 */
	public int getConquestLocationId() {
		return conquestLocation.getId();
	}
}
