package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npcshout.NpcShout;
import com.aionemu.gameserver.model.templates.npcshout.ShoutEventType;
import com.aionemu.gameserver.model.templates.npcshout.ShoutType;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;
import com.aionemu.gameserver.services.NpcShoutsService;

/**
 * NPC 喊话事件处理器，按各类战斗 / 行走 / 死亡事件触发模板喊话。
 * Handles NPC shout events: fires template shouts for combat, walking, death, and related events.
 *
 * @author Rolandas
 */
public final class ShoutEventHandler {

	/**
	 * 看见生物时触发 SEE 喊话。
	 * Fires SEE shouts when a creature is seen.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @param target 看见的目标 / seen target
	 */
	public static void onSee(NpcAI2 npcAI, Creature target) {
		Npc npc = npcAI.getOwner();
		if (DataManager.NPC_SHOUT_DATA.hasAnyShout(npc.getPosition().getMapId(), npc.getNpcId(), ShoutEventType.SEE)) {
			List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(npc.getPosition().getMapId(),
					npc.getNpcId(), ShoutEventType.SEE, null, 0);
			GameFeatureServices.npcShoutsService().shout(npc, target, shouts, 0, false);
			shouts.clear();
		}
	}

	/**
	 * 消失前触发 BEFORE_DESPAWN 喊话。
	 * Fires BEFORE_DESPAWN shouts before despawn.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void onBeforeDespawn(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		if (DataManager.NPC_SHOUT_DATA.hasAnyShout(npc.getPosition().getMapId(), npc.getNpcId(),
				ShoutEventType.BEFORE_DESPAWN)) {
			List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(npc.getPosition().getMapId(),
					npc.getNpcId(), ShoutEventType.BEFORE_DESPAWN, null, 0);
			GameFeatureServices.npcShoutsService().shout(npc, null, shouts, 0, false);
			shouts.clear();
		}
	}

	/**
	 * 到达行走路点时，按概率触发转向或路点喊话。
	 * On reaching a walk point, randomly fires direction-change or waypoint shouts.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void onReachedWalkPoint(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		WalkerTemplate tp = DataManager.WALKER_DATA.getWalkerTemplate(npc.getSpawn().getWalkerId());
		int stepCount = tp.getRouteSteps().size();
		ShoutEventType shoutType = npc.getMoveController().isChangingDirection() ? ShoutEventType.WALK_DIRECTION
				: ShoutEventType.WALK_WAYPOINT;
		if (DataManager.NPC_SHOUT_DATA.hasAnyShout(npc.getPosition().getMapId(), npc.getNpcId(), shoutType)) {
			if (Rnd.get(stepCount) < 2) {
				List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(npc.getPosition().getMapId(),
						npc.getNpcId(), shoutType, null, 0);
				if (npc.getTarget() instanceof Creature) {
					GameFeatureServices.npcShoutsService().shout(npc, (Creature) npc.getTarget(), shouts, 0, false);
				} else {
					GameFeatureServices.npcShoutsService().shout(npc, null, shouts, 0, false);
				}
				shouts.clear();
			}
		}
	}

	/**
	 * 切换目标时触发 SWITCH_TARGET 喊话。
	 * Fires SWITCH_TARGET shouts when the target is switched.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @param creature 新目标 / new target
	 */
	public static void onSwitchedTarget(NpcAI2 npcAI, Creature creature) {
		Npc npc = npcAI.getOwner();
		if (DataManager.NPC_SHOUT_DATA.hasAnyShout(npc.getPosition().getMapId(), npc.getNpcId(),
				ShoutEventType.SWITCH_TARGET)) {
			List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(npc.getPosition().getMapId(),
					npc.getNpcId(), ShoutEventType.SWITCH_TARGET, null, 0);
			GameFeatureServices.npcShoutsService().shout(npc, creature, shouts, 0, false);
			shouts.clear();
		}
	}

