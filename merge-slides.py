#!/usr/bin/env python3
"""
merge-slides.py
Appends all slides from research-slides-new.pptx into server-comparison.pptx,
properly registering relationships and updating the slide ID list.
Fixes:
  - Strips notesSlide references from new slide rels (they'll get no notes)
  - Properly updates Content_Types and presentation.xml
"""
import zipfile, shutil, re, os
from io import BytesIO

SRC  = 'server-comparison.pptx'
NEW  = 'research-slides-new.pptx'
OUT  = 'server-comparison.pptx'
TMP  = '_merge_tmp.pptx'

# ── 1. Read both archives into memory ──────────────────────────────────────────
src_bytes = {}
with zipfile.ZipFile(SRC) as z:
    for name in z.namelist():
        src_bytes[name] = z.read(name)

new_bytes = {}
with zipfile.ZipFile(NEW) as z:
    for name in z.namelist():
        new_bytes[name] = z.read(name)

# ── 2. Determine current state of src ──────────────────────────────────────────
src_slides = sorted(
    [n for n in src_bytes if re.match(r'ppt/slides/slide\d+\.xml$', n)],
    key=lambda x: int(re.search(r'\d+', x.split('/')[-1]).group())
)
max_slide_num = max(int(re.search(r'\d+', s.split('/')[-1]).group()) for s in src_slides)
print(f"Source has {len(src_slides)} slides (max index: {max_slide_num})")

new_slides = sorted(
    [n for n in new_bytes if re.match(r'ppt/slides/slide\d+\.xml$', n)],
    key=lambda x: int(re.search(r'\d+', x.split('/')[-1]).group())
)
print(f"New deck has {len(new_slides)} slides to append")

# ── 3. Parse presentation.xml for slide ID list ────────────────────────────────
pres_xml = src_bytes['ppt/presentation.xml'].decode('utf-8')

id_vals = [int(m) for m in re.findall(r'<p:sldId\s+id="(\d+)"', pres_xml)]
max_id = max(id_vals) if id_vals else 255
print(f"Max existing slide id: {max_id}")

# ── 4. Parse presentation.xml.rels ────────────────────────────────────────────
rels_xml = src_bytes['ppt/_rels/presentation.xml.rels'].decode('utf-8')

rids = [int(m) for m in re.findall(r'Id="rId(\d+)"', rels_xml)]
max_rid = max(rids) if rids else 0
print(f"Max existing rId: {max_rid}")

# ── 5. Copy needed supporting files from new deck ────────────────────────────
# Copy slide layouts that don't exist in src
for name, data in new_bytes.items():
    if (name.startswith('ppt/slideLayouts/slideLayout') or
        name.startswith('ppt/slideLayouts/_rels/slideLayout')):
        if name not in src_bytes:
            src_bytes[name] = data
            print(f"  Copied: {name}")

# ── 6. Find available slideLayout in src ──────────────────────────────────────
available_layouts = sorted(
    [n.split('/')[-1] for n in src_bytes
     if re.match(r'ppt/slideLayouts/slideLayout\d+\.xml$', n)],
    key=lambda x: int(re.search(r'\d+', x).group())
)
# Prefer layout 2 (typically "Title and Content") if available
default_layout = 'slideLayout2.xml' if 'slideLayout2.xml' in available_layouts else available_layouts[0]
print(f"Default fallback layout: {default_layout}")

# ── 7. For each new slide, register it ───────────────────────────────────────
new_sldid_entries = []
new_rels_entries  = []

