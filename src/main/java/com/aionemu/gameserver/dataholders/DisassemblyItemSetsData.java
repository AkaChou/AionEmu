package com.aionemu.gameserver.dataholders;

import com.aionemu.gameserver.model.templates.item.DisassembleItemGroups;
import com.aionemu.gameserver.model.templates.item.DisassemblyItemSet;
import com.aionemu.commons.utils.collections.IntObjectHashMap;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.List;

/**
 * 拆解物品套装数据容器，按物品 ID 索引拆解分组列表。
 * Disassembly item-set data holder, indexing disassemble groups by item id.
 *
 * @author BeckUp.Media
 */
@XmlRootElement(name = "disassemblyitemsets")
@XmlAccessorType(XmlAccessType.FIELD)
public class DisassemblyItemSetsData
{
	@XmlElement(name = "disassemblyitemset")
	private List<DisassemblyItemSet> DisassemblyItemSet;

	@XmlTransient
	private IntObjectHashMap<List<DisassembleItemGroups>> disassemblyItemGroups = new IntObjectHashMap<List<DisassembleItemGroups>>();

	/**
	 * JAXB 反序列化完成后，按拆解物品 ID 索引分组列表。
	 * After JAXB unmarshalling, indexes group lists by disassembly item id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent)
	{
		disassemblyItemGroups.clear();
		for (DisassemblyItemSet template : DisassemblyItemSet) {
			disassemblyItemGroups.put(template.getDisassemblyItemId(), template.getDisassembleSetList().getItemGroups());
		}
	}

	/**
	 * 返回原始拆解套装列表。
	 * Returns the raw disassembly item-set list.
	 *
	 * item-set list
	 */
	public List<DisassemblyItemSet> getDisassemblyItemSet()
	{
		return DisassemblyItemSet;
	}

	/**
	 * 返回已索引的拆解套装数量。
	 * Returns the number of indexed disassembly sets.
	 *
	 * set count
	 */
	public int size()
	{
		return disassemblyItemGroups.size();
	}

	/**
	 * 按物品 ID 获取拆解分组列表。
	 * Returns the disassemble group list for the given item id.
	 *
	 * item id
	 *
	 * @param itemId
	 * @return 分组列表，不存在则为 null / group list or null
	 */
	public List<DisassembleItemGroups> getInfoByItemId(int itemId)
	{
		return disassemblyItemGroups.get(itemId);
	}
}
