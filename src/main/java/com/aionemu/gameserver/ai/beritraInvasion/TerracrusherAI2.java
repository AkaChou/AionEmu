package com.aionemu.gameserver.ai.beritraInvasion;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.landing.LandingPointsEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 贝里特拉入侵相关 NPC AI：Terracrusher（@AIName "terracrusher"），继承 AggressiveNpcAI2。
 * Beritra-invasion related NPC AI: Terracrusher (@AIName "terracrusher"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("terracrusher")
public class TerracrusherAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		addGpPlayer();
		announceEreshkigalDie();
		announceKilledEreshkigal();
		updateTerracrusherLanding();
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
	private void announceKilledEreshkigal() {
		Npc npc = (Npc) getOwner();
		final DescriptionId NameId = new DescriptionId(npc.getObjectTemplate().getNameId());
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player players) {
				AionObject winner = getAggroList().getMostDamage();
				if (winner instanceof Creature) {
					final Creature kill = (Creature) winner;
					// %0 摧毁了 %0，登陆点已增强。 / %0 has destroyed %0 and the Landing is now enhanced.
					GameLocationBootstrapServices.abyssLandingService().AnnounceToPoints(players, kill.getRace().getRaceDescriptionId(), NameId, 0, LandingPointsEnum.MONUMENT);
				}
			}
		});
	}
	
	private void updateTerracrusherLanding() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(getOwner().getAggroList().getMostHated(), getOwner(), 20)) {
					if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ASMODIANS, 24, 0);
					} else if (getOwner().getAggroList().getPlayerWinnerRace() == Race.ELYOS) {
						GameLocationBootstrapServices.abyssLandingService().onRewardMonuments(Race.ELYOS, 12, 0);
					}
				}
			}
		});
	}
}
