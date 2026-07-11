package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * Decorate 动作模板（静态数据/XML）。
 * XML template. / XML template.
 */

public class DecorateAction extends AbstractItemAction {

	@XmlAttribute(name = "id")
	private Integer partId;

	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
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
		if (partId == null) {
			return 0;
		}
		return partId;
	}
}
