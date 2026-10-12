package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.model.team.legion.LegionHistory;
import com.aionemu.gameserver.model.team.legion.LegionHistoryType;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequest;
import com.aionemu.gameserver.model.team.legion.LegionTerritory;
import com.aionemu.gameserver.model.team.legion.LegionWarehouse;
import com.aionemu.gameserver.persistence.dao.LegionDao;
import com.aionemu.gameserver.persistence.entity.InventoryPo;
import com.aionemu.gameserver.persistence.entity.LegionAnnouncementListPo;
import com.aionemu.gameserver.persistence.entity.LegionEmblemPo;
import com.aionemu.gameserver.persistence.entity.LegionHistoryPo;
import com.aionemu.gameserver.persistence.entity.LegionJoinRequestPo;
import com.aionemu.gameserver.persistence.entity.LegionPo;
import com.aionemu.gameserver.persistence.entity.SiegeLocationPo;
import com.aionemu.gameserver.persistence.mapper.InventoryMapper;
import com.aionemu.gameserver.persistence.mapper.LegionAnnouncementListMapper;
import com.aionemu.gameserver.persistence.mapper.LegionEmblemMapper;
import com.aionemu.gameserver.persistence.mapper.LegionHistoryMapper;
import com.aionemu.gameserver.persistence.mapper.LegionJoinRequestMapper;
import com.aionemu.gameserver.persistence.mapper.LegionMapper;
import com.aionemu.gameserver.persistence.mapper.SiegeLocationMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 军团数据访问的 MyBatis-Flex 实现。
 * MyBatis-Flex implementation of the legion data access layer.
 * <p>
 * legions 为主表；公告、徽章、历史、加入申请、仓库物品分别落到各自从表，通过注入对应 Mapper 直接操作。
 * legions is the main table; announcements, emblems, history, join requests and warehouse items live in their own
 * child tables and are handled by the corresponding injected mappers.
 * </p>
 */
@Slf4j
@Service
@Lazy
public class LegionDaoImpl extends ServiceImpl<LegionMapper, LegionPo> implements LegionDao {

	private final LegionAnnouncementListMapper announcementListMapper;
	private final LegionEmblemMapper emblemMapper;
	private final LegionHistoryMapper historyMapper;
	private final LegionJoinRequestMapper joinRequestMapper;
	private final InventoryMapper inventoryMapper;
	private final SiegeLocationMapper siegeLocationMapper;
	private final TransactionTemplate transactionTemplate;

