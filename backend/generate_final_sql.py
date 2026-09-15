"""
最终数据整合：将提取的题目+页面图片映射组合为data.sql
同时为所有年份创建题目记录（即使文本提取失败）
"""
import json
import os
import re

BASE_DIR = r"c:\projects\408真题训练系统"
JSON_PATH = os.path.join(BASE_DIR, "extracted_questions.json")
OUTPUT_SQL = os.path.join(BASE_DIR, "src", "main", "resources", "db", "data.sql")
PAGE_IMG_PREFIX = "/images/pages"

# 题目编号到页码的映射（近似，不同年份略有差异）
# 408真题通常40道选择题在前4-5页，7道大题在后2-3页
def estimate_page(q_num):
    """估算题目所在页码"""
    if q_num <= 8: return 1
    if q_num <= 16: return 2
    if q_num <= 24: return 3
    if q_num <= 32: return 4
    if q_num <= 40: return 5
    if q_num <= 43: return 6
    if q_num <= 47: return 7
    return 1

def escape(s):
    """SQL字符串转义"""
    if s is None:
        return "NULL"
    return "'" + s.replace("'", "''").replace("\\", "\\\\") + "'"

def main():
    # 加载提取的题目
    with open(JSON_PATH, 'r', encoding='utf-8') as f:
        extracted = json.load(f)
    
    # 构建 年份->[题目] 映射
    year_questions = {}
    for year_data in extracted:
        year = year_data[0]["year"]
        year_questions[year] = year_data
    
    sql_lines = [
        "-- ============================================================",
        "-- 408真题训练系统 题目数据（从PDF真题提取）",
        "-- 共 " + str(sum(len(v) for v in year_questions.values())) + " 道题目",
        "-- 覆盖年份: " + ", ".join(str(y) for y in sorted(year_questions.keys())),
        "-- 所有年份的PDF页面图片可在 /images/pages/ 查看",
        "-- ============================================================\n"
    ]
    
    # 为成功提取的年份生成SQL
    for year in sorted(year_questions.keys()):
        questions = year_questions[year]
        sql_lines.append(f"\n-- ========== {year}年 ({len(questions)}题) ==========")
        
        for q in questions:
            q_num = q["question_number"]
            subject = q["subject"]
            qtype = q["type"]
            content = q["content"]
            answer = q.get("answer", "")
            analysis = q.get("analysis", "")
            
            # 构建选项JSON
            options_json = "NULL"
            if q.get("options") and len(q["options"]) > 0:
                options_json = escape(json.dumps(q["options"], ensure_ascii=False))
            
            # 添加原题页面图片引用
            page = estimate_page(q_num)
            page_img = f'{PAGE_IMG_PREFIX}/exam_{year}_p{page:02d}.png'
            
            # 在内容末尾添加查看原题的图片链接（用HTML注释存储，前端可解析）
            content_with_img = content + f'\n<!-- page_img:{page_img} -->'
            
            sql_lines.append(
                f"INSERT INTO question (year, subject, type, question_number, content, options, answer, analysis, knowledge_tag) "
                f"SELECT {year}, {escape(subject)}, {escape(qtype)}, {q_num}, "
                f"{escape(content_with_img)}, {options_json}, {escape(answer)}, {escape(analysis)}, {escape('')} "
                f"WHERE NOT EXISTS (SELECT 1 FROM question WHERE year={year} AND question_number={q_num});"
            )
    
    # 为未成功提取文字但PDF有页面图片的年份，创建占位题目记录
    all_years = set(range(2009, 2026))
    extracted_years = set(year_questions.keys())
    missing_years = all_years - extracted_years
    
    if missing_years:
        sql_lines.append(f"\n-- ========== 以下年份仅保留页面图片（文字提取失败） ==========")
        for year in sorted(missing_years):
            # 检查是否有真题PDF页面图片
            first_page = os.path.join(BASE_DIR, "src", "main", "resources", "static", "images", "pages", f"exam_{year}_p01.png")
            if os.path.exists(first_page):
                sql_lines.append(f"\n-- {year}年：PDF页面图片已就绪，共8页左右")
                # 创建47道占位题目，带页面图片引用
                for q_num in range(1, 48):
                    subject_map = {range(1,12): "数据结构", range(12,23): "计算机组成原理", 
                                  range(23,33): "操作系统", range(33,41): "计算机网络",
                                  range(41,43): "数据结构", range(43,45): "计算机组成原理",
                                  range(45,47): "操作系统"}
                    subject = "数据结构"
                    for r, s in subject_map.items():
                        if q_num in r:
                            subject = s
                            break
                    if q_num == 47: subject = "计算机网络"
                    
                    qtype = "单选" if q_num <= 40 else "综合应用"
                    page = estimate_page(q_num)
                    page_img = f'{PAGE_IMG_PREFIX}/exam_{year}_p{page:02d}.png'
                    content = f'（{year}年第{q_num}题 - 请查看原题图片）\n<!-- page_img:{page_img} -->'
                    
                    sql_lines.append(
                        f"INSERT INTO question (year, subject, type, question_number, content, options, answer, analysis, knowledge_tag) "
                        f"SELECT {year}, {escape(subject)}, {escape(qtype)}, {q_num}, "
                        f"{escape(content)}, NULL, {escape('')}, {escape('（请查看原题图片）')}, {escape('')} "
                        f"WHERE NOT EXISTS (SELECT 1 FROM question WHERE year={year} AND question_number={q_num});"
                    )
    
    # 写入文件
    with open(OUTPUT_SQL, 'w', encoding='utf-8') as f:
        f.write("\n".join(sql_lines))
    
    total = sum(len(v) for v in year_questions.values()) + len(missing_years) * 47
    print(f"SQL生成完成: {OUTPUT_SQL}")
    print(f"  成功提取: {len(extracted_years)}年, {sum(len(v) for v in year_questions.values())}题")
    print(f"  占位题目: {len(missing_years)}年, {len(missing_years)*47}题")
    print(f"  总计: {total}条记录")

if __name__ == "__main__":
    main()
