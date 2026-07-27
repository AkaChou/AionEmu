package com.aionemu.loginserver.service;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.dao.PlayerTransferDAO;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_PTRANSFER_RESPONSE;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferRequest;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferResultStatus;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferStatus;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferTask;

/**
 * 玩家跨服转移服务：轮询待处理任务，协调源/目标游戏服完成角色迁移。
 * Player cross-server transfer service: polls pending tasks and coordinates source/target game servers
 * to complete character migration.
 *
 * @author KID
 */
@Slf4j
public class PlayerTransferService {


    /**
     * 兼容旧入口的单例访问（已弃用，请走 Spring / {@link LoginTransferServices}）。
     * {@link LoginTransferServices}).
     *
     * service instance
     *
     * @return @deprecated 启动迁移后改用服务定位器 / use the service locator after boot migration
     */
    @Deprecated(since = "boot-migration")
    public static PlayerTransferService getInstance() {
        return SingletonHolder.INSTANCE;
    }
    private Map<Integer, PlayerTransferRequest> transfers = new ConcurrentHashMap<>();
    private Map<Integer, PlayerTransferTask> tasks = new ConcurrentHashMap<>();
    private Future<?> veryfyTask;
    private PlayerTransferDAO dao;

    /**
     * 构造服务：启动周期校验任务并解析 DAO。
     * Construct the service: start the periodic verify task and resolve the DAO.
     */
    public PlayerTransferService() {
        veryfyTask = LoginThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                verifyNewTasks();
            }
        }, 10000, 7 * 60000);
        this.dao = DAOManager.getDAO(PlayerTransferDAO.class);
        log.info(I18n.get("log.78b004f6b3cd"));
    }

    /**
     * 首次/周期初始化：从数据库拉取新任务并下发到源服执行。
     * First/periodic init: pull new tasks from the database and dispatch them to the source server.
     */
    protected void verifyNewTasks() {
        List<PlayerTransferTask> tasksNew = this.dao.getNew();
        if (!tasksNew.isEmpty()) {
            log.info(I18n.get("log.f885228ec5ae", tasksNew.size()));
        }
        for (PlayerTransferTask task : tasksNew) {
            GameServerInfo server = GameServerTable.getGameServerInfo(task.sourceServerId);
            if (server == null || server.getConnection() == null) {
                log.error(I18n.get("log.5d40181b390e", task.id, task.sourceServerId));
                continue;
            }

            GameServerInfo targetServer = GameServerTable.getGameServerInfo(task.targetServerId);
            if (targetServer == null || targetServer.getConnection() == null) {
                log.error(I18n.get("log.1309353dab7e", task.id, task.targetServerId));
                continue;
            }

            if (server.isAccountOnGameServer(task.sourceAccountId)) {
                log.error(I18n.get("log.2000c6c1151a", task.id, task.sourceAccountId));
                continue;
            }

            if (targetServer.isAccountOnGameServer(task.targetAccountId)) {
                log.error(I18n.get("log.a18d0a8f761f", task.id, task.targetAccountId));
                continue;
            }

            task.status = PlayerTransferTask.STATUS_ACTIVE;
            tasks.put(task.id, task);
            this.dao.update(task);
            server.getConnection().sendPacket(new SM_PTRANSFER_RESPONSE(PlayerTransferResultStatus.PERFORM_ACTION, task));
            log.info(I18n.get("log.2178ae710382", task.id));
        }
    }

    /**
     * 关闭服务并取消周期校验任务。
     * Shut down the service and cancel the periodic verify task.
     */
    public void shutdown() {
        this.veryfyTask.cancel(true);
    }

    /**
     * 源服上报角色数据后，构造转移请求并转发到目标服。
     * After the source server reports character data, build a transfer request and forward it to the target server.
     *
     * task id
     * character name
     * @param db 角色二进制数据 / character binary payload
     */
    public void requestTransfer(int taskId, String name, byte[] db) {
        PlayerTransferTask task = this.tasks.get(taskId);
        GameServerInfo targetServer = GameServerTable.getGameServerInfo(task.targetServerId);
        if (targetServer == null || targetServer.getConnection() == null) {
            log.error(I18n.get("log.5bb70795354e", task.targetServerId));
            return;
        }

        GameServerInfo server = GameServerTable.getGameServerInfo(task.sourceServerId);
        if (server == null || server.getConnection() == null) {
            log.error(I18n.get("log.5bb70795354e", task.sourceServerId));
            return;
        }

        if (targetServer.isAccountOnGameServer(task.targetAccountId)) {
            log.error(I18n.get("log.a9678828cdc7", task.targetServerId, task.targetAccountId));
            server.getConnection().sendPacket(new SM_PTRANSFER_RESPONSE(PlayerTransferResultStatus.ERROR, taskId, "transfer cant be performed while target account is online at server"));
            return;
        }

        if (transfers.containsKey(taskId)) {
            log.error(I18n.get("log.54093d6fd72a", task.targetServerId, task.targetAccountId));
            server.getConnection().sendPacket(new SM_PTRANSFER_RESPONSE(PlayerTransferResultStatus.ERROR, taskId, "transfer cant be performed while it is already active"));
            return;
        }

        Account account = AccountController.loadAccount(task.targetAccountId);
        Account saccount = AccountController.loadAccount(task.sourceAccountId);

        PlayerTransferRequest request = new PlayerTransferRequest(PlayerTransferStatus.STEP1);
        request.serverId = task.sourceServerId;
        request.targetServerId = task.targetServerId;
        request.targetAccountId = task.targetAccountId;
        request.db = db;
        request.name = name;
        request.targetAccount = account;
        request.account = account;
        request.saccount = saccount;
        request.taskId = taskId;

        byte accountActivated = account.getActivated();
        byte sourceAccountActivated = saccount.getActivated();
        account.setActivated((byte) 0);
        saccount.setActivated((byte) 0);
        if (!persist(null, account, saccount)) {
            account.setActivated(accountActivated);
            saccount.setActivated(sourceAccountActivated);
            return;
        }

        transfers.put(taskId, request);

        targetServer.getConnection().sendPacket(new SM_PTRANSFER_RESPONSE(PlayerTransferResultStatus.SEND_INFO, request));
        log.info(I18n.get("log.36b9709a671f", task.targetServerId));
    }

    /**
     * 源服拒绝执行转移时，标记任务为错误并写回数据库。
     * When the source server refuses the transfer, mark the task as error and persist it.
     *
     * task id
     * refusal reason
     */
    public void onTaskStop(int taskId, String reason) {
        PlayerTransferTask task = this.tasks.remove(taskId);
        task.status = PlayerTransferTask.STATUS_ERROR;
        task.comment = reason;
        this.dao.update(task);
    }

    /**
     * 目标服克隆失败时回滚账号激活状态并通知错误。
     * On target-server clone failure, restore account activation and report the error.
     *
     * task id
     * error reason
     */
    public void onError(int taskId, String reason) {
        PlayerTransferRequest request = this.transfers.get(taskId);
        PlayerTransferTask task = this.tasks.get(taskId);
        if (request == null || task == null) {
            return;
        }
        byte oldStatus = task.status;
        String oldComment = task.comment;
        byte accountActivated = request.account.getActivated();
        byte sourceAccountActivated = request.saccount.getActivated();
        task.status = PlayerTransferTask.STATUS_ERROR;
        task.comment = reason;
        request.account.setActivated((byte) 1);
        request.saccount.setActivated((byte) 1);
        if (!persist(task, request.account, request.saccount)) {
            task.status = oldStatus;
            task.comment = oldComment;
            request.account.setActivated(accountActivated);
            request.saccount.setActivated(sourceAccountActivated);
            return;
        }
        this.transfers.remove(taskId);
        this.tasks.remove(taskId);

        GameServerInfo targetServer = GameServerTable.getGameServerInfo(request.targetServerId);
        if (targetServer == null || targetServer.getConnection() == null) {
            log.error(I18n.get("log.5bb70795354e", request.targetServerId));
            return;
        }
        targetServer.getConnection().sendPacket(new SM_PTRANSFER_RESPONSE(PlayerTransferResultStatus.ERROR, taskId, reason));
    }

    /**
     * 目标服克隆成功后恢复账号并通知源服完成。
     * After successful clone on the target server, restore accounts and notify the source server of completion.
     *
     * task id
     * new player id
     */
    public void onOk(int taskId, int playerId) {
        PlayerTransferRequest request = this.transfers.get(taskId);
        PlayerTransferTask task = this.tasks.get(taskId);
        if (request == null || task == null) {
            return;
        }
        byte oldStatus = task.status;
        String oldComment = task.comment;
        byte accountActivated = request.account.getActivated();
        byte sourceAccountActivated = request.saccount.getActivated();
        task.status = PlayerTransferTask.STATUS_DONE;
        task.comment = "task done";
        request.account.setActivated((byte) 1);
        request.saccount.setActivated((byte) 1);
        if (!persist(task, request.account, request.saccount)) {
            task.status = oldStatus;
            task.comment = oldComment;
            request.account.setActivated(accountActivated);
            request.saccount.setActivated(sourceAccountActivated);
            return;
        }
        this.transfers.remove(taskId);
        this.tasks.remove(taskId);
        log.info(I18n.get("log.98edc435b70d", taskId));

        GameServerInfo sourceServer = GameServerTable.getGameServerInfo(request.serverId);
        if (sourceServer == null || sourceServer.getConnection() == null) {
            log.error(I18n.get("log.5bb70795354e", request.serverId));
            return;
        }
        sourceServer.getConnection().sendPacket(new SM_PTRANSFER_RESPONSE(PlayerTransferResultStatus.OK, request));
    }

	private boolean persist(PlayerTransferTask task, Account... accounts) {
		AccountDAO accountDAO = DAOManager.getDAO(AccountDAO.class);
		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);
			try {
				if (task != null) {
					this.dao.updateInTransaction(con, task);
				}
				for (Account account : accounts) {
					accountDAO.updateInTransaction(con, account);
				}
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
			return true;
		} catch (SQLException e) {
			log.error(I18n.get("log.e0628bfd2f2b", task == null ? 0 : task.id, e));
			return false;
		}
	}

    /**
     * 兼容旧单例的静态持有者。
     * Static holder for the legacy singleton.
     */
    private static final class SingletonHolder {

        private static final PlayerTransferService INSTANCE = new PlayerTransferService();
    }
}
