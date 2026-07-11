package com.aionemu.gameserver.controllers.movement;

/**
 * 移动状态掩码常量，用于客户端同步与 NPC 行走/跑步动画。
 * Movement state mask constants for client sync and NPC walk/run animations.
 */
public class MovementMask {

	/** 立即停止 / Immediate stop */
	public static final byte IMMEDIATE = (byte) 0x00;
	/** 滑翔 / Glide */
	public static final byte GLIDE = (byte) 0x04;
	/** 坠落 / Fall */
	public static final byte FALL = (byte) 0x08;
	/** 载具 / Vehicle */
	public static final byte VEHICLE = (byte) 0x10;
	/** 鼠标点击移动 / Mouse-click move */
	public static final byte MOUSE = (byte) 0x20;
	/** 玩家开始移动 / Player start move */
	public static final byte STARTMOVE = (byte) 0xC0;
	/** NPC 慢走 / NPC walk slow */
	public static final byte NPC_WALK_SLOW = (byte) 0xEA;
	/** NPC 快走 / NPC walk fast */
	public static final byte NPC_WALK_FAST = (byte) 0xE8;
	/** NPC 慢跑 / NPC run slow */
	public static final byte NPC_RUN_SLOW = (byte) 0xE4;
	/** NPC 快跑 / NPC run fast */
	public static final byte NPC_RUN_FAST = (byte) 0xE2;
	/** NPC 开始移动 / NPC start move */
	public static final byte NPC_STARTMOVE = (byte) 0xE0;
}
