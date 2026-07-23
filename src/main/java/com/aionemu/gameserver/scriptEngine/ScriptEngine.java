package com.aionemu.gameserver.scriptEngine;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.configs.Config;

/**
 * 脚本引擎：对应真端 ScriptDLL64 的脚本/事件执行层，统一加载并管理
 * {@link ScriptNpc} 与 {@link ScriptQuest} 绑定。
 * Script engine: the retail-ScriptDLL64-aligned script/event execution layer,
 * uniformly loading and managing {@link ScriptNpc} and {@link ScriptQuest} bindings.
 *
 * <p>本类是 AI Pattern 执行器（{@code RetailPatternAI2}）与 XML 任务执行器
 * （{@code XMLQuest}）之上的统一注册表。各数据所有者完成解析与事件索引注册后，
 * 将脚本绑定填充到 {@link ScriptRegistry}；任务绑定由 {@code QuestEngine} 装载，
 * 避免并行启动时重复解析同一批 XML。不引入 JNI 或真端 {@code ScriptDLL64.dll}。
 */
@Slf4j
public class ScriptEngine implements GameEngine {

	/** Spring ObjectProvider 覆盖钩子 / Spring ObjectProvider override hook */
	private static volatile ObjectProvider<ScriptEngine> instanceProvider;
	/** 脚本注册表 / script registry */
	private final ScriptRegistry registry = new ScriptRegistry();

	/**
	 * 启动期加载脚本绑定数据并填充注册表。
	 * Load script bindings at startup and populate the registry.
	 *
	 * <p>注册表由各数据所有者填充；本引擎只负责生命周期与统一查询入口。
	 * Data owners populate the registry; this engine owns its lifecycle and lookup API.
	 *
	 * @param progressLatch 进度闩锁（可空） / progress latch (nullable)
	 */
	@Override
	public void load(CountDownLatch progressLatch) {
		log.info(I18n.get("log.scriptEngine.loading"));
		ScriptNpcData.load(Config.definitionFile("./definitions/compact/script-npcs.xml")).register(registry);
		log.info(I18n.get("log.scriptEngine.loaded", registry.scriptNpcCount(), registry.scriptQuestCount()));
		if (progressLatch != null) {
			progressLatch.countDown();
		}
	}

	/**
	 * 关闭引擎并清空注册表。
	 * Shut down the engine and clear the registry.
	 */
	@Override
	public void shutdown() {
		registry.clear();
		log.info(I18n.get("log.scriptEngine.shutdown"));
	}

	/**
	 * 返回脚本注册表。
	 * Return the script registry.
	 *
	 * @return 注册表 / registry
	 */
	public ScriptRegistry getRegistry() {
		return registry;
	}

	/**
	 * 返回脚本引擎单例（优先 Spring ObjectProvider）。
	 * Return the script-engine singleton (prefer Spring ObjectProvider when set).
	 *
	 * @return 引擎实例 / engine instance
	 */
	public static final ScriptEngine getInstance() {
		ObjectProvider<ScriptEngine> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置 Spring ObjectProvider 覆盖点。
	 * Install a Spring ObjectProvider override.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<ScriptEngine> provider) {
		instanceProvider = provider;
	}

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		/** 默认引擎实例 / default engine instance */
		protected static final ScriptEngine instance = new ScriptEngine();
	}
}
