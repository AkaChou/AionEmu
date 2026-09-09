package com.aionemu.gameserver.model.gameobjects.player.equipmentsetting;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;
import lombok.Setter;

/**
 * 装备 Setting 游戏对象。
 * Equipment Setting game object.
 */

public class EquipmentSetting {

	/** 获取持久化状态。 / Returns the persistent state. */
	@Getter
	@Setter
	private PersistentState persistentState;
	/** 获取槽位。 / Returns the slot. */
	@Getter
	private final int slot;
	/** 获取名称。 / Returns the name. */
	@Getter
	private final String name;
	/** 获取显示设置。 / Returns the display. */
	@Getter
	private final int display;
	private final int mHand;
	private final int sHand;
	/** 返回头盔。 / Returns the helmet. */
	@Getter
	private final int helmet;
	/** 返回胸甲。 / Returns the torso. */
	@Getter
	private final int torso;
	/** 返回手套。 / Returns the glove. */
	@Getter
	private final int glove;
	/** 返回靴子。 / Returns the boots. */
	@Getter
	private final int boots;
	/** 返回左耳环。 / Returns the earrings left. */
	@Getter
	private final int earringsLeft;
	/** 返回右耳环。 / Returns the earrings right. */
	@Getter
	private final int earringsRight;
	/** 返回左戒指。 / Returns the ring left. */
	@Getter
	private final int ringLeft;
	/** 返回右戒指。 / Returns the ring right. */
	@Getter
	private final int ringRight;
	/** 返回项链。 / Returns the necklace. */
	@Getter
	private final int necklace;
	/** 返回肩甲。 / Returns the shoulder. */
	@Getter
	private final int shoulder;
	/** 返回下衣。 / Returns the pants. */
	@Getter
	private final int pants;
	/** 返回左侧魔力之石。 / Returns the powershard left. */
	@Getter
	private final int powershardLeft;
	/** 返回右侧魔力之石。 / Returns the powershard right. */
	@Getter
	private final int powershardRight;
	/** 返回翅膀。 / Returns the wings. */
	@Getter
	private final int wings;
	/** 返回腰带。 / Returns the waist. */
	@Getter
	private final int waist;
	private final int mOffHand;
	private final int sOffHand;
	/** 返回羽饰。 / Returns the plume. */
	@Getter
	private final int plume;
	/** 返回手镯。 / Returns the bracelet. */
	@Getter
	private final int bracelet;

	public EquipmentSetting(int slot, int display, int mHand, int sHand, int helmet, int torso, int glove, int boots,
			int earringsLeft, int earringsRight, int ringLeft, int ringRight, int necklace, int shoulder, int pants,
			int powershardLeft, int powershardRight, int wings, int waist, int mOffHand, int sOffHand, int plume,
			int bracelet) {
		this(slot, defaultName(slot), display, mHand, sHand, helmet, torso, glove, boots, earringsLeft, earringsRight,
				ringLeft, ringRight, necklace, shoulder, pants, powershardLeft, powershardRight, wings, waist, mOffHand,
				sOffHand, plume, bracelet);
	}

	public EquipmentSetting(int slot, String name, int display, int mHand, int sHand, int helmet, int torso, int glove,
			int boots, int earringsLeft, int earringsRight, int ringLeft, int ringRight, int necklace, int shoulder,
			int pants, int powershardLeft, int powershardRight, int wings, int waist, int mOffHand, int sOffHand,
			int plume, int bracelet) {
		this.slot = slot;
		this.name = name == null || name.isBlank() ? defaultName(slot) : name;
		this.display = display;
		this.mHand = mHand;
		this.sHand = sHand;
		this.helmet = helmet;
		this.torso = torso;
		this.glove = glove;
		this.boots = boots;
		this.earringsLeft = earringsLeft;
		this.earringsRight = earringsRight;
		this.ringLeft = ringLeft;
		this.ringRight = ringRight;
		this.necklace = necklace;
		this.shoulder = shoulder;
		this.pants = pants;
		this.powershardLeft = powershardLeft;
		this.powershardRight = powershardRight;
		this.wings = wings;
		this.waist = waist;
		this.mOffHand = mOffHand;
		this.sOffHand = sOffHand;
		this.plume = plume;
		this.bracelet = bracelet;
	}

	/** 默认名称。 / Default name. */
	public static String defaultName(int slot) {
		return "Equipment Set " + (slot + 1);
	}

	/** 获取主手。 / Gets the main hand. */
	public int getmHand() {
		return mHand;
	}

	/** 获取副手。 / Gets the sub hand. */
	public int getsHand() {
		return sHand;
	}

	/** 获取主武器副手槽。 / Gets the main off-hand weapon slot. */
	public int getmOffHand() {
		return mOffHand;
	}

	/** 获取副武器副手槽。 / Gets the sub off-hand weapon slot. */
	public int getsOffHand() {
		return sOffHand;
	}
}
