package com.aionemu.gameserver.model.gameobjects.player;

import java.util.Optional;

/**
 * 记录服务端发起的当前影片播放，并只接受一次匹配的客户端结束确认。
 * Tracks the current server-initiated movie playback and accepts one matching client completion.
 */
public final class MoviePlaybackAuthority {

	private long nextPlaybackId;
	private Playback active;

	/**
	 * 以服务端发送时间开始新的权威播放；新的影片替代客户端当前可结束的旧影片。
	 * Starts an authoritative playback at server send time; a new movie replaces the prior client-completable movie.
	 *
	 * @param movieId 影片标识 / movie identifier
	 * @param startedAt 服务端发送的 Unix 毫秒时间 / server-send Unix time in milliseconds
	 * @return 新的权威播放快照 / new authoritative playback snapshot
	 */
	public synchronized Playback begin(int movieId, long startedAt) {
		if (!isValidMovieId(movieId) || startedAt <= 0) {
			throw new IllegalArgumentException("Movie playback identity or start time is invalid");
		}
		nextPlaybackId = Math.incrementExact(nextPlaybackId);
		active = new Playback(nextPlaybackId, movieId, startedAt);
		return active;
	}

	/**
	 * 仅消费与当前服务端播放匹配且不早于发送时间的结束确认。
	 * Consumes a completion only when it matches the current server playback and is not earlier than its send time.
	 *
	 * @param movieId 客户端回报的影片标识 / client-reported movie identifier
	 * @param endedAt 服务端接收的 Unix 毫秒时间 / server-receive Unix time in milliseconds
	 * @return 已消费的权威播放，或拒绝时为空 / consumed authoritative playback, or empty when rejected
	 */
	public synchronized Optional<Playback> complete(int movieId, long endedAt) {
		if (active == null || active.movieId() != movieId || endedAt < active.startedAt()) {
			return Optional.empty();
		}
		Playback completed = active;
		active = null;
		return Optional.of(completed);
	}

	/**
	 * 清除会话内未完成的影片；登出、断线和会话替换后不得重放旧确认。
	 * Clears an unfinished session movie so logout, disconnect, or session replacement cannot replay an old completion.
	 */
	public synchronized void clear() {
		active = null;
	}

	/**
	 * 返回当前权威播放的只读快照。
	 * Returns the current authoritative playback snapshot.
	 *
	 * @return 当前播放，或无活动播放时为空 / current playback, or empty when none is active
	 */
	public synchronized Optional<Playback> active() {
		return Optional.ofNullable(active);
	}

	/** 判断影片标识能否无损写入当前协议的无符号 16 位字段。 / Returns whether a movie id fits the protocol unsigned 16-bit field. */
	public static boolean isValidMovieId(int movieId) {
		return movieId > 0 && movieId <= 0xFFFF;
	}

	/** 表示由服务端发送建立的不可变播放凭据。 / Represents immutable playback authority established by a server send. */
	public record Playback(long playbackId, int movieId, long startedAt) {
		/** 校验服务端播放凭据。 / Validates a server playback authority snapshot. */
		public Playback {
			if (playbackId <= 0 || !isValidMovieId(movieId) || startedAt <= 0) {
				throw new IllegalArgumentException("Movie playback snapshot is invalid");
			}
		}
	}
}
