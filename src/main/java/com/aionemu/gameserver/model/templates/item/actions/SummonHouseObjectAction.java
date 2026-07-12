package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 召唤物房屋对象动作模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonHouseObjectAction")
public class SummonHouseObjectAction extends AbstractItemAction {

	@XmlAttribute(name = "id")
	private int objectId;

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		return false;
	}

	/** 执行 / act. */
	@Override
	public void act(Player player, Item parentItem, Item targetItem) {
	}

	/** 返回模板 ID / Returns the template id */
	public int getTemplateId() {
		return objectId;
	}
}
