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
 * 验证 NPC 任务行门控只放行进行中/待领奖或已由任务列表行授权的候选。
 * Verifies the NPC quest-row gate only releases owners that are live or authorized by a quest-row click.
 *
 * <p>关闭“未满65级普通任务标记”后，5.8 客户端仍可能为未接取任务发送任务动作 ID（例如 NPC 203949
 * 的 SELECT1_1/ASK_QUEST_ACCEPT）。同一 NPC 上未接取的其他类别任务（如 IMPORTANT 1373）不能成为
 * 放行未授权普通任务的逃逸口。</p>
 * <p>With the normal-quest marker disabled the 5.8 client may still send quest action ids for unaccepted
 * quests (for example SELECT1_1/ASK_QUEST_ACCEPT on NPC 203949). An unaccepted other-category quest on
 * the same NPC (such as IMPORTANT 1373) must not become an escape hatch for an unauthorized normal quest.</p>
 */
class QuestEngineNpcQuestRowGateTest {
	private static final int NPC_TEMPLATE_ID = 203_949;
	private static final int NPC_OBJECT_ID = 36_770;
	private static final int PLAYER_ID = 7;

	private NpcData originalNpcData;

	@BeforeEach
	void setUp() {
		originalNpcData = DataManager.NPC_DATA;
		DataManager.NPC_DATA = new NpcData();
	}

	@AfterEach
	void tearDown() {
		DataManager.NPC_DATA = originalNpcData;
	}

	/**
	 * 生产 1370/1373：未勾选普通任务标记时，与动作 1012/1007 撞车的未接取 IMPORTANT 1373
	 * 不能放行未授权普通任务 1370。
	 * Production 1370/1373: with the marker disabled, the unaccepted IMPORTANT 1373 sharing actions
	 * 1012/1007 must not dispatch unauthorized normal quest 1370.
	 */
	@Test
	void blocksUnauthorizedNormalQuestWhenAnUnacceptedImportantQuestSharesTheAction() throws Exception {
		QuestEngine engine = engineWithCatalog(productionXml(1370, 1373));
		Player player = player();
		Npc npc = dialogNpc();

		assertEquals("QUEST", engine.questCatalog().findMetadata(1370).orElseThrow().category());
		assertEquals("IMPORTANT", engine.questCatalog().findMetadata(1373).orElseThrow().category());
		assertNull(player.getQuestStateList().getQuestState(1370));
		assertNull(player.getQuestStateList().getQuestState(1373));

		assertTrue(engine.requiresNpcQuestRowSelection(player, npc, 0, QuestDialogAction.SELECT1_1.id()));
		assertTrue(engine.requiresNpcQuestRowSelection(player, npc, 0, QuestDialogAction.ASK_QUEST_ACCEPT.id()));
		assertTrue(engine.requiresNpcQuestRowSelection(player, npc, 1370,
			QuestDialogAction.SELECT1_1.id()));
	}

	/**
	 * 任务行授权（31 + questId）建立会话授权后，同一动作可以进入任务上下文。
	 * Once the quest row (31 + questId) established the session authorization the same action may proceed.
	 */
	@Test
	void releasesTheGateAfterTheQuestRowAuthorizesTheSession() throws Exception {
		QuestEngine engine = engineWithCatalog(productionXml(1370, 1373));
		Player player = player();
		Npc npc = dialogNpc();

		player.rememberNpcQuestDialogSelection(NPC_OBJECT_ID, 1370);

		assertFalse(engine.requiresNpcQuestRowSelection(player, npc, 0, QuestDialogAction.SELECT1_1.id()));
		assertFalse(engine.requiresNpcQuestRowSelection(player, npc, 1370,
			QuestDialogAction.ASK_QUEST_ACCEPT.id()));
	}

	/**
	 * 1006 合同：同一动作上存在已激活的 MISSION owner 时，未授权普通任务不得把它回退成任务列表。
	 * Quest 1006 contract: a live MISSION owner on the same action must keep the gate open for the
	 * colliding unauthorized normal quest.
	 */
	@Test
	void keepsTheGateOpenForAnActiveOtherCategoryOwner() throws Exception {
		QuestEngine engine = engineWithCatalog(new ImmutableQuestCatalog(List.of(
			talkOwner(1006, "MISSION", QuestStatus.REWARD),
			talkOwner(1123, "QUEST", QuestStatus.REWARD))));
		Player player = playerWithState(1006, QuestStatus.REWARD);
		Npc npc = dialogNpc(790_001);

		assertNull(player.getQuestStateList().getQuestState(1123));
		assertFalse(engine.requiresNpcQuestRowSelection(player, npc, 0, QuestDialogAction.QUEST_SELECT.id()));
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

	private static CompiledQuestDefinition talkOwner(int questId, String category, QuestStatus status) {
		return quest(questId)
			.metadata(QuestMetadata.minimal("quest-" + questId, 0, category))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(status, vars("var0", 0)))
			.on(new QuestEvent.TalkToNpc(790_001, QuestDialogAction.QUEST_SELECT.id()))
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
