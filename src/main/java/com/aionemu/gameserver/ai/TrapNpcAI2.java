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
 *
 * @author Encom
 */
@AIName("trap")
public class TrapNpcAI2 extends NpcAI2
{
	private int sensoryRange = 0;
	private Future<?> despawnTask;
	public static int EVENT_SET_TRAP_RANGE = 1;
	
	/**
	 * 处理看见生物事件。
	 * Handle seeing a creature.
	 *
	 * creature
	 */
	@Override
	protected void handleCreatureSee(Creature creature) {
		super.handleCreatureSee(creature);
		tryActivateTrap(creature);
	}
	
	/**
	 * 处理生物移动事件。
	 * Handle creature-moved.
	 *
	 * creature
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
	
	/**
	 * 处理自定义事件。
	 * Handle custom event.
	 *
	 * event id
	 * @param args 附加参数 / extra args
	 */
	@Override
	protected void handleCustomEvent(int eventId, Object... args) {
		if (eventId == EVENT_SET_TRAP_RANGE) {
			String ownerName = getObjectTemplate().getName().toLowerCase();
			if (ownerName.equals("snare trap")
			    || ownerName.equals("caltrop")
				|| ownerName.equals("spike trap")
				|| ownerName.equals("shock trap")
				|| ownerName.equals("sleep trap")
				|| ownerName.equals("blazing trap")
				|| ownerName.equals("explosion trap")
				|| ownerName.equals("specter's trap")
		        || ownerName.equals("explosive trap")
				|| ownerName.equals("poisoning trap")
				|| ownerName.equals("sandstorm trap")
				|| ownerName.equals("trap of slowing")
				|| ownerName.equals("trap of silence")
				|| ownerName.equals("propelling trap")
				|| ownerName.equals("spike bite trap")
				|| ownerName.equals("EV_RA_N_Light_SleepingTrap_G1_NPC")
				|| ownerName.equals("EV_RA_N_Dark_SleepingTrap_G1_NPC")) {
				sensoryRange = 4;
			} else if (ownerName.equals("trap")
				|| ownerName.equals("web trap")
				|| ownerName.equals("ice trap")
				|| ownerName.equals("storm mine")
				|| ownerName.equals("swamp trap")
				|| ownerName.equals("flame trap")
				|| ownerName.equals("sticky trap")
				|| ownerName.equals("skybound trap")
				|| ownerName.equals("paralyze trap")
				|| ownerName.equals("protect symbol")
				|| ownerName.equals("drakan net trap")
				|| ownerName.equals("thornburst trap")
				|| ownerName.equals("thorntwist trap")
				|| ownerName.equals("drakan mine trap")
				|| ownerName.equals("symbol of spirit")
				|| ownerName.equals("destruction trap")
				|| ownerName.equals("unidentified trap")
				|| ownerName.equals("symbol of solidity")
				|| ownerName.equals("symbol of recovery")
				|| ownerName.equals("symbol of protection")
				|| ownerName.equals("trap of clairvoyance")
				|| ownerName.equals("symbol of castle wall")
				|| ownerName.equals("scrapped mechanisms")
				|| ownerName.equals("trap of infernal blaze")
				|| ownerName.equals("Highdeva_Fire_NPC_L_G1")
				|| ownerName.equals("Highdeva_Fire_NPC_D_G1")
				|| ownerName.equals("IDEvent_Solo_Paralyze_NPC")) {
				sensoryRange = 10;
			}
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
