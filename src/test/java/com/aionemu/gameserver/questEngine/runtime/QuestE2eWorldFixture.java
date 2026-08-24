package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.TransformModel;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.e2e.client.VirtualClientState;
import com.aionemu.gameserver.questEngine.e2e.client.QuestTrace;
import com.aionemu.gameserver.questEngine.e2e.world.VirtualClock;
import com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget;
import com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnLocation;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.KnownList;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * 独立内存世界夹具：复用真实 PlayerQuestSpawnPort、PlayerQuestAiPort、PlayerQuestTeleportPort 和
 * PlayerQuestMoviePort，并把 World/AI/传送/出站包替换为可记录的确定性适配器。
 * Isolated in-memory world fixture that reuses the real PlayerQuestSpawnPort, PlayerQuestAiPort,
 * PlayerQuestTeleportPort, and PlayerQuestMoviePort while replacing World/AI/teleport/packet delivery with
 * deterministic recording adapters.
 */
public final class QuestE2eWorldFixture implements AutoCloseable {
	public static final int PLAYER_ID = 7;

	/**
	 * 结束测试回环首次加载 AionConnection 时创建的包处理 worker。
	 * Stops the packet-processing worker created when the test loop first loads AionConnection.
	 *
	 * <p>仅供命令行报告退出前清理测试线程，不触碰真实服务器生命周期。
	 * This is only for command-line report cleanup and does not touch a real server lifecycle.</p>
	 */
	@SuppressWarnings("unchecked")
	public static void shutdownPacketProcessor() {
		try {
			Field processorField = AionConnection.class.getDeclaredField("packetProcessor");
			processorField.setAccessible(true);
			Object processor = processorField.get(null);
			Field threadsField = processor.getClass().getDeclaredField("threads");
			threadsField.setAccessible(true);
			for (Thread thread : List.copyOf((List<Thread>) threadsField.get(processor))) {
				thread.interrupt();
			}
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("cannot stop isolated packet processor", exception);
		}
	}
	private final VirtualClientState state;
	private final QuestTrace trace;
	private final VirtualClock clock = new VirtualClock();
	private final Map<Integer, VisibleObject> objects = new LinkedHashMap<>();
	private final Map<Integer, Integer> activeEffects = new LinkedHashMap<>();
	private final QuestSpawnRegistry spawnRegistry = new QuestSpawnRegistry();
	private final Player player;
	private final ProtocolKnownList playerKnownList;
	private final AionConnection connection;
	private final PlayerQuestSpawnPort spawnPort;
	private final PlayerQuestAiPort aiPort;
	private final PlayerQuestTeleportPort teleportPort;
	private final PlayerQuestMoviePort moviePort;
	private final int oldMinThreads;
	private final int oldMaxThreads;
	private final int oldSpawnThreshold;
	private final int oldKillThreshold;
	private int nextObjectId = 100_000;
	private int packetCursor;

