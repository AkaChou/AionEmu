package com.aionemu.gameserver.services.siegeservice;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.SiegeDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.landing.LandingPointsEnum;
import com.aionemu.gameserver.model.siege.ArtifactLocation;
import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.services.RvrService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 神器据点攻城：首领击杀后结算归属、落地/前哨/RVR 连锁。
 * Artifact siege that resolves ownership and landing/outpost/RVR side-effects after boss kill.
 */
@Slf4j
public class ArtifactSiege extends Siege<ArtifactLocation> {

	/**
	 * 为指定神器据点创建攻城。
	 * Creates a siege for the given artifact location.
	 *
	 * @param siegeLocation 神器据点 / artifact location
	 */
	public ArtifactSiege(ArtifactLocation siegeLocation) {
		super(siegeLocation);
	}

	/**
	 * 初始化神器攻城首领。
	 * Initializes the artifact siege boss.
	 */
	@Override
	protected void onSiegeStart() {
		initSiegeBoss();
	}

	/**
	 * 结束神器攻城：结算占领、重刷和平 NPC、清理区域并立即重启。
	 * Finishes the artifact siege: resolves capture, respawns peace NPCs, clears zones and restarts.
	 */
	@Override
	protected void onSiegeFinish() {
		unregisterSiegeBossListeners();
		deSpawnNpcs(getSiegeLocationId());
		if (isBossKilled()) {
			onCapture();
			broadcastUpdate(getSiegeLocation());
		} else {
			log.error(I18n.get("log.6b626befcbb0", getSiegeLocationId()));
		}
		spawnNpcs(getSiegeLocationId(), getSiegeLocation().getRace(), SiegeModType.PEACE);
		DAOManager.getDAO(SiegeDAO.class).updateLocation(getSiegeLocation());
		getSiegeLocation().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				player.unsetInsideZoneType(ZoneType.SIEGE);
				player.getController().updateZone();
				player.getController().updateNearbyQuests();
				if (isBossKilled() && (SiegeRace.getByRace(player.getRace()) == getSiegeLocation().getRace())) {
					GameEngineServices.questEngine().onKill(new QuestEnv(getBoss(), player, 0, 0));
				}
			}
		});
		startSiege(getSiegeLocationId());
	}

	/**
	 * 根据伤害胜方更新神器归属，并处理落地/前哨/RVR 连锁。
	 * Updates artifact ownership from the damage winner and applies landing/outpost/RVR side-effects.
	 */
	protected void onCapture() {
		SiegeRaceCounter wRaceCounter = getSiegeCounter().getWinnerRaceCounter();
		getSiegeLocation().setRace(wRaceCounter.getSiegeRace());
		Integer wLegionId = wRaceCounter.getWinnerLegionId();
		getSiegeLocation().setLegionId(wLegionId != null ? wLegionId : 0);
		if (getSiegeLocation().getRace() == SiegeRace.BALAUR) {
			final AionServerPacket lRacePacket = new SM_SYSTEM_MESSAGE(1320004,
					getSiegeLocation().getNameAsDescriptionId(), getSiegeLocation().getRace().getDescriptionId());
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player object) {
					PacketSendUtility.sendPacket(object, lRacePacket);
				}
			});
		} else {
			String wPlayerName = "";
			final Race wRace = wRaceCounter.getSiegeRace() == SiegeRace.ELYOS ? Race.ELYOS : Race.ASMODIANS;
			Legion wLegion = wLegionId != null ? GameCoreGameplayServices.legionService().getLegion(wLegionId) : null;
			if (!wRaceCounter.getPlayerDamageCounter().isEmpty()) {
				Integer wPlayerId = wRaceCounter.getPlayerDamageCounter().keySet().iterator().next();
				wPlayerName = PlayerService.getPlayerName(wPlayerId);
			}
			final String winnerName = wLegion != null ? wLegion.getLegionName() : wPlayerName;
			final AionServerPacket wRacePacket = new SM_SYSTEM_MESSAGE(1320002, wRace.getRaceDescriptionId(),
					winnerName, getSiegeLocation().getNameAsDescriptionId());
			final AionServerPacket lRacePacket = new SM_SYSTEM_MESSAGE(1320004,
					getSiegeLocation().getNameAsDescriptionId(), wRace.getRaceDescriptionId());
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, player.getRace().equals(wRace) ? wRacePacket : lRacePacket);
				}
			});
		}
		// 欧比斯登陆 4.9 / Abyss Landing 4.9
		if (getSiegeLocation().getLocationId() == 1224 || getSiegeLocation().getLocationId() == 1401
				|| getSiegeLocation().getLocationId() == 1402 || getSiegeLocation().getLocationId() == 1403) {
			if (getSiegeLocation().getRace() == SiegeRace.BALAUR) {
				return;
			}
			if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
				GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(8000, LandingPointsEnum.ARTIFACT, false);
				GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(8000, LandingPointsEnum.ARTIFACT, true);
			}
			if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
				GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(8000, LandingPointsEnum.ARTIFACT, true);
				GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(8000, LandingPointsEnum.ARTIFACT, false);
			}
		}
		// 前哨 5.8 / Outpost 5.8
		if (getSiegeLocation().getLocationId() >= 8011 && getSiegeLocation().getLocationId() <= 8017
				|| getSiegeLocation().getLocationId() >= 9011 && getSiegeLocation().getLocationId() <= 9017) {
			if (getSiegeLocation().getRace() == SiegeRace.BALAUR) {
				GameLocationBootstrapServices.outpostService().capture(getSiegeLocation().getOutpostId(), Race.NPC);
			}
			if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
				GameLocationBootstrapServices.outpostService().capture(getSiegeLocation().getOutpostId(), Race.ASMODIANS);
			}
			if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
				GameLocationBootstrapServices.outpostService().capture(getSiegeLocation().getOutpostId(), Race.ELYOS);
			}
		}

		if (getSiegeLocation().getLocationId() >= 4012 && getSiegeLocation().getLocationId() <= 4052) {
			if (getSiegeLocation().getRace() == SiegeRace.BALAUR) {
				GameLocationBootstrapServices.outpostService().capture(getSiegeLocation().getOutpostId(), Race.NPC);
			}
			if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
				GameLocationBootstrapServices.outpostService().capture(getSiegeLocation().getOutpostId(), Race.ASMODIANS);
			}
			if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
				GameLocationBootstrapServices.outpostService().capture(getSiegeLocation().getOutpostId(), Race.ELYOS);
			}
		}

		// 伊卢玛/诺斯珀德神器 5.8 / Iluma/Norsvold Artifact 5.8
		if (getSiegeLocation().getLocationId() == 8021 || getSiegeLocation().getLocationId() == 9021) {
			if (SiegeRace.BALAUR != getSiegeLocation().getRace()) {
				switch (getSiegeLocation().getLocationId()) {
				case 8021:
					if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.rvrService().startRvr(7);
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
							// 魔族占领了阿斯特拉的所有基地，因此追加派遣了阿斯特拉守备队支援兵力。
							// After the Asmodians captured all bases in Asteria, additional Asteria garrison reinforcements were dispatched.
								PacketSendUtility.playerSendPacketTime(player,
										SM_SYSTEM_MESSAGE.STR_MSG_LF6_Occupy_All_Start_MSG, 0);
							}
						});
					} else if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.rvrService().stopRvr(7);
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
							// 夺回被魔族占领的阿斯特拉基地后，阿斯特拉守备队支援兵力返回了。
							// After recapturing Asteria bases held by the Asmodians, the Asteria garrison reinforcements returned.
								PacketSendUtility.playerSendPacketTime(player,
										SM_SYSTEM_MESSAGE.STR_MSG_LF6_Occupy_All_End_MSG, 0);
							}
						});
					}
					break;
				case 9021:
					if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.rvrService().startRvr(8);
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
							// 天族占领了诺斯珀德的全部基地，因此追加派遣了诺斯珀德守备队支援兵力。
							// After the Elyos captured all bases in Norsvold, additional Norsvold garrison reinforcements were dispatched.
								PacketSendUtility.playerSendPacketTime(player,
										SM_SYSTEM_MESSAGE.STR_MSG_DF6_Occupy_All_Start_MSG, 0);
							}
						});
					} else if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.rvrService().stopRvr(8);
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
							// 夺回被天族占领的诺斯珀德基地后，诺斯珀德守备队支援兵力返回了。
							// After recapturing Norsvold bases held by the Elyos, the Norsvold garrison reinforcements returned.
								PacketSendUtility.playerSendPacketTime(player,
										SM_SYSTEM_MESSAGE.STR_MSG_DF6_Occupy_All_End_MSG, 0);
							}
						});
					}
					break;
				}
			}
		}
	}

	/**
	 * 神器攻城为无限模式。
	 * Artifact sieges are endless.
	 *
	 * @return 始终为 true / always true
	 */
	@Override
	public boolean isEndless() {
		return true;
	}

	/**
	 * 神器攻城不累计欧比斯点数（空实现）。
	 * Artifact sieges do not accumulate abyss points (no-op).
	 *
	 * @param player 玩家 / player
	 * @param abysPoints 欧比斯点数 / abyss points
	 */
	@Override
	public void addAbyssPoints(Player player, int abysPoints) {
	}
}
