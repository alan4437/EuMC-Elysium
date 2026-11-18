import math,struct,zlib,json
from pathlib import Path
out=Path(__file__).resolve().parents[1]/'src/main/resources/assets/discordlink/textures/gui'
out.mkdir(parents=True, exist_ok=True)
def png(name,inside):
 n=128;samples=4;rows=[]
 for y in range(n):
  row=bytearray([0])
  for x in range(n):
   coverage=sum(inside(x+(sx+.5)/samples,y+(sy+.5)/samples) for sy in range(samples) for sx in range(samples))
   row.extend((255,255,255,round(255*coverage/(samples*samples))))
  rows.append(row)
 def chunk(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d))
 (out/(name+'.png')).write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',n,n,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows),9))+chunk(b'IEND',b''))
 (out/(name+'.png.mcmeta')).write_text(json.dumps({'texture':{'blur':True,'clamp':True}}))
def rounded(x,y):
 qx=max(abs(x-64)-26,0);qy=max(abs(y-64)-26,0)
 return qx*qx+qy*qy <=22*22
png('circle',lambda x,y:(x-64)**2+(y-64)**2<=64**2)
png('dice',lambda x,y:rounded(x,y) and all((x-a)**2+(y-b)**2>7**2 for a,b in [(41,41),(87,41),(64,64),(41,87),(87,87)]))
def search(x,y):
 circle=abs(math.hypot(x-49,y-49)-29)<=4
 t=max(0,min(1,((x-72)*35+(y-72)*35)/(2*35**2)))
 return circle or (x-72-t*35)**2+(y-72-t*35)**2 <=4**2
png('search',search)

# Smooth cached halo for nine-slice glass shadows and switch glow.
def halo(x,y):
 qx=abs(x-64)-16; qy=abs(y-64)-16
 distance=math.hypot(max(qx,0),max(qy,0))+min(max(qx,qy),0)-24
 return .8*math.exp(-max(distance,0)**2/(2*9**2))
png('halo',halo)
