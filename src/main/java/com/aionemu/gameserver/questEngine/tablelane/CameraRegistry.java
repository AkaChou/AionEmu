package com.aionemu.gameserver.questEngine.tablelane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * 真端相机注册表（计划 §6.2：启动期由真端表构建、不可变、逐行可对拍；P0a 相机矩阵为对拍基线）。
 * <p>
 * 每任务一行：宽型别（同任务禁止 6/10 位混用——{@code NATIVE_CAMERA_WIDTH_MIXED}）、槽→required
 * （超掩码即 {@code NATIVE_CAMERA_REQUIRED_EXCEEDS_MASK}）、fullValue 必须等于各槽满值按位组合
 * （{@code NATIVE_CAMERA_FULL_VALUE_INVALID}）；重复注册同一任务 fail-fast。
 * 真端基线：2463 调用点全 SimpleHunt、零混宽、零 fullValue 矛盾、零超掩码（2 例真端自身表/脚本
 * 分歧 13912/23912 由表行数据层显式登记，不在本类兜底）。
 * <p>
 * The retail camera registry (plan §6.2: built from true-end tables at startup, immutable, row-by-row
 * reconcilable; the P0a camera matrix is the reconciliation baseline). One row per quest: width (mixed
 * 6/10-bit widths per quest fail with {@code NATIVE_CAMERA_WIDTH_MIXED}), slot→required mappings
 * (over-mask fails with {@code NATIVE_CAMERA_REQUIRED_EXCEEDS_MASK}), and a fullValue that must equal
 * the bit-OR of all slot requirements ({@code NATIVE_CAMERA_FULL_VALUE_INVALID}); duplicate quest ids
 * fail fast. Retail baseline: all 2463 call sites are SimpleHunt, zero mixing, zero fullValue
 * conflicts, zero over-mask (the two retail-side table/script divergences 13912/23912 are registered
 * explicitly at the data layer, never papered over here).
 */
public final class CameraRegistry {

	/**
	 * 单任务相机行规约（数据层输入；fullValue 由数据显式给出，禁止本类自行"修复"）。
	 * Per-quest camera row spec (data-layer input; fullValue is given explicitly, never auto-"fixed").
	 */
	public record RowSpec(int questId, RawQuestVarsCodec.Width width, int fullValue, Map<Integer, Integer> slotRequires) {
	}

	/** 单任务已验证相机行。 / One validated per-quest camera row. */
	public record CameraRow(int questId, RawQuestVarsCodec.Width width, int fullValue, Map<Integer, Integer> slotRequires) {

		/** 槽 required（未声明槽返回 0）。 / Required count for a slot (0 when undeclared). */
		public int required(int slot) {
			return slotRequires.getOrDefault(slot, 0);
		}
	}

	private final Map<Integer, CameraRow> rowsByQuestId;
	/** 元数据不可编译、未派生相机行的采集行（当前 = 真端 minlevel 999 的休眠行）。 /
	 * Collect rows without a derived camera row because their metadata does not compile (today the
	 * retail {@code minlevel=999} dormant rows). */
	private final Set<Integer> rowsWithoutCollectCamera;

	private CameraRegistry(Map<Integer, CameraRow> rowsByQuestId) {
		this(rowsByQuestId, Set.of());
	}

	private CameraRegistry(Map<Integer, CameraRow> rowsByQuestId, Set<Integer> rowsWithoutCollectCamera) {
		this.rowsByQuestId = rowsByQuestId;
		this.rowsWithoutCollectCamera = Set.copyOf(rowsWithoutCollectCamera);
	}

	/** 未派生相机行的采集行（诊断/门禁用）。 / Collect rows without a camera row (diagnostics and gates). */
	public Set<Integer> collectRowsWithoutCamera() {
		return rowsWithoutCollectCamera;
	}

