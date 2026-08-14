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
 * 贝里特拉入侵相关 NPC AI：Magnorion（@AIName "magnorion"），继承 AggressiveNpcAI2。
 * Beritra-invasion related NPC AI: Magnorion (@AIName "magnorion"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("magnorion")
public class MagnorionAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
		    // 精英马格诺里安 II。 / Elite Magnorion II.
			case 234611:
			    addGpPlayer();
			    announceMagnorionDie();
			break;
			case 234589:
			    addGpPlayer();
			    announceMagnorionDie();
				updateMagnorionLanding();
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
	private void announceMagnorionDie() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 恶魔部队的马格诺已被摧毁。 / The Devil Unit's Magno has been destroyed.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_DIE_01);
			}
		});
	}
	
	private void updateMagnorionLanding() {
		final com.aionemu.gameserver.model.gameobjects.Creature mostHated = getOwner().getAggroList().getMostHated();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (mostHated != null && MathUtil.isIn3dRange(mostHated, getOwner(), 20)) {
					if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ASMODIANS, 20, 0);
					} else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ELYOS, 8, 0);
					}
				}
			}
		});
	}
}
