package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Disassembly 物品 Set 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author BeckUp.Media
 */
@XmlRootElement(name = "disassemblyitemset")
public class DisassemblyItemSet
{
	@XmlAttribute(name = "disassemblyItem_Id")
	private int DisassemblyItemId;

	@XmlElement(name = "disassemble_set_list")
	private DisassembleSetList DisassembleSetList;

	/** 返回 disassembly item id / Returns the disassembly item id */
	public int getDisassemblyItemId()
	{
		return DisassemblyItemId;
	}

	/** 返回 disassemble set list / Returns the disassemble set list */
	public DisassembleSetList getDisassembleSetList()
	{
		return DisassembleSetList;
	}
}
