"""
PDF页面图片提取 + 题目文本提取（组合方案）
1. 所有PDF提取高清页面图片 → 用于网页展示
2. 文字版PDF尝试提取题目文本 → 用于数据库
"""
import fitz
import os
import re
import json

BASE_DIR = r"c:\projects\408真题训练系统"
EXAM_DIR = os.path.join(BASE_DIR, "2009-2025计算机408统考真题")
ANALYSIS_DIR = os.path.join(BASE_DIR, "2009-2025计算机408真题解析")
PAGE_IMG_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "static", "images", "pages")
os.makedirs(PAGE_IMG_DIR, exist_ok=True)

def render_pdf_pages(pdf_path, year, prefix):
    """将PDF每页渲染为PNG图片"""
    doc = fitz.open(pdf_path)
    saved = 0
    for pg in range(doc.page_count):
        page = doc[pg]
        # 渲染为图片，2x缩放保证清晰度
        mat = fitz.Matrix(2, 2)
        pix = page.get_pixmap(matrix=mat)
        img_name = f"{prefix}_{year}_p{pg+1:02d}.png"
        img_path = os.path.join(PAGE_IMG_DIR, img_name)
        pix.save(img_path)
        saved += 1
    doc.close()
    return saved

def main():
    # 处理所有真题PDF和解析PDF
    all_page_images = {}
    
    # 真题PDF → 页面图片
    for fname in sorted(os.listdir(EXAM_DIR)):
        if not fname.endswith('.pdf'):
            continue
        m = re.search(r'(\d{4})年', fname)
        if not m:
            continue
        year = int(m.group(1))
        pdf_path = os.path.join(EXAM_DIR, fname)
        n = render_pdf_pages(pdf_path, year, "exam")
        all_page_images[year] = {"exam_pages": n}
        print(f"  真题 {year}年: {n}页图片")
    
    # 解析PDF → 页面图片
    for fname in sorted(os.listdir(ANALYSIS_DIR)):
        if not fname.endswith('.pdf'):
            continue
        m = re.search(r'(\d{4})年', fname)
        if not m:
            continue
        year = int(m.group(1))
        pdf_path = os.path.join(ANALYSIS_DIR, fname)
        n = render_pdf_pages(pdf_path, year, "analysis")
        if year not in all_page_images:
            all_page_images[year] = {}
        all_page_images[year]["analysis_pages"] = n
        print(f"  解析 {year}年: {n}页图片")
    
    total = sum(v.get("exam_pages", 0) + v.get("analysis_pages", 0) for v in all_page_images.values())
    print(f"\n总计: {total}张页面图片 → {PAGE_IMG_DIR}")

if __name__ == "__main__":
    main()
