package com.aionemu.gameserver.ai.worlds.cygnea;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.poll.AIAnswer;
import com.aionemu.gameserver.ai2.poll.AIAnswers;
import com.aionemu.gameserver.ai2.poll.AIQuestion;

/**
 * Cygnea 区域 NPC AI：Exploding Pearl（@AIName "exploding_pearl"），继承 AggressiveNpcAI2。
 * Cygnea zone NPC AI: Exploding Pearl (@AIName "exploding_pearl"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("exploding_pearl")
public class Exploding_PearlAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			GameEngineServices.skillEngine().getSkill(getOwner(), 19937, 46, getOwner()).useNoAnimationSkill();
			startLifeTask();
		}, 1000);
	}

	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(() -> AI2Actions.deleteOwner(Exploding_PearlAI2.this), 10000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}

	@Override
	protected AIAnswer pollInstance(AIQuestion question) {
        return switch (question) {
            case SHOULD_DECAY -> AIAnswers.NEGATIVE;
            case SHOULD_RESPAWN -> AIAnswers.NEGATIVE;
            case SHOULD_REWARD -> AIAnswers.NEGATIVE;
            default -> null;
        };
	}

	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
