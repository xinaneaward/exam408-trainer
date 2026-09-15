# -*- coding: utf-8 -*-
"""
408真题完整提取脚本 v3
- 提取所有年份(2009-2025)的题目、答案、解析
- 从PDF页面提取图片并关联到对应题目
- 生成完整的JSON和SQL数据
"""
import sys
import os
import re
import json

# Fix console encoding
if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

import fitz

BASE_DIR = r"C:\projects\408真题训练系统"
BACKEND_DIR = os.path.join(BASE_DIR, "backend")
EXAM_DIR = os.path.join(BACKEND_DIR, "2009-2025计算机408统考真题")
ANALYSIS_DIR = os.path.join(BACKEND_DIR, "2009-2025计算机408真题解析")
PAGE_IMG_DIR = os.path.join(BACKEND_DIR, "src", "main", "resources", "static", "images", "pages")
Q_IMG_DIR = os.path.join(BACKEND_DIR, "src", "main", "resources", "static", "images", "questions")
OUTPUT_JSON = os.path.join(BACKEND_DIR, "extracted_questions.json")
OUTPUT_SQL = os.path.join(BACKEND_DIR, "src", "main", "resources", "db", "data.sql")

os.makedirs(PAGE_IMG_DIR, exist_ok=True)
os.makedirs(Q_IMG_DIR, exist_ok=True)

# Subject mapping by question number
def get_subject(q_num):
    if 1 <= q_num <= 11: return "\u6570\u636e\u7ed3\u6784"  # 数据结构
    if 12 <= q_num <= 22: return "\u8ba1\u7b97\u673a\u7ec4\u6210\u539f\u7406"  # 计组
    if 23 <= q_num <= 32: return "\u64cd\u4f5c\u7cfb\u7edf"  # 操作系统
    if 33 <= q_num <= 40: return "\u8ba1\u7b97\u673a\u7f51\u7edc"  # 计网
    if 41 <= q_num <= 43: return "\u6570\u636e\u7ed3\u6784"
    if 44 <= q_num <= 45: return "\u8ba1\u7b97\u673a\u7ec4\u6210\u539f\u7406"
    if q_num == 46: return "\u64cd\u4f5c\u7cfb\u7edf"
    if q_num == 47: return "\u8ba1\u7b97\u673a\u7f51\u7edc"
    return "\u6570\u636e\u7ed3\u6784"

def get_type(q_num):
    return "\u5355\u9009" if q_num <= 40 else "\u7efc\u5408\u5e94\u7528"  # 单选 / 综合应用

def estimate_page(q_num):
    if q_num <= 8: return 1
    if q_num <= 16: return 2
    if q_num <= 24: return 3
    if q_num <= 32: return 4
    if q_num <= 40: return 5
    if q_num <= 43: return 6
    if q_num <= 47: return 7
    return 1

def extract_images_from_page(doc, pageno, year, source_type):
    """Extract images from a specific PDF page"""
    page = doc[pageno]
    images = page.get_images(full=True)
    img_refs = []
    for img_idx, img in enumerate(images):
        xref = img[0]
        try:
            base = doc.extract_image(xref)
            if base and len(base["image"]) > 2000:
                ext = base["ext"]
                name = f"{year}_{source_type}_p{pageno+1}_i{img_idx+1}.{ext}"
                path = os.path.join(Q_IMG_DIR, name)
                with open(path, 'wb') as f:
                    f.write(base["image"])
                img_refs.append(f"/images/questions/{name}")
        except:
            pass
    return img_refs

def extract_questions_from_text(text_pages):
    """Extract question numbers found on each page"""
    # text_pages: list of (page_num, page_text)
    # Returns: dict mapping page_num -> [question_numbers]
    result = {}
    qnum_pattern = re.compile(r'(?:^|\n)\s*(\d{1,2})\s*[.．]\s*(?=[\u4e00-\u9fffA-Za-z(（])', re.MULTILINE)
    
    for pageno, text in text_pages:
        qnums = set()
        for m in qnum_pattern.finditer(text):
            num = int(m.group(1))
            if 1 <= num <= 47:
                qnums.add(num)
        if qnums:
            result[pageno] = sorted(qnums)
    return result

def extract_qnum_from_pageno(pageno, year):
    """Estimate which questions are on a given page (fallback)"""
    # 408 exam: ~7 pages
    # Page 1: Q1-8, Page 2: Q9-16, etc.
    base = (pageno - 1) * 8
    return list(range(base + 1, min(base + 9, 48)))

