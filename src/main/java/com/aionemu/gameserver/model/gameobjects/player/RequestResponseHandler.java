package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 请求响应处理器。
 * Request Response Handler game object.
 *
 * @author Ben
 * @modified Lyahim
 */
public abstract class RequestResponseHandler {

	private Creature requester;

	public RequestResponseHandler(Creature requester) {
		this.requester = requester;
	}

	/**
	 * 收到响应时调用。
	 * Called when a response is received.
	 */
	public void handle(Player responder, int response) {
		if (response == 0) {
			denyRequest(requester, responder);
		} else {
			acceptRequest(requester, responder);
		}
	}

	/**
	 * 玩家接受请求时调用。
	 * Called when the player accepts a request.
	 */
	public abstract void acceptRequest(Creature requester, Player responder);

	/**
	 * 玩家拒绝请求时调用。
	 * Called when the player denies a request.
	 */
	public abstract void denyRequest(Creature requester, Player responder);

}
