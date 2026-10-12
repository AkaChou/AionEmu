package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.gameserver.persistence.dao.BookmarkDao;
import com.aionemu.gameserver.persistence.entity.BookmarkPo;
import com.aionemu.gameserver.persistence.mapper.BookmarkMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

/**
 * 表 bookmark 的数据访问实现。
 * Data access implementation for table bookmark.
 */
@Service
@Lazy
public class BookmarkDaoImpl
	extends ServiceImpl<BookmarkMapper, BookmarkPo>
	implements BookmarkDao {
}
