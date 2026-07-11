package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSetting;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 装备预设方案列表包：下发各槽位装备配置。
 * Equipment preset list packet: per-slot equipment configurations.
 */
public class SM_EQUIPMENT_SETTING extends AionServerPacket {

	private final Collection<EquipmentSetting> equipmentSettings;

	public SM_EQUIPMENT_SETTING(Collection<EquipmentSetting> equipmentSettings) {
		this.equipmentSettings = equipmentSettings;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(equipmentSettings.size());
		for (EquipmentSetting setting : equipmentSettings) {
			writeD(setting.getSlot());
			writeD(setting.getDisplay());
			writeD(setting.getmHand());
			writeD(setting.getsHand());
			writeD(setting.getHelmet());
			writeD(setting.getTorso());
			writeD(setting.getGlove());
			writeD(setting.getBoots());
			writeD(setting.getEarringsLeft());
			writeD(setting.getEarringsRight());
			writeD(setting.getRingLeft());
			writeD(setting.getRingRight());
			writeD(setting.getNecklace());
			writeD(setting.getShoulder());
			writeD(setting.getPants());
			writeD(setting.getPowershardLeft());
			writeD(setting.getPowershardRight());
			writeD(setting.getWings());
			writeD(setting.getWaist());
			writeD(setting.getmOffHand());
			writeD(setting.getsOffHand());
			writeD(setting.getPlume());
			writeD(0);
			writeD(setting.getBracelet());
			writeD(0);
		}
	}
}
