package com.aionemu.gameserver.utils;

import java.io.PrintStream;

public final class ConsoleProgressLineRenderer {

	private static final int BAR_WIDTH = 20;

	private final PrintStream out;
	private final boolean enabled;
	private int lastLineLength;

	public ConsoleProgressLineRenderer(PrintStream out, boolean enabled) {
		this.out = out;
		this.enabled = enabled;
	}

	public synchronized void progress(String name, int current, int total) {
		if (!enabled) {
			return;
		}
		render(line(name, current, total));
	}

	public synchronized void finished(String name, int total) {
		if (!enabled) {
			return;
		}
		render(line(name, total, total));
		out.println();
		out.flush();
		lastLineLength = 0;
	}

	public synchronized void clearLine() {
		if (!enabled) {
			return;
		}
		out.println();
		out.flush();
		lastLineLength = 0;
	}

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

	private String line(String name, int current, int total) {
		return String.format("%s | \"%s\" | %d/%d", progressBar(current, total), name, current, total);
	}

	private String progressBar(int current, int total) {
		int boundedTotal = Math.max(1, total);
		int boundedCurrent = Math.min(boundedTotal, Math.max(0, current));
		int filled = boundedCurrent * BAR_WIDTH / boundedTotal;
		return "█".repeat(filled) + "░".repeat(BAR_WIDTH - filled);
	}
}
