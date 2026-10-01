package com.aionemu.gameserver.questEngine.tablelane;

/**
 * 真端 32 位 raw quest vars 的 6 位/10 位槽编解码（计划 §6.2，P0a 证据：fun_731.cpp:5306/5356）。
 * <p>
 * 6 位 = 5 槽（bit0..29，槽 1 起）；10 位 = 3 槽（bit0..29）；bit30/31 为守卫哨兵区——
 * 真端相机在 {@code vars >= 0x40000000} 时整体放弃写入。本类不复用 {@code QuestVars}
 * （那只服务 XML IR 车道）；槽越界或 vars 带守卫位一律 fail-closed。
 * <p>
 * Retail 32-bit raw quest vars codec for 6-bit/10-bit slot layouts (plan §6.2; P0a evidence
 * fun_731.cpp:5306/5356). 6-bit = 5 slots (bits 0..29, 1-based); 10-bit = 3 slots (bits 0..29);
 * bits 30/31 form the guard-sentinel region the retail camera refuses to touch. Deliberately not
 * reusing {@code QuestVars} (XML-IR lane only); out-of-range slots and guarded vars fail closed.
 */
public final class RawQuestVarsCodec {

	/** 守卫哨兵位：vars 必须 < 0x40000000。 / Guard sentinel: vars must stay below 0x40000000. */
	public static final int GUARD_BITS = 0x40000000;

	/** 槽宽度型别。 / Slot width kind. */
	public enum Width {
		/** 6 位 × 5 槽。 / six bits, up to five slots. */
		SIX(6, 5, 0x3f),
		/** 10 位 × 3 槽。 / ten bits, up to three slots. */
		TEN(10, 3, 0x3ff);

		private final int bits;
		private final int maxSlots;
		private final int slotMask;

		Width(int bits, int maxSlots, int slotMask) {
			this.bits = bits;
			this.maxSlots = maxSlots;
			this.slotMask = slotMask;
		}

		/** 位宽。 / Bit width. */
		public int bits() {
			return bits;
		}

		/** 最大槽数。 / Maximum slot count. */
		public int maxSlots() {
			return maxSlots;
		}

		/** 单槽掩码。 / Per-slot mask. */
		public int slotMask() {
			return slotMask;
		}

		/** 槽 1 基，返回位偏移。 / 1-based slot to bit shift. */
		public int shift(int slot) {
			if (slot < 1 || slot > maxSlots) {
				throw new IllegalArgumentException(
						"NATIVE_RAW_VARS_INVALID: slot " + slot + " out of range 1.." + maxSlots + " (" + name() + ")");
			}
			return (slot - 1) * bits;
		}
	}

	private RawQuestVarsCodec() {
	}

	/** vars 是否处于相机可写区（无守卫位、非负）。 / Whether vars sits in the camera-writable region. */
	public static boolean guardClear(int vars) {
		return vars >= 0 && vars < GUARD_BITS;
	}

	/** 读槽值（槽 1 基）。 / Reads a slot value (1-based). */
	public static int slotValue(Width width, int vars, int slot) {
		requireReadable(vars);
		return (vars >>> width.shift(slot)) & width.slotMask();
	}

	/**
	 * 写槽值并返回整字；超掩码值 fail-closed。
	 * Writes a slot value; values exceeding the slot mask fail closed.
	 */
	public static int withSlotValue(Width width, int vars, int slot, int value) {
		requireReadable(vars);
		if ((value & ~width.slotMask()) != 0) {
			throw new IllegalArgumentException("NATIVE_RAW_VARS_INVALID: value " + value + " exceeds "
					+ width.name() + " slot mask");
		}
		int shift = width.shift(slot);
		return (vars & ~(width.slotMask() << shift)) | (value << shift);
	}

	private static void requireReadable(int vars) {
		if (!guardClear(vars)) {
			throw new IllegalArgumentException(
					"NATIVE_RAW_VARS_INVALID: vars " + Integer.toHexString(vars) + " crosses the guard bits");
		}
	}
}
