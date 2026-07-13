package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 客户端过场动画播放结束通知包，驱动任务与副本回调。
 * Client packet notifying that a cutscene/movie finished; triggers quest and instance handlers.
 */
public class CM_PLAY_MOVIE_END extends AionClientPacket {
	@SuppressWarnings("unused")
	private int type;
	@SuppressWarnings("unused")
	private int targetObjectId;
	@SuppressWarnings("unused")
	private int dialogId;
	private int movieId;
	@SuppressWarnings("unused")
	private int unk;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_PLAY_MOVIE_END(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		type = readC();
		targetObjectId = readD();
		dialogId = readD();
		movieId = readH();
		unk = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		notifyRetailAi(player, targetObjectId, movieId);
		GameEngineServices.questEngine().onMovieEnd(new QuestEnv(null, player, 0, 0), movieId);
		player.getPosition().getWorldMapInstance().getInstanceHandler().onPlayMovieEnd(player, movieId);
	}

	static void notifyRetailAi(Player player, int targetObjectId, int movieId) {
		if (player.getKnownList().getObject(targetObjectId) instanceof Npc npc) {
			npc.getAi2().onQuitCutscene(player, movieId);
		}
	}
}
