package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Production shadow capture entry point.
 *
 * <p>{@link #open(Player, QuestEvent, Collection)} starts one capture scope at a
 * physical event boundary (for example one kill dispatch). The scope freezes the
 * pre-event {@link QuestSnapshot} of every candidate owner before any legacy
 * handler runs, collects the {@link QuestLegacyInvocation} records produced by
 * the bridge while the scope is open, and binds them on close so one physical
 * event yields one {@link QuestShadowBatchRunner.Envelope}: the authoritative
 * event, every frozen pre-event snapshot and the aggregated legacy observations
 * of every owner that ran, bound as a single atomic shadow input.</p>
 *
 * <p>Capture itself is diagnostic only: it never calls a service, never writes a
 * connection, never executes a candidate action and never changes the legacy
 * result contract. A failing scope degrades to no capture instead of altering
 * the old route.</p>
 */
@Slf4j(topic = "QUEST_SHADOW")
public final class QuestShadowCapture implements QuestLegacyObservationSink {
	private final QuestStartEligibilityPort startEligibilityPort;
	private final Set<Integer> startEligibilityOwners;
	private final QuestLegacyObservationStore store = new QuestLegacyObservationStore();
	private final ThreadLocal<Scope> current = new ThreadLocal<>();
	private final List<QuestShadowBatchRunner.Envelope> envelopes = new ArrayList<>();

	public QuestShadowCapture() {
		this(null, Set.of());
	}

	QuestShadowCapture(QuestStartEligibilityPort startEligibilityPort, Set<Integer> startEligibilityOwners) {
		this.startEligibilityPort = startEligibilityPort;
		this.startEligibilityOwners = Set.copyOf(Objects.requireNonNull(startEligibilityOwners,
			"startEligibilityOwners"));
		if (!this.startEligibilityOwners.isEmpty() && startEligibilityPort == null) {
			throw new IllegalArgumentException("startEligibilityPort is required for configured owners");
		}
	}

	/**
	 * Opens a capture scope for one physical event. Snapshots are frozen now,
	 * before any owner runs, and are never refreshed afterwards.
	 *
	 * @param player   the player triggering the event (snapshot source)
	 * @param event    the authoritative typed event fact
	 * @param questIds candidate owner quest ids of this event
	 */
	public synchronized Scope open(Player player, QuestEvent event, Collection<Integer> questIds) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(questIds, "questIds");
		Map<Integer, QuestSnapshot> snapshots = new LinkedHashMap<>();
		for (int questId : questIds) {
			if (questId > 0 && !snapshots.containsKey(questId)) {
				QuestSnapshot snapshot = snapshotOf(player, questId);
				if (startEligibilityOwners.contains(questId)) {
					try {
						snapshot = snapshot.withStartEligibility(
							startEligibilityPort.snapshot(player.getObjectId(), questId));
					} catch (SQLException failure) {
						throw new IllegalStateException("failed to capture quest start eligibility for " + questId,
							failure);
					}
				}
				snapshot = PlayerQuestEventPort.enrich(snapshot, event);
				if (event instanceof QuestEvent.KillRanked ranked && ranked.facts() != null
					&& ranked.facts().recipientId() == player.getObjectId()) {
					snapshot = snapshot.withPvpFacts(ranked.facts());
				} else if (event instanceof QuestEvent.KillInWorld worldKill && worldKill.facts() != null
					&& worldKill.facts().recipientId() == player.getObjectId()) {
					snapshot = snapshot.withPvpFacts(worldKill.facts());
				}
				if (event instanceof QuestEvent.TalkToNpc talk) {
					snapshot = snapshot.withInteractionObjectId(talk.interactionObjectId());
				}
				snapshots.put(questId, snapshot);
			}
		}
		return new Scope(event, Map.copyOf(snapshots), current.get());
	}

	@Override
	public void record(QuestLegacyInvocation invocation) {
		Objects.requireNonNull(invocation, "invocation");
		Scope scope = current.get();
		if (scope != null) {
			scope.record(invocation);
		} else {
			store.record(invocation);
		}
	}

	/** All envelopes produced by closed scopes so far, in close order. */
	public synchronized List<QuestShadowBatchRunner.Envelope> envelopes() {
		return List.copyOf(envelopes);
	}

	/** Delegate the accumulated bindings to the batch runner for one stable report. */
	public QuestShadowBatchReport report(QuestShadowRunner runner, Set<Integer> expectedOwners) {
		Objects.requireNonNull(runner, "runner");
		return QuestShadowBatchRunner.compare(runner, envelopes(), expectedOwners);
	}

	/**
	 * Returns the current batch report and clears the accumulated bindings so
	 * the next batch starts fresh. This keeps capture usable on a long-running
	 * server without unbounded accumulation: a scheduled task can drain one
	 * batch, persist the report and continue capturing the next.
	 */
	public synchronized QuestShadowBatchReport drain(QuestShadowRunner runner, Set<Integer> expectedOwners) {
		QuestShadowBatchReport report = report(runner, expectedOwners);
		envelopes.clear();
		return report;
	}

	/** Discards an aborted installation's pending samples without producing migration evidence. */
	synchronized void discard() {
		envelopes.clear();
	}

	private synchronized void bind(Scope scope) {
		if (scope.invocations.isEmpty()) {
			return;
		}
		Map<Integer, QuestShadowObservation.Owner> owners = new LinkedHashMap<>();
		boolean consumed = false;
		for (QuestLegacyInvocation invocation : scope.invocations) {
			consumed |= invocation.observation().consumed();
			for (QuestShadowObservation.Owner owner : invocation.observation().owners().values()) {
				if (owners.putIfAbsent(owner.questId(), owner) != null) {
					throw new IllegalStateException("duplicate owner in one physical event: " + owner.questId());
				}
			}
		}
		envelopes.add(new QuestShadowBatchRunner.Envelope(scope.event, scope.snapshots,
			new QuestShadowObservation(owners, consumed), scope.invocations));
	}

	/**
	 * Frozen pre-event facts for one owner. Inventory counts and currency
	 * balances are projected read-only from the live player. A null storage or
	 * rank (for example a player being logged out) degrades to an empty map,
	 * the honest "not captured" value, never a fabricated zero balance.
	 */
	static QuestSnapshot snapshotOf(Player player, int questId) {
		QuestState state = player.getQuestStateList().getQuestState(questId);
		QuestStatus status = state == null ? QuestStatus.NONE : state.getStatus();
		int packed = state == null ? 0 : state.getQuestVars().getQuestVars();
		Storage inventory = player.getInventory();
		boolean inventoryCaptured = inventory != null;
		boolean currenciesCaptured = inventory != null || player.getCommonData() != null
			|| player.getAbyssRank() != null;
		var target = player.getTarget();
		boolean positionCaptured = player.getPosition() != null;
		return new QuestSnapshot(player.getObjectId(), questId, status, packed,
			inventoryCaptured ? inventoryOf(player) : null,
			currenciesCaptured ? currenciesOf(player) : null,
			inventoryCaptured, currenciesCaptured, 0, target == null ? 0 : target.getObjectId(),
			positionCaptured ? player.getWorldId() : 0,
			positionCaptured ? player.getInstanceId() : 0,
			positionCaptured ? player.getX() : 0f,
			positionCaptured ? player.getY() : 0f,
			positionCaptured ? player.getZ() : 0f,
			positionCaptured ? player.getHeading() : (byte) 0,
			craftFactsOf(player), null);
	}

	/** Captures only player-owned craft facts; static recipe templates remain owned by the runtime craft port. */
	static QuestCraftSnapshot craftFactsOf(Player player) {
		if (player.getRecipeList() == null || player.getSkillList() == null) {
			return null;
		}
		Map<Integer, Integer> skillLevels = new HashMap<>();
		for (var skill : player.getSkillList().getAllSkills()) {
			if (skill != null && player.getSkillList().isCraftSkill(skill.getSkillId())) {
				skillLevels.put(skill.getSkillId(), skill.getSkillLevel());
			}
		}
		return new QuestCraftSnapshot(player.getRecipeList().getRecipeList(), skillLevels, 1600,
			CraftConfig.MAX_EXPERT_CRAFTING_SKILLS, CraftConfig.MAX_MASTER_CRAFTING_SKILLS);
	}

	/** Read-only projection of the player's inventory: item id → total count. */
	static Map<Integer, Integer> inventoryOf(Player player) {
		Storage inventory = player.getInventory();
		if (inventory == null) {
			return Map.of();
		}
		return toInventoryMap(inventory.getItems());
	}

	/** Pure projection from storage items; null or non-positive entries are skipped. */
	static Map<Integer, Integer> toInventoryMap(List<Item> items) {
		if (items == null) {
			return Map.of();
		}
		Map<Integer, Integer> counts = new HashMap<>();
		for (Item item : items) {
			if (item != null && item.getItemId() > 0 && item.getItemCount() > 0) {
				counts.merge(item.getItemId(), (int) item.getItemCount(), Integer::sum);
			}
		}
		return Map.copyOf(counts);
	}

	/**
	 * Read-only projection of durable currency balances mapped to the reward
	 * kinds the candidate definitions actually reward. Balances that have no
	 * reliable accessor (for example CP) stay absent rather than guessed.
	 */
	static Map<QuestRewardKind, Long> currenciesOf(Player player) {
		Map<QuestRewardKind, Long> balances = new HashMap<>();
		Storage inventory = player.getInventory();
		if (inventory != null) {
			long kinah = inventory.getKinah();
			if (kinah > 0) {
				balances.put(QuestRewardKind.GOLD, kinah);
			}
		}
		if (player.getCommonData() != null) {
			int dp = player.getCommonData().getDp();
			if (dp > 0) {
				balances.put(QuestRewardKind.DP, (long) dp);
			}
		}
		if (player.getAbyssRank() != null) {
			int ap = player.getAbyssRank().getAp();
			if (ap > 0) {
				balances.put(QuestRewardKind.AP, (long) ap);
			}
			int gp = player.getAbyssRank().getGp();
			if (gp > 0) {
				balances.put(QuestRewardKind.GP, (long) gp);
			}
		}
		return Map.copyOf(balances);
	}

	/** One physical-event capture session; not thread-safe by design. */
	public final class Scope implements AutoCloseable {
		private final QuestEvent event;
		private final Map<Integer, QuestSnapshot> snapshots;
		private final Scope previous;
		private final List<QuestLegacyInvocation> invocations = new ArrayList<>();
		private boolean closed;

		private Scope(QuestEvent event, Map<Integer, QuestSnapshot> snapshots, Scope previous) {
			this.event = event;
			this.snapshots = snapshots;
			this.previous = previous;
			current.set(this);
		}

		private void record(QuestLegacyInvocation invocation) {
			invocations.add(invocation);
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}
			closed = true;
			if (previous == null) {
				current.remove();
			} else {
				current.set(previous);
			}
			try {
				bind(this);
			} catch (RuntimeException failure) {
				// 聚合/绑定失败降级为不采集,绝不影响旧路由或结果,但必须可诊断。
				log.warn(I18n.get("log.quest_engine.shadow_bind_failed", event.type(), invocations.size()), failure);
			}
		}
	}
}
