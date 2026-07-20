package com.aionemu.gameserver.ai2;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.scripting.classlistener.AggregatedClassListener;
import com.aionemu.commons.scripting.classlistener.OnClassLoadUnloadListener;
import com.aionemu.commons.scripting.classlistener.ScheduledTaskClassListener;
import com.aionemu.commons.scripting.CompiledScriptLoader;
import com.aionemu.gameserver.GameServerError;
import com.aionemu.gameserver.ai.RetailPatternAI2;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

/**
 * AI2 引擎：负责加载、注册、校验并为生物装配 AI 实例。
 * AI2 engine: loads, registers, validates and attaches AI instances to creatures.
 *
 * @author ATracer
 */
@Slf4j
public class AI2Engine implements GameEngine {

	private static volatile ObjectProvider<AI2Engine> instanceProvider;
	private final Map<String, Class<? extends AbstractAI>> aiMap = new HashMap<String, Class<? extends AbstractAI>>();

	/**
	 * 加载 AI 脚本并注册所有 AI 处理器。
	 * Loads AI scripts and registers all AI handlers.
	 *
	 * @param progressLatch 进度倒计时锁 / progress latch
	 */
	@Override
	public void load(CountDownLatch progressLatch) {
		log.info(I18n.get("log.526564512218"));
		AggregatedClassListener acl = new AggregatedClassListener();
		acl.addClassListener(new OnClassLoadUnloadListener());
		acl.addClassListener(new ScheduledTaskClassListener());
		acl.addClassListener(new AI2HandlerClassListener());

		try {
			acl.postLoad(CompiledScriptLoader.load("com.aionemu.gameserver.ai"));
			log.info(I18n.get("log.ab0828fed65a", aiMap.size()));
			validateScripts();
		} catch (Exception e) {
			throw new GameServerError("Can't initialize ai handlers.", e);
		} finally {
			if (progressLatch != null) {
				progressLatch.countDown();
			}
		}
	}

	/**
	 * 关闭引擎并清空 AI 注册表。
	 * Shuts down the engine and clears the AI registry.
	 */
	@Override
	public void shutdown() {
		log.info(I18n.get("log.376558570f8c"));
		aiMap.clear();
		log.info(I18n.get("log.6bfc701929ea"));
	}

	/**
	 * 按 {@link AIName} 注解将 AI 类注册到名称映射表。
	 * Registers an AI class into the name map using its {@link AIName} annotation.
	 *
	 * AI implementation class
	 */
	public void registerAI(Class<? extends AbstractAI> class1) {
		AIName nameAnnotation = class1.getAnnotation(AIName.class);
		if (nameAnnotation != null) {
			aiMap.put(nameAnnotation.value(), class1);
		}
	}

	/**
	 * 按名称创建 AI 实例并绑定到所有者。
	 * Creates an AI instance by name and binds it to the owner.
	 *
	 * AI name
	 *
	 * @param owner 所有者生物 / owner creature
	 * @param owner
	 * @return 装配好的 AI 实例 / configured AI instance
	 */
	public final AI2 setupAI(String name, Creature owner) {
		AbstractAI aiInstance = null;
		try {
			if (owner instanceof Npc npc) {
				name = selectRegisteredNpcAi(name, npc);
			}
			Class<? extends AbstractAI> aiClass = aiMap.get(name);
			if (aiClass == null) {
				throw new IllegalArgumentException("Unknown AI " + name);
			}
			aiInstance = aiClass.getDeclaredConstructor().newInstance();
			aiInstance.setOwner(owner);
			owner.setAi2(aiInstance);
			if (AIConfig.ONCREATE_DEBUG) {
				aiInstance.setLogging(true);
			}
		} catch (Exception e) {
			log.error(I18n.get("log.b80441439b8c", name, e));
		}
		return aiInstance;
	}

	static String selectNpcAi(String fallback, int npcId, Npc npc) {
		var pattern = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getPattern(npcId);
		return RetailPatternAI2.supports(pattern, npc) ? "retail_pattern" : fallback;
	}

	String selectRegisteredNpcAi(String fallback, Npc npc) {
		String selected = selectNpcAi(fallback, npc == null ? 0 : npc.getNpcId(), npc);
		if (aiMap.containsKey(selected)) {
			return selected;
		}
		String generic = npc != null && npc.getObjectTemplate().getNpcType() == NpcType.AGGRESSIVE
				? AiNames.AGGRESSIVE_NPC.getName() : AiNames.GENERAL_NPC.getName();
		return aiMap.containsKey(generic) ? generic : AiNames.DUMMY_NPC.getName();
	}

	/**
	 * 使用 {@link AiNames} 枚举为 NPC 装配 AI。
	 * Sets up AI for an NPC using an {@link AiNames} enum value.
	 *
	 * AI name enum
	 * target NPC
	 */
	public void setupAI(AiNames aiName, Npc owner) {
		setupAI(aiName.getName(), owner);
	}

	/**
	 * 校验 NPC 模板中引用的 AI 名称是否均已注册。
	 * Validates that all AI names referenced by NPC templates are registered.
	 */
	private void validateScripts() {
		Collection<String> npcAINames = new HashSet<String>();
		for (NpcTemplate npcTemplate : DataManager.NPC_DATA.getNpcData().values()) {
			String ai = npcTemplate.getAi();
			if (!aiMap.containsKey(ai) && !(aiMap.containsKey("retail_pattern") && DataManager.RETAIL_AI_DATA != null
					&& RetailPatternAI2.supports(DataManager.RETAIL_AI_DATA.getPattern(npcTemplate.getTemplateId())))) {
				npcAINames.add(ai);
			}
		}
		if (npcAINames.size() > 0) {
			log.warn(I18n.get("log.84b7db63f072", StringUtils.join(npcAINames, ", ")));
		}
	}

	/**
	 * 获取引擎单例（优先 Spring Provider，否则回退静态持有者）。
	 * Returns the engine singleton (Spring provider first, else static holder).
	 *
	 * AI2Engine instance
	 */
	public static final AI2Engine getInstance() {
		ObjectProvider<AI2Engine> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置 Spring 实例 Provider。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<AI2Engine> provider) {
		instanceProvider = provider;
	}

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final AI2Engine instance = new AI2Engine();
	}
}
