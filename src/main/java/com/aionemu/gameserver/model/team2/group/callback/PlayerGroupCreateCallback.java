package com.aionemu.gameserver.model.team2.group.callback;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家队伍 CreateCallback，用于团队2相关逻辑。
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

	/** 获取基础职业。 / Returns the base class. */
	@Override
	public Class<? extends Callback> getBaseClass() {
		return PlayerGroupCreateCallback.class;
	}

	/** 在 group create 前 / On Before Group Create */
	public abstract void onBeforeGroupCreate(Player player);

	/** 在 group create 后 / On After Group Create */
	public abstract void onAfterGroupCreate(Player player);
}
