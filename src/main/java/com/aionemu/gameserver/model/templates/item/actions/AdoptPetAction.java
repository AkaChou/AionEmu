package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * Adopt 宠物动作模板（静态数据/XML）。
 * XML template.
 */

public class AdoptPetAction extends AbstractItemAction {
	@XmlAttribute(name = "petId")
	private int petId;

	@XmlAttribute(name = "minutes")
	private int expireMinutes;

	@XmlAttribute(name = "sidekick")
	private Boolean isSideKick = false;

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

	/** 返回 pet id / Returns the pet id */
	public int getPetId() {
		return petId;
	}

	/** 返回 expire minutes / Returns the expire minutes */
	public int getExpireMinutes() {
		return expireMinutes;
	}

	/**
	 * @return Whether side kick
	 */
	public Boolean isSideKick() {
		return isSideKick;
	}
}
