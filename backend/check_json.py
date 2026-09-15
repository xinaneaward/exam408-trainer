import json
data = json.load(open(r'c:\projects\408真题训练系统\extracted_questions.json', 'r', encoding='utf-8'))
total = sum(len(y) for y in data)
print(f'年份数: {len(data)}, 总题目: {total}')
for y in data:
    year = y[0]['year']
    has_ans = sum(1 for q in y if q['answer'])
    print(f'  {year}: {len(y)}题 (含答案:{has_ans})')
