package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.follow.FollowStartService;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates;

/**
 * 攻城兵器召唤物控制器，扩展跟随与攻击模式逻辑。
 * Siege weapon summon controller that extends follow and attack-mode logic.
 */
public class SiegeWeaponController extends SummonController {

	/** NPC 技能模板攻城武器 / NPC skill templates for this siege weapon */
	private NpcSkillTemplates skills;

	/**
	 * 根据 NPC 模板 ID 构造攻城兵器控制器。
	 * Constructs a siege weapon controller from an NPC template id.
	 *
	 * NPC 模板 ID / NPC template id
	 */
	public SiegeWeaponController(int npcId) {
		skills = DataManager.NPC_SKILL_DATA.getNpcSkillList(npcId);
	}

	/**
	 * 解除召唤时取消跟随任务并中止移动。
	 * On release, cancels the follow task and aborts movement.
	 *
	 * @param unsummonType 解除召唤类型 / unsummon type
	 */
	@Override
	public void release(final UnsummonType unsummonType) {
		getMaster().getController().cancelTask(TaskId.SUMMON_FOLLOW);
		getOwner().getMoveController().abortMove();
		super.release(unsummonType);
	}

	/**
	 * 进入休息模式并停止跟随主人。
	 * Enters rest mode and stops following the master.
	 */
	@Override
	public void restMode() {
		getMaster().getController().cancelTask(TaskId.SUMMON_FOLLOW);
		super.restMode();
		getOwner().getAi2().onCreatureEvent(AIEventType.STOP_FOLLOW_ME, getMaster());
	}

	/**
	 * 进入未知模式并取消跟随任务。
	 * Enters the unknown mode and cancels the follow task.
	 */
	@Override
	public void setUnkMode() {
		super.setUnkMode();
		getMaster().getController().cancelTask(TaskId.SUMMON_FOLLOW);
	}

	/**
	 * 进入守卫模式并开始跟随主人。
	 * Enters guard mode and starts following the master.
	 */
	@Override
	public final void guardMode() {
		super.guardMode();
		getMaster().getController().cancelTask(TaskId.SUMMON_FOLLOW);
		getOwner().setTarget(getMaster());
		getOwner().getAi2().onCreatureEvent(AIEventType.FOLLOW_ME, getMaster());
		getOwner().getMoveController().moveToTargetObject();
		getMaster().getController().addTask(TaskId.SUMMON_FOLLOW,
				FollowStartService.newFollowingToTargetCheckTask(getOwner(), getMaster()));
	}

	/**
	 * 进入攻击模式并跟随指定目标。
	 * Enters attack mode and follows the specified target.
	 *
	 * target object id
	 */
	@Override
	public void attackMode(int targetObjId) {
		super.attackMode(targetObjId);
		Creature target = (Creature) getOwner().getKnownList().getObject(targetObjId);
		if (target == null) {
			return;
		}
		getOwner().setTarget(target);
		getOwner().getAi2().onCreatureEvent(AIEventType.FOLLOW_ME, target);
		getOwner().getMoveController().moveToTargetObject();
		getMaster().getController().addTask(TaskId.SUMMON_FOLLOW,
				FollowStartService.newFollowingToTargetCheckTask(getOwner(), target));
	}

	/**
	 * 死亡时取消跟随任务并执行召唤物死亡逻辑。
	 * On death, cancels the follow task and runs summon death logic.
	 *
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	@Override
	public void onDie(final Creature lastAttacker) {
		getMaster().getController().cancelTask(TaskId.SUMMON_FOLLOW);
		super.onDie(lastAttacker);
	}

	/**
	 * 获取攻城兵器的 NPC 技能模板。
	 * Gets the NPC skill templates for this siege weapon.
	 *
	 * NPC skill templates
	 */
	public NpcSkillTemplates getNpcSkillTemplates() {
		return skills;
	}
}
