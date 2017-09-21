
from numpy.fft import fft
from numpy import array
import math
import matplotlib
matplotlib.use('TkAgg')
import matplotlib.pyplot as plt 

list  = []
for i in range(512):
	list.append(math.sin(math.pi/6 + i*math.pi/2) + math.sin(i*math.pi/4) +math.sin(i*math.pi/1.1))
samples = array(list)

amplitutes = abs(fft(samples))
print amplitutes

fig1  = plt.figure(1)
fig2 = plt.figure(2)

ax1 = fig1.add_subplot(111)
ax1.plot(samples)

ax2 = fig2.add_subplot(111)
ax2.plot(amplitutes)

plt.show()
#raw_input()



#print (' '.join("%5.3f" % abs(f) for f in fft(a)))
