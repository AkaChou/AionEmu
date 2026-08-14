package com.aionemu.gameserver.model.team2.group.callback;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家队伍创建回调，用于团队 2 相关逻辑。
 * Player Group Create Callback for team 2 logic.
 *
 * @author ATracer
 */
@SuppressWarnings("rawtypes")
public abstract class PlayerGroupCreateCallback implements Callback {

	/** 调用前 / before Call. */
	@Override
	public CallbackResult beforeCall(Object obj, Object[] args) {
		onBeforeGroupCreate((Player) args[0]);
		return CallbackResult.newContinue();
	}

	/** 调用后 / after Call. */
	@Override
	public CallbackResult afterCall(Object obj, Object[] args, Object methodResult) {
		onAfterGroupCreate((Player) args[0]);
		return CallbackResult.newContinue();
	}

	/** 获取回调基础类。 / Returns the base class. */
	@Override
	public Class<? extends Callback> getBaseClass() {
		return PlayerGroupCreateCallback.class;
	}

	/** 队伍创建前 / On Before Group Create */
	public abstract void onBeforeGroupCreate(Player player);

	/** 队伍创建后 / On After Group Create */
	public abstract void onAfterGroupCreate(Player player);
}
