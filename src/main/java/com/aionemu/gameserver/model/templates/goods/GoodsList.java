package com.aionemu.gameserver.model.templates.goods;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.limiteditems.LimitedItem;
import lombok.Getter;

/**
 * Goods 列表模板（静态数据/XML）。
 * XML template.
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GoodsList")
public class GoodsList {

	@XmlElement(name = "item")
	private List<Item> items;
	/**
	 * -- GETTER --
	 *  获取 id 属性值。
	 *  Gets the value of the id property
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;
	@XmlElement(name = "salestime")
	private String salesTime;

	@Getter
	private List<Integer> itemIdList;

	void afterUnmarshal(Unmarshaller u, Object parent) {
		itemIdList = new ArrayList<>();
		if (items == null) {
			return;
		}
		for (Item item : items) {
			itemIdList.add(item.getId());
		}
	}

	/**
	 * @return 限量物品列表 / the limited items
	 */
	public List<LimitedItem> getLimitedItems() {
		List<LimitedItem> limitedItems = new ArrayList<>();
		if (items != null) {
			for (Item item : items) {
				if (item.getBuyLimit() != null && item.getSellLimit() != null) {
					limitedItems.add(new LimitedItem(item.getId(), item.getSellLimit(), item.getBuyLimit(), salesTime));
				}
			}
		}
		return limitedItems;
	}

	/**
	 * 匿名复杂类型的 Java 类（XSD 生成）。 / <p> Java class for anonymous complex type. <p> The following schema fragment specifies the expected content contained within this class. <pre> &lt;complexType> &lt;complexContent> &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"> &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}int" /> &lt;/restriction> &lt;/complexContent> &lt;/complexType> </pre>
	 */
	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "")
	public static class Item {

        /**
         * -- GETTER --
         *  获取 id 属性值。
         *  Gets the value of the id property
         */
        @XmlAttribute
		private int id;
        @XmlAttribute(name = "sell_limit")
		private Integer sellLimit;
        @XmlAttribute(name = "buy_limit")
		private Integer buyLimit;

    }
}
