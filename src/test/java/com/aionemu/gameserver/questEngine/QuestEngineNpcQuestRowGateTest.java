package com.aionemu.gameserver.questEngine;

import com.aionemu.gameserver.ai2.AITemplate;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 NPC 任务行门控与 legacy 引擎一致的 questId==0 对话派发。
 * Verifies the NPC quest-row gate and the legacy-parity questId==0 dialog dispatch.
 *
 * <p>legacy 引擎（参考实现）在 questId==0 时按 NPC 任务顺序逐个尝试，让第一个真正处理该动作的
 * owner 胜出。只有关闭普通任务标记后客户端不可见的未接取普通任务必须由任务列表行授权，
 * 非普通任务类别（IMPORTANT/MISSION 等）与进行中任务照常参与派发。</p>
 * <p>The legacy engine (reference implementation) tries the NPC's quests in order for questId==0 and
 * lets the first owner that actually handles the action win. Only unaccepted normal quests, which the
 * client cannot see once the normal-quest marker is disabled, require a quest-list row; other
 * categories (IMPORTANT/MISSION and so on) and live quests keep taking part.</p>
 */
class QuestEngineNpcQuestRowGateTest {
	private static final int NPC_TEMPLATE_ID = 203_949;
	private static final int REFERENCE_NPC_TEMPLATE_ID = 730_019;
	private static final int REFERENCE_ITEM_NPC_TEMPLATE_ID = 730_039;
	private static final int NPC_OBJECT_ID = 36_770;
	private static final int PLAYER_ID = 7;

	private NpcData originalNpcData;

	@BeforeEach
	void setUp() throws Exception {
		originalNpcData = DataManager.NPC_DATA;
		DataManager.NPC_DATA = new NpcData();
		// 1371 的 ACTION_ITEM_USE 模板需要非 quest_use_item 的 AI 才能通过启动校验。
		// Quest 1371's ACTION_ITEM_USE template needs a non quest_use_item AI to pass startup validation.
		DataManager.NPC_DATA.getNpcData().put(REFERENCE_ITEM_NPC_TEMPLATE_ID,
			npcTemplate(REFERENCE_ITEM_NPC_TEMPLATE_ID, "general"));
	}

	@AfterEach
	void tearDown() {
		DataManager.NPC_DATA = originalNpcData;
	}

	/**
	 * 203949：1370/1371（未接取普通任务）与 1373（未接取 IMPORTANT）共用 1012/1007 时，
	 * 客户端可见的 1373 放行门控，而未接取的普通任务不参与 questId==0 派发。
	 * 203949: when unaccepted normal quests 1370/1371 share 1012/1007 with unaccepted IMPORTANT 1373,
	 * the gate must stay open for the client-visible owner while hidden normal quests stay out.
	 */
	@Test
	void releasesTheGateForClientVisibleQuestsAndKeepsHiddenNormalQuestsOutOfDispatch() throws Exception {
		QuestEngine engine = engineWithCatalog(productionXml(1370, 1371, 1373));
		Player player = player();
		Npc npc = dialogNpc();

		assertEquals("QUEST", engine.questCatalog().findMetadata(1370).orElseThrow().category());
		assertEquals("QUEST", engine.questCatalog().findMetadata(1371).orElseThrow().category());
		assertEquals("IMPORTANT", engine.questCatalog().findMetadata(1373).orElseThrow().category());
		assertNull(player.getQuestStateList().getQuestState(1373));

		assertFalse(engine.requiresNpcQuestRowSelection(player, npc, 0, QuestDialogAction.SELECT1_1.id()));
		assertFalse(engine.requiresNpcQuestRowSelection(player, npc, 0, QuestDialogAction.ASK_QUEST_ACCEPT.id()));
		assertTrue(engine.requiresNpcQuestRowSelection(player, npc, 1370, QuestDialogAction.SELECT1_1.id()));
		assertEquals(List.of(1373), engine.eligibleNpcDialogOwners(player, npc,
			new QuestEvent.TalkToNpc(NPC_TEMPLATE_ID, QuestDialogAction.SELECT1_1.id(), NPC_OBJECT_ID)));
	}

