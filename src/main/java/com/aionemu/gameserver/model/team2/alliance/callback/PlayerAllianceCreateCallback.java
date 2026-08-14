package com.aionemu.gameserver.model.team2.alliance.callback;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家联盟创建回调，用于团队2相关逻辑。
 * Player Alliance Create Callback for team 2 logic.
 */

@SuppressWarnings("rawtypes")
public abstract class PlayerAllianceCreateCallback implements Callback {
	/** 调用前 / before Call. */
	@Override
	public CallbackResult beforeCall(Object obj, Object[] args) {
		onBeforeAllianceCreate((Player) args[0]);
		return CallbackResult.newContinue();
	}

	/** 调用后 / after Call. */
	@Override
	public CallbackResult afterCall(Object obj, Object[] args, Object methodResult) {
		onAfterAllianceCreate((Player) args[0]);
		return CallbackResult.newContinue();
	}

	/** 返回基础回调类。 / Returns the base callback class. */
	@Override
	public Class<? extends Callback> getBaseClass() {
		return PlayerAllianceCreateCallback.class;
	}

	/** 联盟创建前 / Before an alliance is created */
	public abstract void onBeforeAllianceCreate(Player player);

	/** 联盟创建后 / After an alliance is created */
	public abstract void onAfterAllianceCreate(Player player);
}
