package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;

/**
 * Real {@link QuestMoviePort}: after commit, sends {@code SM_PLAY_MOVIE(0, movieId)}.
 * The movie-end callback ({@code MovieEnd(movieId)}) is the authoritative client
 * event; this port never estimates movie duration with a local timer.
 */
public final class PlayerQuestMoviePort implements QuestMoviePort {
	@FunctionalInterface
	public interface MovieCall {
		boolean play(Player player, int movieId);
	}

	private final QuestPlayerPort players;
	private final MovieCall play;

	public PlayerQuestMoviePort(QuestPlayerPort players) {
		this(players, (player, movieId) -> {
			PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movieId));
			return true;
		});
	}

	public PlayerQuestMoviePort(QuestPlayerPort players, MovieCall play) {
		this.players = Objects.requireNonNull(players, "players");
		this.play = Objects.requireNonNull(play, "play");
	}

	@Override
	public boolean playMovie(QuestSnapshot snapshot, QuestMutationPlan plan, int movieId) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (movieId <= 0) {
			throw new IllegalArgumentException("movieId must be positive");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出:无可发送对象,best-effort 跳过。
			return false;
		}
		return play.play(player, movieId);
	}
}
