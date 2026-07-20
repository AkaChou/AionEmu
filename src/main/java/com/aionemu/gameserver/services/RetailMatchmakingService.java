package com.aionemu.gameserver.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.autogroup.AGPlayer;
import com.aionemu.gameserver.model.autogroup.AutoInstance;
import com.aionemu.gameserver.model.autogroup.EntryRequestType;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.autogroup.RetailMatchPlanner;
import com.aionemu.gameserver.model.autogroup.RetailMatchPlanner.Assignment;
import com.aionemu.gameserver.model.autogroup.RetailMatchPlanner.Member;
import com.aionemu.gameserver.model.autogroup.RetailMatchPlanner.Party;
import com.aionemu.gameserver.model.autogroup.RetailMatchPlanner.Plan;
import com.aionemu.gameserver.model.autogroup.RetailMatchSession;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.DynamicInstanceManager;
import com.aionemu.gameserver.services.instance.InstanceAdmissionService;
import com.aionemu.gameserver.services.instance.InstanceLimitService;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

import lombok.extern.slf4j.Slf4j;

/** 5.8 真端 matchmaker 单轨运行服务。 */
@Slf4j
public final class RetailMatchmakingService extends AutoGroupService {
	private static final String SESSION_KEY = "match.session";
	private static final long MATCH_TURN_MILLIS = 3_000;
	private static final long READY_TIMEOUT_MILLIS = 120_000;
	private static final long PENALTY_MILLIS = 10_000;

	private final AtomicLong sequence = new AtomicLong();
	private final Map<Integer, List<Registration>> queues = new HashMap<>();
	private final Map<PlayerMatch, Registration> registrations = new HashMap<>();
	private final Map<Long, ActiveMatch> activeByInstance = new HashMap<>();
	private final Map<Integer, ActiveMatch> activeByPlayer = new HashMap<>();
	private final Map<Integer, Long> penalties = new HashMap<>();
	private final Map<Integer, Boolean> hudOpen = new HashMap<>();
	private Future<?> matchingTask;
	private Future<?> hudTask;

