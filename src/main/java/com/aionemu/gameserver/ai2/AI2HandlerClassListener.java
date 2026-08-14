package com.aionemu.gameserver.ai2;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.lang.reflect.Modifier;

import com.aionemu.commons.scripting.classlistener.ClassListener;
import com.aionemu.commons.utils.ClassUtils;

/**
 * AI2 脚本类加载监听器，在脚本加载后自动注册 {@link AbstractAI} 子类。
 * AI2 script class-load listener that auto-registers {@link AbstractAI} subclasses after load.
 *
 * @author ATracer
 */
@Slf4j
public class AI2HandlerClassListener implements ClassListener {


	/**
	 * 类加载完成后扫描并注册有效的 AI 实现。
	 * Scans and registers valid AI implementations after classes are loaded.
	 *
	 * @param classes 已加载类数组 / loaded classes
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
			if (ClassUtils.isSubclass(c, AbstractAI.class)) {
				Class<? extends AbstractAI> tmp = (Class<? extends AbstractAI>) c;
				if (tmp != null) {
					GameEngineServices.ai2Engine().registerAI(tmp);
				}
			}
		}
	}

	/**
	 * 类卸载前的调试日志输出。
	 * Writes debug logs before classes are unloaded.
	 *
	 * @param classes 即将卸载的类 / classes about to unload
	 */
	@Override
	public void preUnload(Class<?>[] classes) {
		if (log.isDebugEnabled()) {
			for (Class<?> c : classes) {
				log.debug("Unload class " + c.getName());
			}
		}
	}

	/**
	 * 判断类是否可作为 AI 注册（非抽象、非接口、且为 public）。
	 * Returns whether the class is valid for AI registration (concrete, non-interface, public).
	 *
	 * @param clazz 待检查类 / class to check
	 * @return 是否有效 / whether valid
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
