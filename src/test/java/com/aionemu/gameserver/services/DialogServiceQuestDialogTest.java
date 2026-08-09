package com.aionemu.gameserver.services;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai.QuestItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2;
import com.aionemu.gameserver.ai2.AITemplate;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DialogServiceQuestDialogTest {
	private static final int PLAYER_ID = 7;
	private static final int NPC_OBJECT_ID = 900_007;
	private static final int QUEST_ID = 1109;

	private GameEngineServices engineServices;

	@BeforeAll
	static void configurePacketProcessor() {
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = 1;
	}

	@BeforeEach
	void installUnhandledQuestEngine() {
		engineServices = new GameEngineServices(provider(new UnhandledQuestEngine()), null, null, null, null);
	}

	@AfterEach
	void clearEngineProvider() {
		engineServices.destroy();
	}

	@Test
	void activeNpcQuestStartDialogSendsOneQuestPage() throws Exception {
		Player player = playerWithQuest(QuestStatus.START);

		DialogService.onDialogSelect(31, player, npc(new NamedAi("normal")), QUEST_ID, 0);

		assertOnlyDialog(player, 10);
	}

	@Test
	void activeUseItemQuestStartDialogSendsOneClosePage() throws Exception {
		Player player = playerWithQuest(QuestStatus.START);

		DialogService.onDialogSelect(31, player, npc(new ActionItemNpcAI2()), QUEST_ID, 0);

		assertOnlyDialog(player, 0);
	}

	@Test
	void activeQuestInteractionItemStartDialogSendsOneClosePage() throws Exception {
		Player player = playerWithQuest(QuestStatus.START);

		DialogService.onDialogSelect(31, player, npc(new QuestItemNpcAI2()), QUEST_ID, 0);

		assertOnlyDialog(player, 0);
	}

	private static Player playerWithQuest(QuestStatus status) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "playerAccount", new Account(1));
		QuestStateList states = new QuestStateList();
		states.addQuest(QUEST_ID, new QuestState(QUEST_ID, status, 0, 0, null, null, null));
		setField(Player.class, player, "questStateList", states);
		setField(Player.class, player, "clientConnection", packetConnection());
		return player;
	}

	private static Npc npc(AI2 ai) throws Exception {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		setField(AionObject.class, npc, "objectId", NPC_OBJECT_ID);
		npc.setObjectTemplate(new NpcTemplate());
		npc.setAi2(ai);
		return npc;
	}

	private static AionConnection packetConnection() throws Exception {
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		RecordingTransport transport = new RecordingTransport();
		transport.connection = connection;
		setField(AConnection.class, connection, "transport", transport);
		setField(AConnection.class, connection, "guard", new Object());
		setField(AionConnection.class, connection, "sendMsgQueue", new ArrayList<AionServerPacket>());
		return connection;
	}

	private static void assertOnlyDialog(Player player, int expectedPage) throws Exception {
		List<AionServerPacket> packets = packetQueue(player.getClientConnection());
		assertEquals(1, packets.size());
		SM_DIALOG_WINDOW dialog = assertInstanceOf(SM_DIALOG_WINDOW.class, packets.getFirst());
		assertEquals(NPC_OBJECT_ID, intField(SM_DIALOG_WINDOW.class, dialog, "targetObjectId"));
		assertEquals(expectedPage, intField(SM_DIALOG_WINDOW.class, dialog, "dialogID"));
		assertEquals(0, intField(SM_DIALOG_WINDOW.class, dialog, "questId"));
	}

	@SuppressWarnings("unchecked")
	private static List<AionServerPacket> packetQueue(AionConnection connection) {
		try {
			Field field = AionConnection.class.getDeclaredField("sendMsgQueue");
			field.setAccessible(true);
			return (List<AionServerPacket>) field.get(connection);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static int intField(Class<?> declaringClass, Object target, String name) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(target);
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value)
			throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static org.springframework.beans.factory.ObjectProvider<QuestEngine> provider(QuestEngine engine) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(QuestEngine.class.getName(), engine);
		return beanFactory.getBeanProvider(QuestEngine.class);
	}

	private static final class UnhandledQuestEngine extends QuestEngine {
		@Override
		public boolean onDialog(QuestEnv env) {
			return false;
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

	private static final class RecordingTransport implements ConnectionTransport {
		private AionConnection connection;

		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		@Override
		public void enableWriteInterest() {
			packetQueue(connection).getLast();
		}

		@Override
		public void close(boolean forced) {
		}

		@Override
		public boolean onlyClose() {
			return true;
		}
	}
}
