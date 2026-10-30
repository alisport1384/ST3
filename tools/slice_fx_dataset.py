#!/usr/bin/env python3
"""Recreate the exact published GBPUSD sub-windows from the 20-year CSV gzip.
Example: python3 tools/slice_fx_dataset.py /path/GBPUSD-20y-M1-volume.csv.gz 10 output.csv.gz
"""
import sys, gzip, json, datetime, hashlib, os
from pathlib import Path
if len(sys.argv) != 4 or sys.argv[2] not in ('1', '2', '3', '10'):
    sys.exit('usage: slice_fx_dataset.py 20y.csv.gz {1|2|3|10} out.csv.gz')
src, years, out = sys.argv[1], sys.argv[2], sys.argv[3]
meta = json.loads((Path(__file__).resolve().parent.parent / 'datasets/fx_gbpusd_summary.json').read_text())[years]
cut = int(datetime.datetime(2026-int(years), 8, 21, tzinfo=datetime.timezone.utc).timestamp()*1000)
tmp = out + '.part'
try:
    with gzip.open(src, 'rb') as inp, open(tmp,'wb') as raw:
        with gzip.GzipFile(filename='', fileobj=raw, mode='wb', compresslevel=9, mtime=0) as gz:
            gz.write(inp.readline())
            for row in inp:
                if int(row.split(b',',1)[0]) >= cut: gz.write(row)
    h=hashlib.sha256()
    with open(tmp,'rb') as f:
        for block in iter(lambda: f.read(1024*1024), b''): h.update(block)
    if h.hexdigest()!=meta['sha256']:
        raise ValueError('SHA-256 mismatch: source is not the expected 20-year dataset')
    os.replace(tmp,out)
    print(f'{out}: {meta["bars"]} rows; SHA-256 {h.hexdigest()}')
finally:
    if os.path.exists(tmp): os.unlink(tmp)