	public QuestE2eWorldFixture(VirtualClientState state, QuestTrace trace) throws Exception {
		this.state = java.util.Objects.requireNonNull(state, "state");
		this.trace = java.util.Objects.requireNonNull(trace, "trace");
		oldMinThreads = NetworkConfig.PACKET_PROCESSOR_MIN_THREADS;
		oldMaxThreads = NetworkConfig.PACKET_PROCESSOR_MAX_THREADS;
		oldSpawnThreshold = NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD;
		oldKillThreshold = NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD;
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = 1;
		player = createPlayer();
		playerKnownList = (ProtocolKnownList) player.getKnownList();
		connection = player.getClientConnection();
		spawnPort = new PlayerQuestSpawnPort(id -> id == PLAYER_ID ? player : null, spawnRegistry,
			(worldId, instanceId, templateId, x, y, z, heading) -> {
				Npc npc = npc(templateId, worldId, instanceId);
				objects.put(npc.getObjectId(), npc);
				trace.add("WORLD", "spawn:" + templateId + ":" + npc.getObjectId());
				return npc;
			}, bound -> 0);
			aiPort = new PlayerQuestAiPort(id -> id == PLAYER_ID ? player : null, spawnRegistry,
			(npc, ignoredPlayer, target, command, argument) -> {
				trace.add("AI", command + ":" + npc.getObjectId());
				return command != PlayerQuestAiPort.Command.ATTACK_TARGET || target != null;
			}, objectId -> objects.get(objectId),
			(ignoredPlayer, npc, questId, zone) -> followFuture(npc, "zone:" + zone),
			(ignoredPlayer, npc, questId, x, y, z) -> followFuture(npc, "point:" + x + ":" + y + ":" + z),
			(ignoredPlayer, future) -> trace.add("AI", "register-follow"),
			(ignoredPlayer, npc) -> trace.add("AI", "npc-info:" + npc.getObjectId()),
				(ignoredPlayer, templateId) -> objects.values().stream().filter(Npc.class::isInstance)
					.map(Npc.class::cast).filter(npc -> npc.getNpcId() == templateId).findFirst().orElse(null));
		setField(PlayerQuestAiPort.class, aiPort, "targetNpcResolver",
			(PlayerQuestAiPort.TargetNpcResolver) (ignoredPlayer, templateId) -> findNpc(templateId));
		setField(PlayerQuestAiPort.class, aiPort, "targetNpcFollow",
			(PlayerQuestAiPort.TargetNpcFollowCall) (ignoredPlayer, npc, target, questId) ->
				followFuture(npc, "npc:" + target.getObjectId()));
		setField(PlayerQuestAiPort.class, aiPort, "luredNpcWatch",
			(PlayerQuestAiPort.LuredNpcWatchCall) (ignoredPlayer, npc, questId, x, y, z, radius, completion) -> {
				trace.add("AI", "lure:" + npc.getObjectId() + ":" + x + ":" + y + ":" + z + ":" + radius
					+ ":" + completion);
				clock.schedule(1, () -> trace.add("AI", "lure-tick:" + npc.getObjectId()));
				return true;
			});
		teleportPort = new PlayerQuestTeleportPort(id -> id == PLAYER_ID ? player : null,
			(p, worldId, instanceId, x, y, z, heading) -> {
				state.moveTo(worldId, instanceId, x, y, z, heading);
				trace.add("WORLD", "teleport:" + worldId + ":" + instanceId);
				return true;
			});
		moviePort = new PlayerQuestMoviePort(id -> id == PLAYER_ID ? player : null,
			(p, movieId, type) -> {
				trace.add("PACKET", "movie:" + movieId + ":" + type);
				com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(p, new SM_PLAY_MOVIE(type.wireValue(), movieId));
				return true;
			});
	}

	/** 返回真实轻量 Player。 / Returns the real lightweight Player. */
	public Player player() { return player; }
	/** 返回真实连接及其出站包队列。 / Returns the real connection and its outbound queue. */
	public AionConnection connection() { return connection; }
	/** 返回复用的真实 spawn port。 / Returns the reused real spawn port. */
	public PlayerQuestSpawnPort spawnPort() { return spawnPort; }
	/** 返回复用的真实 AI port。 / Returns the reused real AI port. */
	public PlayerQuestAiPort aiPort() { return aiPort; }
	/** 返回复用的真实传送 port。 / Returns the reused real teleport port. */
	public PlayerQuestTeleportPort teleportPort() { return teleportPort; }
	/** 返回复用的真实电影 port。 / Returns the reused real movie port. */
	public PlayerQuestMoviePort moviePort() { return moviePort; }
	/** 返回确定性时钟。 / Returns the deterministic clock. */
	public VirtualClock clock() { return clock; }
	/** 返回权威 spawn 注册表。 / Returns the authoritative spawn registry. */
	public QuestSpawnRegistry spawnRegistry() { return spawnRegistry; }

