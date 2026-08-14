package com.aionemu.gameserver.world.zone.handler;

import java.lang.reflect.Modifier;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.scripting.classlistener.ClassListener;
import com.aionemu.commons.utils.ClassUtils;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

/**
 * 脚本类加载监听器：将有效的 {@link ZoneHandler} 实现注册到区域服务。
 * Script class-load listener: registers valid {@link ZoneHandler} implementations with the zone service.
 *
 * @author MrPoke
 */
@Slf4j(topic = "com.aionemu.gameserver.instance.InstanceHandlerClassListener")
public class ZoneHandlerClassListener implements ClassListener {

	/**
	 * 类加载后扫描并注册区域处理器。
	 * After classes are loaded, scan and register zone handlers.
	 *
	 * @param classes 已加载的类数组 / loaded class array
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
			if (ClassUtils.isSubclass(c, ZoneHandler.class)) {
				Class<? extends ZoneHandler> tmp = (Class<? extends ZoneHandler>) c;
				if (tmp != null) {
					GameWorldBootstrapServices.zoneService().addZoneHandlerClass(tmp);
				}
			}
		}
	}

	/**
	 * 类卸载前记录调试日志。
	 * Log debug messages before classes are unloaded.
	 *
	 * @param classes 待卸载的类数组 / classes about to be unloaded
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
	 * 判断类是否可作为区域处理器（公开、非抽象、非接口）。
	 * Whether the class is a valid zone handler (public, non-abstract, non-interface).
	 *
	 * @param clazz 待检查的类 / class to check
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
