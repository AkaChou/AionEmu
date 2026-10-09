package com.aionemu.gameserver.questEngine.tablelane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Row;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Step;

/**
 * DD `enterarea` 轴的**原生进区端口**（计划 §10.2「P7 DataDriven」步 2 步 c）。
 * <p>
 * 原版语义（`ScriptDLL64.c` 反编译）：进区 handler `FUN_180c47bf0` 取当前步的区别名算名字哈希
 * `FUN_1810798b0`（0x1003F），与进区区名哈希逐值比对 ⇒ 绑定 = **同名**，没有任何名字换算规则。
 * 因此本端口只做两件事：
 * <ol>
 *   <li><b>恒等解析</b>：DD 别名 → 同名注册区（大小写按 {@code ZoneName} 注册表规范形）；
 *       `zones_retail_enterarea.xml` 里的多边形（原版 `<sensory_area>` / `<questscript_area>` /
 *       `<item_use_area>`，坐标与 top/bottom 逐字取自原版世界文件）即该区的判定几何；</li>
 *   <li><b>fail-closed 冻结</b>：原版世界文件里没有该区定义的别名落进 {@link #RETAIL_ABSENT_ALIASES}
 *       —— 不注册、不猜几何、不得用「出生点 + 半径」近似补（旧 Encom 壳已按 QE-109 归一为原版多边形）；
 *       其余任何未登记别名一律抛稳定码异常，禁止静默成为死边。</li>
 * </ol>
 * 本类只解析与校验，不触运行时状态；进区事件面（`QuestEngine.onEnterZone` 原生分流 + 启动期注册）
 * 随 DD 原生 handler 同批接线。
 * <p>
 * Native enter-area port of the DataDriven axis. The retail handler compares the name hash of the current
 * step's alias with the hash of the entered zone name, so the binding is pure identity: a DD alias either
 * resolves to a same-named registered zone (geometry authored straight from the retail world files) or is
 * frozen as retail-absent and fails closed. No renaming rules and no approximated geometry are allowed.
 */
public final class NativeEnterAreaPort {

	/**
	 * 原版世界文件（`<原版根>/Map/Worlds/<world>/world{,_M,_N}.xml`）里**没有**该区定义的别名 = fail-closed 冻结。
	 * <p>
	 * 证据：`p7/tools/enterarea-retail-zone-probe.py` 全量扫描原版 256 个世界目录的三个世界文件变体；
	 * progress 轴 105 个别名里 14 个残留集中在**原版副本残缺的 LF6（Elyos）侧**：镜像的 DF6（Asmodian）侧在 `df6/world_N.xml`
	 * 有完整 `<sensory_area>` 多边形（`DF6_SensoryArea_Q25551_AtoB`、`DF6_SensoryArea_Q25601a_Dynamic_Env`
	 * 等），LF6 侧在原版副本里只有 `LF6_SensoryArea_Q25673` 与绑定 quest 25674 的 questscript 区；
	 * 客户端只有 SP NPC 位点与 `sensory_range=10`（无多边形）⇒ 无几何可依，禁近似。
	 * Frozen retail-absent aliases: the retail copy's LF6 (Elyos) world files carry no definition for these
	 * names while the mirrored DF6 (Asmodian) side has full polygons; the client ships only an SP-NPC anchor
	 * plus {@code sensory_range = 10}, i.e. no polygon. Registrable geometry does not exist, so these steps
	 * stay fail-closed instead of being approximated.
	 */
	public static final Set<String> RETAIL_ABSENT_ALIASES = Set.of(
		"LF6_SensoryArea_Q15551_AtoB",
		"LF6_SensoryArea_Q15551_BtoA",
		"LF6_SensoryArea_Q15552_AtoD",
		"LF6_SensoryArea_Q15552_DtoA",
		"LF6_SensoryArea_Q15553_AtoF",
		"LF6_SensoryArea_Q15553_FtoA",
		"LF6_SensoryArea_Q15554_AtoH",
		"LF6_SensoryArea_Q15554_HtoA",
		"LF6_SensoryArea_Q15601a_Dynamic_Env",
		"LF6_SensoryArea_Q15601b",
		"LF6_SensoryArea_Q15602a_Dynamic_Env",
		"LF6_SensoryArea_Q15604a_Dynamic_Env",
		"LF6_SensoryArea_Q15605a_Named",
		"LF6_SensoryArea_Q15608a_Dynamic_Env");

	private final Map<Integer, Map<Integer, String>> zoneByQuestAndStep;
	private final Map<Integer, List<String>> zonesByQuest;
	private final Map<Integer, Set<String>> absentByQuest;
	private final Set<String> resolvedAliases;
	private final Set<String> absentAliases;

