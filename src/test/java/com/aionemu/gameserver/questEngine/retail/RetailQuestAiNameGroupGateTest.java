package com.aionemu.gameserver.questEngine.retail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 常设门：真端对话名组通道（{@code quest_ai_name} 组表）。
 * <p>
 * 守卫面：①组表本身的数据不变式（成员可解析、同组共享 title_id、组名与 spawn 名互斥）——
 * 逐行覆盖**全表**（P0c-53 起表由数据判据生成：DD 引用的名字 ∧ 客户端声明 ≥2 成员 ∧ 组名非成员
 * 自己的名字）；①′通道优先级（组名优先于名字形态展开：族宽于声明时取声明集）；②绑定侧行为
 * （组名解析为全组成员并打标，普通名仍是单值且不打标）；③通道不外溢（未声明的名字照旧未解析，
 * 组通道不参与步内 npc / 采集目标解析）。
 * <p>
 * Permanent gate for the retail dialog-name group channel: table invariants over every declared row
 * (members resolve, one title_id per group, group names disjoint from spawn names), channel
 * precedence (a declared group outranks name-morphology expansion), binding behaviour (a declared
 * group resolves to every member and is flagged; plain names stay single-valued and unflagged) and
 * the channel's containment (undeclared names stay unresolved; the channel never widens step or
 * target resolution).
 */
class RetailQuestAiNameGroupGateTest {

	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final String GROUP_TABLE =
		"/aion/data/static_data/quest_retail/retail-quest-ai-name-groups.tsv";
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");
	/** 组表声明的组名（P0c-52 批：LDF4 前进基地守备队四方位 × 双阵营）。 / The declared group names. */
	private static final List<String> GROUPS = List.of(
		"LDF4_Advance_Village_Guard_L_North", "LDF4_Advance_Village_Guard_L_East",
		"LDF4_Advance_Village_Guard_L_West", "LDF4_Advance_Village_Guard_L_South",
		"LDF4_Advance_Village_Guard_D_North", "LDF4_Advance_Village_Guard_D_East",
		"LDF4_Advance_Village_Guard_D_West", "LDF4_Advance_Village_Guard_D_South");
	private static final Pattern TEMPLATE = Pattern.compile("<npc_template\\b([^>]*)>");
	private static final Pattern NAME_DESC = Pattern.compile("name_desc=\"([^\"]*)\"");
	private static final Pattern TITLE_ID = Pattern.compile("title_id=\"([^\"]*)\"");

	private static RetailNpcNameIndex index;
	private static Map<String, String> titleIds = new LinkedHashMap<>();

