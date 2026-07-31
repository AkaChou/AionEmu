package com.aionemu.gameserver.questEngine.graph.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.QuestGraphZoneMissionSignalDAO;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Propagation;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ZoneMissionEndedEvent;

/**
 * 将一个 zone-mission 完成事实按显式目标集合投递到多个 graph owner。
 *
 * <p>Legacy {@code onEnterZoneMissionEnd(int[])} may fan out to several quest
 * owners.  A graph event is owner-scoped, therefore the bridge creates one
 * deterministic event per target instead of pretending that one owner can
 * consume another owner's callback.  The supplied ledger is the durable
 * acceptance boundary; an in-memory implementation is deliberately not
 * provided for production construction.</p>
 */
public final class QuestGraphZoneMissionSignalBridge {

	private final SignalLedger ledger;

	public QuestGraphZoneMissionSignalBridge(SignalLedger ledger) {
		this.ledger = Objects.requireNonNull(ledger, "zone mission signal ledger");
	}

	/** Creates the production bridge backed by the durable database ledger. */
	public static QuestGraphZoneMissionSignalBridge production() {
		QuestGraphZoneMissionSignalDAO dao = DAOManager.getDAO(QuestGraphZoneMissionSignalDAO.class);
		return new QuestGraphZoneMissionSignalBridge(new SignalLedger() {
			@Override
			public SignalLease accept(Signal signal) {
				return dao.accept(signal);
			}

			@Override
			public SignalClaim acknowledge(Signal signal, long claimGeneration) {
				return dao.acknowledge(signal, claimGeneration);
			}
		});
	}

	/**
	 * Delivers one source fact to an explicitly enumerated, de-duplicated target
	 * set.  The dispatcher must route the supplied typed event only to the
	 * target owner and return its normal graph status.
	 */
	public DispatchResult dispatch(int playerId, long occurredAt, int sourceQuestId, String sourceEventId,
			Collection<Integer> targetQuestIds, Function<ZoneMissionEndedEvent, DispatchResult> dispatcher) {
		if (playerId <= 0 || occurredAt <= 0 || sourceQuestId <= 0 || sourceEventId == null || sourceEventId.isBlank()
				|| dispatcher == null || targetQuestIds == null) {
			return new DispatchResult(Status.FAILED, Propagation.STOP);
		}
		List<Integer> targets = canonicalTargets(targetQuestIds);
		if (targets.isEmpty()) {
			return new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE);
		}
		Status aggregate = Status.NO_MATCH;
		for (int targetQuestId : targets) {
			Signal signal;
			try {
				signal = new Signal(eventId(sourceEventId, playerId, sourceQuestId, targetQuestId), playerId, occurredAt,
					sourceQuestId, targetQuestId);
			} catch (RuntimeException e) {
				return new DispatchResult(Status.FAILED, Propagation.STOP);
			}
			SignalLease lease;
			try {
				lease = Objects.requireNonNull(ledger.accept(signal), "zone mission signal claim");
			} catch (RuntimeException e) {
				return new DispatchResult(Status.FAILED, Propagation.STOP);
			}
			if (lease.claim() == SignalClaim.REJECTED || lease.claim() == SignalClaim.BUSY) {
				return new DispatchResult(Status.FAILED, Propagation.STOP);
			}
			if (lease.claim() == SignalClaim.ALREADY_APPLIED) {
				aggregate = moreSevere(aggregate, Status.APPLIED);
				continue;
			}
			DispatchResult result;
			try {
				result = Objects.requireNonNull(dispatcher.apply(new ZoneMissionEndedEvent(signal.eventId(), playerId, occurredAt,
					targetQuestId)), "zone mission dispatch result");
			} catch (RuntimeException e) {
				return new DispatchResult(Status.FAILED, Propagation.STOP);
			}
			Status status = result.status();
			if (status == Status.FAILED) {
				return new DispatchResult(Status.FAILED, Propagation.STOP);
			}
			if (status == Status.REJECTED) {
				return new DispatchResult(Status.REJECTED, Propagation.CONTINUE);
			}
			aggregate = moreSevere(aggregate, status == Status.NO_MATCH ? Status.NO_MATCH : Status.APPLIED);
			try {
				SignalClaim acknowledgement = ledger.acknowledge(signal, lease.claimGeneration());
				if (acknowledgement != SignalClaim.APPLIED && acknowledgement != SignalClaim.ALREADY_APPLIED) {
					return new DispatchResult(Status.FAILED, Propagation.STOP);
				}
			} catch (RuntimeException e) {
				return new DispatchResult(Status.FAILED, Propagation.STOP);
			}
		}
		return new DispatchResult(aggregate, aggregate == Status.FAILED ? Propagation.STOP : Propagation.CONTINUE);
	}

	private static List<Integer> canonicalTargets(Collection<Integer> targetQuestIds) {
		LinkedHashSet<Integer> unique = new LinkedHashSet<>();
		for (Integer target : targetQuestIds) {
			if (target == null || target <= 0) {
				throw new IllegalArgumentException("Zone mission target quest id is invalid");
			}
			unique.add(target);
		}
		List<Integer> result = new ArrayList<>(unique);
		result.sort(Comparator.naturalOrder());
		return List.copyOf(result);
	}

	private static Status moreSevere(Status current, Status candidate) {
		return severity(candidate) > severity(current) ? candidate : current;
	}

	private static int severity(Status status) {
		return switch (status) {
			case NO_MATCH -> 0;
			case REJECTED -> 1;
			case APPLIED -> 2;
			case FAILED -> 3;
		};
	}

	private static String eventId(String sourceEventId, int playerId, int sourceQuestId, int targetQuestId) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest((sourceEventId + ':' + playerId + ':' + sourceQuestId + ':' + targetQuestId)
				.getBytes(StandardCharsets.UTF_8));
			StringBuilder value = new StringBuilder("zone-mission-");
			for (byte item : bytes) {
				value.append(String.format("%02x", item));
			}
			return value.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	/** Durable acceptance and acknowledgement boundary for one target signal. */
	public interface SignalLedger {
		SignalLease accept(Signal signal);

		SignalClaim acknowledge(Signal signal, long claimGeneration);
	}

	/** Stable idempotency result returned by the durable signal ledger. */
	public enum SignalClaim {
		APPLIED,
		ALREADY_APPLIED,
		BUSY,
		REJECTED
	}

	/** Atomic claim result; only APPLIED owns a positive generation and may acknowledge it. */
	public record SignalLease(SignalClaim claim, long claimGeneration) {
		public SignalLease {
			if (claim == null || (claim == SignalClaim.APPLIED) != (claimGeneration > 0)) {
				throw new IllegalArgumentException("Zone mission signal lease is invalid");
			}
		}

		public static SignalLease applied(long claimGeneration) {
			return new SignalLease(SignalClaim.APPLIED, claimGeneration);
		}

		public static SignalLease of(SignalClaim claim) {
			return new SignalLease(claim, 0);
		}
	}

	/** Immutable cross-owner signal identity persisted by the ledger. */
	public record Signal(String eventId, int playerId, long occurredAt, int sourceQuestId, int targetQuestId) {
		public Signal {
			if (eventId == null || eventId.isBlank() || playerId <= 0 || occurredAt <= 0 || sourceQuestId <= 0 || targetQuestId <= 0) {
				throw new IllegalArgumentException("Zone mission signal identity is invalid");
			}
		}
	}
}
