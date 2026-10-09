package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.aionemu.gameserver.controllers.PlayerController;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;

/**
 * 事件任务维护面的 native 行恢复门禁：{@code QuestService.startEventQuest} 的 native 分支
 * 与 {@code EventService.StartOrMaintainQuests} 的登录维护联动。
 * <p>
 * 原版事实基线：事件清单（{@code events_config.xml} 的 maintainable）318 行中 315 行为原生车道行
 * （182 SimpleTalk + 16 SimpleHunt + 113 DataDriven），其元数据只存在于原版 {@code quest.xml}
 * （QuestsData 与生产目录同为 733 行集合，取不到模板）。「事件任务」语义来自活动清单，不是 quest.xml
 * 的 {@code category1}（客户端 UI 分组；80900-80938 段为 mission/seen_marker/public）。
 * <p>
 * The native-lane recovery gate for the event maintenance face. Assertions are sourced from the retail
 * quest.xml rows (80022 SimpleTalk / 80900 DataDriven) and the activity-list semantics; unlisted rows
 * stay fail-closed.
 */
class EventQuestNativeMaintenanceTest {

	/** SimpleTalk 事件行：minlevel=10 / pc_light / max_repeat_count=10 / category=event / 无前置。 */
	private static final int TALK_EVENT_REPEAT_10 = 80022;
	/** DataDriven 事件行：minlevel=66 / category=mission（活动清单语义不得被 UI 分组拦截）。 */
	private static final int DD_EVENT_MISSION_CATEGORY = 80900;

	private QuestsData previousQuestsData;
	private ObjectProvider<QuestEngine> previousEngine;
	private ZoneUpdateService previousZoneUpdateService;

	@BeforeEach
	void installBareProductionSurfaces() {
		// 目录面：空 QuestsData（与「目录行 = 733 行、native 行取不到模板」的生产形状等价，
		// 且无需装载 733 行静态数据）；引擎面：裸引擎（native 侧不依赖 typed 目录）；
		// 收尾面：真实构造的 ZoneUpdateService 装入移动循环解析缓存（startEventQuest 的
		// updateZone 收尾走它；构造器仅注册启动钩子，无线程启动）。
		// Empty QuestsData mirrors the production shape for native rows (no template reachable);
		// a bare engine serves the native metadata surface; a real ZoneUpdateService fills the
		// movement-loop resolve cache for the updateZone tail.
		previousQuestsData = QuestService.questsData;
		QuestService.questsData = new QuestsData();
		previousEngine = installBareEngine();
		previousZoneUpdateService = installZoneUpdateService();
	}

	@AfterEach
	void restoreProductionSurfaces() {
		QuestService.questsData = previousQuestsData;
		QuestEngine.setInstanceProvider(previousEngine);
		restoreZoneUpdateService(previousZoneUpdateService);
	}

	@Test
	void startEventQuestBuildsNativeLaneListedRow() {
		Player player = nativePlayer(10);

		assertTrue(QuestService.startEventQuest(env(player, TALK_EVENT_REPEAT_10), QuestStatus.START),
			"清单内 native 事件行必须建档（QuestsData 无模板 ⇒ 回退原版 quest.xml 元数据）");

		QuestState state = player.getQuestStateList().getQuestState(TALK_EVENT_REPEAT_10);
		assertTrue(state != null, "QuestState 必须被创建");
		assertEquals(QuestStatus.START, state.getStatus());
		assertEquals(0, state.getCompleteCount());
	}

	@Test
	void nativeBranchSkipsTheXmlCategoryGateForListedRows() {
		// 80900 的 quest.xml category1 = mission：活动清单（maintainable）才是事件语义来源，
		// UI 分组不得拦下清单内行。
		// The row's client-UI category (mission) must not reject a listed row.
		Player player = nativePlayer(66);

		assertTrue(QuestService.startEventQuest(env(player, DD_EVENT_MISSION_CATEGORY), QuestStatus.START),
			"清单内 mission 分组行照常建档（事件语义 = 活动清单）");
		assertTrue(player.getQuestStateList().getQuestState(DD_EVENT_MISSION_CATEGORY) != null);
	}

	@Test
	void levelGateStillAppliesOnTheNativeBranch() {
		Player player = nativePlayer(9);

		assertFalse(QuestService.startEventQuest(env(player, TALK_EVENT_REPEAT_10), QuestStatus.START),
			"未达 minlevel=10 ⇒ 拒绝");
		assertNull(player.getQuestStateList().getQuestState(TALK_EVENT_REPEAT_10), "拒绝时不建档");
	}

