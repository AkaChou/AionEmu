package com.aionemu.gameserver.model.team.legion;

/**
 * 军团权限掩码枚举。
 * Legion Permissions Mask enumeration.
 *
 * @author MrPoke
 */
public enum LegionPermissionsMask {

	/** 编辑 / Edit. */
	EDIT(0x200),
	/** 邀请 / Invite. */
	INVITE(0x8),
	/** 踢出 / Kick. */
	KICK(0x10),
	/** 仓库取出 / Warehouse withdrawal. */
	WH_WITHDRAWAL(0x4),
	/** 仓库存入 / Warehouse deposit. */
	WH_DEPOSIT(0x1000),
	/** 遗物 / Artifact. */
	ARTIFACT(0x400),
	/** 守护石 / Guardian Stone. */
	GUARDIAN_STONE(0x800);

	private int rank;

	private LegionPermissionsMask(int rank) {
		this.rank = rank;
	}

	/** 检查权限位是否已设置。 / Whether the permission bit is set. */
	public boolean can(int permission) {
		return (rank & permission) != 0;
	}
}