	/**
	 * 任务行授权（31 + questId）后，被授权的普通任务与可见类别一起按任务 ID 顺序参与派发。
	 * After the quest-row authorization (31 + questId) the authorized normal quest joins the dispatch
	 * next to the client-visible owner, ordered by quest id.
	 */
	@Test
	void dispatchesTheRowAuthorizedNormalQuestAlongsideTheVisibleOwner() throws Exception {
		QuestEngine engine = engineWithCatalog(productionXml(1370, 1373));
		Player player = player();
		Npc npc = dialogNpc();

		player.rememberNpcQuestDialogSelection(NPC_OBJECT_ID, 1370);

		assertFalse(engine.requiresNpcQuestRowSelection(player, npc, 0, QuestDialogAction.SELECT1_1.id()));
		assertFalse(engine.requiresNpcQuestRowSelection(player, npc, 1370,
			QuestDialogAction.ASK_QUEST_ACCEPT.id()));
		assertEquals(List.of(1370, 1373), engine.eligibleNpcDialogOwners(player, npc,
			new QuestEvent.TalkToNpc(NPC_TEMPLATE_ID, QuestDialogAction.SELECT1_1.id(), NPC_OBJECT_ID)));
	}

	/**
	 * 参考实现中的同类 NPC：730019 上未接取普通任务 1321/1322 与 IMPORTANT 1320/1478 共用 1012，
	 * 客户端可见的 1320/1478 继续派发，普通任务不参与。
	 * Reference-shaped NPC: on 730019 the unaccepted normal quests 1321/1322 share 1012 with IMPORTANT
	 * 1320/1478, so the client-visible owners keep being dispatched and the normal quests stay out.
	 */
	@Test
	void servesTheVisibleQuestChainOnTheReferenceEltnenQuestNpc() throws Exception {
		QuestEngine engine = engineWithCatalog(productionXml(1320, 1321, 1322, 1478));
		Player player = player();
		Npc npc = dialogNpc(REFERENCE_NPC_TEMPLATE_ID);

		assertFalse(engine.requiresNpcQuestRowSelection(player, npc, 0, QuestDialogAction.SELECT1_1.id()));
		assertEquals(List.of(1320, 1478), engine.eligibleNpcDialogOwners(player, npc,
			new QuestEvent.TalkToNpc(REFERENCE_NPC_TEMPLATE_ID, QuestDialogAction.SELECT1_1.id(),
				NPC_OBJECT_ID)));
	}

