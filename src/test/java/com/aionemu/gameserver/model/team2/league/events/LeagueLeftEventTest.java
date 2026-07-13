package com.aionemu.gameserver.model.team2.league.events;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceMember;
import com.aionemu.gameserver.model.team2.league.League;
import com.aionemu.gameserver.model.team2.league.LeagueMember;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LeagueLeftEventTest {

	@Test
	void keepsLastInAreaAllianceAndPromotesItToLeader() {
		FakeIDFactory idFactory = new ObjenesisStd().newInstance(FakeIDFactory.class);
		GameWorldBootstrapServices services = new GameWorldBootstrapServices(provider(IDFactory.class, idFactory),
				null, null, null, null);
		try {
			PlayerAlliance leaving = alliance(TeamType.IN_AREA_DEFAULT);
			PlayerAlliance remaining = alliance(TeamType.IN_AREA_DEFAULT);
			League league = new League(new LeagueMember(leaving, 0));
			league.addMember(new LeagueMember(leaving, 0));
			league.addMember(new LeagueMember(remaining, 1));
			league.removeMember(leaving.getObjectId());

			new LeagueLeftEvent(league, leaving).checkDisband();

			assertSame(remaining, league.getLeaderObject());
		} finally {
			services.destroy();
		}
	}

	private static PlayerAlliance alliance(TeamType type) {
		return new PlayerAlliance(new PlayerAllianceMember(null), type);
	}

	private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(type.getName(), instance);
		return beanFactory.getBeanProvider(type);
	}

	private static final class FakeIDFactory extends IDFactory {
		private int nextId;

		@Override
		public int nextId() {
			return ++nextId;
		}
	}
}
