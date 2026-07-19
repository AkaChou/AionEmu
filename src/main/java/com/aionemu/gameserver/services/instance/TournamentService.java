package com.aionemu.gameserver.services.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.dao.SeasonRankingDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.autogroup.EntryRequestType;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.ranking.ArenaOfTenacityRank;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.instance.InstanceBuff;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.tournament.TournamentScore;
import com.aionemu.gameserver.model.instance.tournament.TournamentSession;
import com.aionemu.gameserver.model.instance.tournament.TournamentSession.Match;
import com.aionemu.gameserver.model.instance.tournament.TournamentSession.MatchState;
import com.aionemu.gameserver.model.instance.tournament.TournamentSession.State;
import com.aionemu.gameserver.model.instance.tournament.TournamentSession.Team;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceAdmissionService.Admission;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

public final class TournamentService {
	private static final String SESSION_KEY = "tournament.session";
	private static final String LOBBY_UID_KEY = "tournament.lobbyUid";
	private static final String MATCH_ID_KEY = "tournament.matchId";
	private static final Map<Integer, Pending> pending = new HashMap<>();
	private static final Map<Integer, TournamentSession> sessionsByMatchmaker = new HashMap<>();
	private static final Map<Long, TournamentSession> sessionsByLobby = new HashMap<>();
	private static final Map<Integer, Long> playerLobbies = new HashMap<>();
	private static final Map<Integer, Admission> admissions = new HashMap<>();
	private static final Map<Long, Map<Integer, InstanceBuff>> stageBuffs = new HashMap<>();

	private TournamentService() {
	}

	public static synchronized void startLooking(Player player, MatchDefinition definition, EntryRequestType request) {
		if (definition == null || !definition.isTournament() || !definition.isOpen()) {
			return;
		}
		Registration registration = registration(player, definition, request);
		if (registration == null || registration.players().stream().anyMatch(TournamentService::registered)) {
			return;
		}
		TournamentSession session = sessionsByMatchmaker.get(definition.getInstanceMaskId());
		if (session != null && session.state() == State.REGISTERING && session.teams().size() < session.bracketSize()) {
			addTeam(session, registration);
			return;
		}
		Pending queue = pending.computeIfAbsent(definition.getInstanceMaskId(), ignored -> new Pending(definition));
		if (queue.teams().putIfAbsent(registration.team().id(), registration) != null) {
			return;
		}
		waiting(registration, definition);
		if (queue.teams().size() >= 2) {
			createLobby(queue);
		}
	}

	public static synchronized void unregister(Player player, int matchmakerId) {
		Pending queue = pending.get(matchmakerId);
		if (queue != null) {
			Registration registration = queue.registration(player.getObjectId());
			if (registration != null) {
				queue.teams().remove(registration.team().id());
				cancel(registration.players(), matchmakerId);
				if (queue.teams().isEmpty()) {
					pending.remove(matchmakerId);
				}
			}
			return;
		}
		TournamentSession session = session(player);
		if (session == null || session.matchmakerId() != matchmakerId || session.state() != State.REGISTERING) {
			return;
		}
		Team team = teamForPlayer(session, player.getObjectId());
		if (team == null) {
			return;
		}
		List<Integer> keep = session.teams().stream().map(Team::id).filter(id -> id != team.id()).toList();
		session.retainTeams(keep);
		for (TournamentSession.Member member : team.members()) {
			playerLobbies.remove(member.playerId());
			Admission admission = admissions.remove(member.playerId());
			if (admission != null) {
				admission.rollback();
			}
			Player online = worldPlayer(member.playerId());
			if (online != null) {
				PacketSendUtility.sendPacket(online, new SM_AUTO_GROUP(matchmakerId, 2));
			}
		}
		persistAndSchedule(session);
	}

