"""检查多个PDF的类型"""
import fitz
import os

base_dirs = [
    r"c:\projects\408真题训练系统\2009-2025计算机408统考真题",
    r"c:\projects\408真题训练系统\2009-2025计算机408真题解析"
]

for base in base_dirs:
    print(f"\n{'='*60}")
    print(f"目录: {os.path.basename(base)}")
    files = sorted(os.listdir(base))[:3]  # 检查前3个
    for f in files:
        if f.endswith('.pdf'):
            path = os.path.join(base, f)
            doc = fitz.open(path)
            has_text = False
            img_count = 0
            for page_num in range(min(3, doc.page_count)):
                page = doc[page_num]
                text = page.get_text("text").strip()
                if len(text) > 50:
                    has_text = True
                imgs = page.get_images()
                img_count += len(imgs)
            
            print(f"  {f[:20]}: 页数={doc.page_count}, 有文字={has_text}, 总图片={img_count}")
            doc.close()