	/**
	 * 为当前场景安装独立的玩家 NPC faction 状态。
	 * Installs isolated player NPC-faction state for the current scenario.
	 */
	public NpcFaction seedNpcFaction(int npcFactionId, int questId, boolean mentor,
			ENpcFactionQuestState questState) {
		if (npcFactionId <= 0 || questId <= 0) {
			throw new IllegalArgumentException("npcFactionId and questId must be positive");
		}
		try {
			NpcFaction faction = new ObjenesisStd().newInstance(NpcFaction.class);
			setField(NpcFaction.class, faction, "id", npcFactionId);
			setField(NpcFaction.class, faction, "active", true);
			setField(NpcFaction.class, faction, "mentor", mentor);
			setField(NpcFaction.class, faction, "state", java.util.Objects.requireNonNull(questState, "questState"));
			setField(NpcFaction.class, faction, "questId", questId);
			NpcFactions factions = new NpcFactions(player);
			factions.addNpcFaction(faction);
			player.setNpcFactions(factions);
			return faction;
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("cannot initialize in-memory NPC faction", exception);
		}
	}

	/** 返回当前场景的 NPC faction 任务状态。 / Returns the current scenario NPC-faction quest state. */
	public ENpcFactionQuestState npcFactionState(int npcFactionId) {
		NpcFaction faction = player.getNpcFactions() == null ? null
			: player.getNpcFactions().getNpcFactionById(npcFactionId);
		return faction == null ? null : faction.getState();
	}

	/** 对齐轻量 Player 与当前场景的种族和职业事实。 / Aligns the lightweight Player with current scenario race and class facts. */
	public void playerFacts(Race race, PlayerClass playerClass) {
		player.getCommonData().setRace(java.util.Objects.requireNonNull(race, "race"));
		player.getCommonData().setPlayerClass(java.util.Objects.requireNonNull(playerClass, "playerClass"));
	}

	/** 在不加载完整玩家模板的情况下执行合法的二转职业变更。 / Applies a legal class advancement without loading full player templates. */
	public boolean changePlayerClass(PlayerClass playerClass) {
		java.util.Objects.requireNonNull(playerClass, "playerClass");
		PlayerClass current = player.getPlayerClass();
		if (!current.isStartingClass() || playerClass.isStartingClass() || playerClass == PlayerClass.ALL
				|| PlayerClass.getStartingClassFor(playerClass) != current) {
			return false;
		}
		player.getCommonData().setPlayerClass(playerClass);
		state.playerFacts(state.level(), state.race(), playerClass);
		trace.add("WORLD", "player-class:" + current + ":" + playerClass);
		return true;
	}

	/** 记录一个真实 effect-port 应用到内存玩家的效果。 / Records an effect applied to the in-memory player by the real effect port. */
	public void applyEffect(int skillId, int durationMillis) {
		activeEffects.put(skillId, durationMillis);
		trace.add("WORLD", "effect-apply:" + skillId + ":" + durationMillis);
	}

	/** 移除内存玩家效果。 / Removes an in-memory player effect. */
	public void removeEffect(int effectId) {
		activeEffects.remove(effectId);
		trace.add("WORLD", "effect-remove:" + effectId);
	}

	/** 为移除效果场景预置玩家已有状态。 / Seeds an existing effect for a removal scenario. */
	public void seedEffect(int effectId) {
		activeEffects.put(effectId, 0);
	}

	/** 返回玩家是否仍持有指定效果。 / Returns whether the player still has the specified effect. */
	public boolean hasEffect(int effectId) {
		return activeEffects.containsKey(effectId);
	}

	/** 为需要 slot 的 AI 场景种入一个 NPC。 / Seeds an NPC for an AI scenario that needs a slot. */
	public Npc seedSlot(String slot, int templateId) {
		Npc npc = npc(templateId, state.worldId(), state.instanceId());
		objects.put(npc.getObjectId(), npc);
		QuestSnapshot snapshot = snapshot();
		spawnRegistry.register(snapshot, slot, npc);
		state.spawned(slot, npc.getObjectId());
		return npc;
	}

	/** 为交互 objectId 建立常驻 NPC。 / Creates a resident NPC for an interaction object id. */
	public Npc seedInteractionNpc(int npcId, int objectId) {
		Npc npc = npc(npcId, state.worldId(), state.instanceId(), objectId);
		objects.put(objectId, npc);
		playerKnownList.expose(npc);
		state.interactWith(npcId, objectId);
		return npc;
	}

