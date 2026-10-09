package com.aionemu.gameserver.controllers;


import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;

/**
 * 小跟班（Minion）控制器：移动完全由客户端本地模拟，服务端零参与（真端架构）。
 * Minion controller: movement is fully client-simulated, the server takes no part (retail architecture).
 * <p>真端证据：5.8 真端服务端 {@code Familiar}/{@code FamiliarMapMgr}（含 4311 行地图管理器）中
 * move/follow/position/speed 关键词零命中，方法全集只有数据管理与召唤/收回状态帧，
 * 协议层也没有 minion 移动通道——零售的 minion 同速跟随由每个客户端基于主人移动流本地模拟，
 * 因此不存在「追不上→掉队→瞬拉」。本仓宠物（Pet）即同一待遇（PetController 无任何跟随调度）且实机跟随正常。</p>
 * <p>Retail evidence: the 5.8 retail server's {@code Familiar}/{@code FamiliarMapMgr} (including the
 * 4311-line map manager) contains no move/follow/position/speed logic at all — its method set is data
 * management plus summon/abandon state frames, and the protocol has no minion-movement channel. Retail
 * minions follow by per-client simulation over the master's movement stream, so the "fall behind →
 * blink-teleport" loop cannot exist. Our pets already run the same way (no follow scheduling in
 * PetController) and follow correctly.</p>
 * <p>服务端只在这些事件触碰 minion：召唤/收回（SM_MINIONS 5/6）、跨图与飞行传送的位置重置（CL-001 的
 * suspend/restore）、死亡与登出的完整收回。禁止重新引入服务端跟随 tick / SM_MOVE 移动包 / 距离瞬拉
 * （门禁 {@code MinionKnownListTest#minionMovementStaysClientAuthored} 守护）。</p>
 * <p>The server touches the minion only on lifecycle events: summon/release (SM_MINIONS 5/6),
 * cross-map and fly-teleport repositioning (CL-001 suspend/restore), and full release on death/logout.
 * Never reintroduce server-side follow ticks, SM_MOVE segments or distance teleports — the
 * {@code MinionKnownListTest#minionMovementStaysClientAuthored} gate enforces this.</p>
 */
public class MinionController extends VisibleObjectController<Minion> {

    /**
     * 小跟班看到其他对象时的回调（当前无逻辑）。
     * Callback when the minion sees another object (currently no-op).
     * @param object 进入视野的对象 / the object entering sight
     */
    @Override
    public void see(VisibleObject object) {

    }

    /**
     * 小跟班不再看到其他对象时的回调（当前无逻辑）。
     * Callback when the minion no longer sees another object (currently no-op).
     * @param object 离开视野的对象 / the object leaving sight
     * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
     */
    @Override
    public void notSee(VisibleObject object, boolean isOutOfRange) {

    }
}
