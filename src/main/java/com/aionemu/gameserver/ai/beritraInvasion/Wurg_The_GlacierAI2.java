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
 * 贝里特拉入侵相关 NPC AI：Wurg The Glacier（@AIName "wurg_the_glacier"），继承 AggressiveNpcAI2。
 * Beritra-invasion related NPC AI: Wurg The Glacier (@AIName "wurg_the_glacier"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("wurg_the_glacier")
public class Wurg_The_GlacierAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		addGpPlayer();
		updateWurgLanding();
		announceEreshkigalDie();
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
	private void announceEreshkigalDie() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔军团的魔法武器已被摧毁。 / The Ereshkigal Legion's magic weapon has been destroyed.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_Ere_MESSAGE_DIE_01);
			}
		});
	}
	
	private void updateWurgLanding() {
		final com.aionemu.gameserver.model.gameobjects.Creature mostHated = getOwner().getAggroList().getMostHated();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (mostHated != null && MathUtil.isIn3dRange(mostHated, getOwner(), 20)) {
					if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ASMODIANS, 23, 0);
					} else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ELYOS, 11, 0);
					}
				}
			}
		});
	}
}
