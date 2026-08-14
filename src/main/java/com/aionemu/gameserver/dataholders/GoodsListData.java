package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.goods.GoodsList;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 商品列表配置数据容器，维护普通、入库与购买三类商品列表。
 * Goods list configuration data holder for regular, inbound, and purchase goods lists.
 */
@XmlRootElement(name = "goodslists")
@XmlAccessorType(XmlAccessType.FIELD)
public class GoodsListData {
	@XmlElement(required = true)
	protected List<GoodsList> list;

	@XmlElement(name = "in_list")
	protected List<GoodsList> inList;

	@XmlElement(name = "purchase_list")
	protected List<GoodsList> pList;

	private IntObjectHashMap<GoodsList> goodsListData;
	private IntObjectHashMap<GoodsList> goodsInListData;
	private IntObjectHashMap<GoodsList> goodsPurchaseListData;

	/**
	 * JAXB 反序列化完成后，将三类商品列表分别按 ID 建立索引并释放原始列表。
	 * After JAXB unmarshalling, indexes the three goods list types by id and releases the raw lists.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		goodsListData = new IntObjectHashMap<GoodsList>();
		for (GoodsList it : list) {
			goodsListData.put(it.getId(), it);
		}
		goodsInListData = new IntObjectHashMap<GoodsList>();
		for (GoodsList it : inList) {
			goodsInListData.put(it.getId(), it);
		}
		goodsPurchaseListData = new IntObjectHashMap<GoodsList>();
		for (GoodsList it : pList) {
			goodsPurchaseListData.put(it.getId(), it);
		}
		list = null;
		inList = null;
		pList = null;
	}

	/**
	 * 按 ID 获取普通商品列表。
	 * Returns the regular goods list for the given id.
	 *
	 * @param id 商品列表 ID / goods list id
	 * @return 商品列表，不存在则为 null / goods list, or null if absent
	 */
	public GoodsList getGoodsListById(int id) {
		return goodsListData.get(id);
	}

	/**
	 * 按 ID 获取入库商品列表。
	 * Returns the inbound goods list for the given id.
	 *
	 * @param id 商品列表 ID / goods list id
	 * @return 入库商品列表，不存在则为 null / inbound goods list, or null if absent
	 */
	public GoodsList getGoodsInListById(int id) {
		return goodsInListData.get(id);
	}

	/**
	 * 按 ID 获取购买商品列表。
	 * Returns the purchase goods list for the given id.
	 *
	 * @param id 商品列表 ID / goods list id
	 * @return 购买商品列表，不存在则为 null / purchase goods list, or null if absent
	 */
	public GoodsList getGoodsPurchaseListById(int id) {
		return goodsPurchaseListData.get(id);
	}

	/**
	 * 返回三类商品列表条目总数。
	 * Returns the total number of entries across all three goods list types.
	 *
	 * @return 条目总数 / total entry count
	 */
	public int size() {
		return goodsListData.size() + goodsInListData.size() + goodsPurchaseListData.size();
	}
}
