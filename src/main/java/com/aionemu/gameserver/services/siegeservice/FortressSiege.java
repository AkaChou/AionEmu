package com.aionemu.gameserver.services.siegeservice;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.List;
import java.util.Map;

import com.aionemu.commons.callbacks.util.GlobalCallbackHelper;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.SiegeDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.landing.LandingPointsEnum;
import com.aionemu.gameserver.model.siege.ArtifactLocation;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.templates.siegelocation.SiegeLegionReward;
import com.aionemu.gameserver.model.templates.siegelocation.SiegeReward;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.AbyssLandingSpecialService;
import com.aionemu.gameserver.services.MoltenusService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.mail.AbyssSiegeLevel;
import com.aionemu.gameserver.services.mail.MailFormatter;
import com.aionemu.gameserver.services.mail.SiegeResult;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneName;
import com.google.common.collect.Lists;
/**
 * 要塞攻城：脆弱/护盾状态、占领结算、奖励与落地/基地连锁。
 * Fortress siege handling vulnerability/shield, capture settlement, rewards and landing/base side-effects.
 */
@Slf4j(topic = "SIEGE_LOG")
public class FortressSiege extends Siege<FortressLocation> {

	/** 欧比斯点数监听器。 / Abyss points listener. */
	private final AbyssPointsListener addAPListener = new AbyssPointsListener(this);
	/**
	 * 为指定要塞据点创建攻城。
	 * Creates a siege for the given fortress location.
	 *
	 * @param fortress 要塞据点 / fortress location
	 */
	public FortressSiege(FortressLocation fortress) {
		super(fortress);
	}
	/**
	 * 开启要塞攻城：设脆弱/护盾、刷攻城 NPC、初始化首领并处理特殊据点。
	 * Starts the fortress siege: sets vulnerable/shield, spawns siege NPCs, inits boss and special locations.
	 */
	@Override
	public void onSiegeStart() {
		getSiegeLocation().setVulnerable(true);
		getSiegeLocation().setUnderShield(true);
		broadcastState(getSiegeLocation());
		getSiegeLocation().clearLocation();
		GlobalCallbackHelper.addCallback(addAPListener);
		deSpawnNpcs(getSiegeLocationId());
		clearPlayers();
		// GameCoreGameplayServices.battlefieldUnionService().onSiegeStart(getSiegeLocation().getLocationId());
		spawnNpcs(getSiegeLocationId(), getSiegeLocation().getRace(), SiegeModType.SIEGE);
		initSiegeBoss();
		if (getSiegeLocation().getLocationId() == 1131) {
			switch (getSiegeLocation().getLocationId()) {
			case 1131: // Siel's Western Fortress.
				GameFeatureServices.baseService().capture(108, Race.NPC);
				GameFeatureServices.baseService().capture(109, Race.NPC);
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						// 金沙谈判队正遭受龙族攻击。 / The Gold Sand Negotiation Team is under attack by the Balaur.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_War_Soon, 0);
						// 龙族已控制希尔左翼的博米雄。 / The Balaur have taken control of the Bomishung at Siel's Left Wing.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuDr_05,
								6000);
						// 希尔左翼的博米雄正遭受龙族攻击。 / The Bomishung at Siel's Left Wing is under attack by the Balaur.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_AtkDr_05,
								12000);
						// 龙族已控制风暴岛的谢林。 / The Balaur have taken control of the Shairing at the Island of Storm.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuDr_04,
								18000);
						// 风暴岛的谢林正遭受龙族攻击。 / The Shairing at the Island of Storm is under attack by the Balaur.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_AtkDr_04,
								24000);
					}
				});
				break;
			}
		} else if (getSiegeLocation().getLocationId() == 1132) {
			switch (getSiegeLocation().getLocationId()) {
			case 1132: // Siel's Eastern Fortress.
				GameFeatureServices.baseService().capture(110, Race.NPC);
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						// 龙族已控制希尔右翼的萨斯明。 / The Balaur have taken control of the Sasming at Siel's Right Wing.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuDr_06,
								30000);
						// 希尔右翼的萨斯明正遭受龙族攻击。 / The Sasming at Siel's Right Wing is under attack by the Balaur.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_AtkDr_06,
								36000);
					}
				});
				break;
			}
		} else if (getSiegeLocation().getLocationId() == 1141) {
			switch (getSiegeLocation().getLocationId()) {
			case 1141: // Sulfur Fortress.
				GameFeatureServices.baseService().capture(105, Race.NPC);
				GameFeatureServices.baseService().capture(106, Race.NPC);
				GameFeatureServices.baseService().capture(107, Race.NPC);
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						// 龙族已控制硫磺树群岛的奥哈隆。 / The Balaur have taken control of the Oharung at the Sulfur Tree Archipelago.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuDr_01,
								42000);
						// 硫磺树群岛的奥哈隆正遭受龙族攻击。 / The Oharung at the Sulfur Tree Archipelago is under attack by the Balaur.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_AtkDr_01,
								50000);
						// 龙族已控制西风岛的乔阿林。 / The Balaur have taken control of the Joarin at Zephyr Island.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuDr_02,
								56000);
						// 西风岛的乔阿林正遭受龙族攻击。 / The Joarin at Zephyr Island is under attack by the Balaur.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_AtkDr_02,
								62000);
						// 龙族已控制雷博岛的特米伦。 / The Balaur have taken control of the Temirun at Leibo Island.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuDr_03,
								68000);
						// 雷博岛的特米伦正遭受龙族攻击。 / The Temirun at Leibo Island is under attack by the Balaur.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_AtkDr_03,
								74000);
					}
				});
				break;
			}
		} else if (getSiegeLocation().getLocationId() == 10111) {
			switch (getSiegeLocation().getLocationId()) {
			case 10111:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						// 神殿大门将在 5 分钟后打开。 / The Temple Gate will open in 5 minutes.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Gab1_START01, 0);
						// 神殿大门将在 1 分钟后打开。 / The Temple Gate will open in 1 minute.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Gab1_START02, 240000);
						// 神殿大门将在 30 秒后打开。 / The Temple Gate will open in 30 seconds.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Gab1_START03, 270000);
						// 神殿大门将在 10 秒后打开。 / The Temple Gate will open in 10 seconds.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Gab1_START04, 290000);
						// 神殿大门已打开。 / The Temple Gate has opened.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_Gab1_START05, 300000);
					}
				});
				break;
			}
		}
	}
	/**
	 * 结束要塞攻城：撤监听、结算占领/奖励、重刷和平 NPC 与任务联动。
	 * Finishes the fortress siege: removes listeners, settles capture/rewards, respawns peace NPCs and quest hooks.
	 */
	@Override
	public void onSiegeFinish() {
		GlobalCallbackHelper.removeCallback(addAPListener);
		unregisterSiegeBossListeners();
		GameFeatureServices.siegeService().deSpawnNpcs(getSiegeLocationId());
		getSiegeLocation().setVulnerable(false);
		getSiegeLocation().setUnderShield(false);
		if (isBossKilled()) {
			onCapture();
			applyBuff();
			broadcastUpdate(getSiegeLocation());
		} else {
			broadcastState(getSiegeLocation());
		}
		GameFeatureServices.siegeService().spawnNpcs(getSiegeLocationId(), getSiegeLocation().getRace(), SiegeModType.PEACE);
		if (SiegeRace.BALAUR != getSiegeLocation().getRace()) {
			if (getSiegeLocation().getLegionId() > 0) {
				giveRewardsToLegion();
			}
			giveRewardsToPlayers(getSiegeCounter().getRaceCounter(getSiegeLocation().getRace()));
		}
		DAOManager.getDAO(SiegeDAO.class).updateSiegeLocation(getSiegeLocation());
		getSiegeLocation().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				player.unsetInsideZoneType(ZoneType.SIEGE);
				player.getController().updateZone();
				player.getController().updateNearbyQuests();
				if (isBossKilled() && (SiegeRace.getByRace(player.getRace()) == getSiegeLocation().getRace())) {
					GameEngineServices.questEngine().onKill(new QuestEnv(getBoss(), player, 0, 0));
				}
				// 暴怒守护者 5.3 / Enraged Guardian 5.3
				switch (getSiegeLocationId()) {
				case 1131: // Siel's Western Fortress.
					if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.moltenusService().startMoltenus(5);
					}
					if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.moltenusService().startMoltenus(8);
					}
					break;
				case 1132: // Siel's Eastern Fortress.
					if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.moltenusService().startMoltenus(6);
					}
					if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.moltenusService().startMoltenus(9);
					}
					break;
				case 1141: // Sulfur Fortress.
					if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.moltenusService().startMoltenus(4);
					}
					if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.moltenusService().startMoltenus(7);
					}
					break;
				}
			}
		});
	}
	/**
	 * 根据伤害胜方更新要塞与神器归属，并处理落地/基地连锁。
	 * Updates fortress and artifact ownership from the damage winner and applies landing/base side-effects.
	 */
	public void onCapture() {
		SiegeRaceCounter winner = getSiegeCounter().getWinnerRaceCounter();
		SiegeRace looser = getSiegeLocation().getRace();
		getSiegeLocation().setRace(winner.getSiegeRace());
		getArtifact().setRace(winner.getSiegeRace());
		if (SiegeRace.BALAUR == winner.getSiegeRace()) {
			getSiegeLocation().setLegionId(0);
			getArtifact().setLegionId(0);
		} else {
			Integer topLegionId = winner.getWinnerLegionId();
			getSiegeLocation().setLegionId(topLegionId != null ? topLegionId : 0);
			getArtifact().setLegionId(topLegionId != null ? topLegionId : 0);
		}
		// 欧比斯登陆 4.9.1 / Abyss Landing 4.9.1
		if (getSiegeLocation().getLocationId() == 1131 || getSiegeLocation().getLocationId() == 1132
				|| getSiegeLocation().getLocationId() == 1141 || getSiegeLocation().getLocationId() == 1221
				|| getSiegeLocation().getLocationId() == 1231 || getSiegeLocation().getLocationId() == 1241) {
			Player player = null;
			if (SiegeRace.BALAUR != getSiegeLocation().getRace()) {
				switch (getSiegeLocation().getLocationId()) {
				// 希尔西要塞。 / Siel's Western Fortress.
				case 1131:
					if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						// 在卡普斯岛的谢林。 / Shairing At Carpus Isle.
						GameFeatureServices.baseService().capture(108, Race.ASMODIANS);
						// 在希尔左翼的博米雄。 / Bomishung At Siel's Left Wing.
						GameFeatureServices.baseService().capture(109, Race.ASMODIANS);
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
								// 魔族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Asmodians have arrived at the Siel's
								// 西部要塞。 / Western Fortress.
								PacketSendUtility.playerSendPacketTime(player,
										SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_D_02, 0);
							}
						});
					}
					if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						// 在卡普斯岛的谢林。 / Shairing At Carpus Isle.
						GameFeatureServices.baseService().capture(108, Race.ELYOS);
						// 在希尔左翼的博米雄。 / Bomishung At Siel's Left Wing.
						GameFeatureServices.baseService().capture(109, Race.ELYOS);
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
								// 天族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Elyos have arrived at the Siel's
								// 西部要塞。 / Western Fortress.
								PacketSendUtility.playerSendPacketTime(player,
										SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_L_02, 0);
							}
						});
					}
					break;
				// 希尔东要塞。 / Siel's Eastern Fortress.
				case 1132:
					if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						// 在希尔右翼的萨斯明。 / Sasming At Siel's Right Wing.
						GameFeatureServices.baseService().capture(110, Race.ASMODIANS);
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
								// 魔族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Asmodians have arrived at the Siel's
								// 东部要塞。 / Eastern Fortress.
								PacketSendUtility.playerSendPacketTime(player,
										SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_D_03, 0);
							}
						});
					}
					if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						// 在希尔右翼的萨斯明。 / Sasming At Siel's Right Wing.
						GameFeatureServices.baseService().capture(110, Race.ELYOS);
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
								// 天族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Elyos have arrived at the Siel's
								// 东部要塞。 / Eastern Fortress.
								PacketSendUtility.playerSendPacketTime(player,
										SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_L_03, 0);
							}
						});
					}
					break;
				// 硫磺要塞。 / Sulfur Fortress.
				case 1141:
					if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						// 在硫磺群岛的奥哈隆。 / Oharung At The Sulfur Archipelago.
						GameFeatureServices.baseService().capture(105, Race.ASMODIANS);
						// 在西风岛的乔阿林。 / Joarin At Zephyr Island.
						GameFeatureServices.baseService().capture(106, Race.ASMODIANS);
						// 在雷博岛的特米伦。 / Temirun At Leibo Island.
						GameFeatureServices.baseService().capture(107, Race.ASMODIANS);
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
								// 魔族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Asmodians have arrived at the Sulfur
								// 要塞。 / Fortress.
								PacketSendUtility.playerSendPacketTime(player,
										SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_D_01, 0);
							}
						});
					}
					if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						// 在硫磺群岛的奥哈隆。 / Oharung At The Sulfur Archipelago.
						GameFeatureServices.baseService().capture(105, Race.ELYOS);
						// 在西风岛的乔阿林。 / Joarin At Zephyr Island.
						GameFeatureServices.baseService().capture(106, Race.ELYOS);
						// 在雷博岛的特米伦。 / Temirun At Leibo Island.
						GameFeatureServices.baseService().capture(107, Race.ELYOS);
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
								// 天族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Elyos have arrived at the Sulfur
								// 要塞。 / Fortress.
								PacketSendUtility.playerSendPacketTime(player,
										SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_L_01, 0);
							}
						});
					}
					break;
				// 克罗坦避难所。 / Krotan Refuge.
				case 1221:
					if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().startLanding(16);
						GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(35000, LandingPointsEnum.SIEGE, true);
					}
					if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().startLanding(4);
						GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(35000, LandingPointsEnum.SIEGE, true);
					}
					break;
				// 基西斯要塞。 / Kysis Fortress.
				case 1231:
					if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().startLanding(18);
						GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(40000, LandingPointsEnum.SIEGE, true);
					}
					if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().startLanding(6);
						GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(40000, LandingPointsEnum.SIEGE, true);
					}
					break;
				// 米伦要塞。 / Miren Fortress.
				case 1241:
					if (getSiegeLocation().getRace() == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().startLanding(17);
						GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(35000, LandingPointsEnum.SIEGE, true);
					}
					if (getSiegeLocation().getRace() == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().startLanding(5);
						GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(35000, LandingPointsEnum.SIEGE, true);
					}
					break;
				}
				GameLocationBootstrapServices.abyssLandingService().AnnounceToPoints(player,
						getSiegeLocation().getRace().getDescriptionId(), getSiegeLocation().getNameAsDescriptionId(), 0,
						LandingPointsEnum.SIEGE);
			}
			if (SiegeRace.BALAUR == getSiegeLocation().getRace() || winner.getSiegeRace() != looser) {
				switch (getSiegeLocation().getLocationId()) {
				// 希尔西要塞。 / Siel's Western Fortress.
				case 1131:
					if (looser == SiegeRace.ASMODIANS) {
						// 在卡普斯岛的谢林。 / Shairing At Carpus Isle.
						GameFeatureServices.baseService().capture(108, Race.NPC);
						// 在希尔左翼的博米雄。 / Bomishung At Siel's Left Wing.
						GameFeatureServices.baseService().capture(109, Race.NPC);
					}
					if (looser == SiegeRace.ELYOS) {
						// 在卡普斯岛的谢林。 / Shairing At Carpus Isle.
						GameFeatureServices.baseService().capture(108, Race.NPC);
						// 在希尔左翼的博米雄。 / Bomishung At Siel's Left Wing.
						GameFeatureServices.baseService().capture(109, Race.NPC);
					}
					break;
				// 希尔东要塞。 / Siel's Eastern Fortress.
				case 1132:
					if (looser == SiegeRace.ASMODIANS) {
						// 在希尔右翼的萨斯明。 / Sasming At Siel's Right Wing.
						GameFeatureServices.baseService().capture(110, Race.NPC);
					}
					if (looser == SiegeRace.ELYOS) {
						// 在希尔右翼的萨斯明。 / Sasming At Siel's Right Wing.
						GameFeatureServices.baseService().capture(110, Race.NPC);
					}
					break;
				// 硫磺要塞。 / Sulfur Fortress.
				case 1141:
					if (looser == SiegeRace.ASMODIANS) {
						// 在硫磺群岛的奥哈隆。 / Oharung At The Sulfur Archipelago.
						GameFeatureServices.baseService().capture(105, Race.NPC);
						// 在西风岛的乔阿林。 / Joarin At Zephyr Island.
						GameFeatureServices.baseService().capture(106, Race.NPC);
						// 在雷博岛的特米伦。 / Temirun At Leibo Island.
						GameFeatureServices.baseService().capture(107, Race.NPC);
					}
					if (looser == SiegeRace.ELYOS) {
						// 在硫磺群岛的奥哈隆。 / Oharung At The Sulfur Archipelago.
						GameFeatureServices.baseService().capture(105, Race.NPC);
						// 在西风岛的乔阿林。 / Joarin At Zephyr Island.
						GameFeatureServices.baseService().capture(106, Race.NPC);
						// 在雷博岛的特米伦。 / Temirun At Leibo Island.
						GameFeatureServices.baseService().capture(107, Race.NPC);
					}
					break;
				// 克罗坦避难所。 / Krotan Refuge.
				case 1221:
					if (looser == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().stopLanding(16);
						GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(35000, LandingPointsEnum.SIEGE, false);
					}
					if (looser == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().stopLanding(4);
						GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(35000, LandingPointsEnum.SIEGE,
								false);
					}
					break;
				// 基西斯要塞。 / Kysis Fortress.
				case 1231:
					if (looser == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().stopLanding(18);
						GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(40000, LandingPointsEnum.SIEGE, false);
					}
					if (looser == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().stopLanding(6);
						GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(40000, LandingPointsEnum.SIEGE,
								false);
					}
					break;
				// 米伦要塞。 / Miren Fortress.
				case 1241:
					if (looser == SiegeRace.ASMODIANS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().stopLanding(17);
						GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(35000, LandingPointsEnum.SIEGE, false);
					}
					if (looser == SiegeRace.ELYOS) {
						GameLocationBootstrapServices.abyssLandingSpecialService().stopLanding(5);
						GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(35000, LandingPointsEnum.SIEGE,
								false);
					}
					break;
				}
			}
		}
	}
	/**
	 * 向全服玩家应用/刷新要塞占领 Buff。
	 * Applies or refreshes fortress ownership buffs for all players.
	 */
	public void applyBuff() {
		SiegeRaceCounter winner = getSiegeCounter().getWinnerRaceCounter();
		getSiegeLocation().setRace(winner.getSiegeRace());
		getArtifact().setRace(winner.getSiegeRace());
		if (SiegeRace.BALAUR == winner.getSiegeRace()) {
			getSiegeLocation().setLegionId(0);
			getArtifact().setLegionId(0);
		} else {
			Integer topLegionId = winner.getWinnerLegionId();
			getSiegeLocation().setLegionId(topLegionId != null ? topLegionId : 0);
			getArtifact().setLegionId(topLegionId != null ? topLegionId : 0);
		}
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 双种族增益。 / Buff for Both Race.
				if (player.getEffectController().hasAbnormalEffect(getSiegeLocation().getBuffId())) {
					player.getEffectController().removeEffect(getSiegeLocation().getBuffId());
				} else {
					GameEngineServices.skillEngine().applyEffectDirectly(getSiegeLocation().getBuffId(), player, player, 0);
				}
				// 魔族或天族增益。 / Buff for Asmodians or Elyos.
				if (player.getEffectController().hasAbnormalEffect(getSiegeLocation().getBuffIdA())) {
					player.getEffectController().removeEffect(getSiegeLocation().getBuffIdA());
				}
				if (player.getEffectController().hasAbnormalEffect(getSiegeLocation().getBuffIdE())) {
					player.getEffectController().removeEffect(getSiegeLocation().getBuffIdE());
				}
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					GameEngineServices.skillEngine().applyEffectDirectly(getSiegeLocation().getBuffIdA(), player, player, 0);
				}
				if (player.getCommonData().getRace() == Race.ELYOS) {
					GameEngineServices.skillEngine().applyEffectDirectly(getSiegeLocation().getBuffIdE(), player, player, 0);
				}
			}
		});
	}
	/**
	 * 要塞攻城有时限，非无限模式。
	 * Fortress sieges are timed, not endless.
	 *
	 * @return 始终为 false / always false
	 */
	@Override
	public boolean isEndless() {
		return false;
	}
	/**
	 * 将玩家获得的欧比斯点数记入攻城统计。
	 * Records abyss points earned by a player into siege counters.
	 *
	 * @param player 玩家 / player
	 * @param abysPoints 欧比斯点数 / abyss points
	 */
	@Override
	public void addAbyssPoints(Player player, int abysPoints) {
		getSiegeCounter().addAbyssPoints(player, abysPoints);
	}
	/**
	 * 向防守军团旅长发放勋章奖励邮件。
	 * Sends medal reward mail to the defending legion brigade general.
	 */
	protected void giveRewardsToLegion() {
		if (isBossKilled()) {
			return;
		}
		if (getSiegeLocation().getLegionId() == 0) {
			return;
		}
		List<SiegeLegionReward> legionRewards = getSiegeLocation().getLegionReward();
		SiegeResult resultLegion = isBossKilled() ? SiegeResult.OCCUPY : SiegeResult.DEFENDER;
		int legionBGeneral = GameCoreGameplayServices.legionService().getLegionBGeneral(getSiegeLocation().getLegionId());
		if (legionBGeneral != 0) {
			PlayerCommonData BGeneral = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(legionBGeneral);
			if (legionRewards != null) {
				for (SiegeLegionReward medalsType : legionRewards) {
					MailFormatter.sendAbyssRewardMail(getSiegeLocation(), BGeneral, AbyssSiegeLevel.VETERAN_SOLDIER,
							resultLegion, System.currentTimeMillis(), medalsType.getItemId(),
							medalsType.getCount() * SiegeConfig.SIEGE_MEDAL_RATE, 0);
				}
			}
		}
	}
	/**
	 * 判断玩家是否处于要塞相关攻城区。
	 * Returns whether the player is inside a fortress-related siege zone.
	 *
	 * @param player 玩家 / player
	 * @return 是否在攻城区 / whether inside a siege zone
	 */
	public boolean isInSiegeZone(Player player) {
		if (player.isInsideZone(ZoneName.get("EYE_OF_RESHANTA_400010000"))
				|| player.isInsideZone(ZoneName.get("DIVINE_FORTRESS_400010000"))
				|| player.isInsideZone(ZoneName.get("KROTAN_REFUGE_400010000"))
				|| player.isInsideZone(ZoneName.get("KROTAN_ROCK_400010000"))
				|| player.isInsideZone(ZoneName.get("RATTLEFROST_OUTPOST_400010000"))
				|| player.isInsideZone(ZoneName.get("BLOODBURN_REACH_400010000"))
				|| player.isInsideZone(ZoneName.get("SLIVERSLEET_OUTPOST_400010000"))
				|| player.isInsideZone(ZoneName.get("MIREN_FORTRESS_400010000"))
				|| player.isInsideZone(ZoneName.get("MIREN_ISLAND_400010000"))
				|| player.isInsideZone(ZoneName.get("COLDFORGE_OUTPOST_400010000"))
				|| player.isInsideZone(ZoneName.get("SHIMMERFROST_OUTPOST_400010000"))
				|| player.isInsideZone(ZoneName.get("ICEHOWL_OUTPOST_400010000"))
				|| player.isInsideZone(ZoneName.get("KYSIS_FORTRESS_400010000"))
				|| player.isInsideZone(ZoneName.get("KYSIS_ISLE_400010000"))
				|| player.isInsideZone(ZoneName.get("CHILLHAUNT_OUTPOST_400010000"))
				|| player.isInsideZone(ZoneName.get("SIEL_EASTERN_FORTRESS_400010000"))
				|| player.isInsideZone(ZoneName.get("SIEL_RIGHT_WING_A_400010000"))
				|| player.isInsideZone(ZoneName.get("SIEL_RIGHT_WING_B_400010000"))
				|| player.isInsideZone(ZoneName.get("SIEL_LEFT_WING_A_400010000"))
				|| player.isInsideZone(ZoneName.get("SIEL_LEFT_WING_B_400010000"))
				|| player.isInsideZone(ZoneName.get("WING_OF_SIEL_ARCHIPELAGO_A_400010000"))
				|| player.isInsideZone(ZoneName.get("WING_OF_SIEL_ARCHIPELAGO_B_400010000"))
				|| player.isInsideZone(ZoneName.get("HEART_OF_SIEL_400010000"))
				|| player.isInsideZone(ZoneName.get("SIEL_WESTERN_FORTRESS_400010000"))
				|| player.isInsideZone(ZoneName.get("ISLE_OF_DISGRACE_400010000"))
				|| player.isInsideZone(ZoneName.get("ISLE_OF_ROOT_400010000"))
				|| player.isInsideZone(ZoneName.get("ISLE_OF_REPROACH_400010000"))
				|| player.isInsideZone(ZoneName.get("SULFUR_FLOW_400010000"))
				|| player.isInsideZone(ZoneName.get("SULFUR_SWAMP_400010000"))
				|| player.isInsideZone(ZoneName.get("SULFUR_FORTRESS_400010000"))
				|| player.isInsideZone(ZoneName.get("KRAKON_DISPUTE_400010000"))
				|| player.isInsideZone(ZoneName.get("SULFUR_ARCHIPELAGO_400010000"))
				|| player.isInsideZone(ZoneName.get("WESTERN_RIDGE_400010000"))
				|| player.isInsideZone(ZoneName.get("NORTHERN_RIDGE_400010000"))
				|| player.isInsideZone(ZoneName.get("EASTERN_RIDGE_400010000"))
				|| player.isInsideZone(ZoneName.get("SOUTHERN_RIDGE_400010000"))
				|| player.isInsideZone(ZoneName.get("HEROS_FALL_600090000"))
				|| player.isInsideZone(ZoneName.get("ASHEN_GLADE_600090000"))
				|| player.isInsideZone(ZoneName.get("WEALHTHEOWS_KEEP_600090000"))
				|| player.isInsideZone(ZoneName.get("WEALHTHEOWS_KEEP_RUINS_600090000"))
				|| player.isInsideZone(ZoneName.get("MOLTEN_CLIFFS_600090000"))
				|| player.isInsideZone(ZoneName.get("SOUTH_ROAD_600090000"))
				|| player.isInsideZone(ZoneName.get("SMOLDERING_CRAG_600090000"))
				|| player.isInsideZone(ZoneName.get("ANOHA_PASS_600090000"))
				|| player.isInsideZone(ZoneName.get("ANOHA_BINDING_600090000"))) {
			return true;
		}
		return false;
	}
	/**
	 * 将敌对阵营玩家传送出攻城区。
	 * Teleports enemy-race players out of the siege zone.
	 */
	public void clearPlayers() {
		for (Player player : getSiegeLocation().getPlayers().values()) {
			int worldId = getSiegeLocation().getWorldId();
			if (getSiegeLocation().isEnemy(player) && isInSiegeZone(player)) {
				switch (worldId) {
				case 400010000: // Reshanta.
					if (player.getRace() == Race.ASMODIANS) {
						TeleportService2.teleportTo(player, 400010000, 576.90533f, 2542.4539f, 1636.0665f, (byte) 30); // Primum
																														// 登陆点。 / Landing.
					} else if (player.getRace() == Race.ELYOS) {
						TeleportService2.teleportTo(player, 400010000, 2259.2463f, 663.3353f, 1527.9968f, (byte) 94); // Teminon
																														// 登陆点。 / Landing.
					}
					break;
				case 600090000: // Kaldor.
					if (player.getRace() == Race.ASMODIANS) {
						TeleportService2.teleportTo(player, 600090000, 408.1886f, 1359.2572f, 163.51178f, (byte) 96); // Rubirinerk's
																														// 定居点。 / Settlement.
					} else if (player.getRace() == Race.ELYOS) {
						TeleportService2.teleportTo(player, 600090000, 1302.5714f, 1315.4507f, 199.75026f, (byte) 97); // Saparinerk's
																														// 定居点。 / Settlement.
					}
					break;
				}
			}
		}
	}
	/**
	 * 按欧比斯点数排名向玩家发放勋章奖励。
	 * Sends medal rewards to top players ranked by abyss points.
	 *
	 * @param winnerDamage 胜方种族计数器 / winner race counter
	 */
	protected void giveRewardsToPlayers(SiegeRaceCounter winnerDamage) {
		Map<Integer, Long> playerAbyssPoints = winnerDamage.getPlayerAbyssPoints();
		List<Integer> topPlayersIds = Lists.newArrayList(playerAbyssPoints.keySet());
		Map<Integer, String> playerNames = PlayerService.getPlayerNames(playerAbyssPoints.keySet());
		SiegeResult resultPlayers = isBossKilled() ? SiegeResult.OCCUPY : SiegeResult.DEFENDER;
		int i = 0;
		List<SiegeReward> playerRewards = getSiegeLocation().getReward();
		for (SiegeReward topGrade : playerRewards) {
			for (int rewardedPC = 0; i < topPlayersIds.size() && rewardedPC < topGrade.getTop(); ++i) {
				Integer playerId = topPlayersIds.get(i);
				PlayerCommonData pcd = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(playerId);
				++rewardedPC;
				MailFormatter.sendAbyssRewardMail(getSiegeLocation(), pcd, AbyssSiegeLevel.VETERAN_SOLDIER,
						resultPlayers, System.currentTimeMillis(), topGrade.getItemId(),
						topGrade.getCount() * SiegeConfig.SIEGE_MEDAL_RATE, 0);
			}
		}
	}
	/**
	 * @return 关联神器据点 / related artifact location
	 */
	protected ArtifactLocation getArtifact() {
		return GameFeatureServices.siegeService().getFortressArtifacts().get(getSiegeLocationId());
	}
	/**
	 * @return 是否存在关联神器 / whether a related artifact exists
	 */
	protected boolean hasArtifact() {
		return getArtifact() != null;
	}
}
