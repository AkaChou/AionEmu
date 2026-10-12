package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.gameserver.persistence.dao.LegionAnnouncementListDao;
import com.aionemu.gameserver.persistence.entity.LegionAnnouncementListPo;
import com.aionemu.gameserver.persistence.mapper.LegionAnnouncementListMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

/**
 * 表 legion_announcement_list 的数据访问实现。
 * Data access implementation for table legion_announcement_list.
 */
@Service
@Lazy
public class LegionAnnouncementListDaoImpl
	extends ServiceImpl<LegionAnnouncementListMapper, LegionAnnouncementListPo>
	implements LegionAnnouncementListDao {
}
