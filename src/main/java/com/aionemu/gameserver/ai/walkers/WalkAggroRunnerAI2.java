package com.aionemu.gameserver.ai.walkers;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;

/**
 * 巡逻行走 NPC AI：Walk Aggro Runner（@AIName "aggro_runner"），继承 AggressiveNpcAI2。
 * Walker patrol NPC AI: Walk Aggro Runner (@AIName "aggro_runner"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("aggro_runner")
public class WalkAggroRunnerAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleMoveArrived() {
		super.handleMoveArrived();
		getOwner().setState(CreatureState.WEAPON_EQUIPPED);
	}
}
