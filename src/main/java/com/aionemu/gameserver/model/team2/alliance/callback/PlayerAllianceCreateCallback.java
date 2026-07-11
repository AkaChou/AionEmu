package com.aionemu.gameserver.model.team2.alliance.callback;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家联盟 CreateCallback，用于团队2相关逻辑。
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

	/** 获取基础职业。 / Returns the base class. */
	@Override
	public Class<? extends Callback> getBaseClass() {
		return PlayerAllianceCreateCallback.class;
	}

	/** 在 alliance create 前 / On Before Alliance Create */
	public abstract void onBeforeAllianceCreate(Player player);

	/** 在 alliance create 后 / On After Alliance Create */
	public abstract void onAfterAllianceCreate(Player player);
}
