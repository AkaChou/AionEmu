package com.aionemu.gameserver.taskmanager;

import com.aionemu.gameserver.lifecycle.GameCronServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.text.ParseException;
import java.util.Date;

import org.quartz.CronExpression;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.ServerVariablesDAO;

import lombok.Getter;

/**
 * 基于 Cron 表达式的服务器周期任务抽象基类。
 * Abstract base for server periodic tasks driven by a Cron expression.
 *
 * <p>从服务器变量加载上次运行时间，按 Cron 调度下次执行，并在运行后回写下次时间。
 * Loads last run time from server variables, schedules the next run by Cron, and stores the next run time after execution.</p>
 */
public abstract class AbstractCronTask implements Runnable {

	/**
	 * Cron 表达式字符串。
	 * Cron expression string.
	 */
	@Getter
	private String cronExpressionString;

	/**
	 * 解析后的 Cron 表达式。
	 * Parsed Cron expression.
	 */
	private CronExpression runExpression;

	/**
	 * 下次运行时间（Unix 秒）。
	 * Next run time (Unix seconds).
	 */
	@Getter
	private int runTime;

	/**
	 * 两次触发之间的周期（毫秒）。
	 * Period between two triggers in milliseconds.
	 */
	@Getter
	private long period;

	/**
	 * 获取距离应运行时刻的延迟（毫秒）；0 表示应立即处理。
	 * Delay until the task should run (ms); 0 means it should be handled now.
	 *
	 * @return 延迟毫秒数 / Delay in milliseconds
	 */
	abstract protected long getRunDelay();

	/**
	 * 构造早期钩子（解析 Cron 前）。
	 * Early construction hook (before Cron parse).
	 */
	protected void preInit() {
	}

	/**
	 * 构造晚期钩子（解析 Cron 后）。
	 * Late construction hook (after Cron parse).
	 */
	protected void postInit() {
	}

	/**
	 * 服务器变量键名，用于持久化下次运行时间。
	 * Server-variable key used to persist the next run time.
	 *
	 * @return 服务器变量键名 / the variable name
	 */
	abstract protected String getServerTimeVariable();

	/**
	 * 任务主体执行前钩子。
	 * Hook before the main task body.
	 */
	protected void preRun() {
	}

	/**
	 * 执行实际任务逻辑。
	 * Execute the actual task logic.
	 */
	abstract protected void executeTask();

	/**
	 * 若构造时已到点，是否允许立即执行一次。
	 * Whether an immediate run is allowed when already due at construction.
	 *
	 * @return 若 allowed 则为 true / true if allowed
	 */
	abstract protected boolean canRunOnInit();

	/**
	 * 任务主体执行后钩子。
	 * Hook after the main task body.
	 */
	protected void postRun() {
	}

	/**
	 * 使用 Cron 表达式初始化任务并安排下次调度。
	 * Initialize with a Cron expression and schedule the next run.
	 *
	 * @param cronExpression Cron 表达式 / the Cron expression
	 * @throws ParseException 表达式无效时 / when the expression is invalid
	 * @throws NullPointerException 表达式为 null 时 / when the expression is null
	 */
	public AbstractCronTask(String cronExpression) throws ParseException {
		if (cronExpression == null)
			throw new NullPointerException("cronExpressionString");
		cronExpressionString = cronExpression;
		ServerVariablesDAO dao = DAOManager.getDAO(ServerVariablesDAO.class);
		runTime = dao.load(getServerTimeVariable());
		preInit();
		runExpression = new CronExpression(cronExpressionString);
		Date nextDate = runExpression.getTimeAfter(new Date());
		Date nextAfterDate = runExpression.getTimeAfter(nextDate);
		period = nextAfterDate.getTime() - nextDate.getTime();
		postInit();
		if (getRunDelay() == 0) {
			if (canRunOnInit()) {
				GameThreadPoolServices.threadPoolManager().schedule(this, 0);
			} else {
				saveNextRunTime();
			}
		}
		scheduleNextRun();
	}

	/**
	 * 通过 Cron 服务安排下次触发。
	 * Schedule the next trigger via the Cron service.
	 */
	private void scheduleNextRun() {
		GameCronServices.cronService().schedule(this, cronExpressionString, true);
	}

	/**
	 * 计算并持久化下次运行时间到服务器变量。
	 * Compute and persist the next run time into server variables.
	 */
	private void saveNextRunTime() {
		Date nextDate = runExpression.getTimeAfter(new Date());
		ServerVariablesDAO dao = DAOManager.getDAO(ServerVariablesDAO.class);
		runTime = (int) (nextDate.getTime() / 1000);
		dao.store(getServerTimeVariable(), runTime);
	}

	/**
	 * 若仍有延迟则再调度；否则依次执行 preRun → 任务 → 保存 → postRun。
	 * If still delayed, re-schedules; otherwise runs preRun → task → save → postRun.
	 */
	@Override
	public final void run() {
		if (getRunDelay() > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(this, getRunDelay());
		} else {
			preRun();
			executeTask();
			saveNextRunTime();
			postRun();
		}
	}
}