	public static synchronized void pressEnter(Player player, int matchmakerId) {
		TournamentSession session = session(player);
		if (session == null || session.matchmakerId() != matchmakerId || session.state() != State.REGISTERING
				|| admissions.containsKey(player.getObjectId())) {
			return;
		}
		WorldMapInstance lobby = DynamicInstanceManager.find(session.lobbyUid());
		Team team = teamForPlayer(session, player.getObjectId());
		if (lobby == null || team == null) {
			return;
		}
		byte side = (byte) Math.max(0, session.teams().indexOf(team));
		Admission admission = InstanceAdmissionService.admitMatch(lobby, player, side);
		if (admission == null) {
			return;
		}
		Position position = position(tournament(session), side % 2 == 0 ? "lobby_start_01" : "lobby_start_02",
				memberIndex(team, player.getObjectId()));
		admissions.put(player.getObjectId(), admission);
		if (!TeleportService2.teleportTo(player, lobby.getMapId(), lobby.getInstanceId(), position.x(), position.y(),
				position.z(), position.heading())) {
			admissions.remove(player.getObjectId());
			admission.rollback();
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(matchmakerId, 5));
	}

	public static synchronized void cancelEnter(Player player, int matchmakerId) {
		Admission admission = admissions.remove(player.getObjectId());
		if (admission != null) {
			admission.rollback();
		}
		unregister(player, matchmakerId);
	}

