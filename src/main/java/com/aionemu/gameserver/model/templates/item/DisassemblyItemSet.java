package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 分解套装模板：将被分解物品 ID 绑定到分解产出列表。
 * Disassembly set template: binds the disassembled item id to its output lists.
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

	/** 返回被分解物品 ID / Returns the disassembly item id */
	public int getDisassemblyItemId()
	{
		return DisassemblyItemId;
	}

	/** 返回分解产出列表 / Returns the disassemble set list */
	public DisassembleSetList getDisassembleSetList()
	{
		return DisassembleSetList;
	}
}
