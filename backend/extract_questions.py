"""
408真题PDF提取工具 v2 - 改进版
处理多年份不同格式的PDF
"""
import fitz
import os
import re
import json

BASE_DIR = r"c:\projects\408真题训练系统"
EXAM_DIR = os.path.join(BASE_DIR, "2009-2025计算机408统考真题")
ANALYSIS_DIR = os.path.join(BASE_DIR, "2009-2025计算机408真题解析")
OUTPUT_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "static", "images", "questions")
OUTPUT_JSON = os.path.join(BASE_DIR, "extracted_questions.json")
os.makedirs(OUTPUT_DIR, exist_ok=True)

def get_year_files():
    result = {}
    for fname in sorted(os.listdir(ANALYSIS_DIR)):
        m = re.search(r'(\d{4})年', fname)
        if m and fname.endswith('.pdf'):
            year = int(m.group(1))
            if year not in result:
                result[year] = {'analysis': None, 'exam': None}
            result[year]['analysis'] = os.path.join(ANALYSIS_DIR, fname)
    
    for fname in sorted(os.listdir(EXAM_DIR)):
        m = re.search(r'(\d{4})年', fname)
        if m and fname.endswith('.pdf') and '解析' not in fname and '答案' not in fname:
            year = int(m.group(1))
            if year not in result:
                result[year] = {'analysis': None, 'exam': None}
            result[year]['exam'] = os.path.join(EXAM_DIR, fname)
    
    jingjie = os.path.join(EXAM_DIR, "【精解版】2025年408统考真题及答案-皮皮灰.pdf")
    if os.path.exists(jingjie) and 2025 in result:
        result[2025]['analysis_jj'] = jingjie
    return result


def extract_text_and_images(pdf_path, year, source_type):
    """提取PDF的文字和重要图片"""
    doc = fitz.open(pdf_path)
    pages = []
    total_imgs = 0
    
    for pg in range(doc.page_count):
        page = doc[pg]
        text = page.get_text("text")
        images = page.get_images(full=True)
        
        img_refs = []
        for img_idx, img in enumerate(images):
            xref = img[0]
            try:
                base = doc.extract_image(xref)
                if base and len(base["image"]) > 2000:  # 至少2KB
                    ext = base["ext"]
                    name = f"{year}_{source_type}_p{pg+1}_i{img_idx+1}.{ext}"
                    path = os.path.join(OUTPUT_DIR, name)
                    with open(path, 'wb') as f:
                        f.write(base["image"])
                    img_refs.append({"xref": xref, "file": f"/images/questions/{name}", "page": pg+1})
                    total_imgs += 1
            except:
                pass
        
        pages.append({"page": pg+1, "text": text, "images": img_refs})
    
    doc.close()
    print(f"  [{source_type}] {year}: {len(pages)}页, {total_imgs}张图片")
    return pages


def full_text(pages):
    return "\n".join(p["text"] for p in pages)


FIX_NUM = re.compile(r'(\d)\s+(\d)\s*[.．]')  # "0 1 ." -> "01."

def normalize_question_numbers(text):
    """统一题号格式：将 "0 1 ." 变成 "01." """
    return FIX_NUM.sub(r'\1\2.', text)


# 匹配题号（1-47），兼容多种格式
QNUM_RE = re.compile(
    r'(?:^|\n)\s*'           # 行首或换行
    r'('
    r'(?:\d\s+)?\d{1,2}'    # 1-2位数字，可能中间有空格
    r')\s*[.．]\s*'          # 点号 + 空格
    r'(?=[\u4e00-\u9fffA-Za-z(（])',  # 后面必须跟中英文或括号
    re.MULTILINE
)

