package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.model.gameobjects.player.MoviePlaybackAuthority;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 向客户端播放过场/影片（CutScene 或 CutSceneMovie）。
 * Server packet that plays a cutscene or cutscene movie on the client.
 *
 * @author -orz-, MrPoke
 */
public class SM_PLAY_MOVIE extends AionServerPacket {

	private int type = 1; // if 1: CutSceneMovies else CutScenes
	private int movieId = 0;
	private int id = 0; // id scene ?
	private int restrictionId;
	private int objectId;

	/**
	 * 使用给定参数构造 SM_PLAY_MOVIE 包。
	 * Creates a SM_PLAY_MOVIE packet with the given parameters.
	 *
	 * type
	 * movie id
	 */
	public SM_PLAY_MOVIE(int type, int movieId) {
		this.type = type;
		this.movieId = movieId;
	}

	/**
	 * 使用给定参数构造 SM_PLAY_MOVIE 包。
	 * Creates a SM_PLAY_MOVIE packet with the given parameters.
	 *
	 * type
	 * @param id 场景/标识 ID / scene or id
	 * movie id
	 * restriction id
	 */
	public SM_PLAY_MOVIE(int type, int id, int movieId, int restrictionId) {
		this(type, movieId);
		this.id = id;
		this.restrictionId = restrictionId;
	}

	/**
	 * 使用给定参数构造 SM_PLAY_MOVIE 包。
	 * Creates a SM_PLAY_MOVIE packet with the given parameters.
	 *
	 * type
	 * @param id 场景/标识 ID / scene or id
	 * movie id
	 * restriction id
	 * object id
	 */
	public SM_PLAY_MOVIE(int type, int id, int movieId, int restrictionId, int objectId) {
		this(type, id, movieId, restrictionId);
		this.objectId = objectId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		if (player != null && MoviePlaybackAuthority.isValidMovieId(movieId)) {
			player.getMoviePlaybackAuthority().begin(movieId, System.currentTimeMillis());
		}
		writeC(type);
		writeD(objectId);
		writeD(id);
		writeH(movieId);
		writeD(restrictionId);
	}
}
