package com.aionemu.gameserver.model.team.legion;

/**
 * 军团 Permissions 掩码枚举。
 * Legion Permissions Mask enumeration.
 *
 * @author MrPoke
 */
public enum LegionPermissionsMask {

	/** 编辑 / Edit. */
	EDIT(0x200), INVITE(0x8), KICK(0x10), WH_WITHDRAWAL(0x4), WH_DEPOSIT(0x1000), ARTIFACT(0x400),
	/** Guardian Stone / Guardian Stone */
	GUARDIAN_STONE(0x800);

	private int rank;

	private LegionPermissionsMask(int rank) {
		this.rank = rank;
	}

	/** 是否可以。 / Whether . */
	public boolean can(int permission) {
		return (rank & permission) != 0;
	}
}
