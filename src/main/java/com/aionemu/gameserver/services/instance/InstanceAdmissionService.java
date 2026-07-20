package com.aionemu.gameserver.services.instance;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.league.League;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.model.templates.portal.ItemReq;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.templates.portal.PortalLoc;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.portal.PortalReq;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

public final class InstanceAdmissionService {
	private InstanceAdmissionService() {
	}

	public static synchronized Admission admit(PortalPath path, PortalLoc location, Player player) {
		int worldId = location.getWorldId();
		List<Player> members = members(path, player);
		WorldMapInstance instance = existingInstance(worldId, player, members);
		boolean reentry = instance != null && DynamicInstanceManager.hasJoined(instance, player.getObjectId());
		if (reentry) {
			return new Admission(instance, player, false, false, false, 0, List.of());
		}

		if (!preflightMembers(members, worldId, instance)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME);
			return null;
		}
		PortalReq requirement = path.getPortalReq();
		if (!hasPayment(player, requirement)) {
			return null;
		}

		boolean created = false;
		boolean reserved = false;
		if (instance == null) {
			Owner owner = owner(path, player);
			boolean personal = owner.type == DynamicInstance.OWNER_PLAYER;
			int creationId = DynamicInstanceManager.selectCreationId(worldId, player, personal);
			instance = InstanceService.getNextAvailableInstance(worldId, personal ? player.getObjectId() : 0,
					creationId, owner.type, owner.id, (byte) 0);
			created = true;
			registerOwner(instance, owner, player);
		} else if (!instance.isRegistered(player.getObjectId())) {
			int maxPlayers = InstanceService.getMaxPlayers(worldId);
			if (maxPlayers > 0 && DynamicInstanceManager.memberCount(instance) >= maxPlayers) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401718));
				return null;
			}
			DynamicInstanceManager.reserveMember(instance, player, teamId(player), (byte) player.getRace().getRaceId());
		}
		reserved = !created && !DynamicInstanceManager.hasJoined(instance, player.getObjectId());

		long kinah = requirement == null ? 0 : requirement.getKinahReq();
		List<ItemReq> paidItems = requirement == null || requirement.getItemReq() == null
				? List.of() : List.copyOf(requirement.getItemReq());
		boolean limitConsumed = false;
		long paidKinah = 0;
		List<ItemReq> chargedItems = new ArrayList<>();
		try {
			if (kinah > 0 && !player.getInventory().tryDecreaseKinah(kinah)) {
				throw new AdmissionFailure();
			}
			paidKinah = kinah;
			for (ItemReq item : paidItems) {
				if (!player.getInventory().decreaseByItemId(item.getItemId(), item.getItemCount())) {
					throw new AdmissionFailure();
				}
				chargedItems.add(item);
			}
			InstanceLimitService.LimitStatus status = InstanceLimitService.status(player, worldId);
			if (!InstanceLimitService.consume(player, worldId)) {
				throw new AdmissionFailure();
			}
			limitConsumed = status.maxEntries() > 0;
			DynamicInstanceManager.reserveMember(instance, player, teamId(player), (byte) player.getRace().getRaceId());
			return new Admission(instance, player, created, reserved, limitConsumed, paidKinah, List.copyOf(chargedItems));
		} catch (RuntimeException e) {
			rollback(instance, player, created, reserved, limitConsumed, paidKinah, chargedItems);
			if (e instanceof AdmissionFailure) {
				return null;
			}
			throw e;
		}
	}

	public static synchronized Admission admitPersonal(Player player, int worldId) {
		WorldMapInstance instance = InstanceService.getRegisteredInstance(worldId, player.getObjectId());
		if (instance != null) {
			if (DynamicInstanceManager.hasJoined(instance, player.getObjectId())) {
				return new Admission(instance, player, false, false, false, 0, List.of());
			}
			return null;
		}
		if (!eligible(player, worldId) || !InstanceLimitService.status(player, worldId).allowed()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME);
			return null;
		}

		boolean limitConsumed = false;
		try {
			int creationId = DynamicInstanceManager.selectCreationId(worldId, player, true);
			instance = InstanceService.getNextAvailableInstance(worldId, player.getObjectId(), creationId,
					DynamicInstance.OWNER_PLAYER, player.getObjectId(), (byte) 0);
			InstanceService.registerPlayerWithInstance(instance, player);
			InstanceLimitService.LimitStatus status = InstanceLimitService.status(player, worldId);
			if (!InstanceLimitService.consume(player, worldId)) {
				throw new AdmissionFailure();
			}
			limitConsumed = status.maxEntries() > 0;
			return new Admission(instance, player, true, false, limitConsumed, 0, List.of());
		} catch (RuntimeException | Error e) {
			if (instance != null) {
				rollback(instance, player, true, false, limitConsumed, 0, List.of());
			}
			if (e instanceof AdmissionFailure) {
				return null;
			}
			throw e;
		}
	}

	public static synchronized Admission admitMatch(WorldMapInstance instance, Player player, byte side) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		if (dynamic == null || dynamic.getOwnerType() != DynamicInstance.OWNER_MATCH) {
			throw new IllegalStateException("Match entry requires a retail match instance");
		}
		if (DynamicInstanceManager.hasJoined(instance, player.getObjectId())) {
			return new Admission(instance, player, false, false, false, 0, List.of());
		}
		if (instance.isRegistered(player.getObjectId())) {
			return new Admission(instance, player, false, true, false, 0, List.of());
		}
		if (!eligible(player, instance.getMapId()) || !InstanceLimitService.status(player, instance.getMapId()).allowed()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME);
			return null;
		}
		MatchDefinition match = MatchDefinition.getByMaskId(dynamic.getOwnerId());
		if (match == null || match.getInstanceMapId() != instance.getMapId()) {
			throw new IllegalStateException("Invalid retail match instance " + dynamic.getOwnerId());
		}
		int capacity = match.getPlayerSize();
		if (!instance.isRegistered(player.getObjectId()) && capacity > 0
				&& DynamicInstanceManager.memberCount(instance) >= capacity) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401718));
			return null;
		}
		boolean reserved = false;
		try {
			if (!InstanceLimitService.reserveMatch(instance, player, teamId(player), side)) {
				throw new AdmissionFailure();
			}
			reserved = true;
			return new Admission(instance, player, false, true, false, 0, List.of());
		} catch (RuntimeException e) {
			if (reserved) {
				cancelMatchReservation(instance, player);
			}
			if (e instanceof AdmissionFailure) {
				return null;
			}
			throw e;
		}
	}

	public static synchronized void cancelMatchReservation(WorldMapInstance instance, Player player) {
		int restoredLimitKey = DynamicInstanceManager.cancelMatchReservation(instance, player.getObjectId());
		if (restoredLimitKey != 0) {
			InstanceLimitService.restoreEntry(player, restoredLimitKey);
		}
	}

	public static synchronized void cancelMatchReservation(WorldMapInstance instance, int playerId) {
		DynamicInstanceManager.cancelMatchReservation(instance, playerId);
	}

	public static synchronized LunaAdmission admitLuna(Row dungeon, Player player, long lunaCost) {
		int worldId = dungeon.requiredInt("world_id");
		if (!eligible(player, worldId) || lunaCost < 0 || player.getLunaAccount() < lunaCost) {
			return null;
		}
		WorldMapInstance instance = null;
		long balance = player.getLunaAccount();
		try {
			instance = InstanceService.getNextAvailableInstance(worldId, player.getObjectId(),
					dungeon.requiredInt("creation_id"), DynamicInstance.OWNER_PLAYER, player.getObjectId(), (byte) 0);
			InstanceService.registerPlayerWithInstance(instance, player);
			if (lunaCost > 0) {
				player.setLunaAccount(balance - lunaCost);
				if (player.getLunaAccount() != balance - lunaCost) {
					throw new AdmissionFailure();
				}
			}
			return new LunaAdmission(instance, player, balance, lunaCost);
		} catch (RuntimeException | Error e) {
			if (instance != null) {
				InstanceService.destroyInstance(instance);
			}
			if (e instanceof AdmissionFailure) {
				return null;
			}
			throw e;
		}
	}

	public static boolean chargeNonInstancePortal(PortalPath path, Player player) {
		PortalReq requirement = path.getPortalReq();
		if (!hasPayment(player, requirement)) {
			return false;
		}
		long kinah = requirement == null ? 0 : requirement.getKinahReq();
		List<ItemReq> items = requirement == null || requirement.getItemReq() == null
				? List.of() : List.copyOf(requirement.getItemReq());
		if (kinah > 0 && !player.getInventory().tryDecreaseKinah(kinah)) {
			return false;
		}
		List<ItemReq> paid = new ArrayList<>();
		for (ItemReq item : items) {
			if (!player.getInventory().decreaseByItemId(item.getItemId(), item.getItemCount())) {
				player.getInventory().increaseKinah(kinah);
				for (ItemReq previous : paid) {
					ItemService.addItem(player, previous.getItemId(), previous.getItemCount());
				}
				return false;
			}
			paid.add(item);
		}
		return true;
	}

	private static boolean preflightMembers(List<Player> members, int worldId, WorldMapInstance instance) {
		int maxPlayers = InstanceService.getMaxPlayers(worldId);
		if (maxPlayers > 0 && (instance == null ? members.size() : DynamicInstanceManager.memberCount(instance)) > maxPlayers) {
			return false;
		}
		for (Player member : members) {
			if (instance != null && DynamicInstanceManager.hasJoined(instance, member.getObjectId())) {
				continue;
			}
			if (!eligible(member, worldId) || !InstanceLimitService.status(member, worldId).allowed()) {
				return false;
			}
		}
		return true;
	}

	private static boolean eligible(Player player, int worldId) {
		var rule = InstanceLimitService.rule(worldId);
		if (rule == null) {
			return true;
		}
		String side = player.getRace() == Race.ELYOS ? "light" : "dark";
		int min = rule.intValue("enter_min_level_" + side, 0);
		int max = rule.intValue("enter_max_level_" + side, 0);
		return player.getLevel() >= min && (max <= 0 || player.getLevel() <= max)
				&& (!player.isMentor() || rule.booleanValue("can_enter_mentor"));
	}

	private static boolean hasPayment(Player player, PortalReq requirement) {
		if (requirement == null) {
			return true;
		}
		if (player.getInventory().getKinah() < requirement.getKinahReq()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_KINA(requirement.getKinahReq()));
			return false;
		}
		if (requirement.getItemReq() != null) {
			for (ItemReq item : requirement.getItemReq()) {
				if (player.getInventory().getItemCountByItemId(item.getItemId()) < item.getItemCount()) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM_TRY_LATER);
					return false;
				}
			}
		}
		return true;
	}

	private static List<Player> members(PortalPath path, Player player) {
		int size = path.getPlayerCount();
		if (size > 24 && player.isInLeague()) {
			List<Player> result = new ArrayList<>();
			for (PlayerAlliance alliance : player.getPlayerAlliance2().getLeague().getMembers()) {
				result.addAll(alliance.getMembers());
			}
			return result;
		}
		if (size > 6 && player.isInAlliance2()) {
			return new ArrayList<>(player.getPlayerAlliance2().getMembers());
		}
		if (size > 0 && player.isInGroup2()) {
			return new ArrayList<>(player.getPlayerGroup2().getMembers());
		}
		return List.of(player);
	}

	private static WorldMapInstance existingInstance(int worldId, Player player, List<Player> members) {
		WorldMapInstance instance = InstanceService.getRegisteredInstance(worldId, player.getObjectId());
		if (instance != null) {
			return instance;
		}
		int teamId = teamId(player);
		if (teamId != 0) {
			instance = InstanceService.getRegisteredInstance(worldId, teamId);
		}
		if (instance != null) {
			return instance;
		}
		for (Player member : members) {
			instance = InstanceService.getRegisteredInstance(worldId, member.getObjectId());
			if (instance != null) {
				return instance;
			}
		}
		return null;
	}

	private static Owner owner(PortalPath path, Player player) {
		int size = path.getPlayerCount();
		if (size > 24 && player.isInLeague()) {
			return new Owner(DynamicInstance.OWNER_LEAGUE, player.getPlayerAlliance2().getLeague().getObjectId(),
					null, null, player.getPlayerAlliance2().getLeague());
		}
		if (size > 6 && player.isInAlliance2()) {
			return new Owner(DynamicInstance.OWNER_ALLIANCE, player.getPlayerAlliance2().getObjectId(), null,
					player.getPlayerAlliance2(), null);
		}
		if (size > 0 && player.isInGroup2()) {
			return new Owner(DynamicInstance.OWNER_GROUP, player.getPlayerGroup2().getTeamId(), player.getPlayerGroup2(),
					null, null);
		}
		return new Owner(DynamicInstance.OWNER_PLAYER, player.getObjectId(), null, null, null);
	}

	private static void registerOwner(WorldMapInstance instance, Owner owner, Player player) {
		if (owner.group != null) {
			instance.registerGroup(owner.group);
		} else if (owner.alliance != null) {
			instance.registerGroup(owner.alliance);
		} else if (owner.league != null) {
			instance.registerGroup(owner.league);
		} else {
			InstanceService.registerPlayerWithInstance(instance, player);
			return;
		}
		DynamicInstanceManager.reserveMember(instance, player, teamId(player), (byte) player.getRace().getRaceId());
	}

	private static int teamId(Player player) {
		if (player.isInLeague()) {
			return player.getPlayerAlliance2().getLeague().getObjectId();
		}
		if (player.isInAlliance2()) {
			return player.getPlayerAlliance2().getObjectId();
		}
		return player.isInGroup2() ? player.getPlayerGroup2().getTeamId() : 0;
	}

	private static void rollback(WorldMapInstance instance, Player player, boolean created, boolean reserved,
			boolean limitConsumed, long kinah, List<ItemReq> items) {
		if (limitConsumed) {
			InstanceLimitService.restoreEntry(player, InstanceLimitService.limitKey(instance.getMapId()));
		}
		player.getInventory().increaseKinah(kinah);
		for (ItemReq item : items) {
			ItemService.addItem(player, item.getItemId(), item.getItemCount());
		}
		if (created) {
			InstanceService.destroyInstance(instance);
		} else if (reserved) {
			DynamicInstanceManager.removeReservedMember(instance, player.getObjectId());
		}
	}

	public record Admission(WorldMapInstance instance, Player player, boolean created, boolean reserved,
			boolean limitConsumed, long kinah, List<ItemReq> items) {
		public boolean reentry() {
			return !limitConsumed && DynamicInstanceManager.hasJoined(instance, player.getObjectId());
		}

		public void rollback() {
			DynamicInstance dynamic = instance.getDynamicInstance();
			if (reserved && !created && dynamic != null && dynamic.getOwnerType() == DynamicInstance.OWNER_MATCH) {
				cancelMatchReservation(instance, player);
				return;
			}
			InstanceAdmissionService.rollback(instance, player, created, reserved, limitConsumed, kinah, items);
		}
	}

	public record LunaAdmission(WorldMapInstance instance, Player player, long previousBalance, long lunaCost) {
		public void rollback() {
			if (lunaCost > 0 && player.getLunaAccount() != previousBalance) {
				player.setLunaAccount(previousBalance);
				if (player.getLunaAccount() != previousBalance) {
					throw new IllegalStateException("Failed to restore Luna for player " + player.getObjectId());
				}
			}
			InstanceService.destroyInstance(instance);
		}
	}

	private record Owner(byte type, int id, PlayerGroup group, PlayerAlliance alliance, League league) {
	}

	private static final class AdmissionFailure extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
