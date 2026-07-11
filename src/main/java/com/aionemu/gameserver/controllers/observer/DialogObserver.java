package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 对话距离观察者：玩家移出最大对话距离时回调。
 * Dialog distance observer: callback when the player moves beyond max dialog range.
 *
 * @author nrg
 */
public abstract class DialogObserver extends ActionObserver {

	/** 对话响应玩家 / Dialog responder player */
	private Player responder;
	/** 对话请求方（NPC 等） / Dialog requester (NPC, etc.) */
	private Creature requester;
	/** 最大允许距离 / Max allowed distance */
	private int maxDistance;

	/**
	 * requester
	 * responder player
	 * max distance
	 */
	public DialogObserver(Creature requester, Player responder, int maxDistance) {
		super(ObserverType.MOVE);
		this.responder = responder;
		this.requester = requester;
		this.maxDistance = maxDistance;
	}

	@Override
	public void moved() {
		if (!MathUtil.isIn3dRange(responder, requester, maxDistance)) {
			tooFar(requester, responder);
		}
	}

	/**
	 * 玩家距离对话对象过远时调用。
	 * Called when the player is too far from the dialog-serving object.
	 *
	 * requester
	 * responder player
	 */
	public abstract void tooFar(Creature requester, Player responder);
}
