package com.aionemu.gameserver.ai2;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
	 * 这些自定义 AI 的生命周期副作用会生成可被任务引用的 NPC；零售 pattern 没有等价生成动作时，必须保留脚本 AI。
	 * Quest-referenced lifecycle spawns owned by these scripted AIs must not be bypassed by an incomplete retail pattern.
	 */
	private static final Set<String> QUEST_SIDE_EFFECT_AI = Set.of(
		"Mechaturerk", "anikiki", "bighorn_wendigo", "blood_fungus_skinwalker",
		"colossal_forest_flavia", "coral_corask", "crimson_crested_slink", "devious_manduri_beacon",
		"dynamic_iluma_monster", "dynamic_norsvold_monster", "forest_of_life_brohum_changeling",
		"frostgullet_kirrin", "frosty_petrahulk", "gatorback_skilex", "giant_razorback_frillneck",
		"hidden_swamp_bufo", "hugehorn_wendigo", "masked_manduri_monkey_king",
		"masquerading_desert_gehkros", "mine_mage", "molting_honey_klaw", "mysterious_moonlight_brax",
		"nightbloom_gargonops_shifter", "plateau_gihla_chameleon", "progo_klaw_chameleon",
		"razor_clawed_forest_cloke", "rejuvinating_wave_wave_tauric", "roughhorn_wendigo",
		"ruthless_wave_tauric", "skulking_forsaken_zaif", "spirit_forest_worg_morpher",
		"thickhorn_wendigo", "valley_torr_crumbler", "venerable_sea_giant", "warrior_monument",
		"whiptail_metamorph", "young_roundshell_spiner");

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
	 * @param class1 AI 实现类 / AI implementation class
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
	 * @param name AI 名称 / AI name
	 * @param owner 所有者生物 / owner creature
	 * @return 装配好的 AI 实例 / configured AI instance
	 */
	public final AI2 setupAI(String name, Creature owner) {
		AbstractAI aiInstance = null;
		try {
			if (owner instanceof Npc npc) {
				name = selectNpcAi(name, npc.getNpcId(), npc);
			}
			aiInstance = aiMap.get(name).getDeclaredConstructor().newInstance();
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
		// These scripted action items own their interaction protocol; a retail pattern would
		// bypass it and fall through to the generic page-10 dialog.
		if ("quest_use_item".equals(fallback) || "quest_start_use_item".equals(fallback)
			|| "empyrean_blessing".equals(fallback)) {
			return fallback;
		}
		if (QUEST_SIDE_EFFECT_AI.contains(fallback)) {
			return fallback;
		}
		var pattern = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getPattern(npcId);
		return RetailPatternAI2.supports(pattern, npc) ? "retail_pattern" : fallback;
	}

	/**
	 * 使用 {@link AiNames} 枚举为 NPC 装配 AI。
	 * Sets up AI for an NPC using an {@link AiNames} enum value.
	 *
	 * @param aiName AI 名称枚举 / AI name enum
	 * @param owner 目标 NPC / target NPC
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
			npcAINames.add(npcTemplate.getAi());
		}
		npcAINames.removeAll(aiMap.keySet());
		if (npcAINames.size() > 0) {
			log.warn(I18n.get("log.84b7db63f072", StringUtils.join(npcAINames, ", ")));
		}
	}

	/**
	 * 获取引擎单例（优先 Spring Provider，否则回退静态持有者）。
	 * Returns the engine singleton (Spring provider first, else static holder).
	 *
	 * @return AI2Engine 实例 / AI2Engine instance
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
