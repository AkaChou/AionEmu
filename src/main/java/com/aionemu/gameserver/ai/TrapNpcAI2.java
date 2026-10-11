package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.model.skill.NpcSkillEntry;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.concurrent.Future;

/**
 * 陷阱 AI：生成后按规则对进入范围的目标生效。
 * Trap AI that applies effects to targets entering its range after spawn.
 * @author Encom
 */
@AIName("trap")
public class TrapNpcAI2 extends NpcAI2
{
	private Future<?> despawnTask;

	/**
	 * 处理看见生物事件。
	 * Handle seeing a creature.
	 * @param creature 生物 / creature
	 */
	@Override
	protected void handleCreatureSee(Creature creature) {
		super.handleCreatureSee(creature);
		tryActivateTrap(creature);
	}
	
	/**
	 * 处理生物移动事件。
	 * Handle creature-moved.
	 * @param creature 移动的生物 / moved creature
	 */
	@Override
	protected void handleCreatureMoved(Creature creature) {
		super.handleCreatureMoved(creature);
		tryActivateTrap(creature);
	}
	
	private void tryActivateTrap(Creature creature) {
		if (despawnTask != null) {
			return;
		} if (!creature.getLifeStats().isAlreadyDead() &&
		      !creature.isInVisualState(CreatureVisualState.BLINKING) && isInRange(creature, getOwner().getAggroRange())) {
			Creature creator = (Creature) getCreator();
			if (!creator.isEnemy(creature)) {
				return;
			}
			explode(creature);
		}
	}
	
	private void explode(Creature creature) {
		if (setStateIfNot(AIState.FIGHT)) {
			getOwner().unsetVisualState(CreatureVisualState.HIDE1);
			PacketSendUtility.broadcastPacket(getOwner(), new SM_PLAYER_STATE(getOwner()));
			AI2Actions.targetCreature(this, creature);
			NpcSkillEntry npcSkill = getSkillList().getRandomSkill();
			if (npcSkill != null) {
				AI2Actions.useSkill(this, npcSkill.getSkillId());
			}
			despawnTask = GameThreadPoolServices.threadPoolManager().schedule(new TrapDelete(this), 5000);
		}
	}
	
	/**
	 * 是否支持移动。
	 * Whether movement is supported.
	 */
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	private static final class TrapDelete implements Runnable {
		private TrapNpcAI2 ai;
		
		TrapDelete(TrapNpcAI2 ai) {
			this.ai = ai;
		}
		
		@Override
		public void run() {
			AI2Actions.deleteOwner(ai);
			ai = null;
		}
	}
}
