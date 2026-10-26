package com.aionemu.boot.web.config;

/**
 * 配置值的生效来源：控制台据此告知运维「改这里是否真的会生效」。
 * The effective source of a configuration value: the console uses it to tell the operator whether editing
 * this key actually takes effect.
 *
 * @author AionEmu
 */
public enum ValueSource {

	/**
	 * 值来自被编辑的配置文件本身。
	 * The value comes from the configuration file being edited.
	 */
	FILE,

	/**
	 * 值来自运维覆盖层（游戏服 {@code mygs.properties}、登录服 {@code myls.properties}、
	 * 聊天服 {@code mycs.properties}）。
	 * The value comes from the operator override layer (the game server's {@code mygs.properties}, the login
	 * server's {@code myls.properties}, the chat server's {@code mycs.properties}).
	 */
	OVERRIDE_LAYER,

	/**
	 * 值来自 {@code application.yml}。
	 * The value comes from {@code application.yml}.
	 */
	APPLICATION_YML,

	/**
	 * 值来自操作系统环境变量。
	 * The value comes from an operating-system environment variable.
	 */
	ENVIRONMENT,

	/**
	 * 值来自命令行参数。
	 * The value comes from a command-line argument.
	 */
	COMMAND_LINE,

	/**
	 * 值来自 JVM 系统属性。
	 * The value comes from a JVM system property.
	 */
	SYSTEM_PROPERTY,

	/**
	 * 键在配置文件中不存在，取值为配置类声明的默认值。
	 * The key is absent from the configuration files and the value is the configuration class's declared
	 * default.
	 */
	DEFAULT
}