def extract_all_questions(text):
    """从文本中提取所有题目"""
    text = normalize_question_numbers(text)
    
    # 找到所有题号位置
    matches = list(re.finditer(
        r'(?:^|\n)\s*(\d{1,2})\s*[.．]\s*(?=[\u4e00-\u9fffA-Za-z(（])',
        text, re.MULTILINE
    ))
    
    if not matches:
        return []
    
    questions = []
    for i, m in enumerate(matches):
        q_num = int(m.group(1))
        if q_num < 1 or q_num > 47:
            continue
        
        start = m.end()
        end = matches[i+1].start() if i+1 < len(matches) else len(text)
        q_text = text[start:end].strip()
        
        # 判断题型
        qtype = "单选" if q_num <= 40 else "综合应用"
        subject = get_subject(q_num)
        
        # 解析选项（仅单选题）
        options = None
        content = q_text
        if qtype == "单选":
            content, options = split_mcq_content_options(q_text)
        
        questions.append({
            "question_number": q_num,
            "type": qtype,
            "content": clean(content),
            "options": options,
            "subject": subject,
            "answer": "",
            "analysis": "",
            "knowledge_tag": ""
        })
    
    return questions


def split_mcq_content_options(q_text):
    """分离单选题的内容和选项"""
    # 找第一个选项：A. 或 A． 开头的新行
    opt_start = re.search(r'\n\s*A\s*[.．]', q_text)
    if not opt_start:
        return q_text, None
    
    content = q_text[:opt_start.start()].strip()
    opt_text = q_text[opt_start.start():]
    
    # 解析A-D选项
    options = []
    opt_parts = re.split(r'\n\s*(?=[A-D]\s*[.．])', opt_text)
    for part in opt_parts:
        part = part.strip()
        m = re.match(r'([A-D])\s*[.．]\s*(.+)', part, re.DOTALL)
        if m:
            options.append({"key": m.group(1), "text": clean(m.group(2))})
    
    return content, options if options else None


def get_subject(q_num):
    """根据题号判断科目"""
    if 1 <= q_num <= 11: return "数据结构"
    if 12 <= q_num <= 22: return "计算机组成原理"
    if 23 <= q_num <= 32: return "操作系统"
    if 33 <= q_num <= 40: return "计算机网络"
    if q_num in (41, 42): return "数据结构"
    if q_num in (43, 44): return "计算机组成原理"
    if q_num in (45, 46): return "操作系统"
    if q_num == 47: return "计算机网络"
    return "数据结构"


def clean(t):
    """清理文本"""
    t = re.sub(r'\s+', ' ', t)
    return t.strip()


def extract_answers_from_analysis(pages):
    """从解析PDF提取答案"""
    text = full_text(pages)
    text = normalize_question_numbers(text)
    
    answers = {}
    analyses = {}
    
    # 多种答案格式
    ans_patterns = [
        re.compile(r'(\d{1,2})\s*[.．]\s*【答案】\s*([A-D]+)', re.MULTILINE),
        re.compile(r'(\d{1,2})\s*[.．]\s*答案[：:]\s*([A-D]+)', re.MULTILINE),
        re.compile(r'(\d{1,2})\s*[.．]\s*选\s*([A-D]+)', re.MULTILINE),
        re.compile(r'(?:^|\n)\s*(\d{1,2})\s*[.．]\s*([A-D]+)\s*\n', re.MULTILINE),
    ]
    
    for pat in ans_patterns:
        for m in pat.finditer(text):
            q_num = int(m.group(1))
            if q_num <= 47 and q_num not in answers:
                answers[q_num] = m.group(2).strip()
    
    # 提取解析
    analysis_pat = re.compile(
        r'(\d{1,2})\s*[.．]\s*【解析】\s*([\s\S]*?)(?=\n\s*\d{1,2}\s*[.．]|$)',
        re.MULTILINE
    )
    for m in analysis_pat.finditer(text):
        q_num = int(m.group(1))
        if q_num <= 47:
            analyses[q_num] = clean(m.group(2))[:600]
    
    # 也尝试不带【解析】标记的格式
    if not answers:
        # 查找选项后紧跟的答案行
        simple_pat = re.compile(r'(\d{1,2})\s*[.．][\s\S]*?(?:A\s*[.．][\s\S]*?D\s*[.．][\s\S]*?)(?:答案|选)\s*([A-D]+)', re.MULTILINE)
        for m in simple_pat.finditer(text):
            q_num = int(m.group(1))
            if q_num <= 47 and q_num not in answers:
                answers[q_num] = m.group(2).strip()
    
    return answers, analyses


