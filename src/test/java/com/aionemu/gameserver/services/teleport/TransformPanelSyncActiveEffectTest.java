package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.TransformModel;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRANSFORM;
import com.aionemu.gameserver.persistence.dao.PlayerTransformDao;
import com.aionemu.gameserver.persistence.dao.impl.PlayerTransformDaoImpl;
import com.aionemu.gameserver.skillengine.effect.Effects;
import com.aionemu.gameserver.skillengine.effect.PolymorphEffect;
import com.aionemu.gameserver.skillengine.effect.TransformEffect;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.TransformType;

/**
 * 传送后活动变身面板的优先级回归，不访问数据库、网络或游戏世界。
 * Guards active-transform panel precedence after teleport without accessing a database, network or game world.
 */
class TransformPanelSyncActiveEffectTest {
	private static final Path SKILLS_DIRECTORY = Path.of(
		"src/main/resources/aion/definitions/compact/skills");

	private final ObjenesisStd objenesis = new ObjenesisStd();
	private final RecordingTransformDao dao = new RecordingTransformDao();
	private PlayerTransformDao previousDao;
	private static SkillData actualSkills;

	@BeforeAll
	static void loadActualShugoTemplates() throws ReflectiveOperationException {
		Class<?> loader = Class.forName("com.aionemu.gameserver.dataholders.loadingutils.SkillDefinitionLoader");
		Method load = loader.getDeclaredMethod("load", File.class);
		load.setAccessible(true);
		actualSkills = (SkillData) load.invoke(null, SKILLS_DIRECTORY.toFile());
	}

	@BeforeEach
	void bindRecordingDao() throws ReflectiveOperationException {
		previousDao = (PlayerTransformDao) daoField().get(null);
		TransformPanelSync.bindPlayerTransformDAO(dao);
	}

	@AfterEach
	void restoreDao() throws ReflectiveOperationException {
		daoField().set(null, previousDao);
	}

	@Test
	void activeInstancePanelsAreNotOverwrittenByPersistedValues() throws ReflectiveOperationException {
		for (int panelId : List.of(25, 26, 27)) {
			Player player = player(true, panelId);
			TransformModel model = player.getTransformModel();
			Effect activeEffect = model.getActiveTransformEffect();

			TransformPanelSync.loadAndSyncTransformation(player);

			assertEquals(0, dao.loads);
			assertEquals(panelId, model.getPanelId());
			assertEquals(202588, model.getModelId());
			assertEquals(0, model.getItemId());
			assertEquals(TribeClass.IDEVENT01_POLYMORPHL, model.getTribe());
			assertSame(activeEffect, model.getActiveTransformEffect());
			assertTransformPackets(player, panelId, 202588, 0, 1);
		}
	}

	@Test
	void bothLegacySyncEntrypointsRespectTheSelectedActiveForm() throws ReflectiveOperationException {
		for (int panelId : List.of(25, 26, 27, 76)) {
			Player player = player(true, panelId);

			TransformPanelSync.syncInstanceEventPanels(player);
			TransformPanelSync.syncArchdaevaPanels(player);

			assertEquals(panelId, player.getTransformModel().getPanelId());
			assertEquals(0, dao.loads);
			assertTransformPackets(player, panelId, 202588, 0, 2);
		}
	}

	@Test
	void persistedPanelStillLoadsWhenNoRuntimeEffectOwnsTheForm() throws ReflectiveOperationException {
		Player player = player(false, 0);

		TransformPanelSync.loadAndSyncTransformation(player);

		assertEquals(1, dao.loads);
		assertEquals(71, player.getTransformModel().getPanelId());
		assertEquals(102301000, player.getTransformModel().getItemId());
		assertTransformPackets(player, 71, 202588, 102301000, 1);
	}

	@Test
	void panelLessActiveEffectsKeepTheExistingPersistenceFallback() throws ReflectiveOperationException {
		Player player = player(true, 0);

		TransformPanelSync.loadAndSyncTransformation(player);

		assertEquals(1, dao.loads);
		assertEquals(71, player.getTransformModel().getPanelId());
		var sync = TransformPanelSync.class.getDeclaredMethod("syncActiveTransform", Player.class);
		sync.setAccessible(true);
		assertEquals(false, sync.invoke(null, player));
		assertTransformPackets(player, 71, 202588, 102301000, 1);
	}

	@Test
	void syncUsesTheEffectTemplateRatherThanAStaleModelPanel() throws ReflectiveOperationException {
		Player player = player(true, 25);
		player.getTransformModel().setPanelId(71);

		TransformPanelSync.syncInstanceEventPanels(player);

		assertEquals(25, player.getTransformModel().getPanelId());
		assertTransformPackets(player, 25, 202588, 0, 1);
	}

	@Test
	void allActualShugoTransformsSendTheirLoadedPanelModelAndItem() throws ReflectiveOperationException {
		for (int skillId : List.of(21094, 21095, 21096, 21103, 21104, 21105)) {
			SkillTemplate template = actualSkills.getSkillTemplate(skillId);
			assertNotNull(template, "skill " + skillId);
			TransformEffect transform = transformEffect(template);
			Player player = player(template, transform);

			TransformPanelSync.loadAndSyncTransformation(player);
			TransformPanelSync.syncInstanceEventPanels(player);
			TransformPanelSync.syncArchdaevaPanels(player);

			assertEquals(0, dao.loads, "skill " + skillId);
			assertTransformPackets(player, transform.getPanelId(),
				(int) transformValue(transform, "model"), (int) transformValue(transform, "itemId"), 3);
		}
	}

