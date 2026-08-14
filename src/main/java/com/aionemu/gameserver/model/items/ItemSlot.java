package com.aionemu.gameserver.model.items;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * 物品槽位枚举。
 * Item Slot enumeration.
 */

@Getter
public enum ItemSlot {
	/** 主手 / Main Hand */
	MAIN_HAND(1L), SUB_HAND(1L << 1), HELMET(1L << 2), TORSO(1L << 3), GLOVES(1L << 4), BOOTS(1L << 5),
	/** 左耳环 / Earrings Left */
	EARRINGS_LEFT(1L << 6), EARRINGS_RIGHT(1L << 7), RING_LEFT(1L << 8), RING_RIGHT(1L << 9), NECKLACE(1L << 10),
	/** 肩部 / Shoulder */
	SHOULDER(1L << 11), PANTS(1L << 12), POWER_SHARD_RIGHT(1L << 13), POWER_SHARD_LEFT(1L << 14), WINGS(1L << 15),
	/** 腰部 / Waist */
	WAIST(1L << 16), MAIN_OFF_HAND(1L << 17), SUB_OFF_HAND(1L << 18), PLUME(1L << 19), BRACELET(1L << 21),

	/** 主手或副手 / Main Or Sub */
	MAIN_OR_SUB(MAIN_HAND.slotIdMask | SUB_HAND.slotIdMask, true),
	/** 主副手或副副手 / Main Off Or Sub Off */
	MAIN_OFF_OR_SUB_OFF(MAIN_OFF_HAND.slotIdMask | SUB_OFF_HAND.slotIdMask, true),
	/** 左耳环或右耳环 / Earring Right Or Left */
	EARRING_RIGHT_OR_LEFT(EARRINGS_LEFT.slotIdMask | EARRINGS_RIGHT.slotIdMask, true),
	/** 左戒指或右戒指 / Ring Right Or Left */
	RING_RIGHT_OR_LEFT(RING_LEFT.slotIdMask | RING_RIGHT.slotIdMask, true),
	/** 左力量碎片或右力量碎片 / Shard Right Or Left */
	SHARD_RIGHT_OR_LEFT(POWER_SHARD_LEFT.slotIdMask | POWER_SHARD_RIGHT.slotIdMask, true),
	/** 右手 / Right Hand */
	RIGHT_HAND(MAIN_HAND.slotIdMask | MAIN_OFF_HAND.slotIdMask, true),
	/** 左手 / Left Hand */
	LEFT_HAND(SUB_HAND.slotIdMask | SUB_OFF_HAND.slotIdMask, true),

	// 烙印之石槽位 / STIGMA slots
	/** 烙印之石槽位 1 / Stigma1 */
	STIGMA1(1L << 30), // 4.8 checked
	/** 烙印之石槽位 2 / Stigma2 */
	STIGMA2(1L << 31), // 4.8 checked
	/** 烙印之石槽位 3 / Stigma3 */
	STIGMA3(1L << 32), // 4.8 checked
	/** 烙印之石槽位 4 / Stigma4 */
	STIGMA4(1L << 33), // 4.8 checked
	/** 烙印之石槽位 5 / Stigma5 */
	STIGMA5(1L << 34), // 4.8 checked
	/** 烙印之石槽位 6 / Stigma6 */
	STIGMA6(1L << 35), // 4.8 checked
	/** 烙印之石特殊槽位 / Stigma Special */
	STIGMA_SPECIAL(1L << 36), // 5.6 checked

	// 埃斯蒂玛槽位 / ESTIMA slots
	/** 埃斯蒂玛槽位 1 / Cp Slot1 */
	CP_SLOT1(1L << 40), // 5.3 checked
	/** 埃斯蒂玛槽位 2 / Cp Slot2 */
	CP_SLOT2(1L << 41), // 5.3 checked
	/** 埃斯蒂玛槽位 3 / Cp Slot3 */
	CP_SLOT3(1L << 42), // 5.3 checked
	/** 埃斯蒂玛槽位 4 / Cp Slot4 */
	CP_SLOT4(1L << 43), // 5.5 checked
	/** 埃斯蒂玛槽位 5 / Cp Slot5 */
	CP_SLOT5(1L << 44), // 5.5 checked
	/** 埃斯蒂玛槽位 6 / Cp Slot6 */
	CP_SLOT6(1L << 45), // 5.5 checked

	/** 埃斯蒂玛槽位 / Cp Slot */
	CP_SLOT(CP_SLOT1.slotIdMask | CP_SLOT2.slotIdMask | CP_SLOT3.slotIdMask | CP_SLOT4.slotIdMask | CP_SLOT5.slotIdMask
			| CP_SLOT6.slotIdMask, true),
	/** 全部埃斯蒂玛槽位 / All Cp Slot */
	ALL_CP_SLOT(CP_SLOT.slotIdMask, true),

	/** 普通烙印之石槽位 / Regular Stigmas */
	REGULAR_STIGMAS(STIGMA1.slotIdMask | STIGMA2.slotIdMask | STIGMA3.slotIdMask | STIGMA4.slotIdMask
			| STIGMA5.slotIdMask | STIGMA6.slotIdMask | STIGMA_SPECIAL.slotIdMask, true),
	/** 全部烙印之石槽位 / All Stigma */
	ALL_STIGMA(REGULAR_STIGMAS.slotIdMask, true);

	private long slotIdMask;
	private boolean combo;

	private ItemSlot(long mask) {
		this(mask, false);
	}

	private ItemSlot(long mask, boolean combo) {
		this.slotIdMask = mask;
		this.combo = combo;
	}

	/** 是否普通烙印之石槽位 / Whether regular stigma */
	public static boolean isRegularStigma(long slot) {
		return (REGULAR_STIGMAS.slotIdMask & slot) == slot;
	}

	/** 是否烙印之石 / Whether stigma*/
	public static boolean isStigma(long slot) {
		return (ALL_STIGMA.slotIdMask & slot) == slot;
	}

	/**
	 * 判断槽位掩码是否为埃斯蒂玛（创造点）槽位。
	 * Returns whether the slot mask is an Estima (CP) slot.
	 *
	 * @param slot 槽位掩码 / slot mask
	 * @return 是否埃斯蒂玛槽位 / whether estisma
	 */
	public static boolean isEstisma(long slot) {
		return (ALL_CP_SLOT.slotIdMask & slot) == slot;
	}

	/** 返回槽位 / Returns the slots for*/
	public static ItemSlot[] getSlotsFor(long slot) {
		List<ItemSlot> slots = new ArrayList<ItemSlot>();
		for (ItemSlot itemSlot : values()) {
			if (slot != 0 && !itemSlot.isCombo() && (slot & itemSlot.slotIdMask) == itemSlot.slotIdMask) {
				slots.add(itemSlot);
			}
		}
		return slots.toArray(new ItemSlot[slots.size()]);
	}

	/** 返回 slot for / Returns the slot for */
	public static ItemSlot getSlotFor(long slot) {
		ItemSlot[] slots = getSlotsFor(slot);
		if (slots != null && slots.length > 0) {
			return slots[0];
		}
		throw new IllegalArgumentException("Invalid provided slotIdMask " + slot);
	}
}
