package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.PostboxObject;
import com.aionemu.gameserver.model.gameobjects.StorageObject;
import com.aionemu.gameserver.model.gameobjects.UseableItemObject;
import com.aionemu.gameserver.model.templates.housing.UseItemAction;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 房屋物件使用更新服务端包。
 * Server packet that notifies the client of a house-object use update.
 * <p>
 * 按物件类型（邮箱、仓库、可使用道具）写入不同字段。
 * Payload fields differ by object type (postbox, storage, usable item).
 */
public class SM_OBJECT_USE_UPDATE extends AionServerPacket {
	private int usingPlayerId;
	private int ownerPlayerId;
	private int useCount;
	private UseItemAction action = null;
	HouseObject<?> object;

	/**
	 * 构造房屋物件使用更新包。
	 * Builds a house-object use-update packet.
	 *
	 * @param usingPlayerId 使用者玩家 ID / using player id
	 * @param ownerPlayerId 所有者玩家 ID / owner player id
	 * use count
	 * house object being used
	 */
	public SM_OBJECT_USE_UPDATE(int usingPlayerId, int ownerPlayerId, int useCount, HouseObject<?> object) {
		this.usingPlayerId = usingPlayerId;
		this.ownerPlayerId = ownerPlayerId;
		this.useCount = useCount;
		this.object = object;
		if (object instanceof UseableItemObject) {
			this.action = ((UseableItemObject) object).getObjectTemplate().getAction();
		}
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(object.getObjectTemplate().getTypeId());
		if (object instanceof PostboxObject || object instanceof StorageObject) {
			writeD(usingPlayerId);
			writeC(1);
			writeD(object.getObjectId());
		} else if (object instanceof UseableItemObject) {
			writeD(usingPlayerId);
			writeD(ownerPlayerId);
			writeD(object.getObjectId());
			writeD(useCount);
			int checkType = 0;
			if (action != null && action.getCheckType() != null) {
				checkType = action.getCheckType();
			}
			writeC(checkType);
		}
	}
}
