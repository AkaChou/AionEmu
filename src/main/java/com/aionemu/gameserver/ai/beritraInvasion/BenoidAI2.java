package com.aionemu.gameserver.ai.beritraInvasion;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 贝里特拉入侵相关 NPC AI：Benoid（@AIName "benoid"），继承 AggressiveNpcAI2。
 * Beritra-invasion related NPC AI: Benoid (@AIName "benoid"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("benoid")
public class BenoidAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			case 234612:
			case 234613:
			case 234614:
			    addGpPlayer();
			    announceBenoidDie();
			break;
			case 236750:
			    addGpPlayer();
				announceBenoidDie();
				updateBenoidLanding();
			break;
		}
		super.handleDied();
	}
	
	private void addGpPlayer() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(player, getOwner(), 15)) {
					AbyssPointsService.addGp(player, 500);
				}
			}
		});
	}
	private void announceBenoidDie() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 恶魔部队的贝诺伊德已被摧毁。 / The Devil Unit's Benoid has been destroyed.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_DIE_06);
			}
		});
	}
	
	private void updateBenoidLanding() {
		final com.aionemu.gameserver.model.gameobjects.Creature mostHated = getOwner().getAggroList().getMostHated();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (mostHated != null && MathUtil.isIn3dRange(mostHated, getOwner(), 20)) {
					if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ASMODIANS, 19, 0);
					} else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ELYOS, 7, 0);
					}
				}
			}
		});
	}
}
