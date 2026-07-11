package com.aionemu.gameserver.model.templates.item.actions;

import java.util.Collection;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemChargeService;

/**
 * Charge 动作模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChargeItemAction")
public class ChargeAction extends AbstractItemAction {

	@XmlAttribute
	protected int capacity;

	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
	 */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		Collection<Item> conditioningItems = ItemChargeService.filterItemsToCondition(player, null,
				parentItem.getImprovement().getChargeWay());
		return conditioningItems.size() > 0;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, Item parentItem, Item targetItem) {
		if (!player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1)) {
			return;
		}
		Collection<Item> conditioningItems = ItemChargeService.filterItemsToCondition(player, null,
				parentItem.getImprovement().getChargeWay());
		ItemChargeService.chargeItems(player, conditioningItems, capacity);
	}
}
