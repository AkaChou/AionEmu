package com.aionemu.gameserver.model.drop;

import java.nio.ByteBuffer;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.Unmarshaller;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 掉落模型。
 * Drop model.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "drop")
public class Drop {
	@XmlAttribute(name = "item_id", required = true)
	protected int itemId;

	@XmlAttribute(name = "min_amount")
	protected int minAmount = 1;

	@XmlAttribute(name = "max_amount")
	protected int maxAmount;

	@XmlAttribute
	protected float chance = 100;

	@XmlAttribute(name = "no_reduce")
	protected Boolean noReduce = false;

	@XmlAttribute(name = "eachmember")
	protected boolean eachMember = false;
	@XmlAttribute(name = "each_member")
	protected Boolean aionServerEachMember;

	private ItemTemplate template;

	public Drop() {
	}

	public Drop(int itemId, int minAmount, int maxAmount, float chance, boolean noReduce, boolean eachMember) {
		this.itemId = itemId;
		this.minAmount = minAmount;
		this.maxAmount = maxAmount;
		this.chance = chance;
		this.noReduce = noReduce;
		this.eachMember = eachMember;
		template = DataManager.ITEM_DATA.getItemTemplate(itemId);
	}

	@SuppressWarnings("unused")
	private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		if (maxAmount == 0) {
			maxAmount = minAmount;
		}
	}

	/** 获取物品模板。 / Returns the item template. */
	public ItemTemplate getItemTemplate() {
		return template == null ? DataManager.ITEM_DATA.getItemTemplate(itemId) : template;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 返回最小数量 / Returns the min amount */
	public int getMinAmount() {
		return minAmount;
	}

	/** 返回最大数量 / Returns the max amount */
	public int getMaxAmount() {
		return maxAmount;
	}

	/** 返回概率 / Returns the chance */
	public float getChance() {
		return chance;
	}

	/**
	 * 是否不受基于等级的掉率衰减影响。
	 * Whether reduction is disabled for this drop.
	 *
	 * @return 是否不衰减 / whether no reduction
	 */
	public boolean isNoReduction() {
		return noReduce;
	}

	/** 是否每人一份 / Whether each member */
	public Boolean isEachMember() {
		return aionServerEachMember == null ? eachMember : aionServerEachMember;
	}

	/** 加载。 / Load. */
	public static Drop load(ByteBuffer buffer) {
		Drop drop = new Drop();
		drop.itemId = buffer.getInt();
		drop.chance = buffer.getFloat();
		drop.minAmount = buffer.getInt();
		drop.maxAmount = buffer.getInt();
		drop.noReduce = buffer.get() == 1 ? true : false;
		drop.eachMember = buffer.get() == 1 ? true : false;
		return drop;
	}

	/** 返回字符串表示。 / Returns string representation. */
	public String toString() {
		return "Drop [itemId=" + itemId + ", minAmount=" + minAmount + ", maxAmount=" + maxAmount + ", chance=" + chance
				+ ", noReduce=" + noReduce + ", eachMember=" + eachMember + "]";
	}
}
