package com.aionemu.gameserver.dataholders;

import com.aionemu.gameserver.model.templates.item.DisassembleItemGroups;
import com.aionemu.gameserver.model.templates.item.DisassemblyItemSet;
import com.aionemu.commons.utils.collections.IntObjectHashMap;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author BeckUp.Media
 */
@XmlRootElement(name = "disassemblyitemsets")
@XmlAccessorType(XmlAccessType.FIELD)
public class DisassemblyItemSetsData
{
	@XmlElement(name = "disassemblyitemset")
	private List<DisassemblyItemSet> DisassemblyItemSet;

	private IntObjectHashMap<List<DisassembleItemGroups>> disassemblyItemGroups = new IntObjectHashMap<List<DisassembleItemGroups>>();
	void afterUnmarshal(Unmarshaller u, Object parent)
	{
		disassemblyItemGroups.clear();
		for (DisassemblyItemSet template : DisassemblyItemSet) {
			disassemblyItemGroups.put(template.getDisassemblyItemId(), template.getDisassembleSetList().getItemGroups());
		}
	}

	public List<DisassemblyItemSet> getDisassemblyItemSet()
	{
		return DisassemblyItemSet;
	}

	public int size()
	{
		return disassemblyItemGroups.size();
	}

	public List<DisassembleItemGroups> getInfoByItemId(int itemId)
	{
		return disassemblyItemGroups.get(itemId);
	}
}
