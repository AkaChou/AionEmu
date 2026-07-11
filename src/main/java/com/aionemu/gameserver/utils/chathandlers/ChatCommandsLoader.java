package com.aionemu.gameserver.utils.chathandlers;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Modifier;

import com.aionemu.commons.scripting.classlistener.ClassListener;
import com.aionemu.commons.utils.ClassUtils;

/**
 * 聊天命令类加载监听器，将合法的 Admin/Player 命令注册到处理器。
 * Chat-command class loader that registers valid Admin/Player commands on the processor.
 *
 * @author Aquanox
 */
@Slf4j
public class ChatCommandsLoader implements ClassListener {

	/**
	 * 目标命令处理器。
	 * Target chat processor.
	 */
	private ChatProcessor processor;

	/**
	 * 绑定命令处理器。
	 * Bind to a chat processor.
	 *
	 * @param processor 命令处理器 / Chat processor
	 */
	public ChatCommandsLoader(ChatProcessor processor) {
		this.processor = processor;
	}

	/**
	 * 类加载完成后实例化并注册命令。
	 * After classes load, instantiate and register commands.
	 *
	 * Loaded classes
	 */
	@Override
	public void postLoad(Class<?>[] classes) {
		for (Class<?> c : classes) {
			if (!isValidClass(c)) {
				continue;
			}
			Class<?> tmp = (Class<?>) c;
			if (tmp != null) {
				try {
					processor.registerCommand((ChatCommand) tmp.getDeclaredConstructor().newInstance());
				} catch (ReflectiveOperationException e) {
					log.error(I18n.get("log.64cad7b61530", tmp.getName(), e));
				}
			}
		}
		processor.onCompileDone();
	}

	/**
	 * 卸载前钩子（当前无操作）。
	 * Pre-unload hook (no-op).
	 *
	 * @param classes 将卸载的类 / Classes about to unload
	 */
	@Override
	public void preUnload(Class<?>[] classes) {

	}

	/**
	 * 判断类是否为可注册的公开具体命令实现。
	 * Whether the class is a public concrete Admin/Player command implementation.
	 *
	 * @param clazz 待检查类 / Class to check
	 * 若 valid 则为 true / True if valid
	 */
	public boolean isValidClass(Class<?> clazz) {
		final int modifiers = clazz.getModifiers();

		if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
			return false;
		}
		if (!Modifier.isPublic(modifiers)) {
			return false;
		}
		if (!ClassUtils.isSubclass(clazz, AdminCommand.class) && !ClassUtils.isSubclass(clazz, PlayerCommand.class)) {
			return false;
		}
		return true;
	}
}
