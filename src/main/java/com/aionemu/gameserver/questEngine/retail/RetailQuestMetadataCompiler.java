package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.tablelane.NativeNpcFactionNames;
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.QuestRewardGroup;
import com.aionemu.gameserver.questEngine.definition.QuestStartCondition;
import com.aionemu.gameserver.questEngine.definition.QuestStartConditionGroup;
import com.aionemu.gameserver.questEngine.definition.RepeatPolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 原版 quest.xml 行 → {@link QuestMetadata} 的映射器（"原版元数据层"的编译核心）。
 * <p>
 * 关键映射口径（全部经全库对拍验证，见 .agents/summary/scriptdll-quest-driver/）：
 * <ul>
 * <li>name：原版只有 {@code Qxxxx} 符号名；英文名为在库人工资产且运行时无消费方（登记的全局口径分歧）；</li>
 * <li>displayNameId：客户端字符串表 id（{@code quest_name_string_ids.xml}，已验证与生产一致）；</li>
 * <li>等级/种族/性别/分类/重复策略/前置/交付/掉落/奖励/职业奖励：直接映射，符号名经
 * {@link RetailNpcNameIndex}/{@link RetailItemNameIndex}/随机组表解析；</li>
 * <li>称号奖励：{@link RetailQuestTitleIds} 快照；maxLevel：原版 0/998/999=无上限
 * （与生产 82 封顶轴的差异由既有 cap-exceptions 门禁登记）；</li>
 * <li>职业：原版 token → PlayerClass；base token 在 min-level ≥ 10 时展开为进阶线；
 * 16-token 全集 = 通配。</li>
 * </ul>
 * 无法解析的符号名进入 {@link Outcome#unresolved()}，调用方据此降级该任务到 XML。
 * <p>
 * Compiles a retail quest.xml row into quest metadata; unresolvable symbol names are reported so
 * the caller can fall back to XML for that quest.
 */
public final class RetailQuestMetadataCompiler {

	/** 奖励组内 kind 的规范排序（决定 selectable 索引，迁移语义需稳定）。 / Canonical reward kind order. */
	private static final List<String> KIND_ORDER = List.of("GOLD", "KINAH", "EXP", "AP", "GP", "DP", "CP",
		"EXP_BOOST", "ABYSS_OP", "TITLE", "ITEM", "RANDOM", "SELECTABLE_ITEM");

	/** 原版职业 token → PlayerClass（来自既有 class 轴审计，cleric/priest 互换已复核）。 / Retail token to class. */
	private static final Map<String, String> CLASS_TOKENS = Map.ofEntries(
		Map.entry("warrior", "WARRIOR"), Map.entry("fighter", "GLADIATOR"), Map.entry("knight", "TEMPLAR"),
		Map.entry("scout", "SCOUT"), Map.entry("assassin", "ASSASSIN"), Map.entry("ranger", "RANGER"),
		Map.entry("mage", "MAGE"), Map.entry("wizard", "SORCERER"), Map.entry("elementallist", "SPIRIT_MASTER"),
		Map.entry("cleric", "PRIEST"), Map.entry("priest", "CLERIC"), Map.entry("chanter", "CHANTER"),
		Map.entry("engineer", "TECHNIST"), Map.entry("gunner", "GUNSLINGER"), Map.entry("rider", "AETHERTECH"),
		Map.entry("artist", "MUSE"), Map.entry("bard", "SONGWEAVER"));

	/** base token → 两条进阶线（min-level ≥ 10 时展开）。 / Base token to its advanced lines. */
	private static final Map<String, List<String>> BASE_CLASSES = Map.of(
		"warrior", List.of("GLADIATOR", "TEMPLAR"), "scout", List.of("ASSASSIN", "RANGER"),
		"mage", List.of("SORCERER", "SPIRIT_MASTER"), "cleric", List.of("CLERIC", "CHANTER"),
		"engineer", List.of("GUNSLINGER", "AETHERTECH"), "artist", List.of("SONGWEAVER"));

	/**
	 * 职业奖励标签 → class id。原版表对三个技术职业用**短名**标签（{@code gunner/bard/rider}），
	 * 与生产 class-rewards 的长名（GUNSLINGER/SONGWEAVER/AETHERTECH）并存（全库 90 任务均为
	 * 短名+长名成对出现），两套别名映射到同一 class id。
	 * Class reward tag to class id. The retail table spells the three technician classes with
	 * short tags (gunner/bard/rider) alongside the long production names; both map to the same id.
	 */
	private static final Map<String, String> CLASS_REWARD_TAGS = Map.ofEntries(
		Map.entry("fighter_selectable_reward", "FIGHTER"), Map.entry("knight_selectable_reward", "KNIGHT"),
		Map.entry("ranger_selectable_reward", "RANGER"), Map.entry("assassin_selectable_reward", "ASSASSIN"),
		Map.entry("wizard_selectable_reward", "WIZARD"), Map.entry("elementalist_selectable_reward", "ELEMENTALIST"),
		Map.entry("priest_selectable_reward", "PRIEST"), Map.entry("chanter_selectable_reward", "CHANTER"),
		Map.entry("gunslinger_selectable_reward", "GUNSLINGER"),
		Map.entry("songweaver_selectable_reward", "SONGWEAVER"),
		Map.entry("aethertech_selectable_reward", "AETHERTECH"),
		Map.entry("gunner_selectable_reward", "GUNSLINGER"), Map.entry("bard_selectable_reward", "SONGWEAVER"),
		Map.entry("rider_selectable_reward", "AETHERTECH"));

	/**
	 * 原版工艺技能名 → 生产数字 id（全库投票无歧义；{@code any} 无在库对应，见分歧台账）。
	 * Retail craft-skill name to the production numeric id (library-wide unambiguous votes).
	 */
	/**
	 * 原版 CombineTask 表的 {@code combineskill} 符号名 → 技能 id（包内共享，供合成器复用同一张表）。
	 * Retail craft-skill symbol to skill id; shared package-wide so the synthesizer reuses one table.
	 */
	static final Map<String, Integer> COMBINE_SKILLS = Map.of(
		"weaponsmith", 40002, "armorsmith", 40003, "handiwork", 40008, "alchemy", 40007, "tailoring", 40004,
		"cooking", 40001, "menuisier", 40010, "gathering_b", 30002, "aerial_gathering", 30003);

	/**
	 * 原版 NPC 势力名 → 势力 id（全库投票无歧义；原版 Shugo 在库无 npc-faction-id，映射为 0）。
	 * 原版家族表的奖励引用 {@code <地图>_<势力名>} 复合名靠它识别（合成器共用）。
	 * Retail NPC-faction name to faction id (library-wide unambiguous votes); shared with the
	 * synthesizer to recognize {@code <map>_<faction>} reward references.
	 */
	static final Map<String, Integer> NPC_FACTIONS = NativeNpcFactionNames.all();

	/**
	 * 奖励名是否为 {@code <地图>_<势力名>} 复合引用（去掉首个下划线前缀后是已知原版势力名；
	 * {@code _LD} 双侧变体按同势力 {@code _L}/{@code _D} 识别）。系统发放行常用它代替真实交付 NPC 名。
	 * Whether the reward name is a {@code <map>_<faction>} composite over a known retail faction;
	 * two-side {@code _LD} variants count via their {@code _L}/{@code _D} bases.
	 */
	static boolean isFactionComposite(String rewardNpc) {
		if (rewardNpc == null) {
			return false;
		}
		int split = rewardNpc.indexOf('_');
		if (split <= 0 || split == rewardNpc.length() - 1) {
			return false;
		}
		String suffix = rewardNpc.substring(split + 1);
		if (NPC_FACTIONS.containsKey(suffix)) {
			return true;
		}
		if (!suffix.endsWith("_LD")) {
			return false;
		}
		String base = suffix.substring(0, suffix.length() - "_LD".length());
		return NPC_FACTIONS.containsKey(base + "_L") || NPC_FACTIONS.containsKey(base + "_D");
	}

	/** 扩展奖励 _ext 系列的最大槽位。 / Highest *_ext slot the retail table uses. */
	private static final int MAX_EXT_SLOT = 16;

	/**
	 * 掉落死 id 修复：名单全部未实刷时并入同显示名实刷兄弟（M2-c 同判据）；有活 id 或无实刷信息
	 * （空集）时原样返回——宝箱判例（700127/700188 同 name_id 分属不同任务掉落）禁止有活 id 时的扩展。
	 * Dead-id drop repair: attach same-display-name live siblings only when every resolved id is
	 * unspawned; returned unchanged when any id is live or no spawn info (empty set) exists — the
	 * jewel-box case (700127/700188 share a name_id across different quests' drops) forbids
	 * expansion whenever a live id resolves.
	 */
	private static Set<Integer> liveRepair(RetailNpcNameIndex npcs, Set<Integer> resolved,
			Set<Integer> spawnedNpcIds) {
		if (resolved.isEmpty() || spawnedNpcIds == null || spawnedNpcIds.isEmpty()) {
			return resolved;
		}
		for (int npcId : resolved) {
			if (spawnedNpcIds.contains(npcId)) {
				return resolved;
			}
		}
		Set<Integer> live = new TreeSet<>();
		for (int npcId : npcs.withDisplayNameVariants(resolved)) {
			if (spawnedNpcIds.contains(npcId)) {
				live.add(npcId);
			}
		}
		return live.isEmpty() ? resolved : live;
	}

	private RetailQuestMetadataCompiler() {
	}

	/**
	 * 编译结果：元数据 + 未解析符号名清单（非空即不可迁移）。
	 * Compilation outcome: metadata plus unresolved symbol names (non-empty means not migratable).
	 */
	public record Outcome(QuestMetadata metadata, List<String> unresolved) {

		public boolean clean() {
			return unresolved.isEmpty();
		}
	}

	/**
	 * 把原版 quest.xml 行编译成任务元数据。
	 * Compiles a retail quest.xml row into quest metadata.
	 * @param entry 原版行 / retail row
	 * @param npcs NPC 名索引 / npc name index
	 * @param items 物品名索引 / item name index
	 * @param randomRewardIds {@code %随机奖励组名} → 组 id（来自 quest_random_rewards.xml）
	 * @param nameStringIds 任务 id → 客户端字符串表 name id（displayNameId 来源）
	 */
	public static Outcome compile(RetailQuestXmlTable.Entry entry, RetailNpcNameIndex npcs,
			RetailItemNameIndex items, Map<String, Integer> randomRewardIds, Map<Integer, Integer> nameStringIds) {
		return compile(entry, npcs, items, randomRewardIds, nameStringIds, Set.of());
	}

	/**
	 * 带实刷 id 集的编译（生产驱动入口）：掉落 npc 的死 id 修复以世界实刷为准（见 {@link #liveRepair}）。
	 * Compiles with the spawned-id set (the production-driver entry): dead-id drop repair follows
	 * world spawn presence (see {@link #liveRepair}).
	 */
	public static Outcome compile(RetailQuestXmlTable.Entry entry, RetailNpcNameIndex npcs,
			RetailItemNameIndex items, Map<String, Integer> randomRewardIds, Map<Integer, Integer> nameStringIds,
			Set<Integer> spawnedNpcIds) {
		List<String> unresolved = new ArrayList<>();
		int questId = entry.questId();

		int minLevel = orZero(entry.integer("minlevel_permitted"));
		int retailMax = orZero(entry.integer("maxlevel_permitted"));
		int maxLevel = retailMax == 0 || retailMax == 998 || retailMax == 999 ? Integer.MAX_VALUE : retailMax;
		int maxRepeat = Math.max(1, orZero(entry.integer("max_repeat_count")));
		Integer rewardRepeatRaw = entry.integer("reward_repeat_count");
		// 与生产 XML 编译器同规则：无显式 reward_repeat_count 时，255（无限）取 0，其余取 maxRepeat。
		// Same default as the production XML compiler: 0 for unlimited (255), else maxRepeat.
		int rewardRepeat = rewardRepeatRaw == null ? (maxRepeat < 255 ? maxRepeat : 0)
			: Math.max(0, rewardRepeatRaw);
		Set<String> cycles = cycles(entry);
		boolean daily = cycles.contains("ALL");
		boolean weekly = !daily && !cycles.isEmpty();
		RepeatPolicy repeat = new RepeatPolicy(maxRepeat, rewardRepeat, orZero(entry.integer("quest_cooltime")),
			daily, weekly);

		Set<Integer> prerequisites = new TreeSet<>();
		List<QuestStartCondition> startConditions = new ArrayList<>();
		boolean hasNonFinishedFamilies = !entry.numbered("unfinished_quest_cond").isEmpty()
			|| !entry.numbered("noacquired_quest_cond").isEmpty() || !entry.numbered("acquired_quest_cond").isEmpty();
		// 归属规则：带 reward-mode 后缀、或与非 finished 条件族共存 → start-conditions(finished)；
		// 否则 → prerequisites（与生产两种表达语义对齐，分歧在登记表按类登记）。
		// Suffixed or grouped conds become finished start conditions; plain ones become prerequisites.
		for (String raw : entry.numbered("finished_quest_cond")) {
			for (String token : raw.split("[\\s,]+")) {
				if (token.isBlank()) {
					continue;
				}
				int colon = token.indexOf(':');
				int prerequisiteId = questRef(token);
				if (colon >= 0 || hasNonFinishedFamilies) {
					startConditions.add(new QuestStartCondition("finished", prerequisiteId,
						parseRewardMode(prerequisiteId, token, colon)));
				} else {
					prerequisites.add(prerequisiteId);
				}
			}
		}
		startConditions.addAll(condFamily(entry, "unfinished_quest_cond", "unfinished"));
		startConditions.addAll(condFamily(entry, "noacquired_quest_cond", "noacquired"));
		startConditions.addAll(condFamily(entry, "acquired_quest_cond", "acquired"));

		List<QuestItemRequirement> itemRequirements = new ArrayList<>();
		for (String raw : entry.numbered("collect_item")) {
			addItemRequirement(itemRequirements, raw, items, unresolved);
		}

		List<QuestDrop> drops = new ArrayList<>();
		// 原版掉落族带下划线（drop_monster_1 / drop_item_1 / drop_prob_1 / drop_each_member_1）。
		// The retail drop family carries an underscore between the base and the slot number.
		for (int slot = 1; slot <= maxNumberedSlot(entry, "drop_monster_"); slot++) {
			List<String> monsters = splitNames(entry.text("drop_monster_" + slot));
			String dropItem = entry.text("drop_item_" + slot);
			if (monsters.isEmpty() || dropItem == null) {
				continue;
			}
			Integer itemId = items.resolve(dropItem);
			if (itemId == null) {
				unresolved.add("drop_item" + slot + ":" + dropItem);
				continue;
			}
			int chance = orZero(entry.integer("drop_prob_" + slot));
			// 原版缺省 drop_each_member 时按生产约定视为 true（GROUP 掉落）。
			// A missing retail drop_each_member defaults to true (GROUP drops) per production.
			Integer eachMemberRaw = entry.integer("drop_each_member_" + slot);
			boolean eachMember = eachMemberRaw == null || eachMemberRaw > 0;
			// 生产 collecting-step = 原版任务级 collect_progress（掉落生效的交付步）。
			// The production collecting-step equals the quest-level retail collect_progress.
			int collectingStep = orZero(entry.integer("collect_progress"));
			for (String monster : monsters) {
				// 掉落 npc = 精确解析；仅当名单内 id 全部未实刷时，按 M2-c 同判据并入同显示名的
				// 实刷兄弟 id（2631 实证：原版模板 id 213775 已退役、实刷 236924 才是世界体）。
				// 有活 id 的名单不扩展——不同任务的同显示名箱子互不归属（700127/700188 同
				// name_id 350769 分属 2119/1561 的掉落契约，盲并会交叉污染）。
				// Drop npcs resolve exactly; only when EVERY resolved id is unspawned do we attach
				// the same-display-name live siblings (quest 2631: template id 213775 is retired,
				// 236924 is the live world body). Rows with a live id never expand — same-display-name
				// boxes of different quests own different drops (700127/700188 share name_id 350769).
				Set<Integer> npcIds = liveRepair(npcs, npcs.resolve(monster), spawnedNpcIds);
				if (npcIds.isEmpty()) {
					unresolved.add("drop_monster" + slot + ":" + monster);
					continue;
				}
				for (int npcId : npcIds) {
					drops.add(new QuestDrop(npcId, itemId, chance, eachMember, collectingStep));
				}
			}
		}
		drops.sort(Comparator.comparingInt(QuestDrop::npcId).thenComparingInt(QuestDrop::itemId));

		List<QuestRewardGroup> rewardGroups = rewardGroups(entry, items, randomRewardIds, unresolved);
		// 重复奖励组全等时按生产约定折叠为单组（第 2..N 次完成奖励由 repeat 语义接管）。
		// Identical repeat reward groups collapse into the first one (production convention).
		rewardGroups = collapseIdenticalGroups(rewardGroups);
		List<QuestReward> rewards = rewardGroups.stream().flatMap(group -> group.rewards().stream()).toList();
		List<QuestRewardGroup> extendedGroups = extendedRewardGroups(entry, items, unresolved);
		List<QuestReward> extendedRewards = extendedGroups.stream().flatMap(group -> group.rewards().stream())
			.toList();

		Set<String> classes = classes(entry, minLevel);
		String genderRaw = textOr(entry, "gender_permitted", "all").toLowerCase(Locale.ROOT);
		String gender = "all".equals(genderRaw) ? "" : genderRaw.toUpperCase(Locale.ROOT);

		Map<String, List<QuestReward>> classRewards = classRewards(entry, items, unresolved);
		Integer combineSkill = combineSkill(entry);
		List<QuestItemRequirement> inventoryItems = new ArrayList<>();
		for (String raw : entry.numbered("inventory_item_name")) {
			addItemRequirement(inventoryItems, raw, items, unresolved);
		}
		List<QuestItemRequirement> questWorkItems = new ArrayList<>();
		for (String raw : entry.numbered("quest_work_item")) {
			addItemRequirement(questWorkItems, raw, items, unresolved);
		}

		QuestMetadata metadata = new QuestMetadata("Q" + questId,
			nameStringIds.getOrDefault(questId, 0), minLevel, maxLevel, races(entry), category(entry), repeat,
			prerequisites, itemRequirements, rewards, List.copyOf(drops), classes, gender, 0,
			Math.max(1, orZero(entry.integer("max_count_limitedquest"))),
			Math.max(1, orZero(entry.integer("count_recover_limitedquest"))), entry.bool("cannot_share"),
			entry.bool("cannot_giveup"), false, orZero(entry.integer("use_class_reward")),
			combineSkill, entry.integer("combine_skillpoint"), entry.bool("timer"), cycles,
			npcFaction(entry), "NONE", targetType(entry), 0, inventoryItems, questWorkItems, extendedRewards,
			List.of(), List.of(),
			startConditions, classRewards, rewardGroups, extendedGroups, groupsOf(startConditions),
			orZero(entry.integer("reward_extend_stigma1")) > 0);
		return new Outcome(metadata, List.copyOf(unresolved));
	}

	private static String category(RetailQuestXmlTable.Entry entry) {
		return textOr(entry, "category1", "quest").toUpperCase(Locale.ROOT);
	}

	private static Set<String> races(RetailQuestXmlTable.Entry entry) {
		Set<String> races = new LinkedHashSet<>();
		boolean light = false;
		boolean dark = false;
		for (String token : textOr(entry, "race_permitted", "").toLowerCase(Locale.ROOT).split("[\\s,]+")) {
			light |= "pc_light".equals(token);
			dark |= "pc_dark".equals(token);
			if ("pc_all".equals(token)) {
				races.add("PC_ALL");
			}
		}
		if (light && dark) {
			races.add("PC_ALL");
		} else if (light) {
			races.add("ELYOS");
		} else if (dark) {
			races.add("ASMODIANS");
		}
		return races;
	}

	private static Set<String> cycles(RetailQuestXmlTable.Entry entry) {
		Set<String> cycles = new LinkedHashSet<>();
		for (String raw : entry.numbered("quest_repeat_cycle")) {
			for (String token : raw.split("[\\s,]+")) {
				if (!token.isBlank()) {
					cycles.add(token.toUpperCase(Locale.ROOT));
				}
			}
		}
		// 该标签无槽位后缀（quest_repeat_cycle 本体）。 / The tag itself carries no slot suffix.
		String plain = entry.text("quest_repeat_cycle");
		if (plain != null) {
			for (String token : plain.split("[\\s,]+")) {
				if (!token.isBlank()) {
					cycles.add(token.toUpperCase(Locale.ROOT));
				}
			}
		}
		return cycles;
	}

	private static Set<String> classes(RetailQuestXmlTable.Entry entry, int minLevel) {
		return permittedClassNames(entry.text("class_permitted"), minLevel);
	}

	/**
	 * 原版 {@code class_permitted} 词表 → 允许的 {@code PlayerClass} 名集合（空集 = 不限职业）。
	 * 规则与生产元数据同源：≥16 token = 全集通配；基础职业在最低等级 ≥ 10 时展开为两条进阶线；
	 * 未登记 token 忽略。原生接取端口复用本方法，避免第二套职业轴。
	 * <p>
	 * Retail {@code class_permitted} tokens → the permitted {@code PlayerClass} names (empty means
	 * unrestricted). The rule matches the production metadata: a 16-token set is a wildcard, base
	 * classes expand into their two advanced lines at minimum level 10, and unknown tokens are ignored.
	 * The native acquisition port reuses this method instead of keeping a second class axis.
	 */
	public static Set<String> permittedClassNames(String raw, int minLevel) {
		if (raw == null) {
			return Set.of();
		}
		List<String> tokens = new ArrayList<>(List.of(raw.split("[\\s,]+")));
		if (tokens.size() >= 16) {
			return Set.of();
		}
		Set<String> classes = new TreeSet<>();
		for (String token : tokens) {
			if (token.isBlank()) {
				continue;
			}
			String mapped = CLASS_TOKENS.get(token);
			if (mapped == null) {
				continue;
			}
			List<String> advanced = BASE_CLASSES.get(token);
			if (advanced != null && minLevel >= 10) {
				classes.addAll(advanced);
			} else {
				classes.add(mapped);
			}
		}
		return classes;
	}

	private static String targetType(RetailQuestXmlTable.Entry entry) {
		// 原版 battlegroup 在生产 XML 从未出现（该轴在登记表按类记录）；其余按枚举名直映。
		// Retail battlegroup never appears in production XML; the other values map by enum name.
		String raw = entry.text("target_type");
		if (raw == null || "battlegroup".equals(raw)) {
			return "NONE";
		}
		return raw.toUpperCase(Locale.ROOT);
	}

	/** 原版 NPC 势力名 → 势力 id；未知名映射为 0（与生产缺省一致）。 / Maps faction names, else 0. */
	private static int npcFaction(RetailQuestXmlTable.Entry entry) {
		String name = entry.text("npcfaction_name");
		return name == null ? 0 : NPC_FACTIONS.getOrDefault(name.trim(), 0);
	}

	/** 原版工艺技能名 → 数字 id；{@code any} 与未知名按生产缺省置空。 / Maps a craft-skill name, else null. */
	private static Integer combineSkill(RetailQuestXmlTable.Entry entry) {
		String raw = entry.text("combineskill");
		if (raw == null || "any".equals(raw.trim())) {
			return null;
		}
		return COMBINE_SKILLS.get(raw.trim());
	}

	/**
	 * 原版 CombineTask 表的技能符号名 → 技能 id；{@code any} 与未知名返回 null。native CombineTask
	 * 车道（{@code SimpleCombineTaskHandler}）复用同一张表，故本方法对包外可见。
	 * Maps a CombineTask table craft-skill symbol to its id; {@code any} and unknown names yield null.
	 * The native CombineTask lane reuses this very table, so the accessor is public.
	 */
	public static Integer combineSkillId(String raw) {
		if (raw == null) {
			return null;
		}
		String name = raw.trim();
		if (name.isEmpty() || "any".equals(name)) {
			return null;
		}
		return COMBINE_SKILLS.get(name);
	}

	private static List<QuestStartCondition> condFamily(RetailQuestXmlTable.Entry entry, String base, String type) {
		List<QuestStartCondition> conditions = new ArrayList<>();
		for (String raw : entry.numbered(base)) {
			for (String token : raw.split("[\\s,]+")) {
				if (!token.isBlank()) {
					int colon = token.indexOf(':');
					int questId = questRef(token);
					int rewardMode = parseRewardMode(questId, token, colon);
					conditions.add(new QuestStartCondition(type, questId, rewardMode));
				}
			}
		}
		return conditions;
	}

	/**
	 * 开始条件的奖励分支模式（{@code Q1007:1} → 0；无后缀 → 0）：与生产元数据同源，
	 * 原生接取端口复用同一解析，避免第二套实现。
	 * <p>
	 * The reward-mode index of a start condition ({@code Q1007:1} → 0; no suffix → 0), sharing the
	 * production metadata parse so the native acquisition port keeps a single implementation.
	 */
	public static int prerequisiteRewardMode(int questId, String token) {
		if (token == null) {
			return 0;
		}
		int colon = token.indexOf(':');
		return colon < 0 ? 0 : parseRewardMode(questId, token, colon);
	}

	/**
	 * 解析开始条件的奖励分支模式（0 基对齐）。
	 * Parses the reward-mode index aligned to zero-based engine storage.
	 */
	private static int parseRewardMode(int questId, String token, int colon) {
		if (colon < 0) {
			return 0;
		}
		int raw = Integer.parseInt(token.substring(colon + 1));
		if (questId == 1007 || questId == 2009) {
			// NCSoft 原版 quest.xml 中 Q1007:1..6 与 Q2009:1..6 对应 1 基奖励槽（reward_exp1..6：战士、斥候、法师、祭司、枪炮、乐手）；
			// AionEmu 转职仪式（1007.xml / 2009.xml）与数据库 player_quests.reward 以及 quest_data.xml 一致采用 0 基索引（0..5）。
			// Retail Q1007:1..6 / Q2009:1..6 map to 1-based reward slots in retail XML, whereas AionEmu
			// ascension rites, DB player_quests.reward, and quest_data.xml use zero-based reward indices (0..5).
			return Math.max(0, raw - 1);
		}
		return raw;
	}

	/**
	 * {@code Q1007} / {@code Q1007:1} / {@code 1007} / {@code ws_q5015}（工艺引用）→ 数字 id。
	 * Strips the Q prefix, reward-mode suffix, or recipe prefix from a quest reference.
	 */
	private static int questRef(String token) {
		String trimmed = token.trim();
		int colon = trimmed.indexOf(':');
		if (colon >= 0) {
			trimmed = trimmed.substring(0, colon);
		}
		trimmed = trimmed.startsWith("Q") || trimmed.startsWith("q") ? trimmed.substring(1) : trimmed;
		if (!trimmed.chars().allMatch(Character::isDigit)) {
			StringBuilder digits = new StringBuilder();
			for (char c : trimmed.toCharArray()) {
				if (Character.isDigit(c)) {
					digits.append(c);
				}
			}
			trimmed = digits.toString();
		}
		return Integer.parseInt(trimmed);
	}

	private static void addItemRequirement(List<QuestItemRequirement> target, String raw,
			RetailItemNameIndex items, List<String> unresolved) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		String[] tokens = raw.trim().split("\\s+");
		Integer itemId = items.resolve(tokens[0]);
		if (itemId == null) {
			unresolved.add("item:" + tokens[0]);
			return;
		}
		int count = tokens.length > 1 && tokens[1].chars().allMatch(Character::isDigit)
			? Integer.parseInt(tokens[1]) : 1;
		target.add(new QuestItemRequirement(itemId, count));
	}

	/** 奖励槽位按组收集（slot N = 第 N 次奖励组；跨组同值标量只保留首组，对齐生产扁平化约定）。 */
	private static List<QuestRewardGroup> rewardGroups(RetailQuestXmlTable.Entry entry, RetailItemNameIndex items,
			Map<String, Integer> randomRewardIds, List<String> unresolved) {
		List<QuestRewardGroup> groups = new ArrayList<>();
		int maxSlot = maxRewardSlot(entry);
		Set<String> emittedScalars = new java.util.HashSet<>();
		for (int slot = 1; slot <= maxSlot; slot++) {
			List<QuestReward> rewards = new ArrayList<>();
			addScalarOnce(rewards, "GOLD", entry.integer("reward_gold" + slot), emittedScalars);
			addScalarOnce(rewards, "EXP", entry.integer("reward_exp" + slot), emittedScalars);
			addScalarOnce(rewards, "AP", entry.integer("reward_abyss_point" + slot), emittedScalars);
			addScalarOnce(rewards, "GP", entry.integer("reward_glory_point" + slot), emittedScalars);
			addScalarOnce(rewards, "DP", entry.integer("reward_dp" + slot), emittedScalars);
			addScalarOnce(rewards, "CP", entry.integer("reward_cp" + slot), emittedScalars);
			addScalarOnce(rewards, "EXP_BOOST", entry.integer("reward_exp_boost" + slot), emittedScalars);
			addScalarOnce(rewards, "ABYSS_OP", entry.integer("reward_abyss_op_point" + slot), emittedScalars);
			Integer titleId = RetailQuestTitleIds.idOf(entry.text("reward_title" + slot));
			if (titleId != null) {
				rewards.add(new QuestReward("TITLE", titleId, 1));
			}
			for (String raw : entry.numbered("reward_item" + slot + "_")) {
				addNamedReward(rewards, raw, "ITEM", items, randomRewardIds, unresolved);
			}
			for (String raw : entry.numbered("selectable_reward_item" + slot + "_")) {
				addNamedReward(rewards, raw, "SELECTABLE_ITEM", items, randomRewardIds, unresolved);
			}
			if (!rewards.isEmpty()) {
				groups.add(new QuestRewardGroup(sortRewards(rewards)));
			}
		}
		return groups;
	}

	/** 扫描奖励字段可达到的最大槽位（空槽位保持组对齐）。 / Highest reward slot referenced by the row. */
	private static int maxRewardSlot(RetailQuestXmlTable.Entry entry) {
		int maxSlot = 0;
		for (String key : entry.fields().keySet()) {
			for (String prefix : List.of("reward_exp", "reward_gold", "reward_abyss_point", "reward_glory_point",
					"reward_dp", "reward_cp", "reward_exp_boost", "reward_abyss_op_point", "reward_title",
					"reward_item", "selectable_reward_item")) {
				if (!key.startsWith(prefix)) {
					continue;
				}
				String suffix = key.substring(prefix.length());
				int underscore = suffix.indexOf('_');
				String slotPart = underscore >= 0 ? suffix.substring(0, underscore) : suffix;
				if (!slotPart.isEmpty() && slotPart.chars().allMatch(Character::isDigit)) {
					maxSlot = Math.max(maxSlot, Integer.parseInt(slotPart));
				}
			}
		}
		return maxSlot;
	}

	private static List<QuestRewardGroup> collapseIdenticalGroups(List<QuestRewardGroup> groups) {
		if (groups.size() < 2 || !groups.stream().skip(1).allMatch(group -> group.equals(groups.get(0)))) {
			return groups;
		}
		return List.of(groups.get(0));
	}

	/** 扩展奖励（{@code *_ext} 族）单组。 / The extended-reward group from the *_ext family. */
	private static List<QuestRewardGroup> extendedRewardGroups(RetailQuestXmlTable.Entry entry,
			RetailItemNameIndex items, List<String> unresolved) {
		List<QuestReward> rewards = new ArrayList<>();
		addScalar(rewards, "GOLD", entry.integer("reward_gold_ext"));
		addScalar(rewards, "EXP", entry.integer("reward_exp_ext"));
		Integer titleId = RetailQuestTitleIds.idOf(entry.text("reward_title_ext"));
		if (titleId != null) {
			rewards.add(new QuestReward("TITLE", titleId, 1));
		}
		for (int index = 1; index <= MAX_EXT_SLOT; index++) {
			String item = entry.text("reward_item_ext_" + index);
			if (item != null) {
				addNamedReward(rewards, item, "ITEM", items, null, unresolved);
			}
			String selectable = entry.text("selectable_reward_item_ext_" + index);
			if (selectable != null) {
				addNamedReward(rewards, selectable, "SELECTABLE_ITEM", items, null, unresolved);
			}
		}
		return rewards.isEmpty() ? List.of() : List.of(new QuestRewardGroup(sortRewards(rewards)));
	}

	private static Map<String, List<QuestReward>> classRewards(RetailQuestXmlTable.Entry entry,
			RetailItemNameIndex items, List<String> unresolved) {
		Map<String, List<QuestReward>> classRewards = new TreeMap<>();
		for (Map.Entry<String, String> tag : CLASS_REWARD_TAGS.entrySet()) {
			List<QuestReward> rewards = new ArrayList<>();
			for (String raw : entry.block(tag.getKey())) {
				addNamedReward(rewards, raw, "ITEM", items, null, unresolved);
			}
			if (!rewards.isEmpty()) {
				classRewards.put(tag.getValue(), rewards);
			}
		}
		return classRewards;
	}

	/**
	 * 解析带计数的符号名奖励（{@code name [count]}；{@code %前缀}=随机奖励组）。
	 * Parses a symbolic reward with optional count; a {@code %} prefix marks a random group.
	 */
	private static void addNamedReward(List<QuestReward> target, String raw, String kind,
			RetailItemNameIndex items, Map<String, Integer> randomRewardIds, List<String> unresolved) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		String[] tokens = raw.trim().split("\\s+");
		String name = tokens[0];
		long count = tokens.length > 1 && tokens[1].chars().allMatch(Character::isDigit)
			? Long.parseLong(tokens[1]) : 1;
		if (name.startsWith("%")) {
			// 随机组缺失时按生产转换约定跳过（在库 quest_random_rewards.xml 未收录该组）。
			// Unknown random groups are dropped, matching the production conversion convention.
			Integer groupId = randomRewardIds == null ? null : randomRewardIds.get(name);
			if (groupId != null) {
				target.add(new QuestReward("RANDOM", groupId, count));
			}
			return;
		}
		Integer itemId = items.resolve(name);
		if (itemId == null) {
			unresolved.add("reward:" + name);
			return;
		}
		target.add(new QuestReward(kind, itemId, count));
	}

	private static void addScalar(List<QuestReward> target, String kind, Integer amount) {
		if (amount != null && amount != 0) {
			target.add(new QuestReward(kind, 0, amount));
		}
	}

	/** 跨组同值标量只在首组发出（对齐生产扁平化去重约定）。 / Emits a scalar once across groups. */
	private static void addScalarOnce(List<QuestReward> target, String kind, Integer amount,
			Set<String> emitted) {
		if (amount == null || amount == 0) {
			return;
		}
		if (emitted.add(kind + ":" + amount)) {
			target.add(new QuestReward(kind, 0, amount));
		}
	}

	private static List<QuestReward> sortRewards(List<QuestReward> rewards) {
		List<QuestReward> sorted = new ArrayList<>(rewards);
		// 仅按 kind 档位稳定排序：同 kind 保留原版表序（与生产奖励组顺序约定一致）。
		// Stable sort by kind only: same-kind rewards keep the retail table order.
		sorted.sort(Comparator.comparingInt((QuestReward reward) -> {
			int index = KIND_ORDER.indexOf(reward.kind());
			return index < 0 ? KIND_ORDER.size() : index;
		}));
		return List.copyOf(sorted);
	}

	private static List<QuestStartConditionGroup> groupsOf(List<QuestStartCondition> conditions) {
		return conditions.isEmpty() ? List.of() : List.of(new QuestStartConditionGroup(conditions));
	}

	private static List<String> splitNames(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		List<String> names = new ArrayList<>();
		for (String token : raw.split("[\\s,]+")) {
			if (!token.isBlank()) {
				names.add(token);
			}
		}
		return List.copyOf(names);
	}

	/** 字段族 {@code base<N>} 的最大 N（缺槽位容忍）。 / Highest N of a numbered tag family. */
	private static int maxNumberedSlot(RetailQuestXmlTable.Entry entry, String base) {
		int max = 0;
		for (String key : entry.fields().keySet()) {
			if (!key.startsWith(base)) {
				continue;
			}
			String suffix = key.substring(base.length());
			if (!suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit)) {
				max = Math.max(max, Integer.parseInt(suffix));
			}
		}
		return max;
	}

	private static String textOr(RetailQuestXmlTable.Entry entry, String tag, String fallback) {
		String value = entry.text(tag);
		return value == null ? fallback : value;
	}

	private static int orZero(Integer value) {
		return value == null ? 0 : value;
	}
}
