package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemDialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;

/**
 * 将服务端确认的物品使用绑定为短期、一次性的任务对话授权，并拒绝伪造、跨 owner 和重放。
 * Binds a server-confirmed item use to a short-lived, one-time quest dialog authorization and rejects forgery,
 * cross-owner use, and replay.
 */
public final class QuestGraphItemDialogBridge {

	private static final long MAX_LIFETIME_MILLIS = 300_000;
	private final int playerId;
	private final ItemAuthority itemAuthority;
	private final Map<Integer, Authorization> authorizations = new ConcurrentHashMap<>();

	/**
	 * 创建绑定单一玩家和服务端物品归属查询的 bridge。
	 * Creates a bridge bound to one player and a server-side item ownership query.
	 */
	public QuestGraphItemDialogBridge(int playerId, ItemAuthority itemAuthority) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Item dialog bridge player id is invalid");
		}
		this.playerId = playerId;
		this.itemAuthority = Objects.requireNonNull(itemAuthority, "item authority");
	}

	/**
	 * 从目标节点的已编译 ITEM_DIALOG 出边建立一次性授权；重试只能复用相同授权标识。
	 * Opens one-time authorization from compiled ITEM_DIALOG edges on the target node; retries may only reuse the same
	 * authorization identity.
	 */
	public synchronized Authorization open(ItemUseEvent source, CompiledQuestGraph graph, String targetNode,
			long authorizationId, long expiresAt) {
		Objects.requireNonNull(source, "item use event");
		Objects.requireNonNull(graph, "quest graph");
		if (source.playerId() != playerId || authorizationId <= 0 || expiresAt <= source.occurredAt()
				|| expiresAt - source.occurredAt() > MAX_LIFETIME_MILLIS
				|| !itemAuthority.owns(playerId, source.itemId(), source.itemObjectId())) {
			throw new IllegalArgumentException("Item dialog authorization input is invalid");
		}
		CompiledQuestGraph.Node node = graph.nodes().get(targetNode);
		if (node == null) {
			throw new IllegalArgumentException("Item dialog target node is missing");
		}
		Set<String> allowedDialogs = new LinkedHashSet<>();
		for (CompiledQuestGraph.Transition transition : node.transitions()) {
			CompiledQuestGraph.Event event = transition.event();
			if (event.type() == EventType.ITEM_DIALOG && event.targetId() == source.itemId()
					&& event.qualifier() != null && !event.qualifier().isBlank()) {
				allowedDialogs.add(event.qualifier());
			}
		}
		if (allowedDialogs.isEmpty()) {
			throw new IllegalArgumentException("Item dialog graph has no authorized choices");
		}
		Authorization existing = authorizations.get(graph.questId());
		if (existing != null && existing.expiresAt() >= source.occurredAt()) {
			if (existing.authorizationId() == authorizationId && existing.sourceEventId().equals(source.eventId())
					&& existing.itemId() == source.itemId() && existing.itemObjectId() == source.itemObjectId()
					&& existing.issuedAt() == source.occurredAt() && existing.allowedDialogs().equals(allowedDialogs)
					&& existing.expiresAt() == expiresAt) {
				return existing;
			}
			throw new IllegalStateException("A different item dialog authorization is already active");
		}
		Authorization created = new Authorization(authorizationId, source.eventId(), playerId, graph.questId(),
			source.itemId(), source.itemObjectId(), source.occurredAt(), expiresAt, allowedDialogs);
		authorizations.put(graph.questId(), created);
		return created;
	}

	/**
	 * 校验并消费一次性授权，生成只能由服务端 bridge 构造的 ITEM_DIALOG 事件。
	 * Validates and consumes one-time authorization, producing an ITEM_DIALOG event only through the server bridge.
	 */
	public synchronized ItemDialogEvent accept(ClientDialogInput input) {
		Objects.requireNonNull(input, "item dialog input");
		Authorization authorization = authorizations.get(input.questId());
		if (input.playerId() != playerId || authorization == null
				|| authorization.authorizationId() != input.authorizationId()
				|| authorization.playerId() != input.playerId() || authorization.questId() != input.questId()
				|| authorization.itemId() != input.itemId() || authorization.itemObjectId() != input.itemObjectId()
				|| input.occurredAt() < authorization.issuedAt()
				|| !authorization.allowedDialogs().contains(input.dialog())) {
			throw new IllegalArgumentException("Item dialog authorization was rejected");
		}
		if (input.occurredAt() > authorization.expiresAt()
				|| !itemAuthority.owns(playerId, input.itemId(), input.itemObjectId())) {
			authorizations.remove(input.questId(), authorization);
			throw new IllegalArgumentException("Item dialog authorization expired or lost item authority");
		}
		authorizations.remove(input.questId(), authorization);
		return new ItemDialogEvent(input.eventId(), playerId, input.occurredAt(), input.questId(), input.itemId(),
			input.itemObjectId(), input.dialog(), input.authorizationId());
	}

	/** 清理单个任务 owner 的待处理授权。 / Clears pending authorization for one quest owner. */
	public synchronized void removeOwner(int questId) {
		if (questId > 0) {
			authorizations.remove(questId);
		}
	}

	/** 登出、断线或 bridge 销毁时清理全部临时授权。 / Clears all transient authorizations on logout, disconnect, or bridge disposal. */
	public synchronized void clear() {
		authorizations.clear();
	}

	/** 查询当前待处理授权数，仅用于确定性审计。 / Returns the pending authorization count for deterministic audit. */
	public int pendingCount() {
		return authorizations.size();
	}

	/** 定义服务端物品实例归属查询端口。 / Defines the server-side item-instance ownership query port. */
	@FunctionalInterface
	public interface ItemAuthority {
		/** 校验物品实例仍由目标玩家持有且模板一致。 / Verifies the item instance is still owned by the player with the expected template. */
		boolean owns(int playerId, int itemId, int itemObjectId);
	}

	/** 表示已冻结的一次性物品对话授权。 / Represents a frozen one-time item dialog authorization. */
	public record Authorization(long authorizationId, String sourceEventId, int playerId, int questId, int itemId,
			int itemObjectId, long issuedAt, long expiresAt, Set<String> allowedDialogs) {
		/** 复制动作集合并校验全部服务端字段。 / Copies the action set and validates every server-side field. */
		public Authorization {
			allowedDialogs = Set.copyOf(allowedDialogs);
			if (authorizationId <= 0 || sourceEventId == null || sourceEventId.isBlank() || playerId <= 0 || questId <= 0
					|| itemId <= 0 || itemObjectId <= 0 || issuedAt < 0 || expiresAt <= issuedAt || allowedDialogs.isEmpty()
					|| allowedDialogs.stream().anyMatch(value -> value == null || value.isBlank())) {
				throw new IllegalArgumentException("Item dialog authorization is invalid");
			}
		}
	}

	/** 表示经网络层解析但尚未获得任务权限的客户端对话输入。 / Represents client dialog input parsed by the network layer but not yet quest-authorized. */
	public record ClientDialogInput(String eventId, int playerId, long occurredAt, int questId, int itemId,
			int itemObjectId, String dialog, long authorizationId) {
		/** 校验输入形状；业务权限仍由 bridge 校验。 / Validates input shape; the bridge still validates business authority. */
		public ClientDialogInput {
			if (eventId == null || eventId.isBlank() || playerId <= 0 || occurredAt < 0 || questId <= 0 || itemId <= 0
					|| itemObjectId <= 0 || dialog == null || dialog.isBlank() || authorizationId <= 0) {
				throw new IllegalArgumentException("Item dialog client input is invalid");
			}
		}
	}
}
