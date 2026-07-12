package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.controllers.attack.AttackResult;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplateType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Collections;
import java.util.function.BiPredicate;

/**
 * 仇恨 / 支援事件处理器，负责 NPC 进入仇恨、友方支援与守卫反击。
 * Handles aggro and support events: NPC aggro entry, ally support, and guard counter-attacks.
 *
 * @author ATracer
 */
public class AggroEventHandler {

	/**
	 * 处理 NPC 对目标产生仇恨：过滤管理员中立/敌意，广播攻击包，并延迟通知周围友方。
	 * Handles NPC aggro on a target: filters admin neutral/enmity, broadcasts attack packet, and schedules ally notification.
	 *
	 * NPC AI instance
	 * @param myTarget 仇恨目标生物 / creature being aggroed
	 */
	public static void onAggro(NpcAI2 npcAI, final Creature myTarget) {
		final Npc owner = npcAI.getOwner();
		if (myTarget.getAdminNeutral() == 1 || myTarget.getAdminNeutral() == 3 || myTarget.getAdminEnmity() == 1
				|| myTarget.getAdminEnmity() == 3) {
			return;
		}
		PacketSendUtility.broadcastPacket(owner, new SM_ATTACK(owner, myTarget, 0, 633, 0,
				Collections.singletonList(new AttackResult(0, AttackStatus.NORMALHIT))));

		GameThreadPoolServices.threadPoolManager().schedule(new AggroNotifier(owner, myTarget, true), 500);
	}

	/**
	 * 处理友方请求支援：在支援范围内且视线可达时，协助攻击其目标。
	 * Handles ally support request: assists attacking the ally's target when in range and with line of sight.
	 *
	 * NPC AI instance
	 *
	 * @param notMyTarget 请求支援的友方生物 / ally creature requesting support
	 * @param notMyTarget
	 * @return 是否成功进入支援 / whether support was engaged
	 */
	public static boolean onCreatureNeedsSupport(NpcAI2 npcAI, Creature notMyTarget) {
		Npc owner = npcAI.getOwner();
		VisibleObject myTarget = notMyTarget.getTarget();
		if (myTarget instanceof Creature) {
			Creature targetCreature = (Creature) myTarget;
			if (canReceiveSupport(owner, notMyTarget, targetCreature, owner.getAggroRange(),
					GameWorldServices.geoService()::canSee)) {
				if (npcAI.poll(AIQuestion.CAN_SHOUT)) {
					ShoutEventHandler.onSupport(npcAI, targetCreature);
				}
				PacketSendUtility.broadcastPacket(owner, new SM_ATTACK(owner, targetCreature, 0, 633, 0,
						Collections.singletonList(new AttackResult(0, AttackStatus.NORMALHIT))));
				GameThreadPoolServices.threadPoolManager().schedule(new AggroNotifier(owner, targetCreature, false), 500);
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断是否可对友方提供支援（友方关系、距离、双方视线）。
	 * Checks whether support can be provided (ally relation, distance, mutual line of sight).
	 *
	 * supporting NPC
	 * @param notMyTarget 请求支援的友方 / ally requesting support
	 * @param targetCreature 友方当前目标 / ally's current target
	 * support range
	 * @param canSee 视线判定谓词 / line-of-sight predicate
	 * @return 是否可支援 / whether support is allowed
	 */
	static boolean canReceiveSupport(Npc owner, Creature notMyTarget, Creature targetCreature, float supportRange,
			BiPredicate<VisibleObject, VisibleObject> canSee) {
		return notMyTarget.isSupportFrom(owner)
				&& MathUtil.isIn3dRange(owner, notMyTarget, supportRange)
				&& canSee.test(owner, notMyTarget)
				&& canSee.test(owner, targetCreature);
	}

	/**
	 * 守卫反击：当守卫发现攻击者正在攻击非敌对玩家时，对攻击者建立仇恨。
	 * Guard counter-attack: starts hate on an attacker who is targeting a non-enemy player.
	 *
	 * NPC AI instance
	 * attacker
	 *
	 * @return 是否成功建立守卫仇恨 / whether guard hate was started
	 */
	public static boolean onGuardAgainstAttacker(NpcAI2 npcAI, Creature attacker) {
		Npc owner = npcAI.getOwner();
		TribeClass tribe = owner.getTribe();
		if (!tribe.isGuard() && owner.getObjectTemplate().getNpcTemplateType() != NpcTemplateType.GUARD) {
			return false;
		}
		VisibleObject target = attacker.getTarget();
		if (target != null && target instanceof Player) {
			Player playerTarget = (Player) target;
			if (!owner.isEnemy(playerTarget) && owner.isEnemy(attacker)
					&& MathUtil.isInRange(owner, playerTarget, owner.getAggroRange())
					&& GameWorldServices.geoService().canSee(owner, attacker)) {
				owner.getAggroList().startHate(attacker);
				return true;
			}
		}
		return false;
	}

	/**
	 * 延迟仇恨通知任务：向目标添加仇恨，并可选广播周围 NPC 的支援事件。
	 * Delayed aggro notification: adds hate to the target and optionally broadcasts support events to nearby NPCs.
	 */
	private static final class AggroNotifier implements Runnable {

		private Npc aggressive;
		private Creature target;
		private boolean broadcast;

		/**
		 * 构造延迟仇恨通知任务。
		 * Creates a delayed aggro notification task.
		 *
		 * @param aggressive 发起仇恨的 NPC / NPC generating aggro
		 * hate target
		 * @param broadcast 是否广播支援事件 / whether to broadcast support events
		 */
		AggroNotifier(Npc aggressive, Creature target, boolean broadcast) {
			this.aggressive = aggressive;
			this.target = target;
			this.broadcast = broadcast;
		}

		@Override
		public void run() {
			aggressive.getAggroList().addHate(target, 1);
			if (broadcast) {
				aggressive.getKnownList().doOnAllNpcs(new Visitor<Npc>() {

					@Override
					public void visit(Npc object) {
						object.getAi2().onCreatureEvent(AIEventType.CREATURE_NEEDS_SUPPORT, aggressive);
					}
				});
			}
			aggressive = null;
			target = null;
		}
	}
}