	/**
	 * 死亡时触发 DIED 喊话。
	 * Fires DIED shouts on death.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void onDied(NpcAI2 npcAI) {
		Npc owner = npcAI.getOwner();
		if (DataManager.NPC_SHOUT_DATA.hasAnyShout(owner.getPosition().getMapId(), owner.getNpcId(),
				ShoutEventType.DIED)) {
			List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(owner.getPosition().getMapId(),
					owner.getNpcId(), ShoutEventType.DIED, null, 0);
			if (shouts.size() > 0) {
				GameFeatureServices.npcShoutsService().shout(owner, (Creature) owner.getTarget(), shouts, 0, false);
			}
			shouts.clear();
		}
	}

	/**
	 * 准备攻击时触发 ATTACK_BEGIN 喊话。
	 * Fires ATTACK_BEGIN shouts when the NPC is ready to attack.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @param creature 攻击目标 / attack target
	 */
	public static void onAttackBegin(NpcAI2 npcAI, Creature creature) {
		Npc npc = npcAI.getOwner();
		if (DataManager.NPC_SHOUT_DATA.hasAnyShout(npc.getPosition().getMapId(), npc.getNpcId(),
				ShoutEventType.ATTACK_BEGIN)) {
			List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(npc.getPosition().getMapId(),
					npc.getNpcId(), ShoutEventType.ATTACK_BEGIN, null, 0);
			GameFeatureServices.npcShoutsService().shout(npc, creature, shouts, 0, false);
			shouts.clear();
			return;
		}
	}

	/**
	 * 处理被攻击 / 求助喊话（首次受击时 ATTACKED 或 HELPCALL）。
	 * Handles attacked / help shouts (ATTACKED or HELPCALL on first hit).
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @param creature 攻击者 / attacker
	 */
	public static void onHelp(NpcAI2 npcAI, Creature creature) {
		Npc npc = npcAI.getOwner();
		if (npc.getAttackedCount() == 0) {
			if (DataManager.NPC_SHOUT_DATA.hasAnyShout(npc.getPosition().getMapId(), npc.getNpcId(),
					ShoutEventType.ATTACKED)) {
				List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(npc.getPosition().getMapId(),
						npc.getNpcId(), ShoutEventType.ATTACKED, null, 0);
				GameFeatureServices.npcShoutsService().shout(npc, creature, shouts, 0, false);
				shouts.clear();
				return;
			}
			ShoutEventType eventType = supportEventType(false);
			if (DataManager.NPC_SHOUT_DATA.hasAnyShout(npc.getPosition().getMapId(), npc.getNpcId(), eventType)) {
				List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(npc.getPosition().getMapId(),
						npc.getNpcId(), eventType, null, 0);
				GameFeatureServices.npcShoutsService().shout(npc, creature, shouts, 0, false);
				shouts.clear();
			}
		}
	}

	/**
	 * 友军响应求助时触发 HELP 喊话。
	 * Fires HELP shouts when an ally responds to a support request.
	 */
	public static void onSupport(NpcAI2 npcAI, Creature target) {
		Npc npc = npcAI.getOwner();
		ShoutEventType eventType = supportEventType(true);
		if (DataManager.NPC_SHOUT_DATA.hasAnyShout(npc.getPosition().getMapId(), npc.getNpcId(), eventType)) {
			List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(npc.getPosition().getMapId(), npc.getNpcId(),
					eventType, null, 0);
			GameFeatureServices.npcShoutsService().shout(npc, target, shouts, 0, false);
			shouts.clear();
		}
	}

	static ShoutEventType supportEventType(boolean responder) {
		return responder ? ShoutEventType.HELP : ShoutEventType.HELPCALL;
	}