def extract_answers_from_analysis(doc, year):
    """Extract answers and analyses from analysis PDF"""
    text = ""
    for pg in range(doc.page_count):
        try:
            text += doc[pg].get_text("text") + "\n"
        except:
            pass
    
    answers = {}
    analyses = {}
    
    # Pattern 1: "1. 【答案】A" or "1. 【解析】..."
    ans_pat = re.compile(r'(\d{1,2})\s*[.．]\s*【\u7b54\u6848】\s*([A-D\s]+?)(?=\n|$)', re.MULTILINE)
    ana_pat = re.compile(r'(\d{1,2})\s*[.．]\s*【\u89e3\u6790】\s*([\s\S]*?)(?=\n\s*\d{1,2}\s*[.．]\s*【|$)', re.MULTILINE)
    
    for m in ans_pat.finditer(text):
        num = int(m.group(1))
        if num <= 47:
            answers[num] = m.group(2).strip()
    
    for m in ana_pat.finditer(text):
        num = int(m.group(1))
        if num <= 47:
            analyses[num] = m.group(2).strip()
    
    # Pattern 2: "1-5 CDABD" style
    if not answers:
        simple_pat = re.compile(r'(\d{1,2})\s*[-~]\s*(\d{1,2})\s*([A-D]{2,})', re.MULTILINE)
        for m in simple_pat.finditer(text):
            start = int(m.group(1))
            end = int(m.group(2))
            ans_str = m.group(3)
            for i, ch in enumerate(ans_str):
                if start + i <= end:
                    answers[start + i] = ch
    
    return answers, analyses

def get_pdf_files():
    """Get all exam and analysis PDF files by year"""
    files = {}
    for fname in sorted(os.listdir(EXAM_DIR)):
        m = re.search(r'(\d{4})\u5e74', fname)
        if m and fname.endswith('.pdf') and '\u89e3\u6790' not in fname and '\u7b54\u6848' not in fname:
            year = int(m.group(1))
            files.setdefault(year, {})['exam'] = os.path.join(EXAM_DIR, fname)
    
    for fname in sorted(os.listdir(ANALYSIS_DIR)):
        m = re.search(r'(\d{4})\u5e74', fname)
        if m and fname.endswith('.pdf'):
            year = int(m.group(1))
            files.setdefault(year, {})['analysis'] = os.path.join(ANALYSIS_DIR, fname)
    
    return files

def process_year(year, files):
    """Process a single year: extract questions, images, answers"""
    print(f"\n=== Processing {year} ===")
    questions = []
    
    exam_path = files.get('exam')
    analysis_path = files.get('analysis')
    
    if not exam_path or not os.path.exists(exam_path):
        print(f"  No exam PDF for {year}")
        return None
    
    # Open exam PDF
    try:
        doc = fitz.open(exam_path)
    except Exception as e:
        print(f"  Cannot open exam PDF: {e}")
        return None
    
    # Extract page-by-page text and images
    page_questions = {}  # page_num -> [question_numbers]
    page_images = {}     # page_num -> [image_urls]
    page_texts = []
    
    for pg in range(doc.page_count):
        try:
            text = doc[pg].get_text("text")
        except:
            text = ""
        page_texts.append((pg, text))
        
        # Extract images from this page
        imgs = extract_images_from_page(doc, pg, year, "exam")
        if imgs:
            page_images[pg] = imgs
        
        # Find question numbers on this page
        qnums = set()
        qnum_pattern = re.compile(r'(?:^|\n)\s*(\d{1,2})\s*[.．]\s*(?=[\u4e00-\u9fffA-Za-z(（])', re.MULTILINE)
        for m in qnum_pattern.finditer(text):
            num = int(m.group(1))
            if 1 <= num <= 47:
                qnums.add(num)
        
        if qnums:
            page_questions[pg] = sorted(qnums)
    
    print(f"  Pages: {doc.page_count}, Questions found on pages: {page_questions}")
    
    # Create all 47 questions with image links
    for q_num in range(1, 48):
        # Find which page this question is on
        q_page = None
        for pg, qns in sorted(page_questions.items()):
            if q_num in qns:
                q_page = pg
                break
        
        # If not found, estimate
        if q_page is None:
            q_page = estimate_page(q_num) - 1  # convert to 0-indexed
        
        # Get images from the same page
        q_images = page_images.get(q_page, [])
        
        subject = get_subject(q_num)
        qtype = get_type(q_num)
        
        page = q_page + 1  # 1-indexed
        page_img = f"/images/pages/exam_{year}_p{page:02d}.png"
        
        # Build content with image references
        content = f"({year}\u5e74\u7b2c{q_num}\u9898) "
        
        # Add question-specific images
        img_markers = ""
        if q_images:
            for img_url in q_images:
                img_markers += f"\n<!-- page_img:{img_url} -->"
        
        # Always add page image reference
        if not q_images:
            img_markers += f"\n<!-- page_img:{page_img} -->"
        
        questions.append({
            "year": year,
            "question_number": q_num,
            "subject": subject,
            "type": qtype,
            "content": content,
            "options": None,
            "answer": "",
            "analysis": "",
            "knowledge_tag": "",
            "page_images": q_images,
            "page_img": page_img
        })
    
    # Extract answers from analysis PDF
    if analysis_path and os.path.exists(analysis_path):
        try:
            a_doc = fitz.open(analysis_path)
            answers, analyses = extract_answers_from_analysis(a_doc, year)
            a_doc.close()
            
            for q in questions:
                num = q["question_number"]
                if num in answers:
                    q["answer"] = answers[num]
                if num in analyses:
                    q["analysis"] = analyses[num]
            
            ans_count = sum(1 for q in questions if q["answer"])
            ana_count = sum(1 for q in questions if q["analysis"])
            print(f"  Answers: {ans_count}/47, Analyses: {ana_count}/47")
        except Exception as e:
            print(f"  Analysis extraction error: {e}")
    else:
        print(f"  No analysis PDF for {year}")
    
    return questions