	@Test
	void unlistedRowStaysFailClosed() {
		Player player = nativePlayer(80);

		assertFalse(QuestService.startEventQuest(env(player, 999999), QuestStatus.START),
			"目录与原版全量表中都不存在的行 ⇒ fail-closed");
		assertNull(player.getQuestStateList().getQuestState(999999));
	}

	@Test
	void existingNativeRowIsResetWithinTheRepeatBudget() {
		// 未达上限（completeCount=1 < 10）⇒ 重置为 START 且变量清零。
		Player player = nativePlayer(10);
		QuestState state = NativeTalkFixture.add(player, TALK_EVENT_REPEAT_10, QuestStatus.COMPLETE, 0);
		state.setCompleteCount(1);
		state.setQuestVar(1);

		assertTrue(QuestService.startEventQuest(env(player, TALK_EVENT_REPEAT_10), QuestStatus.START));
		assertEquals(QuestStatus.START, state.getStatus(), "循环活动行未达上限 ⇒ 登录维护重置");
		assertEquals(0, state.getQuestVars().getQuestVars(), "重置同时清零任务变量");

		// 超出上限（completeCount=11 > 10）⇒ 保持原状。
		Player spent = nativePlayer(10);
		QuestState spentState = NativeTalkFixture.add(spent, TALK_EVENT_REPEAT_10, QuestStatus.COMPLETE, 0);
		spentState.setCompleteCount(11);
		spentState.setQuestVar(1);

		assertTrue(QuestService.startEventQuest(env(spent, TALK_EVENT_REPEAT_10), QuestStatus.START));
		assertEquals(QuestStatus.COMPLETE, spentState.getStatus(), "超出重复上限不得重置");
		assertEquals(1, spentState.getQuestVars().getQuestVars());
	}

	@Test
	void startOrMaintainQuestsGrantsNativeRowsOnLogin() {
		// 登录维护联动：EventService 的 metadata 获取回退（:155）→ matchesEventQuestMetadata
		//（等级/种族/职业/性别 + 分组前置）→ startEventQuest native 分支建档。
		// The login-maintenance chain end to end for a listed native row.
		EventService service = new ObjenesisStd().newInstance(EventService.class);
		Player player = nativePlayer(10);

		service.StartOrMaintainQuests(player, List.of(TALK_EVENT_REPEAT_10).listIterator(), Map.of(), true);

		QuestState state = player.getQuestStateList().getQuestState(TALK_EVENT_REPEAT_10);
		assertTrue(state != null, "登录维护必须为清单内 native 行建档（1157 同款回退口径）");
		assertEquals(QuestStatus.START, state.getStatus());
	}

	// ------------------------------------------------------------ 夹具

	private static QuestEnv env(Player player, int questId) {
		return new QuestEnv(null, player, questId, 0);
	}

	/** 原生车道测试玩家（天族战士，带包捕获连接与控制器）。 / A native-lane test player. */
	private static Player nativePlayer(int level) {
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, level);
		PlayerController controller = new PlayerController();
		setField(VisibleObject.class, player, "controller", controller);
		controller.setOwner(player);
		return player;
	}

	/** 装入移动循环的 ZoneUpdateService 解析缓存（updateZone 收尾依赖）。 / Fills the movement-loop resolve cache. */
	private static ZoneUpdateService installZoneUpdateService() {
		try {
			Field field = GameMovementLoopServices.class.getDeclaredField("resolvedZoneUpdateService");
			field.setAccessible(true);
			ZoneUpdateService previous = (ZoneUpdateService) field.get(null);
			field.set(null, new ZoneUpdateService());
			return previous;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void restoreZoneUpdateService(ZoneUpdateService previous) {
		try {
			Field field = GameMovementLoopServices.class.getDeclaredField("resolvedZoneUpdateService");
			field.setAccessible(true);
			field.set(null, previous);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** 装一个裸引擎（空 typed 目录）：native 元数据面不依赖生产目录。 / A bare engine for the native surface. */
	@SuppressWarnings("unchecked")
	private static ObjectProvider<QuestEngine> installBareEngine() {
		try {
			Field providerField = QuestEngine.class.getDeclaredField("instanceProvider");
			providerField.setAccessible(true);
			ObjectProvider<QuestEngine> previous = (ObjectProvider<QuestEngine>) providerField.get(null);
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			beanFactory.registerSingleton(EventQuestNativeMaintenanceTest.class.getName() + ".engine",
				new QuestEngine());
			QuestEngine.setInstanceProvider(beanFactory.getBeanProvider(QuestEngine.class));
			return previous;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) {
		try {
			Field field = declaringClass.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
