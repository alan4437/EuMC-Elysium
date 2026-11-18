import colorsys,math,struct,zlib
from pathlib import Path
n=256; rows=[]
for y in range(n):
 row=bytearray([0])
 for x in range(n):
  dx=(x+.5-128)/127;dy=(y+.5-128)/127;r=math.hypot(dx,dy)
  rgb=colorsys.hsv_to_rgb((math.atan2(dy,dx)/(2*math.pi))%1,min(1,r),1)
  row.extend([*(round(c*255) for c in rgb),round(max(0,min(1,(1-r)*127))*255)])
 rows.append(row)
def chunk(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d))
p=Path(__file__).resolve().parents[1]/'src/main/resources/assets/discordlink/textures/gui/colour-wheel.png'
p.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',n,n,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows),9))+chunk(b'IEND',b''))
p.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":true,"clamp":true}}')