for i, new_slide_path in enumerate(new_slides):
    new_idx = max_slide_num + 1 + i
    new_sid = max_id + 1 + i
    new_rid = max_rid + 1 + i

    old_idx = int(re.search(r'\d+', new_slide_path.split('/')[-1]).group())
    old_rels_path = f'ppt/slides/_rels/slide{old_idx}.xml.rels'

    # Copy slide XML
    src_bytes[f'ppt/slides/slide{new_idx}.xml'] = new_bytes[new_slide_path]

    # Process rels: strip notesSlide references, keep layout/media refs
    if old_rels_path in new_bytes:
        rels_content = new_bytes[old_rels_path].decode('utf-8')

        # Remove any notesSlide relationship lines
        rels_content = re.sub(
            r'\s*<Relationship[^>]+notesSlide[^>]*/>\s*', '\n',
            rels_content
        )

        # Fix layout references — ensure the layout exists in src
        def fix_layout_ref(m):
            layout_file = m.group(1)
            if f'ppt/slideLayouts/{layout_file}' in src_bytes:
                return m.group(0)  # keep as-is
            print(f"    Slide {new_idx}: remapping {layout_file} → {default_layout}")
            return m.group(0).replace(layout_file, default_layout)

        rels_content = re.sub(
            r'Target="\.\./slideLayouts/(slideLayout\d+\.xml)"',
            lambda m: m.group(0) if f'ppt/slideLayouts/{m.group(1)}' in src_bytes
                      else m.group(0).replace(m.group(1), default_layout),
            rels_content
        )

        src_bytes[f'ppt/slides/_rels/slide{new_idx}.xml.rels'] = rels_content.encode('utf-8')
    else:
        # Minimal rels
        min_rels = (
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            f'<Relationship Id="rId1" '
            f'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" '
            f'Target="../slideLayouts/{default_layout}"/>'
            '</Relationships>'
        )
        src_bytes[f'ppt/slides/_rels/slide{new_idx}.xml.rels'] = min_rels.encode('utf-8')
        print(f"  Slide {new_idx}: minimal rels with {default_layout}")

    new_sldid_entries.append(f'<p:sldId id="{new_sid}" r:id="rId{new_rid}"/>')
    new_rels_entries.append(
        f'<Relationship Id="rId{new_rid}" '
        f'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" '
        f'Target="slides/slide{new_idx}.xml"/>'
    )
    print(f"  Added slide {new_idx} (id={new_sid}, rId=rId{new_rid})")

# ── 8. Update [Content_Types].xml ────────────────────────────────────────────
ct_xml = src_bytes['[Content_Types].xml'].decode('utf-8')
ct_inserts = []
for i in range(len(new_slides)):
    new_idx = max_slide_num + 1 + i
    sn = f'/ppt/slides/slide{new_idx}.xml'
    if sn not in ct_xml:
        ct_inserts.append(
            f'<Override PartName="{sn}" '
            f'ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>'
        )
if ct_inserts:
    ct_xml = ct_xml.replace('</Types>', '  ' + '\n  '.join(ct_inserts) + '\n</Types>')
src_bytes['[Content_Types].xml'] = ct_xml.encode('utf-8')

# ── 9. Update ppt/_rels/presentation.xml.rels ────────────────────────────────
rels_xml = rels_xml.replace(
    '</Relationships>',
    '  ' + '\n  '.join(new_rels_entries) + '\n</Relationships>'
)
src_bytes['ppt/_rels/presentation.xml.rels'] = rels_xml.encode('utf-8')

# ── 10. Update ppt/presentation.xml sldIdLst ─────────────────────────────────
pres_xml = pres_xml.replace(
    '</p:sldIdLst>',
    '    ' + '\n    '.join(new_sldid_entries) + '\n  </p:sldIdLst>'
)
src_bytes['ppt/presentation.xml'] = pres_xml.encode('utf-8')

# ── 11. Write merged archive ──────────────────────────────────────────────────
with zipfile.ZipFile(TMP, 'w', zipfile.ZIP_DEFLATED) as zout:
    for name, data in src_bytes.items():
        zout.writestr(name, data)

shutil.move(TMP, OUT)
print(f"\nMerge complete! Slides now: {len(src_slides) + len(new_slides)}")
print(f"Output: {OUT}")
