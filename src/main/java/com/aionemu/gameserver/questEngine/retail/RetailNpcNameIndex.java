package com.aionemu.gameserver.questEngine.retail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NPC {@code name_desc} → npc_id 索引。
 * <p>
 * 真端任务表用 spawn 名（如 {@code CherubimL_1_n}、{@code DF3_NPC_Hasyaditan}）描述目标，
 * 而服务端运行时按 npc_id 计数；本索引就是两者之间的桥（Phase 4 对账已验证 1:1 无歧义）。
 * <p>
 * 同一只怪在本服常常存在多个 npc_id（如 Draupnir 副官的 {@code 213802} 与实刷变体 {@code 237267}，
 * 二者 {@code name_id} 相同、客户端显示同一名字），而任务引擎按精确 npc_id 建路由
 * （{@code QuestEventIndex} 不做变体展开）。因此本索引额外维护"同名族"：
 * 击杀目标按族闭包展开，保证玩家击杀客户端所见的那只怪一定计数。
 * 家族成员数超过 {@link #DISPLAY_NAME_FAMILY_LIMIT} 时视为占位名（如 name_id 350000 有 4000+ 成员），
 * 不参与展开，避免把无关怪并入同一目标集。
 * <p>
 * Index from NPC {@code name_desc} to npc ids, plus the display-name family used to expand
 * kill targets (the quest engine routes kills by exact npc id).
 */
public final class RetailNpcNameIndex {

	private static final Pattern TEMPLATE = Pattern.compile("<npc_template\\b[^>]*>");
	private static final Pattern NAME_DESC = Pattern.compile("name_desc=\"([^\"]*)\"");
	private static final Pattern NPC_ID = Pattern.compile("npc_id=\"(\\d+)\"");
	private static final Pattern NAME_ID = Pattern.compile("name_id=\"([^\"]*)\"");

	/** 同名族展开上限：家族成员数超过此值视为占位名，不展开。 / Family size cap; larger families are placeholder names. */
	public static final int DISPLAY_NAME_FAMILY_LIMIT = 16;

	/**
	 * 真端任务表会省略 {@code NPC_} 前缀：真端 npc 注册表（{@code Map/XML/npcs.xml}）里写着
	 * {@code NPC_Gardugu}，而 {@code Quest_SimpleHunt.xml} 的接取名写 {@code Gardugu}。
	 * 本索引为带前缀的 {@code name_desc} 额外登记一条"去前缀"别名，让该写法可解析。
	 * The retail quest table drops the {@code NPC_} prefix, so the index registers the stripped name as an alias.
	 */
	private static final String NPC_PREFIX = "npc_";

	private final Map<String, Set<Integer>> byName;
	private final Map<Integer, Set<Integer>> byDisplayNameFamily;
	/** 真端对话名组：{@code quest_ai_name} → 成员 {@code name_desc} 原文（表内声明）与解析后的 id 集。 /
	 * Retail dialog-name groups: the declared member list and the resolved npc id set per name. */
	private final Map<String, Set<String>> questAiNameGroupMembers;
	private final Map<String, Set<Integer>> questAiNameGroups;

	private RetailNpcNameIndex(Map<String, Set<Integer>> byName, Map<Integer, Set<Integer>> byDisplayNameFamily,
			Map<String, Set<String>> questAiNameGroupMembers, Map<String, Set<Integer>> questAiNameGroups) {
		this.byName = Map.copyOf(byName);
		this.byDisplayNameFamily = Map.copyOf(byDisplayNameFamily);
		this.questAiNameGroupMembers = Map.copyOf(questAiNameGroupMembers);
		this.questAiNameGroups = Map.copyOf(questAiNameGroups);
	}

	/** 按名字解析 npc_id 集合（大小写不敏感）。 / Resolves npc ids by spawn name, case-insensitively. */
	public Set<Integer> resolve(String name) {
		if (name == null || name.isBlank()) {
			return Set.of();
		}
		String key = name.trim().toLowerCase(Locale.ROOT);
		if (key.chars().allMatch(Character::isDigit)) {
			return Set.of(Integer.parseInt(key));
		}
		return byName.getOrDefault(key, Set.of());
	}

	/**
	 * 前缀变体解析：精确未命中时展开 {@code 前缀_} 变体家族（18738 形：真端表接取名
	 * {@code IDRaksha_Solo_StageStart} 无独立模板，实际模板为 {@code _A.._F} 六个阶段变体，
	 * 每个变体都是合法接取 NPC）；再未命中时展开中缀变体（28738 形：表名
	 * {@code ..._StageStart_Dark} 的 {@code _Dark} 段插在变体字母之前，模板为
	 * {@code _A_Dark.._F_Dark}——去掉末段做前缀匹配，再要求候选含该末段以区分阵营）。
	 * 精确命中时退化为普通解析。
	 * Prefix-variant resolution: on an exact miss, expands the {@code prefix_} variant family
	 * (the 18738 shape: the retail table's acquire name {@code IDRaksha_Solo_StageStart} has no
	 * template of its own — the real templates are the six stage variants {@code _A.._F}, each a
	 * legal acquire npc); on a second miss, expands infix variants (the 28738 shape: the table
	 * name's {@code _Dark} segment sits before the variant letter, templates {@code _A_Dark..} —
	 * match the stem without the last segment and require the candidate to contain that segment,
	 * which keeps the factions apart). An exact hit degrades to the plain resolution.
	 */
	public Set<Integer> resolveVariants(String name) {
		if (name == null || name.isBlank()) {
			return Set.of();
		}
		String key = name.trim().toLowerCase(Locale.ROOT);
		if (key.chars().allMatch(Character::isDigit)) {
			return Set.of(Integer.parseInt(key));
		}
		Set<Integer> exact = byName.get(key);
		if (exact != null) {
			return exact;
		}
		Set<Integer> ids = new java.util.TreeSet<>();
		byName.forEach((candidate, found) -> {
			if (candidate.startsWith(key + "_")) {
				ids.addAll(found);
			}
		});
		if (!ids.isEmpty()) {
			return ids;
		}
		int lastSeparator = key.lastIndexOf('_');
		if (lastSeparator > 0) {
			String stem = key.substring(0, lastSeparator);
			String tail = key.substring(lastSeparator + 1);
			byName.forEach((candidate, found) -> {
				if (candidate.startsWith(stem + "_") && candidate.contains("_" + tail)) {
					ids.addAll(found);
				}
			});
		}
		return ids;
	}

	public int size() {
		return byName.size();
	}

	/**
	 * 真端对话名组判定：名字在组表里声明（{@code quest_ai_name} 是 ScriptDLL 的对话路由名，
	 * 一只哨卫的"守备队"同组若干 npc 共用同一个对话名）。
	 * <p>
	 * 与普通 spawn 名解析**刻意分道**：组名只在真端数据自己写出组名的字段（接取/交付）上使用，
	 * 不会让别的照常单值解析的位点（步内 npc、采集目标等）静默变宽。
	 * Declared retail dialog-name group: the ScriptDLL dialog routing name shared by the members of
	 * one guard squad. The channel is deliberately separate from plain spawn-name resolution so that
	 * only the retail fields that actually carry a group name widen, never the single-value sites.
	 */
	public boolean isQuestAiNameGroup(String name) {
		return name != null && questAiNameGroups.containsKey(name.trim().toLowerCase(Locale.ROOT));
	}

	/** 组名的成员 {@code name_desc} 原文（表内声明，按声明序）。 / Declared member list of a group name. */
	public Set<String> questAiNameGroupMembers(String name) {
		if (name == null) {
			return Set.of();
		}
		return questAiNameGroupMembers.getOrDefault(name.trim().toLowerCase(Locale.ROOT), Set.of());
	}

	/** 组名 → 成员 npc id 集（成员模板缺失时该成员缺席）。 / Group name to the member npc ids. */
	public Set<Integer> resolveQuestAiNameGroup(String name) {
		if (name == null) {
			return Set.of();
		}
		return questAiNameGroups.getOrDefault(name.trim().toLowerCase(Locale.ROOT), Set.of());
	}

	/**
	 * 先精确解析，未命中再看真端对话名组。供真端数据里写着对话名的字段（接取 / 交付）使用：
	 * 精确名与组名互斥（永久门校验），因此不存在"精确命中被组展开覆盖"的歧义。
	 * Exact resolution first, then the declared dialog-name group. Used for the retail fields that
	 * carry a dialog name (acquire / hand-in); exact names and group names are disjoint (checked by
	 * the permanent gate), so a group expansion can never shadow an exact name.
	 */
	public Set<Integer> resolveAllOrQuestAiNameGroup(String name) {
		Set<Integer> exact = resolve(name);
		return exact.isEmpty() ? resolveQuestAiNameGroup(name) : exact;
	}

	/**
	 * 接取/交付位点的**统一名字通道**：精确 → 真端对话名组（客户端声明的 {@code quest_ai_name} 组，
	 * 成员全展开）→ 名前变体（18738/28738 形：前缀 `_A..`、中缀 `..._Dark` 家族）。命中即止。
	 * <p>
	 * 顺序的理由：**客户端声明优先于名字形态推导**——同一基名可能横跨阵营镜像（`IDRaksha_Solo_
	 * StageStart` 的前缀族含 `_A_Dark` 三个暗面变体，而客户端把暗面声明成**另一个**对话名
	 * `..._StageStart_Dark`）；按族名展开会把对面阵营的 NPC 也绑成接取人（遗留 XML 与客户端都只给
	 * 本方三人）。组表声明了就用声明集，组表没声明才退回名字形态展开。精确名、组名、变体家族三者
	 * 互斥（精确名与组名互斥由永久门校验），因此"先精确"不会被更宽的通道遮蔽。这样同一名字在四个
	 * 位点（接取/交付 × 各族）上得到同一集合，而单 owner 形的门照旧拦多值。
	 * <p>
	 * The unified name channel for the acquire / hand-in sites: exact, then the declared
	 * {@code quest_ai_name} group with every member, then name variants (the 18738/28738 prefix and
	 * infix families). The client declaration outranks name-morphology inference: one base name may
	 * straddle faction mirrors (the {@code IDRaksha_Solo_StageStart} prefix family carries three
	 * {@code _Dark} variants, while the client declares the dark side under a different dialog name),
	 * and the legacy XML as well as the client only bind a faction's own members. A declared group is
	 * therefore used as declared; only an undeclared name falls back to morphological expansion. The
	 * first hit wins, exact names and group names are disjoint, and one name yields one set across all
	 * four sites, while the single-owner gates still refuse multi-value sets.
	 */
	public Set<Integer> resolvePartyName(String name) {
		Set<Integer> exact = resolve(name);
		if (!exact.isEmpty()) {
			return exact;
		}
		Set<Integer> group = resolveQuestAiNameGroup(name);
		return group.isEmpty() ? resolveVariants(name) : group;
	}

	/**
	 * 从若干 NPC 模板文件流构建索引（调用方负责关闭流）。
	 * Builds the index from NPC template streams; the caller owns the streams.
	 */
	public static RetailNpcNameIndex build(Collection<InputStream> templates) throws IOException {
		return build(templates, List.of());
	}

	/**
	 * 同 {@link #build(Collection)}，并额外加载真端对话名组表（TSV：{@code 组名 \t 成员1,成员2,...}，
	 * {@code #} 起头为注释行）。
	 * Same as {@link #build(Collection)} plus the retail dialog-name group table (TSV rows
	 * {@code name \t member1,member2,...}; {@code #} starts a comment line).
	 */
	public static RetailNpcNameIndex build(Collection<InputStream> templates,
			Collection<InputStream> questAiNameGroupTables) throws IOException {
		Map<String, Set<Integer>> byName = new LinkedHashMap<>();
		Map<String, Set<Integer>> byNameId = new LinkedHashMap<>();
		Map<Integer, String> nameIdByNpc = new LinkedHashMap<>();
		for (InputStream input : templates) {
			String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
			Matcher template = TEMPLATE.matcher(text);
			while (template.find()) {
				String tag = template.group();
				Matcher name = NAME_DESC.matcher(tag);
				Matcher npcId = NPC_ID.matcher(tag);
				if (!name.find() || !npcId.find()) {
					continue;
				}
				int id = Integer.parseInt(npcId.group(1));
				byName.computeIfAbsent(name.group(1).toLowerCase(Locale.ROOT), key -> new LinkedHashSet<>()).add(id);
				Matcher nameId = NAME_ID.matcher(tag);
				if (!nameId.find() || nameId.group(1).isBlank()) {
					continue;
				}
				nameIdByNpc.put(id, nameId.group(1));
				byNameId.computeIfAbsent(nameId.group(1), key -> new LinkedHashSet<>()).add(id);
			}
		}
		Map<Integer, Set<Integer>> families = new LinkedHashMap<>();
		nameIdByNpc.forEach((npcId, nameId) -> {
			Set<Integer> members = byNameId.get(nameId);
			if (members != null && members.size() > 1 && members.size() <= DISPLAY_NAME_FAMILY_LIMIT) {
				families.put(npcId, Set.copyOf(members));
			}
		});
		addStrippedPrefixAliases(byName);
		Map<String, Set<String>> groupMembers = new LinkedHashMap<>();
		Map<String, Set<Integer>> groups = new LinkedHashMap<>();
		for (InputStream input : questAiNameGroupTables) {
			for (String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
				String row = line.strip();
				if (row.isEmpty() || row.startsWith("#")) {
					continue;
				}
				String[] parts = row.split("\t");
				if (parts.length < 2 || parts[1].isBlank()) {
					throw new IOException("malformed dialog-name group row: " + row);
				}
				String key = parts[0].strip().toLowerCase(Locale.ROOT);
				Set<String> declared = new LinkedHashSet<>();
				Set<Integer> ids = new LinkedHashSet<>();
				for (String member : parts[1].split(",")) {
					if (member.isBlank()) {
						continue;
					}
					declared.add(member.strip());
					ids.addAll(byName.getOrDefault(member.strip().toLowerCase(Locale.ROOT), Set.of()));
				}
				groupMembers.put(key, Set.copyOf(declared));
				// 成员 id 集保序落地（表内声明序，生成器按 id 升序写）：`Set.copyOf` 会改成哈希序，
				// 让下游按 owner 展开的接取/交付路由顺序在两次运行间不稳定（P0c-53 实测：18742 的接取
				// 路由顺序由升序变成 206380/206379/206378，客户端契约测试按列表比较即红）。
				// The member id set keeps its insertion order (the table's declaration order, written
				// ascending by id): Set.copyOf would switch to hash order and make the per-owner accept
				// routes' order unstable across runs (P0c-53: quest 18742's routes flipped to
				// 206380/206379/206378, which breaks the list-comparing client contract test).
				groups.put(key, Collections.unmodifiableSet(new LinkedHashSet<>(ids)));
			}
		}
		return new RetailNpcNameIndex(byName, families, groupMembers, groups);
	}

	/**
	 * 为 {@code NPC_xxx} 形式的 {@code name_desc} 补 {@code xxx} 别名；已有精确同名条目时保留精确条目。
	 * Adds the {@code NPC_}-stripped alias for prefixed names; an exact name always wins.
	 */
	private static void addStrippedPrefixAliases(Map<String, Set<Integer>> byName) {
		Map<String, Set<Integer>> aliases = new LinkedHashMap<>();
		byName.forEach((name, ids) -> {
			if (name.startsWith(NPC_PREFIX) && name.length() > NPC_PREFIX.length()
				&& !byName.containsKey(name.substring(NPC_PREFIX.length()))) {
				aliases.put(name.substring(NPC_PREFIX.length()), ids);
			}
		});
		byName.putAll(aliases);
	}

	/**
	 * 击杀目标等价集：每个 npc_id 加上与它同客户端显示名（同 {@code name_id}）的其它 npc_id。
	 * <p>
	 * 用于把真端表里的目标 id 展开成本服实际可击杀的 id 集合；族未知或族过大时只返回原 id。
	 * Kill-target equivalence set: each npc id plus the other ids sharing its display name.
	 */
	public Set<Integer> withDisplayNameVariants(Collection<Integer> npcIds) {
		Set<Integer> expanded = new LinkedHashSet<>();
		for (int npcId : npcIds) {
			expanded.add(npcId);
			Set<Integer> family = byDisplayNameFamily.get(npcId);
			if (family != null) {
				expanded.addAll(family);
			}
		}
		return Set.copyOf(expanded);
	}

	/** 解析一组名字；未解析到的名字单独返回。 / Resolves names and reports the unresolved ones. */
	public Resolution resolveAll(List<String> names) {
		Set<Integer> ids = new LinkedHashSet<>();
		Set<String> unresolved = new LinkedHashSet<>();
		for (String name : names) {
			Set<Integer> hit = resolve(name);
			if (hit.isEmpty()) {
				unresolved.add(name);
			} else {
				ids.addAll(hit);
			}
		}
		return new Resolution(ids, unresolved);
	}

	/** 解析结果。 / Result of resolving a name list. */
	public record Resolution(Set<Integer> npcIds, Set<String> unresolvedNames) {
	}
}
