import numpy as np
import math


class Audioprocessor:
    silenceThreshold = 0.0004;
    rangeThreshold = 10;
    normaliseMode = 2;
    energyOversampleFactor = 2;

    def __init__(self, data, sampleRate):
        # varible for int array for audio data
        self.audioData = data;
        self.sampleRate = sampleRate;
        self.hopTime = 0.01;
        self.fftTime = 0.04644;
        self.hopSize = int(self.sampleRate * self.hopTime);
        self.fftSize = int(2 ** (int(math.log(self.fftTime * self.sampleRate) / math.log(2))));
        self.frameCount = 0;
        self.frameRMS = 0;
        # Long term average frame energy
        self.ltAverage = 0;
        self.circBuffer;
        self.cbIndex = 0;
        self.window;
        self.reBuffer;
        self.imBuffer;
        self.prevPhase;
        self.prevPrevPhase;
        self.phaseDeviation;
        self.spectralFlux;
        self.freqMap
        self.freqMapSize;
        self.prevFrame;
        self.newFrame;
        self.frames;
        self.beatVectors;
        self.energy;
        self.onsets;
        self.y2Onsets;
        self.totalFrames = int(len(self.audioData)/(self.hopSize));
        self.init_variables()


    def set_input_data(self, data):
        self.audioData = data;

    def make_freq_map(self):
        self.freqMap = np.zeros(self.fftSize / 2 + 1)
        binWidth = self.sampleRate / self.fftSize
        crossOverBin = int(2 / ((2 ** (1 / 12.0)) - 1))
        crossoverMidi = int(math.round(math.log(crossOverBin * binWidth / 440)/math.log(2) * 12 + 69))
        i = 0

        while i<= crossOverBin:
            self.freqMap[ i ] = i
            i += 1

        while i<=self.fftSize/2:
            midi = math.log(i * binWidth / 440) / math.log(2) * 12 + 69
            if midi  > 127:
                midi = 127
            self.freqMap[i] = crossOverBin + int(math.round(midi) - crossoverMidi)
            i+=1

        self.freqMapSize  =  self.freqMap[i-1] + 1

    def over_threshold(self,data, index, width, threshold, is_relative, average):
        pre = 3
        post = 1
        if data[index] < average:
            return False

        if is_relative:
            i_start = index - pre * width
            if i_start < 0:
                i_start = 0
            i_stop = index + post * width

            if i_stop > len(data):
                i_stop = len(data)

            sum = 0
            count = i_stop - i_start
            while i_start < i_stop:
                sum+=data[i_start]
                i_start+=1

            return (data[index] > sum / count + threshold)
        else:
            return (data[index] > threshold)



    def find_peaks(self, data, width, thrshold, decay_rate, is_relative):
        peaks = []
        maxp =0
        mid =0
        end = len(data)
        average = data[0]
        while(mid < end):
            average = decay_rate * average + (1 - decay_rate) * data[mid]
            if average < data[mid]:
                average = data[mid]
            i = mid - width
            if i< 0:
                i= 0
            stop = mid + width + 1
            if stop > len(data):
                stop = len(data)
            maxp = i
            i+=1
            for i in range(stop):
                if data[i] > data[maxp]:
                    maxp = i
            if maxp == mid:
                if self.over_threshold(data, maxp, width, thrshold, is_relative, average):
                    peaks.add(maxp)

            mid+=1

        return peaks


    def find_on_sets(self, p1,p2):
        peaks = self.find_peaks(self.spectralFlux, int(0.06 / self.hopTime), p1 ,p2, True)
        self.onsets = np.zeros(len(peaks))
        self.y2Onsets = np.zeros(len(self.onsets))

        for i in range(len(self.onsets)):
            self.onsets[i] = peaks[i] * self.hopTime
            self.y2Onsets[i] = self.spectralFlux[peaks[i]]


        return True




    def process_input_data(self):
        for i in xrange(self.totalFrames):
            self.processFrame()

        p1 = 0.35;
        p2 = 0.84;
        self.normalise_array(self.spectralFlux)
        self.normalise_array(self.freqSpectralFlux)
        self.normalise_array(self.binSpectralFlux)
        self.find_on_sets(p1, p2)
        #TO DO calculate all flux matrices




    def normalise_array(self,data):
        sx=0.0;
        sxx=0.0;
        for i in xrange(len(data)):
            sx+=data[i]
            sxx+=data[i]*data[i]
        mean=sx/len(data)
        sd=math.sqrt((sxx-sx*mean)/len(data))
        if(sd==0):
            sd=1
        for i in xrange(len(data)):
            data[i]=(data[i]-mean)/sd;

        return True

    def init_variables(self):
        self.make_freq_map()
        self.circBuffer = np.zeros(self.fftSize,float)
        self.reBuffer = np.zeros(self.fftSize,float)
        self.imBuffer = np.zeros(self.fftSize,float)
        self.prevPhase = np.zeros(self.fftSize,float)
        self.prevPrevPhase = np.zeros(self.fftSize,float)
        self.prevFrame = np.zeros(self.fftSize,float)
        self.window =  self.createHammingWindow(self.fftSize, self.fftSize)

        for i in xrange(0,self.fftSize):
            self.window[i] *= math.sqrt(self.fftSize)

        self.newFrame = np.zeros(self.freqMapSize,float)
        self.frames = np.zeros((self.totalFrames, self.freqMapSize), dtype=float)

        self.energy = np.zeros(self.totalFrames * self.energyOversampleFactor)
        self.phaseDeviation = np.zeros(self.totalFrames, dtype=float)
        self.spectralFlux = np.zeros(self.totalFrames, dtype=float)
        self.freqSpectralFlux = np.zeros((self.fftSize/2 + 1, self.totalFrames), dtype=float)

        self.freqSpectralFluxNormalise = np.zeros((self.fftSize/2 + 1, self.totalFrames), dtype=float)
        self.binSpectralFlux = np.zeros((self.freqMapSize, self.totalFrames), dtype=float)

        self.binSpectralFluxNormalise =np.zeros((self.freqMapSize, self.totalFrames), dtype=float)
        self.frameCount = 0
        self.cbIndex = 0
        self.frameRMS = 0
        self.ltAverage = 0
        self.progressCallback = None


    def fill_array_with_zeros(self, array):
        for i in xrange(len(array)):
            array[i] = 0
    def processFrame(self):
         self.updateFrame()
         for i in range(self.fftSize):
             self.reBuffer[i] = self.window[i] * self.circBuffer[self.cbIndex]
             self.cbIndex+=1
             if self.cbIndex == self.fftSize:
                 self.cbIndex = 0

         self.imBuffer = self.fill_array_with_zeros(self.imBuffer)
         self.magnitude_phase_fft(self.reBuffer, self.imBuffer)
         self.newFrame = self.fill_array_with_zeros(self.newFrame)
         flux = 0

         for i in xrange(self.fftSize/2 + 1):
             if (self.reBuffer > self.prevFrame[i]):
                 self.freqSpectralFlux[i][self.frameCount] = self.reBuffer - self.prevFrame[i]
                 self.binSpectralFlux[self.freqMap[i]][self.frameCount] +=self.reBuffer[i] - self.prevFrame[i]
                 flux +=self.reBuffer[i] - self.prevFrame[i]

             self.newFrame[self.freqMap[i]] +=self.reBuffer[i]
             self.spectralFlux[self.frameCount] = flux
             for i in range(self.freqMapSize):
                 self.frames[self.frameCount][i] = self.newFrame[i]
             index = self.cbIndex - (self.fftSize - self.hopSize)
             if index < 0:
                 index+=self.fftSize

             sz = (self.fftSize - self.hopSize)/self.energyOversampleFactor
             for j in xrange(self.energyOversampleFactor):
                new_energy = 0
                for i in range(sz):
                    new_energy +=self.circBuffer[index] * self.circBuffer[index]
                    index+=1
                    if index == self.fftSize:
                        index = 0

                if (new_energy/sz <=1e-6):
                    self.energy[self.frameCount * self.energyOversampleFactor + j] = 0
                else:
                    self.energy[self.frameCount * self.energyOversampleFactor + j] = math.log(new_energy/sz) + 13.816

             decay = 0
             if self.frameCount >=200:
                decay  = 0.99
             elif self.frameCount < 100:
                decay = 0
             else:
                decay = (self.frameCount - 100)/100.0

             if self.ltAverage ==0:
                 self.ltAverage = self.frameRMS
             else:
                 self.ltAverage = self.ltAverage * decay + self.frameRMS * (1.0 * decay)

             if (self.frameRMS <= self.silenceThreshold):
                 for i in range(self.freqMapSize):
                     self.frames[self.frameCount][i] = 0
             else:
                 for i in range(self.freqMapSize):
                     self.frames[self.frameCount][i] /=self.ltAverage

             tmp = self.prevFrame
             self.prevFrame = self.reBuffer
             self.reBuffer  =tmp
             self.frameCount+=1

         return True





             #AudioProcessor 629

    def rint(num):
        """Rounds toward the even number if equidistant"""
        return round(num + (num % 2 - 1 if (num % 1 == 0.5) else 0))


    def magnitude_phase_fft(self, real, imaginary, direction = -1):
        n = len(real)
        bits = int(self.rint(math.log(n)/math.log((2))))
        local_n = 0.0
        j = 0
        for i in range(0,n-1):
            if (i<j):
                temp = real[j]
                real[j] = real[i]
                real[i] = temp
                temp = imaginary[i]
                imaginary[i] = imaginary[j]
                imaginary[j] = temp

            k = n / 2
            while ((k>=1) & (k-1 < j)):
                j = j- k
                k = k/2

            j = j + k

        for m in range(1,bits+1):
            local_n = 1 << m
            wjk_r = 1
            wjk_i = 0
            theta = 2 * math.pi / local_n
            wjk_r = math.cos(theta)
            wjk_i = direction * math.sin(theta)
            nby_2 = local_n/2
            for j in range(0, nby_2):
                for k in range(j,n):
                    id = k + nby_2
                    tempr = wjk_r * real[id] - wjk_i * imaginary[id]
                    tempi = wjk_r * imaginary[id] + wjk_i * real[id]
                    real[id] = real[k] - tempr
                    imaginary[id] = imaginary[k] - tempi
                    real[k]+=tempr
                    imaginary[k] +=tempi
                wtemp = wjk_r
                wjk_r = wjk_r * wjk_r - wjk_i* wjk_i
                wjk_i = wjk_r * wjk_i + wjk_i * wtemp

        for i in range(len(real)):
            real[i] = math.sqrt(real[i])











    def updateFrame(self):
        current_hop_sample = np.zeros(self.hopeSize, dtype=int)

        for i in xrange(len(current_hop_sample)):
            current_hop_sample[i] = self.audData[self.frameCount * self.hopeSize + i]
            self.frameRMS +=current_hop_sample[i] * current_hop_sample[i]
            self.circBuffer[self.cbIndex] = current_hop_sample[i]
            self.cbIndex+=1
            if self.cbIndex == self.fftSize:
                self.cbIndex = 0

        self.frameRMS = math.sqrt(self.frameRMS/len(current_hop_sample))
        return True






    def createHammingWindow(self, size, support):
        data = np.empty(size, dtype=float)
        if support > size :
            support = size

        start = (len(data) - size) / 2;
        stop = (len(data) + size) / 2;
        scale = 1.0 / float(size) / 0.54;
        factor = (2 * math.pi) / float(size);
        i = 0
        while start < stop:
            data[i] = scale * (25.0 / 46.0 - 21.0 / 46.0 * math.cos(factor * i))
            i+=1
        return data



