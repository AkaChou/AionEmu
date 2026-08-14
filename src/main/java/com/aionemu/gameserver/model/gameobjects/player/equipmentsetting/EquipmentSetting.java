package com.aionemu.gameserver.model.gameobjects.player.equipmentsetting;

import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * 装备 Setting 游戏对象。
 * Equipment Setting game object.
 */

public class EquipmentSetting {

	private PersistentState persistentState;
	private final int slot;
	private final String name;
	private final int display;
	private final int mHand;
	private final int sHand;
	private final int helmet;
	private final int torso;
	private final int glove;
	private final int boots;
	private final int earringsLeft;
	private final int earringsRight;
	private final int ringLeft;
	private final int ringRight;
	private final int necklace;
	private final int shoulder;
	private final int pants;
	private final int powershardLeft;
	private final int powershardRight;
	private final int wings;
	private final int waist;
	private final int mOffHand;
	private final int sOffHand;
	private final int plume;
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

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState persistentState) {
		this.persistentState = persistentState;
	}

	/** 获取槽位。 / Returns the slot. */
	public int getSlot() {
		return slot;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 获取显示设置。 / Returns the display. */
	public int getDisplay() {
		return display;
	}

	/** 获取主手。 / Gets the main hand. */
	public int getmHand() {
		return mHand;
	}

	/** 获取副手。 / Gets the sub hand. */
	public int getsHand() {
		return sHand;
	}

	/** 返回头盔。 / Returns the helmet. */
	public int getHelmet() {
		return helmet;
	}

	/** 返回胸甲。 / Returns the torso. */
	public int getTorso() {
		return torso;
	}

	/** 返回手套。 / Returns the glove. */
	public int getGlove() {
		return glove;
	}

	/** 返回靴子。 / Returns the boots. */
	public int getBoots() {
		return boots;
	}

	/** 返回左耳环。 / Returns the earrings left. */
	public int getEarringsLeft() {
		return earringsLeft;
	}

	/** 返回右耳环。 / Returns the earrings right. */
	public int getEarringsRight() {
		return earringsRight;
	}

	/** 返回左戒指。 / Returns the ring left. */
	public int getRingLeft() {
		return ringLeft;
	}

	/** 返回右戒指。 / Returns the ring right. */
	public int getRingRight() {
		return ringRight;
	}

	/** 返回项链。 / Returns the necklace. */
	public int getNecklace() {
		return necklace;
	}

	/** 返回肩甲。 / Returns the shoulder. */
	public int getShoulder() {
		return shoulder;
	}

	/** 返回下衣。 / Returns the pants. */
	public int getPants() {
		return pants;
	}

	/** 返回左侧魔力之石。 / Returns the powershard left. */
	public int getPowershardLeft() {
		return powershardLeft;
	}

	/** 返回右侧魔力之石。 / Returns the powershard right. */
	public int getPowershardRight() {
		return powershardRight;
	}

	/** 返回翅膀。 / Returns the wings. */
	public int getWings() {
		return wings;
	}

	/** 返回腰带。 / Returns the waist. */
	public int getWaist() {
		return waist;
	}

	/** 获取主武器副手槽。 / Gets the main off-hand weapon slot. */
	public int getmOffHand() {
		return mOffHand;
	}

	/** 获取副武器副手槽。 / Gets the sub off-hand weapon slot. */
	public int getsOffHand() {
		return sOffHand;
	}

	/** 返回羽饰。 / Returns the plume. */
	public int getPlume() {
		return plume;
	}

	/** 返回手镯。 / Returns the bracelet. */
	public int getBracelet() {
		return bracelet;
	}
}
