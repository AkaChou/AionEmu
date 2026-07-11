package com.aionemu.gameserver.ai.walkers;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.handler.MoveEventHandler;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.utils.MathUtil;
import org.apache.commons.lang3.ArrayUtils;

/**
 * 巡逻行走 NPC AI：Polor Serin（@AIName "polorserin"），继承 WalkGeneralRunnerAI2。
 * Walker patrol NPC AI: Polor Serin (@AIName "polorserin"), extends WalkGeneralRunnerAI2.
 *
 * @author Encom
 */
@AIName("polorserin")
public class PolorSerinAI2 extends WalkGeneralRunnerAI2
{
	static final int[] stopAdults = { 203129, 203132 };
	
	@Override
	protected void handleMoveArrived() {
		boolean adultsNear = false;
		for (VisibleObject object : getOwner().getKnownList().getKnownObjectsSnapshot()) {
			if (object instanceof Npc) {
				Npc npc = (Npc)object;
				if (!ArrayUtils.contains(stopAdults, npc.getNpcId()))
					continue;
				if (MathUtil.isIn3dRange(npc, getOwner(), getOwner().getAggroRange())) {
					adultsNear = true;
					break;
				}
			}
		} if (adultsNear) {
			MoveEventHandler.onMoveArrived(this);
			getOwner().unsetState(CreatureState.WEAPON_EQUIPPED);
		} else {
			super.handleMoveArrived();
			getOwner().setState(CreatureState.WEAPON_EQUIPPED);
		}
	}
}
