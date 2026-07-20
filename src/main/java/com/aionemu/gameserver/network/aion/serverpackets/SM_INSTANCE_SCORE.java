package com.aionemu.gameserver.network.aion.serverpackets;


import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.ContaminatedUnderpathReward;
import com.aionemu.gameserver.model.instance.instancereward.DarkPoetaReward;
import com.aionemu.gameserver.model.instance.instancereward.DredgionReward;
import com.aionemu.gameserver.model.instance.instancereward.EngulfedOphidanBridgeReward;
import com.aionemu.gameserver.model.instance.instancereward.EternalBastionReward;
import com.aionemu.gameserver.model.instance.instancereward.EvergaleCanyonReward;
import com.aionemu.gameserver.model.instance.instancereward.FissureOfOblivionReward;
import com.aionemu.gameserver.model.instance.instancereward.HarmonyArenaReward;
import com.aionemu.gameserver.model.instance.instancereward.IDEventDefReward;
import com.aionemu.gameserver.model.instance.instancereward.IdgelDomeReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.IronWallWarfrontReward;
import com.aionemu.gameserver.model.instance.instancereward.KamarBattlefieldReward;
import com.aionemu.gameserver.model.instance.instancereward.LandMarkReward;
import com.aionemu.gameserver.model.instance.instancereward.PvPArenaReward;
import com.aionemu.gameserver.model.instance.instancereward.SealedArgentManorReward;
import com.aionemu.gameserver.model.instance.instancereward.SecretMunitionsFactoryReward;
import com.aionemu.gameserver.model.instance.instancereward.ShugoEmperorVaultReward;
import com.aionemu.gameserver.model.instance.instancereward.SmolderingReward;
import com.aionemu.gameserver.model.instance.instancereward.StonespearReachReward;
import com.aionemu.gameserver.model.instance.instancereward.TreasureIslandReward;
import com.aionemu.gameserver.model.instance.playerreward.ContaminatedUnderpathPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.BattlegroundPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.CruciblePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.DredgionPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.EngulfedOphidanBridgePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.EternalBastionPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.EvergaleCanyonPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.FissureOfOblivionPlayerReward;
import com.aionemu.gameserver.model.instance.tournament.TournamentScore;
import com.aionemu.gameserver.model.instance.playerreward.HarmonyGroupReward;
import com.aionemu.gameserver.model.instance.playerreward.IDEventDefPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.IdgelDomePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.IronWallWarfrontPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.KamarBattlefieldPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.LandMarkPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.PvPArenaPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.SealedArgentManorPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.SecretMunitionsFactoryPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.ShugoEmperorVaultPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.SmolderingPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.StonespearReachPlayerReward;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;

import java.util.ArrayList;

