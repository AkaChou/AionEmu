/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.ai2.manager;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.AttackIntention;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.world.geo.GeoService;

/**
 * NPC攻击管理器
 * 负责处理NPC的攻击调度和攻击逻辑
 * @author ATracer
 * @modified Yon (Aion Reconstruction Project) -- removed non-retail-like leash in {@link #checkGiveupDistance(NpcAI2)}.
 */
public class AttackManager {

	/**
	 * 开始攻击目标
	 * @param npcAI
	 */
	public static void startAttacking(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: startAttacking");
		}
		npcAI.getOwner().getGameStats().setFightStartingTime();
		EmoteManager.emoteStartAttacking(npcAI.getOwner());
		scheduleNextAttack(npcAI);
	}

	/**
	 * 安排下一次攻击
	 * 修复：添加了重复调度检查，防止多次调度导致一次攻击多次伤害
	 * @param npcAI
	 */
	public static void scheduleNextAttack(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: scheduleNextAttack");
		}
		
		// 检查是否已经调度了攻击，防止重复调度
		if (npcAI.getOwner().getGameStats().isNextAttackScheduled()) {
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "Attack already scheduled, skipping");
			}
			return;
		}
		
		// don't start attack while in casting substate
		AISubState subState = npcAI.getSubState();
		if (subState == AISubState.NONE) {
			chooseAttack(npcAI, npcAI.getOwner().getGameStats().getNextAttackInterval());
		} else {
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "Will not choose attack in substate" + subState);
			}
		}
	}

	/**
	 * 选择攻击类型
	 * @param npcAI
	 * @param delay 攻击延迟时间
	 */
	protected static void chooseAttack(NpcAI2 npcAI, int delay) {
		AttackIntention attackIntention = npcAI.chooseAttackIntention();
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: chooseAttack " + attackIntention + " delay " + delay);
		}
		if (!npcAI.canThink()) {
			return;
		}
		switch (attackIntention) {
			case SIMPLE_ATTACK:
				// 普通攻击
				SimpleAttackManager.performAttack(npcAI, delay);
				break;
			case SKILL_ATTACK:
				// 技能攻击
				SkillAttackManager.performAttack(npcAI, delay);
				break;
			case FINISH_ATTACK:
				// 结束攻击，进入思考状态
				npcAI.think();
				break;
			default:
				break;
		}
	}

	/**
	 * 目标太远时的处理
	 * @param npcAI
	 */
	public static void targetTooFar(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: attackTimeDelta " + npc.getGameStats().getLastAttackTimeDelta());
		}

		// 如果有更仇恨的目标，切换到那个目标
		if (npc.getGameStats().getLastChangeTargetTimeDelta() > 5) {
			Creature mostHated = npc.getAggroList().getMostHated();
			if (mostHated != null && !mostHated.getLifeStats().isAlreadyDead()
					&& !npc.isTargeting(mostHated.getObjectId())) {
				if (npcAI.isLogging()) {
					AI2Logger.info(npcAI, "AttackManager: switching target during chase");
				}
				npcAI.onCreatureEvent(AIEventType.TARGET_CHANGED, mostHated);
				return;
			}
		}
		// 无法看到目标，放弃目标
		if (!npc.canSee((Creature) npc.getTarget())) {
			npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
			return;
		}
		// 检查是否应该放弃目标
		if (checkGiveupDistance(npcAI)) {
			npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
			return;
		}
		// 尝试移动到目标
		if (npcAI.isMoveSupported()){
			npc.getMoveController().moveToTargetObject();
			return;
		}
		npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
	}

	/**
	 * 检查是否应该放弃目标
	 * @param npcAI
	 * @return true表示应该放弃目标
	 */
	private static boolean checkGiveupDistance(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		// if target run away too far
		float distanceToTarget = npc.getDistanceToTarget();
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: distanceToTarget " + distanceToTarget);
		}
		// TODO may be ask AI too
		int chaseTarget = npc.isBoss() ? 200 : npc.getPosition().getWorldMapInstance().getTemplate().getAiInfo().getChaseTarget();
		if (distanceToTarget > chaseTarget) {
			return true;
		}

		double distanceToHome = npc.getDistanceToSpawnLocation();
		int chaseHome = npc.isBoss() ? 350 : npc.getPosition().getWorldMapInstance().getTemplate().getAiInfo().getChaseHome();
//        if (distanceToHome > chaseHome) { //Leashes like this don't exist on retail (with very few exceptions)
//            return true;
//        }
		// 如果NPC离家太远且长时间没有攻击
		// start thinking about home after 100 meters and no attack for 10 seconds (only for default monsters)
		if (chaseHome <= 200 || distanceToHome > chaseHome) { // TODO: Check Client and use chase_user_by_trace value
			if ((npc.getGameStats().getLastAttackTimeDelta() > 10 && npc.getGameStats().getLastAttackedTimeDelta() > 10)
				/*|| (distanceToHome > chaseHome / 2 && npc.getGameStats().getLastAttackedTimeDelta() > 10)*/) {
				return true;
			}
		}
		return false;
	}
}