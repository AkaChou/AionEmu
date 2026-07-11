package com.aionemu.gameserver.ai.walkers;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;

/**
 * 巡逻行走 NPC AI：Walk General Runner（@AIName "general_runner"），继承 GeneralNpcAI2。
 * Walker patrol NPC AI: Walk General Runner (@AIName "general_runner"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("general_runner")
public class WalkGeneralRunnerAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleMoveArrived() {
		super.handleMoveArrived();
		getOwner().setState(CreatureState.WEAPON_EQUIPPED);
	}
}
