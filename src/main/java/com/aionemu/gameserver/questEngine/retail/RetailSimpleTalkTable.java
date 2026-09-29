package com.aionemu.gameserver.questEngine.retail;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 真端 SimpleTalk 模板表（Map/XML/Quest_SimpleTalk.xml，入仓副本 UTF-8）只读视图。
 * <p>
 * 每行 = 接取 NPC 名 + 报告 NPC 名（必填），外加可选轴：
 * 对话链 {@code talk_npcN}（N=1..3）、物品轴 {@code give_item}/{@code remove_itemN}/{@code item_check}、
 * 前置 {@code con_quest}、过场 {@code cutsceneid1}/{@code cs1_haction}。
 * 这些轴决定 ScriptDLL64 的三条 helper（{@code FUN_180cab520} 单步 /
 * {@code FUN_180cabb10} 状态链 / {@code FUN_180caca90} 报告）走哪条分支。
 * <p>
 * Read-only view of the retail SimpleTalk template table.
 */
public final class RetailSimpleTalkTable {

	/** 真端表允许的最大对话链步数（talk_npc1..talk_npcN）。 / Highest talk-chain step the table uses. */
	private static final int MAX_STEPS = 3;

	private final Map<Integer, Entry> entries;

	private RetailSimpleTalkTable(Map<Integer, Entry> entries) {
		this.entries = Map.copyOf(entries);
	}

	public Optional<Entry> find(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}

	public int size() {
		return entries.size();
	}

	/** 全部任务 ID（归属门禁与族扫描用）。 / All quest ids, for ownership gates. */
	public Set<Integer> questIds() {
		return entries.keySet();
	}

	/**
	 * 解析真端模板表。文件带内部 DTD（实体是纯文本替换），因此允许内部子集、禁止外部访问。
	 * Parses the retail table; it carries an internal DTD subset with text-only entities.
	 */
	public static RetailSimpleTalkTable load(InputStream input) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Document document = factory.newDocumentBuilder().parse(input);
			Map<Integer, Entry> entries = new HashMap<>();
			NodeList nodes = document.getElementsByTagName("id");
			for (int index = 0; index < nodes.getLength(); index++) {
				Node node = nodes.item(index);
				if (!(node instanceof Element element)) {
					continue;
				}
				String raw = element.getAttribute("id");
				if (raw == null || !raw.chars().allMatch(Character::isDigit)) {
					continue;
				}
				Entry entry = parseEntry(element, Integer.parseInt(raw));
				if (entry != null) {
					entries.put(entry.questId(), entry);
				}
			}
			return new RetailSimpleTalkTable(entries);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("failed to parse retail SimpleTalk table", e);
		}
	}

	private static Entry parseEntry(Element element, int questId) {
		String acquired = text(element, "acquired_npc_name");
		String reward = text(element, "reward_npc_name");
		if (acquired == null && reward == null) {
			return null;
		}
		List<String> steps = new ArrayList<>(MAX_STEPS);
		for (int step = 1; step <= MAX_STEPS; step++) {
			String npc = text(element, "talk_npc" + step);
			if (npc != null) {
				steps.add(npc);
			}
		}
		boolean givesItem = text(element, "give_item") != null || text(element, "give_item1") != null;
		boolean removesItem = text(element, "remove_item1") != null
			|| text(element, "remove_item2") != null
			|| text(element, "remove_item3") != null;
		return new Entry(questId, acquired, reward, List.copyOf(steps), givesItem, removesItem,
			text(element, "item_check") != null, text(element, "con_quest"),
			intValue(element, "cutsceneid1"), intValue(element, "cs1_haction"),
			text(element, "give_item"), RetailGrantKind.of(acquired));
	}

	/** 严格整数轴：真端表机器生成，畸形值必须炸出来而非静默忽略。 / Strict int axis; malformed values fail loudly. */
	private static int intValue(Element parent, String tag) {
		String value = text(parent, tag);
		return value == null ? -1 : Integer.parseInt(value);
	}

	private static String text(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		if (nodes.getLength() == 0) {
			return null;
		}
		String value = nodes.item(0).getTextContent();
		return value == null || value.isBlank() ? null : value.trim();
	}

	/**
	 * 一个任务行。 / One retail quest row.
	 *
	 * @param acquiredNpc      接取 NPC 名（可为真端类别哨兵，见 {@link #grantKind()}）
	 * @param talkNpcs         对话链上的中间 NPC（talk_npc1..N，按序；空表示单步）
	 * @param itemCheck        真端要求报告时校验任务物品
	 * @param conQuest         真端表声明的后续任务（与 quest.xml 的 finished_quest_condN 互补）
	 * @param cutsceneMovieId  过场轴 movie id（{@code cutsceneid1}；-1 = 无过场）
	 * @param cutsceneTrigger  过场触发动作 id（{@code cs1_haction}，QuestDialogAction 值域；-1 = 无触发，
	 *                         如 13800 传送门行——过场由传送触发，机制未定留档）
	 * @param grantKind        接取名类别（NPC 名或三类系统发放哨兵）/ acquire-name kind
	 */
	public record Entry(int questId, String acquiredNpc, String rewardNpc, List<String> talkNpcs,
			boolean givesItem, boolean removesItem, boolean itemCheck, String conQuest,
			int cutsceneMovieId, int cutsceneTrigger,
			String giveItemSymbol, RetailGrantKind grantKind) {

		/** 是否为单步形态（无对话链）。 / Whether the row is the single-step shape. */
		public boolean singleStep() {
			return talkNpcs.isEmpty();
		}

		/** 是否声明过场轴（P0c-10k/10n：movie id 在真端表，无外部登记）。 / Whether the row declares a cutscene. */
		public boolean cutscene() {
			return cutsceneMovieId >= 0;
		}
	}
}
