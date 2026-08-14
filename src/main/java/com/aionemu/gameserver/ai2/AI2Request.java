package com.aionemu.gameserver.ai2;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * AI 向玩家发起请求（如确认窗口）的回调抽象。
 * Abstract callback for AI-initiated player requests (e.g. confirmation windows).
 *
 * @author ATracer
 */
public abstract class AI2Request {

	/**
	 * 玩家接受请求时调用。
	 * Invoked when the player accepts the request.
	 *
	 * @param requester 请求发起者 / request initiator
	 * @param responder 应答玩家 / responding player
	 */
	public abstract void acceptRequest(Creature requester, Player responder);

	/**
	 * 玩家拒绝请求时调用；默认空实现。
	 * Invoked when the player denies the request; empty by default.
	 *
	 * @param requester 请求发起者 / request initiator
	 * @param responder 应答玩家 / responding player
	 */
	public void denyRequest(Creature requester, Player responder) {
	};
}
