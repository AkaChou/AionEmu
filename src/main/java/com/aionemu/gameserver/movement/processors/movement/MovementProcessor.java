package com.aionemu.gameserver.movement.processors.movement;

import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.movement.processors.AGameProcessor;
import com.aionemu.gameserver.movement.processors.movement.motor.AMovementMotor;
import com.aionemu.gameserver.movement.processors.movement.motor.ReturnMotor;

/**
 * 生物移动处理器：管理生物与移动电机的注册，并提供回归等电机应用入口。
 * Creature movement processor: tracks creature-to-motor registration and applies motors such as return.
 */
public class MovementProcessor extends AGameProcessor {

	/**
	 * 已注册生物到其当前移动电机的映射。
	 * Map of registered creatures to their current movement motors.
	 */
	private final ConcurrentHashMap<Creature, AMovementMotor> _registeredCreatures = new ConcurrentHashMap<Creature, AMovementMotor>();

	/**
	 * 以 12 个工作线程创建移动处理器。
	 * Create a movement processor with 12 worker threads.
	 */
	public MovementProcessor() {
		super(12);
	}

	/**
	 * 为生物替换移动电机：停止旧电机并启动新电机。
	 * Replace a creature's movement motor: stop the old motor and start the new one.
	 *
	 * @param creature 目标生物 / target creature
	 * @param newMotor 新移动电机 / new movement motor
	 * @return 始终为 true（应用成功） / Always {@code true} on success
	 */
	private boolean applyMotor(Creature creature, AMovementMotor newMotor) {
		AMovementMotor oldMotor = this._registeredCreatures.put(creature, newMotor);
		if (oldMotor == newMotor) {
			throw new Error("Attempt to replace same movement motors");
		}
		if (oldMotor != null) {
			oldMotor.stop();
			newMotor.start();
		}
		return true;
	}

	/**
	 * 为 NPC 应用回归电机，使其移向指定地点。
	 * Apply a return motor so the NPC moves toward the given spot.
	 *
	 * @param creature 目标 NPC / target NPC
	 * @param spot 回归目标点 / return destination
	 * @return 已应用的回归电机，失败为 null / applied return motor, or null on failure
	 */
	public AMovementMotor applyReturn(Npc creature, Vector3f spot) {
		ReturnMotor returnMotor = new ReturnMotor(creature, spot, this);
		if (this.applyMotor(creature, returnMotor)) {
			return returnMotor;
		}
		return null;
	}
}
