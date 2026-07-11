package com.aionemu.gameserver.model.team2.alliance.callback;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;

/**
 * 玩家联盟 DisbandCallback，用于团队2相关逻辑。
 * Player Alliance Disband Callback for team 2 logic.
 *
 * @author ATracer
 */
@SuppressWarnings("rawtypes")
public abstract class PlayerAllianceDisbandCallback implements Callback {

	/** 调用前 / before Call. */
	@Override
	public CallbackResult beforeCall(Object obj, Object[] args) {
		onBeforeAllianceDisband((PlayerAlliance) args[0]);
		return CallbackResult.newContinue();
	}

	/** 调用后 / after Call. */
	@Override
	public CallbackResult afterCall(Object obj, Object[] args, Object methodResult) {
		onAfterAllianceDisband((PlayerAlliance) args[0]);
		return CallbackResult.newContinue();
	}

	/** 获取基础职业。 / Returns the base class. */
	@Override
	public Class<? extends Callback> getBaseClass() {
		return PlayerAllianceDisbandCallback.class;
	}

	/** 在 alliance disband 前 / On Before Alliance Disband */
	public abstract void onBeforeAllianceDisband(PlayerAlliance alliance);

	/** 在 alliance disband 后 / On After Alliance Disband */
	public abstract void onAfterAllianceDisband(PlayerAlliance alliance);
}
