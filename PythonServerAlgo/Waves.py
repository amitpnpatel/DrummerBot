import matplotlib
matplotlib.use('TkAgg')
import matplotlib.pyplot as plt 
from pylab import*
from scipy.io import wavfile
import numpy as np



matplotlib.pyplot.rcParams['agg.path.chunksize'] = 20000	

text_file = open("Output.txt","w")
sampleRate, samples = wavfile.read('Drum1_Snare.wav')
sample=samples

print sampleRate

i=1
temporary = []

for counter in range(len(sample)):
	#s1[i]=float(s1[i]/1000.0)
	temp = sample[counter]/100
	temporary.append(temp*temp)


sample_square = np.asarray(temporary)

 

'''li = []
sum=0
j=1

for i in range(len(s1)):
	if j%1000==0:
		li.append(sum)
		sum=0
	else:
		j+=1
		sum+=s1[i]

s1=np.asarray(li)			

print s1
'''
final_sample = []
sum = 0

for counter in range(len(sample_square)):
	
	sum+=sample_square[counter]
	
	if counter%999 == 0:
		final_sample.append(sum)
		sum = 0
	
final_sample = np.asarray(final_sample)


plt.plot(final_sample)
plt.show(final_sample.all())
savefig('Drum1_Snare.png')


'''i=1
for element in s1:
	text_file.write("Sample no:"+str(i)+' '+ str(element)+"\n")
	i+=1
'''

text_file.close()

while 1:
	i =1

