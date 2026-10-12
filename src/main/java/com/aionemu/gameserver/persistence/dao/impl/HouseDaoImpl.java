package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.templates.housing.Building;
import com.aionemu.gameserver.model.templates.housing.BuildingType;
import com.aionemu.gameserver.model.templates.housing.HouseAddress;
import com.aionemu.gameserver.model.templates.housing.HousingLand;
import com.aionemu.gameserver.persistence.dao.HouseDao;
import com.aionemu.gameserver.persistence.entity.HousePo;
import com.aionemu.gameserver.persistence.mapper.HouseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 房屋数据访问的 MyBatis-Flex 实现。
 * MyBatis-Flex implementation of the houses data access layer.
 */
@Slf4j
@Service
@Lazy
public class HouseDaoImpl extends ServiceImpl<HouseMapper, HousePo> implements HouseDao {

	@Override
	public void storeHouse(House house) {
		if (house.getPersistentState() == PersistentState.NEW) {
			insertNewHouse(house);
		} else {
			updateHouse(house);
		}
		house.setPersistentState(PersistentState.UPDATED);
	}

	@Override
	public Map<Integer, House> loadHouses(Collection<HousingLand> lands, boolean studios) {
		Map<Integer, House> houses = new HashMap<>();
		Map<Integer, HouseAddress> addressesById = new HashMap<>();
		Map<Integer, List<Building>> buildingsForAddress = new HashMap<>();

		for (HousingLand land : lands) {
			for (HouseAddress address : land.getAddresses()) {
				addressesById.put(address.getId(), address);
				buildingsForAddress.put(address.getId(), land.getBuildings());
			}
		}

		Map<Integer, Integer> addressHouseIds = new HashMap<>();

		try {
			mapper.selectListByQuery(studios
				? QueryWrapper.create().where(HousePo::getAddress).eq(2001).or(HousePo::getAddress).eq(3001)
				: QueryWrapper.create().where(HousePo::getAddress).ne(2001).and(HousePo::getAddress).ne(3001))
				.forEach(po -> {
					int houseId = po.getId();
					int buildingId = po.getBuildingId();
					int addressId = po.getAddress();

					HouseAddress address = addressesById.get(addressId);
					if (address == null) {
						log.warn(I18n.get("log.32a2bfc0a656", addressId));
						return;
					}

					List<Building> buildings = buildingsForAddress.get(address.getId());
					Building building = null;
					if (buildings != null) {
						for (Building b : buildings) {
							if (b.getId() == buildingId) {
								building = b;
								break;
							}
						}
					}

					if (building == null) {
						log.warn(I18n.get("log.89b55a9b2477", buildingId, addressId));
						return;
					}

					if (addressHouseIds.containsKey(address.getId())) {
						log.warn(I18n.get("log.28a74968b3a8", address.getId()));
						return;
					}

					House house = new House(houseId, building, address);
					if (building.getType() == BuildingType.PERSONAL_FIELD) {
						addressHouseIds.put(address.getId(), houseId);
					}

					house.setOwnerId(po.getPlayerId());
					house.setAcquiredTime(po.getAcquireTime());
					house.setPermissions(po.getSettings());

					String statusStr = po.getStatus();
					try {
						house.setStatus(HouseStatus.valueOf(statusStr));
					} catch (IllegalArgumentException e) {
						log.warn(I18n.get("log.9d052d863885", statusStr, houseId));
						house.setStatus(HouseStatus.INACTIVE);
					}

					house.setFeePaid(po.getFeePaid() != 0);
					house.setNextPay(po.getNextPay());
					house.setSellStarted(po.getSellStarted());

					byte[] signNotice = po.getSignNotice();
					if (signNotice != null) {
						house.setSignNotice(signNotice);
					}

					int id = studios ? house.getOwnerId() : address.getId();
					houses.put(id, house);
				});
		} catch (Exception e) {
			log.error(I18n.get("log.423d09af3ba2"), e);
		}

		return houses;
	}

	@Override
	public void deleteHouse(int playerId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create().where(HousePo::getPlayerId).eq(playerId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f2facfdc760c", playerId), e);
		}
	}

	@Override
	public int[] getUsedIDs() {
		try {
			List<HousePo> houses = mapper.selectListByQuery(QueryWrapper.create());
			int[] result = new int[houses.size()];
			for (int i = 0; i < houses.size(); i++) {
				result[i] = houses.get(i).getId();
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.977a91e82734"), e);
			return new int[0];
		}
	}

	private void insertNewHouse(House house) {
		try {
			Timestamp acquireTime = house.getAcquiredTime() == null
				? new Timestamp(System.currentTimeMillis())
				: house.getAcquiredTime();
			byte[] signNotice = house.getSignNotice();

			HousePo po = new HousePo();
			po.setId(house.getObjectId());
			po.setAddress(house.getAddress().getId());
			po.setBuildingId(house.getBuilding().getId());
			po.setPlayerId(house.getOwnerId());
			po.setAcquireTime(acquireTime);
			po.setSettings(house.getPermissions());
			po.setStatus(house.getStatus().toString());
			po.setFeePaid(house.isFeePaid() ? 1 : 0);
			po.setNextPay(house.getNextPay());
			po.setSellStarted(house.getSellStarted());
			po.setSignNotice(signNotice != null && signNotice.length > 0 ? signNotice : null);
			mapper.insert(po);
		} catch (Exception e) {
			log.error(I18n.get("log.4abe7981971b", house.getObjectId()), e);
		}
	}

	private void updateHouse(House house) {
		try {
			byte[] signNotice = house.getSignNotice();

			UpdateChain.of(HousePo.class)
				.set(HousePo::getBuildingId, house.getBuilding().getId())
				.set(HousePo::getPlayerId, house.getOwnerId())
				.set(HousePo::getAcquireTime, house.getAcquiredTime())
				.set(HousePo::getSettings, house.getPermissions())
				.set(HousePo::getStatus, house.getStatus().toString())
				.set(HousePo::getFeePaid, house.isFeePaid() ? 1 : 0)
				.set(HousePo::getNextPay, house.getNextPay())
				.set(HousePo::getSellStarted, house.getSellStarted())
				.set(HousePo::getSignNotice, signNotice != null && signNotice.length > 0 ? signNotice : null)
				.where(HousePo::getId).eq(house.getObjectId())
				.update();
		} catch (Exception e) {
			log.error(I18n.get("log.340314d885f4", house.getObjectId()), e);
		}
	}
}
