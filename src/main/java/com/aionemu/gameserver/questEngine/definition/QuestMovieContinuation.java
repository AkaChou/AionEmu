package com.aionemu.gameserver.questEngine.definition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * MOVIE_CONTINUATION_RESPONSE 规则的共享实现，供编译器和测试门禁使用。
 * Shared implementation of the MOVIE_CONTINUATION_RESPONSE rule for the compiler and test gates.
 */
public final class QuestMovieContinuation {
	private QuestMovieContinuation() {
	}

	public static List<Violation> violations(QuestDefinition definition, QuestDialogContract contract) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(contract, "contract");
		if (contract.isEmpty()) {
			return List.of();
		}
		List<Violation> result = new ArrayList<>();
		for (QuestTransition transition : definition.transitions()) {
			if (!(transition.event() instanceof QuestEvent.TalkToNpc talk) || talk.dialogId() == null) {
				continue;
			}
			if (transition.sourceNode() == null || !transition.sourceNode().equals(transition.targetNode())) {
				continue;
			}
			if (!contract.hasButtonPage(definition.id(), talk.dialogId())) {
				continue;
			}
			Set<Integer> movieIds = movieIds(transition);
			if (movieIds.isEmpty()) {
				continue;
			}
			if (transition.afterCommit().stream().anyMatch(QuestMovieContinuation::continuesDialog)) {
				continue;
			}
			if (hasMovieEndRoute(definition, transition.sourceNode(), movieIds)) {
				continue;
			}
			result.add(new Violation(definition.id(), transition.sourceNode(), talk.dialogId(),
				Set.copyOf(movieIds)));
		}
		return List.copyOf(result);
	}

	public static Set<Integer> movieIds(QuestTransition transition) {
		Set<Integer> result = new HashSet<>();
		for (AfterCommitAction action : transition.afterCommit()) {
			if (action instanceof AfterCommitAction.PlayMovie movie) {
				result.add(movie.movieId());
			} else if (action instanceof AfterCommitAction.PlayMovieRandom random) {
				result.addAll(random.movieIds());
			}
		}
		return Set.copyOf(result);
	}

	public static boolean continuesDialog(AfterCommitAction action) {
		return action instanceof AfterCommitAction.ShowQuestDialog
			|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
			|| action instanceof AfterCommitAction.ShowDialogWindow
			|| action instanceof AfterCommitAction.CloseDialog
			|| action instanceof AfterCommitAction.TeleportPlayer
			|| action instanceof AfterCommitAction.FlightTeleport;
	}

	public static boolean hasMovieEndRoute(QuestDefinition definition, String sourceNode, Set<Integer> movieIds) {
		return definition.transitions().stream()
			.filter(candidate -> Objects.equals(candidate.sourceNode(), sourceNode))
			.filter(candidate -> candidate.event() instanceof QuestEvent.MovieEnd end
				&& movieIds.contains(end.movieId()))
			.anyMatch(candidate -> !candidate.afterCommit().isEmpty());
	}

	public record Violation(int questId, String sourceNode, int dialogId, Set<Integer> movieIds) {
		public Violation {
			if (questId <= 0) {
				throw new IllegalArgumentException("questId must be positive");
			}
			sourceNode = Objects.requireNonNull(sourceNode, "sourceNode");
			if (dialogId <= 0) {
				throw new IllegalArgumentException("dialogId must be positive");
			}
			movieIds = Set.copyOf(movieIds);
		}
	}
}
