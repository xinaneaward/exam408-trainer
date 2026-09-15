"""检查所有年份PDF类型"""
import fitz
import os

base_dir = r"c:\projects\408真题训练系统\2009-2025计算机408统考真题"

files = sorted([f for f in os.listdir(base_dir) if f.endswith('.pdf')])
for f in files:
    path = os.path.join(base_dir, f)
    doc = fitz.open(path)
    
    # 检查前2页文字
    total_text = 0
    total_imgs = 0
    for page_num in range(doc.page_count):
        page = doc[page_num]
        total_text += len(page.get_text("text").strip())
        total_imgs += len(page.get_images())
    
    ptype = "扫描版(无文字)" if total_text < 100 else f"文字版({total_text}字)"
    print(f"{f[:30]:35s} | 页数={doc.page_count:2d} | {ptype:20s} | 图片数={total_imgs}")
    doc.close()