	private NativeEnterAreaPort(Map<Integer, Map<Integer, String>> zoneByQuestAndStep,
			Map<Integer, List<String>> zonesByQuest, Map<Integer, Set<String>> absentByQuest,
			Set<String> resolvedAliases, Set<String> absentAliases) {
		this.zoneByQuestAndStep = Map.copyOf(zoneByQuestAndStep);
		this.zonesByQuest = Map.copyOf(zonesByQuest);
		this.absentByQuest = Map.copyOf(absentByQuest);
		this.resolvedAliases = Set.copyOf(resolvedAliases);
		this.absentAliases = Set.copyOf(absentAliases);
	}

	/**
	 * 按切换集建立端口视图：逐行解析 `EnterArea` 步 → 同名注册区，未登记且未冻结者 fail-closed。
	 *
	 * @param table             原版 DD 原生行模型 / the native DD row model
	 * @param routedQuestIds    本车道接管的 quest id（P7 切换集）/ the quest ids this lane owns
	 * @param registeredZones   已登记区名（大小写不敏感）/ the registered zone names
	 * @return 端口视图 / the port view
	 */
	public static NativeEnterAreaPort create(DataDrivenQuestTable table, Set<Integer> routedQuestIds,
			Set<String> registeredZones) {
		Map<String, String> canonical = new TreeMap<>();
		for (String zone : registeredZones) {
			canonical.putIfAbsent(zone.toUpperCase(Locale.ROOT), zone);
		}
		Map<Integer, Map<Integer, String>> byStep = new LinkedHashMap<>();
		Map<Integer, List<String>> byQuest = new LinkedHashMap<>();
		Map<Integer, Set<String>> absentByQuest = new LinkedHashMap<>();
		Set<String> resolved = new LinkedHashSet<>();
		Set<String> absent = new LinkedHashSet<>();
		for (int questId : new java.util.TreeSet<>(routedQuestIds)) {
			Row row = table.find(questId).orElse(null);
			if (row == null) {
				continue;
			}
			for (Step step : row.steps()) {
				if (step.kind() != DataDrivenQuestTable.Kind.ENTER_AREA) {
					continue;
				}
				String alias = step.payload().trim();
				String registered = canonical.get(alias.toUpperCase(Locale.ROOT));
				if (registered != null) {
					byStep.computeIfAbsent(questId, key -> new TreeMap<>()).put(step.index(), registered);
					byQuest.computeIfAbsent(questId, key -> new ArrayList<>());
					if (!byQuest.get(questId).contains(registered)) {
						byQuest.get(questId).add(registered);
					}
					resolved.add(alias);
					continue;
				}
				if (RETAIL_ABSENT_ALIASES.contains(alias)) {
					absentByQuest.computeIfAbsent(questId, key -> new LinkedHashSet<>()).add(alias);
					absent.add(alias);
					continue;
				}
				throw new IllegalStateException("DATA_DRIVEN_ENTER_AREA_ZONE_UNRESOLVED: quest " + questId
					+ " step " + step.index() + " alias " + alias);
			}
		}
		return new NativeEnterAreaPort(byStep, byQuest, absentByQuest, resolved, absent);
	}

	/** 某步解析出的同名注册区（非 EnterArea 步或未解析返回空）。 / Resolved zone name of one step. */
	public Optional<String> zoneName(int questId, int stepIndex) {
		return Optional.ofNullable(zoneByQuestAndStep.getOrDefault(questId, Map.of()).get(stepIndex));
	}

	/** 该行声明过的进区（表序去重），供启动期注册进区兴趣。 / Distinct zone names declared by a row. */
	public List<String> zoneNames(int questId) {
		return List.copyOf(zonesByQuest.getOrDefault(questId, List.of()));
	}

	/** 该行被冻结的「原版无区」别名（fail-closed 面，不得注册）。 / Frozen retail-absent aliases of a row. */
	public Set<String> absentAliases(int questId) {
		return Collections.unmodifiableSet(absentByQuest.getOrDefault(questId, Set.of()));
	}

	/** 该行是否在进区轴上有可注册的区。 / Whether the row declares any registrable enter-area zone. */
	public boolean hasResolvableZone(int questId) {
		return zonesByQuest.containsKey(questId);
	}

	/** 全部解析成功的别名（去重）。 / Every resolved alias. */
	public Set<String> resolvedAliases() {
		return resolvedAliases;
	}

	/** 全部被冻结的别名（去重）。 / Every frozen absent alias. */
	public Set<String> frozenAbsentAliases() {
		return absentAliases;
	}
}
