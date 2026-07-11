package com.aionemu.gameserver.ai.worlds.reshanta.abyssLanding;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Reshanta 区域 NPC AI：Stolen Harbinger Call（@AIName "stolen_harbinger_call"），继承 NpcAI2。
 * Reshanta zone NPC AI: Stolen Harbinger Call (@AIName "stolen_harbinger_call"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("stolen_harbinger_call")
public class Stolen_Harbinger_CallAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 22781, 1, getOwner()).useNoAnimationSkill(); //Guardian Sanctuary Icon.
		GameEngineServices.skillEngine().getSkill(getOwner(), 22783, 1, getOwner()).useNoAnimationSkill(); //Guardian Sanctuary Field.
	}
	
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			case 884030: //Stolen Harbinger's Call <6,000 Points>
			case 884031: //Stolen Harbinger's Call <6,000 Points>
			case 884032: //Stolen Harbinger's Call <6,000 Points>
				updateHarbingerLanding1();
			break;
			case 884033: //Stolen Harbinger's Call <10,000 Points>
			case 884034: //Stolen Harbinger's Call <10,000 Points>
				updateHarbingerLanding2();
			break;
			case 884035: //Stolen Harbinger's Call <16,000 Points>
			case 884036: //Stolen Harbinger's Call <16,000 Points>
				updateHarbingerLanding3();
			break;
			case 884037: //Stolen Harbinger's Call <30,000 Points>
				updateHarbingerLanding4();
			break;
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	private void updateHarbingerLanding1() {
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
	private void updateHarbingerLanding2() {
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
	private void updateHarbingerLanding3() {
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
	private void updateHarbingerLanding4() {
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
