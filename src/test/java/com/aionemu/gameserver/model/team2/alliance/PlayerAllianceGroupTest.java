package com.aionemu.gameserver.model.team2.alliance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerAllianceGroupTest {

	@Test
	void reportsMemberLevelRange() {
		TestGroup group = new TestGroup();
		group.addForTest(new TestMember(1, 42));
		group.addForTest(new TestMember(2, 75));

		assertEquals(42, group.getMinExpPlayerLevel());
		assertEquals(75, group.getMaxExpPlayerLevel());
	}

	private static class TestGroup extends PlayerAllianceGroup {
		TestGroup() {
			super(null, 1000);
		}

		void addForTest(PlayerAllianceMember member) {
			members.put(member.getObjectId(), member);
		}
	}

	private static class TestMember extends PlayerAllianceMember {
		private final int objectId;
		private final byte level;

		TestMember(int objectId, int level) {
			super(null);
			this.objectId = objectId;
			this.level = (byte) level;
		}

		@Override
		public Integer getObjectId() {
			return objectId;
		}

		@Override
		public byte getLevel() {
			return level;
		}
	}
}
