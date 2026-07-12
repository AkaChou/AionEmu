package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Disassemble 物品模板（静态数据/XML）。
 * XML template.
 *
 * @author BeckUp.Media
 */
@XmlRootElement(name = "item")
public class DisassembleItems
{
	@XmlAttribute(name = "iProb")
	private int ItemProb;
	@XmlAttribute(name = "custom")
	private boolean custom;
	@XmlElement(name = "create")
	private DisassembleItem ItemToCreate;

	/** 返回 item prob / Returns the item prob */
	public int getItemProb()
	{
		return ItemProb;
	}

	/** 获取物品。 / Returns the item. */
	public DisassembleItem getItem()
	{
		return ItemToCreate;
	}
}