	@BeforeAll
	static void setUp() throws Exception {
		index = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_TEMPLATES),
			RetailQuestAiNameGroupsFixture.streams());
		for (String file : NPC_TEMPLATES) {
			String text = new String(open(NPC_DIR + file).readAllBytes(), StandardCharsets.UTF_8);
			Matcher template = TEMPLATE.matcher(text);
			while (template.find()) {
				String tag = template.group(1);
				Matcher name = NAME_DESC.matcher(tag);
				Matcher title = TITLE_ID.matcher(tag);
				if (name.find() && title.find()) {
					titleIds.put(name.group(1), title.group(1));
				}
			}
		}
	}

	/** ①数据不变式：成员全解析、同组共享一个 title_id、组名与 spawn 名互斥。 / Table invariants. */
	@Test
	void groupTableInvariantsHold() {
		for (String group : GROUPS) {
			assertTrue(index.isQuestAiNameGroup(group), () -> group + " 未在组表声明");
			Set<String> members = index.questAiNameGroupMembers(group);
			Set<Integer> ids = index.resolveQuestAiNameGroup(group);
			assertTrue(members.size() >= 2, () -> group + " 成员数 < 2：" + members);
			assertEquals(members.size(), ids.size(),
				() -> group + " 成员模板未全解析：" + members + " -> " + ids);
			Set<String> titles = new TreeSet<>();
			members.forEach(member -> titles.add(titleIds.getOrDefault(member, "?")));
			assertEquals(1, titles.size(),
				() -> group + " 成员 title_id 不唯一（真端编制不变式）：" + titles);
			assertTrue(index.resolveAll(List.of(group)).npcIds().isEmpty(),
				() -> group + " 同时命中 spawn 名（通道互斥性被破坏）");
			assertEquals(ids, index.resolveAllOrQuestAiNameGroup(group),
				() -> group + " 组通道解析结果与成员集不一致");
		}
	}

	/** ①′全表不变式 + 绑定：每一行声明的组都满足编制不变式，且 hunt 计划两轴取到成员集并打标。 */
	@Test
	void everyDeclaredGroupSatisfiesTheTableInvariants() throws Exception {
		Map<String, List<String>> table = declaredGroups();
		assertTrue(table.size() >= 31, () -> "组表规模回退：" + table.size());
		for (Map.Entry<String, List<String>> declared : table.entrySet()) {
			String group = declared.getKey();
			List<String> members = declared.getValue();
			assertTrue(members.size() >= 2, () -> group + " 成员数 < 2：" + members);
			assertTrue(index.isQuestAiNameGroup(group), () -> group + " 未在组表声明");
			Set<Integer> ids = index.resolveQuestAiNameGroup(group);
			assertEquals(members.size(), ids.size(), () -> group + " 成员模板未全解析：" + members);
			Set<String> titles = new TreeSet<>();
			members.forEach(member -> titles.add(titleIds.getOrDefault(member, "?")));
			assertEquals(1, titles.size(),
				() -> group + " 成员 title_id 不唯一（真端编制不变式）：" + titles);
			assertTrue(index.resolveAll(List.of(group)).npcIds().isEmpty(),
				() -> group + " 同时命中 spawn 名（通道互斥性被破坏）");
			assertEquals(ids, index.resolveAllOrQuestAiNameGroup(group),
				() -> group + " 组通道解析结果与成员集不一致");
			RetailSimpleHuntPlan plan = bindPlan(1, group);
			assertEquals(ids, plan.acquiredNpcIds(), () -> group + " 接取集与成员集不一致");
			assertEquals(ids, plan.rewardNpcIds(), () -> group + " 交付集与成员集不一致");
			assertTrue(plan.acquiredNpcIsQuestAiNameGroup() && plan.rewardNpcIsQuestAiNameGroup(),
				() -> group + " 接取/交付两侧的组标记缺失");
		}
	}

	/**
	 * ①″通道优先级：**声明优先于名字形态**——族宽于声明时统一通道必须给声明集。
	 * （P0c-53 的落地证据：光明侧副本台阶 6→3、金星商人 5→2，两处收窄都由客户端 npc 块的
	 * {@code quest_ai_name} 声明背书；本判据把它常设化，防止形态展开再把阵营镜像/邻组 NPC 绑回来。）
	 * Channel precedence: a declared group outranks name-morphology expansion, pinned whenever a
	 * variant family is wider than the declaration (the P0c-53 narrowing evidence).
	 */
	@Test
	void groupDeclarationOutranksNameMorphology() throws Exception {
		int pinned = 0;
		for (String group : declaredGroups().keySet()) {
			Set<Integer> members = index.resolveQuestAiNameGroup(group);
			Set<Integer> variants = index.resolveVariants(group);
			if (variants.isEmpty() || variants.equals(members)) {
				continue;
			}
			pinned++;
			Set<Integer> declaredMembers = members;
			Set<Integer> morphology = variants;
			assertEquals(declaredMembers, index.resolvePartyName(group),
				() -> group + " 统一通道必须取声明集，而形态展开给了 " + morphology);
			assertTrue(morphology.containsAll(declaredMembers),
				() -> group + " 形态展开集应包含声明集（族⊇声明）");
		}
		int pinnedGroups = pinned;
		assertTrue(pinnedGroups >= 2, () -> "本判据退化为空断言（族宽于声明的组应至少两个）：" + pinnedGroups);
	}

	/** ②绑定侧行为：组名 → 全组成员 + 打标；同族单名行仍是单值且不打标。 / Binding side. */
	@Test
	void bindingExpandsGroupsOnlyWhenDeclared() {
		for (String group : GROUPS) {
			RetailSimpleHuntPlan groupPlan = bindPlan(questIdOf(group), group);
			assertTrue(groupPlan.acquiredNpcIsQuestAiNameGroup(),
				() -> group + " 接取侧未打组标记");
			assertTrue(groupPlan.rewardNpcIsQuestAiNameGroup(),
				() -> group + " 交付侧未打组标记");
			assertEquals(index.resolveQuestAiNameGroup(group), groupPlan.acquiredNpcIds(),
				() -> group + " 接取集与成员集不一致");
			assertEquals(index.resolveQuestAiNameGroup(group), groupPlan.rewardNpcIds(),
				() -> group + " 交付集与成员集不一致");
		}
		// 同族单名行（09_L2 / 09_D2）：精确解析、不打标，组通道不得外溢。
		// The single-name siblings (09_L2 / 09_D2): exact resolution, unflagged, no group spillover.
		for (String single : List.of("LDF4_Advance_Village_Guard09_L2", "LDF4_Advance_Village_Guard09_D2")) {
			RetailSimpleHuntPlan plan = bindPlan(single.endsWith("_L2") ? 13770 : 23770, single);
			assertFalse(plan.acquiredNpcIsQuestAiNameGroup(), () -> single + " 不应打组标记");
			assertFalse(plan.rewardNpcIsQuestAiNameGroup(), () -> single + " 不应打组标记");
			assertEquals(1, plan.acquiredNpcIds().size(), () -> single + " 接取集应为单值");
			assertEquals(1, plan.rewardNpcIds().size(), () -> single + " 交付集应为单值");
		}
	}

	/** ③通道不外溢：未声明的名字既不是组也不解析；组通道不参与步内/目标名解析。 / Containment. */
	@Test
	void channelDoesNotWidenOtherNames() {
		for (String undeclared : List.of("LDF4_Advance_Village_Guard_L_North2", "LDF5_Village_Guard10_L",
				"Ab1_Village_Guard50_L")) {
			assertFalse(index.isQuestAiNameGroup(undeclared), () -> undeclared + " 不应被当作组名");
			assertTrue(index.resolveQuestAiNameGroup(undeclared).isEmpty(),
				() -> undeclared + " 不应有组解析结果");
		}
		// 组员名本身仍是普通 spawn 名（组通道只认得组名，不给组员加别名）。
		// The members stay plain spawn names: the channel knows the group name only, adding no aliases.
		for (String member : List.of("LDF4_Advance_Village_Guard13_L2", "LDF4_Advance_Village_Guard12_D2")) {
			assertFalse(index.isQuestAiNameGroup(member), () -> member + " 不应被当作组名");
			assertEquals(1, index.resolve(member).size(), () -> member + " 应是唯一 spawn 名");
		}
	}

	private static int questIdOf(String group) {
		// 组名 → 该组首个 DD 行（L 阵营 13758..，D 阵营 23758..；方位序 North/East/West/South 与表序一致）。
		// Group name to its first DD row (light 13758.., dark 23758..; district order matches the table).
		int base = group.contains("_D_") ? 23758 : 13758;
		return base + GROUPS.indexOf(group) % 4 * 3;
	}

	private static RetailSimpleHuntPlan bindPlan(int questId, String name) {
		return RetailSimpleHuntPlan.bind(new RetailSimpleHuntTable.Entry(questId,
			List.of(new RetailSimpleHuntTable.Counter(1, 1, List.of(""))), name, name, null,
			RetailGrantKind.NPC), index);
	}

	private static List<InputStream> openAll(String dir, List<String> files) throws Exception {
		List<InputStream> inputs = new ArrayList<>();
		for (String file : files) {
			inputs.add(open(dir + file));
		}
		return inputs;
	}

	/** 组表全量读取（`组名 \t 成员1,成员2`；`#` 起头为注释）。 / Every declared group row. */
	private static Map<String, List<String>> declaredGroups() throws Exception {
		Map<String, List<String>> groups = new LinkedHashMap<>();
		for (String line : lines(open(GROUP_TABLE))) {
			if (line.isBlank() || line.startsWith("#")) {
				continue;
			}
			String[] parts = line.split("\t");
			assertEquals(2, parts.length, () -> "组表行列数异常：" + line);
			groups.put(parts[0], List.of(parts[1].split(",")));
		}
		return groups;
	}

	private static List<String> lines(InputStream input) throws Exception {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return reader.lines().toList();
		}
	}

	private static InputStream open(String resource) throws Exception {
		InputStream input = RetailQuestAiNameGroupGateTest.class.getResourceAsStream(resource);
		if (input == null) {
			throw new IllegalStateException("missing resource " + resource);
		}
		return input;
	}
}
