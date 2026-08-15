package com.aionemu.gameserver.ai2.manager;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * NPC 跟随管理器：处理跟随目标过远时的追击移动。
 * NPC follow manager: handles chase movement when the follow target is too far.
 *
 * @author ATracer
 */
public class FollowManager {

	/**
	 * 跟随目标过远时尝试向目标移动。
	 * Attempts to move toward the target when the follow target is too far.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void targetTooFar(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "Follow manager - targetTooFar");
		}
		startMoving(npcAI);
	}

	/**
	 * 立即启动朝当前跟随目标的移动；无移动能力的 NPC 保持原地。
	 * Starts movement toward the current follow target immediately; immobile NPCs remain stationary.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @return 是否启动移动 / whether movement was started
	 */
	public static boolean startMoving(NpcAI2 npcAI) {
		if (!npcAI.isMoveSupported()) {
			return false;
		}
		Npc npc = npcAI.getOwner();
		npc.getMoveController().moveToTargetObject();
		return true;
	}
}
