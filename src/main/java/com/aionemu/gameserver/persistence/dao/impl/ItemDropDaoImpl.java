package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.gameserver.persistence.dao.ItemDropDao;
import com.aionemu.gameserver.persistence.entity.ItemDropPo;
import com.aionemu.gameserver.persistence.mapper.ItemDropMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

/**
 * 表 item_drop 的数据访问实现。
 * Data access implementation for table item_drop.
 */
@Service
@Lazy
public class ItemDropDaoImpl
	extends ServiceImpl<ItemDropMapper, ItemDropPo>
	implements ItemDropDao {
}
