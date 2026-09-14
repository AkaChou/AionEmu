import re
import os

def replace_in_file(path, pattern, replacement):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = re.sub(pattern, replacement, content)
    if new_content != content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

print("Batch repair helper loaded")