	/** 为需要世界目标的 AI 场景种入 NPC，但不改变当前交互对象。 / Seeds an NPC for an AI world target without replacing the current interaction object. */
	public Npc seedWorldNpc(int npcId) {
		Npc npc = npc(npcId, state.worldId(), state.instanceId());
		objects.put(npc.getObjectId(), npc);
		return npc;
	}

	/** 返回最后一次 CM_DIALOG_SELECT 是否由 typed owner 处理。 / Returns whether the last CM_DIALOG_SELECT was handled by a typed owner. */
	public boolean protocolDialogHandled(int objectId) {
		VisibleObject object = objects.get(objectId);
		return object instanceof Npc npc && npc.getController() instanceof ProtocolNpcController controller
			&& controller.consumeHandled();
	}

	/** 为 CM_USE_ITEM 种入一个只读任务物品。 / Seeds a read-only quest item for CM_USE_ITEM. */
	public Item seedQuestItem(int itemId, int itemObjectId) {
		try {
			ItemTemplate template = new ItemTemplate();
			setField(ItemTemplate.class, template, "itemId", itemId);
			int[] requiredLevels = new int[PlayerClass.values().length];
			java.util.Arrays.fill(requiredLevels, 1);
			setField(ItemTemplate.class, template, "restricts", requiredLevels);
			Item item = new Item(itemObjectId, template);
			if (!(player.getInventory() instanceof ProtocolStorage storage)) {
				throw new IllegalStateException("protocol storage is not installed");
			}
			storage.expose(item);
			state.setItemCount(itemId, 1);
			return item;
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("cannot initialize protocol quest item", exception);
		}
	}

	/** 运行一个虚拟 tick，并返回本 tick 新产生的包观察。 / Runs one virtual tick and returns observations for packets emitted during it. */
	public List<ServerPacketObservation> tick(long millis) {
		clock.tick(millis);
		return drainPackets();
	}

	/** 读取并解析真实出站包队列新增内容。 / Reads and parses newly appended real outbound packets. */
	public List<ServerPacketObservation> drainPackets() {
		List<AionServerPacket> queue = packetQueue(connection);
		List<ServerPacketObservation> result = new ArrayList<>();
		while (packetCursor < queue.size()) {
			AionServerPacket packet = queue.get(packetCursor++);
			ServerPacketObservation observation = observe(packet);
			result.add(observation);
			state.observe(observation);
		}
		return result;
	}

	/** 清除当前连接包观察游标但保留真实队列，适用于一个场景的阶段性断言。 / Resets the observation cursor while retaining the real queue. */
	public void markPacketsRead() {
		packetCursor = packetQueue(connection).size();
	}

	/** 从状态构造默认快照，供真实 spawn/Ai 端口使用。 / Builds a default snapshot from state for real spawn/AI ports. */
	public QuestSnapshot snapshot() {
		return new QuestSnapshot(PLAYER_ID, state.questId(), state.status(), state.packedVariables(), state.inventory(),
			Map.of(), true, true, state.currentObjectId(), state.currentObjectId(), state.worldId(), state.instanceId(),
			state.x(), state.y(), state.z(), state.heading());
	}

	@Override
	public void close() {
		spawnRegistry.cleanup(PLAYER_ID, state.questId());
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = oldMinThreads;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = oldMaxThreads;
		NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = oldSpawnThreshold;
		NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = oldKillThreshold;
	}

	private Future<?> followFuture(Npc npc, String detail) {
		trace.add("AI", "follow:" + npc.getObjectId() + ":" + detail);
		return clock.schedule(1, () -> trace.add("AI", "tick:" + npc.getObjectId()));
	}

