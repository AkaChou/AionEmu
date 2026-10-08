# -*- coding: utf-8 -*-
"""模拟 Aion HTML 解析器规则：元素内容不能同时含文本(cdata)与子元素。
两类违规：
  A. mixed: 元素内既有非空白直接文本（含子元素间 tail）又有子元素
  B. syntax: XML 语法错误（标签不配对等，ET 报错）
"""
import sys, xml.etree.ElementTree as ET
from pathlib import Path

def strip_tag(tag):
    return tag.rsplit('}', 1)[-1] if '}' in tag else tag

def check_element(elem, issues, path):
    name = strip_tag(elem.tag)
    path = f"{path}/{name}"
    direct_text = []
    if elem.text and elem.text.strip():
        direct_text.append(elem.text.strip()[:40])
    for child in elem:
        if child.tail and child.tail.strip():
            direct_text.append(child.tail.strip()[:40])
    has_child = len(elem) > 0
    if has_child and direct_text:
        issues.append(("mixed", path, direct_text))
    for child in elem:
        check_element(child, issues, path)

def scan_text(text):
    issues = []
    try:
        root = ET.fromstring(text)
    except ET.ParseError as e:
        return [("syntax", str(e), [])]
    check_element(root, issues, "")
    return issues
