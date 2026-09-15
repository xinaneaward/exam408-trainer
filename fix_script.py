import re, sys

with open("C:/projects/408真题训练系统/backend/extract_all.py", "r", encoding="utf-8") as f:
    content = f.read()

# Fix 1: swap doc.close() and page_count print
content = content.replace(
    'doc.close()\n    \n    print(f"  Pages: {doc.page_count}',
    'print(f"  Pages: {doc.page_count}'
)

# Remove orphaned doc.close()
lines = content.split("\n")
result = []
for line in lines:
    stripped = line.strip()
    if stripped == "doc.close()":
        continue
    result.append(line)
content = "\n".join(result)

with open("C:/projects/408真题训练系统/backend/extract_all.py", "w", encoding="utf-8") as f:
    f.write(content)
print("Fixed")
