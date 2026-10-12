package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 legions 的持久化实体。
 * Persistence entity for table legions.
 * <p>
 * 类型校正：四个权限列的 DDL 原类型为 int，但遗留实现以 {@code rs.getShort} 读取并直接交给
 * {@code Legion.setLegionPermissions(short, short, short, short)}，故此处保留为 {@code Short}。
 * Type correction: the four permission columns are DDL {@code int}, but the legacy implementation read them via
 * {@code rs.getShort} and fed them straight into {@code Legion.setLegionPermissions(short, short, short, short)},
 * so they are kept as {@code Short} here.
 * </p>
 */
@Data
@Table("legions")
public class LegionPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer id;

	private String name;

	private Integer level;

	private Long contributionPoints;

	private Short deputyPermission;

	private Short centurionPermission;

	private Short legionaryPermission;

	private Short volunteerPermission;

	private Integer disbandTime;

	private Integer rankCp;

	private Integer rankPos;

	private Integer oldRankPos;

	private String description;

	@Column("joinType")
	private Integer joinType;

	@Column("minJoinLevel")
	private Integer minJoinLevel;

	private Integer territory;

}
