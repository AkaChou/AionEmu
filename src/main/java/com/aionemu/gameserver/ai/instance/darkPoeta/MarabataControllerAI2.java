package com.aionemu.gameserver.ai.instance.darkPoeta;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * Dark Poeta 副本 NPC AI：Marabata Controller（@AIName "marabatacontroller"），继承 NpcAI2。
 * Dark Poeta instance NPC AI: Marabata Controller (@AIName "marabatacontroller"), extends NpcAI2.
 * @author Encom
 */
@AIName("marabatacontroller")
public class MarabataControllerAI2 extends NpcAI2
{
	private Npc getBoss() {
		Npc npc = switch (getNpcId()) {
			case 700443, 700444, 700442 -> getPosition().getWorldMapInstance().getNpc(214850);
			case 700446, 700447, 700445 -> getPosition().getWorldMapInstance().getNpc(214851);
			case 700440, 700441, 700439 -> getPosition().getWorldMapInstance().getNpc(214849);
			default -> null;
		};
		return npc;
	}

	private void applyEffect(boolean remove) {
		Npc boss = getBoss();
		if (boss != null && !boss.getLifeStats().isAlreadyDead()) {
			switch (getNpcId()) {
				case 700443:
				case 700446:
				case 700440:
				    if (remove) {
					    boss.getEffectController().removeEffect(18556);
				    } else {
					    boss.getController().useSkill(18556);
				    }
				break;
				case 700444:
				case 700447:
				case 700441:
				case 700442:
				case 700445:
				case 700439:
				    if (remove) {
					    boss.getEffectController().removeEffect(18110);
				    } else {
					    boss.getController().useSkill(18110);
				    }
				break;
			}
		}
	}

	@Override
	protected void handleDied() {
		super.handleDied();
		applyEffect(true);
		AI2Actions.deleteOwner(this);
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		applyEffect(false);
		GameThreadPoolServices.threadPoolManager().schedule(() -> useSkill(), 2000);
	}

	private void useSkill() {
		if (isAlreadyDead()) {
			return;
		}
		AI2Actions.targetSelf(this);
		int skill = switch (getNpcId()) {
            case 700443, 700446, 700440 -> 18554;
            case 700444, 700447, 700441 -> 18555;
            case 700442, 700445, 700439 -> 18553;
            default -> 0;
        };
        AI2Actions.useSkill(this, skill);
	}
}
