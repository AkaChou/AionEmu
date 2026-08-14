package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;

/**
 * 既有 32 位 quest_vars 值中经过校验的一段字段切片。
 * A checked slice of the existing 32-bit quest_vars value.
 */
public record BitField(String name, int offset, int width, int minValue, int maxValue,
		PersistenceMode persistence, ProgressScope scope) {
	public BitField {
		name = requireText(name, "name");
		persistence = Objects.requireNonNull(persistence, "persistence");
		scope = Objects.requireNonNull(scope, "scope");
		if (offset < 0 || offset > 31) {
			throw new IllegalArgumentException("bit-field offset must be in [0, 31]");
		}
		if (width < 1 || width > 32 || offset + width > 32) {
			throw new IllegalArgumentException("bit-field must fit within 32 quest_vars bits");
		}
		long representableMax = width == 32 ? Integer.MAX_VALUE : (1L << width) - 1L;
		if (minValue < 0 || maxValue < minValue || maxValue > representableMax) {
			throw new IllegalArgumentException("bit-field value range exceeds its declared width");
		}
	}

	public BitField(String name, int offset, int width, PersistenceMode persistence) {
		this(name, offset, width, 0, width == 32 ? Integer.MAX_VALUE : (int) ((1L << width) - 1L), persistence,
			ProgressScope.LOCAL);
	}

	public int mask() {
		return (int) (((1L << width) - 1L) << offset);
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}
}