	public static synchronized void showWindow(Player player, MatchDefinition definition) {
		if (definition != null && definition.isTournament() && definition.isOpen()
				&& definition.hasLevelPermit(player.getLevel())) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(definition.getInstanceMaskId()));
		}
	}

	public static synchronized void attachInstance(WorldMapInstance instance) {
		Row tournament = DataManager.RETAIL_INSTANCE_DATA.tournamentForLobbyWorld(instance.getMapId());
		if (tournament != null) {
			String encoded = instance.getRuntimeState().get(SESSION_KEY);
			if (encoded == null || encoded.isBlank()) {
				return;
			}
			TournamentSession session = TournamentSession.decode(encoded);
			sessionsByLobby.put(session.lobbyUid(), session);
			sessionsByMatchmaker.put(session.matchmakerId(), session);
			for (Team team : session.teams()) {
				for (TournamentSession.Member member : team.members()) {
					playerLobbies.put(member.playerId(), session.lobbyUid());
				}
			}
			persistAndSchedule(session);
			return;
		}
		if (DataManager.RETAIL_INSTANCE_DATA.tournamentForStageWorld(instance.getMapId()) != null) {
			attachStage(instance, 0);
		}
	}

	public static synchronized void onEnterInstance(WorldMapInstance instance, Player player) {
		admissions.remove(player.getObjectId());
		TournamentSession session = session(instance);
		if (session == null) {
			return;
		}
		if (instance.getInstanceUid() == session.lobbyUid()) {
			sendLobbyScore(session, player, 0);
		} else {
			int matchId = instance.getRuntimeState().getInt(MATCH_ID_KEY, 0);
			sendStageScore(session, matchId, player);
		}
	}

	public static synchronized void onInstanceDestroy(WorldMapInstance instance) {
		stageBuffs.remove(instance.getInstanceUid());
		TournamentSession session = sessionsByLobby.get(instance.getInstanceUid());
		if (session == null) {
			return;
		}
		sessionsByLobby.remove(session.lobbyUid());
		sessionsByMatchmaker.remove(session.matchmakerId(), session);
		for (Team team : session.teams()) {
			for (TournamentSession.Member member : team.members()) {
				playerLobbies.remove(member.playerId(), session.lobbyUid());
				Admission admission = admissions.remove(member.playerId());
				if (admission != null) {
					admission.rollback();
				}
			}
		}
	}

	public static synchronized boolean onDie(WorldMapInstance stage, Player victim, Creature lastAttacker) {
		TournamentSession session = session(stage);
		int matchId = stage.getRuntimeState().getInt(MATCH_ID_KEY, 0);
		Match match = session == null ? null : session.match(matchId);
		if (match == null || !match.state().active()) {
			return false;
		}
		int victimTeam = teamId(session, victim.getObjectId());
		if (victimTeam == 0 || victimTeam != match.teamA() && victimTeam != match.teamB()) {
			return false;
		}
		Creature master = lastAttacker == null ? null : lastAttacker.getMaster();
		int attackerTeam = master instanceof Player ? teamId(session, master.getObjectId()) : 0;
		int winnerTeam = attackerTeam != 0 && attackerTeam != victimTeam ? attackerTeam
				: victimTeam == match.teamA() ? match.teamB() : match.teamA();
		Row tournament = tournament(session);
		if ("Party_relay".equals(tournament.value("type"))) {
			session.addScore(matchId, winnerTeam, 1);
			int next = session.advanceRelay(matchId, victimTeam);
			Team team = session.team(victimTeam);
			if (next >= team.members().size()) {
				finishMatch(session, matchId, winnerTeam);
			} else {
				returnToLobby(session, victim);
				Player relay = worldPlayer(team.members().get(next).playerId());
				if (relay != null) {
					moveToStage(session, stage, match, relay, victimTeam == match.teamA() ? 0 : 1, next);
					protect(relay, 1);
				}
				persistAndSchedule(session);
				sendStageScore(session, matchId, stage);
			}
			return true;
		}
		int score = session.addScore(matchId, winnerTeam, 1);
		int threshold = tournament.requiredInt("round_" + (match.round() + 1) + "_win_kill_point");
		if (score >= threshold) {
			finishMatch(session, matchId, winnerTeam);
		} else {
			setDoors(stage, false);
			persistAndSchedule(session);
			sendStageScore(session, matchId, stage);
			long reopen = Math.max(15, tournament.intValue("door_reopen_time", 15)) * 1000L;
			GameThreadPoolServices.threadPoolManager().schedule(() -> reviveAndReset(session.lobbyUid(), matchId,
					victim.getObjectId(), reopen), 1000);
		}
		return true;
	}

	public static synchronized boolean onRevive(WorldMapInstance instance, Player player) {
		TournamentSession session = session(instance);
		if (session == null || instance.getInstanceUid() == session.lobbyUid()) {
			return false;
		}
		PlayerReviveService.revive(player, 100, 100, false, 0);
		player.getGameStats().updateStatsAndSpeedVisually();
		return true;
	}

	private static Registration registration(Player player, MatchDefinition definition, EntryRequestType request) {
		List<Player> players;
		int teamId;
		if (definition.hasRegisterGroup()) {
			if (request != EntryRequestType.GROUP_ENTRY || !player.isInGroup2()
					|| !player.getPlayerGroup2().isLeader(player)) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_NOT_LEADER);
				return null;
			}
			players = new ArrayList<>(player.getPlayerGroup2().getOnlineMembers());
			teamId = player.getPlayerGroup2().getTeamId();
		} else {
			if (request != EntryRequestType.NEW_GROUP_ENTRY) {
				return null;
			}
			players = List.of(player);
			teamId = player.getObjectId();
		}
		if (players.size() < definition.getMinimumPlayersPerSide() || players.size() > definition.getPlayersPerSide()) {
			return null;
		}
		List<TournamentSession.Member> members = new ArrayList<>(players.size());
		for (int order = 0; order < players.size(); order++) {
			Player member = players.get(order);
			if (!definition.hasLevelPermit(member.getLevel())
					|| !InstanceLimitService.status(member, definition.getInstanceMapId()).allowed()) {
				PacketSendUtility.sendPacket(member, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME);
				return null;
			}
			members.add(new TournamentSession.Member(member.getObjectId(), member.getName(), member.getLevel(),
					member.getPlayerClass().getClassId(), order));
		}
		return new Registration(new Team(teamId, members), List.copyOf(players));
	}

	private static boolean registered(Player player) {
		if (playerLobbies.containsKey(player.getObjectId()) || admissions.containsKey(player.getObjectId())) {
			return true;
		}
		return pending.values().stream().anyMatch(queue -> queue.registration(player.getObjectId()) != null);
	}

	private static void waiting(Registration registration, MatchDefinition definition) {
		for (Player member : registration.players()) {
			if (definition.hasHudRegister()) {
				PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(definition.getInstanceMaskId(), 6, true));
			}
			PacketSendUtility.sendPacket(member, new SM_SYSTEM_MESSAGE(1400194, definition.getInstanceMapId()));
			PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(definition.getInstanceMaskId(), 1,
					definition.hasRegisterGroup() ? EntryRequestType.GROUP_ENTRY.getId() : EntryRequestType.NEW_GROUP_ENTRY.getId(),
					registration.players().getFirst().getName()));
		}
	}

	private static void createLobby(Pending queue) {
		MatchDefinition definition = queue.definition();
		Row tournament = DataManager.RETAIL_INSTANCE_DATA.tournament(definition.getTournamentId());
		WorldMapInstance lobby = InstanceService.getNextAvailableInstance(tournament.requiredInt("lobby_world_id"), 0,
				tournament.requiredInt("lobby_creation_id"), DynamicInstance.OWNER_MATCH,
				definition.getInstanceMaskId(), (byte) tournament.intValue("lobby_spawn_page", 0));
		long deadline = System.currentTimeMillis() + Math.max(1_000L, definition.getTime());
		TournamentSession session = new TournamentSession(definition.getTournamentId(), definition.getInstanceMaskId(),
				lobby.getInstanceUid(), definition.getMatchSides(), deadline);
		sessionsByLobby.put(session.lobbyUid(), session);
		sessionsByMatchmaker.put(session.matchmakerId(), session);
		for (Registration registration : queue.teams().values()) {
			if (session.teams().size() >= session.bracketSize()) {
				break;
			}
			addTeam(session, registration);
		}
		pending.remove(definition.getInstanceMaskId());
		persistAndSchedule(session);
		attachInstance(lobby);
	}

	private static void addTeam(TournamentSession session, Registration registration) {
		if (!session.addTeam(registration.team())) {
			return;
		}
		for (Player member : registration.players()) {
			playerLobbies.put(member.getObjectId(), session.lobbyUid());
			PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(session.matchmakerId(), 4));
		}
		persistAndSchedule(session);
	}

	private static void freezeRegistration(long lobbyUid, int expectedVersion, long expectedDeadline) {
		TournamentSession session = sessionsByLobby.get(lobbyUid);
		if (session == null || session.state() != State.REGISTERING || session.stateVersion() != expectedVersion
				|| session.deadline() != expectedDeadline) {
			return;
		}
		WorldMapInstance lobby = DynamicInstanceManager.find(lobbyUid);
		if (lobby == null) {
			return;
		}
		List<Integer> admitted = session.teams().stream().filter(team -> team.members().stream()
				.allMatch(member -> DynamicInstanceManager.hasJoined(lobby, member.playerId()))).map(Team::id).toList();
		session.retainTeams(admitted);
		if (session.teams().size() < 2) {
			cancelSession(session, lobby);
			return;
		}
		session.freeze(System.currentTimeMillis());
		persist(session);
		beginRound(session.lobbyUid(), session.stateVersion(), session.deadline());
	}

	private static void beginRound(long lobbyUid, int expectedVersion, long expectedDeadline) {
		TournamentSession session = sessionsByLobby.get(lobbyUid);
		if (session == null || session.state() != State.BETWEEN_ROUNDS || session.stateVersion() != expectedVersion
				|| session.deadline() != expectedDeadline) {
			return;
		}
		Row tournament = tournament(session);
		long waitDeadline = System.currentTimeMillis() + tournament.requiredInt("wait_time") * 1000L;
		List<Match> matches = session.createRound(waitDeadline);
		for (Match match : matches) {
			WorldMapInstance stage = InstanceService.getNextAvailableInstance(tournament.requiredInt("stage_world_id"), 0,
					tournament.requiredInt("stage_creation_id"), DynamicInstance.OWNER_MATCH, session.matchmakerId(),
					(byte) tournament.intValue("stage_spawn_page", 0));
			stage.getRuntimeState().put(LOBBY_UID_KEY, session.lobbyUid());
			stage.getRuntimeState().put(MATCH_ID_KEY, match.id());
			session.bindStage(match.id(), stage.getInstanceUid());
			reserveAndMove(session, stage, match, tournament);
			attachInstance(stage);
		}
		persistAndSchedule(session);
		broadcastLobby(session, 4);
	}

	private static void reserveAndMove(TournamentSession session, WorldMapInstance stage, Match match, Row tournament) {
		boolean relay = "Party_relay".equals(tournament.value("type"));
		for (int side = 0; side < 2; side++) {
			Team team = session.team(side == 0 ? match.teamA() : match.teamB());
			for (int index = 0; index < team.members().size(); index++) {
				Player player = worldPlayer(team.members().get(index).playerId());
				if (player == null) {
					continue;
				}
				DynamicInstanceManager.reserveMember(stage, player, team.id(), (byte) side);
				if (!relay || index == 0) {
					moveToStage(session, stage, match, player, side, index);
				}
			}
		}
	}

	private static void startWaiting(long lobbyUid, int expectedVersion, long expectedDeadline) {
		TournamentSession session = sessionsByLobby.get(lobbyUid);
		if (session == null || session.state() != State.ROUND_ACTIVE || session.stateVersion() != expectedVersion) {
			return;
		}
		Row tournament = tournament(session);
		long playDeadline = System.currentTimeMillis() + tournament.requiredInt("limit_time") * 1000L;
		for (Match match : session.matches()) {
			if (match.state() == MatchState.WAITING && match.deadline() == expectedDeadline) {
				session.startMatch(match.id(), playDeadline);
				WorldMapInstance stage = DynamicInstanceManager.find(match.stageUid());
				if (stage != null) {
					setDoors(stage, true);
					if ("Party_normal".equals(tournament.value("type"))) {
						RetailConditionSpawnEngine.setVariable(stage, "partytournament_start", 1, 0);
						applyPartyBuff(stage);
					}
					sendStageScore(session, match.id(), stage);
				}
			}
		}
		persistAndSchedule(session);
	}

	private static void timeout(long lobbyUid, int matchId, int expectedVersion, long expectedDeadline) {
		TournamentSession session = sessionsByLobby.get(lobbyUid);
		Match match = session == null ? null : session.match(matchId);
		if (match == null || session.stateVersion() != expectedVersion || match.deadline() != expectedDeadline
				|| !match.state().active()) {
			return;
		}
		Row tournament = tournament(session);
		if (match.state() == MatchState.PLAYING && match.scoreA() == match.scoreB()
				&& tournament.intValue("over_time", 0) > 0) {
			session.startOvertime(matchId, System.currentTimeMillis() + tournament.requiredInt("over_time") * 1000L);
			persistAndSchedule(session);
			sendStageScore(session, matchId, DynamicInstanceManager.find(match.stageUid()));
			return;
		}
		finishMatch(session, matchId, decideWinner(session, match));
	}

	private static int decideWinner(TournamentSession session, Match match) {
		if (match.scoreA() != match.scoreB()) {
			return match.scoreA() > match.scoreB() ? match.teamA() : match.teamB();
		}
		WorldMapInstance stage = DynamicInstanceManager.find(match.stageUid());
		int aliveA = alive(stage, session.team(match.teamA()));
		int aliveB = alive(stage, session.team(match.teamB()));
		if (aliveA != aliveB) {
			return aliveA > aliveB ? match.teamA() : match.teamB();
		}
		if (match.killsA() != match.killsB()) {
			return match.killsA() > match.killsB() ? match.teamA() : match.teamB();
		}
		return ThreadLocalRandom.current().nextBoolean() ? match.teamA() : match.teamB();
	}

	private static void finishMatch(TournamentSession session, int matchId, int winnerId) {
		Match match = session.match(matchId);
		if (match == null || match.state() == MatchState.FINISHED) {
			return;
		}
		WorldMapInstance lobby = DynamicInstanceManager.find(session.lobbyUid());
		grantRound(lobby, session, session.team(match.teamA()), match.round() + 1);
		grantRound(lobby, session, session.team(match.teamB()), match.round() + 1);
		session.finishMatch(matchId, winnerId, System.currentTimeMillis());
		WorldMapInstance stage = DynamicInstanceManager.find(match.stageUid());
		if (stage != null) {
			setDoors(stage, false);
			sendStageScore(session, matchId, stage);
			for (Player player : List.copyOf(stage.getPlayersInside())) {
				returnToLobby(session, player);
			}
			GameThreadPoolServices.threadPoolManager().schedule(() -> destroyStage(match.stageUid()), 5_000);
		}
		if (session.state() == State.COMPLETE) {
			int extraStart = session.roundCount() + 1;
			for (int round = extraStart; round <= tournament(session).requiredInt("round_count"); round++) {
				grantRound(lobby, session, session.team(session.champion()), round);
			}
			persist(session);
			storePlacements(session);
			broadcastLobby(session, 5);
			GameThreadPoolServices.threadPoolManager().schedule(() -> destroyLobby(session.lobbyUid()), 60_000);
		} else {
			persistAndSchedule(session);
			broadcastLobby(session, 4);
		}
	}

	private static void grantRound(WorldMapInstance lobby, TournamentSession session, Team team, int round) {
		if (lobby == null || team == null || round > tournament(session).requiredInt("round_count")) {
			return;
		}
		InstanceSettlementService.RewardPlan plan = InstanceSettlementService.tournamentPlan(tournament(session), round);
		String key = "tournament:" + session.tournamentId() + ":round:" + round;
		for (TournamentSession.Member member : team.members()) {
			Player player = worldPlayer(member.playerId());
			if (player == null) {
				InstanceSettlementService.queue(lobby, member.playerId(), key, plan);
			} else {
				InstanceSettlementService.settle(lobby.getInstanceUid(), player, key, plan);
			}
		}
	}

	private static void storePlacements(TournamentSession session) {
		for (Team team : session.teams()) {
			int placement = session.placement(team.id());
			for (TournamentSession.Member member : team.members()) {
				Player player = worldPlayer(member.playerId());
				if (player == null) {
					continue;
				}
				ArenaOfTenacityRank rank = player.getTenacityRank();
				if (rank == null) {
					rank = new ArenaOfTenacityRank(0, 0, 0, 0, 0, 0, placement);
					rank.setPersistentState(PersistentState.NEW);
					player.setTenacityRank(rank);
				} else {
					rank.setPossitionMatch(placement);
					rank.setPersistentState(PersistentState.UPDATE_REQUIRED);
				}
				DAOManager.getDAO(SeasonRankingDAO.class).storeTenacityRank(player);
			}
		}
	}

	private static void persistAndSchedule(TournamentSession session) {
		persist(session);
		schedule(session);
	}

	private static void persist(TournamentSession session) {
		WorldMapInstance lobby = DynamicInstanceManager.find(session.lobbyUid());
		if (lobby != null) {
			lobby.getRuntimeState().put(SESSION_KEY, session.encode());
		}
	}

	private static void schedule(TournamentSession session) {
		int version = session.stateVersion();
		long now = System.currentTimeMillis();
		if (session.state() == State.REGISTERING) {
			scheduleAt(session.deadline(), () -> freezeRegistration(session.lobbyUid(), version, session.deadline()), now);
		} else if (session.state() == State.BETWEEN_ROUNDS) {
			scheduleAt(session.deadline(), () -> beginRound(session.lobbyUid(), version, session.deadline()), now);
		} else if (session.state() == State.ROUND_ACTIVE) {
			for (Match match : session.matches()) {
				if (match.state() == MatchState.WAITING) {
					long deadline = match.deadline();
					scheduleAt(deadline, () -> startWaiting(session.lobbyUid(), version, deadline), now);
				} else if (match.state().active()) {
					long deadline = match.deadline();
					scheduleAt(deadline, () -> timeout(session.lobbyUid(), match.id(), version, deadline), now);
				}
			}
		}
	}

	private static void scheduleAt(long deadline, Runnable action, long now) {
		GameThreadPoolServices.threadPoolManager().schedule(action, Math.max(1, deadline - now));
	}

	private static void attachStage(WorldMapInstance stage, int attempt) {
		long lobbyUid = stage.getRuntimeState().getLong(LOBBY_UID_KEY, 0);
		TournamentSession session = sessionsByLobby.get(lobbyUid);
		if (session != null) {
			setDoors(stage, false);
			persistAndSchedule(session);
			return;
		}
		if (lobbyUid > 0 && attempt < 20) {
			GameThreadPoolServices.threadPoolManager().schedule(() -> {
				synchronized (TournamentService.class) {
					attachStage(stage, attempt + 1);
				}
			}, 250);
		}
	}

	private static TournamentSession session(WorldMapInstance instance) {
		long lobbyUid = instance.getRuntimeState().getLong(LOBBY_UID_KEY, instance.getInstanceUid());
		return sessionsByLobby.get(lobbyUid);
	}

	private static TournamentSession session(Player player) {
		Long uid = playerLobbies.get(player.getObjectId());
		return uid == null ? null : sessionsByLobby.get(uid);
	}

	private static Row tournament(TournamentSession session) {
		Row row = DataManager.RETAIL_INSTANCE_DATA.tournament(session.tournamentId());
		if (row == null) {
			throw new IllegalStateException("Missing tournament " + session.tournamentId());
		}
		return row;
	}

	private static Team teamForPlayer(TournamentSession session, int playerId) {
		return session.teams().stream().filter(team -> team.members().stream()
				.anyMatch(member -> member.playerId() == playerId)).findFirst().orElse(null);
	}

	private static int teamId(TournamentSession session, int playerId) {
		Team team = teamForPlayer(session, playerId);
		return team == null ? 0 : team.id();
	}

	private static int memberIndex(Team team, int playerId) {
		for (int i = 0; i < team.members().size(); i++) {
			if (team.members().get(i).playerId() == playerId) {
				return i;
			}
		}
		return 0;
	}

	private static Player worldPlayer(int playerId) {
		return GameWorldBootstrapServices.world().findPlayer(playerId);
	}

	private static void moveToStage(TournamentSession session, WorldMapInstance stage, Match match, Player player,
			int side, int order) {
		Position position = position(tournament(session), side == 0 ? "stage_start_01" : "stage_start_02", order);
		TeleportService2.teleportTo(player, stage.getMapId(), stage.getInstanceId(), position.x(), position.y(),
				position.z(), position.heading());
	}

	private static void returnToLobby(TournamentSession session, Player player) {
		WorldMapInstance lobby = DynamicInstanceManager.find(session.lobbyUid());
		Team team = teamForPlayer(session, player.getObjectId());
		if (lobby == null || team == null) {
			return;
		}
		if (player.getLifeStats().isAlreadyDead()) {
			PlayerReviveService.revive(player, 100, 100, false, 0);
		}
		int side = Math.max(0, session.teams().indexOf(team));
		Position position = position(tournament(session), side % 2 == 0 ? "lobby_start_01" : "lobby_start_02",
				memberIndex(team, player.getObjectId()));
		TeleportService2.teleportTo(player, lobby.getMapId(), lobby.getInstanceId(), position.x(), position.y(),
				position.z(), position.heading());
	}

	private static Position position(Row tournament, String key, int index) {
		String[] positions = tournament.value(key).split(";");
		String[] values = positions[Math.floorMod(index, positions.length)].split(",");
		if (values.length != 4) {
			throw new IllegalStateException("Invalid tournament position " + key);
		}
		return new Position(Float.parseFloat(values[0]), Float.parseFloat(values[1]), Float.parseFloat(values[2]),
				(byte) Math.round(Float.parseFloat(values[3]) / 3f));
	}

	private static void sendLobbyScore(TournamentSession session, Player player, int packetType) {
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(tournament(session).requiredInt("lobby_world_id"),
				remaining(session.deadline()), scoreType(session), TournamentScore.lobby(session, packetType)));
	}

	private static void broadcastLobby(TournamentSession session, int packetType) {
		for (Team team : session.teams()) {
			for (TournamentSession.Member member : team.members()) {
				Player player = worldPlayer(member.playerId());
				if (player != null) {
					sendLobbyScore(session, player, packetType);
				}
			}
		}
	}

	private static void sendStageScore(TournamentSession session, int matchId, Player player) {
		if (player == null || session.match(matchId) == null) {
			return;
		}
		Match match = session.match(matchId);
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(tournament(session).requiredInt("stage_world_id"),
				remaining(match.deadline()), scoreType(session), TournamentScore.stage(session, matchId, 4)));
	}

	private static void sendStageScore(TournamentSession session, int matchId, WorldMapInstance stage) {
		if (stage != null) {
			for (Player player : stage.getPlayersInside()) {
				sendStageScore(session, matchId, player);
			}
		}
	}

	private static InstanceScoreType scoreType(TournamentSession session) {
		return session.state() == State.COMPLETE ? InstanceScoreType.END_PROGRESS
				: session.state() == State.ROUND_ACTIVE ? InstanceScoreType.START_PROGRESS : InstanceScoreType.PREPARING;
	}

	private static int remaining(long deadline) {
		return deadline <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, Math.max(0, deadline - System.currentTimeMillis()));
	}

	private static void setDoors(WorldMapInstance instance, boolean open) {
		for (StaticDoor door : instance.getDoors().values()) {
			door.setOpen(open);
		}
	}

	private static void applyPartyBuff(WorldMapInstance stage) {
		Map<Integer, InstanceBuff> buffs = stageBuffs.computeIfAbsent(stage.getInstanceUid(), ignored -> new HashMap<>());
		for (Player player : stage.getPlayersInside()) {
			InstanceBuff buff = new InstanceBuff(8);
			buff.applyEffect(player, 10_000);
			buffs.put(player.getObjectId(), buff);
		}
	}

	private static void protect(Player player, int seconds) {
		player.getController().startProtectionActiveTask();
		GameThreadPoolServices.threadPoolManager().schedule(player.getController()::stopProtectionActiveTask,
				seconds * 1000L);
	}

	private static void reviveAndReset(long lobbyUid, int matchId, int playerId, long reopenDelay) {
		synchronized (TournamentService.class) {
			TournamentSession session = sessionsByLobby.get(lobbyUid);
			Match match = session == null ? null : session.match(matchId);
			Player player = worldPlayer(playerId);
			if (match == null || !match.state().active() || player == null) {
				return;
			}
			PlayerReviveService.revive(player, 100, 100, false, 0);
			player.getGameStats().updateStatsAndSpeedVisually();
			WorldMapInstance stage = DynamicInstanceManager.find(match.stageUid());
			if (stage != null) {
				int team = teamId(session, playerId);
				moveToStage(session, stage, match, player, team == match.teamA() ? 0 : 1,
						memberIndex(session.team(team), playerId));
				protect(player, 5);
				GameThreadPoolServices.threadPoolManager().schedule(() -> setDoors(stage, true), reopenDelay);
			}
		}
	}

	private static int alive(WorldMapInstance stage, Team team) {
		if (stage == null || team == null) {
			return 0;
		}
		int alive = 0;
		for (TournamentSession.Member member : team.members()) {
			Player player = stage.getPlayer(member.playerId());
			if (player != null && !player.getLifeStats().isAlreadyDead()) {
				alive++;
			}
		}
		return alive;
	}

	private static void cancelSession(TournamentSession session, WorldMapInstance lobby) {
		for (Team team : session.teams()) {
			for (TournamentSession.Member member : team.members()) {
				Player player = worldPlayer(member.playerId());
				if (player != null) {
					PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(session.matchmakerId(), 7));
				}
			}
		}
		InstanceService.destroyInstance(lobby);
	}

	private static void cancel(List<Player> players, int matchmakerId) {
		for (Player player : players) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(matchmakerId, 2));
		}
	}

	private static void destroyStage(long stageUid) {
		synchronized (TournamentService.class) {
			WorldMapInstance stage = DynamicInstanceManager.find(stageUid);
			if (stage != null) {
				InstanceService.destroyInstance(stage);
			}
		}
	}

	private static void destroyLobby(long lobbyUid) {
		synchronized (TournamentService.class) {
			WorldMapInstance lobby = DynamicInstanceManager.find(lobbyUid);
			if (lobby != null) {
				InstanceService.destroyInstance(lobby);
			}
		}
	}

	private record Pending(MatchDefinition definition, Map<Integer, Registration> teams) {
		private Pending(MatchDefinition definition) {
			this(definition, new LinkedHashMap<>());
		}

		private Registration registration(int playerId) {
			return teams.values().stream().filter(registration -> registration.team().members().stream()
					.anyMatch(member -> member.playerId() == playerId)).findFirst().orElse(null);
		}
	}

	private record Registration(Team team, List<Player> players) {
	}

	private record Position(float x, float y, float z, byte heading) {
	}
}
