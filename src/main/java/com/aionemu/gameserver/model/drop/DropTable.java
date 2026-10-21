package com.aionemu.gameserver.model.drop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 掉落组内不可变的紧凑列式存储。
 * Immutable columnar storage for the entries of one drop group.
 * <p>启动期不再为每个候选掉落构造 {@link Drop} 对象；运行期只在实际抽中条目时
 * 创建兼容视图，从而降低静态数据阶段的分配、GC 与内存占用。</p>
 * <p>Startup no longer builds a {@link Drop} object for every candidate; runtime creates a
 * compatibility view only for the entries that are actually selected, reducing allocation,
 * GC pressure and memory use during static-data loading.</p>
 */
public final class DropTable {

	private static final byte FLAG_NO_REDUCE = 1;
	private static final byte FLAG_EACH_MEMBER = 1 << 1;
	private static final byte FLAG_EACH_MEMBER_OVERRIDE = 1 << 2;
	private static final byte FLAG_EACH_MEMBER_OVERRIDE_VALUE = 1 << 3;

	private static final DropTable EMPTY = new DropTable(new int[0], new int[0], new int[0],
		new float[0], new byte[0]);

	private final int[] itemIds;
	private final int[] minAmounts;
	private final int[] maxAmounts;
	private final float[] chances;
	private final byte[] flags;

	private DropTable(int[] itemIds, int[] minAmounts, int[] maxAmounts, float[] chances, byte[] flags) {
		this.itemIds = itemIds;
		this.minAmounts = minAmounts;
		this.maxAmounts = maxAmounts;
		this.chances = chances;
		this.flags = flags;
	}

	/**
	 * 返回空表。
	 * Returns the empty table.
	 * @return 空表 / empty table
	 */
	public static DropTable empty() {
		return EMPTY;
	}

	/**
	 * 从兼容的 Drop 列表构建表；仅用于 JAXB 兼容路径和测试。
	 * Builds a table from a compatibility Drop list; intended for the JAXB compatibility path
	 * and tests.
	 * @param drops 掉落列表 / drop list
	 * @return 紧凑表 / compact table
	 */
	public static DropTable from(List<Drop> drops) {
		if (drops == null || drops.isEmpty()) {
			return EMPTY;
		}
		Builder builder = new Builder(drops.size());
		for (Drop drop : drops) {
			builder.add(drop);
		}
		return builder.build();
	}

	/**
	 * 创建表构建器。
	 * Creates a table builder.
	 * @return 构建器 / builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 返回条目数量。
	 * Returns the entry count.
	 * @return 条目数量 / entry count
	 */
	public int size() {
		return itemIds.length;
	}

	/**
	 * 表是否为空。
	 * Whether the table is empty.
	 * @return 为空时为 true / {@code true} when empty
	 */
	public boolean isEmpty() {
		return itemIds.length == 0;
	}

	/**
	 * 返回指定索引的物品 ID。
	 * Returns the item id at the requested index.
	 * @param index 索引 / index
	 * @return 物品 ID / item id
	 */
	public int itemId(int index) {
		return itemIds[index];
	}

	/**
	 * 返回指定索引的最小数量。
	 * Returns the minimum amount at the requested index.
	 * @param index 索引 / index
	 * @return 最小数量 / minimum amount
	 */
	public int minAmount(int index) {
		return minAmounts[index];
	}

	/**
	 * 返回指定索引的最大数量。
	 * Returns the maximum amount at the requested index.
	 * @param index 索引 / index
	 * @return 最大数量 / maximum amount
	 */
	public int maxAmount(int index) {
		return maxAmounts[index];
	}

	/**
	 * 返回指定索引的概率。
	 * Returns the chance at the requested index.
	 * @param index 索引 / index
	 * @return 概率 / chance
	 */
	public float chance(int index) {
		return chances[index];
	}

	/**
	 * 指定条目是否不受等级衰减影响。
	 * Whether the entry ignores level-based chance reduction.
	 * @param index 索引 / index
	 * @return 不受衰减时为 true / {@code true} when reduction is disabled
	 */
	public boolean isNoReduction(int index) {
		return (flags[index] & FLAG_NO_REDUCE) != 0;
	}

	/**
	 * 指定条目是否每人一份。
	 * Whether the entry is granted to each group member.
	 * @param index 索引 / index
	 * @return 每人一份时为 true / {@code true} when each member receives it
	 */
	public boolean isEachMember(int index) {
		byte entryFlags = flags[index];
		if ((entryFlags & FLAG_EACH_MEMBER_OVERRIDE) != 0) {
			return (entryFlags & FLAG_EACH_MEMBER_OVERRIDE_VALUE) != 0;
		}
		return (entryFlags & FLAG_EACH_MEMBER) != 0;
	}