	/**
	 * 1006 合同：同一动作上存在已激活的 MISSION owner 时，未授权普通任务不得把它回退成任务列表。
	 * Quest 1006 contract: a live MISSION owner on the same action must keep the gate open for the
	 * colliding unauthorized normal quest.
	 */
	@Test
	void keepsTheGateOpenForAnActiveOtherCategoryOwner() throws Exception {
		QuestEngine engine = engineWithCatalog(new ImmutableQuestCatalog(List.of(
			talkOwner(1006, 790_001, "MISSION", QuestStatus.REWARD, QuestDialogAction.QUEST_SELECT.id()),
			talkOwner(1123, 790_001, "QUEST", QuestStatus.REWARD, QuestDialogAction.QUEST_SELECT.id()))));
		Player player = playerWithState(1006, QuestStatus.REWARD);
		Npc npc = dialogNpc(790_001);

		assertNull(player.getQuestStateList().getQuestState(1123));
		assertFalse(engine.requiresNpcQuestRowSelection(player, npc, 0, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(1006), engine.eligibleNpcDialogOwners(player, npc,
			new QuestEvent.TalkToNpc(790_001, QuestDialogAction.QUEST_SELECT.id(), NPC_OBJECT_ID)));
	}

	/**
	 * 纯边界：匹配候选只有未接取普通任务时，门控要求任务列表行选择，且没有可派发的 owner。
	 * Boundary: when every matching candidate is an unaccepted normal quest the gate requires a quest
	 * row and no owner is dispatchable.
	 */
	@Test
	void requiresRowSelectionWhenEveryMatchingCandidateIsAnUnacceptedNormalQuest() throws Exception {
		QuestEngine engine = engineWithCatalog(new ImmutableQuestCatalog(List.of(
			talkOwner(1370, NPC_TEMPLATE_ID, "QUEST", QuestStatus.NONE,
				QuestDialogAction.SELECT1_1.id()))));
		Player player = player();
		Npc npc = dialogNpc();

		assertTrue(engine.requiresNpcQuestRowSelection(player, npc, 0, QuestDialogAction.SELECT1_1.id()));
		assertEquals(List.of(), engine.eligibleNpcDialogOwners(player, npc,
			new QuestEvent.TalkToNpc(NPC_TEMPLATE_ID, QuestDialogAction.SELECT1_1.id(), NPC_OBJECT_ID)));
	}

	/**
	 * 兼容边界：候选只有未接取的其他类别任务时保持原行为，不额外要求任务行选择。
	 * Compatibility boundary: a lone unaccepted other-category candidate keeps the previous behavior and
	 * does not force a quest-row selection.
	 */
	@Test
	void doesNotForceRowSelectionForALoneUnacceptedOtherCategoryCandidate() throws Exception {
		QuestEngine engine = engineWithCatalog(productionXml(1373));

		assertFalse(engine.requiresNpcQuestRowSelection(player(), dialogNpc(), 0,
			QuestDialogAction.SELECT1_1.id()));
	}

	private static QuestEngine engineWithCatalog(QuestCatalog catalog) {
		QuestEngine engine = new QuestEngine();
		engine.installProductionDefinitions(catalog);
		return engine;
	}

	private static QuestCatalog productionXml(int... questIds) throws Exception {
		List<CompiledQuestDefinition> definitions = new ArrayList<>();
		for (int questId : questIds) {
			String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
			try (InputStream input = QuestEngineNpcQuestRowGateTest.class.getResourceAsStream(resource)) {
				if (input == null) {
					throw new IllegalStateException("missing quest definition " + resource);
				}
				definitions.add(QuestDefinitionXmlCompiler.compile(input));
			}
		}
		return new ImmutableQuestCatalog(definitions);
	}

	private static CompiledQuestDefinition talkOwner(int questId, int npcId, String category,
			QuestStatus status, int actionId) {
		return quest(questId)
			.metadata(QuestMetadata.minimal("quest-" + questId, 0, category))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(status, vars("var0", 0)))
			.on(new QuestEvent.TalkToNpc(npcId, actionId))
			.from("unaccepted").goTo("unaccepted")
			.compile();
	}

	private static Player player() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "questStateList", new QuestStateList());
		return player;
	}

	private static Player playerWithState(int questId, QuestStatus status) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		QuestStateList states = new QuestStateList();
		states.addQuest(questId, new QuestState(questId, status, 0, 0, null, null, null));
		setField(Player.class, player, "questStateList", states);
		return player;
	}

	private static Npc dialogNpc() throws Exception {
		return dialogNpc(NPC_TEMPLATE_ID);
	}

	private static Npc dialogNpc(int npcId) throws Exception {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		setField(AionObject.class, npc, "objectId", NPC_OBJECT_ID);
		NpcTemplate template = new DialogNpcTemplate();
		setField(NpcTemplate.class, template, "npcId", npcId);
		npc.setObjectTemplate(template);
		npc.setAi2(new NamedAi("normal"));
		return npc;
	}

	private static NpcTemplate npcTemplate(int npcId, String ai) throws Exception {
		NpcTemplate template = new NpcTemplate();
		setField(NpcTemplate.class, template, "npcId", npcId);
		setField(NpcTemplate.class, template, "ai", ai);
		return template;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value)
			throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class DialogNpcTemplate extends NpcTemplate {
		@Override
		public boolean isDialogNpc() {
			return true;
		}
	}

	private static final class NamedAi extends AITemplate {
		private final String name;

		private NamedAi(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}
}
