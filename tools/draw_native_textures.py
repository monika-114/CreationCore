#!/usr/bin/env python3
"""Original art drawn at final pixel size; no source images or downsampling."""
from pathlib import Path
from PIL import Image, ImageDraw
TEX=Path(__file__).resolve().parents[1]/'src/main/resources/assets/creationcore/textures'
INK='#100c17'; BLACK='#18121f'; PANEL='#292034'; SHADE='#211a2b'
FRAME='#493157'; PURPLE='#5f377f'; MID='#8757ab'; LIGHT='#b989dc'; WHITE='#e3c6f5'

def tile():
    im=Image.new('RGB',(16,16),BLACK); d=ImageDraw.Draw(im)
    d.rectangle((0,0,15,15),outline=INK)
    d.line((1,14,1,1,14,1),fill=MID); d.line((14,1,14,14,1,14),fill=FRAME)
    d.rectangle((2,2,13,13),fill=PANEL)
    for x,y in ((2,2),(13,2),(2,13),(13,13)): d.point((x,y),fill=LIGHT)
    return im,d

def save(im,name):
    p=TEX/name; p.parent.mkdir(parents=True,exist_ok=True); im.save(p)

def top(crafter=False,active=False):
    im,d=tile()
    for y in (3,7,11):
        for x in (3,7,11):
            d.rectangle((x,y,x+2,y+2),fill=INK)
            d.line((x,y+2,x,y,x+2,y),fill=LIGHT if active else MID)
            d.point((x+1,y+1),fill=LIGHT if active else PURPLE)
    if crafter:
        for x,y in ((7,1),(8,14),(1,8),(14,7)): d.point((x,y),fill=WHITE if active else LIGHT)
    return im

def side(kind,active=False):
    im,d=tile()
    if kind=='front':
        d.rectangle((4,4,11,10),fill=INK,outline=MID)
        d.line((5,5,10,5),fill=FRAME)
        d.line((6,8,9,8),fill=LIGHT if active else PURPLE)
        d.line((7,7,8,7),fill=WHITE if active else LIGHT)
        d.line((7,11,8,11),fill=LIGHT); d.point((7,12),fill=MID)
    elif kind=='bottom':
        d.rectangle((4,4,11,11),outline=FRAME); d.rectangle((6,6,9,9),outline=PURPLE)
    elif kind=='table':
        d.rectangle((3,3,12,5),fill=PURPLE); d.line((4,3,11,3),fill=LIGHT)
        d.rectangle((3,7,5,13),fill=SHADE); d.rectangle((10,7,12,13),fill=SHADE)
        d.line((3,7,3,12),fill=FRAME); d.line((10,7,10,12),fill=FRAME)
        d.polygon(((7,7),(9,9),(7,11),(6,9)),fill=MID); d.point((7,8),fill=WHITE)
    else:
        d.line((2,7,5,7,5,5,7,5),fill=MID)
        d.line((13,8,10,8,10,10,8,10),fill=LIGHT if active else PURPLE)
        d.rectangle((6,6,9,9),fill=INK,outline=LIGHT if active else MID)
        d.rectangle((7,7,8,8),fill=WHITE if active else PURPLE)
        d.line((3,11,5,11),fill=FRAME); d.line((10,4,12,4),fill=FRAME)
    return im

for prefix in ('creative_crafter',):
    crafter=prefix.endswith('crafter')
    save(top(crafter),f'block/{prefix}_top.png')
    save(side('bottom'),f'block/{prefix}_bottom.png')
    save(side('front' if crafter else 'table'),f'block/{prefix}_front.png')
    save(side('side' if crafter else 'table'),f'block/{prefix}_side.png')
    if crafter:
        save(top(True,True),f'block/{prefix}_top_active.png')
        save(side('front',True),f'block/{prefix}_front_active.png')
        save(side('side',True),f'block/{prefix}_side_active.png')

def panel(crafter):
    im=Image.new('RGB',(176,166),INK); d=ImageDraw.Draw(im)
    d.rectangle((1,1,174,164),fill=FRAME); d.rectangle((2,2,173,163),fill=PANEL)
    d.line((2,163,2,2,173,2),fill=MID); d.line((173,3,173,163,3,163),fill=BLACK)
    d.line((4,69,171,69),fill=INK); d.line((4,70,171,70),fill=FRAME)
    for x,y in ((4,4),(169,4),(4,159),(169,159)):
        d.rectangle((x,y,x+2,y+2),fill=PURPLE); d.point((x,y),fill=LIGHT)
    if crafter:
        d.line((8,23,8,58,16,58),fill=PURPLE)
        d.line((162,23,166,23,166,59,158,59),fill=PURPLE)
        for y in (30,39,48): d.line((10,y,13,y),fill=MID)
    else:
        d.line((13,24,8,29,8,50,13,55),fill=PURPLE)
        d.line((158,24,164,30,164,49,158,55),fill=PURPLE)
    def slot(x,y):
        d.rectangle((x-1,y-1,x+16,y+16),fill=INK)
        d.rectangle((x,y,x+16,y+16),fill=MID)
        d.rectangle((x,y,x+15,y+15),fill=SHADE)
        d.line((x,y+15,x,y,x+15,y),fill=BLACK)
    for row in range(3):
        for col in range(3): slot((26 if crafter else 30)+18*col,17+18*row)
    for row in range(3):
        for col in range(9): slot(8+18*col,84+18*row)
    for col in range(9): slot(8+18*col,142)
    x=134 if crafter else 124
    d.rectangle((x-4,31,x+19,54),fill=MID); d.rectangle((x-3,32,x+18,53),fill=BLACK)
    slot(x,35)
    # The crafter's redstone indicator is drawn by the vanilla screen.
    if not crafter:
        d.rectangle((94,40,108,45),fill=LIGHT)
        for i in range(7): d.line((106+i,34+i,106+i,51-i),fill=LIGHT)
    else:
        d.line((87,42,94,42),fill=FRAME); d.line((115,42,123,42),fill=FRAME)
    return im

for crafter in (False,True):
    name='creative_crafter' if crafter else 'creative_crafting_table'
    save(panel(crafter),f'gui/container/{name}.png')
print('Drew crafter textures at 16x16 and GUI backgrounds at 176x166. Restored workbench block faces are preserved.')