	/**
	 * 返回指定索引的 Drop 兼容视图。
	 * Returns a Drop compatibility view for the requested index.
	 * @param index 索引 / index
	 * @return Drop 视图 / Drop view
	 */
	public Drop dropAt(int index) {
		Drop drop = new Drop();
		drop.setItemId(itemIds[index]);
		drop.setMinAmount(minAmounts[index]);
		drop.setMaxAmount(maxAmounts[index]);
		drop.setChance(chances[index]);
		byte entryFlags = flags[index];
		drop.setNoReduce((entryFlags & FLAG_NO_REDUCE) != 0);
		drop.setEachMember((entryFlags & FLAG_EACH_MEMBER) != 0);
		if ((entryFlags & FLAG_EACH_MEMBER_OVERRIDE) != 0) {
			drop.setAionServerEachMember((entryFlags & FLAG_EACH_MEMBER_OVERRIDE_VALUE) != 0);
		}
		return drop;
	}

	/**
	 * 将全部条目物化为兼容 Drop 列表；仅供管理命令、导出和测试使用。
	 * Materializes all entries as a compatibility Drop list; intended for admin commands,
	 * exports and tests only.
	 * @return 新的可变列表 / new mutable list
	 */
	public List<Drop> toDropList() {
		List<Drop> drops = new ArrayList<>(size());
		for (int i = 0; i < size(); i++) {
			drops.add(dropAt(i));
		}
		return drops;
	}

	/**
	 * 追加另一张表，保持两张表原有顺序。
	 * Appends another table while preserving both tables' order.
	 * @param other 待追加表 / table to append
	 * @return 新表或当前表 / new table or this table
	 */
	public DropTable append(DropTable other) {
		if (other == null || other.isEmpty()) {
			return this;
		}
		if (isEmpty()) {
			return other;
		}
		Builder builder = new Builder(size() + other.size());
		builder.addAll(this);
		builder.addAll(other);
		return builder.build();
	}

	/**
	 * 过滤掉指定物品 ID；没有命中时返回当前不可变表。
	 * Removes the requested item ids; returns this immutable table when nothing is removed.
	 * @param itemIdsToRemove 待移除物品 ID / item ids to remove
	 * @return 过滤后的表 / filtered table
	 */
	public DropTable withoutItemIds(ItemIdSet itemIdsToRemove) {
		if (isEmpty() || itemIdsToRemove == null || itemIdsToRemove.isEmpty()) {
			return this;
		}
		Builder builder = new Builder(size());
		boolean removed = false;
		for (int i = 0; i < size(); i++) {
			if (itemIdsToRemove.contains(itemIds[i])) {
				removed = true;
			} else {
				builder.addEntry(itemIds[i], minAmounts[i], maxAmounts[i], chances[i], flags[i]);
			}
		}
		return removed ? builder.build() : this;
	}

	/**
	 * 紧凑表的构建器；只用于启动期和兼容转换路径。
	 * Builder for the compact table; used only during startup and compatibility conversion.
	 */
	public static final class Builder {

		private int[] itemIds;
		private int[] minAmounts;
		private int[] maxAmounts;
		private float[] chances;
		private byte[] flags;
		private int size;

		private Builder() {
			this(8);
		}

		/**
		 * 创建具有预估容量的构建器。
		 * Creates a builder with the estimated capacity.
		 * @param initialCapacity 初始容量 / initial capacity
		 */
		public Builder(int initialCapacity) {
			int capacity = Math.max(4, initialCapacity);
			itemIds = new int[capacity];
			minAmounts = new int[capacity];
			maxAmounts = new int[capacity];
			chances = new float[capacity];
			flags = new byte[capacity];
		}

		/**
		 * 添加一个 Drop 兼容对象。
		 * Adds one Drop compatibility object.
		 * @param drop 掉落对象 / drop object
		 * @return 当前构建器 / this builder
		 */
		public Builder add(Drop drop) {
			Objects.requireNonNull(drop, "drop");
			// 兼容路径必须逐字段保留 Drop 的值；max_amount=0 的归一化只属于 XML 解析路径。
			// The compatibility path must preserve every Drop field; max_amount=0 normalization
			// belongs to the XML parse path.
			int entryFlags = 0;
			if (Boolean.TRUE.equals(drop.getNoReduce())) {
				entryFlags |= FLAG_NO_REDUCE;
			}
			if (drop.isEachMember()) {
				entryFlags |= FLAG_EACH_MEMBER;
			}
			Boolean eachMemberOverride = drop.getAionServerEachMember();
			if (eachMemberOverride != null) {
				entryFlags |= FLAG_EACH_MEMBER_OVERRIDE;
				if (eachMemberOverride) {
					entryFlags |= FLAG_EACH_MEMBER_OVERRIDE_VALUE;
				}
			}
			addEntry(drop.getItemId(), drop.getMinAmount(), drop.getMaxAmount(), drop.getChance(),
				(byte) entryFlags);
			return this;
		}

