package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamPath;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamRoute;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRANSFORM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WINDSTREAM;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端风流/气流路径状态同步请求包（进入、加速、退出等）。
 * Client packet syncing windstream path state (enter, boost, exit, etc.).
 */
public class CM_WINDSTREAM extends AionClientPacket {
	int teleportId;
	int distance;
	int state;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_WINDSTREAM(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		teleportId = readD();
		distance = readD();
		state = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		switch (state) {
		case 0:
			player.unsetPlayerMode(PlayerMode.RIDE);
			PacketSendUtility.sendPacket(player, new SM_WINDSTREAM(state, 1));
			break;
		case 1:
			if (player.isUsingFlyTeleport() || player.isInPlayerMode(PlayerMode.WINDSTREAM) || !player.isFlying()) {
				return;
			}
			WindstreamRoute route = DataManager.WINDSTREAM_DATA.getRoute(player.getPosition().getMapId(), teleportId);
			if (route == null) {
				return;
			}
			player.setPlayerMode(PlayerMode.WINDSTREAM, new WindstreamPath(route, teleportId, distance));
			if (player.isGM()) {
				PacketSendUtility.sendMessage(player,
						"You enter teleportId: " + teleportId + ", distance: " + distance);
			}
			player.unsetState(CreatureState.ACTIVE);
			player.unsetState(CreatureState.GLIDING);
			player.setState(CreatureState.FLYING);
			player.setFlyState(1);
			PacketSendUtility.broadcastPacket(player,
					new SM_EMOTION(player, EmotionType.WINDSTREAM, teleportId, distance), true);
			player.getLifeStats().triggerFpRestore();
			GameEngineServices.questEngine().onEnterWindStream(new QuestEnv(null, player, 0, 0), teleportId);
			break;
		case 7:
		case 8:
			PacketSendUtility.broadcastPacket(player,
				new SM_EMOTION(player, state == 7 ? EmotionType.WINDSTREAM_START_BOOST : EmotionType.WINDSTREAM_END_BOOST, 0, 0), true);
			PacketSendUtility.sendPacket(player, new SM_WINDSTREAM(state, 1));
			break;
		case 2:
		case 3:
			if (!player.isInPlayerMode(PlayerMode.WINDSTREAM)) {
				return;
			}
			player.unsetState(CreatureState.FLYING);
			player.setState(CreatureState.ACTIVE);
			if (state == 2) {
				player.setState(CreatureState.GLIDING);
				player.getLifeStats().triggerFpReduce();
			}
			if (player.isTransformed()) {
				player.setState(CreatureState.GLIDING);
				player.getLifeStats().triggerFpReduce();

				PacketSendUtility.broadcastPacketAndReceive(player,
						new SM_TRANSFORM(player, player.getTransformedModelId(), true, player.getTransformedItemId()));
				PacketSendUtility.broadcastPacketAndReceive(player, new SM_TRANSFORM(player, true));
				player.getEffectController().updatePlayerEffectIcons();
			}
			PacketSendUtility.broadcastPacket(player,
					new SM_EMOTION(player, state == 2 ? EmotionType.WINDSTREAM_END : EmotionType.WINDSTREAM_EXIT, 0, 0),
					true);
			player.setFlyState(player.isInState(CreatureState.GLIDING) ? 2 : 0);
			player.getGameStats().updateStatsAndSpeedVisually();
			PacketSendUtility.sendPacket(player, new SM_WINDSTREAM(state, 1));
			player.unsetPlayerMode(PlayerMode.WINDSTREAM);
			break;
		case 4:
			PacketSendUtility.sendPacket(player, new SM_WINDSTREAM(state, 1));
			break;
		}
	}
}
