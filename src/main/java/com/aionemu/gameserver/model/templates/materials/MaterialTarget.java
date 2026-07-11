package com.aionemu.gameserver.model.templates.materials;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 材料 Target 枚举。
 * Material Target enumeration.
 *
 * @author Rolandas
 */
@XmlType(name = "MaterialTarget")
@XmlEnum
public enum MaterialTarget {

	/** 全部 / All. */
	ALL, NPC, PLAYER, PLAYER_WITH_PET;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static MaterialTarget fromValue(String value) {
		return valueOf(value);
	}

	/** 是否目标 / Whether target */
	public boolean isTarget(Creature creature) {
		if (this == ALL) {
			return true;
		}
		if (this == NPC) {
			return creature instanceof Npc;
		}
		if (this == PLAYER) {
			return creature instanceof Player;
		}
		if (this == PLAYER_WITH_PET) {
			return creature instanceof Player || creature instanceof Summon && ((Summon) creature).getMaster() != null;
		}
		return false;
	}
}
