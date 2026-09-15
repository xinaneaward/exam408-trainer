"""查看PDF文字格式"""
import fitz

# 看2024年的（最新的文字版）
doc = fitz.open(r"c:\projects\408真题训练系统\2009-2025计算机408统考真题\2024年计算机408统考真题.pdf")

for page_num in range(min(5, doc.page_count)):
    page = doc[page_num]
    text = page.get_text("text")
    print(f"\n{'='*60}")
    print(f"=== 第 {page_num+1} 页 ===")
    print(text[:1500])

    # 也看下这个页面上的图片
    imgs = page.get_images(full=True)
    if imgs:
        print(f"\n--- 该页图片 ({len(imgs)}个) ---")
        for img in imgs[:5]:
            xref = img[0]
            # 获取图片在页面上的位置
            rects = page.get_image_rects(img)
            for r in rects[:2]:
                print(f"  xref={xref}, 位置={r}")

doc.close()
