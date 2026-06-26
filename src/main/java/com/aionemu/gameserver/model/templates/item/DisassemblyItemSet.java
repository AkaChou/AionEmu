package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author BeckUp.Media
 */
@XmlRootElement(name = "disassemblyitemset")
public class DisassemblyItemSet
{
	@XmlAttribute(name = "disassemblyItem_Id")
	private int DisassemblyItemId;

	@XmlElement(name = "disassemble_set_list")
	private DisassembleSetList DisassembleSetList;

	public int getDisassemblyItemId()
	{
		return DisassemblyItemId;
	}

	public DisassembleSetList getDisassembleSetList()
	{
		return DisassembleSetList;
	}
}
