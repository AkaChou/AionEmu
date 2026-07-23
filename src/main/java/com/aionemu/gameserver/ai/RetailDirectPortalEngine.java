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
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Slf4j
public final class RetailDirectPortalEngine {

	private static final Map<Integer, ActivePortal> ACTIVE = new HashMap<>();
	private static final Map<Integer, ActivePortal> BY_NPC = new HashMap<>();

	private RetailDirectPortalEngine() {
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

	public static void use(Npc npc, Player player) {
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
		boolean exhausted;
		synchronized (active) {
			if (active.closed) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_DIRECT_PORTAL_NO_PORTAL);
				return;
			}
			if (active.remaining == 0) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_DIRECT_PORTAL_USE_COUNT_LIMIT);
				return;
			}
			active.remaining--;
			exhausted = active.remaining == 0;
		}
		DirectPortalPoint point = active.destinationPoint;
		boolean teleported = TeleportService2.teleportTo(player, definition.destination().worldId(), point.x(), point.y(),
			point.z(), MathUtil.convertDegreeToHeading(point.direction()), animationFor(definition.invadeType()));
		if (!teleported) {
			synchronized (active) {
				if (!active.closed) {
					active.remaining++;
				}
			}
			return;
		}
		SM_SYSTEM_MESSAGE notice = noticeFor(definition.invadeType());
		if (notice != null) {
			PacketSendUtility.sendPacket(player, notice);
		}
		if (exhausted && closesWhenExhausted(definition.invadeType())) {
			closeIfActive(definition.id(), active);
		}
	}

	static TeleportAnimation animationFor(int invadeType) {
		return switch (invadeType) {
			case 1, 2, 3, 6 -> TeleportAnimation.INVASION_PORTAL;
			default -> TeleportAnimation.DIRECT_PORTAL;
		};
	}

	static boolean closesWhenExhausted(int invadeType) {
		return invadeType != 6;
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
		int instanceId = owner.getWorldId() == endpoint.worldId() ? owner.getInstanceId() : 1;
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
		private int remaining;
		private volatile boolean closed;
		private Future<?> expiry;

		private ActivePortal(DirectPortal definition, Npc start, Npc destination, DirectPortalPoint destinationPoint) {
			this.definition = definition;
			this.start = start;
			this.destination = destination;
			this.destinationPoint = destinationPoint;
			remaining = definition.count();
		}
	}
}
