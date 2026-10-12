package com.aionemu.gameserver.services;

import com.aionemu.gameserver.persistence.dao.impl.WebRewardDaoImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.persistence.dao.WebRewardDao;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;
import com.aionemu.gameserver.persistence.dao.impl.InventoryDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.ItemStoneDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.MailDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerDaoImpl;
import com.aionemu.gameserver.services.mail.SystemMailService;
import java.util.List;
import org.junit.jupiter.api.Test;

class WebshopServiceTest {

	@Test
	void deliversWebRewardAsExpressSystemMail() {
		FakeRewardServiceDAO rewardDao = new FakeRewardServiceDAO();
		RecordingSystemMailService mailService = new RecordingSystemMailService(true);

		boolean delivered = WebshopService.deliverRewardMail("角色A",
				new RewardEntryItem(11, 188052, 3), rewardDao, mailService);

		assertTrue(delivered);
		assertEquals(11, rewardDao.updatedUnique);
		assertEquals("角色A", mailService.recipientName);
		assertEquals(188052, mailService.itemId);
		assertEquals(3, mailService.itemCount);
		assertEquals(LetterType.EXPRESS, mailService.letterType);
	}

	private static class RecordingSystemMailService extends SystemMailService {
		private final boolean result;
		private String recipientName;
		private int itemId;
		private long itemCount;
		private LetterType letterType;

		private RecordingSystemMailService(boolean result) {
			super(new PlayerDaoImpl(), new InventoryDaoImpl(), new MailDaoImpl(new ItemStoneDaoImpl()));
			this.result = result;
		}

		@Override
		public boolean sendMail(String sender, String recipientName, String title, String message, int attachedItemObjId,
				long attachedItemCount, long attachedKinahCount, long attachedAPCount, LetterType letterType) {
			this.recipientName = recipientName;
			this.itemId = attachedItemObjId;
			this.itemCount = attachedItemCount;
			this.letterType = letterType;
			return result;
		}
	}

	private static class FakeRewardServiceDAO extends WebRewardDaoImpl {
		private int updatedUnique;

		@Override
		public List<RewardEntryItem> getAvailable(int playerId) {
			return List.of();
		}

		@Override
		public void uncheckAvailable(List<Integer> ids) {
		}

		@Override
		public void setUpdateDown(int unique) {
		}

		@Override
		public boolean setUpdate(int unique) {
			this.updatedUnique = unique;
			return true;
		}
	}
}
