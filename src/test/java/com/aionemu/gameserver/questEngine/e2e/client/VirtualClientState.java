package com.aionemu.gameserver.questEngine.e2e.client;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 单场景可变的客户端与内存世界状态；所有变更都由明确的任务事务或提交后动作驱动。
 * Mutable client and in-memory-world state scoped to one scenario; every mutation is driven by an explicit quest
 * transaction or after-commit action.
 */
public final class VirtualClientState {
	private final int questId;
	private QuestStatus status = QuestStatus.NONE;
	private int packedVariables;
	private final Map<Integer, Integer> inventory = new LinkedHashMap<>();
	private int level = 65;
	private Race race = Race.ELYOS;
	private PlayerClass playerClass = PlayerClass.GLADIATOR;
	private int worldId = 110010000;
	private int instanceId = 1;
	private float x;
	private float y;
	private float z;
	private byte heading;
	private int currentNpcId;
	private int currentObjectId;
	private int currentPage;
	private int currentPageTargetObjectId;
	private final List<ServerPacketObservation> packets = new ArrayList<>();
	private final Map<String, Integer> spawnedNpcObjectIds = new LinkedHashMap<>();
	private final Map<String, Boolean> followingSlots = new LinkedHashMap<>();

	public VirtualClientState(int questId) {
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		this.questId = questId;
	}

	/** 原子替换任务的规范投影。 / Atomically replaces the canonical quest projection. */
	public void project(QuestStatus status, int packedVariables) {
		this.status = Objects.requireNonNull(status, "status");
		this.packedVariables = packedVariables;
	}

	/** 记录当前权威交互对象。 / Records the current authoritative interaction object. */
	public void interactWith(int npcId, int objectId) {
		if (npcId <= 0 || objectId <= 0) {
			throw new IllegalArgumentException("interaction ids must be positive");
		}
		currentNpcId = npcId;
		currentObjectId = objectId;
	}

	/** 将客户端页面切换到服务端刚发送的页面。 / Moves the client to the page just sent by the server. */
	public void showPage(int page) {
		if (page < 0) {
			throw new IllegalArgumentException("page must be non-negative");
		}
		currentPage = page;
	}

	/** 关闭当前客户端页面。 / Closes the current client page. */
	public void closePage() {
		currentPage = 0;
		currentPageTargetObjectId = 0;
	}

	/** 替换一个相关物品的确定数量。 / Replaces the known count of one relevant item. */
	public void setItemCount(int itemId, int count) {
		if (itemId <= 0 || count < 0) {
			throw new IllegalArgumentException("itemId must be positive and count non-negative");
		}
		if (count == 0) {
			inventory.remove(itemId);
		} else {
			inventory.put(itemId, count);
		}
	}

	/** 记录一个已解析服务端包并同步页面状态。 / Records a parsed server packet and synchronizes page state. */
	public void observe(ServerPacketObservation observation) {
		packets.add(Objects.requireNonNull(observation, "observation"));
		if (observation.type() == ServerPacketObservation.Type.DIALOG_WINDOW) {
			if (observation.dialogId() == 0) {
				closePage();
			} else {
				showPage(observation.dialogId());
				currentPageTargetObjectId = observation.targetObjectId();
			}
		}
	}

	/** 记录 slot 到权威 NPC objectId 的映射。 / Records a slot-to-authoritative-NPC object-id mapping. */
	public void spawned(String slot, int objectId) {
		if (slot == null || slot.isBlank() || objectId <= 0) {
			throw new IllegalArgumentException("spawn slot and objectId must be valid");
		}
		spawnedNpcObjectIds.put(slot, objectId);
	}

	/** 移除 slot 的权威 NPC。 / Removes the authoritative NPC for a slot. */
	public void despawned(String slot) {
		spawnedNpcObjectIds.remove(slot);
		followingSlots.remove(slot);
	}

	/** 更新 slot 的跟随状态。 / Updates the follow state for a slot. */
	public void following(String slot, boolean following) {
		followingSlots.put(slot, following);
	}

	/** 更新内存世界坐标。 / Updates the in-memory world coordinates. */
	public void moveTo(int worldId, int instanceId, float x, float y, float z, byte heading) {
		if (worldId <= 0 || instanceId <= 0) {
			throw new IllegalArgumentException("worldId and instanceId must be positive");
		}
		this.worldId = worldId;
		this.instanceId = instanceId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
	}

	public int questId() { return questId; }
	public QuestStatus status() { return status; }
	public int packedVariables() { return packedVariables; }
	public Map<Integer, Integer> inventory() { return Map.copyOf(inventory); }
	public int level() { return level; }
	public Race race() { return race; }
	public PlayerClass playerClass() { return playerClass; }
	public int worldId() { return worldId; }
	public int instanceId() { return instanceId; }
	public float x() { return x; }
	public float y() { return y; }
	public float z() { return z; }
	public byte heading() { return heading; }
	public int currentNpcId() { return currentNpcId; }
	public int currentObjectId() { return currentObjectId; }
	public int currentPage() { return currentPage; }
	public int currentPageTargetObjectId() { return currentPageTargetObjectId; }
	public List<ServerPacketObservation> packets() { return List.copyOf(packets); }
	public Map<String, Integer> spawnedNpcObjectIds() { return Map.copyOf(spawnedNpcObjectIds); }
	public Map<String, Boolean> followingSlots() { return Map.copyOf(followingSlots); }

	/** 设置玩家种族和职业事实。 / Sets player race and class facts. */
	public void playerFacts(int level, Race race, PlayerClass playerClass) {
		if (level <= 0) {
			throw new IllegalArgumentException("level must be positive");
		}
		this.level = level;
		this.race = Objects.requireNonNull(race, "race");
		this.playerClass = Objects.requireNonNull(playerClass, "playerClass");
	}
}
