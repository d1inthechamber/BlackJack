"""Original procedural cartoon vocal effects and a mellow Green Room instrumental loop."""
from pathlib import Path
import numpy as np
from scipy import signal
from scipy.io import wavfile
import subprocess
out=Path('app/src/main/res/raw');out.mkdir(parents=True,exist_ok=True)
sr=22050;rng=np.random.default_rng(541)
def vowel(duration,pitch,formants,breath=.04):
 t=np.arange(int(sr*duration))/sr
 f=pitch*(1+.022*np.sin(2*np.pi*5.4*t))*(1-.20*t/duration)
 phase=np.cumsum(f)/sr
 source=signal.sawtooth(2*np.pi*phase,.72)*.65+rng.normal(0,breath,len(t))
 y=np.zeros_like(t)
 for hz,band,gain in formants:
  b,a=signal.iirpeak(hz, hz/band,fs=sr);y+=signal.lfilter(b,a,source)*gain
 env=np.sin(np.pi*np.clip(t/duration,0,1))**.65
 y*=env
 return y/(np.max(np.abs(y))+1e-6)
def save(name,x):
 x=np.tanh(x*1.1)*.65
 # fade joins and leave headroom
 x[:min(220,len(x))]*=np.linspace(0,1,min(220,len(x)));x[-min(500,len(x)):]*=np.linspace(1,0,min(500,len(x)))
 wavfile.write('/tmp/'+name+'.wav',sr,(x*32767).astype(np.int16))
 subprocess.run(['ffmpeg','-y','-loglevel','error','-i','/tmp/'+name+'.wav','-c:a','libvorbis','-q:a','4',str(out/(name+'.ogg'))],check=True)
save('dealer_grunt',vowel(.40,103,[(370,110,1),(1050,150,.55),(2300,240,.15)],.10))
save('dealer_groan',vowel(1.10,98,[(490,120,1),(920,170,.6),(2450,270,.12)],.07))
parts=[]
for i,d in enumerate([.24,.22,.27,.29]):
 parts.extend([vowel(d,132-i*7,[(760,140,1),(1230,170,.7),(2600,300,.15)],.1)*(.9-i*.09),np.zeros(int(sr*.095))])
save('dealer_laugh',np.concatenate(parts))
# 16-bar, 80 bpm minor dub/lounge instrumental; synthesized, no sampled music.
beat=60/80;duration=16*4*beat;t=np.arange(int(sr*duration))/sr;mix=np.zeros_like(t)
def note(start,length,midi,amp,kind='key'):
 i=int(start*sr);n=min(int(length*sr),len(mix)-i)
 if n<=0:return
 u=np.arange(n)/sr;hz=440*2**((midi-69)/12)
 if kind=='bass':v=np.sin(2*np.pi*hz*u)+.2*np.sin(4*np.pi*hz*u);env=np.minimum(u/.02,1)*np.exp(-u*3)
 else:v=np.sin(2*np.pi*hz*u)+.25*np.sin(2*np.pi*hz*2*u)*np.exp(-u*8);env=np.minimum(u/.009,1)*np.exp(-u*7)
 mix[i:i+n]+=v*env*amp
for bar in range(16):
 root=[45,43,41,40][bar%4]
 for b in [0,1.5,2.75]:note((bar*4+b)*beat,.65,root,.18,'bass')
 for b in [.5,1.5,2.5,3.5]:
  for step in [12,15,19]:note((bar*4+b)*beat,.28,root+step,.08)
 for b in range(4):
  start=int((bar*4+b)*beat*sr);n=int(.13*sr);u=np.arange(n)/sr
  drum=np.sin(2*np.pi*(60*u+15*(1-np.exp(-u*40))/40))*np.exp(-u*32)*.25
  if b%2:drum+=rng.normal(0,.1,n)*np.exp(-u*50)
  mix[start:start+n]+=drum
save('music_green',mix)
print('Created 3 vocal effects and a 48-second instrumental loop')
