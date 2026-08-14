package com.aionemu.gameserver.model.team2.alliance.callback;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;

/**
 * 玩家加入联盟回调，用于团队2相关逻辑。
 * Add Player To Alliance Callback for team 2 logic.
 *
 * @author ATracer
 */
@SuppressWarnings("rawtypes")
public abstract class AddPlayerToAllianceCallback implements Callback {

	/** 调用前 / before Call. */
	@Override
	public CallbackResult beforeCall(Object obj, Object[] args) {
		onBeforePlayerAddToAlliance((PlayerAlliance) args[0], (Player) args[1]);
		return CallbackResult.newContinue();
	}

	/** 调用后 / after Call. */
	@Override
	public CallbackResult afterCall(Object obj, Object[] args, Object methodResult) {
		onAfterPlayerAddToAlliance((PlayerAlliance) args[0], (Player) args[1]);
		return CallbackResult.newContinue();
	}

	/** 返回基础回调类。 / Returns the base callback class. */
	@Override
	public Class<? extends Callback> getBaseClass() {
		return AddPlayerToAllianceCallback.class;
	}

	/** 玩家加入联盟前 / Before a player is added to the alliance */
	public abstract void onBeforePlayerAddToAlliance(PlayerAlliance alliance, Player player);

	/** 玩家加入联盟后 / After a player is added to the alliance */
	public abstract void onAfterPlayerAddToAlliance(PlayerAlliance alliance, Player player);
}
