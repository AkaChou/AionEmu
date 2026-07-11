package com.aionemu.gameserver.ai.worlds.reshanta.abyssLanding;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Reshanta 区域 NPC AI：Spirit Monument（@AIName "spirit_monument"），继承 NpcAI2。
 * Reshanta zone NPC AI: Spirit Monument (@AIName "spirit_monument"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("spirit_monument")
public class Spirit_MonumentAI2 extends NpcAI2
{
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			// 天族纪念碑。 / Elyos Monument.
			case 883922: //Krotan Guardian Spirit's Monument <20,000 Points>
				updateGuardianLanding(1);
			break;
			case 883923: //Miren Guardian Spirit's Monument <20,000 Points>
				updateGuardianLanding(2);
			break;
			case 883924: //Kysis Guardian Spirit's Monument <20,000 Points>
				updateGuardianLanding(3);
			break;
			// 魔族纪念碑。 / Asmodians Monument.
			case 883941: //Krotan Guardian Spirit's Monument <20,000 Points>
				updateGuardianLanding(13);
			break;
			case 883942: //Miren Guardian Spirit's Monument <20,000 Points>
				updateGuardianLanding(14);
			break;
			case 883943: //Kysis Guardian Spirit's Monument <20,000 Points>
				updateGuardianLanding(15);
			break;
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	private void updateGuardianLanding(final int id) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
                    if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
                        GameLocationBootstrapServices.abyssLandingService().onDieMonuments(Race.ASMODIANS, id, 20000);
                    } else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
                        GameLocationBootstrapServices.abyssLandingService().onDieMonuments(Race.ELYOS, id, 20000);
                    }
                }
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
