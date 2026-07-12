package com.aionemu.gameserver.model;

/**
 * 聊天类型枚举。
 * Chat type enumeration.
 *
 * @author SoulKeeper, Imaginary
 */
public enum ChatType {
	/** 普通（白） / Normal (white) */
	NORMAL(0x00),
	/** 喊话（橙） / Shout (orange) */
	SHOUT(0x03),
	/** 密语（绿） / Whisper (green) */
	WHISPER(0x04),
	/** 小队（蓝） / Group (blue) */
	GROUP(0x05),
	/** 联盟（青） / Alliance (aqua) */
	ALLIANCE(0x06),
	/** 小队长 / Group leader*/
	GROUP_LEADER(0x07),
	/** 战团（深蓝） / League (dark blue) */
	LEAGUE(0x08),
	/** 战团警报（橙） / League alert (orange) */
	LEAGUE_ALERT(0x09),
	/** 军团（绿） / Legion (green) */
	LEGION(0x0A),
	/** 喊话2（橙，仅「全部」页） / Shout2 (orange, All tab only) */
	SHOUT2(0x0C),
	/** 命令（黄） / Command (yellow) */
	COMMAND(0x1A),
	/** 同盟（蓝） / Coalition (blue) */
	COALITION(0x1B),
	/** 同盟警报（橙） / Coalition alert (orange) */
	COALITION_ALERT(0x1C),
	/** 公告 / Announce */
	ANNOUNCE(0x37),

	/** 频道 1–10 / Channels 1–10 */
	CH1(0x0E), CH2(0x0F), CH3(0x10), CH4(0x11), CH5(0x12),
	CH6(0x13), CH7(0x14), CH8(0x15), CH9(0x16), CH10(0x17),

	/** 全局：系统白字（低） / Global: white low (GM/system) */
	WHITE_LOW(0x1F, true),
	/** 全局：金黄系统消息 / Global: golden yellow system */
	GOLDEN_YELLOW(0x20, true),
	/** 全局：黄色系统消息 / Global: yellow system */
	YELLOW(0x22, true),
	/** 全局：白色系统消息 / Global: white system */
	WHITE(0x23, true),
	/** 全局：亮黄系统消息 / Global: bright yellow system */
	BRIGHT_YELLOW(0x25, true),
	/** 全局：白字居中公告 / Global: white center notice */
	WHITE_CENTER(0x26, true),
	/** 全局：黄字居中公告 / Global: yellow center announcement */
	YELLOW_CENTER(0x27, true),
	/** 全局：亮黄居中系统公告 / Global: bright yellow center notice */
	BRIGHT_YELLOW_CENTER(0x28, true);

	private final int intValue;
	private boolean sysMsg;

	/**
	 * 以客户端整型表示构造聊天类型。
	 * Constructs chat type from client integer representation.
	 *
	 * @param intValue 客户端整型值 / client integer value
	 */
	private ChatType(int intValue) {
		this(intValue, false);
	}

	/**
	 * 转为客户端整型表示。
	 * Converts to client integer representation.
	 *
	 * @return 客户端聊天类型值 / client chat type value
	 */
	public int toInteger() {
		return intValue;
	}

	/**
	 * 按客户端整型值查找聊天类型。
	 * Returns chat type by client integer representation.
	 *
	 * @param integerValue 客户端整型值 / client integer value
	 * chat type
	 *
	 * @param integerValue
	 * @throws IllegalArgumentException 无匹配类型时 / if no matching type
	 */
	public static ChatType getChatTypeByInt(int integerValue) throws IllegalArgumentException {
		for (ChatType ct : ChatType.values()) {
			if (ct.toInteger() == integerValue) {
				return ct;
			}
		}
		throw new IllegalArgumentException("Unsupported chat type: " + integerValue);
	}

	private ChatType(int intValue, boolean sysMsg) {
		this.intValue = intValue;
		this.sysMsg = sysMsg;
	}

	/**
	 * 是否为全种族可见的系统消息类型。
	 * Whether this is a system message readable by all races.
	 *
	 * @return 是系统消息则为 true / true if system message
	 */
	public boolean isSysMsg() {
		return sysMsg;
	}
}
