#!/usr/bin/env python3
"""静态数据阶段 JFR 归因 / JFR attribution for the static-data phase.

用法 / Usage: python3 .agents/summary/startup-perf/jfr-attribute.py <exec.json> <other.json>
（先用 `jfr print --json` 导出 jdk.ExecutionSample 与其它事件，见 README/文档记录的命令。）
"""
import collections
import datetime
import json
import sys


def load(path):
	with open(path, encoding='utf-8') as handle:
		return json.load(handle)['recording']['events']


def ts(value):
	return datetime.datetime.fromisoformat(value)


def millis(value):
	# JFR JSON 的 duration 可能是 "0.00500 ms" 或 ISO-8601 时长（"PT0.000044375S"）
	# JFR JSON durations appear either as "0.00500 ms" or ISO-8601 ("PT0.000044375S")
	if isinstance(value, (int, float)):
		return float(value)
	text = str(value).strip()
	if not text:
		return 0.0
	if text.startswith('PT'):
		return float(text[2:-1]) * 1000.0
	return float(text.replace('ms', '').strip())


def thread_name(event):
	return event['values']['sampledThread'].get('javaName') or ''


def frames(event):
	return [frame['method']['type']['name'] + '.' + frame['method']['name']
		for frame in event['values'].get('stackTrace', {}).get('frames', [])]


def leaf_unit(stack):
	# 取栈中最靠近叶子的 aionemu 帧，作为"工作单元" / leaf-most aionemu frame = work unit
	for frame in stack:
		if frame.startswith('com.aionemu'):
			return frame
	return stack[0] if stack else '<empty>'


def main(argv):
	exec_events = load(argv[1])
	other_events = load(argv[2])

	static = [(ts(e['values']['startTime']), thread_name(e), e) for e in exec_events
		if thread_name(e).startswith('static-data-loader')]
	if not static:
		print('no static-data-loader samples found')
		return
	window_start = min(item[0] for item in static)
	window_end = max(item[0] for item in static)
	wall = (window_end - window_start).total_seconds()
	print(f'window: {window_start:%H:%M:%S.%f} .. {window_end:%H:%M:%S.%f}  wall={wall:.2f}s')
	print(f'static-data samples: {len(static)} (~{len(static) * 0.01:.1f} core-seconds @10ms period)')

	in_window = [(ts(e['values']['startTime']), thread_name(e), e) for e in exec_events
		if window_start <= ts(e['values']['startTime']) <= window_end]
	by_thread = collections.Counter(name for _, name, _ in in_window)
	print(f'\n# execution samples in window by thread (total {len(in_window)}):')
	for name, count in by_thread.most_common(12):
		print(f'  {count:5d}  {count * 0.01:6.2f} core-s  {name}')

	units = collections.Counter(leaf_unit(frames(e)) for _, _, e in static)
	print(f'\n# static-data work units (leaf-most com.aionemu frame):')
	for name, count in units.most_common(20):
		print(f'  {count:5d}  {count * 0.01:6.2f} core-s  {name}')

	inclusive = collections.Counter()
	for _, _, event in static:
		for frame in set(f for f in frames(event) if f.startswith('com/aionemu')):
			inclusive[frame] += 1
	print(f'\n# static-data inclusive aionemu frames (samples containing the frame, top 25):')
	for name, count in inclusive.most_common(25):
		print(f'  {count:5d}  {100.0 * count / len(static):5.1f}%  {name}')

	sampling = collections.Counter(pin for e in (x[2] for x in static) for pin in frames(e)[:1])
	print('\n# static-data leaf frames (top 10):')
	for name, count in sampling.most_common(10):
		print(f'  {count:5d}  {name}')

	print('\n# other events inside window:')
	gc_by_name = collections.Counter()
	gc_total = 0.0
	class_load = [0, 0.0]
	compilation = 0
	alloc = collections.Counter()
	other_types = collections.Counter()
	for event in other_events:
		start = ts(event['values']['startTime'])
		if not (window_start <= start <= window_end):
			continue
		kind = event['type']
		other_types[kind] += 1
		if kind == 'jdk.GCPhaseParallel':
			duration = millis(event['values']['duration'])
			gc_total += duration
			gc_by_name[event['values']['name']] += duration
		elif kind == 'jdk.ClassLoad':
			class_load[0] += 1
			class_load[1] += millis(event['values'].get('duration', 0.0))
		elif kind == 'jdk.Compilation':
			compilation += 1
		elif kind == 'jdk.ObjectAllocationSample':
			alloc[event['values']['objectClass']['name']] += event['values'].get('weight', 0.0) or 0.0
	print(f'  event counts: {dict(other_types)}')
	print(f'  GC parallel phase CPU total: {gc_total:.0f} ms')
	for name, duration in gc_by_name.most_common(8):
		print(f'      {duration:8.1f} ms  {name}')
	print(f'  class loads: {class_load[0]} events, {class_load[1]:.0f} ms total duration')
	print(f'  JIT compilations: {compilation}')
	total_alloc = sum(alloc.values())
	if total_alloc:
		print(f'  allocation samples weight total: {total_alloc / 1024 / 1024:.1f} MB')
		for name, weight in alloc.most_common(10):
			print(f'      {weight / 1024 / 1024:8.1f} MB  {name}')


if __name__ == '__main__':
	main(sys.argv)
