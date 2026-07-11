package com.aionemu.gameserver.controllers.movement;

import com.aionemu.gameserver.model.gameobjects.Summon;

/**
 * 召唤物移动控制器。
 * Summon move controller.
 *
 * @author ATracer
 */
public class SummonMoveController extends PlayableMoveController<Summon> {

	/**
	 * 使用指定召唤物构造控制器。
	 * Construct the controller for the given summon.
	 *
	 * Summon owner
	 */
	public SummonMoveController(Summon owner) {
		super(owner);
	}

	/**
	 * 向当前目标对象移动，默认不处理。
	 * Move toward the current target object; the default implementation is a no-op.
	 */
	public void moveToTargetObject() {
	}

	/**
	 * 技能施放时设置为立即移动掩码。
	 * Set the immediate movement mask during skill cast.
	 */
	@Override
	public void skillMovement() {
		this.movementMask = MovementMask.IMMEDIATE;

	}
}
