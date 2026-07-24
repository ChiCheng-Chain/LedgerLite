"""生成 launcher 图标 PNG。纯标准库（zlib + struct）。
支持 RGB(color_type=2) 和 RGBA(6)。整图缩放到各密度。"""
import zlib, struct, os

SRC = r"C:\Users\mlamp\Documents\Codex\2026-07-23\new-chat-4\outputs\abstract_leaf_icon_transparent_clean.png"
OUT_DIR = r"d:\LedgerLite\android\app\src\main\res"

# launcher 图标各密度像素尺寸
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

def read_png(path):
    with open(path, "rb") as f:
        data = f.read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    pos = 8
    width = height = ct = 0
    idat = []
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos+4])[0]
        typ = data[pos+4:pos+8]
        chunk = data[pos+8:pos+8+ln]
        if typ == b"IHDR":
            width, height, _, ct = struct.unpack(">IIBB", chunk[:10])
        elif typ == b"IDAT":
            idat.append(chunk)
        elif typ == b"IEND":
            break
        pos += 8 + ln + 4
    raw = zlib.decompress(b"".join(idat))
    bpp = 4 if ct == 6 else 3
    stride = width * bpp
    pixels = bytearray()
    prev = bytearray(stride)
    i = 0
    for y in range(height):
        f = raw[i]; i += 1
        line = bytearray(raw[i:i+stride]); i += stride
        for x in range(stride):
            a = line[x-bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x-bpp] if x >= bpp else 0
            if f == 1: line[x] = (line[x] + a) & 0xff
            elif f == 2: line[x] = (line[x] + b) & 0xff
            elif f == 3: line[x] = (line[x] + (a + b) // 2) & 0xff
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p-a), abs(p-b), abs(p-c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 0xff
        pixels.extend(line)
        prev = line
    return width, height, ct, bytes(pixels)

def write_png(path, width, height, px, has_alpha):
    def chunk(typ, data):
        c = struct.pack(">I", len(data)) + typ + data
        c += struct.pack(">I", zlib.crc32(typ + data) & 0xffffffff)
        return c
    sig = b"\x89PNG\r\n\x1a\n"
    ct = 6 if has_alpha else 2
    ihdr = struct.pack(">IIBBBBB", width, height, 8, ct, 0, 0, 0)
    bpp = 4 if has_alpha else 3
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        raw.extend(px[y*width*bpp:(y+1)*width*bpp])
    idat = zlib.compress(bytes(raw), 9)
    with open(path, "wb") as f:
        f.write(sig + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b""))

def scale(src_w, src_h, src_px, src_ct, dst_w, dst_h):
    """双线性缩放。src_ct 2=RGB(3), 6=RGBA(4)。输出与源同通道。"""
    bpp = 4 if src_ct == 6 else 3
    out = bytearray(dst_w * dst_h * bpp)
    for dy in range(dst_h):
        sy = (dy + 0.5) * src_h / dst_h - 0.5
        y0 = int(sy); y1 = min(src_h-1, y0+1); fy = sy - y0
        if y0 < 0: y0 = 0
        for dx in range(dst_w):
            sx = (dx + 0.5) * src_w / dst_w - 0.5
            x0 = int(sx); x1 = min(src_w-1, x0+1); fx = sx - x0
            if x0 < 0: x0 = 0
            o00 = (y0*src_w + x0)*bpp
            o01 = (y0*src_w + x1)*bpp
            o10 = (y1*src_w + x0)*bpp
            o11 = (y1*src_w + x1)*bpp
            do = (dy*dst_w + dx)*bpp
            for ch in range(bpp):
                v = (src_px[o00+ch]*(1-fx)*(1-fy) + src_px[o01+ch]*fx*(1-fy)
                     + src_px[o10+ch]*(1-fx)*fy + src_px[o11+ch]*fx*fy)
                out[do+ch] = int(v + 0.5) & 0xff
    return bytes(out)

def main():
    sw, sh, ct, spx = read_png(SRC)
    print(f"源图 {sw}x{sh} colortype={ct}")
    has_alpha = (ct == 6)
    for name, size in DENSITIES.items():
        out = scale(sw, sh, spx, ct, size, size)
        d = os.path.join(OUT_DIR, f"mipmap-{name}")
        os.makedirs(d, exist_ok=True)
        p = os.path.join(d, "ic_launcher.png")
        write_png(p, size, size, out, has_alpha)
        # round icon 同图
        write_png(os.path.join(d, "ic_launcher_round.png"), size, size, out, has_alpha)
        print(f"  {name}: {size}x{size} -> {p}")

if __name__ == "__main__":
    main()
