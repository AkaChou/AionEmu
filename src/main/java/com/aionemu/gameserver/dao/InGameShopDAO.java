package com.aionemu.gameserver.dao;

import java.util.List;
import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.ingameshop.IGItem;

/**
 * 游戏内商城数据访问对象。
 * In-game shop data access object.
 *
 * @author xTz, KID
 */
public abstract class InGameShopDAO implements DAO {

	/**
	 * 删除游戏内商城物品。
	 * Deletes an in-game shop item.
	 *
	 * item ID
	 * category
	 * @param list 列表序号 / list index
	 * @param param 附加参数 / extra parameter
	 * whether successful
	 */
	public abstract boolean deleteIngameShopItem(int itemId, byte category, byte list, int param);

	/**
	 * 加载全部游戏内商城物品。
	 * Loads all in-game shop items.
	 *
	 * @return 分类到物品列表的映射 / map of category to item list
	 */
	public abstract Map<Byte, List<IGItem>> loadInGameShopItems();

	/**
	 * 保存游戏内商城物品。
	 * Saves an in-game shop item.
	 *
	 * object or record ID
	 * item template ID
	 * @param paramLong1 价格或数量相关值 / price or count related value
	 * @param paramLong2 价格或数量相关值 / price or count related value
	 * category
	 * sub-category
	 * list index
	 * @param paramInt4 销量或附加数值 / sales or extra value
	 * extra flag
	 * extra flag
	 * title or name
	 * description
	 */
	public abstract void saveIngameShopItem(int paramInt1, int paramInt2, long paramLong1, long paramLong2,
			byte paramByte1, byte paramByte2, int paramInt3, int paramInt4, byte paramByte3, byte paramByte4,
			String paramString1, String paramString2);

	/**
	 * 增加商品销量。
	 * Increases sales count for an item.
	 *
	 * shop object ID
	 * current sales
	 * whether successful
	 */
	public abstract boolean increaseSales(int object, int current);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	@Override
	public String getClassName() {
		return InGameShopDAO.class.getName();
	}
}
