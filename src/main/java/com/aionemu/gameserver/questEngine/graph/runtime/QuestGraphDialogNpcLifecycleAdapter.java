package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DialogNpcLifecycleAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DialogNpcLifecycleMode;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;

/**
 * Executes the closed lifecycle operation for the exact NPC used by a dialog.
 *
 * <p>The dialog event carries only immutable identity.  This adapter resolves
 * that identity again at preflight and execution time; a template match or an
 * NPC found by name is never sufficient.  Production callers must provide a
 * durable ledger so a crash cannot cause the same lifecycle operation to be
 * accepted twice.</p>
 */
public final class QuestGraphDialogNpcLifecycleAdapter {

	private final int playerId;
	private final Function<DialogNpcLifecycleCommand, PreflightResult> preflightEndpoint;
	private final Function<DialogNpcLifecycleCommand, ActionResult> endpoint;
	private final Function<DialogNpcLifecycleCommand, ActionResult> retryEndpoint;
	private final LifecycleLedger ledger;
	private final Set<String> acceptedKeys = new HashSet<>();

	/** Creates the production adapter with explicit durable ownership and retry ports. */
	public QuestGraphDialogNpcLifecycleAdapter(Player player, LifecycleLedger ledger,
			Function<DialogNpcLifecycleCommand, ActionResult> retryEndpoint) {
		this(requirePlayer(player).getObjectId(), command -> preflightNpc(player, command),
			command -> executeNpc(player, command), retryEndpoint, ledger);
	}

