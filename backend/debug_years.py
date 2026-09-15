"""调试失败年份的文本格式"""
import fitz, re

for year in [2009, 2010, 2013, 2015]:
    path = rf"c:\projects\408真题训练系统\2009-2025计算机408统考真题\{year}年计算机408统考真题.pdf"
    doc = fitz.open(path)
    
    # 只看第一页
    text = doc[0].get_text("text")
    print(f"\n===== {year} 年 第1页 =====")
    print(text[:1500])
    print(f"\n--- 查找题号模式 ---")
    
    # 尝试各种匹配
    for pat_name, pat in [
        ("1-2 digit + .", r'(\d{1,2})\s*\.\s*(?=[\u4e00-\u9fff])'),
        ("1-2 digit + ．", r'(\d{1,2})\s*．\s*(?=[\u4e00-\u9fff])'),
        ("0+digit + .", r'0\s*(\d)\s*\.\s*(?=[\u4e00-\u9fff])'),
        ("space-digit + .", r'(\d\s+\d)\s*\.'),
    ]:
        matches = re.findall(pat, text)
        if matches:
            print(f"  {pat_name}: {matches[:10]}")
    
    doc.close()
