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

	public SM_CUSTOM_SETTINGS(Player player) {
		this(player.getObjectId(), 1, player.getPlayerSettings().getDisplay(), player.getPlayerSettings().getDeny());
	}

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
