package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.services.events.ArcadeUpgradeService;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端街机升级活动操作请求包（开启/关闭/尝试/领奖等）。
 * Client packet for arcade upgrade event actions (open/close/try/reward, etc.).
 * @author Ranastic
 */
public class CM_UPGRADE_ARCADE extends AionClientPacket {
	private int action;

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
		readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		switch (action) {
		case 0:
			ArcadeUpgradeService.getInstance().startArcadeUpgrade(player);
			break;
		case 1:
			ArcadeUpgradeService.getInstance().closeWindow(player);
			break;
		case 2:
			ArcadeUpgradeService.getInstance().tryArcadeUpgrade(player);
			break;
		case 3:
			ArcadeUpgradeService.getInstance().getReward(player);
			break;
		case 4:
			player.getUpgradeArcade().setReTry(true);
			ArcadeUpgradeService.getInstance().tryArcadeUpgrade(player);
			break;
		case 5:
			ArcadeUpgradeService.getInstance().showRewardList(player);
			break;
		default:
			break;
		}
	}
}
