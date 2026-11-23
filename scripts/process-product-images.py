"""P20B: xử lý ảnh sản phẩm demo do owner đặt vào assets-raw/products/.

Chạy từ thư mục gốc repo (cần Pillow có hỗ trợ WebP):
    python scripts/process-product-images.py

- Ghép theo research_id ở đầu tên file (r001.JPG, R001_final.png -> r001; hậu tố -2, _2, " 2" là ảnh phụ).
  Sau mã phải là hết tên hoặc một ký tự không phải chữ số; r0010.jpg KHÔNG được hiểu là r001 hay r010 (không đoán).
- Định dạng nhận diện theo nội dung file (Pillow), không theo đuôi.
- Cắt 4:5 quanh tâm, không bóp méo; vùng cắt nhỏ hơn 600x750 -> QUA_NHO, bỏ qua; nhỏ hơn 800x1000 -> giữ cỡ, cảnh báo;
  lớn hơn -> thu về 800x1000. Xuất WebP q80, giảm 5 mỗi bước tới 60 nếu > 300 KB. Không ghi EXIF/ICC/XMP.
- Ghi src/main/resources/static/img/products/<id>.webp và <id>-2.webp (ảnh phụ chỉ lưu file).
- Cập nhật docs/data/danh-sach-anh-san-pham.csv (trang_thai, thêm cột nguon_anh/giay_phep nếu chưa có) và
  docs/data/product-research.csv (image_path; image_source/image_license chép từ nguon_anh/giay_phep khi owner đã điền).
- Ghi kết quả JSON vào docs/qa/P20B/process-images.json.
"""
import csv
import io
import json
import os
import re
import sys
from datetime import datetime, timezone

from PIL import Image, ImageOps

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, 'assets-raw', 'products')
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'static', 'img', 'products')
LIST_CSV = os.path.join(ROOT, 'docs', 'data', 'danh-sach-anh-san-pham.csv')
RESEARCH_CSV = os.path.join(ROOT, 'docs', 'data', 'product-research.csv')
QA_JSON = os.path.join(ROOT, 'docs', 'qa', 'P20B', 'process-images.json')

TARGET = (800, 1000)
MIN_CROP = (600, 750)
MAX_BYTES = 300 * 1024
QUALITY_START, QUALITY_MIN, QUALITY_STEP = 80, 60, 5
SEED_NAME = re.compile(r'^[a-z0-9](?:[a-z0-9-]{0,98}[a-z0-9])?\.(?:webp|jpg|png)$')  # khớp regex trong seed-demo.ps1
FILE_ID = re.compile(r'^r(\d{3})(?![0-9])(?P<phu>[-_ ]2(?![0-9]))?', re.IGNORECASE)
READABLE = {'JPEG', 'PNG', 'WEBP'}


def crop_4_5(img):
    w, h = img.size
    if w * 5 >= h * 4:   # quá rộng -> cắt hai bên
        cw, ch = h * 4 // 5, h
    else:                # quá cao -> cắt trên dưới
        cw, ch = w, w * 5 // 4
    left, top = (w - cw) // 2, (h - ch) // 2
    return img.crop((left, top, left + cw, top + ch))


def to_rgb(img):
    if img.mode in ('RGBA', 'LA') or (img.mode == 'P' and 'transparency' in img.info):
        rgba = img.convert('RGBA')
        bg = Image.new('RGB', rgba.size, (255, 255, 255))   # nền trắng cho ảnh trong suốt
        bg.paste(rgba, mask=rgba.split()[3])
        return bg
    return img.convert('RGB')


def encode(img):
    for q in range(QUALITY_START, QUALITY_MIN - 1, -QUALITY_STEP):
        buf = io.BytesIO()
        img.save(buf, 'WEBP', quality=q, method=6)   # không truyền exif/icc_profile -> không có metadata
        if buf.tell() <= MAX_BYTES:
            return buf.getvalue(), q, True
    return buf.getvalue(), q, False


def read_csv(path):
    raw = open(path, 'rb').read().decode('utf-8')
    rows = list(csv.reader(io.StringIO(raw)))
    return rows[0], rows[1:]


def write_list_csv(header, rows):
    out = io.StringIO()
    w = csv.writer(out, quoting=csv.QUOTE_ALL, lineterminator='\r\n')
    w.writerow(header)
    w.writerows(rows)
    open(LIST_CSV, 'w', encoding='utf-8', newline='').write(out.getvalue())


def write_research_csv(header, rows):
    # Giữ định dạng hiện có: header không quote, mọi ô dữ liệu quote, CRLF.
    out = io.StringIO()
    out.write(','.join(header) + '\r\n')
    w = csv.writer(out, quoting=csv.QUOTE_ALL, lineterminator='\r\n')
    w.writerows(rows)
    open(RESEARCH_CSV, 'w', encoding='utf-8', newline='').write(out.getvalue())


