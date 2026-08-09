package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.cp.PlayerCPList;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_LIST;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Real transactional equipment port used by typed quest actions.
 *
 * <p>Unequipping is deliberately applied before inventory removal. Both the
 * equipment slots and the inventory projection are snapshotted so a later
 * JDBC/state failure restores the pre-event ownership.</p>
 */
public final class PlayerQuestEquipmentPort implements QuestEquipmentPort {
	private final QuestPlayerPort players;
	private final InventoryDAO inventoryDao;
	private final PlayerStigmasEquippedDAO stigmaDao;

	public PlayerQuestEquipmentPort(QuestPlayerPort players, InventoryDAO inventoryDao,
		PlayerStigmasEquippedDAO stigmaDao) {
		this.players = Objects.requireNonNull(players, "players");
		this.inventoryDao = Objects.requireNonNull(inventoryDao, "inventoryDao");
		this.stigmaDao = Objects.requireNonNull(stigmaDao, "stigmaDao");
	}

	@Override
	public void preflight(Connection connection, QuestSnapshot snapshot,
		List<QuestAction.UnequipItem> unequips) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		if (snapshot.equipmentFacts() == null) {
			throw new SQLException("equipment facts are not captured for player " + snapshot.playerId());
		}
		for (QuestAction.UnequipItem unequip : unequips) {
			if (unequip.itemId() <= 0) {
				throw new SQLException("invalid equipment item for player " + snapshot.playerId());
			}
		}
	}

	@Override
	public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
		List<QuestAction.UnequipItem> unequips) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		Player player = players.find(snapshot.playerId());
		if (player == null || player.getEquipment() == null || player.getInventory() == null
			|| player.getSkillList() == null || player.getEquipedStigmaList() == null
			|| player.getCP() == null) {
			throw new SQLException("player equipment is unavailable: " + snapshot.playerId());
		}
		var equipmentSnapshot = player.getEquipment().transactionSnapshot();
		var inventorySnapshot = player.getInventory().transactionSnapshot();
		var skillSnapshot = player.getSkillList().transactionSnapshot();
		var stigmaSnapshot = player.getEquipedStigmaList().transactionSnapshot();
		PlayerCPList.TransactionSnapshot cpSnapshot = player.getCP().transactionSnapshot();
		int linkedSkill = player.getLinkedSkill();
		int stigmaSet = player.getStigmaSet();
		int creativityPoint = player.getCreativityPoint();
		boolean powerShard = player.isInState(CreatureState.POWERSHARD);
		List<Item> originallyEquipped = List.copyOf(player.getEquipment().getEquippedItems());
		QuestInventoryPersistenceStage inventoryStage = QuestInventoryPersistenceStage.none();
		try {
			for (QuestAction.UnequipItem unequip : unequips) {
				for (Item item : List.copyOf(player.getEquipment().getEquippedItemsByItemId(unequip.itemId()))) {
					if (player.getEquipment().unEquipItemInTransaction(item.getObjectId(), 0) == null) {
						throw new SQLException("failed to unequip item " + unequip.itemId()
							+ " for player " + snapshot.playerId());
					}
				}
				if (unequip.removeReturnedCount() > 0) {
					int available = (int) Math.min(Integer.MAX_VALUE,
						player.getInventory().getItemCountByItemId(unequip.itemId()));
					int removeCount = Math.min(available, unequip.removeReturnedCount());
					if (removeCount > 0
						&& !player.getInventory().decreaseByItemId(unequip.itemId(), removeCount)) {
						throw new SQLException("failed to remove returned item " + unequip.itemId()
							+ " for player " + snapshot.playerId());
					}
				}
			}
			List<Item> runtimeItemsToRestore = originallyEquipped.stream()
				.filter(item -> !item.isEquipped())
				.toList();
			List<Item> dirty = List.copyOf(player.getDirtyItemsToUpdate());
			inventoryStage = QuestInventoryPersistenceStage.persist(inventoryDao, connection, player, dirty);
			stigmaDao.storeItemsInTransaction(connection, player);
			final QuestInventoryPersistenceStage committedInventoryStage = inventoryStage;
			return QuestTransactionParticipant.of(() -> {
				committedInventoryStage.afterCommit();
				stigmaDao.markStored(player);
			}, () -> restore(player, inventorySnapshot, equipmentSnapshot, skillSnapshot,
				stigmaSnapshot, cpSnapshot, creativityPoint, powerShard, runtimeItemsToRestore,
				linkedSkill, stigmaSet, committedInventoryStage));
		} catch (SQLException | RuntimeException failure) {
			try {
				restore(player, inventorySnapshot, equipmentSnapshot, skillSnapshot,
					stigmaSnapshot, cpSnapshot, creativityPoint, powerShard, originallyEquipped.stream()
						.filter(item -> !item.isEquipped())
						.toList(), linkedSkill, stigmaSet, inventoryStage);
			} catch (RuntimeException restoreFailure) {
				failure.addSuppressed(restoreFailure);
			}
			throw failure;
		}
	}

	private static void restore(Player player,
		com.aionemu.gameserver.model.items.storage.Storage.TransactionSnapshot inventorySnapshot,
		com.aionemu.gameserver.model.gameobjects.player.Equipment.TransactionSnapshot equipmentSnapshot,
		com.aionemu.gameserver.model.skill.PlayerSkillList.TransactionSnapshot skillSnapshot,
		com.aionemu.gameserver.model.skill.linked_skill.PlayerEquippedStigmaList.TransactionSnapshot stigmaSnapshot,
		PlayerCPList.TransactionSnapshot cpSnapshot, int creativityPoint, boolean powerShard,
		List<Item> runtimeItemsToRestore,
		int linkedSkill, int stigmaSet, QuestInventoryPersistenceStage inventoryStage) {
		inventoryStage.afterRollback();
		inventorySnapshot.restore();
		equipmentSnapshot.restore();
		stigmaSnapshot.restore();
		cpSnapshot.restore();
		player.setCreativityPoint(creativityPoint);
		player.setLinkedSkill(linkedSkill);
		player.setStigmaSet(stigmaSet);
		for (Item item : runtimeItemsToRestore) {
			player.getEquipment().restoreItemRuntimeState(item);
		}
		skillSnapshot.restore();
		if (powerShard) {
			player.setState(CreatureState.POWERSHARD);
		} else {
			player.unsetState(CreatureState.POWERSHARD);
		}
		PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getBasicSkills()));
		PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getLinkedSkills()));
		PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
	}
}
