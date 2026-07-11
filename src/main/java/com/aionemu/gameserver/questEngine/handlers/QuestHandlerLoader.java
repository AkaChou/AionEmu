package com.aionemu.gameserver.questEngine.handlers;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.lang.reflect.Modifier;

import com.aionemu.commons.scripting.classlistener.ClassListener;
import com.aionemu.commons.utils.ClassUtils;
import com.aionemu.gameserver.questEngine.QuestEngine;

/**
 * 任务处理器脚本加载器：在脚本引擎装载/卸载类时注册或清空 {@link QuestHandler}。
 * Quest-handler script loader that registers or clears {@link QuestHandler}
 * instances when the script engine loads or unloads classes.
 *
 * @author MrPoke
 */
@Slf4j
public class QuestHandlerLoader implements ClassListener {

	/**
	 * 创建空加载器。
	 * Create an empty loader.
	 */
	public QuestHandlerLoader() {
	}

	/**
	 * 类装载完成后：实例化并注册所有合法的 {@link QuestHandler} 子类。
	 * After classes are loaded: instantiate and register every valid {@link QuestHandler} subclass.
	 *
	 * @param classes 新装载的类数组 / Newly loaded classes
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void postLoad(Class<?>[] classes) {
		for (Class<?> c : classes) {
			if (log.isDebugEnabled()) {
				log.debug("Load class " + c.getName());
			}
			if (!isValidClass(c)) {
				continue;
			}
			if (ClassUtils.isSubclass(c, QuestHandler.class)) {
				try {
					Class<? extends QuestHandler> tmp = (Class<? extends QuestHandler>) c;
					if (tmp != null) {
						GameEngineServices.questEngine().addQuestHandler(tmp.getDeclaredConstructor().newInstance());
					}
				} catch (Exception e) {
					throw new RuntimeException("Failed to load quest handler class: " + c.getName(), e);
				}
			}
		}
	}

	/**
	 * 类卸载前：清空任务引擎中全部已注册处理器。
	 * Before classes unload: clear every registered handler from the quest engine.
	 *
	 * @param classes 即将卸载的类数组 / Classes about to unload
	 */
	@Override
	public void preUnload(Class<?>[] classes) {
		if (log.isDebugEnabled()) {
			for (Class<?> c : classes) {
				// 调试消息 / debug messages
				log.debug("Unload class " + c.getName());
			}
		}
		GameEngineServices.questEngine().clear();
	}

	/**
	 * 判断类是否可作为任务处理器装载（非抽象/接口且 public）。
	 * Whether a class is loadable as a quest handler (public, non-abstract, non-interface).
	 *
	 * @param clazz 待检查类 / Class under inspection
	 * @return {@code true} when valid。 / {@code true} when valid
	 */
	public boolean isValidClass(Class<?> clazz) {
		final int modifiers = clazz.getModifiers();

		if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
			return false;
		}
		if (!Modifier.isPublic(modifiers)) {
			return false;
		}
		return true;
	}
}