	private Player player(boolean activeEffect, int panelId) throws ReflectiveOperationException {
		Player player = objenesis.newInstance(Player.class);
		TransformModel model = objenesis.newInstance(TransformModel.class);
		model.setModelId(202588);
		model.setTransformType(TransformType.FORM1);
		model.setPanelId(panelId);
		if (activeEffect) {
			PolymorphEffect polymorph = new PolymorphEffect();
			setField(TransformEffect.class, polymorph, "panelid", panelId);
			Effects effects = new Effects();
			effects.getEffects().add(polymorph);
			SkillTemplate skill = new SkillTemplate();
			setField(SkillTemplate.class, skill, "effects", effects);
			Effect effect = objenesis.newInstance(Effect.class);
			setField(Effect.class, effect, "skillTemplate", skill);
			model.setActiveTransform(effect, 175, false, 0);
			model.setTribe(TribeClass.IDEVENT01_POLYMORPHL, false);
		}
		player.setTransformModel(model);
		setField(Player.class, player, "clientConnection", packetConnection());
		return player;
	}

	private Player player(SkillTemplate template, TransformEffect transform) throws ReflectiveOperationException {
		Player player = objenesis.newInstance(Player.class);
		TransformModel model = objenesis.newInstance(TransformModel.class);
		model.setModelId((int) transformValue(transform, "model"));
		model.setTransformType((TransformType) transformValue(transform, "type"));
		model.setPanelId(transform.getPanelId());
		model.setItemId((int) transformValue(transform, "itemId"));
		model.setTribe((TribeClass) transformValue(transform, "tribe"), false);

		Effect effect = objenesis.newInstance(Effect.class);
		setField(Effect.class, effect, "skillTemplate", template);
		model.setActiveTransform(effect, (int) transformValue(transform, "transformLevel"),
			(boolean) transformValue(transform, "useItem"), (int) transformValue(transform, "animationSkillId"));

		player.setTransformModel(model);
		setField(Player.class, player, "clientConnection", packetConnection());
		return player;
	}

	private static TransformEffect transformEffect(SkillTemplate template) {
		return template.getEffects().getEffects().stream()
			.filter(TransformEffect.class::isInstance)
			.map(TransformEffect.class::cast)
			.findFirst()
			.orElseThrow(() -> new AssertionError("Missing transform effect for skill " + template.getSkillId()));
	}

	private static void assertTransformPackets(Player player, int panelId, int modelId, int itemId, int count)
		throws ReflectiveOperationException {
		List<AionServerPacket> packets = packetQueue(player);
		assertEquals(count, packets.size());
		for (AionServerPacket packet : packets) {
			SM_TRANSFORM transform = assertInstanceOf(SM_TRANSFORM.class, packet);
			assertEquals(panelId, intField(transform, "panelId"));
			assertEquals(modelId, intField(transform, "modelId"));
			assertEquals(itemId, intField(transform, "itemId"));
		}
	}

	private AionConnection packetConnection() throws ReflectiveOperationException {
		AionConnection connection = objenesis.newInstance(AionConnection.class);
		setField(AConnection.class, connection, "transport", new RecordingTransport());
		setField(AConnection.class, connection, "guard", new Object());
		setField(AionConnection.class, connection, "sendMsgQueue", new ArrayList<AionServerPacket>());
		return connection;
	}

	@SuppressWarnings("unchecked")
	private static List<AionServerPacket> packetQueue(Player player) throws ReflectiveOperationException {
		Field field = AionConnection.class.getDeclaredField("sendMsgQueue");
		field.setAccessible(true);
		return (List<AionServerPacket>) field.get(player.getClientConnection());
	}

	private static Object transformValue(TransformEffect effect, String name) {
		try {
			Field field = TransformEffect.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(effect);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Cannot read TransformEffect." + name, e);
		}
	}

	private static int intField(Object target, String name) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(target);
	}

	private static void setField(Class<?> owner, Object target, String name, Object value)
		throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Field daoField() throws ReflectiveOperationException {
		Field field = TransformPanelSync.class.getDeclaredField("boundPlayerTransformDAO");
		field.setAccessible(true);
		return field;
	}

	/**
	 * 模拟与副本变身不同的持久化面板，记录是否被误加载。
	 * Simulates a persisted panel different from the instance form and records accidental loads.
	 */
	private static final class RecordingTransformDao extends PlayerTransformDaoImpl {
		private int loads;

		@Override
		public void loadPlTransfo(Player player) {
			loads++;
			player.getTransformModel().setPanelId(71);
			player.getTransformModel().setItemId(102301000);
		}
	}

	/**
	 * 仅保留连接发送队列的内存传输，不创建通道或触发实际网络写入。
	 * In-memory transport retaining the connection send queue without creating a channel or writing to the network.
	 */
	private static final class RecordingTransport implements ConnectionTransport {

		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		@Override
		public void enableWriteInterest() {
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