	/** 从行规约构建并全量校验。 / Builds from row specs, validating every row. */
	public static CameraRegistry fromSpecs(Collection<RowSpec> specs) {
		Map<Integer, CameraRow> rows = new LinkedHashMap<>();
		for (RowSpec spec : specs) {
			if (spec.width() == null || spec.slotRequires() == null || spec.slotRequires().isEmpty()) {
				throw new IllegalStateException("NATIVE_CAMERA_ROW_MISSING: quest " + spec.questId()
						+ " has no slot requirements");
			}
			if (rows.containsKey(spec.questId())) {
				// 真端同一任务只出现一行（零混宽）；重复注册 = 数据矛盾。
				// Retail has exactly one row per quest (zero width mixing); duplicates are data conflicts.
				throw new IllegalStateException("NATIVE_CAMERA_WIDTH_MIXED: quest " + spec.questId()
						+ " registered more than once");
			}
			Map<Integer, Integer> slotRequires = new TreeMap<>();
			int combined = 0;
			for (Map.Entry<Integer, Integer> entry : spec.slotRequires().entrySet()) {
				int slot = entry.getKey();
				int required = entry.getValue();
				if (required <= 0) {
					throw new IllegalStateException("NATIVE_CAMERA_ROW_MISSING: quest " + spec.questId()
							+ " slot " + slot + " has non-positive required " + required);
				}
				if (required > spec.width().slotMask()) {
					throw new IllegalStateException("NATIVE_CAMERA_REQUIRED_EXCEEDS_MASK: quest " + spec.questId()
							+ " slot " + slot + " required " + required + " exceeds "
							+ spec.width().name() + " mask");
				}
				slotRequires.put(slot, required);
				combined |= required << spec.width().shift(slot);
			}
			if (combined != spec.fullValue()) {
				throw new IllegalStateException("NATIVE_CAMERA_FULL_VALUE_INVALID: quest " + spec.questId()
						+ " fullValue " + Integer.toHexString(spec.fullValue()) + " != slot combination "
						+ Integer.toHexString(combined));
			}
			rows.put(spec.questId(), new CameraRow(spec.questId(), spec.width(), spec.fullValue(),
					Map.copyOf(slotRequires)));
		}
		return new CameraRegistry(Map.copyOf(rows));
	}

	private static CameraRegistry withUnresolvedCollect(List<RowSpec> specs, Set<Integer> unresolvedCollectMetadata) {
		CameraRegistry registry = fromSpecs(specs);
		return new CameraRegistry(registry.rowsByQuestId, unresolvedCollectMetadata);
	}

	/** 已注册任务数。 / Number of registered quests. */
	public int size() {
		return rowsByQuestId.size();
	}

	/** 按任务查询。 / Looks a row up by quest id. */
	public Optional<CameraRow> find(int questId) {
		return Optional.ofNullable(rowsByQuestId.get(questId));
	}

	/** 按任务查询，未注册即 fail-closed。 / Looks a row up; unregistered quests fail closed. */
	public CameraRow require(int questId) {
		CameraRow row = rowsByQuestId.get(questId);
		if (row == null) {
			throw new IllegalStateException("NATIVE_CAMERA_ROW_MISSING: quest " + questId + " has no camera row");
		}
		return row;
	}

	private static volatile CameraRegistry defaultInstance;

	/**
	 * 确保相机注册表已装载。
	 * Ensures the camera registry is loaded.
	 */
	public static void ensureLoaded() {
		instance();
	}

	/**
	 * 获取由数据表装载构建的单例注册表。
	 * Returns the singleton registry built from the table loader.
	 */
	public static CameraRegistry instance() {
		CameraRegistry local = defaultInstance;
		if (local == null) {
			synchronized (CameraRegistry.class) {
				local = defaultInstance;
				if (local == null) {
					local = loadDefault();
					defaultInstance = local;
				}
			}
		}
		return local;
	}

	private static CameraRegistry loadDefault() {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		List<RowSpec> specs = new ArrayList<>();
		for (NativeQuestTableLoader.SimpleHuntRow row : loader.rows()) {
			if (!row.killSlots().isEmpty()) {
				specs.add(loader.cameraSpec(row));
			}
		}
		for (NativeQuestTableLoader.SimpleSerialHuntRow row : loader.serialHuntRows()) {
			if (!row.stages().isEmpty()) {
				specs.add(loader.cameraSpec(row));
			}
		}
		// 采集族（P4）：单槽相机，required 直接来自真端 quest.xml 的 collect_item 计数（与交付门同源）。
		// 无采集计数的 9 行（事件/测试形态）不派生相机行，由处理器视为不可路由。
		// Collect family (P4): a single-slot camera whose requirement comes straight from the retail
		// quest.xml collect_item counts (the same source as the hand-in gate). The nine rows without a
		// collect count (event/test shapes) derive no camera row and stay unroutable in the handler.
		java.util.Set<Integer> unresolvedCollectMetadata = new java.util.TreeSet<>();
		for (NativeQuestTableLoader.SimpleCollectItemRow row : loader.collectRows()) {
			Map<Integer, Integer> slotRequires =
				NativeCollectSpecs.collectSlotRequirements(row.questId(), unresolvedCollectMetadata);
			if (!slotRequires.isEmpty()) {
				specs.add(loader.cameraSpec(row.questId(), slotRequires));
			}
		}
		return withUnresolvedCollect(List.copyOf(specs), unresolvedCollectMetadata);
	}
}
