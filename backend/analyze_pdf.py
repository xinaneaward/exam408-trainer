"""分析408真题PDF结构"""
import fitz  # PyMuPDF
import json
import os

pdf_path = r"c:\projects\408真题训练系统\2009-2025计算机408统考真题\2025年计算机408统考真题.pdf"
doc = fitz.open(pdf_path)

print(f"总页数: {doc.page_count}")

# 分析前几页
for page_num in range(min(5, doc.page_count)):
    page = doc[page_num]
    text = page.get_text("text")
    print(f"\n{'='*60}")
    print(f"=== 第 {page_num+1} 页 ===")
    print(f"=== 文字内容 (前800字) ===")
    print(text[:800])
    
    # 检查图片
    images = page.get_images()
    print(f"\n=== 图片数量: {len(images)} ===")
    for i, img in enumerate(images[:5]):
        xref = img[0]
        pix = fitz.Pixmap(doc, xref)
        print(f"  图片{i+1}: xref={xref}, 尺寸={pix.width}x{pix.height}, 色彩空间={pix.colorspace}")

doc.close()
