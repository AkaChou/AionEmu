package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.ai.BombTemplate;

/**
 * 炸弹 AI：延迟后对周围造成爆炸伤害并消失。
 * Bomb AI that explodes after a delay, damages nearby targets, and despawns.
 *
 * @author Encom
 */
@AIName("bomb")
public class BombAi2 extends AggressiveNpcAI2
{
	private BombTemplate template;
	
	/**
	 * 处理生成完成事件。
	 * Handle post-spawn.
	 */
	@Override
	protected void handleSpawned() {
		bombSkill();
	}
	
	private void bombSkill() {
		template = DataManager.AI_DATA.getAiTemplate().get(getNpcId()).getBombs().getBombTemplate();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				useSkill(template.getSkillId());
			}
		}, template.getCd());
	}
	
	private void useSkill(int skill) {
		AI2Actions.targetSelf(this);
		AI2Actions.useSkill(this, skill);
		int duration = DataManager.SKILL_DATA.getSkillTemplate(skill).getDuration();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(BombAi2.this);
			}
		}, duration != 0 ? duration + 1000 : 0);
	}
}