	private Player createPlayer() throws Exception {
		ObjenesisStd objenesis = new ObjenesisStd();
		Player result = objenesis.newInstance(Player.class);
		setField(AionObject.class, result, "objectId", PLAYER_ID);
		result.setPosition(new WorldPosition(state.worldId()));
		result.getPosition().setXYZH(state.x(), state.y(), state.z(), state.heading());
		setField(Player.class, result, "questStateList", new QuestStateList());
		PlayerCommonData commonData = new PlayerCommonData(PLAYER_ID);
		commonData.setRace(Race.ELYOS);
		commonData.setPlayerClass(PlayerClass.GLADIATOR);
		commonData.setGender(Gender.MALE);
		setField(PlayerCommonData.class, commonData, "level", 65);
		setField(Player.class, result, "playerCommonData", commonData);
		Account account = new Account(1);
		account.setAccessLevel((byte) 0);
		setField(Player.class, result, "playerAccount", account);
		ProtocolStorage inventory = new ProtocolStorage();
		inventory.setOwner(result);
			setField(Player.class, result, "inventory", inventory);
			result.setEquipment(new Equipment(result));
			result.setGameStats(new ProtocolPlayerGameStats(result));
		setField(VisibleObject.class, result, "objectTemplate", new NpcTemplate());
		result.setTransformModel(new TransformModel(result));
		result.setEffectController(new PlayerEffectController(result));
		result.setKnownlist(new ProtocolKnownList(result));
		AionConnection packetConnection = objenesis.newInstance(AionConnection.class);
		RecordingTransport transport = new RecordingTransport();
		transport.connection = packetConnection;
		setField(AConnection.class, packetConnection, "transport", transport);
		setField(AConnection.class, packetConnection, "guard", new Object());
		setField(AionConnection.class, packetConnection, "sendMsgQueue", new ArrayList<AionServerPacket>());
		setField(AionConnection.class, packetConnection, "state", AionConnection.State.IN_GAME);
		setField(AionConnection.class, packetConnection, "activePlayer",
			new java.util.concurrent.atomic.AtomicReference<>(result));
		packetConnection.setAccount(account);
		result.setClientConnection(packetConnection);
		return result;
	}

	private Npc findNpc(int templateId) {
		return objects.values().stream().filter(Npc.class::isInstance).map(Npc.class::cast)
			.filter(npc -> npc.getNpcId() == templateId).findFirst().orElse(null);
	}

	private Npc npc(int templateId, int worldId, int instanceId) {
		return npc(templateId, worldId, instanceId, nextObjectId++);
	}

	private Npc npc(int templateId, int worldId, int instanceId, int objectId) {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		try {
			setField(AionObject.class, npc, "objectId", objectId);
			NpcTemplate template = new NpcTemplate();
			setField(NpcTemplate.class, template, "npcId", templateId);
			npc.setObjectTemplate(template);
			ProtocolNpcController controller = new ProtocolNpcController();
			setField(VisibleObject.class, npc, "controller", controller);
			controller.setOwner(npc);
		} catch (Exception exception) {
			throw new IllegalStateException("cannot initialize in-memory NPC", exception);
		}
		npc.setPosition(new WorldPosition(worldId));
		npc.getPosition().setXYZH(0f, 0f, 0f, (byte) 0);
		return npc;
	}

	private static ServerPacketObservation observe(AionServerPacket packet) {
		try {
			if (packet instanceof SM_DIALOG_WINDOW dialog) {
				return ServerPacketObservation.dialog(intField(SM_DIALOG_WINDOW.class, dialog, "targetObjectId"),
					intField(SM_DIALOG_WINDOW.class, dialog, "dialogID"), intField(SM_DIALOG_WINDOW.class, dialog, "questId"));
			}
			if (packet instanceof SM_QUEST_ACTION action) {
				return ServerPacketObservation.questAction(intField(SM_QUEST_ACTION.class, action, "questId"),
					intField(SM_QUEST_ACTION.class, action, "action"), intField(SM_QUEST_ACTION.class, action, "status"),
					intField(SM_QUEST_ACTION.class, action, "step"));
			}
			if (packet instanceof SM_PLAY_MOVIE movie) {
				return ServerPacketObservation.movie(intField(SM_PLAY_MOVIE.class, movie, "movieId"),
					intField(SM_PLAY_MOVIE.class, movie, "type"));
			}
			return ServerPacketObservation.other(packet.getClass().getSimpleName());
		} catch (ReflectiveOperationException exception) {
			return ServerPacketObservation.other(packet.getClass().getSimpleName() + ":reflection-failure");
		}
	}

