package com.aionemu.gameserver.ai2.follow;

import java.util.concurrent.Future;
import java.util.function.IntUnaryOperator;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.controllers.SiegeWeaponController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.summons.SummonMode;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 召唤物跟随任务 AI：周期性检查与主人/目标距离，过远解散、离目标则移动、到达则攻击。
 * Summon follow task AI: periodically checks master/target range, releases if too far, moves when out of range, attacks on arrival.
 */
public class FollowSummonTaskAI implements Runnable {

	private static final float DEFAULT_TARGET_RANGE = 2f;
	private static final float SKILL_RANGE_PADDING = 2f;

	private Creature target;
	private Summon summon;
	private Player master;
	private float targetX;
	private float targetY;
	private float targetZ;
	private Future<?> task;

	/**
	 * 创建召唤物跟随任务，绑定目标、召唤物与主人，并记录目标坐标。
	 * Creates a summon follow task bound to target, summon, and master, and records leading coordinates.
	 *
	 * @param target 跟随/攻击目标 / follow/attack target
	 * @param summon 跟随召唤物 / Following summon
	 */
	public FollowSummonTaskAI(Creature target, Summon summon) {
		this.target = target;
		this.summon = summon;
		this.master = summon.getMaster();
		task = summon.getMaster().getController().getTask(TaskId.SUMMON_FOLLOW);
		setLeadingCoordinates();
	}

	/**
	 * 缓存目标当前坐标作为领航坐标。
	 * Caches the target's current coordinates as leading coordinates.
	 */
	private void setLeadingCoordinates() {
		targetX = target.getX();
		targetY = target.getY();
		targetZ = target.getZ();
	}

	/**
	 * 周期执行：校验引用、主人距离、目标距离；必要时解散、移动或攻击。
	 * Periodic run: validates refs, master range, and target range; releases, moves, or attacks as needed.
	 */
	@Override
	public void run() {
		if (target == null || summon == null || master == null) {
			if (task != null) {
				task.cancel(true);
			}
			return;
		}
		if (master.isUsingFlyTeleport()) {
			return;
		}
		if (!isInMasterRange()) {
			SummonsService.doMode(SummonMode.RELEASE, summon, UnsummonType.DISTANCE);
			return;
		}
		if (!isInTargetRange()) {
			if (targetX != target.getX() || targetY != target.getY() || targetZ != target.getZ()) {
				setLeadingCoordinates();
				onOutOfTargetRange();
			}
		} else if (!master.equals(target)) {
			onDestination();
		}
	}

	/**
	 * 判断召唤物是否在目标允许射程内。
	 * Returns whether the summon is within the allowed target range.
	 *
	 * @return 在射程内为 {@code true} / {@code true} if in target range
	 */
	private boolean isInTargetRange() {
		return MathUtil.isIn3dRange(target, summon, targetRange());
	}

	/**
	 * 计算目标射程：攻城武器按技能第一目标距离，其它使用默认值。
	 * Computes target range: siege weapons use skill first-target range, others use the default.
	 *
	 * @return 目标射程 / target range
	 */
	private float targetRange() {
		if (summon.getController() instanceof SiegeWeaponController controller) {
			return targetRangeFor(!master.equals(target), controller.getNpcSkillTemplates(), FollowSummonTaskAI::skillFirstTargetRange);
		}
		return DEFAULT_TARGET_RANGE;
	}

	/**
	 * 按是否攻击目标与 NPC 技能模板计算射程。
	 * Computes range based on whether attacking a target and the NPC skill templates.
	 *
	 * @param attackTarget 是否攻击目标（非跟随主人） / whether attacking a target (not following master)
	 * @param npcSkillTemplates NPC 技能模板 / NPC skill templates
	 * @param skillRangeProvider 技能射程提供者 / skill-range provider
	 * @return 目标射程 / target range
	 */
	static float targetRangeFor(boolean attackTarget, NpcSkillTemplates npcSkillTemplates, IntUnaryOperator skillRangeProvider) {
		if (!attackTarget) {
			return DEFAULT_TARGET_RANGE;
		}
		return siegeWeaponTargetRange(npcSkillTemplates, skillRangeProvider);
	}

	/**
	 * 攻城武器目标射程：取首个技能的第一目标距离并加边距。
	 * Siege-weapon target range: first skill's first-target range plus padding.
	 *
	 * @param npcSkillTemplates NPC 技能模板 / NPC skill templates
	 * @param skillRangeProvider 技能射程提供者 / skill-range provider
	 * @return 目标射程 / target range
	 */
	static float siegeWeaponTargetRange(NpcSkillTemplates npcSkillTemplates, IntUnaryOperator skillRangeProvider) {
		if (npcSkillTemplates == null || npcSkillTemplates.getNpcSkills() == null || npcSkillTemplates.getNpcSkills().isEmpty()) {
			return DEFAULT_TARGET_RANGE;
		}
		NpcSkillTemplate npcSkill = npcSkillTemplates.getNpcSkills().get(0);
		if (npcSkill == null) {
			return DEFAULT_TARGET_RANGE;
		}
		int skillRange = skillRangeProvider.applyAsInt(npcSkill.getSkillid());
		if (skillRange <= 0) {
			return DEFAULT_TARGET_RANGE;
		}
		return Math.max(DEFAULT_TARGET_RANGE, skillRange + SKILL_RANGE_PADDING);
	}

	/**
	 * 从技能数据读取第一目标射程。
	 * Reads first-target range from skill data.
	 *
	 * @param skillId 技能 ID / skill id
	 * @return 第一目标射程，失败为 0 / first-target range, or 0 on failure
	 */
	private static int skillFirstTargetRange(int skillId) {
		if (DataManager.SKILL_DATA == null) {
			return 0;
		}
		SkillTemplate skillTemplate = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (skillTemplate == null || skillTemplate.getProperties() == null) {
			return 0;
		}
		return skillTemplate.getProperties().getFirstTargetRange();
	}

	/**
	 * 判断召唤物是否在主人 50 码内。
	 * Returns whether the summon is within 50 units of the master.
	 *
	 * @return 在主人范围内为 {@code true} / {@code true} if within master range
	 */
	private boolean isInMasterRange() {
		return MathUtil.isIn3dRange(master, summon, 50);
	}

	/**
	 * 到达目标射程后触发对目标的攻击事件。
	 * Fires an ATTACK creature event when within target range.
	 */
	protected void onDestination() {
		summon.getAi2().onCreatureEvent(AIEventType.ATTACK, target);
	}

	/**
	 * 目标移出射程时触发移动校验事件。
	 * Fires MOVE_VALIDATE when the target moves out of range.
	 */
	private void onOutOfTargetRange() {
		summon.getAi2().onGeneralEvent(AIEventType.MOVE_VALIDATE);
	}
}