	public synchronized void startScheduleNotifications() {
		if (hudTask != null && !hudTask.isDone()) {
			return;
		}
		refreshHud(false);
		hudTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
			synchronized (RetailMatchmakingService.this) {
				refreshHud(true);
			}
		}, 60_000, 60_000);
	}

	public synchronized void shutdown() {
		if (matchingTask != null) {
			matchingTask.cancel(false);
			matchingTask = null;
		}
		if (hudTask != null) {
			hudTask.cancel(false);
			hudTask = null;
		}
	}

	@Override
	public synchronized void startLooking(Player player, int instanceMaskId, EntryRequestType requestType) {
		MatchDefinition definition = MatchDefinition.getByMaskId(instanceMaskId);
		if (definition == null || definition.isTournament() || !canQueue(player, requestType, definition)) {
			return;
		}
		List<Player> members = members(player, requestType);
		if (members.isEmpty() || members.stream().anyMatch(member -> registrations.containsKey(
				new PlayerMatch(member.getObjectId(), instanceMaskId)))) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400181, definition.getInstanceMapId()));
			return;
		}
		long now = System.currentTimeMillis();
		if (members.stream().anyMatch(member -> penalties.getOrDefault(member.getObjectId(), 0L) > now)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400181, definition.getInstanceMapId()));
			return;
		}
		Registration registration = new Registration(sequence.incrementAndGet(), now, instanceMaskId, requestType,
				teamId(player, requestType), definition.isTeamMatch()
						? GameRuntimeServices.findGroupService().instanceGroupEntryId(player, instanceMaskId) : 0,
				members.stream().map(QueuedMember::new).toList());
		queues.computeIfAbsent(instanceMaskId, ignored -> new ArrayList<>()).add(registration);
		for (QueuedMember member : registration.members) {
			registrations.put(new PlayerMatch(member.playerId, instanceMaskId), registration);
		}
		for (Player member : members) {
			sendQueued(member, definition, requestType, player.getName());
		}
		ensureTimer();
		runTurn();
	}

	public synchronized void unregisterLooking(Player player, int instanceMaskId) {
		unregisterLooking(player, instanceMaskId, true);
	}

	public synchronized void unregisterLooking(Player player, int instanceMaskId, boolean penalty) {
		Registration registration = registrations.get(new PlayerMatch(player.getObjectId(), instanceMaskId));
		if (registration != null) {
			removeRegistration(registration, true, penalty);
		}
	}

	public synchronized void refreshLooking(Player player, int instanceMaskId) {
		Registration registration = registrations.get(new PlayerMatch(player.getObjectId(), instanceMaskId));
		if (registration != null) {
			removeRegistration(registration, true, false);
		}
	}

	public synchronized void onTeamChanged(TemporaryPlayerTeam<?> team) {
		Set<Integer> currentMembers = team.getMembers().stream().map(Player::getObjectId)
				.collect(java.util.stream.Collectors.toSet());
		for (ActiveMatch match : new HashSet<>(activeByPlayer.values())) {
			if (!match.definition.isTeamMatch()) {
				continue;
			}
			boolean changed = false;
			for (RetailMatchSession.Member member : match.session.members()) {
				if (member.teamId() != team.getTeamId() || member.entered() || currentMembers.contains(member.playerId())) {
					continue;
				}
				Player player = player(member.playerId());
				if (player != null) {
					match.adapter.unregister(player);
					InstanceAdmissionService.cancelMatchReservation(match.instance, player);
					closeTeamMatchWindow(player, match, false);
					sendCancelled(player, match.definition);
				} else {
					match.adapter.players.remove(member.playerId());
					InstanceAdmissionService.cancelMatchReservation(match.instance, member.playerId());
				}
				match.session.remove(member.playerId(), "TEAM_CHANGED");
				activeByPlayer.remove(member.playerId(), match);
				penalties.put(member.playerId(), System.currentTimeMillis() + PENALTY_MILLIS);
				changed = true;
			}
			if (changed) {
				persist(match);
				if (!finishIfEmpty(match, "TEAM_CHANGED")) {
					sendTeamMatchUpdates(match);
					scheduleReadyTimeout(match);
				}
			}
		}
	}

	public synchronized void pressEnterTeamMatch(Player player) {
		ActiveMatch match = activeByPlayer.get(player.getObjectId());
		if (match != null && match.definition.isTeamMatch()) {
			pressEnter(player, match.definition.getInstanceMaskId());
		}
	}

	@Override
	public void unregisterLooking(Player player, byte instanceMaskId) {
		unregisterLooking(player, Byte.toUnsignedInt(instanceMaskId));
	}

	@Override
	public synchronized void pressEnter(Player player, int instanceMaskId) {
		ActiveMatch match = activeByPlayer.get(player.getObjectId());
		if (match == null || match.definition.getInstanceMaskId() != instanceMaskId) {
			return;
		}
		RetailMatchSession.Member member = match.session.member(player.getObjectId());
		AGPlayer matchPlayer = match.adapter.players.get(player.getObjectId());
		if (member == null || member.entered() || matchPlayer == null) {
			return;
		}
		match.adapter.onPressEnter(player);
		if (!matchPlayer.isPressedEnter()) {
			return;
		}
		match.session.pressEnter(player.getObjectId());
		persist(match);
		if (!match.definition.isTeamMatch()) {
			if (player.isInGroup2()) {
				PlayerGroupService.removePlayer(player);
			}
			if (player.isInAlliance2()) {
				PlayerAllianceService.removePlayer(player);
			}
		}
		PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 5));
		closeTeamMatchWindow(player, match, true);
	}

	@Override
	public synchronized void cancelEnter(Player player, int instanceMaskId) {
		ActiveMatch match = activeByPlayer.get(player.getObjectId());
		if (match == null || match.definition.getInstanceMaskId() != instanceMaskId) {
			return;
		}
		RetailMatchSession.Member member = match.session.member(player.getObjectId());
		if (member == null || member.entered()) {
			return;
		}
		match.adapter.unregister(player);
		InstanceAdmissionService.cancelMatchReservation(match.instance, player);
		closeTeamMatchWindow(player, match, false);
		match.session.remove(player.getObjectId(), "VOLUNTARY_CANCEL");
		activeByPlayer.remove(player.getObjectId(), match);
		penalties.put(player.getObjectId(), System.currentTimeMillis() + PENALTY_MILLIS);
		persist(match);
		sendCancelled(player, match.definition);
		finishIfEmpty(match, "VOLUNTARY_CANCEL");
	}

	@Override
	public synchronized void onEnterInstance(Player player) {
		ActiveMatch match = activeByPlayer.get(player.getObjectId());
		if (match == null || match.instance != player.getPosition().getWorldMapInstance()
				|| !match.adapter.players.containsKey(player.getObjectId())) {
			return;
		}
		match.adapter.onEnterInstance(player);
		match.session.markEntered(player.getObjectId());
		persist(match);
	}

	@Override
	public synchronized void onLeaveInstance(Player player) {
		ActiveMatch match = activeByPlayer.get(player.getObjectId());
		if (match == null || match.instance != player.getPosition().getWorldMapInstance()
				|| match.session.member(player.getObjectId()) == null) {
			return;
		}
		match.adapter.onLeaveInstance(player);
		match.session.leave(player.getObjectId());
		activeByPlayer.remove(player.getObjectId(), match);
		DynamicInstanceManager.revokeMember(match.instance, player.getObjectId());
		persist(match);
		finishIfEmpty(match, "ALL_PLAYERS_LEFT");
		runTurn();
	}

	@Override
	public synchronized void onPlayerLogin(Player player) {
		for (MatchDefinition definition : MatchDefinition.all()) {
			if (!definition.isTournament() && definition.hasHudRegister()) {
				boolean close = !definition.isOpen() || !definition.hasLevelPermit(player.getLevel())
						|| !InstanceLimitService.status(player, definition.getInstanceMapId()).allowed();
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(definition.getInstanceMaskId(),
						SM_AUTO_GROUP.wnd_EntryIcon, close));
			}
		}
		for (Map.Entry<PlayerMatch, Registration> entry : registrations.entrySet()) {
			if (entry.getKey().playerId == player.getObjectId()) {
				Registration registration = entry.getValue();
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(registration.matchmakerId, 8,
						waitTime(registration), player.getName()));
			}
		}
		ActiveMatch match = activeByPlayer.get(player.getObjectId());
		if (match != null) {
			match.session.markOnline(player.getObjectId(), true);
			AGPlayer matchPlayer = match.adapter.players.get(player.getObjectId());
			if (matchPlayer != null) {
				matchPlayer.setOnline(true);
			}
			RetailMatchSession.Member member = match.session.member(player.getObjectId());
				if (member != null && !member.entered()) {
					if (match.definition.isTeamMatch()) {
						sendTeamMatchReady(player, match);
					} else {
						PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(match.definition.getInstanceMaskId(), 4));
					}
				}
			persist(match);
		}
		ensureTimer();
	}

	@Override
	public synchronized void onPlayerLogOut(Player player) {
		for (Registration registration : new HashSet<>(registrations.values())) {
			if (registration.members.stream().anyMatch(member -> member.playerId == player.getObjectId())) {
				removeRegistration(registration, true, false);
			}
		}
		ActiveMatch match = activeByPlayer.get(player.getObjectId());
		if (match == null) {
			return;
		}
		match.session.markOnline(player.getObjectId(), false);
		AGPlayer matchPlayer = match.adapter.players.get(player.getObjectId());
		if (matchPlayer != null) {
			matchPlayer.setOnline(false);
		}
		persist(match);
	}

	@Override
	public synchronized void unRegisterInstance(byte instanceMaskId) {
		List<Registration> queue = new ArrayList<>(queues.getOrDefault(Byte.toUnsignedInt(instanceMaskId), List.of()));
		for (Registration registration : queue) {
			removeRegistration(registration, true, false);
		}
	}

	@Override
	public synchronized void unRegisterInstance(Integer instanceId) {
		List<ActiveMatch> matches = activeByInstance.values().stream()
				.filter(match -> match.instance.getInstanceId() == instanceId).toList();
		if (matches.size() > 1) {
			throw new IllegalStateException("Ambiguous retail match runtime instance id " + instanceId);
		}
		if (!matches.isEmpty()) {
			ActiveMatch match = matches.getFirst();
			closeMatch(match, "INSTANCE_CLOSED", true);
		}
	}

	@Override
	public synchronized void unRegisterInstance(WorldMapInstance instance) {
		ActiveMatch match = activeByInstance.get(instanceUid(instance));
		if (match != null) {
			closeMatch(match, "INSTANCE_CLOSED", true);
		}
	}

	@Override
	public synchronized boolean isAutoInstance(int instanceId) {
		return activeByInstance.values().stream().anyMatch(match -> match.instance.getInstanceId() == instanceId);
	}

	@Override
	public synchronized boolean isAutoInstance(Player player) {
		ActiveMatch match = activeByPlayer.get(player.getObjectId());
		return match != null && match.instance == player.getPosition().getWorldMapInstance();
	}

	public synchronized void showWindow(Player player, MatchDefinition definition) {
		if (definition != null && !definition.isTournament() && definition.isOpen()
				&& definition.hasLevelPermit(player.getLevel())
				&& InstanceLimitService.status(player, definition.getInstanceMapId()).allowed()) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(definition.getInstanceMaskId()));
		}
	}

	public synchronized void attachInstance(WorldMapInstance instance) {
		String encoded = instance.getRuntimeState().get(SESSION_KEY);
		if (encoded == null) {
			return;
		}
		RetailMatchSession session = RetailMatchSession.decode(encoded);
		if (instance.getDynamicInstance() == null || session.instanceUid() != instance.getDynamicInstance().getInstanceUid()) {
			throw new IllegalStateException("Retail match session instance uid mismatch");
		}
		MatchDefinition definition = MatchDefinition.getByMaskId(session.matchmakerId());
		if (definition == null || definition.isTournament() || definition.getInstanceMapId() != instance.getMapId()) {
			throw new IllegalStateException("Invalid restored retail match " + session.matchmakerId());
		}
		AutoInstance adapter = definition.getAutoInstance();
		adapter.initsialize(definition.getInstanceMaskId());
		session.resetPendingEntries();
		for (RetailMatchSession.Member member : session.members()) {
			AGPlayer player = new AGPlayer(member.playerId(), race(member.raceId()),
					PlayerClass.getPlayerClassById(member.classId()), member.name(), member.side(), member.entered(),
					member.online(), false);
			adapter.restorePlayer(player);
		}
		adapter.onInstanceCreate(instance);
		ActiveMatch match = new ActiveMatch(definition, instance, adapter, session);
		activeByInstance.put(instanceUid(instance), match);
		for (RetailMatchSession.Member member : session.members()) {
			activeByPlayer.put(member.playerId(), match);
		}
		persist(match);
		scheduleReadyTimeout(match);
		ensureTimer();
	}

	private void runTurn() {
		try {
			long now = System.currentTimeMillis();
			for (MatchDefinition definition : MatchDefinition.all()) {
				if (definition.isTournament() || !definition.isOpen()) {
					continue;
				}
				int created = 0;
				long active = activeByInstance.values().stream().filter(match -> match.definition.getInstanceMaskId()
						== definition.getInstanceMaskId()).count();
				while (active + created < definition.getMaximumInstances()
						&& created < definition.getDraftLimitPerTurn() && createMatch(definition, now)) {
					created++;
				}
				fillExisting(definition, now);
			}
		} catch (RuntimeException e) {
			log.error(I18n.get("log.5c6a86deae91"), e);
		}
	}

	private boolean createMatch(MatchDefinition definition, long now) {
		List<Registration> candidates = validRegistrations(definition, false);
		Plan plan = RetailMatchPlanner.draft(definition, candidates.stream().map(Registration::party).toList(),
				List.of(), now, true);
		if (plan.isEmpty()) {
			return false;
		}
		Map<Integer, Player> players = onlinePlayers(plan);
		if (players.size() != plan.assignments().size()) {
			return false;
		}
		WorldMapInstance instance = null;
		ActiveMatch match = null;
		try {
			AutoInstance adapter = definition.getAutoInstance();
			adapter.initsialize(definition.getInstanceMaskId());
			instance = InstanceService.getNextAvailableInstance(definition.getInstanceMapId(), 0,
					definition.getCreationId(), DynamicInstance.OWNER_MATCH, definition.getInstanceMaskId(),
					definition.getDifficultId());
			for (Assignment assignment : plan.assignments()) {
				Player player = players.get(assignment.member().playerId());
				AGPlayer matchPlayer = new AGPlayer(player);
				matchPlayer.setMatchSide(assignment.side());
				adapter.restorePlayer(matchPlayer);
			}
			adapter.onInstanceCreate(instance);
			long draftDeadline = draftDeadline(definition, now);
			List<RetailMatchSession.Member> members = plan.assignments().stream().map(assignment -> {
				Player player = players.get(assignment.member().playerId());
				return sessionMember(player, assignment.side(), teamId(plan, assignment.member().playerId()),
						instanceGroupEntryId(definition, plan, assignment), now);
			}).toList();
			RetailMatchSession session = new RetailMatchSession(definition.getInstanceMaskId(),
					instance.getDynamicInstance().getInstanceUid(), now, now + READY_TIMEOUT_MILLIS, draftDeadline, members);
			match = new ActiveMatch(definition, instance, adapter, session);
			activeByInstance.put(instanceUid(instance), match);
			for (RetailMatchSession.Member member : members) {
				activeByPlayer.put(member.playerId(), match);
			}
			persist(match);
			List<Registration> matched = removeMatchedParties(plan.parties());
			if (definition.isTeamMatch()) {
				sendTeamMatchUpdates(match, players.keySet());
				GameRuntimeServices.findGroupService().removeMatchedInstanceGroups(matched.stream()
						.map(registration -> registration.instanceGroupEntryId).filter(entryId -> entryId != 0).toList());
			} else {
				for (Player player : players.values()) {
					PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(definition.getInstanceMaskId(), 4));
				}
			}
			scheduleReadyTimeout(match);
			return true;
		} catch (RuntimeException | Error e) {
			if (match != null) {
				activeByInstance.remove(instanceUid(match.instance), match);
				for (RetailMatchSession.Member member : match.session.members()) {
					activeByPlayer.remove(member.playerId(), match);
				}
				match.adapter.clear();
			}
			if (instance != null) {
				InstanceService.destroyInstance(instance);
			}
			throw e;
		}
	}

	private void fillExisting(MatchDefinition definition, long now) {
		List<Registration> candidates = validRegistrations(definition, true);
		if (candidates.isEmpty()) {
			return;
		}
		for (ActiveMatch match : new ArrayList<>(activeByInstance.values())) {
			if (match.definition.getInstanceMaskId() != definition.getInstanceMaskId()
					|| !match.session.acceptsLateEntry(now)) {
				continue;
			}
			List<Assignment> existing = match.session.members().stream()
					.map(member -> new Assignment(new Member(member.playerId(), member.name(),
							PlayerClass.getPlayerClassById(member.classId()), race(member.raceId())), member.side()))
					.toList();
			Plan plan = RetailMatchPlanner.draft(definition, candidates.stream().map(Registration::party).toList(),
					existing, now, false);
			if (plan.isEmpty()) {
				continue;
			}
			Map<Integer, Player> players = onlinePlayers(plan);
			if (players.size() != plan.assignments().size()) {
				continue;
			}
			for (Assignment assignment : plan.assignments()) {
				Player player = players.get(assignment.member().playerId());
				AGPlayer matchPlayer = new AGPlayer(player);
				matchPlayer.setMatchSide(assignment.side());
				match.adapter.restorePlayer(matchPlayer);
				match.session.add(sessionMember(player, assignment.side(),
						teamId(plan, assignment.member().playerId()),
						instanceGroupEntryId(definition, plan, assignment), now), now);
				activeByPlayer.put(player.getObjectId(), match);
			}
			persist(match);
			List<Registration> matched = removeMatchedParties(plan.parties());
			if (definition.isTeamMatch()) {
				sendTeamMatchUpdates(match, players.keySet());
				GameRuntimeServices.findGroupService().removeMatchedInstanceGroups(matched.stream()
						.map(registration -> registration.instanceGroupEntryId).filter(entryId -> entryId != 0).toList());
			} else {
				for (Player player : players.values()) {
					PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(definition.getInstanceMaskId(), 4));
				}
			}
			scheduleReadyTimeout(match);
			candidates = validRegistrations(definition, true);
			if (candidates.isEmpty()) {
				return;
			}
		}
	}

	private List<Registration> validRegistrations(MatchDefinition definition, boolean fastOnly) {
		List<Registration> result = new ArrayList<>();
		for (Registration registration : new ArrayList<>(queues.getOrDefault(definition.getInstanceMaskId(), List.of()))) {
			if (fastOnly && !registration.requestType.isFastGroupEntry()) {
				continue;
			}
			List<Player> players = registration.members.stream().map(member -> player(member.playerId)).toList();
			if (players.stream().anyMatch(java.util.Objects::isNull)
					|| players.stream().anyMatch(player -> !definition.hasLevelPermit(player.getLevel())
							|| !InstanceLimitService.status(player, definition.getInstanceMapId()).allowed())) {
				continue;
			}
			result.add(registration);
		}
		return result;
	}

	private Map<Integer, Player> onlinePlayers(Plan plan) {
		Map<Integer, Player> result = new HashMap<>();
		for (Assignment assignment : plan.assignments()) {
			Player player = player(assignment.member().playerId());
			if (player != null) {
				result.put(player.getObjectId(), player);
			}
		}
		return result;
	}

	private void scheduleReadyTimeout(ActiveMatch match) {
		if (match.readyTask != null) {
			match.readyTask.cancel(false);
		}
		long now = System.currentTimeMillis();
		long deadline = match.session.members().stream().filter(member -> !member.entered())
				.mapToLong(member -> member.registeredAt() + READY_TIMEOUT_MILLIS).min().orElse(0);
		if (deadline == 0) {
			match.readyTask = null;
			return;
		}
		match.readyTask = GameThreadPoolServices.threadPoolManager().schedule(() -> readyTimeout(match),
				Math.max(0, deadline - now));
	}

	private synchronized void readyTimeout(ActiveMatch match) {
		if (activeByInstance.get(instanceUid(match.instance)) != match) {
			return;
		}
		long now = System.currentTimeMillis();
		for (RetailMatchSession.Member member : match.session.members()) {
			if (member.entered() || member.registeredAt() + READY_TIMEOUT_MILLIS > now) {
				continue;
			}
			Player player = player(member.playerId());
			if (player != null) {
				match.adapter.unregister(player);
				InstanceAdmissionService.cancelMatchReservation(match.instance, player);
				sendCancelled(player, match.definition);
				closeTeamMatchWindow(player, match, false);
			} else {
				match.adapter.players.remove(member.playerId());
				InstanceAdmissionService.cancelMatchReservation(match.instance, member.playerId());
			}
			match.session.remove(member.playerId(), "READY_TIMEOUT");
			activeByPlayer.remove(member.playerId(), match);
			penalties.put(member.playerId(), now + PENALTY_MILLIS);
		}
		persist(match);
		if (!finishIfEmpty(match, "READY_TIMEOUT")) {
			scheduleReadyTimeout(match);
			runTurn();
		}
	}

	private boolean finishIfEmpty(ActiveMatch match, String reason) {
		if (!match.session.members().isEmpty()) {
			return false;
		}
		closeMatch(match, reason, true);
		return true;
	}

	private void closeMatch(ActiveMatch match, String reason, boolean destroy) {
		if (match.readyTask != null) {
			match.readyTask.cancel(false);
		}
		if ("INSTANCE_CLOSED".equals(reason)) {
			match.session.finish();
		} else {
			match.session.cancel(reason);
		}
		persist(match);
		activeByInstance.remove(instanceUid(match.instance), match);
		for (RetailMatchSession.Member member : match.session.members()) {
			Player player = player(member.playerId());
			if (!member.entered()) {
				if (player == null) {
					InstanceAdmissionService.cancelMatchReservation(match.instance, member.playerId());
				} else {
					InstanceAdmissionService.cancelMatchReservation(match.instance, player);
					closeTeamMatchWindow(player, match, false);
				}
			}
			activeByPlayer.remove(member.playerId(), match);
		}
		match.adapter.clear();
		if (destroy) {
			InstanceService.destroyInstance(match.instance);
		}
	}

	private void persist(ActiveMatch match) {
		match.instance.getRuntimeState().put(SESSION_KEY, match.session.encode());
	}

	static long instanceUid(WorldMapInstance instance) {
		if (instance.getDynamicInstance() == null || instance.getDynamicInstance().getInstanceUid() <= 0) {
			throw new IllegalStateException("Retail match requires a persisted dynamic instance");
		}
		return instance.getDynamicInstance().getInstanceUid();
	}

	private List<Registration> removeMatchedParties(List<Party> parties) {
		Set<Registration> selected = new HashSet<>();
		for (Party party : parties) {
			Registration registration = findRegistration(party.sequence());
			if (registration != null) {
				selected.add(registration);
			}
		}
		Set<Integer> matchedPlayers = new HashSet<>();
		for (Registration registration : selected) {
			registration.members.forEach(member -> matchedPlayers.add(member.playerId));
			removeRegistration(registration, false, false);
		}
		for (Integer playerId : matchedPlayers) {
			for (Registration registration : new HashSet<>(registrations.values())) {
				if (registration.members.stream().anyMatch(member -> member.playerId == playerId)) {
					removeRegistration(registration, true, false);
				}
			}
		}
		return List.copyOf(selected);
	}

	private Registration findRegistration(long registrationSequence) {
		for (List<Registration> queue : queues.values()) {
			for (Registration registration : queue) {
				if (registration.sequence == registrationSequence) {
					return registration;
				}
			}
		}
		return null;
	}

	private void removeRegistration(Registration registration, boolean notify, boolean penalty) {
		List<Registration> queue = queues.get(registration.matchmakerId);
		if (queue != null) {
			queue.remove(registration);
			if (queue.isEmpty()) {
				queues.remove(registration.matchmakerId);
			}
		}
		MatchDefinition definition = MatchDefinition.getByMaskId(registration.matchmakerId);
		long penaltyUntil = System.currentTimeMillis() + PENALTY_MILLIS;
		for (QueuedMember member : registration.members) {
			registrations.remove(new PlayerMatch(member.playerId, registration.matchmakerId), registration);
			if (penalty) {
				penalties.put(member.playerId, penaltyUntil);
			}
			Player player = player(member.playerId);
			if (notify && player != null && definition != null) {
				sendCancelled(player, definition);
			}
		}
	}

	private boolean canQueue(Player player, EntryRequestType requestType, MatchDefinition definition) {
		if (!definition.isOpen()) {
			return false;
		}
		if (!definition.hasLevelPermit(player.getLevel())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
			return false;
		}
		if (!InstanceLimitService.status(player, definition.getInstanceMapId()).allowed()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME);
			return false;
		}
		if (requestType.isGroupEntry()) {
			if (!definition.hasRegisterGroup()) {
				return false;
			}
			TemporaryPlayerTeam<?> team = player.getCurrentTeam();
			if (team == null || !team.isLeader(player)) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_NOT_LEADER);
				return false;
			}
			List<Player> members = new ArrayList<>(team.getOnlineMembers());
			if (members.size() > definition.getPlayersPerSide()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_TOO_MANY_MEMBERS(
						definition.getPlayersPerSide(), Integer.toString(definition.getInstanceMapId())));
				return false;
			}
			for (Player member : members) {
				if (!definition.hasLevelPermit(member.getLevel())
						|| !InstanceLimitService.status(member, definition.getInstanceMapId()).allowed()) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
			}
			return true;
		}
		return requestType.isFastGroupEntry() ? definition.hasRegisterFast()
				: requestType.isSpecialPurpose() ? definition.hasSpecialPurpose() : definition.hasRegisterNew();
	}

	private static List<Player> members(Player player, EntryRequestType requestType) {
		return requestType.isGroupEntry() && player.getCurrentTeam() != null
				? new ArrayList<>(player.getCurrentTeam().getOnlineMembers()) : List.of(player);
	}

	private static int teamId(Player player, EntryRequestType requestType) {
		return requestType.isGroupEntry() && player.getCurrentTeam() != null ? player.getCurrentTeam().getTeamId() : 0;
	}

	private static int teamId(Plan plan, int playerId) {
		return plan.parties().stream().filter(party -> party.members().stream()
				.anyMatch(member -> member.playerId() == playerId)).mapToInt(Party::teamId).findFirst().orElse(0);
	}

	private static RetailMatchSession.Member sessionMember(Player player, byte side, int teamId,
			int instanceGroupEntryId, long invitedAt) {
		return new RetailMatchSession.Member(player.getObjectId(), player.getName(), player.getPlayerClass().getClassId(),
				player.getLevel(), (byte) player.getRace().getRaceId(), side, teamId, instanceGroupEntryId, invitedAt,
				false, false, true);
	}

	private int instanceGroupEntryId(MatchDefinition definition, Plan plan, Assignment assignment) {
		if (!definition.isTeamMatch()) {
			return 0;
		}
		Registration direct = registration(plan, assignment.member().playerId());
		if (direct != null && direct.instanceGroupEntryId != 0) {
			return direct.instanceGroupEntryId;
		}
		for (Assignment candidate : plan.assignments()) {
			if (candidate.side() == assignment.side()) {
				Registration registration = registration(plan, candidate.member().playerId());
				if (registration != null && registration.instanceGroupEntryId != 0) {
					return registration.instanceGroupEntryId;
				}
			}
		}
		return assignment.member().playerId();
	}

	private Registration registration(Plan plan, int playerId) {
		return plan.parties().stream().filter(party -> party.members().stream()
				.anyMatch(member -> member.playerId() == playerId)).map(Party::sequence).map(this::findRegistration)
				.filter(java.util.Objects::nonNull).findFirst().orElse(null);
	}

	private static void sendTeamMatchUpdates(ActiveMatch match) {
		sendTeamMatchUpdates(match, Set.of());
	}

	private static void sendTeamMatchUpdates(ActiveMatch match, Set<Integer> newPlayerIds) {
		for (RetailMatchSession.Member member : match.session.members()) {
			Player player = player(member.playerId());
			if (player == null || member.instanceGroupEntryId() == 0) {
				continue;
			}
			if (newPlayerIds.contains(member.playerId())) {
				sendTeamMatchReady(player, match);
				continue;
			}
			List<RetailMatchSession.Member> members = match.session.members().stream()
					.filter(entry -> entry.instanceGroupEntryId() == member.instanceGroupEntryId()).toList();
			PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(0x18, member.instanceGroupEntryId(),
					match.definition.getInstanceMaskId(), members));
		}
	}

	private static void sendTeamMatchReady(Player player, ActiveMatch match) {
		RetailMatchSession.Member member = match.session.member(player.getObjectId());
		if (member == null || member.instanceGroupEntryId() == 0) {
			return;
		}
		List<RetailMatchSession.Member> members = match.session.members().stream()
				.filter(entry -> entry.instanceGroupEntryId() == member.instanceGroupEntryId()).toList();
		int instanceMaskId = match.definition.getInstanceMaskId();
		PacketSendUtility.sendPacket(player,
				new SM_FIND_GROUP(0x16, member.instanceGroupEntryId(), instanceMaskId, false));
		PacketSendUtility.sendPacket(player,
				new SM_FIND_GROUP(0x18, member.instanceGroupEntryId(), instanceMaskId, members));
		PacketSendUtility.sendPacket(player,
				new SM_FIND_GROUP(0x12, member.instanceGroupEntryId(), instanceMaskId, false));
	}

	private static void closeTeamMatchWindow(Player player, ActiveMatch match, boolean showEnterMessage) {
		if (!match.definition.isTeamMatch()) {
			return;
		}
		RetailMatchSession.Member member = match.session.member(player.getObjectId());
		if (member != null && member.instanceGroupEntryId() != 0) {
			PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(0x17, member.instanceGroupEntryId(),
					match.definition.getInstanceMaskId(), showEnterMessage));
		}
	}

	private static Race race(byte raceId) {
		return raceId == Race.ELYOS.getRaceId() ? Race.ELYOS : Race.ASMODIANS;
	}

	private static long draftDeadline(MatchDefinition definition, long createdAt) {
		long tolerance = definition.getAgeToleranceMillis();
		long draft = definition.getTime();
		if (tolerance <= 0) {
			return draft <= 0 ? 0 : createdAt + draft;
		}
		if (draft <= 0) {
			return createdAt + tolerance;
		}
		return createdAt + Math.min(tolerance, draft);
	}

	private static int waitTime(Registration registration) {
		return (int) ((System.currentTimeMillis() - registration.registeredAt) / 1000) * 256
				+ registration.requestType.getId();
	}

	private static void sendQueued(Player player, MatchDefinition definition, EntryRequestType requestType, String name) {
		if (definition.hasHudRegister()) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(definition.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
		}
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400194, definition.getInstanceMapId()));
		PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(definition.getInstanceMaskId(), 1,
				requestType.getId(), name));
	}

	private static void sendCancelled(Player player, MatchDefinition definition) {
		if (definition.hasHudRegister() && definition.isOpen()) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(definition.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon));
		}
		PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(definition.getInstanceMaskId(), 2));
	}

	private static Player player(int playerId) {
		return GameWorldBootstrapServices.world().findPlayer(playerId);
	}

	private void ensureTimer() {
		if (matchingTask == null || matchingTask.isDone()) {
			// ponytail: 单服匹配吞吐足够；实测出现锁竞争时再按 matchmaker 分片。
			matchingTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
					() -> {
						synchronized (RetailMatchmakingService.this) {
							runTurn();
						}
					}, MATCH_TURN_MILLIS, MATCH_TURN_MILLIS);
		}
	}

	private void refreshHud(boolean notifyTransitions) {
		for (MatchDefinition definition : MatchDefinition.all()) {
			if (definition.isTournament() || !definition.hasHudRegister()) {
				continue;
			}
			boolean open = definition.isOpen();
			Boolean previous = hudOpen.put(definition.getInstanceMaskId(), open);
			if (!notifyTransitions || previous == null || previous == open) {
				continue;
			}
			Iterator<Player> players = GameWorldBootstrapServices.world().getPlayersIterator();
			while (players.hasNext()) {
				Player player = players.next();
				boolean close = !open || !definition.hasLevelPermit(player.getLevel())
						|| !InstanceLimitService.status(player, definition.getInstanceMapId()).allowed();
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(definition.getInstanceMaskId(),
						SM_AUTO_GROUP.wnd_EntryIcon, close));
			}
		}
	}

	private record PlayerMatch(int playerId, int matchmakerId) {
	}

	private static final class QueuedMember {
		private final int playerId;
		private final String name;
		private final PlayerClass playerClass;
		private final Race race;

		private QueuedMember(Player player) {
			playerId = player.getObjectId();
			name = player.getName();
			playerClass = player.getPlayerClass();
			race = player.getRace();
		}
	}

	private static final class Registration {
		private final long sequence;
		private final long registeredAt;
		private final int matchmakerId;
		private final EntryRequestType requestType;
		private final int teamId;
		private final int instanceGroupEntryId;
		private final List<QueuedMember> members;

		private Registration(long sequence, long registeredAt, int matchmakerId, EntryRequestType requestType,
				int teamId, int instanceGroupEntryId, List<QueuedMember> members) {
			this.sequence = sequence;
			this.registeredAt = registeredAt;
			this.matchmakerId = matchmakerId;
			this.requestType = requestType;
			this.teamId = teamId;
			this.instanceGroupEntryId = instanceGroupEntryId;
			this.members = List.copyOf(members);
		}

		private Party party() {
			return new Party(sequence, registeredAt, requestType, teamId, members.stream()
					.map(member -> new Member(member.playerId, member.name, member.playerClass, member.race)).toList());
		}
	}

	private static final class ActiveMatch {
		private final MatchDefinition definition;
		private final WorldMapInstance instance;
		private final AutoInstance adapter;
		private final RetailMatchSession session;
		private Future<?> readyTask;

		private ActiveMatch(MatchDefinition definition, WorldMapInstance instance, AutoInstance adapter,
				RetailMatchSession session) {
			this.definition = definition;
			this.instance = instance;
			this.adapter = adapter;
			this.session = session;
		}
	}
}
