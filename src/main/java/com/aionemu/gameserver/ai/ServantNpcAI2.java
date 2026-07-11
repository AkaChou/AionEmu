package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.poll.AIAnswer;
import com.aionemu.gameserver.ai2.poll.AIAnswers;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.NpcObjectType;
import com.aionemu.gameserver.model.skill.NpcSkillEntry;

import java.util.concurrent.Future;

/**
 * 随从/召唤物 AI：跟随主人并协助战斗。
 * Servant/summon AI that follows its master and assists in combat.
 *
 * @author Encom
 */
@AIName("servant")
public class ServantNpcAI2 extends GeneralNpcAI2
{
	/**
	 * 执行 AI 思考循环（状态机 tick）。
	 * Run one AI think cycle (state-machine tick).
	 */
	@Override
	public void think() {
	}
	
	/**
	 * 处理生成完成事件。
	 * Handle post-spawn.
	 */
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		if (getCreator() != null) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					if (getOwner().getNpcObjectType() != NpcObjectType.TOTEM) {
						AI2Actions.targetCreature(ServantNpcAI2.this, (Creature) getCreator().getTarget());
					} else {
						AI2Actions.targetSelf(ServantNpcAI2.this);
					}
					healOrAttack();
				}
			}, 200);
		}
	}
	
	private void healOrAttack() {
		if (skillId == 0) {
			NpcSkillEntry npcSkill = getSkillList().getRandomSkill();
			if (npcSkill == null)
				return;
			skillId = npcSkill.getSkillId();
		}
		int duration = getOwner().getNpcObjectType() == NpcObjectType.TOTEM ? 3000 : 5000;
		Future<?> task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				getOwner().getController().useSkill(skillId, 1);
			}
		}, 1000, duration);
		getOwner().getController().addTask(TaskId.SKILL_USE, task);
	}
	
	@Override
	protected AIAnswer pollInstance(AIQuestion question) {
		switch (question) {
			case SHOULD_DECAY:
				return AIAnswers.NEGATIVE;
			case SHOULD_RESPAWN:
				return AIAnswers.NEGATIVE;
			case SHOULD_REWARD:
				return AIAnswers.NEGATIVE;
			default:
				return null;
		}
	}
}
