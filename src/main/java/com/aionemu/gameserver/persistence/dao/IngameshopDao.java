package com.aionemu.gameserver.persistence.dao;

import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.ingameshop.IGItem;
import com.aionemu.gameserver.persistence.entity.IngameshopPo;
import com.mybatisflex.core.service.IService;

/**
 * 游戏内商城数据访问对象。
 * In-game shop data access object.
 * @author xTz, KID
 */
public interface IngameshopDao extends IService<IngameshopPo> {

	/**
	 * 删除游戏内商城物品。
	 * Deletes an in-game shop item.
	 * @param itemId 物品 ID / item ID
	 * @param category 类别 / category
	 * @param list 列表序号 / list index
	 * @param param 附加参数 / extra parameter
	 */
	void deleteIngameShopItem(int itemId, byte category, byte list, int param);

	/**
	 * 加载全部游戏内商城物品。
	 * Loads all in-game shop items.
	 * @return 分类到物品列表的映射 / map of category to item list
	 */
	Map<Byte, List<IGItem>> loadInGameShopItems();

	/**
	 * 保存游戏内商城物品。
	 * Saves an in-game shop item.
	 * @param objectId 对象或记录 ID / object or record ID
	 * @param itemId 物品模板 ID / item template ID
	 * @param itemCount 价格或数量相关值 / price or count related value
	 * @param itemPrice 价格或数量相关值 / price or count related value
	 * @param category 类别 / category
	 * @param subCategory 子类别 / sub-category
	 * @param list 列表索引 / list index
	 * @param salesRanking 销量或附加数值 / sales or extra value
	 * @param itemType 附加标记 / extra flag
	 * @param gift 附加标记 / extra flag
	 * @param titleDescription 标题或名称 / title or name
	 * @param description 描述 / description
	 */
	void saveIngameShopItem(int objectId, int itemId, long itemCount, long itemPrice, byte category, byte subCategory,
		int list, int salesRanking, byte itemType, byte gift, String titleDescription, String description);

	/**
	 * 增加商品销量。
	 * Increases sales count for an item.
	 * @param object 商店对象 ID / shop object ID
	 * @param current 当前销量 / current sales
	 */
	void increaseSales(int object, int current);
}
