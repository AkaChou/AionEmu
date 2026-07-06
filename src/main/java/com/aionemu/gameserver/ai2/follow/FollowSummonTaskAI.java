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

	public FollowSummonTaskAI(Creature target, Summon summon) {
		this.target = target;
		this.summon = summon;
		this.master = summon.getMaster();
		task = summon.getMaster().getController().getTask(TaskId.SUMMON_FOLLOW);
		setLeadingCoordinates();
	}

	private void setLeadingCoordinates() {
		targetX = target.getX();
		targetY = target.getY();
		targetZ = target.getZ();
	}

	@Override
	public void run() {
		if (target == null || summon == null || master == null) {
			if (task != null) {
				task.cancel(true);
			}
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

	private boolean isInTargetRange() {
		return MathUtil.isIn3dRange(target, summon, targetRange());
	}

	private float targetRange() {
		if (summon.getController() instanceof SiegeWeaponController controller) {
			return targetRangeFor(!master.equals(target), controller.getNpcSkillTemplates(), FollowSummonTaskAI::skillFirstTargetRange);
		}
		return DEFAULT_TARGET_RANGE;
	}

	static float targetRangeFor(boolean attackTarget, NpcSkillTemplates npcSkillTemplates, IntUnaryOperator skillRangeProvider) {
		if (!attackTarget) {
			return DEFAULT_TARGET_RANGE;
		}
		return siegeWeaponTargetRange(npcSkillTemplates, skillRangeProvider);
	}

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

	private boolean isInMasterRange() {
		return MathUtil.isIn3dRange(master, summon, 50);
	}

	protected void onDestination() {
		summon.getAi2().onCreatureEvent(AIEventType.ATTACK, target);
	}

	private void onOutOfTargetRange() {
		summon.getAi2().onGeneralEvent(AIEventType.MOVE_VALIDATE);
	}
}
