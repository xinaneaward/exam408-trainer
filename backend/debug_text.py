"""调试：查看2021年真题文本格式"""
import fitz

doc = fitz.open(r"c:\projects\408真题训练系统\2009-2025计算机408统考真题\2021年计算机408统考真题.pdf")

for page_num in range(min(3, doc.page_count)):
    page = doc[page_num]
    text = page.get_text("text")
    print(f"\n{'='*60}")
    print(f"第 {page_num+1} 页 原始文本:")
    print(repr(text[:2000]))
    
    # 查找关键模式
    if '一、' in text:
        print("  ✓ 找到 '一、'")
    if '单项' in text:
        print("  ✓ 找到 '单项'")
    if '二、' in text:
        print("  ✓ 找到 '二、'")
    
    # 找题号模式
    import re
    nums = re.findall(r'(?:^|\n)\s*(\d{1,2})\s*[.．]', text)
    if nums:
        print(f"  ✓ 找到题号: {nums[:10]}")

doc.close()
