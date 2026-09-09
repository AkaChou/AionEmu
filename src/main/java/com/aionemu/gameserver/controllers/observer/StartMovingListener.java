package com.aionemu.gameserver.controllers.observer;

import lombok.Getter;

/**
 * 监听施法者是否开始移动，用于打断需静止的技能。
 * Listens whether the effector started moving; used to interrupt skills that require stillness.
 *
 * @author ATracer
 */
public class StartMovingListener extends ActionObserver {

	/** 施法者是否已移动 / Whether the effector has moved */
	@Getter
	private boolean effectorMoved = false;

	/**
	 * 创建移动开始监听器。
	 * Create a start-moving listener.
	 */
	public StartMovingListener() {
		super(ObserverType.MOVE);
	}

	@Override
	public void moved() {
		effectorMoved = true;
	}
}
