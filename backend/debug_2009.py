"""深入检查2009年PDF - 所有页"""
import fitz

doc = fitz.open(r"c:\projects\408真题训练系统\2009-2025计算机408统考真题\2009年计算机408统考真题.pdf")

for pg in range(doc.page_count):
    page = doc[pg]
    text = page.get_text("text")
    
    # 也试一下 blocks 模式
    blocks = page.get_text("blocks")
    block_text = "\n".join(b[4] for b in blocks if b[6] == 0)  # text blocks only
    
    print(f"\n--- 第{pg+1}页 text长度={len(text)}, blocks文本长度={len(block_text)} ---")
    if len(text) > 50:
        print(text[:500])
    elif len(block_text) > 50:
        print("blocks模式:", block_text[:500])
    else:
        print("  (极少量文字)")

doc.close()