	public LegionDaoImpl(LegionAnnouncementListMapper announcementListMapper, LegionEmblemMapper emblemMapper,
			LegionHistoryMapper historyMapper, LegionJoinRequestMapper joinRequestMapper,
			InventoryMapper inventoryMapper, SiegeLocationMapper siegeLocationMapper,
			TransactionTemplate transactionTemplate) {
		this.announcementListMapper = announcementListMapper;
		this.emblemMapper = emblemMapper;
		this.historyMapper = historyMapper;
		this.joinRequestMapper = joinRequestMapper;
		this.inventoryMapper = inventoryMapper;
		this.siegeLocationMapper = siegeLocationMapper;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public boolean isNameUsed(final String name) {
		try {
			return mapper.selectCountByQuery(QueryWrapper.create().where(LegionPo::getName).eq(name)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.84ada47a4666", name), e);
			return true;
		}
	}

	@Override
	public Collection<Integer> getLegionIdsWithTerritories() {
		Collection<Integer> legionIds = new ArrayList<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(LegionPo::getTerritory).gt(0))
				.forEach(po -> legionIds.add(po.getId()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.3117e02da50f"), e);
		}
		return legionIds;
	}

	@Override
	public void saveNewLegion(final Legion legion) {
		try {
			log.debug("Saving new legion: {} {}", legion.getLegionId(), legion.getLegionName());
			LegionPo po = new LegionPo();
			po.setId(legion.getLegionId());
			po.setName(legion.getLegionName());
			// 仅插入 id 与 name，其余列依赖数据库默认值 / Insert only id and name; other columns fall back to defaults
			mapper.insertSelective(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5560e48eed9e", legion.getLegionId()), e);
		}
	}

	@Override
	public void storeLegion(final Legion legion) {
		try {
			log.debug("Storing legion {} {}", legion.getLegionId(), legion.getLegionName());

			int territoryId = (legion.getTerritory() != null && legion.getTerritory().getId() > 0)
				? legion.getTerritory().getId() : 0;

			UpdateChain.of(LegionPo.class)
				.set(LegionPo::getName, legion.getLegionName())
				.set(LegionPo::getLevel, legion.getLegionLevel())
				.set(LegionPo::getContributionPoints, legion.getContributionPoints())
				.set(LegionPo::getDeputyPermission, legion.getDeputyPermission())
				.set(LegionPo::getCenturionPermission, legion.getCenturionPermission())
				.set(LegionPo::getLegionaryPermission, legion.getLegionaryPermission())
				.set(LegionPo::getVolunteerPermission, legion.getVolunteerPermission())
				.set(LegionPo::getDisbandTime, legion.getDisbandTime())
				.set(LegionPo::getDescription, legion.getLegionDescription())
				.set(LegionPo::getJoinType, legion.getLegionJoinType())
				.set(LegionPo::getMinJoinLevel, legion.getMinLevel())
				.set(LegionPo::getTerritory, territoryId)
				.where(LegionPo::getId).eq(legion.getLegionId())
				.update();

			if (!legion.getJoinRequestMap().isEmpty()) {
				for (LegionJoinRequest ljr : legion.getJoinRequestMap().values()) {
					storeLegionJoinRequest(ljr);
				}
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.9f74efb13ee5", legion.getLegionId()), e);
		}
	}

	@Override
	public Legion loadLegion(final String legionName) {
		Legion legion = null;
		try {
			LegionPo po = mapper.selectOneByQuery(QueryWrapper.create().where(LegionPo::getName).eq(legionName));
			legion = po == null ? null : toLegion(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ef450357e2bb", legionName), e);
		}

		log.debug("Loaded legion: {}", legion != null ? legion.getLegionId() : "null");
		return legion;
	}

	@Override
	public Legion loadLegion(final int legionId) {
		Legion legion = null;
		try {
			LegionPo po = mapper.selectOneById(legionId);
			if (po != null) {
				legion = toLegion(po);
				for (LegionJoinRequest ljr : loadLegionJoinRequests(legion.getLegionId())) {
					legion.addJoinRequest(ljr);
				}
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.bd9d366b0d7a", legionId), e);
		}

		log.debug("Loaded legion: {}", legion != null ? legion.getLegionId() : "null");
		return legion;
	}

	@Override
	public void deleteLegion(int legionId) {
		try {
			// 删除军团并清除其攻城归属，二者保持同一事务 / Delete the legion and clear its siege ownership atomically
			transactionTemplate.executeWithoutResult(_ -> {
				mapper.deleteById(legionId);
				UpdateChain.of(SiegeLocationPo.class)
					.set(SiegeLocationPo::getLegionId, 0)
					.where(SiegeLocationPo::getLegionId).eq(legionId)
					.update();
			});
		} catch (RuntimeException e) {
			log.error(I18n.get("log.9be4d3aa4400", legionId), e);
		}
	}

	@Override
	public int[] getUsedIDs() {
		try {
			List<LegionPo> legions = mapper.selectListByQuery(QueryWrapper.create());
			int[] result = new int[legions.size()];
			for (int i = 0; i < legions.size(); i++) {
				result[i] = legions.get(i).getId();
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.82d77c44202e"), e);
			return new int[0];
		}
	}

	@Override
	public TreeMap<Timestamp, String> loadAnnouncementList(final int legionId) {
		final TreeMap<Timestamp, String> announcementList = new TreeMap<>();
		try {
			announcementListMapper
				.selectListByQuery(QueryWrapper.create()
					.where(LegionAnnouncementListPo::getLegionId).eq(legionId)
					.orderBy(LegionAnnouncementListPo::getDate).asc()
					.limit(7))
				.forEach(po -> announcementList.put(po.getDate(), po.getAnnouncement()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c4ec74ecbd88", legionId), e);
		}

		log.debug("Loaded announcement list for legion: {}", legionId);
		return announcementList;
	}

	@Override
	public void saveNewAnnouncement(final int legionId, final Timestamp currentTime, final String message) {
		try {
			log.debug("Saving new announcement for legion: {}", legionId);
			LegionAnnouncementListPo po = new LegionAnnouncementListPo();
			po.setLegionId(legionId);
			po.setAnnouncement(message);
			po.setDate(currentTime);
			announcementListMapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d7d4b79baa01", legionId), e);
		}
	}

	@Override
	public void removeAnnouncement(int legionId, Timestamp unixTime) {
		try {
			announcementListMapper.deleteByQuery(QueryWrapper.create()
				.where(LegionAnnouncementListPo::getLegionId).eq(legionId)
				.and(LegionAnnouncementListPo::getDate).eq(unixTime));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.eb7e7af87a03", legionId), e);
		}
	}

	@Override
	public void storeLegionEmblem(final int legionId, final LegionEmblem legionEmblem) {
		if (!validEmblem(legionEmblem)) {
			return;
		}

		if (!checkEmblem(legionId)) {
			createLegionEmblem(legionId, legionEmblem);
		} else if (legionEmblem.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
			updateLegionEmblem(legionId, legionEmblem);
		}

		legionEmblem.setPersistentState(PersistentState.UPDATED);
	}

	@Override
	public LegionEmblem loadLegionEmblem(final int legionId) {
		LegionEmblem emblem = new LegionEmblem();
		boolean found = false;
		try {
			LegionEmblemPo po = emblemMapper.selectOneByQuery(QueryWrapper.create()
				.where(LegionEmblemPo::getLegionId).eq(legionId));
			if (po != null) {
				emblem.setEmblem(
					po.getEmblemId(),
					po.getColorR(),
					po.getColorG(),
					po.getColorB(),
					LegionEmblemType.valueOf(po.getEmblemType()),
					po.getEmblemData());
				found = true;
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.256b0f63755e", legionId), e);
		}

		if (!found) {
			emblem.setEmblem(0, 0, 0, 0, LegionEmblemType.DEFAULT, null);
		}

		emblem.setPersistentState(PersistentState.UPDATED);
		return emblem;
	}

	@Override
	public LegionWarehouse loadLegionStorage(Legion legion) {
		final LegionWarehouse inventory = new LegionWarehouse(legion);
		final int legionId = legion.getLegionId();
		final int storage = StorageType.LEGION_WAREHOUSE.getId();

		try {
			inventoryMapper
				.selectListByQuery(QueryWrapper.create()
					.where(InventoryPo::getItemOwner).eq(legionId)
					.and(InventoryPo::getItemLocation).eq(storage)
					.and(InventoryPo::getEquiped).eq(0))
				.forEach(po -> {
					Item item = constructItem(po, storage);
					item.setPersistentState(PersistentState.UPDATED);
					inventory.onLoadHandler(item);
				});
		} catch (Exception e) {
			log.error(I18n.get("log.d65e37a60b80", legionId), e);
		}

		return inventory;
	}

	@Override
	public void loadLegionHistory(final Legion legion) {
		final Collection<LegionHistory> history = legion.getLegionHistory();
		try {
			historyMapper
				.selectListByQuery(QueryWrapper.create()
					.where(LegionHistoryPo::getLegionId).eq(legion.getLegionId())
					.orderBy(LegionHistoryPo::getDate).asc())
				.forEach(po -> {
					try {
						history.add(new LegionHistory(
							LegionHistoryType.valueOf(po.getHistoryType()),
							po.getName(),
							po.getDate(),
							po.getTabId(),
							po.getDescription()));
					} catch (IllegalArgumentException e) {
						log.warn(I18n.get("log.cfe6b02e5699", legion.getLegionId(), po.getHistoryType()));
					}
				});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c1a020fe3695", legion.getLegionId()), e);
		}
	}

	@Override
	public void saveNewLegionHistory(final int legionId, final LegionHistory legionHistory) {
		try {
			LegionHistoryPo po = new LegionHistoryPo();
			po.setLegionId(legionId);
			po.setDate(legionHistory.getTime());
			po.setHistoryType(legionHistory.getLegionHistoryType().toString());
			po.setName(legionHistory.getName());
			po.setTabId(legionHistory.getTabId());
			po.setDescription(legionHistory.getDescription());
			historyMapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.db8f371bd4f0", legionId), e);
		}
	}

	@Override
	public void updateLegionDescription(final Legion legion) {
		try {
			UpdateChain.of(LegionPo.class)
				.set(LegionPo::getDescription, legion.getLegionDescription())
				.set(LegionPo::getJoinType, legion.getLegionJoinType())
				.set(LegionPo::getMinJoinLevel, legion.getMinLevel())
				.where(LegionPo::getId).eq(legion.getLegionId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.21551dfa9189", legion.getLegionId()), e);
		}
	}

	@Override
	public void storeLegionJoinRequest(final LegionJoinRequest legionJoinRequest) {
		try {
			LegionJoinRequestPo po = new LegionJoinRequestPo();
			po.setLegionId(legionJoinRequest.getLegionId());
			po.setPlayerId(legionJoinRequest.getPlayerId());
			po.setPlayerName(legionJoinRequest.getPlayerName());
			po.setPlayerClassId(legionJoinRequest.getPlayerClass());
			po.setPlayerRaceId(legionJoinRequest.getRace());
			po.setPlayerLevel(legionJoinRequest.getLevel());
			po.setPlayerGenderId(legionJoinRequest.getGenderId());
			po.setJoinRequestMsg(legionJoinRequest.getMsg());
			po.setDate(legionJoinRequest.getDate());
			joinRequestMapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d118ce68ab6e", legionJoinRequest.getLegionId(),
				legionJoinRequest.getPlayerId()), e);
		}
	}

	@Override
	public List<LegionJoinRequest> loadLegionJoinRequests(final int legionId) {
		final List<LegionJoinRequest> requestList = new ArrayList<>();
		try {
			joinRequestMapper
				.selectListByQuery(QueryWrapper.create()
					.where(LegionJoinRequestPo::getLegionId).eq(legionId)
					.orderBy(LegionJoinRequestPo::getDate).asc())
				.forEach(po -> {
					LegionJoinRequest ljr = new LegionJoinRequest();
					ljr.setLegionId(po.getLegionId());
					ljr.setPlayerId(po.getPlayerId());
					ljr.setPlayerName(po.getPlayerName());
					ljr.setPlayerClass(po.getPlayerClassId());
					ljr.setRace(po.getPlayerRaceId());
					ljr.setLevel(po.getPlayerLevel());
					ljr.setGenderId(po.getPlayerGenderId());
					ljr.setDate(po.getDate());
					requestList.add(ljr);
				});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.e21f88ccc6c6", legionId), e);
		}

		return requestList;
	}

	@Override
	public void deleteLegionJoinRequest(int legionId, int playerId) {
		try {
			joinRequestMapper.deleteByQuery(QueryWrapper.create()
				.where(LegionJoinRequestPo::getLegionId).eq(legionId)
				.and(LegionJoinRequestPo::getPlayerId).eq(playerId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c35e7fe03617", legionId, playerId), e);
		}
	}

	private boolean validEmblem(final LegionEmblem legionEmblem) {
		return !(legionEmblem.getEmblemType() == LegionEmblemType.CUSTOM && legionEmblem.getCustomEmblemData() == null);
	}

	private boolean checkEmblem(final int legionid) {
		try {
			return emblemMapper.selectCountByQuery(QueryWrapper.create()
				.where(LegionEmblemPo::getLegionId).eq(legionid)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5ee404f2d84f", legionid), e);
			return false;
		}
	}

	private void createLegionEmblem(final int legionId, final LegionEmblem legionEmblem) {
		try {
			LegionEmblemPo po = new LegionEmblemPo();
			po.setLegionId(legionId);
			po.setEmblemId(legionEmblem.getEmblemId());
			po.setColorR(legionEmblem.getColor_r());
			po.setColorG(legionEmblem.getColor_g());
			po.setColorB(legionEmblem.getColor_b());
			po.setEmblemType(legionEmblem.getEmblemType().toString());
			po.setEmblemData(legionEmblem.getCustomEmblemData());
			emblemMapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0a3029c9bb09", legionId), e);
		}
	}

	private void updateLegionEmblem(final int legionId, final LegionEmblem legionEmblem) {
		try {
			UpdateChain.of(LegionEmblemPo.class)
				.set(LegionEmblemPo::getEmblemId, legionEmblem.getEmblemId())
				.set(LegionEmblemPo::getColorR, legionEmblem.getColor_r())
				.set(LegionEmblemPo::getColorG, legionEmblem.getColor_g())
				.set(LegionEmblemPo::getColorB, legionEmblem.getColor_b())
				.set(LegionEmblemPo::getEmblemType, legionEmblem.getEmblemType().toString())
				.set(LegionEmblemPo::getEmblemData, legionEmblem.getCustomEmblemData())
				.where(LegionEmblemPo::getLegionId).eq(legionId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.746f2ebe921f", legionId), e);
		}
	}

	private static Legion toLegion(LegionPo po) {
		Legion legion = new Legion();
		legion.setLegionId(po.getId());
		legion.setLegionName(po.getName());
		legion.setLegionLevel(po.getLevel());
		legion.addContributionPoints(po.getContributionPoints());

		int terrId = intVal(po.getTerritory());
		LegionTerritory t = new LegionTerritory(terrId);
		if (terrId > 0) {
			t.setLegionId(legion.getLegionId());
			t.setLegionName(legion.getLegionName());
		}
		legion.setTerritory(t);

		legion.setLegionPermissions(
			po.getDeputyPermission(),
			po.getCenturionPermission(),
			po.getLegionaryPermission(),
			po.getVolunteerPermission());

		legion.setDescription(po.getDescription());
		legion.setJoinType(po.getJoinType());
		legion.setMinJoinLevel(po.getMinJoinLevel());
		legion.setDisbandTime(po.getDisbandTime());

		return legion;
	}

	private static Item constructItem(InventoryPo po, int storage) {
		return new Item(
			intVal(po.getItemUniqueId()),
			intVal(po.getItemId()),
			longVal(po.getItemCount()),
			intVal(po.getItemColor()),
			intVal(po.getColorExpires()),
			po.getItemCreator(),
			intVal(po.getExpireTime()),
			intVal(po.getActivationCount()),
			boolVal(po.getEquiped()),
			false,
			longVal(po.getSlot()),
			storage,
			intVal(po.getEnchant()),
			intVal(po.getEnchantBonus()),
			intVal(po.getItemSkin()),
			intVal(po.getFusionedItem()),
			intVal(po.getOptionalSocket()),
			intVal(po.getOptionalFusionSocket()),
			intVal(po.getCharge()),
			intVal(po.getRndBonus()),
			intVal(po.getRndCount()),
			intVal(po.getWrappableCount()),
			boolVal(po.getPacked()),
			intVal(po.getTemperingLevel()),
			boolVal(po.getTopped()),
			intVal(po.getStrengthenSkill()),
			intVal(po.getSkinSkill()),
			boolVal(po.getLunaReskin()),
			intVal(po.getReductionLevel()),
			intVal(po.getSeal()),
			boolVal(po.getIsEnhance()),
			intVal(po.getEnhanceSkillId()),
			intVal(po.getEnhanceSkillEnchant()));
	}

	/**
	 * 数值列按 Object 读取：原实现以 rs.getInt/getLong 读取（NULL 记 0），且 inventory 实体可能与
	 * 其他闭包并行校正类型，故此处对 Boolean/Number 均兼容。
	 * Number columns are read as Object: the legacy implementation used rs.getInt/getLong (NULL became 0), and the
	 * inventory entity may be re-typed by a sibling closure, so both Boolean and Number are tolerated.
	 */
	private static int intVal(Object value) {
		return value == null ? 0 : ((Number) value).intValue();
	}

	private static long longVal(Object value) {
		return value == null ? 0L : ((Number) value).longValue();
	}

	private static boolean boolVal(Object value) {
		if (value instanceof Boolean b) {
			return b;
		}
		return value instanceof Number n && n.intValue() != 0;
	}
}
