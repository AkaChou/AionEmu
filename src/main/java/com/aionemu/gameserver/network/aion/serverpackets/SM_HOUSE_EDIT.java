package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.HouseDecoration;
import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.UseableItemObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端房屋编辑操作结果的服务端包（添加、移除、放置、删除等）。
 * Server packet that notifies the client of house edit action results (add, remove, place, delete, etc.).
 */
public class SM_HOUSE_EDIT extends AionServerPacket {
	private int action;
	private int storeId;
	private int itemObjectId;
	private float x, y, z;
	private int rotation;

	/**
	 * 构造仅含动作类型的房屋编辑包。
	 * Creates a house edit packet with action type only.
	 *
	 * @param action 编辑动作类型 / edit action type
	 */
	public SM_HOUSE_EDIT(int action) {
		this.action = action;
	}

	/**
	 * 构造含仓库与物品对象 ID 的房屋编辑包（添加/移除等）。
	 * Creates a house edit packet with store and item object ids (add/remove, etc.).
	 *
	 * @param action 编辑动作类型 / edit action type
	 * store id
	 * item object id
	 */
	public SM_HOUSE_EDIT(int action, int storeId, int itemObjectId) {
		this(action);
		this.itemObjectId = itemObjectId;
		this.storeId = storeId;
	}

	/**
	 * 构造含放置坐标与旋转的房屋编辑包。
	 * Creates a house edit packet with placement coordinates and rotation.
	 *
	 * @param action 编辑动作类型 / edit action type
	 * item object id
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 * rotation
	 */
	public SM_HOUSE_EDIT(int action, int itemObjectId, float x, float y, float z, int rotation) {
		this.action = action;
		this.itemObjectId = itemObjectId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.rotation = rotation;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		if (player == null || player.getHouseRegistry() == null) {
			return;
		}
		HouseObject<?> obj = player.getHouseRegistry().getObjectByObjId(itemObjectId);
		if (action == 3) {
			int templateId = 0;
			int typeId = 0;
			if (obj == null) {
				HouseDecoration deco = player.getHouseRegistry().getCustomPartByObjId(itemObjectId);
				templateId = deco.getTemplate().getId();
			} else {
				templateId = obj.getObjectTemplate().getTemplateId();
				typeId = obj.getObjectTemplate().getTypeId();
			}
			writeC(action);
			writeC(storeId);
			writeD(itemObjectId);
			writeD(templateId);
			if (obj != null && obj.getUseSecondsLeft() > 0) {
				writeD(obj.getUseSecondsLeft());
			} else {
				writeD(0);
			}
			Integer color = null;
			if (obj != null) {
				color = obj.getColor();
			}
			if (color != null && color > 0) {
				writeC(1);
				writeC((color & 0xFF0000) >> 16);
				writeC((color & 0xFF00) >> 8);
				writeC((color & 0xFF));
			} else {
				writeC(0);
				writeC(0);
				writeC(0);
				writeC(0);
			}
			writeD(0);
			writeC(typeId);
			if (obj != null && obj instanceof UseableItemObject) {
				writeD(player.getObjectId());
				((UseableItemObject) obj).writeUsageData(getBuf());
			}
		} else if (action == 4) {
			writeC(action);
			writeC(storeId);
			writeD(itemObjectId);
		} else if (action == 5) {
			writeC(action);
			writeD(player.getHouseOwnerId());
			writeD(player.getCommonData().getPlayerObjId());
			writeD(itemObjectId);
			writeD(obj.getObjectTemplate().getTemplateId());
			writeF(x);
			writeF(y);
			writeF(z);
			writeH(rotation);
			writeD(player.getHouseObjectCooldownList().getReuseDelay(itemObjectId));
			if (obj.getUseSecondsLeft() > 0) {
				writeD(obj.getUseSecondsLeft());
			} else {
				writeD(0);
			}
			Integer color = obj.getColor();
			writeC(color == null ? 0 : 1);
			if (color == null) {
				writeC(0);
				writeC(0);
				writeC(0);
			} else {
				writeC((color & 0xFF0000) >> 16);
				writeC((color & 0xFF00) >> 8);
				writeC((color & 0xFF));
			}
			writeD(0);
			writeC(obj.getObjectTemplate().getTypeId());
			if (obj instanceof UseableItemObject) {
				((UseableItemObject) obj).writeUsageData(getBuf());
			}
		} else if (action == 7) {
			writeC(action);
			writeD(itemObjectId);
		} else {
			writeC(action);
		}
	}
}