def escape_sql(s):
    if s is None: return "NULL"
    return "'" + str(s).replace("'", "''").replace("\\", "\\\\") + "'"

def main():
    all_years = list(range(2009, 2026))
    files = get_pdf_files()
    
    all_questions = []
    
    for year in all_years:
        if year in files:
            qs = process_year(year, files[year])
            if qs:
                all_questions.append(qs)
        else:
            print(f"\n=== {year} - No PDF files found ===")
    
    # Save JSON
    with open(OUTPUT_JSON, 'w', encoding='utf-8') as f:
        json.dump(all_questions, f, ensure_ascii=False, indent=2)
    
    total = sum(len(qs) for qs in all_questions)
    has_ans = sum(1 for qs in all_questions for q in qs if q["answer"])
    has_ana = sum(1 for qs in all_questions for q in qs if q["analysis"])
    has_img = sum(1 for qs in all_questions for q in qs if q.get("page_images"))
    
    print(f"\n{'='*60}")
    print(f"Complete! {len(all_questions)} years, {total} questions")
    print(f"  With answers: {has_ans}")
    print(f"  With analysis: {has_ana}")
    print(f"  With images: {has_img}")
    print(f"  JSON: {OUTPUT_JSON}")
    
    # Generate SQL
    sql_lines = [
        "-- 408\u771f\u9898\u8bad\u7ec3\u7cfb\u7edf \u9898\u76ee\u6570\u636e",
        f"-- \u5171 {total} \u9053\u9898\u76ee, {len(all_questions)} \u5e74",
        ""
    ]
    
    for year_qs in all_questions:
        if not year_qs: continue
        year = year_qs[0]["year"]
        sql_lines.append(f"\n-- ========== {year}\u5e74 ({len(year_qs)}\u9898) ==========")
        
        for q in year_qs:
            content = q["content"]
            answer = q.get("answer", "")
            analysis = q.get("analysis", "")
            
            # Add page image reference
            page_img = q.get("page_img", "")
            if page_img:
                content += f"\n<!-- page_img:{page_img} -->"
            
            options_json = "NULL"
            if q.get("options"):
                options_json = escape_sql(json.dumps(q["options"], ensure_ascii=False))
            
            sql_lines.append(
                f"INSERT INTO question (year, subject, type, question_number, content, options, answer, analysis, knowledge_tag) "
                f"SELECT {year}, {escape_sql(q['subject'])}, {escape_sql(q['type'])}, {q['question_number']}, "
                f"{escape_sql(content)}, {options_json}, {escape_sql(answer)}, {escape_sql(analysis)}, '' "
                f"WHERE NOT EXISTS (SELECT 1 FROM question WHERE year={year} AND question_number={q['question_number']});"
            )
    
    with open(OUTPUT_SQL, 'w', encoding='utf-8') as f:
        f.write("\n".join(sql_lines))
    
    print(f"  SQL: {OUTPUT_SQL}")

if __name__ == "__main__":
    main()
