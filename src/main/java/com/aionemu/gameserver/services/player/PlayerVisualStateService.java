package com.aionemu.gameserver.services.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 玩家可视状态服务，校验隐身与可见性。
 * Player visual state service validating hide and see conditions.
 */
public class PlayerVisualStateService {

	/**
	 * 校验隐身可见性。
	 * Validates hide visibility.
	 *
	 * @param hiden 是否隐藏 / hiden
	 */
	public static void hideValidate(final Player hiden) {
		hiden.getKnownList().doOnAllPlayers(new Visitor<Player>() {

			@Override
			/**
			 * visit 方法。
			 * visit method.
			 *
			 * observer
			 */
			public void visit(Player observer) {
				boolean canSee = observer.canSee(hiden);
				boolean isSee = observer.isSeePlayer(hiden);

				if (canSee && !isSee) {
					observer.getKnownList().addVisualObject(hiden);
				} else if (!canSee && isSee) {
					observer.getKnownList().delVisualObject(hiden, false);
				}
			}
		});
	}

	/**
	 * 校验看见条件。
	 * Validates see conditions.
	 *
	 * search
	 */
	public static void seeValidate(final Player search) {
		search.getKnownList().doOnAllPlayers(new Visitor<Player>() {

			@Override
			/**
			 * visit 方法。
			 * visit method.
			 *
			 * hide
			 */
			public void visit(Player hide) {
				boolean canSee = search.canSee(hide);
				boolean isSee = search.isSeePlayer(hide);

				if (canSee && !isSee) {
					search.getKnownList().addVisualObject(hide);
				} else if (!canSee && isSee) {
					search.getKnownList().delVisualObject(hide, false);
				}
			}
		});
	}
}