package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.controllers.ObserveController;
import com.aionemu.gameserver.controllers.PlayerController;
import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.TribeRelationsData;
import com.aionemu.gameserver.lifecycle.TestServiceProviders;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.TransformModel;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_INFO;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.services.player.PlayerServiceDaos;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillSubType;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.KnownList;

/**
 * 固化变身 tribe 的 JAXB 绑定、生命周期恢复与客户端 NPC 关系同步。
 * Pins transform-tribe JAXB binding, lifecycle restoration and client NPC relation synchronization.
 */
class TransformTribeRuntimeTest {

	private TribeRelationsData previousTribeRelations;
	private Object previousDaos;

	@BeforeEach
	void bindRuntimeDependencies() throws Exception {
		previousTribeRelations = DataManager.TRIBE_RELATIONS_DATA;
		DataManager.TRIBE_RELATIONS_DATA = (TribeRelationsData) JAXBContext
			.newInstance(TribeRelationsData.class)
			.createUnmarshaller()
			.unmarshal(getClass().getResource(
				"/aion/data/static_data/tribe/tribe_relations.xml"));
		previousDaos = playerServiceDaosField().get(null);
		PlayerService.bindDaos(newPlayerServiceDaos());
		TestServiceProviders.install(PacketBroadcaster.class, new PacketBroadcaster());
	}

	@AfterEach
	void restoreRuntimeDependencies() throws ReflectiveOperationException {
		TestServiceProviders.clear(PacketBroadcaster.class);
		playerServiceDaosField().set(null, previousDaos);
		DataManager.TRIBE_RELATIONS_DATA = previousTribeRelations;
	}

	@Test
	void bindsTribeFromXmlAndRestoresSpawnOverrideAfterTransformEnds() throws Exception {
		TestTransformEffect template = (TestTransformEffect) JAXBContext.newInstance(TestTransformEffect.class)
			.createUnmarshaller()
			.unmarshal(new StringReader(
				"<transform model=\"202000\" panelid=\"27\" tribe=\"AGGRESSIVE_ALL\"/>"));
		TestCreature creature = new TestCreature();
		creature.getTransformModel().setTribe(TribeClass.GENERAL, true);
		Effect effect = effect(creature, template, "transform", 10);

		assertEquals(27, template.getPanelId());
		effect.startEffect(false);
		assertEquals(TribeClass.AGGRESSIVE_ALL, creature.getTransformModel().getTribe());

		effect.endEffect();
		assertNull(creature.getTransformModel().getActiveTransformEffect());
		assertEquals(TribeClass.GENERAL, creature.getTransformModel().getTribe());
	}

	@Test
	void ordinaryPlayerReturnsToRacialTribeAfterTransformEnds() {
		TestPlayer player = newPlayer();
		TestTransformEffect template = new TestTransformEffect(202004, 27, TribeClass.AGGRESSIVE_ALL, 10);
		Effect effect = effect(player, template, "ordinary-player", 11);

		effect.startEffect(false);
		assertEquals(TribeClass.AGGRESSIVE_ALL, player.getTribe());

		effect.endEffect();
		assertEquals(TribeClass.PC, player.getTribe());
	}

	@Test
	void replacementRotationClearsStaleTribeWhenNextTemplateHasNone() {
		TestCreature creature = new TestCreature();
		TestTransformEffect primary = new TestTransformEffect(202001, 27, TribeClass.AGGRESSIVE_ALL, 200);
		TestTransformEffect fallback = new TestTransformEffect(202002, 28, null, 100);
		Effect fallbackEffect = effect(creature, fallback, "fallback", 11);
		creature.getEffectController().addEffect(fallbackEffect);
		Effect primaryEffect = effect(creature, primary, "primary", 10);
		creature.getEffectController().addEffect(primaryEffect);
		assertTrue(creature.getTransformModel().isActiveTransform(primaryEffect));
		assertTrue(creature.getEffectController().getAbnormalEffects().containsAll(List.of(primaryEffect, fallbackEffect)));

		primaryEffect.endEffect();
		assertTrue(creature.getTransformModel().isActiveTransform(fallbackEffect));
		assertNull(creature.getTransformModel().getTribe());

		fallbackEffect.endEffect();
		assertNull(creature.getTransformModel().getActiveTransformEffect());
		assertNull(creature.getTransformModel().getTribe());
	}

