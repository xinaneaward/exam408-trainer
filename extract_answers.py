"""提取2009-2025年408真题解析PDF中的答案和解析"""
import fitz  # PyMuPDF
import json
import re
import os

PDF_DIR = r"c:\projects\408真题训练系统\2009-2025计算机408真题解析"
OUTPUT = "answer_data.json"

results = {}

for year in range(2009, 2026):
    fname = f"{year}年计算机408统考真题解析.pdf"
    if year == 2025:
        fname = "2025年计算机408统考真题答案.pdf"
    fpath = os.path.join(PDF_DIR, fname)
    if not os.path.exists(fpath):
        print(f"  {year}: 文件不存在")
        continue
    
    doc = fitz.open(fpath)
    full_text = ""
    for page in doc:
        full_text += page.get_text()
    doc.close()
    
    year_answers = {}
    # 提取单选答案 (题号1-40)
    # 常见格式: "1. A" 或 "1．A" 或 "1、A"
    for qnum in range(1, 41):
        patterns = [
            rf"{qnum}[\.\．\、\s]+([A-D])",
            rf"第{qnum}题[^A-D]*?([A-D])",
            rf"{qnum:02d}[\.\．\、\s]+([A-D])",
        ]
        ans = None
        for pat in patterns:
            m = re.search(pat, full_text)
            if m:
                ans = m.group(1)
                break
        
        if ans:
            # 提取该题解析
            # 尝试找到从"解析"或"答案"开始到下一题之间的内容
            analysis = ""
            ans_pos = m.start()
            # 向后找200字符作为解析
            analysis = full_text[ans_pos:ans_pos+300].strip()
            analysis = re.sub(r'[\t\n\r]+', ' ', analysis)[:250]
            
            year_answers[str(qnum)] = {"answer": ans, "analysis": analysis}
    
    # 提取综合应用题答案 (题号41-47)
    for qnum in range(41, 48):
        patterns = [
            rf"{qnum}[\.\．]{1,2}\s*([\s\S]{0,300}?)(?=\n\s*(?:4[2-7]|[1-3]\d|[A-D])[\.\．]|\Z)",
            rf"第{qnum}题[\s\S]{0,400}?(?=\n\s*第4[2-7]题|\Z)",
        ]
        analysis = ""
        for pat in patterns:
            m = re.search(pat, full_text)
            if m:
                analysis = m.group(0).strip()[:400]
                analysis = re.sub(r'[\t\r]+', ' ', analysis).replace('\n', ' ')
                break
        
        if analysis:
            year_answers[str(qnum)] = {"answer": "（综合应用题—见解析）", "analysis": analysis}
    
    results[str(year)] = year_answers
    print(f"  {year}: 提取到 {len(year_answers)} 题答案")

with open(OUTPUT, 'w', encoding='utf-8') as f:
    json.dump(results, f, ensure_ascii=False, indent=2)

total = sum(len(v) for v in results.values())
print(f"\n总计提取 {total} 题答案 → {OUTPUT}")
