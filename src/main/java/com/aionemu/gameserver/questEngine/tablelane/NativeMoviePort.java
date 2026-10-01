package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.questEngine.definition.QuestMovieType;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 原生任务车道的过场端口：真端表 {@code cutsceneid1}/{@code cs1_haction} 声明的 movie 唯一出口。
 * <p>
 * 真端 codegen 在交付/报告节点的槽 0x35 挂 {@code PlayMovie} thunk；本端口是它在 Java 侧的等价物
 * （包类型取自 {@link QuestMovieType#CUTSCENE}，与旧 IR 的 {@code AfterCommitAction.PlayMovie} 同型）。
 * The cutscene port of the native quest lane: the single exit for movies declared by the retail table
 * ({@code cutsceneid1}/{@code cs1_haction}), the Java-side equivalent of the retail 0x35 slot thunk.
 */
public interface NativeMoviePort {

	/** 播放过场（真端 PlayMovie）。 / Plays the cutscene (retail PlayMovie). */
	void play(Player player, int movieId);

	/**
	 * 播放电影型资源（真端 `PlayMovie` 独立槽 +0x1b8，对应 DD 附加动作 `Movie|Movie2 N` 词形；
	 * 与 {@link #play} 的差异只在客户端资源包型）。
	 * Plays a movie-type resource (the retail PlayMovie slot +0x1b8, the DD extra-action
	 * {@code Movie|Movie2 N} token); differs from {@link #play} only in the client resource
	 * packet type.
	 */
	default void playMovie(Player player, int movieId) {
		play(player, movieId);
	}

	/** 生产实现（客户端过场资源类型）。 / The live implementation (client cutscene resource type). */
	static NativeMoviePort live() {
		return Live.INSTANCE;
	}

	/** 生产实现持有者。 / Live implementation holder. */
	final class Live implements NativeMoviePort {

		private static final NativeMoviePort INSTANCE = new Live();

		private Live() {
		}

		@Override
		public void play(Player player, int movieId) {
			PacketSendUtility.sendPacket(player,
				new SM_PLAY_MOVIE(QuestMovieType.CUTSCENE.wireValue(), movieId));
		}

		@Override
		public void playMovie(Player player, int movieId) {
			PacketSendUtility.sendPacket(player,
				new SM_PLAY_MOVIE(QuestMovieType.CUTSCENE_MOVIE.wireValue(), movieId));
		}
	}
}