	@Test
	void transformStartAndEndRefreshVisibleNpcTypePerPlayerTribe() throws Exception {
		TestPlayer player = newPlayer();
		TestNpc npc = TestNpc.create();
		player.setKnownlist(new SingleNpcKnownList(player, npc));
		TestTransformEffect template = new TestTransformEffect(202003, 27, TribeClass.IDEVENT01_POLYMORPHL, 10);
		Effect effect = effect(player, template, "player-transform", 12);

		assertEquals(NpcType.NON_ATTACKABLE.getId(), npcTypeId(new SM_NPC_INFO(npc, player)));
		effect.startEffect(false);
		assertEquals(NpcType.ATTACKABLE.getId(), lastNpcTypeId(player));

		effect.endEffect();
		assertEquals(NpcType.NON_ATTACKABLE.getId(), lastNpcTypeId(player));
	}

	private static Effect effect(Creature owner, TestTransformEffect template, String stack, int skillId) {
		SkillTemplate skill = new SkillTemplate();
		setField(skill, "skillId", skillId);
		setField(skill, "stack", stack);
		setField(skill, "activationAttribute", ActivationAttribute.ACTIVE);
		setField(skill, "subType", SkillSubType.NONE);
		setField(skill, "targetSlot", SkillTargetSlot.BUFF);
		setField(skill, "dispelCategory", DispelCategoryType.BUFF);
		Effects effects = new Effects();
		effects.getEffects().add(template);
		setField(skill, "effects", effects);
		Effect effect = new Effect(owner, owner, skill, 1, 0);
		effect.addAllEffectToSucess();
		return effect;
	}

	private static TestPlayer newPlayer() {
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		PlayerController controller = new PlayerController();
		controller.setOwner(player);
		setField(AionObject.class, player, "objectId", 1);
		setField(VisibleObject.class, player, "position", new WorldPosition(1));
		setField(VisibleObject.class, player, "controller", controller);
		setField(Creature.class, player, "observeController", new ObserveController());
		player.setTransformModel(new TransformModel(player));
		player.setKnownlist(new KnownList(player));
		player.setEffectController(new PlayerEffectController(player));
		player.setClientConnection(packetConnection());
		return player;
	}

	private static AionConnection packetConnection() {
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		setField(AConnection.class, connection, "transport", new RecordingTransport());
		setField(AConnection.class, connection, "guard", new Object());
		setField(AionConnection.class, connection, "sendMsgQueue", new ArrayList<AionServerPacket>());
		return connection;
	}

	@SuppressWarnings("unchecked")
	private static int lastNpcTypeId(Player player) throws ReflectiveOperationException {
		Field queueField = AionConnection.class.getDeclaredField("sendMsgQueue");
		queueField.setAccessible(true);
		List<AionServerPacket> packets = (List<AionServerPacket>) queueField.get(player.getClientConnection());
		SM_NPC_INFO packet = packets.stream()
			.filter(SM_NPC_INFO.class::isInstance)
			.map(SM_NPC_INFO.class::cast)
			.reduce((first, second) -> second)
			.orElseThrow();
		return npcTypeId(packet);
	}

	private static int npcTypeId(SM_NPC_INFO packet) throws ReflectiveOperationException {
		Field typeField = SM_NPC_INFO.class.getDeclaredField("npcTypeId");
		typeField.setAccessible(true);
		return typeField.getInt(packet);
	}

	private static PlayerServiceDaos newPlayerServiceDaos() throws ReflectiveOperationException {
		var components = PlayerServiceDaos.class.getRecordComponents();
		Class<?>[] parameterTypes = new Class<?>[components.length];
		Object[] arguments = new Object[components.length];
		for (int i = 0; i < components.length; i++) {
			parameterTypes[i] = components[i].getType();
			arguments[i] = Proxy.newProxyInstance(parameterTypes[i].getClassLoader(),
				new Class<?>[] { parameterTypes[i] }, (proxy, method, args) -> defaultValue(method.getReturnType()));
		}
		return PlayerServiceDaos.class.getConstructor(parameterTypes).newInstance(arguments);
	}

	private static Object defaultValue(Class<?> returnType) {
		if (!returnType.isPrimitive()) {
			return null;
		}
		if (returnType == boolean.class) {
			return false;
		}
		if (returnType == char.class) {
			return '\0';
		}
		if (returnType == byte.class) {
			return (byte) 0;
		}
		if (returnType == short.class) {
			return (short) 0;
		}
		if (returnType == int.class) {
			return 0;
		}
		if (returnType == long.class) {
			return 0L;
		}
		if (returnType == float.class) {
			return 0F;
		}
		return 0D;
	}

	private static Field playerServiceDaosField() throws NoSuchFieldException {
		Field field = PlayerService.class.getDeclaredField("boundDaos");
		field.setAccessible(true);
		return field;
	}

