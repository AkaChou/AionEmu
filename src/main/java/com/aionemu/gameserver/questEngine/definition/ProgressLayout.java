package com.aionemu.gameserver.questEngine.definition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable checked layout for the 32-bit quest_vars projection. */
public final class ProgressLayout {
	private final List<BitField> fields;
	private final Map<String, BitField> byName;

	private ProgressLayout(List<BitField> fields) {
		this.fields = List.copyOf(fields);
		Map<String, BitField> index = new LinkedHashMap<>();
		int used = 0;
		for (BitField field : fields) {
			if (index.putIfAbsent(field.name(), field) != null) {
				throw new IllegalArgumentException("duplicate progress field: " + field.name());
			}
			if ((used & field.mask()) != 0) {
				throw new IllegalArgumentException("overlapping progress field: " + field.name());
			}
			used |= field.mask();
		}
		this.byName = Map.copyOf(index);
	}

	public static ProgressLayout empty() {
		return new ProgressLayout(List.of());
	}

	public static ProgressLayout of(List<BitField> fields) {
		Objects.requireNonNull(fields, "fields");
		return new ProgressLayout(fields);
	}

	public List<BitField> fields() {
		return fields;
	}

	public BitField field(String name) {
		return byName.get(name);
	}

	public int pack(Map<String, Integer> values) {
		Objects.requireNonNull(values, "values");
		int packed = 0;
		for (BitField field : fields) {
			Integer value = values.get(field.name());
			if (value == null) {
				continue;
			}
			if (value < field.minValue() || value > field.maxValue()) {
				throw new IllegalArgumentException("value out of range for progress field: " + field.name());
			}
			packed |= value << field.offset();
		}
		if (values.keySet().stream().anyMatch(name -> !byName.containsKey(name))) {
			throw new IllegalArgumentException("unknown progress field");
		}
		return packed;
	}

	public Map<String, Integer> unpack(int packed) {
		Map<String, Integer> values = new LinkedHashMap<>();
		for (BitField field : fields) {
			int value = (packed >>> field.offset()) & (field.mask() >>> field.offset());
			values.put(field.name(), value);
		}
		return Map.copyOf(values);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ProgressLayout layout && fields.equals(layout.fields);
	}

	@Override
	public int hashCode() {
		return fields.hashCode();
	}

	@Override
	public String toString() {
		return "ProgressLayout" + fields;
	}

	public static final class Builder {
		private final List<BitField> fields = new ArrayList<>();

		public Builder add(BitField field) {
			fields.add(Objects.requireNonNull(field, "field"));
			return this;
		}

		public ProgressLayout build() {
			return ProgressLayout.of(fields.stream().sorted(Comparator.comparingInt(BitField::offset)).toList());
		}
	}
}