	/** Creates an injectable adapter for focused runtime composition and deterministic verification. */
	QuestGraphDialogNpcLifecycleAdapter(int playerId,
			Function<DialogNpcLifecycleCommand, PreflightResult> preflightEndpoint,
			Function<DialogNpcLifecycleCommand, ActionResult> endpoint,
			Function<DialogNpcLifecycleCommand, ActionResult> retryEndpoint, LifecycleLedger ledger) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Dialog NPC lifecycle player id is invalid");
		}
		this.playerId = playerId;
		this.preflightEndpoint = Objects.requireNonNull(preflightEndpoint, "dialog NPC lifecycle preflight");
		this.endpoint = Objects.requireNonNull(endpoint, "dialog NPC lifecycle endpoint");
		this.retryEndpoint = Objects.requireNonNull(retryEndpoint, "dialog NPC lifecycle retry");
		this.ledger = Objects.requireNonNull(ledger, "dialog NPC lifecycle ledger");
	}

	/** Validates the immutable dialog identity and the live NPC before PREPARED. */
	public synchronized PreflightResult preflight(ActionInvocation invocation) {
		try {
			if (!validOwner(invocation)) {
				return PreflightResult.FAILED;
			}
			return Objects.requireNonNull(preflightEndpoint.apply(command(invocation)), "dialog NPC lifecycle preflight result");
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
	}

	/** Executes exactly once, with durable claim before the world mutation and acknowledgement after it. */
	public synchronized ActionResult execute(ActionInvocation invocation) {
		try {
			if (!validOwner(invocation)) {
				return ActionResult.FAILED;
			}
			DialogNpcLifecycleCommand command = command(invocation);
			if (acceptedKeys.contains(command.idempotencyKey())) {
				return ActionResult.ALREADY_APPLIED;
			}
			Claim claim = Objects.requireNonNull(ledger.claim(command), "dialog NPC lifecycle claim");
			if (claim == Claim.REJECTED) {
				return ActionResult.FAILED;
			}
			if (claim == Claim.ALREADY_APPLIED) {
				acceptedKeys.add(command.idempotencyKey());
				return ActionResult.ALREADY_APPLIED;
			}
			ActionResult direct;
			try {
				direct = Objects.requireNonNull(endpoint.apply(command), "dialog NPC lifecycle endpoint result");
			} catch (RuntimeException e) {
				direct = ActionResult.FAILED;
			}
			ActionResult result = accepted(direct) ? direct : endpointResult(retryEndpoint, command);
			if (!accepted(result)) {
				return ActionResult.FAILED;
			}
			Claim acknowledgement = Objects.requireNonNull(ledger.acknowledge(command), "dialog NPC lifecycle acknowledgement");
			if (acknowledgement != Claim.APPLIED && acknowledgement != Claim.ALREADY_APPLIED) {
				return ActionResult.FAILED;
			}
			acceptedKeys.add(command.idempotencyKey());
			return result;
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	/** Clears only process-local de-duplication; durable ownership remains in the ledger. */
	public synchronized void clearSession() {
		acceptedKeys.clear();
	}

	private static ActionResult endpointResult(Function<DialogNpcLifecycleCommand, ActionResult> endpoint,
			DialogNpcLifecycleCommand command) {
		try {
			return Objects.requireNonNull(endpoint.apply(command), "dialog NPC lifecycle endpoint result");
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	private boolean validOwner(ActionInvocation invocation) {
		return invocation != null && invocation.event().playerId() == playerId
			&& invocation.action() instanceof DialogNpcLifecycleAction;
	}

	private static DialogNpcLifecycleCommand command(ActionInvocation invocation) {
		if (!(invocation.action() instanceof DialogNpcLifecycleAction action)
				|| !(invocation.event() instanceof QuestGraphEvent.DialogEvent dialog)
				|| dialog.npcId() <= 0 || dialog.npcObjectId() <= 0) {
			throw new IllegalArgumentException("Dialog NPC lifecycle requires an exact NPC dialog snapshot");
		}
		return new DialogNpcLifecycleCommand(invocation.questId(), invocation.event().playerId(), dialog.npcId(), dialog.npcObjectId(),
			action.mode(), invocation.idempotencyKey());
	}

	private static PreflightResult preflightNpc(Player player, DialogNpcLifecycleCommand command) {
		Npc npc = resolveNpc(player, command);
		if (npc == null || npc.getSpawn() == null) {
			return PreflightResult.FAILED;
		}
		if (command.mode() == DialogNpcLifecycleMode.SCHEDULE_RESPAWN_THEN_DELETE && npc.getSpawn().isNoRespawn()) {
			return PreflightResult.REJECTED;
		}
		return PreflightResult.READY;
	}

	private static ActionResult executeNpc(Player player, DialogNpcLifecycleCommand command) {
		Npc npc = resolveNpc(player, command);
		if (npc == null || npc.getSpawn() == null) {
			return ActionResult.FAILED;
		}
		if (command.mode() == DialogNpcLifecycleMode.SCHEDULE_RESPAWN_THEN_DELETE
				&& npc.getController().scheduleRespawn() == null) {
			return ActionResult.FAILED;
		}
		npc.getController().onDelete();
		return ActionResult.APPLIED;
	}

	private static Npc resolveNpc(Player player, DialogNpcLifecycleCommand command) {
		if (player == null || player.getObjectId() != command.playerId() || !player.isSpawned()
				|| !(player.getKnownList().getObject(command.npcObjectId()) instanceof Npc npc)
				|| npc.getObjectId() != command.npcObjectId() || npc.getNpcId() != command.npcId() || !npc.isSpawned()
				|| npc.getWorldId() != player.getWorldId() || npc.getInstanceId() != player.getInstanceId()
				|| !npc.isInWorld()) {
			return null;
		}
		return npc;
	}

	private static boolean accepted(ActionResult result) {
		return result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED;
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	/** Durable claim/acknowledgement boundary for one immutable lifecycle command. */
	public interface LifecycleLedger {
		Claim claim(DialogNpcLifecycleCommand command);

		Claim acknowledge(DialogNpcLifecycleCommand command);
	}

	public enum Claim {
		APPLIED,
		ALREADY_APPLIED,
		REJECTED
	}

	/** Immutable identity passed to the lifecycle endpoint; it contains no game-object reference. */
	public record DialogNpcLifecycleCommand(int questId, int playerId, int npcId, int npcObjectId,
			DialogNpcLifecycleMode mode, String idempotencyKey) {
		public DialogNpcLifecycleCommand {
			if (questId <= 0 || playerId <= 0 || npcId <= 0 || npcObjectId <= 0 || mode == null
					|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Dialog NPC lifecycle command is invalid");
			}
		}
	}
}