	@SuppressWarnings("unchecked")
	private static List<AionServerPacket> packetQueue(AionConnection connection) {
		try {
			Field field = AionConnection.class.getDeclaredField("sendMsgQueue");
			field.setAccessible(true);
			return (List<AionServerPacket>) field.get(connection);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("cannot inspect packet queue", exception);
		}
	}

	private static int intField(Class<?> declaringClass, Object target, String name) throws ReflectiveOperationException {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(target);
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value)
			throws ReflectiveOperationException {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	/** 只提供协议包构造所需速度的轻量玩家属性。 / Lightweight player stats providing only speeds required by packets. */
	private static final class ProtocolPlayerGameStats extends PlayerGameStats {
		private final Player owner;

		private ProtocolPlayerGameStats(Player owner) {
			super(owner);
			this.owner = owner;
		}

		@Override
		public Stat2 getAttackSpeed() {
			return new AdditionStat(StatEnum.ATTACK_SPEED, 1500, owner);
		}

		@Override
		public Stat2 getMovementSpeed() {
			return new AdditionStat(StatEnum.SPEED, 6000, owner);
		}
	}

	/** 仅暴露协议场景权威对象的已知列表。 / Known list exposing only authoritative protocol-scenario objects. */
	private static final class ProtocolKnownList extends KnownList {
		private final Map<Integer, VisibleObject> exposed = new LinkedHashMap<>();

		private ProtocolKnownList(VisibleObject owner) {
			super(owner);
		}

		private void expose(VisibleObject object) {
			exposed.put(object.getObjectId(), object);
		}

		@Override
		public VisibleObject getObject(int targetObjectId) {
			return exposed.get(targetObjectId);
		}
	}

	/** 只保存协议测试任务物品的轻量背包。 / Lightweight inventory retaining only protocol-test quest items. */
	private static final class ProtocolStorage extends PlayerStorage {
		private final Map<Integer, Item> exposed = new LinkedHashMap<>();

		private ProtocolStorage() {
			super(StorageType.CUBE);
		}

		private void expose(Item item) {
			exposed.put(item.getObjectId(), item);
		}

		@Override
		public Item getItemByObjId(int itemObjId) {
			return exposed.get(itemObjId);
		}

		@Override
		public long getItemCountByItemId(int itemId) {
			return exposed.values().stream()
				.filter(item -> item.getItemTemplate().getTemplateId() == itemId)
				.mapToLong(Item::getItemCount).sum();
		}
	}

	/**
	 * 把 NPC 控制器选择和交互物完成回送到真实 QuestEngine 入口。
	 * Routes NPC controller selections and interaction-object completion into the real QuestEngine ingress.
	 */
	private static final class ProtocolNpcController extends NpcController {
		private boolean handled;

		/**
		 * 将真实 CM_SHOW_DIALOG 解码后的交互请求确定性推进到 AI 使用完成回调。
		 * Deterministically advances the interaction decoded from a real CM_SHOW_DIALOG to the AI use-completion
		 * callback.
		 */
		@Override
		public void onDialogRequest(Player player) {
			handled = GameEngineServices.questEngine().onDialog(new QuestEnv(
				getOwner(), player, 0, QuestDialog.USE_OBJECT.id()));
			if (!handled) {
				handled = GameEngineServices.questEngine().onDialog(new QuestEnv(
					getOwner(), player, 0, QuestDialog.START_DIALOG.id()));
			}
		}

		@Override
		public void onDialogSelect(int dialogId, Player player, int questId, int extendedRewardIndex) {
			handled = GameEngineServices.questEngine().onDialog(
				new QuestEnv(getOwner(), player, questId, dialogId));
		}

		private boolean consumeHandled() {
			boolean result = handled;
			handled = false;
			return result;
		}
	}

	/** 捕获出站包而不触发真实网络写入。 / Captures outbound packets without real network writes. */
	private static final class RecordingTransport implements ConnectionTransport {
		private AionConnection connection;
		@Override public String getIP() { return "127.0.0.1"; }
		@Override public void enableWriteInterest() { }
		@Override public void close(boolean forced) { }
		@Override public boolean onlyClose() { return true; }
	}
}