def main():
    files = sorted(f for f in os.listdir(RAW) if os.path.isfile(os.path.join(RAW, f)) and f.lower() != 'readme.txt')
    skipped, groups = [], {}
    for f in files:
        m = FILE_ID.match(f)
        if not m:
            skipped.append({'file': f, 'ly_do': 'KHONG_GHEP_DUOC: tên không bắt đầu bằng r + đúng 3 chữ số (vd r010.jpg, r010-2.jpg)'})
            continue
        key = ('r' + m.group(1), 'phu' if m.group('phu') else 'chinh')
        groups.setdefault(key, []).append(f)

    list_header, list_rows = read_csv(LIST_CSV)
    for col in ('nguon_anh', 'giay_phep'):
        if col not in list_header:
            list_header.append(col)
            for r in list_rows:
                r.append('')
    li = {c: i for i, c in enumerate(list_header)}
    known = {r[li['research_id']] for r in list_rows}

    processed = {}
    for (rid, kind), names in sorted(groups.items()):
        if rid not in known:
            skipped += [{'file': n, 'ly_do': f'KHONG_GHEP_DUOC: {rid} không có trong catalog'} for n in names]
            continue
        if len(names) > 1:
            skipped += [{'file': n, 'ly_do': f'KHONG_GHEP_DUOC: {rid} có {len(names)} ảnh {kind}, không đoán'} for n in names]
            continue
        src = names[0]
        item = {'file': src, 'research_id': rid, 'loai': kind}
        try:
            with Image.open(os.path.join(RAW, src)) as im:
                item['dinh_dang'] = im.format
                if im.format not in READABLE:
                    raise ValueError(f'định dạng {im.format} không hỗ trợ')
                im = ImageOps.exif_transpose(im)
                item['goc'] = list(im.size)
                img = crop_4_5(to_rgb(im))
        except Exception as e:  # noqa: BLE001 - báo lỗi đọc từng file, không dừng cả lượt
            skipped.append({'file': src, 'ly_do': f'KHONG_DOC_DUOC: {e}'})
            continue
        item['sau_cat'] = list(img.size)
        if img.size[0] < MIN_CROP[0] or img.size[1] < MIN_CROP[1]:
            item['trang_thai'] = 'QUA_NHO'
            skipped.append({'file': src, 'ly_do': f'QUA_NHO: sau cắt 4:5 còn {img.size[0]}x{img.size[1]} < 600x750'})
            processed[(rid, kind)] = item
            continue
        if img.size[0] > TARGET[0]:
            img = img.resize(TARGET, Image.Resampling.LANCZOS)
        else:
            item['canh_bao'] = f'nhỏ hơn 800x1000, giữ nguyên {img.size[0]}x{img.size[1]}'
        data, q, ok = encode(img)
        name = f'{rid}{"-2" if kind == "phu" else ""}.webp'
        assert SEED_NAME.match(name), name
        if not ok:
            item['trang_thai'] = 'QUA_DUNG_LUONG'
            skipped.append({'file': src, 'ly_do': f'QUA_DUNG_LUONG: {len(data)} B > 300 KB ở chất lượng {q}'})
            processed[(rid, kind)] = item
            continue
        os.makedirs(OUT, exist_ok=True)
        open(os.path.join(OUT, name), 'wb').write(data)
        item.update({'xuat': f'/img/products/{name}', 'kich_thuoc': list(img.size), 'chat_luong': q, 'bytes': len(data), 'trang_thai': 'OK'})
        processed[(rid, kind)] = item

    def ok(rid, kind):
        return processed.get((rid, kind), {}).get('trang_thai') == 'OK'

    for r in list_rows:
        rid = r[li['research_id']]
        chinh = processed.get((rid, 'chinh'))
        if ok(rid, 'chinh'):
            r[li['trang_thai']] = 'DA_NAP' if ok(rid, 'phu') else 'THIEU_ANH_PHU'
        elif chinh:
            r[li['trang_thai']] = chinh['trang_thai']
        elif any(s['file'].lower().startswith(rid) for s in skipped):
            r[li['trang_thai']] = 'KHONG_GHEP_DUOC'
        else:
            r[li['trang_thai']] = 'CHUA_CO_ANH'
    write_list_csv(list_header, list_rows)

    rh, rrows = read_csv(RESEARCH_CSV)
    ri = {c: i for i, c in enumerate(rh)}
    nguon = {r[li['sku']]: (r[li['nguon_anh']].strip(), r[li['giay_phep']].strip()) for r in list_rows}
    can_nguon = []
    for r in rrows:
        rid = r[ri['research_id']].lower()
        if not ok(rid, 'chinh'):
            continue   # chưa có ảnh: giữ nguyên image_path (placeholder) và nguồn cũ
        r[ri['image_path']] = processed[(rid, 'chinh')]['xuat']
        src, lic = nguon.get(r[ri['variant_sku']], ('', ''))
        if src and lic:
            r[ri['image_source']], r[ri['image_license']] = src, lic
        else:
            can_nguon.append(r[ri['variant_sku']])
    write_research_csv(rh, rrows)

    report = {
        'thoi_gian': datetime.now(timezone.utc).isoformat(timespec='seconds'),
        'pillow': Image.__version__,
        'so_file_goc': len(files),
        'so_sku': len(list_rows),
        'sku_da_nap': sum(1 for r in list_rows if r[li['trang_thai']] in ('DA_NAP', 'THIEU_ANH_PHU')),
        'xu_ly': list(processed.values()),
        'bo_qua': skipped,
        'sku_can_owner_dien_nguon': can_nguon,
    }
    os.makedirs(os.path.dirname(QA_JSON), exist_ok=True)
    open(QA_JSON, 'w', encoding='utf-8').write(json.dumps(report, ensure_ascii=False, indent=2))
    print(json.dumps({k: report[k] for k in ('so_file_goc', 'so_sku', 'sku_da_nap')}, ensure_ascii=False))
    for s in skipped:
        print('BO QUA', s['file'], '-', s['ly_do'])
    for it in processed.values():
        if it.get('trang_thai') == 'OK':
            print('OK', it['file'], '->', it['xuat'], it['kich_thuoc'], f"q{it['chat_luong']}", it['bytes'], 'B', it.get('canh_bao', ''))
    return 0


if __name__ == '__main__':
    sys.exit(main())
