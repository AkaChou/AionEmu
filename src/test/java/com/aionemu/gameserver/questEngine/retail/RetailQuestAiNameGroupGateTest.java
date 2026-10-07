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
 * 守卫面：①组表本身的数据不变式（成员可解析、同组共享 title_id、组名精确通道不与声明冲突——
 * 精确命中只能为空或与声明集等集，见 {@link #assertExactChannelAgreesWithGroup}）——
 * 逐行覆盖**全表**（P0c-53 起表由数据判据生成：DD 引用的名字 ∧ 客户端声明 ≥2 成员 ∧ 组名非成员
 * 自己的名字）；①′通道优先级（组名优先于名字形态展开：族宽于声明时取声明集）；②绑定侧行为
 * （组名解析为全组成员并打标，普通名仍是单值且不打标）；③通道不外溢（未声明的名字照旧未解析，
 * 组通道不参与步内 npc / 采集目标解析）。
 * <p>
 * Permanent gate for the retail dialog-name group channel: table invariants over every declared row
 * (members resolve, one title_id per group, the group name's exact channel never disagrees with the
 * declaration — an exact hit is either absent or identical to the declared set), channel
 * precedence (a declared group outranks name-morphology expansion), binding behaviour (a declared
 * group resolves to every member and is flagged; plain names stay single-valued and unflagged) and
 * the channel's containment (undeclared names stay unresolved; the channel never widens step or
 * target resolution).
 */
class RetailQuestAiNameGroupGateTest {

	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
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
			assertExactChannelAgreesWithGroup(group, ids);
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
			// 全表编制不变式（P0c-53 口径修正）：组由**客户端声明**裁定，成员可跨显示名角色——
			// GAb1_0X_VillageYY_Guard 族（Disilgot Espionaut/Commander/Vanguard 三职）共用一个
			// 对话名，全表普查 76 组中 16 组为多 title_id；HousingManager_Li/Da（功能性住房管理员）
			// 全组成员无 title_id。故「同组共享 title_id」只对首代同名变体族（上方固定 GROUPS 清单）
			// 成立，不能作全表判据；全表判据 = 组内 title 存在性一致（要么全带、要么全不带——混编
			// 意味着有不相干的 NPC 被并进组）。
			// Whole-table invariant (P0c-53 correction): the group is adjudicated by the client
			// declaration and its members may span display roles — the GAb1 guard family shares one
			// dialog name across the Espionaut/Commander/Vanguard roles (16 of 76 groups carry
			// multiple title_ids) while the functional housing managers carry none. The shared-title
			// rule only holds for the first same-name variant batch (the fixed GROUPS list above);
			// the whole-table rule is that title presence is uniform within a group (mixing titled
			// and untitled members would mean an unrelated NPC joined the group).
			int titled = 0;
			for (String member : members) {
				if (!titleIds.getOrDefault(member, "").isBlank()) {
					titled++;
				}
			}
			int titledMembers = titled;
			assertTrue(titledMembers == 0 || titledMembers == members.size(),
				() -> group + " 成员 title 存在性混编（" + titledMembers + "/" + members.size()
					+ " 带 title）：" + members);
			assertExactChannelAgreesWithGroup(group, ids);
			assertEquals(ids, index.resolveAllOrQuestAiNameGroup(group),
				() -> group + " 组通道解析结果与成员集不一致");
			// P8 重锚：旧 IR 计划（RetailSimpleHuntPlan.bind）的组展开即此解析通道的直通调用，
			// 绑定面由解析器通道断言本身覆盖；live 运行时的接取/交付登记由
			// QuestEngineNpcDialogDispatchTest 与各家族门承担。
			// P8 re-anchor: the old IR plan's group expansion was a pass-through of this very resolver
			// channel, so the binding side is covered by the resolver assertions; live runtime
			// registration is held by QuestEngineNpcDialogDispatchTest and the family gates.
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

