package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 玩家自定义显示/拒绝设置同步包。
 * Server packet syncing a player's custom display and deny settings.
 *
 * @author Sweetkr
 */
public class SM_CUSTOM_SETTINGS extends AionServerPacket {

	private Integer obj;
	private int unk = 0;
	private int display;
	private int deny;

	/**
	 * 按玩家自定义显示/拒绝设置构造同步包。
	 * Creates a sync packet from the player's custom display/deny settings.
	 *
	 * @param player 目标玩家 / target player
	 */
	public SM_CUSTOM_SETTINGS(Player player) {
		this(player.getObjectId(), 1, player.getPlayerSettings().getDisplay(), player.getPlayerSettings().getDeny());
	}

	/**
	 * 按原始值构造自定义设置同步包。
	 * Creates a custom settings sync packet from raw values.
	 *
	 * @param objectId 目标对象 ID / target object id
	 * @param unk 未知字段 / unknown field
	 * @param display 显示设置 / display settings
	 * @param deny 拒绝设置 / deny settings
	 */
	public SM_CUSTOM_SETTINGS(int objectId, int unk, int display, int deny) {
		obj = objectId;
		this.display = display;
		this.deny = deny;
		this.unk = unk;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(obj);
		writeC(unk); // 未知 / unk
		writeH(display);
		writeH(deny);
	}
}
