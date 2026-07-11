package com.aionemu.gameserver.ai.worlds.reshanta.abyssLanding;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Reshanta 区域 NPC AI：Siege Commander（@AIName "siege_commander"），继承 AggressiveNpcAI2。
 * Reshanta zone NPC AI: Siege Commander (@AIName "siege_commander"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("siege_commander")
public class Siege_CommanderAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			// 天族指挥官。 / Elyos Commander.
			case 883635: //Krotan Commander <15,000 Points>
				updateCommanderLanding1(4);
			break;
			case 883665: //Miren Commander <15,000 Points>
				updateCommanderLanding1(5);
			break;
			case 883666: //Kysis Commander <20,000 Points>
				updateCommanderLanding2(6);
			break;
			// 魔族指挥官。 / Asmodians Commander.
			case 883636: //Krotan Commander <15,000 Points>
				updateCommanderLanding1(16);
			break;
			case 883667: //Miren Commander <15,000 Points>
				updateCommanderLanding1(17);
			break;
			case 883668: //Kysis Commander <20,000 Points>
				updateCommanderLanding2(18);
			break;
		}
		
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	private void updateCommanderLanding1(final int id) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
                    if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
                        GameLocationBootstrapServices.abyssLandingService().onDieCommander(Race.ASMODIANS, id, 15000);
                    } else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
                        GameLocationBootstrapServices.abyssLandingService().onDieCommander(Race.ELYOS, id, 15000);
                    }
                }
			}
		});
	}
	private void updateCommanderLanding2(final int id) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
                    if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
                        GameLocationBootstrapServices.abyssLandingService().onDieCommander(Race.ASMODIANS, id, 20000);
                    } else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
                        GameLocationBootstrapServices.abyssLandingService().onDieCommander(Race.ELYOS, id, 20000);
                    }
                }
			}
		});
	}
}