	/** ②绑定侧行为：组名 → 全组成员（解析器通道）；同族单名行仍是单值且不打组标记。 / Binding side. */
	@Test
	void bindingExpandsGroupsOnlyWhenDeclared() {
		for (String group : GROUPS) {
			int questId = questIdOf(group);
			Set<Integer> members = index.resolveQuestAiNameGroup(group);
			// P8 重锚：绑定面走解析器通道（旧 IR 计划的展开即其直通调用）——
			// 组解析 = 全成员、统一通道取声明集、组名不命中 spawn 名。
			// P8 re-anchor: the binding side goes through the resolver channel (the old IR plan's
			// expansion was its pass-through) — group resolution equals the member set, the unified
			// channel takes the declared set, and the group name never matches a spawn name.
			assertFalse(members.isEmpty(), () -> group + " 组解析为空");
			assertEquals(members, index.resolveAllOrQuestAiNameGroup(group),
				() -> group + " 统一通道未取声明成员集");
			assertExactChannelAgreesWithGroup(group, members);
			assertTrue(index.isQuestAiNameGroup(group), () -> group + " 未打组标记");
		}
		// 同族单名行（09_L2 / 09_D2）：精确解析为唯一 id、不打组标记，组通道不得外溢。
		// The single-name siblings (09_L2 / 09_D2): exact single resolution, unflagged, no spillover.
		for (String single : List.of("LDF4_Advance_Village_Guard09_L2", "LDF4_Advance_Village_Guard09_D2")) {
			assertFalse(index.isQuestAiNameGroup(single), () -> single + " 不应打组标记");
			assertEquals(1, index.resolve(single).size(), () -> single + " 应解析为唯一 spawn 名");
			assertEquals(1, index.resolveAll(List.of(single)).npcIds().size(),
				() -> single + " 统一通道不得展开为多值");
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

	/**
	 * 通道一致性：组名的**精确通道**命中要么为空（组名本身不是 spawn 名），要么与声明成员集**等集**。
	 * <p>
	 * 等集收敛是合法的：版本化别名表（{@code retail-npc-name-aliases.xml}，2026-09-30「追加真端逻辑名」
	 * 批）先于组表承载了同名事实，P0c-53 组表（客户端声明通道）落地后，{@code gab1_sub_fuen_e} /
	 * {@code housingmanager_li|da} / {@code npc_event_svs_jabsuroong} / {@code npc_event_devasday_shugo}
	 * 五条为两表同集重复——加载器对此明确容忍（「等集重复 = 同一事实已由既有条目承载……id 集不一致
	 * 才是真数据冲突」，见 {@code RetailNpcNameIndex.addVersionedNpcIdAliases}）。精确命中给出**不同**
	 * 集合才是真冲突：那会让「先精确」静默遮蔽客户端声明（统一通道的优先级前提）。
	 * <p>
	 * Channel consistency: the group name's exact channel must either miss or be identical to the
	 * declared member set. Identical-set convergence is legitimate — the versioned alias ledger
	 * carried five of these names before the P0c-53 client-declared group table did, and the loader
	 * explicitly tolerates same-set duplication. Only a *different* set is a real conflict: it would
	 * let "exact first" silently shadow the client declaration.
	 */
	private static void assertExactChannelAgreesWithGroup(String group, Set<Integer> declaredIds) {
		Set<Integer> exact = index.resolveAll(List.of(group)).npcIds();
		assertTrue(exact.isEmpty() || declaredIds.equals(exact),
			() -> group + " 精确通道命中 " + exact + "，既非空集也不等于声明成员集 " + declaredIds
				+ "（通道互斥性被破坏）");
	}

	private static int questIdOf(String group) {
		// 组名 → 该组首个 DD 行（L 阵营 13758..，D 阵营 23758..；方位序 North/East/West/South 与表序一致）。
		// Group name to its first DD row (light 13758.., dark 23758..; district order matches the table).
		int base = group.contains("_D_") ? 23758 : 13758;
		return base + GROUPS.indexOf(group) % 4 * 3;
	}

	private static List<InputStream> openAll(String dir, List<String> files) throws Exception {
		List<InputStream> inputs = new ArrayList<>();
		for (String file : files) {
			inputs.add(open(dir + file));
		}
		return inputs;
	}

	/** 组表全量读取（`组名 \t 成员1,成员2`；`#` 起头为注释）。 / Every declared group row. */
	private static Map<String, List<String>> declaredGroups() {
		return RetailQuestAiNameGroups.defaultGroups();
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
