package com.aionemu.gameserver.ai;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortal;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalEndpoint;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalGroup;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalPoint;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIRECT_PORTAL_USE_COUNT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Slf4j
public final class RetailDirectPortalEngine {

	static final int USE_COUNT_LIMIT = -1;
	static final int NOT_ENOUGH_AP = -2;
	private static final Map<Integer, ActivePortal> ACTIVE = new HashMap<>();
	private static final Map<Integer, ActivePortal> BY_NPC = new HashMap<>();
	private static long lastScheduleCheck;

	private RetailDirectPortalEngine() {
	}

	public static void startScheduler() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(RetailDirectPortalEngine::checkScheduledPortals,
			40_000, 40_000);
	}

	private static void checkScheduledPortals() {
		checkScheduledPortals(LocalDateTime.now(), System.currentTimeMillis() / 1000);
	}

	static synchronized void checkScheduledPortals(LocalDateTime now, long epochSecond) {
		if (now.getMinute() != 0 || epochSecond < lastScheduleCheck + 3600 || DataManager.RETAIL_AI_DATA == null) {
			return;
		}
		lastScheduleCheck = epochSecond;
		int index = scheduleIndex(now.getDayOfWeek(), now.getHour());
		for (DirectPortal definition : DataManager.RETAIL_AI_DATA.directPortals()) {
			if (!definition.schedule().isEmpty() && shouldOpen(definition.schedule().get(index), Rnd.get(100))) {
				open(definition.id());
			}
		}
	}

	static int scheduleIndex(DayOfWeek day, int hour) {
		return (day.getValue() - 1) * 24 + hour;
	}

	static boolean shouldOpen(int probability, int roll) {
		return roll < probability;
	}

	public static boolean open(int id) {
		return open(id, null);
	}

	public static synchronized boolean open(int id, Npc owner) {
		ActivePortal existing = ACTIVE.get(id);
		if (existing != null && !existing.closed) {
			return true;
		}
		DirectPortal definition = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getDirectPortal(id);
		if (definition == null) {
			return false;
		}
		DirectPortalPoint startPoint = selectPoint(definition.start());
		DirectPortalPoint destinationPoint = selectPoint(definition.destination());
		Npc start = spawn(definition.start(), startPoint, owner);
		Npc destination = spawn(definition.destination(), destinationPoint, owner);
		if (start == null || destination == null) {
			delete(start);
			delete(destination);
			log.warn(I18n.get("log.retail_portal.open_failed", id));
			return false;
		}
		ActivePortal active = new ActivePortal(definition, start, destination, destinationPoint);
		ACTIVE.put(id, active);
		BY_NPC.put(start.getObjectId(), active);
		BY_NPC.put(destination.getObjectId(), active);
		if (definition.invadeType() == 6) {
			setInvadeAreas(active, true);
		}
		active.expiry = GameThreadPoolServices.threadPoolManager().schedule(
			() -> closeIfActive(id, active), definition.time() * 1000L);
		broadcastUseCount(active);
		return true;
	}

	public static synchronized boolean openByUser(int id, Npc owner, Player player) {
		DirectPortal definition = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getDirectPortal(id);
		if (definition == null || definition.needItem().isBlank() || definition.groupId() <= 0
				|| definition.invadeType() != 5) {
			return false;
		}
		if (ACTIVE.values().stream().anyMatch(active -> !active.closed
				&& active.definition.groupId() == definition.groupId())) {
			return false;
		}
		var item = DataManager.ITEM_DATA.getItemTemplate(definition.needItem());
		if (item == null || player.getInventory().getItemCountByItemId(item.getTemplateId()) == 0 || !open(id, owner)) {
			return false;
		}
		if (!player.getInventory().decreaseByItemId(item.getTemplateId(), 1)) {
			close(id);
			return false;
		}
		return true;
	}

	public static synchronized void close(int id) {
		ActivePortal active = ACTIVE.remove(id);
		if (active != null) {
			close(active);
		}
	}

	public static int extraCostFor(Npc npc) {
		ActivePortal active;
		synchronized (RetailDirectPortalEngine.class) {
			active = BY_NPC.get(npc.getObjectId());
		}
		if (active == null || npc != active.start) {
			return 0;
		}
		synchronized (active) {
			return active.closed ? 0 : active.uses.extraCost(active.definition.extraCostAp());
		}
	}

	public static void sendUseCount(Npc npc, Player player) {
		ActivePortal active;
		synchronized (RetailDirectPortalEngine.class) {
			active = BY_NPC.get(npc.getObjectId());
		}
		if (active == null) {
			return;
		}
		synchronized (active) {
			if (!active.closed) {
				PacketSendUtility.sendPacket(player, useCountPacket(active, npc == active.start));
			}
		}
	}

	public static void use(Npc npc, Player player) {
		use(npc, player, false);
	}

	public static void use(Npc npc, Player player, boolean allowExtraUse) {
		ActivePortal active;
		synchronized (RetailDirectPortalEngine.class) {
			active = BY_NPC.get(npc.getObjectId());
		}
		if (active == null || npc != active.start) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_DIRECT_PORTAL_NO_PORTAL);
			return;
		}
		if (player.getFlyState() != 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_DIRECT_PORTAL_WHILE_FLYING);
			return;
		}
		DirectPortal definition = active.definition;
		if (player.getLevel() < definition.minLevel() || player.getLevel() > definition.maxLevel()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_DIRECT_PORTAL_LEVEL_LIMIT);
			return;
		}
		if (definition.titleId() > 0 && !player.getTitleList().contains(definition.titleId())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_DIRECT_PORTAL_NOT_TITLE);
			return;
		}
		int apCost;
		boolean exhausted;
		synchronized (active) {
			if (active.closed) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_DIRECT_PORTAL_NO_PORTAL);
				return;
			}
			recountDepartedPlayers(active);
			apCost = active.uses.reserve(allowExtraUse, player.getAbyssRank().getAp(), definition.extraCostAp());
			if (apCost == USE_COUNT_LIMIT) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_DIRECT_PORTAL_USE_COUNT_LIMIT);
				return;
			}
			if (apCost == NOT_ENOUGH_AP) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT);
				return;
			}
			exhausted = active.uses.exhausted();
		}
		if (apCost > 0) {
			synchronized (player.getAbyssRank()) {
				if (player.getAbyssRank().getAp() < apCost) {
					synchronized (active) {
						active.uses.release(apCost);
					}
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT);
					return;
				}
				AbyssPointsService.addAp(player, -apCost);
			}
		}
		Origin origin = new Origin(player.getWorldId(), player.getInstanceId(), player.getX(), player.getY(),
			player.getZ(), player.getHeading());
		DirectPortalPoint point = active.destinationPoint;
		boolean teleported = TeleportService2.teleportTo(player, definition.destination().worldId(), point.x(), point.y(),
			point.z(), MathUtil.convertDegreeToHeading(point.direction()), animationFor(definition.invadeType()));
		if (!teleported) {
			if (apCost > 0) {
				AbyssPointsService.addAp(player, apCost);
			}
			synchronized (active) {
				if (!active.closed) {
					active.uses.release(apCost);
				}
			}
			return;
		}
		if (definition.closeForceOut()) {
			synchronized (active) {
				active.origins.put(player.getObjectId(), origin);
			}
		}
		SM_SYSTEM_MESSAGE notice = noticeFor(definition.invadeType());
		if (notice != null) {
			PacketSendUtility.sendPacket(player, notice);
		}
		if (exhausted && closesWhenExhausted(definition.invadeType(), definition.recount())) {
			closeIfActive(definition.id(), active);
		} else {
			broadcastUseCount(active);
		}
	}

	private static void broadcastUseCount(ActivePortal active) {
		SM_DIRECT_PORTAL_USE_COUNT startPacket;
		SM_DIRECT_PORTAL_USE_COUNT destinationPacket;
		synchronized (active) {
			if (active.closed) {
				return;
			}
			startPacket = useCountPacket(active, true);
			destinationPacket = useCountPacket(active, false);
		}
		PacketSendUtility.broadcastPacket(active.start, startPacket);
		PacketSendUtility.broadcastPacket(active.destination, destinationPacket);
	}

	private static SM_DIRECT_PORTAL_USE_COUNT useCountPacket(ActivePortal active, boolean startSide) {
		return new SM_DIRECT_PORTAL_USE_COUNT(active.start.getObjectId(),
			active.uses.used(active.definition.count()), active.remainingSeconds(), startSide,
			active.definition.invadeType(), active.uses.extraUsed(active.definition.extraCount()));
	}

	static TeleportAnimation animationFor(int invadeType) {
		return switch (invadeType) {
			case 1, 2, 3, 6 -> TeleportAnimation.INVASION_PORTAL;
			default -> TeleportAnimation.DIRECT_PORTAL;
		};
	}

	static boolean closesWhenExhausted(int invadeType, boolean recount) {
		return !recount && invadeType != 6;
	}

	static SM_SYSTEM_MESSAGE noticeFor(int invadeType) {
		return switch (invadeType) {
			case 1 -> SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_OPEN_NOTICE;
			case 2 -> SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_OPEN_NOTICE;
			case 3 -> SM_SYSTEM_MESSAGE.STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_5S;
			case 6 -> SM_SYSTEM_MESSAGE.STR_MSG_RVR_DIRECT_PORTAL_OPEN_NOTICE;
			default -> null;
		};
	}

	static DirectPortalPoint selectPoint(DirectPortalEndpoint endpoint) {
		int totalWeight = endpoint.groups().stream().mapToInt(DirectPortalGroup::weight).sum();
		int roll = totalWeight > 0 ? Rnd.get(totalWeight) : Rnd.get(endpoint.groups().size());
		DirectPortalGroup selected = endpoint.groups().get(endpoint.groups().size() - 1);
		if (totalWeight > 0) {
			for (DirectPortalGroup group : endpoint.groups()) {
				roll -= group.weight();
				if (roll < 0) {
					selected = group;
					break;
				}
			}
		} else {
			selected = endpoint.groups().get(roll);
		}
		return selected.points().get(Rnd.get(selected.points().size()));
	}

	private static Npc spawn(DirectPortalEndpoint endpoint, DirectPortalPoint point, Npc owner) {
		int instanceId = owner != null && owner.getWorldId() == endpoint.worldId() ? owner.getInstanceId() : 1;
		VisibleObject object = SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(endpoint.worldId(), endpoint.npcId(),
			point.x(), point.y(), point.z(), MathUtil.convertDegreeToHeading(point.direction())), instanceId);
		if (!(object instanceof Npc npc)) {
			return null;
		}
		GameEngineServices.ai2Engine().setupAI("retail_direct_portal", npc);
		npc.getAi2().onGeneralEvent(AIEventType.SPAWNED);
		return npc;
	}

	private static synchronized void closeIfActive(int id, ActivePortal active) {
		if (ACTIVE.remove(id, active)) {
			close(active);
		}
	}

	private static void close(ActivePortal active) {
		synchronized (active) {
			active.closed = true;
		}
		forceOutPlayers(active);
		if (active.definition.invadeType() == 6) {
			setInvadeAreas(active, false);
		}
		BY_NPC.remove(active.start.getObjectId());
		BY_NPC.remove(active.destination.getObjectId());
		if (active.expiry != null) {
			active.expiry.cancel(false);
		}
		delete(active.start);
		delete(active.destination);
	}

	private static void recountDepartedPlayers(ActivePortal active) {
		if (!active.definition.recount()) {
			return;
		}
		int destinationWorldId = active.definition.destination().worldId();
		int destinationInstanceId = active.destination.getInstanceId();
		active.origins.entrySet().removeIf(entry -> {
			Player player = GameWorldBootstrapServices.world().findPlayer(entry.getKey());
			if (player != null && player.getWorldId() == destinationWorldId && player.getInstanceId() == destinationInstanceId) {
				return false;
			}
			active.uses.recount();
			return true;
		});
	}

	private static void forceOutPlayers(ActivePortal active) {
		if (!active.definition.closeForceOut()) {
			return;
		}
		for (Player player : active.destination.getPosition().getWorldMapInstance().getPlayersInside()) {
			Origin origin = active.origins.get(player.getObjectId());
			if (origin != null) {
				TeleportService2.teleportTo(player, origin.worldId(), origin.instanceId(), origin.x(), origin.y(), origin.z(),
					origin.heading(), TeleportAnimation.DIRECT_PORTAL);
			}
		}
	}

	private static void setInvadeAreas(ActivePortal active, boolean enabled) {
		setInvadeAreas(active.start, "InvadePortalStart_" + active.definition.id(), enabled);
		setInvadeAreas(active.destination, "InvadePortalDest_" + active.definition.id(), enabled);
	}

	private static void setInvadeAreas(Npc portal, String prefix, boolean enabled) {
		var instance = portal.getPosition().getWorldMapInstance();
		RetailAreaEngine.setEnabled(instance, "AI_CONTROL_AREA_QUESTSCRIPT", prefix + "_QuestArea", enabled);
		RetailAreaEngine.setEnabled(instance, "AI_CONTROL_AREA_RESURRECT", prefix + "_ResurrectArea", enabled);
		for (String suffix : new String[] { "_AtkGroupOnArea", "_AtkGroupOffArea", "_DefGroupOnArea", "_DefGroupOffArea" }) {
			RetailAreaEngine.setEnabled(instance, "AI_CONTROL_AREA_GROUPCTRL", prefix + suffix, enabled);
		}
	}

	private static void delete(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}

	private static final class ActivePortal {
		private final DirectPortal definition;
		private final Npc start;
		private final Npc destination;
		private final DirectPortalPoint destinationPoint;
		private final Map<Integer, Origin> origins = new HashMap<>();
		private final UseCounter uses;
		private final long expiresAtMillis;
		private volatile boolean closed;
		private Future<?> expiry;

		private ActivePortal(DirectPortal definition, Npc start, Npc destination, DirectPortalPoint destinationPoint) {
			this.definition = definition;
			this.start = start;
			this.destination = destination;
			this.destinationPoint = destinationPoint;
			uses = new UseCounter(definition.count(), definition.extraCount());
			expiresAtMillis = System.currentTimeMillis() + definition.time() * 1000L;
		}

		private int remainingSeconds() {
			return (int) Math.max(0, (expiresAtMillis - System.currentTimeMillis()) / 1000);
		}
	}

	static final class UseCounter {
		private int remaining;
		private int extraRemaining;

		UseCounter(int remaining, int extraRemaining) {
			this.remaining = remaining;
			this.extraRemaining = extraRemaining;
		}

		int extraCost(int extraCostAp) {
			return remaining == 0 && extraRemaining > 0 ? extraCostAp : 0;
		}

		int used(int count) {
			return count - remaining;
		}

		int extraUsed(int extraCount) {
			return extraCount - extraRemaining;
		}

		int reserve(boolean allowExtraUse, int availableAp, int extraCostAp) {
			if (remaining > 0) {
				remaining--;
				return 0;
			}
			if (!allowExtraUse || extraRemaining == 0) {
				return USE_COUNT_LIMIT;
			}
			if (availableAp < extraCostAp) {
				return NOT_ENOUGH_AP;
			}
			extraRemaining--;
			return extraCostAp;
		}

		void release(int apCost) {
			if (apCost > 0) {
				extraRemaining++;
			} else {
				remaining++;
			}
		}

		void recount() {
			remaining++;
		}

		boolean exhausted() {
			return remaining == 0 && extraRemaining == 0;
		}
	}

	private record Origin(int worldId, int instanceId, float x, float y, float z, byte heading) {
	}
}
