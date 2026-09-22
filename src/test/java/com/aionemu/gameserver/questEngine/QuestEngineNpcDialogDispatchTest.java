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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证 NPC 对话在 questId==0 时按 legacy 引擎的通用顺序派发。
 * Verifies the generic legacy-ordered NPC dialog dispatch for questId==0.
 * <p>legacy 引擎（参考实现 /Users/mc/IdeaProjects/AionEmu 的 QuestEngine.onDialog）在 questId==0 时
 * 按 NPC 任务顺序逐个尝试，让第一个真正处理该动作的 owner 胜出；任何候选都不会被“必须先在任务列表
 * 里选一次”挡掉。本仓库保留同一通用规则，并让客户端可见/进行中/已由任务列表行选择的任务优先。</p>
 * <p>The legacy engine (reference implementation: QuestEngine.onDialog in /Users/mc/IdeaProjects/AionEmu)
 * tries the NPC's quests in order for questId==0 and lets the first owner that actually handles the
 * action win; no candidate is blocked behind a quest-row requirement. This repository keeps that
 * generic rule and prefers client-visible, live, or quest-row-selected quests first.</p>
 */
class QuestEngineNpcDialogDispatchTest {
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
			npcTemplate(REFERENCE_ITEM_NPC_TEMPLATE_ID, "ai"));
	}

	@AfterEach
	void tearDown() {
		DataManager.NPC_DATA = originalNpcData;
	}

	/**
	 * 203949：1370/1371（未接取普通任务）与 1373（未接取 IMPORTANT）共用 1012 时，
	 * 可见的 1373 优先，未接取普通任务仍排在后面继续被尝试。
	 * 203949: when unaccepted normal quests 1370/1371 share 1012 with unaccepted IMPORTANT 1373 the
	 * visible 1373 is preferred, and the unaccepted normal quests still stay in the dispatch order.
	 */
	@Test
	void prefersClientVisibleOwnersAndStillDispatchesUnacceptedNormalQuests() throws Exception {
		QuestEngine engine = engineWithCatalog(productionXml(1370, 1371, 1373));
		Player player = player();
		Npc npc = dialogNpc();

		assertEquals("QUEST", engine.questCatalog().findMetadata(1370).orElseThrow().category());
		assertEquals("QUEST", engine.questCatalog().findMetadata(1371).orElseThrow().category());
		assertEquals("IMPORTANT", engine.questCatalog().findMetadata(1373).orElseThrow().category());
		assertNull(player.getQuestStateList().getQuestState(1373));

		assertEquals(List.of(1373, 1370, 1371), engine.npcDialogDispatchOwners(player, npc,
			new QuestEvent.TalkToNpc(NPC_TEMPLATE_ID, QuestDialogAction.SELECT1_1.id(), NPC_OBJECT_ID)));
	}

	/**
	 * 任务列表行选择过的任务排在最前，其余可见 owner 与未接取普通任务依次跟上。
	 * A quest-row-selected quest is dispatched first; the other visible owners and the unaccepted
	 * normal quests follow.
	 */
	@Test
	void dispatchesTheRowSelectedQuestFirst() throws Exception {
		QuestEngine engine = engineWithCatalog(productionXml(1370, 1373));
		Player player = player();
		Npc npc = dialogNpc();

		player.rememberNpcQuestDialogSelection(NPC_OBJECT_ID, 1370);

		assertEquals(List.of(1370, 1373), engine.npcDialogDispatchOwners(player, npc,
			new QuestEvent.TalkToNpc(NPC_TEMPLATE_ID, QuestDialogAction.SELECT1_1.id(), NPC_OBJECT_ID)));
	}

	/**
	 * 参考实现中的同类 NPC：730019 上未接取普通任务 1321/1322 与 IMPORTANT 1320/1478 共用 1012，
	 * 可见的 1320/1478 优先，普通任务仍在派发序列里，因此点击一定会被服务到。
	 * Reference-shaped NPC: on 730019 the unaccepted normal quests 1321/1322 share 1012 with IMPORTANT
	 * 1320/1478; the visible owners are preferred and the normal quests remain dispatchable, so the click
	 * is always served.
	 */
	@Test
	void servesTheVisibleQuestChainOnTheReferenceEltnenQuestNpc() throws Exception {
		QuestEngine engine = engineWithCatalog(productionXml(1320, 1321, 1322, 1478));
		Player player = player();
		Npc npc = dialogNpc(REFERENCE_NPC_TEMPLATE_ID);

		assertEquals(List.of(1320, 1478, 1321, 1322), engine.npcDialogDispatchOwners(player, npc,
			new QuestEvent.TalkToNpc(REFERENCE_NPC_TEMPLATE_ID, QuestDialogAction.SELECT1_1.id(),
				NPC_OBJECT_ID)));
	}

	/**
	 * 通用性关键用例：候选只有未接取普通任务时，它们依然进入派发序列（单任务场景就是 legacy 行为）。
	 * Key genericity case: when every matching candidate is an unaccepted normal quest they still enter
	 * the dispatch order, which matches the legacy engine for the single-quest case.
	 */
	@Test
	void dispatchesLoneUnacceptedNormalQuestsInsteadOfDeadEnding() throws Exception {
		QuestEngine engine = engineWithCatalog(new ImmutableQuestCatalog(List.of(
			talkOwner(1370, NPC_TEMPLATE_ID, "QUEST", QuestStatus.NONE,
				QuestDialogAction.SELECT1_1.id()))));
		Player player = player();
		Npc npc = dialogNpc();

		assertEquals(List.of(1370), engine.npcDialogDispatchOwners(player, npc,
			new QuestEvent.TalkToNpc(NPC_TEMPLATE_ID, QuestDialogAction.SELECT1_1.id(), NPC_OBJECT_ID)));
	}

	/**
	 * 1006 合同：同一动作上存在已激活的 MISSION owner 时它优先，未接取普通任务仍排在后面。
	 * Quest 1006 contract: a live MISSION owner is preferred on the colliding action while the
	 * unaccepted normal quest stays in the tail of the dispatch order.
	 */
	@Test
	void prefersTheActiveMissionOwnerOnTheCollidingAction() throws Exception {
		QuestEngine engine = engineWithCatalog(new ImmutableQuestCatalog(List.of(
			talkOwner(1006, 790_001, "MISSION", QuestStatus.REWARD, QuestDialogAction.QUEST_SELECT.id()),
			talkOwner(1123, 790_001, "QUEST", QuestStatus.REWARD, QuestDialogAction.QUEST_SELECT.id()))));
		Player player = playerWithState(1006, QuestStatus.REWARD);
		Npc npc = dialogNpc(790_001);

		assertNull(player.getQuestStateList().getQuestState(1123));
		assertEquals(List.of(1006, 1123), engine.npcDialogDispatchOwners(player, npc,
			new QuestEvent.TalkToNpc(790_001, QuestDialogAction.QUEST_SELECT.id(), NPC_OBJECT_ID)));
	}

	/**
	 * 兼容边界：候选只有未接取的其他类别任务时，它同样先于普通任务被派发。
	 * Compatibility boundary: a lone unaccepted other-category candidate is dispatched ahead of any
	 * normal quest as well.
	 */
	@Test
	void prefersALoneUnacceptedOtherCategoryCandidate() throws Exception {
		QuestEngine engine = engineWithCatalog(productionXml(1373));

		assertEquals(List.of(1373), engine.npcDialogDispatchOwners(player(), dialogNpc(),
			new QuestEvent.TalkToNpc(NPC_TEMPLATE_ID, QuestDialogAction.SELECT1_1.id(), NPC_OBJECT_ID)));
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
			try (InputStream input = QuestEngineNpcDialogDispatchTest.class.getResourceAsStream(resource)) {
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
