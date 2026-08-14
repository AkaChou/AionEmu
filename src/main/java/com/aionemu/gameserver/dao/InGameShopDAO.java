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
	 * @param itemId 物品 ID / item ID
	 * @param category 类别 / category
	 * @param list 列表序号 / list index
	 * @param param 附加参数 / extra parameter
	 * @return 是否成功 / whether successful
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
	 * @param paramInt1 对象或记录 ID / object or record ID
	 * @param paramInt2 物品模板 ID / item template ID
	 * @param paramLong1 价格或数量相关值 / price or count related value
	 * @param paramLong2 价格或数量相关值 / price or count related value
	 * @param paramByte1 类别 / category
	 * @param paramByte2 子类别 / sub-category
	 * @param paramInt3 列表索引 / list index
	 * @param paramInt4 销量或附加数值 / sales or extra value
	 * @param paramByte3 附加标记 / extra flag
	 * @param paramByte4 附加标记 / extra flag
	 * @param paramString1 标题或名称 / title or name
	 * @param paramString2 描述 / description
	 */
	public abstract void saveIngameShopItem(int paramInt1, int paramInt2, long paramLong1, long paramLong2,
			byte paramByte1, byte paramByte2, int paramInt3, int paramInt4, byte paramByte3, byte paramByte4,
			String paramString1, String paramString2);

	/**
	 * 增加商品销量。
	 * Increases sales count for an item.
	 *
	 * @param object 商店对象 ID / shop object ID
	 * @param current 当前销量 / current sales
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean increaseSales(int object, int current);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public String getClassName() {
		return InGameShopDAO.class.getName();
	}
}
