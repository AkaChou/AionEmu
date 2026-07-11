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
 * Reshanta 区域 NPC AI：Harbinger Equilibrium（@AIName "harbinger_equilibrium"），继承 NpcAI2。
 * Reshanta zone NPC AI: Harbinger Equilibrium (@AIName "harbinger_equilibrium"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("harbinger_equilibrium")
public class Harbinger_EquilibriumAI2 extends NpcAI2
{
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			case 883931: //Harbinger's Equilibrium <6,000 Points>
			case 883932: //Harbinger's Equilibrium <6,000 Points>
			case 883933: //Harbinger's Equilibrium <6,000 Points>
				updateEquilibriumLanding1();
			break;
			case 883934: //Harbinger's Equilibrium <10,000 Points>
			case 883935: //Harbinger's Equilibrium <10,000 Points>
				updateEquilibriumLanding2();
			break;
			case 883936: //Harbinger's Equilibrium <16,000 Points>
			case 883937: //Harbinger's Equilibrium <16,000 Points>
				updateEquilibriumLanding3();
			break;
			case 883938: //Harbinger's Equilibrium <30,000 Points>
				updateEquilibriumLanding4();
			break;
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	private void updateEquilibriumLanding1() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
                    if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
                        GameLocationBootstrapServices.abyssLandingService().onRewardFacility(Race.ASMODIANS, 6000);
                    } else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
                        GameLocationBootstrapServices.abyssLandingService().onRewardFacility(Race.ELYOS, 6000);
                    }
                }
			}
		});
	}
	private void updateEquilibriumLanding2() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
                    if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
                        GameLocationBootstrapServices.abyssLandingService().onRewardFacility(Race.ASMODIANS, 10000);
                    } else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
                        GameLocationBootstrapServices.abyssLandingService().onRewardFacility(Race.ELYOS, 10000);
                    }
                }
			}
		});
	}
	private void updateEquilibriumLanding3() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
                    if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
                        GameLocationBootstrapServices.abyssLandingService().onRewardFacility(Race.ASMODIANS, 16000);
                    } else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
                        GameLocationBootstrapServices.abyssLandingService().onRewardFacility(Race.ELYOS, 16000);
                    }
                }
			}
		});
	}
	private void updateEquilibriumLanding4() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
                    if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
                        GameLocationBootstrapServices.abyssLandingService().onRewardFacility(Race.ASMODIANS, 30000);
                    } else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
                        GameLocationBootstrapServices.abyssLandingService().onRewardFacility(Race.ELYOS, 30000);
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
