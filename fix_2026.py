import json

file_path = r'c:\projects\408真题训练系统\src\main\resources\static\choice\2026.json'

with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

for q in data:
    if q["题号"] == 40:
        q["选项"] = [
            "A. 用户跟踪",
            "B. 个性化推荐",
            "C. 构建虚拟购物车",
            "D. 缩短web对象的响应时间"
        ]
        break

with open(file_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("已修复第40题的选项")
