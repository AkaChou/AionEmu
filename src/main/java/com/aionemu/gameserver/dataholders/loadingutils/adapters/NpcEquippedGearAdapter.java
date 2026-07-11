package com.aionemu.gameserver.dataholders.loadingutils.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import com.aionemu.gameserver.model.items.NpcEquippedGear;

/**
 * NPC 装备 JAXB 适配器，将 {@link NpcEquipmentList} 转为运行时 {@link NpcEquippedGear}。
 * NPC equipped-gear JAXB adapter converting {@link NpcEquipmentList} into runtime {@link NpcEquippedGear}.
 *
 * @author Luno
 */
public class NpcEquippedGearAdapter extends XmlAdapter<NpcEquipmentList, NpcEquippedGear> {

	/**
	 * 序列化未实现，始终返回 null。
	 * Marshaling is not implemented and always returns null.
	 *
	 * @param v 运行时装备 / runtime gear
	 * always null
	 */
	@Override
	public NpcEquipmentList marshal(NpcEquippedGear v) throws Exception {
		return null;
	}

	/**
	 * 将 JAXB 装备列表反序列化为运行时装备对象。
	 * Unmarshals a JAXB equipment list into a runtime gear object.
	 *
	 * @param v JAXB 装备列表 / JAXB equipment list
	 * @return 运行时装备 / runtime gear
	 */
	@Override
	public NpcEquippedGear unmarshal(NpcEquipmentList v) throws Exception {
		return new NpcEquippedGear(v);
	}
}
