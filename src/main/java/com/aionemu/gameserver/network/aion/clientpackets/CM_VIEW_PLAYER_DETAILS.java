package com.aionemu.gameserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.DeniedStatus;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_VIEW_PLAYER_DETAILS;

/**
 * 客户端查看其他玩家装备详情请求包。
 * Client packet for viewing another player's equipment details.
 *
 * @author Avol
 */
@Slf4j
public class CM_VIEW_PLAYER_DETAILS extends AionClientPacket {


	private int targetObjectId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_VIEW_PLAYER_DETAILS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		targetObjectId = readD();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = this.getConnection().getActivePlayer();
		VisibleObject obj = player.getKnownList().getObject(targetObjectId);
		if (obj == null) {
			// targetObjectId 可能为 0 / probably targetObjectId can be 0
			log.warn(I18n.get("log.6fc3165d4f7d", targetObjectId));
			return;
		}

		if (obj instanceof Player) {
			Player target = (Player) obj;

			if (!target.getPlayerSettings().isInDeniedStatus(DeniedStatus.VIEW_DETAILS)
					|| player.getAccessLevel() >= AdminConfig.ADMIN_VIEW_DETAILS)
				sendPacket(new SM_VIEW_PLAYER_DETAILS(target.getEquipment().getEquippedItemsWithoutStigma(), target));
			else {
				sendPacket(SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_WATCH(target.getName()));
				return;
			}
		}
	}
}
