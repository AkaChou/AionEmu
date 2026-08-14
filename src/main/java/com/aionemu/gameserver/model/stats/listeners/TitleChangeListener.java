package com.aionemu.gameserver.model.stats.listeners;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.stats.container.CreatureGameStats;
import com.aionemu.gameserver.model.templates.TitleTemplate;

/**
 * 称号 Change 监听器，用于属性相关逻辑。
 * Title Change Listener for stats logic.
 */

public class TitleChangeListener {
	/** 称号加成变更时 / On bonus title change */
	public static void onBonusTitleChange(CreatureGameStats<?> cgs, int titleId, boolean isSet) {
		TitleTemplate tt = DataManager.TITLE_DATA.getTitleTemplate(titleId);
		if (tt == null) {
			return;
		}
		if (!isSet) {
			cgs.endEffect(tt);
		} else {
			cgs.addEffect(tt, tt.getModifiers());
		}
	}
}
