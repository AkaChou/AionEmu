package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemDialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemDialogBridge.Authorization;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemDialogBridge.ClientDialogInput;

/**
 * 验证物品对话 bridge 的图派生授权、服务端物品归属、一次性消费、过期、恢复与清理合同。
 * Verifies graph-derived authorization, server-side item ownership, one-time consumption, expiry, recovery, and cleanup
 * contracts for the item-dialog bridge.
 */
class QuestGraphItemDialogBridgeTest {

	private static final Path SCHEMA = Path.of("src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd");
	private static final int PLAYER_ID = 7;
	private static final int ITEM_ID = 182200001;
	private static final int ITEM_OBJECT_ID = 5001;
	private static final long AUTHORIZATION_ID = 91;

	@TempDir
	Path tempDir;
	private CompiledQuestGraph graph;
	private AtomicBoolean ownsItem;
	private QuestGraphItemDialogBridge bridge;

	/** 编译正式 XSD/IR fixture 并建立服务端物品归属端口。 / Compiles the formal XSD/IR fixture and creates the server item-ownership port. */
	@BeforeEach
	void setUp() throws Exception {
		Path xml = tempDir.resolve("item-dialog.xml");
		Files.writeString(xml, document(), StandardCharsets.UTF_8);
		graph = QuestGraphCompiler.load(xml, SCHEMA,
			new QuestGraphCompiler.References(Set.of(1), Set.of(), Set.of(ITEM_ID), Set.of(), Set.of(), Set.of()))
			.graphs().get(1);
		ownsItem = new AtomicBoolean(true);
		bridge = new QuestGraphItemDialogBridge(PLAYER_ID,
			(playerId, itemId, itemObjectId) -> ownsItem.get() && playerId == PLAYER_ID && itemId == ITEM_ID
				&& itemObjectId == ITEM_OBJECT_ID);
	}

	/** 验证授权只能从目标节点的已编译动作集合派生，且相同重试复用原授权。 / Verifies authorization is derived only from compiled target-node choices and identical retries reuse it. */
	@Test
	void opensGraphDerivedAuthorizationAndReusesIdenticalRetry() {
		ItemUseEvent source = source(1_000);
		Authorization authorization = bridge.open(source, graph, "offer", AUTHORIZATION_ID, 2_000);

		assertEquals(1_000, authorization.issuedAt());
		assertEquals(2_000, authorization.expiresAt());
		assertEquals(Set.of("ACCEPT_QUEST", "REFUSE_QUEST"), authorization.allowedDialogs());
		assertSame(authorization, bridge.open(source, graph, "offer", AUTHORIZATION_ID, 2_000));
		assertThrows(IllegalStateException.class,
			() -> bridge.open(source, graph, "offer", AUTHORIZATION_ID + 1, 2_000));
		assertThrows(IllegalArgumentException.class,
			() -> bridge.open(source, graph, "active", AUTHORIZATION_ID, 2_000));
	}

	/** 验证合法选择生成 owner 绑定事件并立刻消费授权，重放失败。 / Verifies a valid choice creates an owner-bound event, consumes authorization immediately, and rejects replay. */
	@Test
	void acceptsOnceAndRejectsReplay() {
		bridge.open(source(1_000), graph, "offer", AUTHORIZATION_ID, 2_000);
		ClientDialogInput input = input("accepted", 1_500, 1, ITEM_ID, ITEM_OBJECT_ID, "ACCEPT_QUEST", AUTHORIZATION_ID);

		ItemDialogEvent event = bridge.accept(input);

		assertEquals(new ItemDialogEvent("accepted", PLAYER_ID, 1_500, 1, ITEM_ID, ITEM_OBJECT_ID,
			"ACCEPT_QUEST", AUTHORIZATION_ID), event);
		assertEquals(0, bridge.pendingCount());
		assertThrows(IllegalArgumentException.class, () -> bridge.accept(input));
	}

	/** 验证伪造 owner、物品、动作、授权、授权前时间、过期和归属漂移均失败关闭。 / Verifies forged owner, item, choice, token, pre-issue time, expiry, and ownership drift all fail closed. */
	@Test
	void rejectsForgeryExpiryAndOwnershipDrift() {
		bridge.open(source(1_000), graph, "offer", AUTHORIZATION_ID, 2_000);
		assertRejected(input("owner", 1_500, 2, ITEM_ID, ITEM_OBJECT_ID, "ACCEPT_QUEST", AUTHORIZATION_ID));
		assertRejected(input("item", 1_500, 1, ITEM_ID + 1, ITEM_OBJECT_ID, "ACCEPT_QUEST", AUTHORIZATION_ID));
		assertRejected(input("object", 1_500, 1, ITEM_ID, ITEM_OBJECT_ID + 1, "ACCEPT_QUEST", AUTHORIZATION_ID));
		assertRejected(input("dialog", 1_500, 1, ITEM_ID, ITEM_OBJECT_ID, "FINISH_DIALOG", AUTHORIZATION_ID));
		assertRejected(input("token", 1_500, 1, ITEM_ID, ITEM_OBJECT_ID, "ACCEPT_QUEST", AUTHORIZATION_ID + 1));
		assertRejected(input("early", 999, 1, ITEM_ID, ITEM_OBJECT_ID, "ACCEPT_QUEST", AUTHORIZATION_ID));
		ownsItem.set(false);
		assertRejected(input("lost", 1_500, 1, ITEM_ID, ITEM_OBJECT_ID, "ACCEPT_QUEST", AUTHORIZATION_ID));
		assertEquals(0, bridge.pendingCount());

		ownsItem.set(true);
		bridge.open(source(2_100), graph, "offer", AUTHORIZATION_ID + 1, 3_000);
		assertRejected(input("expired", 3_001, 1, ITEM_ID, ITEM_OBJECT_ID, "ACCEPT_QUEST", AUTHORIZATION_ID + 1));
		assertEquals(0, bridge.pendingCount());
	}