	private static void setField(Object target, String name, Object value) {
		setField(target.getClass(), target, name, value);
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

	/**
	 * JAXB 测试效果：读取基础类上的 {@code tribe} 属性。
	 * JAXB test effect that reads the {@code tribe} attribute from the base class.
	 */
	@XmlRootElement(name = "transform")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class TestTransformEffect extends TransformEffect {

		/** 供 JAXB 反射实例化。 / Used by JAXB reflection. */
		public TestTransformEffect() {
		}

		/**
		 * 构造带指定 model、面板与 tribe 的效果模板。
		 * Constructs an effect template with the given model, panel and tribe.
		 *
		 * @param model 模型 ID / model id
		 * @param panelId 面板 ID / panel id
		 * @param tribe 变身 tribe / transform tribe
		 * @param transformLevel 变身等级 / transform level
		 */
		TestTransformEffect(int model, int panelId, TribeClass tribe, int transformLevel) {
			this.model = model;
			this.panelid = panelId;
			this.tribe = tribe;
			this.transformLevel = transformLevel;
		}

		@Override
		public void startEffect(Effect effect) {
			super.startEffect(effect, null);
		}

		@Override
		public void endEffect(Effect effect) {
			super.endEffect(effect, null);
		}
	}

	/**
	 * 无具体 AI 的测试生物。
	 * Test creature without a concrete AI.
	 */
	private static class TestCreature extends Creature {

		TestCreature() {
			super(1, new CreatureController<>() {}, null, new TestPlayerTemplate(), new WorldPosition(1));
			setKnownlist(new KnownList(this));
			setEffectController(new EffectController(this));
		}

		@Override
		public String getName() {
			return "test-creature";
		}

		@Override
		public byte getLevel() {
			return 1;
		}
	}

	/**
	 * 仅提供变身所需模板、种族与位置的最小玩家。
	 * Minimal player supplying only the template, race and position required by transforms.
	 */
	private static final class TestPlayer extends Player {

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public VisibleObjectTemplate getObjectTemplate() {
			return TEST_TEMPLATE;
		}

		@Override
		public Race getRace() {
			return Race.ELYOS;
		}

		@Override
		public WorldPosition getPosition() {
			return TEST_POSITION;
		}

		private static final TestPlayerTemplate TEST_TEMPLATE = new TestPlayerTemplate();
		private static final WorldPosition TEST_POSITION = new WorldPosition(1);
	}

	/**
	 * 玩家对象模板桩。
	 * Player object-template stub.
	 */
	private static final class TestPlayerTemplate extends VisibleObjectTemplate {

		@Override
		public int getTemplateId() {
			return 1;
		}

		@Override
		public String getName() {
			return "test-player";
		}

		@Override
		public int getNameId() {
			return 1;
		}
	}

	/**
	 * 按玩家观察者重算关系的最小 NPC。
	 * Minimal NPC whose relation is recalculated for the observing player.
	 */
	private static final class TestNpc extends Npc {

		private TestNpc() {
			super(0, null, null, null);
		}

		private static TestNpc create() {
			TestNpc npc = new ObjenesisStd().newInstance(TestNpc.class);
			setField(AionObject.class, npc, "objectId", 2);
			npc.objectTemplate = new TestNpcTemplate();
			npc.setNpcType(npc.getObjectTemplate().getNpcType());
			npc.setTransformModel(new TransformModel(npc));
			npc.setKnownlist(new KnownList(npc));
			return npc;
		}
	}

	/**
	 * 与普通玩家友好的测试 NPC 模板。
	 * Test NPC template friendly to ordinary players.
	 */
	private static final class TestNpcTemplate extends NpcTemplate {

		private TestNpcTemplate() {
			setNpcId(219510);
			setLevel((byte) 65);
			setNameId(1);
			setName("Kobold Looter");
			setNpcType(NpcType.ATTACKABLE);
			setRace(Race.BROWNIE);
			setTribe(TribeClass.IDEVENT01_SUMMON);
		}
	}

	/**
	 * 仅返回单个 NPC 的已知列表。
	 * Known list exposing a single NPC for relation-refresh assertions.
	 */
	private static final class SingleNpcKnownList extends KnownList {

		private final Npc npc;

		SingleNpcKnownList(VisibleObject owner, Npc npc) {
			super(owner);
			this.npc = npc;
		}

		@Override
		public List<VisibleObject> getVisibleObjectsSnapshot() {
			return List.of(npc);
		}
	}

	/**
	 * 无事件循环的测试传输。
	 * Test transport without an event loop.
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
