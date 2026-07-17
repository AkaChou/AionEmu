package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 抽象碰撞观察者：在移动时对几何体做射线检测并回调。
 * Abstract collision observer: ray-tests geometry on move and invokes a callback.
 *
 * @author MrPoke
 * @moved Rolandas
 */
public abstract class AbstractCollisionObserver extends ActionObserver {

	/** 被观察生物 / Observed creature */
	protected Creature creature;
	/** 上一位置 / Previous position */
	protected Vector3f oldPos;
	/** 碰撞几何体 / Collision geometry */
	protected Spatial geometry;
	/** 碰撞意图掩码 / Collision intention mask */
	protected byte intentions;
	/** 检测类型（接触/穿越） / Check type (touch/pass) */
	private final CheckType checkType;
	/** 异步检测是否正在运行 / Whether async check is running */
	private AtomicBoolean isRunning = new AtomicBoolean();

	/**
	 * 默认 PASS 检测类型构造。
	 * Constructor with default PASS check type.
	 *
	 * creature
	 * geometry
	 * collision intentions
	 */
	public AbstractCollisionObserver(Creature creature, Spatial geometry, byte intentions) {
		this(creature, geometry, intentions, CheckType.PASS);
	}

	/**
	 * creature
	 * geometry
	 * collision intentions
	 * check type
	 */
	public AbstractCollisionObserver(Creature creature, Spatial geometry, byte intentions, CheckType checkType) {
		super(ObserverType.MOVE_OR_DIE);
		this.creature = creature;
		this.geometry = geometry;
		this.oldPos = new Vector3f(creature.getX(), creature.getY(), creature.getZ());
		this.intentions = intentions;
		this.checkType = checkType;
	}

	@Override
	public void moved() {
		if (!isRunning.getAndSet(true)) {
			GameThreadPoolServices.threadPoolManager().execute(new Runnable() {

				@Override
				public void run() {
					try {
						Vector3f pos;
						Vector3f dir;
						if (checkType == CheckType.TOUCH) {
							float x = creature.getX();
							float y = creature.getY();
							float z = creature.getZ();
							float zMax = z + 0.05f + creature.getObjectTemplate().getBoundRadius().getUpper();
							float zMin = z - 0.11f;
							if (!creature.isFlying()) {
									float geoZ = GameWorldServices.geoService().getZ(creature.getWorldId(), x, y, z, 100.0f, creature.getInstanceId());
								if (!Float.isNaN(geoZ)) {
									zMin = geoZ - 0.11f;
								}
							}
							pos = new Vector3f(x, y, zMax);
							dir = new Vector3f(x, y, zMin);
						} else {
							pos = new Vector3f(creature.getX(), creature.getY(), creature.getZ() + GeoMap.COLLISION_CHECK_Z_OFFSET);
							dir = oldPos.clone();
							dir.setZ(dir.getZ() + GeoMap.COLLISION_CHECK_Z_OFFSET);
						}
						Float limit = pos.distance(dir);
						dir.subtractLocal(pos).normalizeLocal();
						Ray r = new Ray(pos, dir);
						r.setLimit(limit);
						CollisionResults results = new CollisionResults(intentions, true, creature.getInstanceId());
						geometry.collideWith(r, results);
						onMoved(results);
						oldPos = pos;
					} finally {
						isRunning.set(false);
					}
				}
			});
		}
	}

	/**
	 * 移动检测完成后的回调。
	 * Callback after a move collision check completes.
	 *
	 * collision results
	 */
	public abstract void onMoved(CollisionResults result);

	/**
	 * 碰撞检测类型。
	 * Collision check type.
	 */
	public enum CheckType {
		/** 接触检测（竖直射线） / Touch check (vertical ray) */
		TOUCH,
		/** 穿越检测（位移路径） / Pass-through check (movement path) */
		PASS
	}
}