	/** 验证登出、断线、owner reload/retirement 和进程重启都不会保留旧授权。 / Verifies logout, disconnect, owner reload or retirement, and process restart never retain stale authorization. */
	@Test
	void clearsAuthorizationOnOwnerAndSessionLifecycle() {
		bridge.open(source(1_000), graph, "offer", AUTHORIZATION_ID, 2_000);
		bridge.removeOwner(1);
		assertEquals(0, bridge.pendingCount());
		bridge.open(source(2_100), graph, "offer", AUTHORIZATION_ID + 1, 3_000);
		bridge.clear();
		assertEquals(0, bridge.pendingCount());

		QuestGraphItemDialogBridge restarted = new QuestGraphItemDialogBridge(PLAYER_ID,
			(playerId, itemId, itemObjectId) -> true);
		assertThrows(IllegalArgumentException.class, () -> restarted.accept(
			input("restart", 2_500, 1, ITEM_ID, ITEM_OBJECT_ID, "ACCEPT_QUEST", AUTHORIZATION_ID + 1)));
	}

	/** 验证授权打开输入的服务端时间、生命周期与物品归属边界。 / Verifies server time, lifetime, and item-ownership boundaries when opening authorization. */
	@Test
	void rejectsInvalidAuthorizationOpening() {
		assertThrows(IllegalArgumentException.class,
			() -> bridge.open(source(1_000), graph, "offer", AUTHORIZATION_ID, 1_000));
		assertThrows(IllegalArgumentException.class,
			() -> bridge.open(source(1_000), graph, "offer", AUTHORIZATION_ID, 301_001));
		ownsItem.set(false);
		assertThrows(IllegalArgumentException.class,
			() -> bridge.open(source(1_000), graph, "offer", AUTHORIZATION_ID, 2_000));
	}

	/** 断言客户端输入被失败关闭且不消费待处理授权。 / Asserts client input fails closed without consuming pending authorization. */
	private void assertRejected(ClientDialogInput input) {
		assertThrows(IllegalArgumentException.class, () -> bridge.accept(input));
	}

	/** 创建服务端确认的物品使用事件。 / Creates a server-confirmed item-use event. */
	private static ItemUseEvent source(long occurredAt) {
		return new ItemUseEvent("item-use", PLAYER_ID, occurredAt, ITEM_ID, ITEM_OBJECT_ID);
	}

	/** 创建网络层已解析但尚未授权的客户端输入。 / Creates network-parsed but not yet authorized client input. */
	private static ClientDialogInput input(String eventId, long occurredAt, int questId, int itemId, int itemObjectId,
			String dialog, long authorizationId) {
		return new ClientDialogInput(eventId, PLAYER_ID, occurredAt, questId, itemId, itemObjectId, dialog, authorizationId);
	}

	/** 返回贯穿正式 XSD/compiler 的物品对话图。 / Returns an item-dialog graph that traverses the formal XSD and compiler. */
	private static String document() {
		return """
			<quest_graphs>
				<quest_graph quest_id="1" version="1" scope="PLAYER" initial_node="offer">
					<node id="offer">
						<transition id="open" priority="10" to="offer">
							<item-use item_id="182200001"/>
							<conditions><quest-status op="IN" values="NONE"/></conditions>
							<actions><send-dialog dialog_id="4"/></actions>
						</transition>
						<transition id="accept" priority="20" to="active">
							<item-dialog item_id="182200001" dialog="ACCEPT_QUEST"/>
							<conditions><quest-status op="IN" values="NONE"/></conditions>
							<actions><start-quest/><sync-quest-status/><close-dialog/></actions>
						</transition>
						<transition id="refuse" priority="30" to="offer">
							<item-dialog item_id="182200001" dialog="REFUSE_QUEST"/>
							<conditions><quest-status op="IN" values="NONE"/></conditions>
							<actions><close-dialog/></actions>
						</transition>
					</node>
					<node id="active" terminal="true"/>
				</quest_graph>
			</quest_graphs>
			""";
	}
}
