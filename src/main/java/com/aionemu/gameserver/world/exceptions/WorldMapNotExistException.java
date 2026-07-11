package com.aionemu.gameserver.world.exceptions;

/**
 * 引用了不存在的世界地图时抛出，通常表示严重错误。
 * Thrown when an object references a world map that does not exist; indicates a serious error.
 *
 * @author -Nemesiss-
 */
@SuppressWarnings("serial")
public class WorldMapNotExistException extends RuntimeException {

	/**
	 * 构造无详细消息的异常。
	 * Constructs a {@code WorldMapNotExistException} with no detail message.
	 */
	public WorldMapNotExistException() {
		super();
	}

	/**
	 * 使用指定详细消息构造异常。
	 * Constructs a {@code WorldMapNotExistException} with the specified detail message.
	 *
	 * @param s 详细消息 / the detail message
	 */
	public WorldMapNotExistException(String s) {
		super(s);
	}
}
