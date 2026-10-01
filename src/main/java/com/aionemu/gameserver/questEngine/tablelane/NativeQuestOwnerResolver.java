package com.aionemu.gameserver.questEngine.tablelane;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 真端表 id 集 × XML-only id 集的 owner 解析器（计划 §6.2：替代 retention 台账的推导式 owner）。
 * <p>
 * 集合来源：family loader 装载的真端表行 id（本批 = SimpleHunt，后续家族批逐个并入）∪
 * {@code quest/definitions/quests/*.xml} 文件名 id（XML-only 车道目录）。迁移期内两集合允许交叠
 * （交叠即「表行已有 XML 定义残留」的切换待办清单）；{@link #requireDisjoint()} 是 go-live 门：
 * 家族切换批在生产路由生效前必须消掉本族交叠，否则 {@code NATIVE_OWNER_CONFLICT} fail-fast。
 * <p>
 * 2026-10-01 快照：XML 目录 742 个定义；与 SimpleHunt 表 1863 行交叠恰 3 个 ——
 * 14112/16961（表行 NATIVE_READY 但 XML 定义残留，切换批同批删除）与 14123（表行名字多义冻结中，
 * 与 {@code p1/simplehunt-frozen-rows.tsv} 一致）。
 * <p>
 * The owner resolver over the retail-table id set × the XML-only id set (plan §6.2: a derived
 * owner replacing the retention ledger). Sources: family-table row ids loaded by tablelane loaders
 * (SimpleHunt in this tranche; later family batches plug theirs in) ∪ the
 * {@code quest/definitions/quests/*.xml} file-name ids (the XML-only lane directory). During
 * migration the sets MAY overlap — the overlap is precisely the switch backlog of "table rows with
 * leftover XML definitions"; {@link #requireDisjoint()} is the go-live gate: a family switch batch
 * must clear its overlap before production routing goes live, or fail fast with
 * {@code NATIVE_OWNER_CONFLICT}.
 * <p>
 * 2026-10-01 snapshot: 742 XML definitions; exactly 3 overlap the 1863 SimpleHunt rows —
 * 14112/16961 (table rows NATIVE_READY with leftover XML definitions, to be deleted in the switch
 * batch) and 14123 (table row frozen for an ambiguous name, consistent with
 * {@code p1/simplehunt-frozen-rows.tsv}).
 */
public final class NativeQuestOwnerResolver {

	/** owner 结论。 / Ownership verdict. */
	public enum Owner {
		/** 真端表车道。 / Retail-table lane. */
		NATIVE_TABLE,
		/** XML-only IR 车道。 / XML-only IR lane. */
		XML_ONLY,
		/** 两侧皆无。 / Neither lane. */
		UNOWNED
	}

	private static final String XML_DEFINITIONS_DIR = "aion/data/static_data/quest/definitions/quests";
	private static final Pattern XML_ID_FILE = Pattern.compile("(\\d+)\\.xml");

	private static volatile NativeQuestOwnerResolver instance;

	private final Set<Integer> retailTableIds;
	private final Set<Integer> xmlOnlyIds;
	private final Set<Integer> conflicts;

	private NativeQuestOwnerResolver(Set<Integer> retailTableIds, Set<Integer> xmlOnlyIds) {
		this.retailTableIds = retailTableIds;
		this.xmlOnlyIds = xmlOnlyIds;
		Set<Integer> overlap = new TreeSet<>(retailTableIds);
		overlap.retainAll(xmlOnlyIds);
		this.conflicts = Collections.unmodifiableSet(overlap);
	}

	/** 已装载的解析器（未装载则先装载）。 / The loaded resolver; loads it first when absent. */
	public static NativeQuestOwnerResolver instance() {
		NativeQuestOwnerResolver local = instance;
		if (local == null) {
			synchronized (NativeQuestOwnerResolver.class) {
				local = instance;
				if (local == null) {
					local = load(NativeQuestOwnerResolver.class.getClassLoader());
					instance = local;
				}
			}
		}
		return local;
	}

	/** 启动期强制装载（失败即异常，由调用方决定是否终止启动）。 / Eagerly loads at startup; throws on failure. */
	public static void ensureLoaded() {
		instance();
	}

	/** 从 classpath 装载：真端表行 id（现有 family loader）∪ XML 定义目录文件名 id。 / Loads retail-table ids from the family loaders and XML ids from the definitions directory. */
	static NativeQuestOwnerResolver load(ClassLoader loader) {
		Set<Integer> retailTableIds = new TreeSet<>();
		// 家族 loader 清单：SimpleHunt 先行；后续家族切换批把各自 loader 并入此处。
		// Family loader list: SimpleHunt first; later family batches add their loaders here.
		for (NativeQuestTableLoader.SimpleHuntRow row : NativeQuestTableLoader.instance().rows()) {
			retailTableIds.add(row.questId());
		}
		return new NativeQuestOwnerResolver(Collections.unmodifiableSet(retailTableIds),
				Collections.unmodifiableSet(scanXmlDefinitionIds(loader)));
	}

	private static Set<Integer> scanXmlDefinitionIds(ClassLoader loader) {
		URL dirUrl = loader.getResource(XML_DEFINITIONS_DIR);
		if (dirUrl == null || !"file".equals(dirUrl.getProtocol())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: quest definitions directory '"
					+ XML_DEFINITIONS_DIR + "' is not available as classpath files");
		}
		File dir;
		try {
			dir = new File(URI.create(dirUrl.toExternalForm().replace(" ", "%20")));
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: bad definitions directory URL " + dirUrl, e);
		}
		File[] files = dir.listFiles(File::isFile);
		if (files == null || files.length == 0) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: no quest definitions under " + XML_DEFINITIONS_DIR);
		}
		Set<Integer> ids = new TreeSet<>();
		for (File file : files) {
			// 文件名即任务 id（NNNN.xml）；非数字文件名 = 目录形状被破坏，fail-closed。
			// File names are quest ids (NNNN.xml); a non-numeric name means a broken directory shape.
			Matcher matcher = XML_ID_FILE.matcher(file.getName());
			if (!matcher.matches()) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: unexpected file in "
						+ XML_DEFINITIONS_DIR + ": " + file.getName());
			}
			ids.add(Integer.parseInt(matcher.group(1)));
		}
		return ids;
	}

	/** 从既有集合构建（包内可见供负例测试）。 / Builds from explicit sets (package-visible for tests). */
	static NativeQuestOwnerResolver fromSets(Set<Integer> retailTableIds, Set<Integer> xmlOnlyIds) {
		return new NativeQuestOwnerResolver(Set.copyOf(retailTableIds), Set.copyOf(xmlOnlyIds));
	}

	/** 真端表 id 集（跨已接入 family 的并集）。 / The retail-table id set (union over plugged-in families). */
	public Set<Integer> retailTableIds() {
		return retailTableIds;
	}

	/** XML-only 定义 id 集。 / The XML-only definition id set. */
	public Set<Integer> xmlOnlyIds() {
		return xmlOnlyIds;
	}

	/** 两侧交叠（迁移期 = 切换待办清单；go-live 时必须为空）。 / The overlap (migration: switch backlog; must be empty at go-live). */
	public Set<Integer> conflicts() {
		return conflicts;
	}

	/** 交叠是否为空。 / Whether the overlap is empty. */
	public boolean disjoint() {
		return conflicts.isEmpty();
	}

	/**
	 * go-live 门：交叠非空即 {@code NATIVE_OWNER_CONFLICT} fail-fast。
	 * The go-live gate: any overlap fails fast with {@code NATIVE_OWNER_CONFLICT}.
	 */
	public void requireDisjoint() {
		if (!conflicts.isEmpty()) {
			throw new IllegalStateException("NATIVE_OWNER_CONFLICT: quest ids claimed by both lanes: "
					+ conflicts);
		}
	}

	/**
	 * 单任务归属：真端表优先（迁移期语义：切换后表车道赢），两侧皆无 = UNOWNED。
	 * Per-quest ownership: retail-table precedence (migration semantics: the table lane wins after
	 * a switch); neither side = UNOWNED.
	 */
	public Owner ownerOf(int questId) {
		if (retailTableIds.contains(questId)) {
			return Owner.NATIVE_TABLE;
		}
		if (xmlOnlyIds.contains(questId)) {
			return Owner.XML_ONLY;
		}
		return Owner.UNOWNED;
	}
}
