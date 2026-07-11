package com.aionemu.gameserver.instance;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.lang.reflect.Modifier;

import com.aionemu.commons.scripting.classlistener.ClassListener;
import com.aionemu.commons.utils.ClassUtils;
import com.aionemu.gameserver.instance.handlers.InstanceHandler;

/**
 * 副本处理器类监听器：在脚本类加载后将其注册到 {@link InstanceEngine}。
 * Instance-handler class listener: registers loaded script classes with {@link InstanceEngine}.
 *
 * @author ATracer
 */
@Slf4j
public class InstanceHandlerClassListener implements ClassListener {

	/**
	 * 类加载后回调：筛选并注册 {@link InstanceHandler} 实现。
	 * Post-load callback: filter and register {@link InstanceHandler} implementations.
	 *
	 * @param classes 刚加载的类 / newly loaded classes
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
			if (ClassUtils.isSubclass(c, InstanceHandler.class)) {
				Class<? extends InstanceHandler> tmp = (Class<? extends InstanceHandler>) c;
				if (tmp != null) {
					GameEngineServices.instanceEngine().addInstanceHandlerClass(tmp);
				}
			}
		}
	}

	/**
	 * 类卸载前回调（调试日志）。
	 * Pre-unload callback (debug logging).
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
	 * 判断类是否可作为具体处理器注册（非抽象、非接口、公开）。
	 * Whether the class is eligible for registration as a concrete handler (not abstract/interface, public).
	 *
	 * @param clazz 待检查类 / class to check
	 * @return 可注册则为 {@code true} / {@code true} if registerable
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
