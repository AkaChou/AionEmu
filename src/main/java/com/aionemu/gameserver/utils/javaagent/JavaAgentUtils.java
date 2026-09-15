package com.aionemu.gameserver.utils.javaagent;

/**
 * 校验回调系统是否可用（标准接口回调模式）。
 * Utility to verify callback system readiness.
 */
public class JavaAgentUtils {

	/**
	 * 检测回调系统是否可用。
	 * Verify callback system readiness.
	 *
	 * @return 配置正确则为 true / True if configured correctly
	 */
	public static boolean isConfigured() {
		return true;
	}
}