		/**
		 * 添加原始条目。
		 * Adds a raw entry.
		 * @param itemId 物品 ID / item id
		 * @param minAmount 最小数量 / minimum amount
		 * @param maxAmount 最大数量 / maximum amount
		 * @param chance 概率 / chance
		 * @param noReduce 是否不受等级衰减影响 / whether level reduction is disabled
		 * @param eachMember 是否每人一份 / whether each member receives it
		 * @param eachMemberOverride Aion each_member 覆盖值，可为 null /
		 *                           Aion each_member override, may be null
		 * @return 当前构建器 / this builder
		 */
		public Builder add(int itemId, int minAmount, int maxAmount, float chance, boolean noReduce,
			boolean eachMember, Boolean eachMemberOverride) {
			ensureCapacity(size + 1);
			int entryFlags = 0;
			if (noReduce) {
				entryFlags |= FLAG_NO_REDUCE;
			}
			if (eachMember) {
				entryFlags |= FLAG_EACH_MEMBER;
			}
			if (eachMemberOverride != null) {
				entryFlags |= FLAG_EACH_MEMBER_OVERRIDE;
				if (eachMemberOverride) {
					entryFlags |= FLAG_EACH_MEMBER_OVERRIDE_VALUE;
				}
			}
			addEntry(itemId, minAmount, maxAmount == 0 ? minAmount : maxAmount, chance, (byte) entryFlags);
			return this;
		}

		private void addEntry(int itemId, int minAmount, int maxAmount, float chance, byte entryFlags) {
			ensureCapacity(size + 1);
			itemIds[size] = itemId;
			minAmounts[size] = minAmount;
			maxAmounts[size] = maxAmount;
			chances[size] = chance;
			flags[size] = entryFlags;
			size++;
		}

		private void addAll(DropTable table) {
			ensureCapacity(size + table.size());
			System.arraycopy(table.itemIds, 0, itemIds, size, table.size());
			System.arraycopy(table.minAmounts, 0, minAmounts, size, table.size());
			System.arraycopy(table.maxAmounts, 0, maxAmounts, size, table.size());
			System.arraycopy(table.chances, 0, chances, size, table.size());
			System.arraycopy(table.flags, 0, flags, size, table.size());
			size += table.size();
		}

		/**
		 * 构建不可变表。
		 * Builds the immutable table.
		 * @return 紧凑表 / compact table
		 */
		public DropTable build() {
			if (size == 0) {
				return EMPTY;
			}
			if (size == itemIds.length) {
				return new DropTable(itemIds, minAmounts, maxAmounts, chances, flags);
			}
			return new DropTable(Arrays.copyOf(itemIds, size), Arrays.copyOf(minAmounts, size),
				Arrays.copyOf(maxAmounts, size), Arrays.copyOf(chances, size), Arrays.copyOf(flags, size));
		}

		private void ensureCapacity(int required) {
			if (required <= itemIds.length) {
				return;
			}
			int capacity = Math.max(required, itemIds.length + (itemIds.length >> 1));
			itemIds = Arrays.copyOf(itemIds, capacity);
			minAmounts = Arrays.copyOf(minAmounts, capacity);
			maxAmounts = Arrays.copyOf(maxAmounts, capacity);
			chances = Arrays.copyOf(chances, capacity);
			flags = Arrays.copyOf(flags, capacity);
		}
	}

	/**
	 * 物品 ID 的 primitive 开放寻址集合，避免合并重复 NPC 时为每个 ID 装箱。
	 * Primitive open-addressing set for item ids, avoiding boxing every id while merging
	 * duplicate NPCs.
	 */
	public static final class ItemIdSet {

		private int[] keys;
		private boolean[] used;
		private int size;

		/**
		 * 创建集合。
		 * Creates the set.
		 */
		public ItemIdSet() {
			keys = new int[16];
			used = new boolean[16];
		}

		/**
		 * 添加物品 ID。
		 * Adds an item id.
		 * @param itemId 物品 ID / item id
		 */
		public void add(int itemId) {
			if (size + 1 > (keys.length >> 1)) {
				resize(keys.length << 1);
			}
			int slot = slot(itemId, keys.length);
			while (used[slot]) {
				if (keys[slot] == itemId) {
					return;
				}
				slot = (slot + 1) & (keys.length - 1);
			}
			used[slot] = true;
			keys[slot] = itemId;
			size++;
		}

		/**
		 * 是否包含物品 ID。
		 * Whether the set contains the item id.
		 * @param itemId 物品 ID / item id
		 * @return 包含时为 true / {@code true} when present
		 */
		public boolean contains(int itemId) {
			int slot = slot(itemId, keys.length);
			while (used[slot]) {
				if (keys[slot] == itemId) {
					return true;
				}
				slot = (slot + 1) & (keys.length - 1);
			}
			return false;
		}

		/**
		 * 返回集合大小。
		 * Returns the set size.
		 * @return 集合大小 / set size
		 */
		public int size() {
			return size;
		}

		/**
		 * 集合是否为空。
		 * Whether the set is empty.
		 * @return 为空时为 true / {@code true} when empty
		 */
		public boolean isEmpty() {
			return size == 0;
		}

		private void resize(int capacity) {
			int[] oldKeys = keys;
			boolean[] oldUsed = used;
			keys = new int[capacity];
			used = new boolean[capacity];
			size = 0;
			for (int i = 0; i < oldKeys.length; i++) {
				if (oldUsed[i]) {
					add(oldKeys[i]);
				}
			}
		}

		private static int slot(int key, int capacity) {
			int hash = key ^ (key >>> 16);
			return hash & (capacity - 1);
		}
	}
}