def process_year(year, files):
    print(f"\n处理 {year} 年:")
    
    # 提取真题PDF
    exam_pages = []
    if files['exam'] and os.path.exists(files['exam']):
        exam_pages = extract_text_and_images(files['exam'], year, "exam")
    elif files['analysis'] and os.path.exists(files['analysis']):
        exam_pages = extract_text_and_images(files['analysis'], year, "exam")
    
    if not exam_pages:
        print(f"  ⚠ 无PDF文件")
        return None
    
    # 检查文字量
    total_text = sum(len(p["text"].strip()) for p in exam_pages)
    if total_text < 200:
        print(f"  ⚠ 扫描版，跳过自动提取 (总文字:{total_text})")
        return None
    
    # 解析题目
    text = full_text(exam_pages)
    questions = extract_all_questions(text)
    
    if not questions:
        print(f"  ⚠ 未解析出题目")
        return None
    
    # 解析答案
    if files.get('analysis') and os.path.exists(files['analysis']):
        ap = extract_text_and_images(files['analysis'], year, "analysis")
        answers, analyses = extract_answers_from_analysis(ap)
    elif files.get('analysis_jj') and os.path.exists(files['analysis_jj']):
        ap = extract_text_and_images(files['analysis_jj'], year, "analysis_jj")
        answers, analyses = extract_answers_from_analysis(ap)
    else:
        answers, analyses = {}, {}
    
    # 填充答案
    has_answer = 0
    for q in questions:
        n = q["question_number"]
        if n in answers:
            q["answer"] = answers[n]
            has_answer += 1
        if n in analyses:
            q["analysis"] = analyses[n]
    
    print(f"  ✓ {len(questions)}题 (含答案:{has_answer})")
    return questions


def main():
    year_files = get_year_files()
    all_questions = []
    failed_years = []
    
    for year in sorted(year_files.keys()):
        qs = process_year(year, year_files[year])
        if qs:
            for q in qs:
                q["year"] = year
            all_questions.append(qs)
        else:
            failed_years.append(year)
    
    # 统计
    total = sum(len(qs) for qs in all_questions)
    has_ans = sum(1 for qs in all_questions for q in qs if q["answer"])
    
    print(f"\n{'='*60}")
    print(f"提取完成!")
    print(f"  成功: {len(all_questions)}年, {total}道题目, {has_ans}道有答案")
    if failed_years:
        print(f"  失败年份: {failed_years}")
    
    with open(OUTPUT_JSON, 'w', encoding='utf-8') as f:
        json.dump(all_questions, f, ensure_ascii=False, indent=2)
    print(f"  JSON: {OUTPUT_JSON}")
    
    # 生成SQL
    sql_lines = ["-- 从PDF提取的408真题数据\n"]
    for yqs in all_questions:
        for q in yqs:
            opts = "NULL"
            if q["options"]:
                opts = "'" + json.dumps(q["options"], ensure_ascii=False).replace("'", "''") + "'"
            content = q["content"].replace("'", "''")
            ans = q["answer"].replace("'", "''")
            ana = q["analysis"].replace("'", "''")
            tag = q.get("knowledge_tag", "")
            
            sql_lines.append(
                f"INSERT INTO question (year, subject, type, question_number, content, options, answer, analysis, knowledge_tag) "
                f"SELECT {q['year']}, '{q['subject']}', '{q['type']}', {q['question_number']}, "
                f"'{content}', {opts}, '{ans}', '{ana}', '{tag}' "
                f"WHERE NOT EXISTS (SELECT 1 FROM question WHERE year={q['year']} AND question_number={q['question_number']});"
            )
    
    sql_path = os.path.join(BASE_DIR, "extracted_data.sql")
    with open(sql_path, 'w', encoding='utf-8') as f:
        f.write("\n".join(sql_lines))
    print(f"  SQL: {sql_path}")

if __name__ == "__main__":
    main()
