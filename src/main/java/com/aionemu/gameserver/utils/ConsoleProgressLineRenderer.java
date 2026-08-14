package com.aionemu.gameserver.utils;

import java.io.PrintStream;

/**
 * 控制台单行进度条渲染器，通过回车覆盖同一行输出进度。
 * Single-line console progress renderer that overwrites the same line via carriage return.
 */
public final class ConsoleProgressLineRenderer {

	private static final int BAR_WIDTH = 20;

	/** 输出流 / Output stream */
	private final PrintStream out;
	/** 是否启用渲染 / Whether rendering is enabled */
	private final boolean enabled;
	/** 上一行输出长度，用于补空白清除残留 / Length of the last rendered line for padding */
	private int lastLineLength;

	/**
	 * 创建进度行渲染器。
	 * Creates a progress line renderer.
	 *
	 * @param out 输出流 / Output stream
	 * @param enabled 是否启用 / Whether enabled
	 */
	public ConsoleProgressLineRenderer(PrintStream out, boolean enabled) {
		this.out = out;
		this.enabled = enabled;
	}

	/**
	 * 渲染当前进度。
	 * Renders the current progress.
	 *
	 * @param name 进度名称 / Progress name
	 * @param current 当前值 / Current value
	 * @param total 总值 / Total value
	 */
	public synchronized void progress(String name, int current, int total) {
		if (!enabled) {
			return;
		}
		render(line(name, current, total));
	}

	/**
	 * 渲染完成状态并换行。
	 * Renders the finished state and advances to the next line.
	 *
	 * @param name 进度名称 / Progress name
	 * @param total 总值 / Total value
	 */
	public synchronized void finished(String name, int total) {
		if (!enabled) {
			return;
		}
		render(line(name, total, total));
		out.println();
		out.flush();
		lastLineLength = 0;
	}

	/**
	 * 清除当前进度行（换行并重置长度）。
	 * Clears the current progress line (new line and reset length).
	 */
	public synchronized void clearLine() {
		if (!enabled) {
			return;
		}
		out.println();
		out.flush();
		lastLineLength = 0;
	}

	/**
	 * 在同一行覆盖输出进度文本。
	 * Overwrites the progress text on the same line.
	 *
	 * @param line 进度文本 / Progress text
	 */
	private void render(String line) {
		out.print('\r');
		out.print(line);
		int padding = lastLineLength - line.length();
		if (padding > 0) {
			out.print(" ".repeat(padding));
		}
		out.flush();
		lastLineLength = line.length();
	}

	/**
	 * 组装进度行文本。
	 * Builds the progress line text.
	 *
	 * @param name 进度名称 / Progress name
	 * @param current 当前值 / Current value
	 * @param total 总值 / Total value
	 * @return 进度行文本 / Progress line text
	 */
	private String line(String name, int current, int total) {
		return String.format("%s | \"%s\" | %d/%d", progressBar(current, total), name, current, total);
	}

	/**
	 * 生成固定宽度的进度条字符串。
	 * Builds a fixed-width progress bar string.
	 *
	 * @param current 当前值 / Current value
	 * @param total 总值 / Total value
	 * @return 进度条字符串 / Progress bar string
	 */
	private String progressBar(int current, int total) {
		int boundedTotal = Math.max(1, total);
		int boundedCurrent = Math.min(boundedTotal, Math.max(0, current));
		int filled = boundedCurrent * BAR_WIDTH / boundedTotal;
		return "█".repeat(filled) + "░".repeat(BAR_WIDTH - filled);
	}
}