	/**
	 * 处理 NPC 对 NPC 攻击时的 SAY 类型 ATTACKED 喊话。
	 * Handles SAY-type ATTACKED shouts for NPC-vs-NPC attacks.
	 * <p>
	 * 所有此类喊话必须为 SAY 类型。
	 * All such shouts must be of type SAY.
	 * </p>
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @param target 被攻击目标 / attack target
	 */
	public static void onEnemyAttack(NpcAI2 npcAI, Creature target) {
		final Npc npc = npcAI.getOwner();
		if (!DataManager.NPC_SHOUT_DATA.hasAnyShout(npc.getPosition().getMapId(), npc.getNpcId(),
				ShoutEventType.ATTACKED)) {
			return;
		}
		List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(npc.getPosition().getMapId(), npc.getNpcId(),
				ShoutEventType.ATTACKED, null, 0);

		List<NpcShout> finalShouts = new ArrayList<NpcShout>();
		for (NpcShout s : shouts) {
			if (s.getShoutType() == ShoutType.SAY) {
				finalShouts.add(s);
			}
		}

		if (finalShouts.size() == 0) {
			return;
		}
		int randomShout = Rnd.get(finalShouts.size());
		final NpcShout shout = finalShouts.get(randomShout);
		finalShouts.clear();
		shouts.clear();

		if (!npc.mayShout(shout.getPollDelay() / 1000)) {
			return;
		}
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				Iterator<Player> iter = npc.getKnownList().getKnownPlayers().values().iterator();
				while (iter.hasNext()) {
					Player kObj = iter.next();
					if (kObj.getLifeStats().isAlreadyDead()) {
						return;
					}
					GameFeatureServices.npcShoutsService().shout(npc, kObj, shout, shout.getPollDelay() / 1000);
				}
			}
		}, 0);
	}

	/**
	 * 施法时触发 CAST_K 数值型喊话。
	 * Fires CAST_K numeric shouts when casting.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @param creature 目标生物 / target creature
	 */
	public static void onCast(NpcAI2 npcAI, Creature creature) {
		handleNumericEvent(npcAI, creature, ShoutEventType.CAST_K);
	}

	/**
	 * 攻击目标时触发 ATTACK_K 数值型喊话。
	 * Fires ATTACK_K numeric shouts when attacking a target.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @param creature 攻击目标 / attack target
	 */
	public static void onAttack(NpcAI2 npcAI, Creature creature) {
		handleNumericEvent(npcAI, creature, ShoutEventType.ATTACK_K);
	}

	/**
	 * 处理按技能编号匹配的数值型喊话事件。
	 * Handles numeric shout events matched by skill number.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @param creature 目标生物 / target creature
	 * @param eventType 喊话事件类型 / shout event type
	 */
	private static void handleNumericEvent(NpcAI2 npcAI, Creature creature, ShoutEventType eventType) {
		Npc owner = npcAI.getOwner();
		List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(owner.getPosition().getMapId(),
				owner.getNpcId(), eventType, null, 0);
		if (shouts == null) {
			return;
		}
		List<NpcShout> validShouts = new ArrayList<NpcShout>();
		List<NpcShout> nonNumberedShouts = new ArrayList<NpcShout>();
		for (NpcShout shout : shouts) {
			if (shout.getSkillNo() == 0) {
				nonNumberedShouts.add(shout);
			} else if (shout.getSkillNo() == owner.getSkillNumber()) {
				validShouts.add(shout);
			}
		}

		if (validShouts.size() == 0) {
			validShouts.clear();
			validShouts = nonNumberedShouts;
		} else {
			nonNumberedShouts.clear();
		}

		if (validShouts.size() > 0) {
			GameFeatureServices.npcShoutsService().shout(owner, creature, validShouts, 0, false);
		}

		validShouts.clear();
		shouts.clear();
	}

	/**
	 * 攻击结束时触发 ATTACK_END 喊话。
	 * Fires ATTACK_END shouts when the attack sequence ends.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void onAttackEnd(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		if (DataManager.NPC_SHOUT_DATA.hasAnyShout(npc.getPosition().getMapId(), npc.getNpcId(),
				ShoutEventType.ATTACK_END)) {
			List<NpcShout> shouts = DataManager.NPC_SHOUT_DATA.getNpcShouts(npc.getPosition().getMapId(),
					npc.getNpcId(), ShoutEventType.ATTACK_END, null, 0);
			GameFeatureServices.npcShoutsService().shout(npc, null, shouts, 0, false);
			shouts.clear();
		}
	}
}
