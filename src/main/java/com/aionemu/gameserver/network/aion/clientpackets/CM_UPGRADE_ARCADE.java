package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.events.ArcadeUpgradeService;

/**
 * 客户端街机升级活动操作请求包（开启/关闭/尝试/领奖等）。
 * Client packet for arcade upgrade event actions (open/close/try/reward, etc.).
 *
 * @author Ranastic
 */
public class CM_UPGRADE_ARCADE extends AionClientPacket {
	private int action;
	@SuppressWarnings("unused")
	private int sessionId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_UPGRADE_ARCADE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		action = readC();
		sessionId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		switch (action) {
		case 0:
			GameFeatureServices.arcadeUpgradeService().startArcadeUpgrade(player);
			break;
		case 1:
			GameFeatureServices.arcadeUpgradeService().closeWindow(player);
			break;
		case 2:
			GameFeatureServices.arcadeUpgradeService().tryArcadeUpgrade(player);
			break;
		case 3:
			GameFeatureServices.arcadeUpgradeService().getReward(player);
			break;
		case 4:
			player.getUpgradeArcade().setReTry(true);
			GameFeatureServices.arcadeUpgradeService().tryArcadeUpgrade(player);
			break;
		case 5:
			GameFeatureServices.arcadeUpgradeService().showRewardList(player);
			break;
		default:
			break;
		}
	}
}
