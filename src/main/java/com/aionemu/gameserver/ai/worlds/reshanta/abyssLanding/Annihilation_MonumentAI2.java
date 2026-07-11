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
 * Reshanta 区域 NPC AI：Annihilation Monument（@AIName "annihilation_monument"），继承 NpcAI2。
 * Reshanta zone NPC AI: Annihilation Monument (@AIName "annihilation_monument"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("annihilation_monument")
public class Annihilation_MonumentAI2 extends NpcAI2
{
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			case 883925: //Ide Cannon Tumon 1 Annihilation Monument <10,000 Points>
				updateAnnihilationLanding1(7);
			break;
			case 883926: //Ide Cannon Tumon 2 Annihilation Monument <10,000 Points>
				updateAnnihilationLanding1(8);
			break;
			case 883927: //Artillery Tumon 1 Annihilation Monument <10,000 Points>
				updateAnnihilationLanding1(9);
			break;
			case 883928: //Artillery Tumon 2 Annihilation Monument <10,000 Points>
				updateAnnihilationLanding1(10);
			break;
			case 883929: //Wurg The Glacier Annihilation Monument <15,000 Points>
				updateAnnihilationLanding2(11);
			break;
			case 883930: //Terracrusher Annihilation Monument <15,000 Points>
				updateAnnihilationLanding2(12);
			break;
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	private void updateAnnihilationLanding1(final int id) {
        com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
                    if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
                        GameLocationBootstrapServices.abyssLandingService().onDieMonuments(Race.ASMODIANS, id, 10000);
                    } else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
                        GameLocationBootstrapServices.abyssLandingService().onDieMonuments(Race.ELYOS, id, 10000);
                    }
                }
            }
        });
    }
	private void updateAnnihilationLanding2(final int id) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
                    if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
                        GameLocationBootstrapServices.abyssLandingService().onDieMonuments(Race.ASMODIANS, id, 15000);
                    } else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
                        GameLocationBootstrapServices.abyssLandingService().onDieMonuments(Race.ELYOS, id, 15000);
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
