package com.aionemu.gameserver.model.gameobjects.player.equipmentsetting;

import com.aionemu.gameserver.model.gameobjects.PersistentState;

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

	public static String defaultName(int slot) {
		return "Equipment Set " + (slot + 1);
	}

	public PersistentState getPersistentState() {
		return persistentState;
	}

	public void setPersistentState(PersistentState persistentState) {
		this.persistentState = persistentState;
	}

	public int getSlot() {
		return slot;
	}

	public String getName() {
		return name;
	}

	public int getDisplay() {
		return display;
	}

	public int getmHand() {
		return mHand;
	}

	public int getsHand() {
		return sHand;
	}

	public int getHelmet() {
		return helmet;
	}

	public int getTorso() {
		return torso;
	}

	public int getGlove() {
		return glove;
	}

	public int getBoots() {
		return boots;
	}

	public int getEarringsLeft() {
		return earringsLeft;
	}

	public int getEarringsRight() {
		return earringsRight;
	}

	public int getRingLeft() {
		return ringLeft;
	}

	public int getRingRight() {
		return ringRight;
	}

	public int getNecklace() {
		return necklace;
	}

	public int getShoulder() {
		return shoulder;
	}

	public int getPants() {
		return pants;
	}

	public int getPowershardLeft() {
		return powershardLeft;
	}

	public int getPowershardRight() {
		return powershardRight;
	}

	public int getWings() {
		return wings;
	}

	public int getWaist() {
		return waist;
	}

	public int getmOffHand() {
		return mOffHand;
	}

	public int getsOffHand() {
		return sOffHand;
	}

	public int getPlume() {
		return plume;
	}

	public int getBracelet() {
		return bracelet;
	}
}
