package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 分解物品组中的单条产出：概率与产出物品。
 * Single output entry of a disassemble group: probability and produced item.
 *
 * @author BeckUp.Media
 */
@Getter
@XmlRootElement(name = "item")
public class DisassembleItems
{
	/** 返回 item prob / Returns the item prob */
	@XmlAttribute(name = "iProb")
	private int ItemProb;
	@XmlAttribute(name = "custom")
	private boolean custom;
	@XmlElement(name = "create")
	private DisassembleItem ItemToCreate;

	/** 获取物品。 / Returns the item. */
	public DisassembleItem getItem()
	{
		return ItemToCreate;
	}
}
