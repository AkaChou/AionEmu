package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.questEngine.definition.QuestMovieType;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;

/**
 * 真实 {@link QuestMoviePort}：提交后以请求的客户端资源类型发送 {@link SM_PLAY_MOVIE}。
 * Real {@link QuestMoviePort}: after commit, sends {@link SM_PLAY_MOVIE} with the requested client resource type.
 * The movie-end callback ({@code MovieEnd(movieId)}) is the authoritative client
 * event; this port never estimates movie duration with a local timer.
 */
public final class PlayerQuestMoviePort implements QuestMoviePort {
	@FunctionalInterface
	public interface MovieCall {
		boolean play(Player player, int movieId);
	}

	private final QuestPlayerPort players;
	@FunctionalInterface
	public interface TypedMovieCall {
		boolean play(Player player, int movieId, QuestMovieType type);
	}

	private final TypedMovieCall play;

	public PlayerQuestMoviePort(QuestPlayerPort players) {
		this(players, (player, movieId, type) -> {
			PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(type.wireValue(), movieId));
			return true;
		});
	}

	public PlayerQuestMoviePort(QuestPlayerPort players, MovieCall play) {
		this.players = Objects.requireNonNull(players, "players");
		MovieCall call = Objects.requireNonNull(play, "play");
		this.play = (player, movieId, type) -> call.play(player, movieId);
	}

	public PlayerQuestMoviePort(QuestPlayerPort players, TypedMovieCall play) {
		this.players = Objects.requireNonNull(players, "players");
		this.play = Objects.requireNonNull(play, "play");
	}

	@Override
	public boolean playMovie(QuestSnapshot snapshot, QuestMutationPlan plan, int movieId) {
		return playMovie(snapshot, plan, movieId, QuestMovieType.CUTSCENE);
	}

	@Override
	public boolean playMovie(QuestSnapshot snapshot, QuestMutationPlan plan, int movieId, QuestMovieType type) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(type, "type");
		if (movieId <= 0) {
			throw new IllegalArgumentException("movieId must be positive");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出：无可发送对象，best-effort 跳过。 / Commit succeeded but player logged out: nothing to send to, best-effort skip.
			return false;
		}
		return play.play(player, movieId, type);
	}
}
