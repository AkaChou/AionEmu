package com.aionemu.gameserver.model.drop;

import java.nio.ByteBuffer;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.Unmarshaller;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 掉落模型。
 * Drop model.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "drop")
@NoArgsConstructor
public class Drop {
	/** 返回物品 ID / Returns the item id */
	@XmlAttribute(name = "item_id", required = true)
	protected int itemId;

	/** 返回最小数量 / Returns the min amount */
	@XmlAttribute(name = "min_amount")
	protected int minAmount = 1;

	/** 返回最大数量 / Returns the max amount */
	@XmlAttribute(name = "max_amount")
	protected int maxAmount;

	/** 返回概率 / Returns the chance */
	@XmlAttribute
	protected float chance = 100;

	@XmlAttribute(name = "no_reduce")
	protected Boolean noReduce = false;

	@XmlAttribute(name = "eachmember")
	protected boolean eachMember = false;
	@XmlAttribute(name = "each_member")
	protected Boolean aionServerEachMember;

	private ItemTemplate template;

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

	/** 设置物品 ID（供流式绑定器使用）。 / Sets the item id for the streaming binder. */
	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	/** 设置最小数量。 / Sets the minimum amount. */
	public void setMinAmount(int minAmount) {
		this.minAmount = minAmount;
	}

	/** 设置最大数量。 / Sets the maximum amount. */
	public void setMaxAmount(int maxAmount) {
		this.maxAmount = maxAmount;
	}

	/** 设置概率。 / Sets the chance. */
	public void setChance(float chance) {
		this.chance = chance;
	}

	/** 设置是否不受等级衰减。 / Sets the no-reduction flag. */
	public void setNoReduce(Boolean noReduce) {
		this.noReduce = noReduce;
	}

	/** 设置是否每人一份。 / Sets the each-member flag. */
	public void setEachMember(boolean eachMember) {
		this.eachMember = eachMember;
	}

	/** 设置 Aion 服务端每人一份标记。 / Sets the Aion-server each-member marker. */
	public void setAionServerEachMember(Boolean aionServerEachMember) {
		this.aionServerEachMember = aionServerEachMember;
	}

	/**
	 * 是否不受基于等级的掉率衰减影响。
	 * Whether reduction is disabled for this drop.
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
		drop.noReduce = buffer.get() == 1;
		drop.eachMember = buffer.get() == 1;
		return drop;
	}

	/** 返回字符串表示。 / Returns string representation. */
	public String toString() {
		return "Drop [itemId=" + itemId + ", minAmount=" + minAmount + ", maxAmount=" + maxAmount + ", chance=" + chance
				+ ", noReduce=" + noReduce + ", eachMember=" + eachMember + "]";
	}
}