/**
 * 向客户端同步各类副本/战场计分板、奖励与玩家状态的服务端包。
 * Server packet synchronizing instance/battlefield scoreboard, rewards, and player state to the client.
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class SM_INSTANCE_SCORE extends AionServerPacket {

	private int type;
	private int mapId;
	private int instanceTime;
	private InstanceScoreType instanceScoreType;
	private InstanceReward instanceReward;
	private List<Player> players;
	private Integer object;
	private int PlayerStatus = 0;
	private int PlayerRaceId = 0;
	private TournamentScore tournamentScore;

	public SM_INSTANCE_SCORE(int mapId, int instanceTime, InstanceScoreType instanceScoreType,
			TournamentScore tournamentScore) {
		this.mapId = mapId;
		this.instanceTime = instanceTime;
		this.instanceScoreType = instanceScoreType;
		this.tournamentScore = tournamentScore;
	}

	/**
	 * 按类型同步计分，并附带目标对象、玩家状态与种族。
	 * Syncs score by type with target object, player status, and race.
	 *
	 * @param type 同步类型 / sync type
	 * @param instanceTime 副本剩余/经过时间 / instance time
	 * @param instanceReward 副本奖励上下文 / instance reward context
	 * related object id
	 * player status
	 * player race id
	 */
	public SM_INSTANCE_SCORE(int type, int instanceTime, InstanceReward instanceReward, Integer object,
			int PlayerStatus, int PlayerRaceId) {
		this.mapId = instanceReward.getMapId();
		this.type = type;
		this.instanceTime = instanceTime;
		this.instanceReward = instanceReward;
		this.object = object;
		this.PlayerStatus = PlayerStatus;
		this.PlayerRaceId = PlayerRaceId;
		instanceScoreType = instanceReward.getInstanceScoreType();
	}

	/**
	 * 按类型同步计分，并附带目标对象。
	 * Syncs score by type with a target object.
	 *
	 * @param type 同步类型 / sync type
	 * @param instanceTime 副本剩余/经过时间 / instance time
	 * @param instanceReward 副本奖励上下文 / instance reward context
	 * related object id
	 */
	public SM_INSTANCE_SCORE(int type, int instanceTime, InstanceReward instanceReward, Integer object) {
		this.mapId = instanceReward.getMapId();
		this.type = type;
		this.instanceTime = instanceTime;
		this.instanceReward = instanceReward;
		this.object = object;
		instanceScoreType = instanceReward.getInstanceScoreType();
	}

	public SM_INSTANCE_SCORE(int type, int instanceTime, InstanceReward instanceReward, Integer object,
			List<Player> players) {
		this(type, instanceTime, instanceReward, object);
		this.players = players;
	}

	/**
	 * 按玩家列表同步整场计分板。
	 * Syncs the full scoreboard for a player list.
	 *
	 * @param instanceTime 副本剩余/经过时间 / instance time
	 * @param instanceReward 副本奖励上下文 / instance reward context
	 * player list
	 */
	public SM_INSTANCE_SCORE(int instanceTime, InstanceReward instanceReward, List<Player> players) {
		this.mapId = instanceReward.getMapId();
		this.instanceTime = instanceTime;
		this.instanceReward = instanceReward;
		this.players = players;
		instanceScoreType = instanceReward.getInstanceScoreType();
	}

	/**
	 * 按类型与玩家列表同步计分（兼容重载，{@code tis} 未使用）。
	 * Syncs score by type and player list (compat overload; {@code tis} unused).
	 *
	 * @param type 同步类型 / sync type
	 * @param instanceTime 副本剩余/经过时间 / instance time
	 * @param instanceReward 副本奖励上下文 / instance reward context
	 * player list
	 * @param tis 未使用的兼容参数 / unused compatibility flag
	 */
	public SM_INSTANCE_SCORE(int type, int instanceTime, InstanceReward instanceReward, List<Player> players,
			boolean tis) {
		this.mapId = instanceReward.getMapId();
		this.type = type;
		this.instanceTime = instanceTime;
		this.instanceReward = instanceReward;
		this.players = players;
		instanceScoreType = instanceReward.getInstanceScoreType();
	}

	/**
	 * 使用显式计分类型同步副本奖励。
	 * Syncs instance reward with an explicit score type.
	 *
	 * @param instanceReward 副本奖励上下文 / instance reward context
	 * score type
	 */
	public SM_INSTANCE_SCORE(InstanceReward instanceReward, InstanceScoreType instanceScoreType) {
		this.mapId = instanceReward.getMapId();
		this.instanceReward = instanceReward;
		this.instanceScoreType = instanceScoreType;
	}

	/**
	 * 使用奖励自带的计分类型同步。
	 * Syncs using the score type carried by the reward.
	 *
	 * @param instanceReward 副本奖励上下文 / instance reward context
	 */
	public SM_INSTANCE_SCORE(InstanceReward instanceReward) {
		this.mapId = instanceReward.getMapId();
		this.instanceReward = instanceReward;
		this.instanceScoreType = instanceReward.getInstanceScoreType();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void writeImpl(AionConnection con) {
		int playerCount = 0;
		Player owner = con.getActivePlayer();
		Integer ownerObject = owner.getObjectId();
		writeD(mapId);
		writeD(instanceTime);
		writeD(instanceScoreType.getId());
		if (tournamentScore != null) {
			writeB(tournamentScore.payload());
			return;
		}
		switch (mapId) {
		case 300450000: // Arena Of Harmony 3.9
		case 300570000: // Harmony Training Grounds 3.9
		case 301100000: // Unity Training Grounds 3.9
			HarmonyArenaReward harmonyArena = (HarmonyArenaReward) instanceReward;
			if (object == null) {
				object = ownerObject;
			}
			HarmonyGroupReward harmonyGroupReward = harmonyArena.getHarmonyGroupReward(object);
			writeC(type);
			switch (type) {
			case 2:
				writeD(0);
				writeD(harmonyArena.getRound());
				break;
			case 3:
				writeD(harmonyGroupReward.getOwner());
				writeS(harmonyGroupReward.getAGPlayer(object).getName(), 52);
				writeD(harmonyGroupReward.getId());
				writeD(object);
				break;
			case 4:
				writeD(harmonyArena.getPlayerReward(object).getRemaningTime());
				writeD(0);
				writeD(0);
				writeD(object);
				break;
			case 5:
				PvPArenaPlayerReward harmonyPlayerReward = harmonyArena.getPlayerReward(object);
				if (harmonyArena.isRewarded() && harmonyArena.canRewarded() && harmonyPlayerReward != null) {
					writeArenaReward(harmonyPlayerReward);
				} else {
					writeB(new byte[76]);
				}
				writeD(0);
				writeD(0);
				writeD(0);
				writeD(harmonyPlayerReward == null ? 0 : harmonyPlayerReward.getParticipationPercent());
				writeD(harmonyGroupReward.getPoints());
				break;
			case 6:
				writeD(3);
				writeD(harmonyArena.getCapPoints());
				writeD(3);
				writeD(1);
				writeD(harmonyArena.getBuffId());
				writeD(2);
				writeD(0);
				writeD(harmonyArena.getRound());
				List<HarmonyGroupReward> groups = harmonyArena.getHarmonyGroupInside();
				writeC(groups.size());
				for (HarmonyGroupReward group : groups) {
					writeC(harmonyArena.getRank(group.getPoints()));
					writeD(group.getPvPKills());
					writeD(group.getPoints());
					writeD(group.getOwner());
					List<Player> members = harmonyArena.getPlayersInside(group);
					writeC(members.size());
					int i = 0;
					for (Player p : members) {
						PvPArenaPlayerReward rewardedPlayer = harmonyArena.getPlayerReward(p.getObjectId());
						writeD(0);
						writeD(rewardedPlayer.getRemaningTime());
						writeD(0);
						writeC(group.getOwner());
						writeC(i);
						writeH(0);
						writeS(p.getName(), 52);
						writeD(p.getObjectId());
						i++;
					}
				}
				break;
			case 10:
				writeC(harmonyArena.getRank(harmonyGroupReward.getPoints()));
				writeD(harmonyGroupReward.getPvPKills());
				writeD(harmonyGroupReward.getPoints());
				writeD(harmonyGroupReward.getOwner());
				break;
			}
			break;
		case 300110000: // Baranath Dredgion.
		case 300210000: // Chantra Dredgion.
		case 300440000: // Terath Dredgion.
		case 301650000: // Ashunatal Dredgion.
			fillTableWithGroup(Race.ELYOS);
			fillTableWithGroup(Race.ASMODIANS);
			DredgionReward dredgionReward = (DredgionReward) instanceReward;
			int elyosScore = dredgionReward.getPointsByRace(Race.ELYOS).intValue();
			int asmosScore = dredgionReward.getPointsByRace(Race.ASMODIANS).intValue();
			writeD(instanceScoreType.isEndProgress() ? (asmosScore > elyosScore ? 1 : 0) : 255);
			writeD(elyosScore);
			writeD(asmosScore);
			writeH(0);
			for (DredgionReward.DredgionRooms dredgionRoom : dredgionReward.getDredgionRooms()) {
				writeC(dredgionRoom.getState());
			}
			break;
		case 301120000: // Kamar Battlefield 4.3
			KamarBattlefieldReward kbr = (KamarBattlefieldReward) instanceReward;
			if (object == null) {
				object = ownerObject;
			}
			KamarBattlefieldPlayerReward kbpr = kbr.getPlayerReward(object);
			writeC(type);
			switch (type) {
			case 2:
				writeD(0);
				writeD(kbr.getTime());
				break;
			case 3:
				writeD(10);
				writeD(PlayerStatus);
				writeD(object);
				writeD(PlayerRaceId);
				break;
			case 4:
				writeD(10);
				writeD(PlayerStatus);
				writeD(object);
				break;
			case 5:
				writeBattlegroundReward(kbpr, 5);
				break;
			case 6:
				int counter = 0;
				writeD(100);
				for (Player player : players) {
					if (player.getRace() != Race.ELYOS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				counter = 0;
				for (Player player : players) {
					if (player.getRace() != Race.ASMODIANS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				writeC(0);
				writeD(kbr.getPvpKillsByRace(Race.ELYOS).intValue());
				writeD(kbr.getPointsByRace(Race.ELYOS).intValue());
				writeD(0);
				writeD((kbr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				writeC(0);
				writeD(kbr.getPvpKillsByRace(Race.ASMODIANS).intValue());
				writeD(kbr.getPointsByRace(Race.ASMODIANS).intValue());
				writeD(1);
				writeD((kbr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				break;
			case 7:
				kamarBattlefieldTable(Race.ELYOS);
				kamarBattlefieldTable(Race.ASMODIANS);
				break;
			case 8:
				writeD(object);
				break;
			case 10:
				writeC(0);
				writeD(kbr.getPvpKillsByRace(kbpr.getRace()).intValue());
				writeD(kbr.getPointsByRace(kbpr.getRace()).intValue());
				writeD(kbpr.getRace().getRaceId());
				writeD(object);
				break;
			case 11:
				int TeamScore = kbr.getPointsByRace(kbpr.getRace()).intValue();
				int OppositeTeamScore = kbr.getPointsByRace(kbpr.getRace()).intValue();
				writeC(0);
				writeD(kbr.getPvpKillsByRace(kbpr.getRace()).intValue());
				writeD(TeamScore);
				writeD(kbpr.getRace().getRaceId());
				writeD(TeamScore == OppositeTeamScore ? 65535 : 0);
				break;
			}
			break;
		case 301210000: // Engulfed Ophidan Bridge 4.5
		case 301670000: // Ophidan Warpath 5.1
			EngulfedOphidanBridgeReward eobr = (EngulfedOphidanBridgeReward) instanceReward;
			if (object == null) {
				object = ownerObject;
			}
			EngulfedOphidanBridgePlayerReward eobpr = eobr.getPlayerReward(object);
			writeC(type);
			switch (type) {
			case 2:
				writeD(0);
				for (Player player : players) {
					switch (player.getWorldId()) {
					case 301210000: // Engulfed Ophidan Bridge 4.7
						writeD(eobr.getTime());
						break;
					case 301670000: // Ophidan Warpath 5.1
						writeD(eobr.getTime2());
						break;
					}
				}
				break;
			case 3:
				writeD(11);
				writeD(PlayerStatus);
				writeD(object);
				writeD(PlayerRaceId);
				break;
			case 4:
				writeD(11);
				writeD(PlayerStatus);
				writeD(object);
				break;
			case 5:
				writeBattlegroundReward(eobpr, 5);
				break;
			case 6:
				int counter = 0;
				writeD(100);
				for (Player player : players) {
					if (player.getRace() != Race.ELYOS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				counter = 0;
				for (Player player : players) {
					if (player.getRace() != Race.ASMODIANS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				writeC(0);
				writeD(eobr.getPvpKillsByRace(Race.ELYOS).intValue());
				writeD(eobr.getPointsByRace(Race.ELYOS).intValue());
				writeD(0);
				writeD((eobr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				writeC(0);
				writeD(eobr.getPvpKillsByRace(Race.ASMODIANS).intValue());
				writeD(eobr.getPointsByRace(Race.ASMODIANS).intValue());
				writeD(1);
				writeD((eobr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				break;
			case 7:
				engulfedOphidanBridgeTable(Race.ELYOS);
				engulfedOphidanBridgeTable(Race.ASMODIANS);
				break;
			case 8:
				writeD(object);
				break;
			case 10:
				writeC(0);
				writeD(eobr.getPvpKillsByRace(eobpr.getRace()).intValue());
				writeD(eobr.getPointsByRace(eobpr.getRace()).intValue());
				writeD(eobpr.getRace().getRaceId());
				writeD(object);
				break;
			case 11:
				int TeamScore2 = eobr.getPointsByRace(eobpr.getRace()).intValue();
				int OppositeTeamScore2 = eobr.getPointsByRace(eobpr.getRace()).intValue();
				writeC(0);
				writeD(eobr.getPvpKillsByRace(eobpr.getRace()).intValue());
				writeD(TeamScore2);
				writeD(eobpr.getRace().getRaceId());
				writeD(TeamScore2 == OppositeTeamScore2 ? 65535 : 0);
				break;
			}
			break;
		case 301220000: // Iron Wall Warfront 4.5
			IronWallWarfrontReward iwwr = (IronWallWarfrontReward) instanceReward;
			if (object == null) {
				object = ownerObject;
			}
			IronWallWarfrontPlayerReward iwwpr = iwwr.getPlayerReward(object);
			writeC(type);
			switch (type) {
			case 2:
				writeD(0);
				writeD(instanceTime);
				break;
			case 3:
				writeD(12);
				writeD(PlayerStatus);
				writeD(object);
				writeD(PlayerRaceId);
				break;
			case 4:
				writeD(12);
				writeD(PlayerStatus);
				writeD(object);
				break;
			case 5:
				writeBattlegroundReward(iwwpr, 5);
				break;
			case 6:
				int counter = 0;
				writeD(100);
				for (Player player : players) {
					if (player.getRace() != Race.ELYOS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				counter = 0;
				for (Player player : players) {
					if (player.getRace() != Race.ASMODIANS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				writeC(0);
				writeD(iwwr.getPvpKillsByRace(Race.ELYOS).intValue());
				writeD(iwwr.getPointsByRace(Race.ELYOS).intValue());
				writeD(0);
				writeD((iwwr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				writeC(0);
				writeD(iwwr.getPvpKillsByRace(Race.ASMODIANS).intValue());
				writeD(iwwr.getPointsByRace(Race.ASMODIANS).intValue());
				writeD(1);
				writeD((iwwr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				break;
			case 7:
				ironWallWarfrontTable(Race.ELYOS);
				ironWallWarfrontTable(Race.ASMODIANS);
				break;
			case 8:
				writeD(object);
				break;
			case 10:
				writeC(0);
				writeD(iwwr.getPvpKillsByRace(iwwpr.getRace()).intValue());
				writeD(iwwr.getPointsByRace(iwwpr.getRace()).intValue());
				writeD(iwwpr.getRace().getRaceId());
				writeD(object);
				break;
			case 11:
				int TeamScore3 = iwwr.getPointsByRace(iwwpr.getRace()).intValue();
				Race opposingRace = iwwpr.getRace() == Race.ELYOS ? Race.ASMODIANS : Race.ELYOS;
				int OppositeTeamScore3 = iwwr.getPointsByRace(opposingRace).intValue();
				writeC(0);
				writeD(iwwr.getPvpKillsByRace(iwwpr.getRace()).intValue());
				writeD(TeamScore3);
				writeD(iwwpr.getRace().getRaceId());
				writeD(TeamScore3 == OppositeTeamScore3 ? 65535 : 0);
				break;
			}
			break;
		case 301310000: // Idgel Dome 4.7
			IdgelDomeReward idr = (IdgelDomeReward) instanceReward;
			if (object == null) {
				object = ownerObject;
			}
			IdgelDomePlayerReward idpr = idr.getPlayerReward(object);
			writeC(type);
			switch (type) {
			case 2:
				writeD(0);
				writeD(idr.getTime());
				break;
			case 3:
				writeD(15);
				writeD(PlayerStatus);
				writeD(object);
				writeD(PlayerRaceId);
				break;
			case 4:
				writeD(15);
				writeD(PlayerStatus);
				writeD(object);
				break;
			case 5:
				writeBattlegroundReward(idpr, 5);
				break;
			case 6:
				int counter = 0;
				writeD(100);
				for (Player player : players) {
					if (player.getRace() != Race.ELYOS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				counter = 0;
				for (Player player : players) {
					if (player.getRace() != Race.ASMODIANS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				writeC(0);
				writeD(idr.getPvpKillsByRace(Race.ELYOS).intValue());
				writeD(idr.getPointsByRace(Race.ELYOS).intValue());
				writeD(0);
				writeD((idr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				writeC(0);
				writeD(idr.getPvpKillsByRace(Race.ASMODIANS).intValue());
				writeD(idr.getPointsByRace(Race.ASMODIANS).intValue());
				writeD(1);
				writeD((idr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				break;
			case 7:
				idgelDomeTable(Race.ELYOS);
				idgelDomeTable(Race.ASMODIANS);
				break;
			case 8:
				writeD(object);
				break;
			case 10:
				writeC(0);
				writeD(idr.getPvpKillsByRace(idpr.getRace()).intValue());
				writeD(idr.getPointsByRace(idpr.getRace()).intValue());
				writeD(idpr.getRace().getRaceId());
				writeD(object);
				break;
			case 11:
				int TeamScore4 = idr.getPointsByRace(idpr.getRace()).intValue();
				int OppositeTeamScore4 = idr.getPointsByRace(idpr.getRace()).intValue();
				writeC(0);
				writeD(idr.getPvpKillsByRace(idpr.getRace()).intValue());
				writeD(TeamScore4);
				writeD(idpr.getRace().getRaceId());
				writeD(TeamScore4 == OppositeTeamScore4 ? 65535 : 0);
				break;
			}
			break;
		case 301680000: // Idgel Dome Landmark 5.1
			LandMarkReward lmr = (LandMarkReward) instanceReward;
			if (object == null) {
				object = ownerObject;
			}
			LandMarkPlayerReward lmpr = lmr.getPlayerReward(object);
			writeC(type);
			switch (type) {
			case 2:
				writeD(0);
				writeD(lmr.getTime());
				break;
			case 3:
				writeD(15);
				writeD(PlayerStatus);
				writeD(object);
				writeD(PlayerRaceId);
				break;
			case 4:
				writeD(15);
				writeD(PlayerStatus);
				writeD(object);
				break;
			case 5:
				writeBattlegroundReward(lmpr, 5);
				break;
			case 6:
				int counter = 0;
				writeD(100);
				for (Player player : players) {
					if (player.getRace() != Race.ELYOS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				counter = 0;
				for (Player player : players) {
					if (player.getRace() != Race.ASMODIANS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				writeC(0);
				writeD(lmr.getPvpKillsByRace(Race.ELYOS).intValue());
				writeD(lmr.getPointsByRace(Race.ELYOS).intValue());
				writeD(0);
				writeD((lmr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				writeC(0);
				writeD(lmr.getPvpKillsByRace(Race.ASMODIANS).intValue());
				writeD(lmr.getPointsByRace(Race.ASMODIANS).intValue());
				writeD(1);
				writeD((lmr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				break;
			case 7:
				landMarkTable(Race.ELYOS);
				landMarkTable(Race.ASMODIANS);
				break;
			case 8:
				writeD(object);
				break;
			case 10:
				writeC(0);
				writeD(lmr.getPvpKillsByRace(lmpr.getRace()).intValue());
				writeD(lmr.getPointsByRace(lmpr.getRace()).intValue());
				writeD(lmpr.getRace().getRaceId());
				writeD(object);
				break;
			case 11:
				int TeamScore5 = lmr.getPointsByRace(lmpr.getRace()).intValue();
				int OppositeTeamScore5 = lmr.getPointsByRace(lmpr.getRace()).intValue();
				writeC(0);
				writeD(lmr.getPvpKillsByRace(lmpr.getRace()).intValue());
				writeD(TeamScore5);
				writeD(lmpr.getRace().getRaceId());
				writeD(TeamScore5 == OppositeTeamScore5 ? 65535 : 0);
				break;
			}
			break;
		case 302350000: // Evergale Canyon 5.5
			EvergaleCanyonReward ecr = (EvergaleCanyonReward) instanceReward;
			if (object == null) {
				object = ownerObject;
			}
			EvergaleCanyonPlayerReward ecpr = ecr.getPlayerReward(object);
			writeC(type);
			switch (type) {
			case 2:
				writeD(0);
				writeD(instanceTime);
				break;
			case 3:
				writeD(10);
				writeD(PlayerStatus);
				writeD(object);
				writeD(PlayerRaceId);
				break;
			case 4:
				writeD(10);
				writeD(PlayerStatus);
				writeD(object);
				break;
			case 5:
				writeBattlegroundReward(ecpr, 4);
				break;
			case 6:
				int counter = 0;
				writeD(100);
				for (Player player : players) {
					if (player.getRace() != Race.ELYOS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				counter = 0;
				for (Player player : players) {
					if (player.getRace() != Race.ASMODIANS) {
						continue;
					}
					writeD(15);
					writeD(player.getLifeStats().isAlreadyDead() ? 60 : 0);
					writeD(player.getObjectId());
					counter++;
				}
				if (counter < 24) {
					writeB(new byte[12 * (24 - counter)]);
				}
				writeC(0);
				writeD(ecr.getPvpKillsByRace(Race.ELYOS).intValue());
				writeD(ecr.getPointsByRace(Race.ELYOS).intValue());
				writeD(0);
				writeD((ecr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				writeC(0);
				writeD(ecr.getPvpKillsByRace(Race.ASMODIANS).intValue());
				writeD(ecr.getPointsByRace(Race.ASMODIANS).intValue());
				writeD(1);
				writeD((ecr.getInstanceScoreType() == instanceScoreType.PREPARING ? 65535 : 1));
				break;
			case 7:
				evergaleCanyonTable(Race.ELYOS);
				evergaleCanyonTable(Race.ASMODIANS);
				break;
			case 8:
				writeD(object);
				break;
			case 10:
				writeC(0);
				writeD(ecr.getPvpKillsByRace(ecpr.getRace()).intValue());
				writeD(ecr.getPointsByRace(ecpr.getRace()).intValue());
				writeD(ecpr.getRace().getRaceId());
				writeD(object);
				break;
			case 11:
				int TeamScore6 = ecr.getPointsByRace(ecpr.getRace()).intValue();
				Race oppositeRace = ecpr.getRace() == Race.ELYOS ? Race.ASMODIANS : Race.ELYOS;
				int OppositeTeamScore6 = ecr.getPointsByRace(oppositeRace).intValue();
				writeC(0);
				writeD(ecr.getPvpKillsByRace(ecpr.getRace()).intValue());
				writeD(TeamScore6);
				writeD(ecpr.getRace().getRaceId());
				writeD(TeamScore6 == OppositeTeamScore6 ? 65535 : 0);
				break;
			}
			break;
		case 301700000: // Treasure Island of Courage 5.8
			TreasureIslandReward treasure = (TreasureIslandReward) instanceReward;
			if (object == null) {
				object = ownerObject;
			}
			BattlegroundPlayerReward treasurePlayer = treasure.getPlayerReward(object);
			writeC(type);
			switch (type) {
				case 3:
					writeD(15);
					writeD(PlayerStatus);
					writeD(object);
					writeD(PlayerRaceId);
					break;
				case 4:
					writeD(15);
					writeD(PlayerStatus);
					writeD(object);
					break;
				case 5:
					writeTreasureIslandReward(treasurePlayer);
					break;
				case 6:
					writeD(100);
					writeTreasureIslandStatuses(Race.ELYOS);
					writeTreasureIslandStatuses(Race.ASMODIANS);
					writeTreasureIslandTeam(treasure, Race.ELYOS);
					writeTreasureIslandTeam(treasure, Race.ASMODIANS);
					break;
				case 7:
					writeD(treasurePlayer.getRace().getRaceId());
					writeTreasureIslandTable(treasurePlayer.getRace());
					break;
				case 10:
					writeC(0);
					writeD(0);
					writeD(treasure.getPointsByRace(treasurePlayer.getRace()));
					writeD(treasurePlayer.getRace().getRaceId());
					writeD(object);
					break;
				case 11:
					writeC(0);
					writeD(0);
					writeD(treasure.getPointsByRace(treasurePlayer.getRace()));
					writeD(treasurePlayer.getRace().getRaceId());
					writeD(treasure.isPreparing() ? 65535 : 1);
					break;
			}
			break;
		case 300300000: // Empyrean Crucible 2.5
		case 300320000: // Empyrean Crucible Challenge 2.6
			for (CruciblePlayerReward playerReward : (List<CruciblePlayerReward>) instanceReward
					.getInstanceRewards()) {
				writeD(playerReward.getOwner());
				writeD(playerReward.getPoints());
				writeD(instanceScoreType.isEndProgress() ? 3 : 1);
				writeD(playerReward.getInsignia());
				playerCount++;
			}
			if (playerCount < 6) {
				writeB(new byte[16 * (6 - playerCount)]);
			}
			break;
		case 300040000: // Dark Poeta.
			DarkPoetaReward dpr = (DarkPoetaReward) instanceReward;
			writeD(dpr.getPoints());
			writeD(dpr.getNpcKills());
			writeD(dpr.getGatherCollections());
			writeD(dpr.getRank());
			break;
		case 300540000: // Eternal Bastion 4.8
			for (EternalBastionPlayerReward playerReward : (List<EternalBastionPlayerReward>) instanceReward
					.getInstanceRewards()) {
				EternalBastionReward etr = (EternalBastionReward) instanceReward;
				writeD(etr.getPoints());
				writeD(etr.getNpcKills());
				writeD(0);
				writeD(etr.getRank());
				writeD(0);
				writeD(playerReward.getScoreAP());
				writeD(0);
				writeD(0);
				writeD(0);
				if (etr.getPoints() >= 60000) {
					writeD(188052595); // High Grade Material Box.
					writeD(playerReward.getHighGradeMaterialBox());
					writeD(186000242); // Ceramium Medal.
					writeD(playerReward.getCeramium());
					writeD(188052598); // Low Grade Material Support Bundle.
				} else {
					writeD(0);
					writeD(0);
					writeD(0);
					writeD(0);
					writeD(0);
				}
				if (etr.getPoints() >= 90000) {
					writeD(188052594); // Highest Grade Material Box.
					writeD(playerReward.getHighestGradeMaterialBox());
					writeD(186000242); // Ceramium Medal.
					writeD(playerReward.getCeramium());
					writeD(188052596); // Highest Grade Material Support Bundle.
				} else {
					writeD(0);
					writeD(0);
					writeD(0);
				}
			}
			break;
		case 301400000: // The Shugo Emperor's Vault 4.7.5
		case 301590000: // Emperor Trillirunerk's Safe 4.9.1
			for (ShugoEmperorVaultPlayerReward playerReward : (List<ShugoEmperorVaultPlayerReward>) instanceReward
					.getInstanceRewards()) {
				ShugoEmperorVaultReward sevr = (ShugoEmperorVaultReward) instanceReward;
				writeD(sevr.getPoints());
				writeD(sevr.getNpcKills());
				writeD(0);
				writeD(sevr.getRank());
				writeD(0);
				writeD(0);
				writeD(instanceScoreType.isEndProgress() ? playerReward.getRustedVaultKey() : 0);
				writeD(instanceScoreType.isEndProgress() ? playerReward.getRustedVaultKey() : 0);
				writeD(0);
				writeD(0);
				writeD(0);
				writeD(0);
				writeD(0);
			}
			break;
		case 301500000: // Stonespear Reach 4.8
			for (StonespearReachPlayerReward playerReward : (List<StonespearReachPlayerReward>) instanceReward
					.getInstanceRewards()) {
				StonespearReachReward srr = (StonespearReachReward) instanceReward;
				writeD(srr.getPoints());
				writeD(srr.getNpcKills());
				writeD(srr.getRank());
			}
			break;
		case 301510000: // Sealed Argent Manor 4.9.1
			for (SealedArgentManorPlayerReward playerReward : (List<SealedArgentManorPlayerReward>) instanceReward
					.getInstanceRewards()) {
				SealedArgentManorReward samr = (SealedArgentManorReward) instanceReward;
				writeD(samr.getPoints());
				writeD(samr.getNpcKills());
				writeD(0);
				writeD(samr.getRank());
				writeD(0);
				writeD(playerReward.getScoreAP());
				writeD(0);
				writeD(0);
				writeD(0);
				if (samr.getPoints() >= 11500) {
					writeD(188054115); // Argent Manor Box.
					writeD(playerReward.getArgentManorBox());
					writeD(188054116); // Lesser Argent Manor Box.
				} else {
					writeD(0);
					writeD(0);
					writeD(0);
					writeD(0);
					writeD(0);
				}
				if (samr.getPoints() >= 16000) {
					writeD(188054114); // Greater Argent Manor Box.
					writeD(playerReward.getGreaterArgentManorBox());
				} else {
					writeD(0);
					writeD(0);
					writeD(0);
				}
			}
			break;
		case 301630000: // Contaminated Underpath 5.1
			for (ContaminatedUnderpathPlayerReward playerReward : (List<ContaminatedUnderpathPlayerReward>) instanceReward
					.getInstanceRewards()) {
				ContaminatedUnderpathReward cur = (ContaminatedUnderpathReward) instanceReward;
				writeD(cur.getPoints());
				writeD(cur.getNpcKills());
				writeD(0);
				writeD(cur.getRank());
				writeD(0);
				writeD(playerReward.getScoreAP());
				writeD(0);
				writeD(0);
				writeD(0);
				if (cur.getPoints() >= 50) {
					writeD(188055664); // Contaminated Underpath Special Pouch.
					writeD(playerReward.getContaminatedUnderpathSpecialPouch());
					writeD(188055599); // Contaminated Highest Reward Bundle.
				} else {
					writeD(0);
					writeD(0);
					writeD(0);
					writeD(0);
					writeD(0);
				}
				if (cur.getPoints() >= 549000) {
					writeD(188055598); // Contaminated Premium Reward Bundle.
					writeD(playerReward.getContaminatedPremiumRewardBundle());
				} else {
					writeD(0);
					writeD(0);
					writeD(0);
				}
			}
			break;
		case 301631000: // [Event] Contaminated Underpath 5.6
		case 301632000: // IDEvent_Def_H 5.8
			for (IDEventDefPlayerReward playerReward : (List<IDEventDefPlayerReward>) instanceReward
					.getInstanceRewards()) {
				IDEventDefReward def = (IDEventDefReward) instanceReward;
				writeD(def.getPoints());
				writeD(def.getNpcKills());
				writeD(0);
				writeD(def.getRank());
				writeD(0);
				writeD(playerReward.getScoreAP());
				writeD(0);
				writeD(0);
				writeD(0);
				if (def.getPoints() >= 220000) {
					writeD(188054115); // A랭크 보물 상자.
					writeD(playerReward.getWrapCashIDEventDefLiveARank());
				} else {
					writeD(0);
					writeD(0);
					writeD(0);
					writeD(0);
					writeD(0);
				}
				if (def.getPoints() >= 500000) {
					writeD(188058265); // S랭크 보물 상자.
					writeD(playerReward.getWrapCashIDEventDefLiveSRank());
				} else {
					writeD(0);
					writeD(0);
					writeD(0);
				}
			}
			break;
		case 301640000: // Secret Munitions Factory 5.1
			for (SecretMunitionsFactoryPlayerReward playerReward : (List<SecretMunitionsFactoryPlayerReward>) instanceReward
					.getInstanceRewards()) {
				SecretMunitionsFactoryReward smfr = (SecretMunitionsFactoryReward) instanceReward;
				RewardPlan plan = InstanceSettlementService.lunaPlan(mapId, smfr.getRank());
				writeD(smfr.getPoints());
				writeD(smfr.getNpcKills());
				writeD(0);
				writeD(smfr.getRank());
				writeD(0);
				writeD(plan.ap());
				writeD(0);
				writeD(0);
				writeD(0);
				for (int slot = 0; slot < 3; slot++) {
					if (slot < plan.items().size()) {
						writeD(plan.items().get(slot).itemId());
						writeD(Math.toIntExact(plan.items().get(slot).count()));
					} else {
						writeD(0);
						writeD(0);
					}
				}
				writeD(0);
				writeD(0);
			}
			break;
		case 302000000: // Smoldering Fire Temple 5.1
			for (SmolderingPlayerReward playerReward : (List<SmolderingPlayerReward>) instanceReward
					.getInstanceRewards()) {
				SmolderingReward sr = (SmolderingReward) instanceReward;
				writeD(sr.getPoints());
				writeD(sr.getNpcKills());
				writeD(0);
				writeD(sr.getRank());
				writeD(0);
				writeD(0);
				writeD(instanceScoreType.isEndProgress() ? playerReward.getSmolderingKey() : 0);
				writeD(instanceScoreType.isEndProgress() ? playerReward.getSmolderingKey() : 0);
				writeD(0);
				writeD(0);
				writeD(0);
				writeD(0);
				writeD(0);
			}
			break;
		case 302100000: // Fissure Of Oblivion 5.1
		case 302110000: // [Opportunity] Fissure Of Oblivion 5.6
			for (FissureOfOblivionPlayerReward playerReward : (List<FissureOfOblivionPlayerReward>) instanceReward
					.getInstanceRewards()) {
				FissureOfOblivionReward oblivion = (FissureOfOblivionReward) instanceReward;
				writeD(oblivion.getPoints());
				writeD(oblivion.getNpcKills());
				writeD(0);
				writeD(oblivion.getRank());
				writeD(0);
				writeD(0);
				writeD(instanceScoreType.isEndProgress() ? playerReward.getFrozenMarbleOfMemory() : 0);
				writeD(instanceScoreType.isEndProgress() ? playerReward.getFrozenMarbleOfMemory() : 0);
				writeD(0);
				writeD(0);
				writeD(0);
				writeD(0);
				writeD(0);
			}
			break;
		case 300350000: // Arena Of Chaos.
		case 300360000: // Arena Of Discipline.
		case 300420000: // Chaos Training Grounds.
		case 300430000: // Discipline Training Grounds.
		case 300550000: // Arena Of Glory.
			PvPArenaReward arenaReward = (PvPArenaReward) instanceReward;
			PvPArenaPlayerReward rewardedPlayer = arenaReward.getPlayerReward(ownerObject);
			int rank, points;
			boolean isRewarded = arenaReward.isRewarded();
			for (Player player : players) {
				InstancePlayerReward reward = arenaReward.getPlayerReward(player.getObjectId());
				PvPArenaPlayerReward playerReward = (PvPArenaPlayerReward) reward;
				points = playerReward.getPoints();
				rank = arenaReward.getRank(playerReward.getScorePoints());
				writeD(playerReward.getOwner());
				writeD(playerReward.getPvPKills());
				writeD(isRewarded ? points + playerReward.getTimeBonus() : points);
				writeD(0);
				writeC(0);
				writeC(player.getPlayerClass().getClassId());
				writeC(1);
				writeC(rank);
				writeD(playerReward.getRemaningTime());
				writeD(isRewarded ? playerReward.getTimeBonus() : 0);
				writeD(0);
				writeD(0);
				writeH(isRewarded ? playerReward.getParticipationPercent() : 0);
				writeS(player.getName(), 54);
				playerCount++;
			}
			if (playerCount < 12) {
				writeB(new byte[92 * (12 - playerCount)]);
			}
			if (isRewarded && arenaReward.canRewarded() && rewardedPlayer != null) {
				writeArenaReward(rewardedPlayer);
			} else {
				writeB(new byte[76]);
			}
			writeD(arenaReward.getBuffId());
			writeD(arenaReward.getZone());
			writeD(arenaReward.getRound());
			writeD(arenaReward.getCapPoints());
			writeD(arenaReward.getScoreModifierStartStage());
			writeD(0);
			break;
		}
	}

	private void writeArenaReward(PvPArenaPlayerReward reward) {
		writeD(reward.getBasicAP());
		writeD(reward.getBasicGP());
		writeD(reward.getScoreAP());
		writeD(reward.getScoreGP());
		writeD(reward.getRankingAP());
		writeD(reward.getRankingGP());
		writeD(reward.getItem1Id());
		writeD(reward.getBasicItem1());
		writeD(reward.getPlayItem1());
		writeD(reward.getRankItem1());
		writeD(reward.getItem2Id());
		writeD(reward.getBasicItem2());
		writeD(reward.getPlayItem2());
		writeD(reward.getRankItem2());
		writeD(reward.getBonusItem1Id());
		writeD(reward.getBonusItem1Count());
		writeD(reward.getBonusItem2Id());
		writeD(reward.getBonusItem2Count());
		writeD(0);
	}

	private void fillTableWithGroup(Race race) {
		int count = 0;
		DredgionReward dredgionReward = (DredgionReward) instanceReward;
		for (Player player : players) {
			if (!race.equals(player.getRace())) {
				continue;
			}
			InstancePlayerReward playerReward = dredgionReward.getPlayerReward(player.getObjectId());
			DredgionPlayerReward dpr = (DredgionPlayerReward) playerReward;
			writeD(playerReward.getOwner());
			writeD(player.getAbyssRank().getRank().getId());
			writeD(dpr.getPvPKills());
			writeD(dpr.getMonsterKills());
			writeD(dpr.getZoneCaptured());
			writeD(dpr.getPoints());
			if (instanceScoreType.isEndProgress()) {
				boolean winner = race.equals(dredgionReward.getWinningRace());
				writeD((winner ? dredgionReward.getWinnerPoints() : dredgionReward.getLooserPoints())
						+ (int) (dpr.getPoints() * 1.6f));
				writeD((winner ? dredgionReward.getWinnerPoints() : dredgionReward.getLooserPoints()));
			} else {
				writeB(new byte[8]);
			}
			writeC(player.getPlayerClass().getClassId());
			writeC(0);
			writeS(player.getName(), 54);
			count++;
		}
		if (count < 6) {
			writeB(new byte[88 * (6 - count)]);
		}
	}

	private void kamarBattlefieldTable(Race race) {
		int count = 0;
		KamarBattlefieldReward kbr = (KamarBattlefieldReward) instanceReward;
		boolean isFirst = false;
		for (Player player : players) {
			if (!race.equals(player.getRace())) {
				continue;
			}
			KamarBattlefieldPlayerReward kbpr = kbr.getPlayerReward(player.getObjectId());
			writeD(player.getObjectId());
			writeC(player.getPlayerClass().getClassId());
			writeC(player.getAbyssRank().getRank().getId());
			writeC(0);
			writeH(0);
			writeD(kbpr.getPvPKills());
			writeD(kbpr.getPoints());
			writeS(player.getName(), 52);
			count++;
		}
		if (count < 12) {
			writeB(new byte[69 * (12 - count)]);
		}
		writeB(new byte[828]);
	}

	private void engulfedOphidanBridgeTable(Race race) {
		int count = 0;
		EngulfedOphidanBridgeReward eobr = (EngulfedOphidanBridgeReward) instanceReward;
		boolean isFirst = false;
		for (Player player : players) {
			if (!race.equals(player.getRace())) {
				continue;
			}
			EngulfedOphidanBridgePlayerReward eobpr = eobr.getPlayerReward(player.getObjectId());
			writeD(player.getObjectId());
			writeC(player.getPlayerClass().getClassId());
			writeC(player.getAbyssRank().getRank().getId());
			writeC(0);
			writeH(0);
			writeD(eobpr.getPvPKills());
			writeD(eobpr.getPoints());
			writeS(player.getName(), 52);
			count++;
		}
		if (count < 12) {
			writeB(new byte[69 * (12 - count)]);
		}
		writeB(new byte[828]);
	}

	private void ironWallWarfrontTable(Race race) {
		int count = 0;
		IronWallWarfrontReward iwwr = (IronWallWarfrontReward) instanceReward;
		boolean isFirst = false;
		for (Player player : players) {
			if (!race.equals(player.getRace())) {
				continue;
			}
			IronWallWarfrontPlayerReward iwwpr = iwwr.getPlayerReward(player.getObjectId());
			writeD(player.getObjectId());
			writeC(player.getPlayerClass().getClassId());
			writeC(player.getAbyssRank().getRank().getId());
			writeC(0);
			writeH(0);
			writeD(iwwpr.getPvPKills());
			writeD(iwwpr.getPoints());
			writeS(player.getName(), 52);
			count++;
		}
		if (count < 12) {
			writeB(new byte[69 * (12 - count)]);
		}
		writeB(new byte[828]);
	}

	private void idgelDomeTable(Race race) {
		int count = 0;
		IdgelDomeReward idr = (IdgelDomeReward) instanceReward;
		boolean isFirst = false;
		for (Player player : players) {
			if (!race.equals(player.getRace())) {
				continue;
			}
			IdgelDomePlayerReward idpr = idr.getPlayerReward(player.getObjectId());
			writeD(player.getObjectId());
			writeC(player.getPlayerClass().getClassId());
			writeC(player.getAbyssRank().getRank().getId());
			writeC(0);
			writeH(0);
			writeD(idpr.getPvPKills());
			writeD(idpr.getPoints());
			writeS(player.getName(), 52);
			count++;
		}
		if (count < 12) {
			writeB(new byte[69 * (12 - count)]);
		}
		writeB(new byte[828]);
	}

	private void landMarkTable(Race race) {
		int count = 0;
		LandMarkReward lmr = (LandMarkReward) instanceReward;
		boolean isFirst = false;
		for (Player player : players) {
			if (!race.equals(player.getRace())) {
				continue;
			}
			LandMarkPlayerReward lmpr = lmr.getPlayerReward(player.getObjectId());
			writeD(player.getObjectId());
			writeC(player.getPlayerClass().getClassId());
			writeC(player.getAbyssRank().getRank().getId());
			writeC(0);
			writeH(0);
			writeD(lmpr.getPvPKills());
			writeD(lmpr.getPoints());
			writeS(player.getName(), 52);
			count++;
		}
		if (count < 12) {
			writeB(new byte[69 * (12 - count)]);
		}
		writeB(new byte[828]);
	}

	private void evergaleCanyonTable(Race race) {
		int count = 0;
		EvergaleCanyonReward ecr = (EvergaleCanyonReward) instanceReward;
		boolean isFirst = false;
		for (Player player : players) {
			if (!race.equals(player.getRace())) {
				continue;
			}
			EvergaleCanyonPlayerReward ecpr = ecr.getPlayerReward(player.getObjectId());
			writeD(player.getObjectId());
			writeC(player.getPlayerClass().getClassId());
			writeC(player.getAbyssRank().getRank().getId());
			writeC(0);
			writeH(0);
			writeD(ecpr.getPvPKills());
			writeD(ecpr.getPoints());
			writeS(player.getName(), 52);
			count++;
		}
		if (count < 12) {
			writeB(new byte[69 * (12 - count)]);
		}
		writeB(new byte[828]);
	}

	private void writeBattlegroundReward(BattlegroundPlayerReward reward, int itemSlots) {
		writeD((int) (reward.getParticipation() * 100));
		writeD(reward.getRewardExp());
		writeD(reward.getBonusExp());
		writeD(reward.getRewardAp());
		writeD(reward.getBonusAp());
		writeD(reward.getRewardGp());
		writeD(reward.getBonusGp());
		for (int slot = 0; slot < itemSlots; slot++) {
			writeD(reward.getRewardItemId(slot));
			writeQ(reward.getRewardItemCount(slot));
		}
		writeC(1);
	}

	private void writeTreasureIslandStatuses(Race race) {
		int count = 0;
		for (Player current : players) {
			if (current.getRace() != race || count == 96) {
				continue;
			}
			writeD(15);
			writeD(current.getLifeStats().isAlreadyDead() ? 60 : 0);
			writeD(current.getObjectId());
			count++;
		}
		writeB(new byte[12 * (96 - count)]);
	}

	private void writeTreasureIslandTeam(TreasureIslandReward reward, Race race) {
		writeC(0);
		writeD(0);
		writeD(reward.getPointsByRace(race));
		writeD(race.getRaceId());
		writeD(reward.isPreparing() ? 65535 : 1);
	}

	private void writeTreasureIslandTable(Race race) {
		TreasureIslandReward treasure = (TreasureIslandReward) instanceReward;
		int count = 0;
		for (Player current : players) {
			if (current.getRace() != race || count == 96) {
				continue;
			}
			BattlegroundPlayerReward currentReward = treasure.getPlayerReward(current.getObjectId());
			if (currentReward == null) {
				continue;
			}
			writeD(current.getObjectId());
			writeC(current.getPlayerClass().getClassId());
			writeC(current.getAbyssRank().getRank().getId());
			writeC(0);
			writeH(0);
			writeD(currentReward.getPvPKills());
			writeD(currentReward.getPoints());
			writeS(current.getName(), 52);
			count++;
		}
		writeB(new byte[69 * (96 - count)]);
	}

	private void writeTreasureIslandReward(BattlegroundPlayerReward reward) {
		writeD((int) (reward.getParticipation() * 100));
		writeD(reward.getRewardExp());
		writeD(reward.getBonusExp());
		writeD(reward.getRewardAp());
		writeD(reward.getBonusAp());
		writeD(reward.getRewardGp());
		writeD(reward.getBonusGp());
		writeD(reward.getRewardItemId(0));
		writeD((int) reward.getRewardItemCount(0));
		writeD(0);
		writeB(new byte[28]);
		writeQ(0);
		writeB(new byte[16]);
		writeC(0);
		writeB(new byte[12]);
		writeB(new byte[24]);
	}
}
