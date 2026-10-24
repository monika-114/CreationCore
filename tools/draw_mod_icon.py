#!/usr/bin/env python3
"""Render the actual legacy workbench textures as a crisp isometric block icon."""
from pathlib import Path
from PIL import Image

RES=Path(__file__).resolve().parents[1]/'src/main/resources'
BLOCK=RES/'assets/creationcore/textures/block'
im=Image.new('RGBA',(128,128),(0,0,0,0))

def face(name,origin,a,b,shade):
    tex=Image.open(BLOCK/f'creative_crafting_table_{name}.png').convert('RGBA')
    det=a[0]*b[1]-a[1]*b[0]
    for y in range(128):
        for x in range(128):
            dx=x+0.5-origin[0]; dy=y+0.5-origin[1]
            u=(dx*b[1]-dy*b[0])/det; v=(a[0]*dy-a[1]*dx)/det
            if 0<=u<1 and 0<=v<1:
                color=tex.getpixel((min(15,int(u*16)),min(15,int(v*16))))
                im.putpixel((x,y),tuple(round(c*shade) for c in color[:3])+(color[3],))

face('front',(8,34),(56,28),(0,56),0.9)
face('side',(64,62),(56,-28),(0,56),0.7)
face('top',(64,6),(56,28),(-56,28),1.0)
im.save(RES/'logo.png')
print('Rendered 128x128 logo from restored 16x16 workbench textures without smoothing.')
