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
	 * NPC AI instance
	 */
	public static void targetTooFar(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "Follow manager - targetTooFar");
		}
		if (npcAI.isMoveSupported()) {
			npc.getMoveController().moveToTargetObject();
		}
	}
}
