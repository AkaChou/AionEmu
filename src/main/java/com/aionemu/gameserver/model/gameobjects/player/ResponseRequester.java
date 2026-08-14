package com.aionemu.gameserver.model.gameobjects.player;

import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;

/**
 * 响应 Requester 游戏对象。
 * Response Requester game object.
 *
 * @author Ben
 */
@Slf4j
public class ResponseRequester {

	private Player player;
	private HashMap<Integer, RequestResponseHandler> map = new HashMap<Integer, RequestResponseHandler>();

	public ResponseRequester(Player player) {
		this.player = player;
	}

	/**
	 * 为此消息 ID 添加处理器，已存在时返回 false。
	 * Adds this handler to this messageID, returns false if there already exists one
	 *
	 * @param messageId 请求消息 ID / ID of the request message
	 * @return 是否成功 / true or false
	 */
	public synchronized boolean putRequest(int messageId, RequestResponseHandler handler) {
		if (map.containsKey(messageId))
			return false;

		map.put(messageId, handler);
		return true;
	}

	/**
	 * 以给定响应对给定消息 ID 作答，返回是否成功。
	 * Responds to the given message ID with the given response, returns success.
	 */
	public synchronized boolean respond(int messageId, int response) {
		RequestResponseHandler handler = map.get(messageId);
		if (handler != null) {
			map.remove(messageId);
			log.debug("RequestResponseHandler triggered for response code " + messageId + " from " + player.getName());
			handler.handle(player, response);
			return true;
		}
		return false;
	}

	/**
	 * 对所有请求自动以 0 响应，传入给定玩家为响应者。
	 * Automatically responds 0 to all requests, passing the given player as the responder
	 */
	public synchronized void denyAll() {
		for (RequestResponseHandler handler : map.values()) {
			handler.handle(player, 0);
		}
		map.clear();
	}
}
