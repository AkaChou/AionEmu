package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * 移动事件处理器，负责移动校验与到达目标时的后续处理。
 * Handles move events: move validation and arrival handling.
 *
 * @author ATracer Rework: Angry Catster
 */
public class MoveEventHandler {

	/**
	 * 移动校验：通知控制器移动，并检查目标是否过远。
	 * Validates movement: notifies controller of move and checks if the target is too far.
	 *
	 * NPC AI instance
	 */
	public static final void onMoveValidate(NpcAI2 npcAI) {
		npcAI.getOwner().getController().onMove();
		TargetEventHandler.onTargetTooFar(npcAI);
	}

	/**
	 * 移动到达：通知控制器移动，并处理目标已到达。
	 * Handles arrival: notifies controller of move and processes target reached.
	 *
	 * NPC AI instance
	 */
	public static final void onMoveArrived(NpcAI2 npcAI) {
		npcAI.getOwner().getController().onMove();
		TargetEventHandler.onTargetReached(npcAI);
	}
}
