package BeatDetection;

import audiowave.FFT;
import util.Peaks;
import util.Profile;


import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * Audio processing class (adapted from PerformanceMatcher).
 */
public class AudioProcessor {


    /**
     * Input data stream for this performance (possibly in compressed format)
     */
    protected AudioInputStream rawInputStream;

    /**
     * Uncompressed version of <code>rawInputStream</code>.
     * In the (normal) case where the input is already PCM data,
     * <code>rawInputStream == pcmInputStream</code>
     */
    protected AudioInputStream pcmInputStream;

    /**
     * Line for audio output (not used, since output is done by AudioPlayer)
     */
    protected SourceDataLine audioOut;

    /**
     * Format of the audio data in <code>pcmInputStream</code>
     */
    protected AudioFormat audioFormat;

    /**
     * Number of channels of audio in <code>audioFormat</code>
     */
    protected int channels;

    /**
     * Sample rate of audio in <code>audioFormat</code>
     */
    protected float sampleRate;

    /**
     * Source of input data.
     * Could be extended to include live input from the sound card.
     */
    protected String audioFileName;

    /**
     * Spacing of audio frames (determines the amount of overlap or skip
     * between frames). This value is expressed in seconds and can be set by
     * the command line option <b>-h hopTime</b>. (Default = 0.020s)
     */
    protected double hopTime;

    /**
     * The approximate size of an FFT frame in seconds, as set by the command
     * line option <b>-f FFTTime</b>. (Default = 0.04644s).  The value is
     * adjusted so that <code>fftSize</code> is always power of 2.
     */
    private double fftTime;

    /**
     * Spacing of audio frames in samples (see <code>hopTime</code>)
     */
    private int hopSize;

    /**
     * The size of an FFT frame in samples (see <code>fftTime</code>)
     */
    private int fftSize;

    /**
     * The number of overlapping frames of audio data which have been read.
     */
    private int frameCount;

    /**
     * RMS amplitude of the current frame.
     */
    private double frameRMS;

    /**
     * Long term average frame energy (in frequency domain representation).
     */
    private double ltAverage;

    /**
     * Audio data is initially read in PCM format into this buffer.
     */
    private byte[] inputBuffer;

    /**
     * Audio data is scaled to the range [0,1] and averaged to one channel and
     * stored in a circular buffer for reuse (if hopTime &lt; fftTime).
     */
    private double[] circBuffer;

    /**
     * The index of the next position to write in the circular buffer.
     */
    private int cbIndex;

    /**
     * The window function for the STFT, currently a Hamming window.
     */
    private double[] window;

    /**
     * The real part of the data for the in-place FFT computation.
     * Since input data is real, this initially contains the input data.
     */
    private double[] reBuffer;

    /**
     * The imaginary part of the data for the in-place FFT computation.
     * Since input data is real, this initially contains zeros.
     */
    private double[] imBuffer;

    /**
     * Phase of the previous frame, for calculating an onset function
     * based on spectral phase deviation.
     */
    private double[] prevPhase;

    /**
     * Phase of the frame before the previous frame, for calculating an
     * onset function based on spectral phase deviation.
     */
    private double[] prevPrevPhase;

    /**
     * Phase deviation onset detection function, indexed by frame.
     */
    protected double[] phaseDeviation;

    /**
     * Spectral flux onset detection function, indexed by frame.
     */
    private double[] spectralFlux;

    /**
     * Spectral flux for different frequency
     */
    protected double[][] freqSpectralFlux;


    /**
     * Normalized Spectral flux for different frequency
     */
    protected double[][] freqSpectralFluxNormalise;

    /**
     * Spectral flux for different frequency bin
     */
    protected double[][] binSpectralFlux;


    /**
     * Normalized Spectral flux for different frequency bin
     */
    protected double[][] binSpectralFluxNormalise;




    /**
     * Onset peak matrix for Frequency Spectral flux
     */
    protected int[][] peakFreqSpectralMatrix;

    /**
     * Onset peak matrix for Frequency bin Spectral flux
     */

    protected int[][] peakbinSpectralMatrix;

    /**
     * Onset peak matrix for Total Spectral flux
     */

    protected int[] peakSpectralFluxTotal;

    /**
     * A mapping function for mapping FFT bins to final frequency bins.
     * The mapping is linear (1-1) until the resolution reaches 2 points per
     * semitone, then logarithmic with a semitone resolution.  e.g. for
     * 44.1kHz sampling rate and fftSize of 2048 (46ms), bin spacing is
     * 21.5Hz, which is mapped linearly for bins 0-34 (0 to 732Hz), and
     * logarithmically for the remaining bins (midi notes 79 to 127, bins 35 to
     * 83), where all energy above note 127 is mapped into the final bin.
     */
    protected int[] freqMap;

    /**
     * The number of entries in <code>freqMap</code>. Note that the length of
     * the array is greater, because its size is not known at creation time.
     */
    protected int freqMapSize;

    /**
     * The magnitude spectrum of the most recent frame.
     * Used for calculating the spectral flux.
     */
    protected double[] prevFrame;

    /**
     * The magnitude spectrum of the current frame.
     */
    protected double[] newFrame;

    /**
     * The magnitude spectra of all frames, used for plotting the spectrogram.
     */
    protected double[][] frames;


    /**
     * The magnitude spectra of all frames, used for plotting the spectrogram based on raw FFT
     */

    protected double[][] rawFrames;

    /**
     * Onset vectors based on normalised FFT
     */


    protected double[][] beatVectors;

    /**
     * Onset vectors based on raw FFT
     */

    protected double[][] rawBeatVectors;
    /**
     * The RMS energy of all frames.
     */
    protected double[] energy;

    /**
     * The estimated onset times from peak-picking the onset detection function(s).
     */
    protected double[] onsets;


    /**
     * The y-coordinates of the onsets for plotting. Only used if doOnsetPlot is true
     */
    protected double[] y2Onsets;


    /**
     * Total number of audio frames if known, or -1 for live or compressed input.
     */
    protected int totalFrames;

    /**
     * Standard input for interactive prompts (for debugging).
     */
    BufferedReader stdIn;


    /**
     * Flag for enabling or disabling debugging output
     */
    public static boolean debug = false;

    /**
     * Flag for plotting onset detection function.
     */
    public static boolean doOnsetPlot = false;

    /**
     * Flag for suppressing all standard output messages except results.
     */
    protected static boolean silent = true;


    /**
     * RMS frame energy below this value results in the frame being set to zero,
     * so that normalisation does not have undesired side-effects.
     */
    public static double silenceThreshold = 0.0004;

    /**
     * For dynamic range compression, this value is added to the log magnitude
     * in each frequency bin and any remaining negative values are then set to zero.
     */
    public static double rangeThreshold = 10;

    /**
     * Determines method of normalisation. Values can be:<ul>
     * <li>0: no normalisation</li>
     * <li>1: normalisation by current frame energy</li>
     * <li>2: normalisation by exponential average of frame energy</li>
     * </ul>
     */
    public static int normaliseMode = 2;

    /**
     * Ratio between rate of sampling the signal energy (for the amplitude envelope) and the hop size
     */
    public static int energyOversampleFactor = 2;

    /**
     * Audio buffer for live input. (Not used yet)
     */
    public static final int liveInputBufferSize = 32768; /* ~195ms buffer @CD */

    /**
     * Maximum file length in seconds. Used for static allocation of arrays.
     */
    public static final int MAX_LENGTH = 3600;    // i.e. 1 hour


    /**
     * Constructor: note that streams are not opened until the input file is set
     * (see <code>setInputFile()</code>).
     */
    public AudioProcessor() {
        cbIndex = 0;
        frameRMS = 0;
        ltAverage = 0;
        frameCount = 0;
        hopSize = 0;
        fftSize = 0;
        hopTime = 0.010;    // DEFAULT, overridden with -h
        fftTime = 0.04644;    // DEFAULT, overridden with -f

        stdIn = new BufferedReader(new InputStreamReader(System.in));
        if (doOnsetPlot) {

        }

    } // constructor

    /**
     * For debugging, outputs information about the AudioProcessor to
     * standard error.
     */
    public void print() {
        System.err.println(this);
    } // print()

    /**
     * For interactive pause - wait for user to hit Enter
     */
    public String readLine() {
        try {
            return stdIn.readLine();
        } catch (Exception e) {
            return null;
        }
    } // readLine()

    /**
     * Gives some basic information about the audio being processed.
     */
    public String toString() {
        return "AudioProcessor\n" +
                String.format("\tFile: %s (%3.1f kHz, %1d channels)\n",
                        audioFileName, sampleRate / 1000, channels) +
                String.format("\tHop / FFT sizes: %5.3f / %5.3f",
                        hopTime, hopTime * fftSize / hopSize);
    } // toString()



    /**
     * Sets up the streams and buffers for live audio input (CD quality).
     * If any Exception is thrown within this method, it is caught, and any
     * opened streams are closed, and <code>pcmInputStream</code> is set to
     * <code>null</code>, indicating that the method did not complete
     * successfully.
     */
    public void setLiveInput() {
        try {
            channels = 2;
            sampleRate = 44100;
            AudioFormat desiredFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16,
                    channels, channels * 2, sampleRate, false);
            TargetDataLine tdl = AudioSystem.getTargetDataLine(desiredFormat);
            tdl.open(desiredFormat, liveInputBufferSize);
            pcmInputStream = new AudioInputStream(tdl);
            audioFormat = pcmInputStream.getFormat();
            init();
            tdl.start();
        } catch (Exception e) {
            e.printStackTrace();
            closeStreams();    // make sure it exits in a consistent state
        }
    } // setLiveInput()

    /**
     * Sets up the streams and buffers for audio file input.
     * If any Exception is thrown within this method, it is caught, and any
     * opened streams are closed, and <code>pcmInputStream</code> is set to
     * <code>null</code>, indicating that the method did not complete
     * successfully.
     *
     * @param fileName The path name of the input audio file.
     */
    public void setInputFile(String fileName) {
        closeStreams();        // release previously allocated resources
        audioFileName = fileName;
        try {
            if (audioFileName == null)
                throw new Exception("No input file specified");
            File audioFile = new File(audioFileName);
            if (!audioFile.isFile())
                throw new FileNotFoundException(
                        "Requested file does not exist: " + audioFileName);
            rawInputStream = AudioSystem.getAudioInputStream(audioFile);
            audioFormat = rawInputStream.getFormat();
            channels = audioFormat.getChannels();
            sampleRate = audioFormat.getSampleRate();
            pcmInputStream = rawInputStream;
            if ((audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) ||
                    (audioFormat.getFrameSize() != channels * 2) ||
                    audioFormat.isBigEndian()) {
                AudioFormat desiredFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16,
                        channels, channels * 2, sampleRate, false);
                pcmInputStream = AudioSystem.getAudioInputStream(desiredFormat,
                        rawInputStream);
                audioFormat = desiredFormat;
            }
            init();
        } catch (Exception e) {
            e.printStackTrace();
            closeStreams();    // make sure it exits in a consistent state
        }
    } // setInputFile()

    /**
     * Allocates memory for arrays, based on parameter settings
     */
    protected void init() {
        hopSize = (int) Math.round(sampleRate * hopTime);
        fftSize = (int) Math.round(Math.pow(2,
                Math.round(Math.log(fftTime * sampleRate) / Math.log(2))));
        makeFreqMap(fftSize, sampleRate);
        int buffSize = hopSize * channels * 2;
        if ((inputBuffer == null) || (inputBuffer.length != buffSize))
            inputBuffer = new byte[buffSize];
        if ((circBuffer == null) || (circBuffer.length != fftSize)) {
            circBuffer = new double[fftSize];
            reBuffer = new double[fftSize];
            imBuffer = new double[fftSize];
            prevPhase = new double[fftSize];
            prevPrevPhase = new double[fftSize];
            prevFrame = new double[fftSize];
            window = FFT.makeWindow(FFT.HAMMING, fftSize, fftSize);
            for (int i = 0; i < fftSize; i++)
                window[i] *= Math.sqrt(fftSize);
        }
        if (pcmInputStream == rawInputStream)
            totalFrames = (int) (pcmInputStream.getFrameLength() / hopSize);
        else
            totalFrames = (int) (MAX_LENGTH / hopTime);
        if ((newFrame == null) || (newFrame.length != freqMapSize)) {
            newFrame = new double[freqMapSize];
            frames = new double[totalFrames][freqMapSize];
            rawFrames = new double[totalFrames][freqMapSize];


        } else if (frames.length != totalFrames) {
            frames = new double[totalFrames][freqMapSize];
            rawFrames = new double[totalFrames][freqMapSize];

        }


        energy = new double[totalFrames * energyOversampleFactor];
        phaseDeviation = new double[totalFrames];
        spectralFlux = new double[totalFrames];
        freqSpectralFlux = new double[fftSize / 2 + 1][totalFrames];
        freqSpectralFluxNormalise = new double[fftSize / 2 + 1][totalFrames];
        binSpectralFlux = new double[freqMapSize][totalFrames];
        binSpectralFluxNormalise = new double[freqMapSize][totalFrames];
        frameCount = 0;
        cbIndex = 0;
        frameRMS = 0;
        ltAverage = 0;

    } // init()

    /**
     * Closes the input stream(s) associated with this object.
     */
    public void closeStreams() {
        if (pcmInputStream != null) {
            try {
                pcmInputStream.close();
                if (pcmInputStream != rawInputStream)
                    rawInputStream.close();
                if (audioOut != null) {
                    audioOut.drain();
                    audioOut.close();
                }
            } catch (Exception e) {
            }
            pcmInputStream = null;
            audioOut = null;
        }
    } // closeStreams()

    /**
     * Creates a map of FFT frequency bins to comparison bins.
     * Where the spacing of FFT bins is less than 0.5 semitones, the mapping is
     * one to one. Where the spacing is greater than 0.5 semitones, the FFT
     * energy is mapped into semitone-wide bins. No scaling is performed; that
     * is the energy is summed into the comparison bins. See also
     * processFrame()
     */
    protected void makeFreqMap(int fftSize, float sampleRate) {
        freqMap = new int[fftSize / 2 + 1];
        double binWidth = sampleRate / fftSize;
        int crossoverBin = (int) (2 / (Math.pow(2, 1 / 12.0) - 1));
        int crossoverMidi = (int) Math.round(Math.log(crossoverBin * binWidth / 440) /
                Math.log(2) * 12 + 69);
        // freq = 440 * Math.pow(2, (midi-69)/12.0) / binWidth;
        int i = 0;
        while (i <= crossoverBin)
            freqMap[i++] = i;
        while (i <= fftSize / 2) {
            double midi = Math.log(i * binWidth / 440) / Math.log(2) * 12 + 69;
            if (midi > 127)
                midi = 127;
            freqMap[i++] = crossoverBin + (int) Math.round(midi) - crossoverMidi;
        }
        freqMapSize = freqMap[i - 1] + 1;
    } // makeFreqMap()

    /**
     * Calculates the weighted phase deviation onset detection function.
     * Not used.
     * TODO: Test the change to WPD fn
     */
    protected void weightedPhaseDeviation() {
        if (frameCount < 2)
            phaseDeviation[frameCount] = 0;
        else {
            for (int i = 0; i < fftSize; i++) {
                double pd = imBuffer[i] - 2 * prevPhase[i] + prevPrevPhase[i];
                double pd1 = Math.abs(Math.IEEEremainder(pd, 2 * Math.PI));
                phaseDeviation[frameCount] += pd1 * reBuffer[i];
                // System.err.printf("%7.3f   %7.3f\n", pd/Math.PI, pd1/Math.PI);
            }
        }
        phaseDeviation[frameCount] /= fftSize * Math.PI;
        double[] tmp = prevPrevPhase;
        prevPrevPhase = prevPhase;
        prevPhase = imBuffer;
        imBuffer = tmp;
    } // weightedPhaseDeviation()

    /**
     * Reads a frame of input data, averages the channels to mono, scales
     * to a maximum possible absolute value of 1, and stores the audio data
     * in a circular input buffer.
     *
     * @return true if a frame (or part of a frame, if it is the final frame)
     * is read. If a complete frame cannot be read, the InputStream is set
     * to null.
     */
    public boolean getFrame() {
        if (pcmInputStream == null)
            return false;
        try {
            int bytesRead = (int) pcmInputStream.read(inputBuffer);
            if ((audioOut != null) && (bytesRead > 0))
                if (audioOut.write(inputBuffer, 0, bytesRead) != bytesRead)
                    System.err.println("Error writing to audio device");
            if (bytesRead < inputBuffer.length) {
                if (!silent)
                    System.err.println("End of input: " + audioFileName);
                closeStreams();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeStreams();
            return false;
        }
        frameRMS = 0;
        double sample;
        switch (channels) {
            case 1:
                for (int i = 0; i < inputBuffer.length; i += 2) {
                    sample = ((inputBuffer[i + 1] << 8) |
                            (inputBuffer[i] & 0xff)) / 32768.0;
                    frameRMS += sample * sample;
                    circBuffer[cbIndex++] = sample;
                    if (cbIndex == fftSize)
                        cbIndex = 0;
                }
                break;
            case 2: // saves ~0.1% of RT (total input overhead ~0.4%) :)
                for (int i = 0; i < inputBuffer.length; i += 4) {
                    sample = (((inputBuffer[i + 1] << 8) | (inputBuffer[i] & 0xff)) +
                            ((inputBuffer[i + 3] << 8) | (inputBuffer[i + 2] & 0xff)))
                            / 65536.0;
                    frameRMS += sample * sample;
                    circBuffer[cbIndex++] = sample;
                    if (cbIndex == fftSize)
                        cbIndex = 0;
                }
                break;
            default:
                for (int i = 0; i < inputBuffer.length; ) {
                    sample = 0;
                    for (int j = 0; j < channels; j++, i += 2)
                        sample += (inputBuffer[i + 1] << 8) | (inputBuffer[i] & 0xff);
                    sample /= 32768.0 * channels;
                    frameRMS += sample * sample;
                    circBuffer[cbIndex++] = sample;
                    if (cbIndex == fftSize)
                        cbIndex = 0;
                }
        }
        frameRMS = Math.sqrt(frameRMS / inputBuffer.length * 2 * channels);
        return true;
    } // getFrame()

    /**
     * Processes a frame of audio data by first computing the STFT with a
     * Hamming window, then mapping the frequency bins into a part-linear
     * part-logarithmic array, then computing the spectral flux
     * then (optionally) normalising and calculating onsets.
     */
    protected void processFrame() {
        if (getFrame()) {
            for (int i = 0; i < fftSize; i++) {
                reBuffer[i] = window[i] * circBuffer[cbIndex];
                if (++cbIndex == fftSize)
                    cbIndex = 0;
            }
            Arrays.fill(imBuffer, 0);
            FFT.magnitudePhaseFFT(reBuffer, imBuffer);
            Arrays.fill(newFrame, 0);
            double flux = 0;
            for (int i = 0; i <= fftSize / 2; i++) {
                if (reBuffer[i] > prevFrame[i]) {
                    freqSpectralFlux[i][frameCount] = reBuffer[i] - prevFrame[i];
                    binSpectralFlux[freqMap[i]][frameCount] += reBuffer[i] - prevFrame[i];
                    flux += reBuffer[i] - prevFrame[i];
                }
                newFrame[freqMap[i]] += reBuffer[i];
            }
            //TODO: implement frequency bin flux
            spectralFlux[frameCount] = flux;
            for (int i = 0; i < freqMapSize; i++) {
                frames[frameCount][i] = newFrame[i];
                rawFrames[frameCount][i] = newFrame[i];
            }

            int index = cbIndex - (fftSize - hopSize);
            if (index < 0)
                index += fftSize;
            int sz = (fftSize - hopSize) / energyOversampleFactor;
            for (int j = 0; j < energyOversampleFactor; j++) {
                double newEnergy = 0;
                for (int i = 0; i < sz; i++) {
                    newEnergy += circBuffer[index] * circBuffer[index];
                    if (++index == fftSize)
                        index = 0;
                }
                energy[frameCount * energyOversampleFactor + j] =
                        newEnergy / sz <= 1e-6 ? 0 : Math.log(newEnergy / sz) + 13.816;
            }
            double decay = frameCount >= 200 ? 0.99 :
                    (frameCount < 100 ? 0 : (frameCount - 100) / 100.0);
            if (ltAverage == 0)
                ltAverage = frameRMS;
            else
                ltAverage = ltAverage * decay + frameRMS * (1.0 - decay);
            if (frameRMS <= silenceThreshold)
                for (int i = 0; i < freqMapSize; i++)
                    frames[frameCount][i] = 0;
            else {
                if (normaliseMode == 1)
                    for (int i = 0; i < freqMapSize; i++)
                        frames[frameCount][i] /= frameRMS;
                else if (normaliseMode == 2)
                    for (int i = 0; i < freqMapSize; i++)
                        frames[frameCount][i] /= ltAverage;
                for (int i = 0; i < freqMapSize; i++) {
                    frames[frameCount][i] = Math.log(frames[frameCount][i]) + rangeThreshold;
                    if (frames[frameCount][i] < 0)
                        frames[frameCount][i] = 0;
                }
            }
            double[] tmp = prevFrame;
            prevFrame = reBuffer;
            reBuffer = tmp;
            frameCount++;
            if ((frameCount % 100) == 0) {
                if (!silent) {
                    System.err.printf("Progress: %1d %5.3f %5.3f\n",
                            frameCount, frameRMS, ltAverage);
                    Profile.report();
                }

            }
        }
    } // processFrame()


    /**
     * Processes a complete file of audio data.
     */
    public void processFile() {
        while (pcmInputStream != null) {
            // Profile.start(0);
            processFrame();
            // Profile.log(0);
            if (Thread.currentThread().isInterrupted()) {
                System.err.println("info: INTERRUPTED in processFile()");
                return;
            }
        }


        double p1 = 0.35;
        double p2 = 0.84;

        Peaks.normalise(spectralFlux);
        computeNomaliseFreqSpectralFlux();
        computeNomaliseBinSpectralFlux();
        findOnsets(p1, p2);
        peakFreqSpectralMatrix = findPeakMatrix(freqSpectralFluxNormalise);
        peakbinSpectralMatrix = findPeakMatrix(binSpectralFluxNormalise);
        peakSpectralFluxTotal = Peaks.findPeaks(spectralFlux, (int) Math.round(0.06 / hopTime),
                0.35, 0.84, true, 100);
         if (doOnsetPlot) {

        }
        if (debug) {
            System.err.printf("Onsets: %d\nContinue? ", onsets.length);
            readLine();
        }
    } // processFile()

    void computeNomaliseFreqSpectralFlux() {
        for (int freqcounter = 0; freqcounter < freqSpectralFluxNormalise.length; freqcounter++) {
            for (int framecounter = 0; framecounter < freqSpectralFluxNormalise[freqcounter].length; framecounter++) {
                freqSpectralFluxNormalise[freqcounter][framecounter] = freqSpectralFlux[freqcounter][framecounter];
            }
            Peaks.normalise(freqSpectralFluxNormalise[freqcounter]);
        }
    }

    void computeNomaliseBinSpectralFlux() {
        for (int binindex = 0; binindex < binSpectralFluxNormalise.length; binindex++) {
            for (int framecounter = 0; framecounter < binSpectralFluxNormalise[binindex].length; framecounter++) {
                binSpectralFluxNormalise[binindex][framecounter] = binSpectralFlux[binindex][framecounter];
            }
            Peaks.normalise(binSpectralFluxNormalise[binindex]);
        }
    }

    public int[][] findPeakMatrix(double[][] data) {
        int[][] matrix = new int[data.length][];
        for (int index = 0; index < matrix.length; index++) {
            matrix[index] = Peaks.findPeaks(data[index], (int) Math.round(0.06 / hopTime),
                    0.35, 0.84, true, index + 1);
        }
        return matrix;
    }


    public void findOnsets(double p1, double p2) {
        LinkedList<Integer> peaks = Peaks.findPeaks(spectralFlux, (int) Math.round(0.06 / hopTime), p1, p2, true);
        onsets = new double[peaks.size()];
        y2Onsets = new double[onsets.length];
        Iterator<Integer> it = peaks.iterator();
        double minSalience = Peaks.min(spectralFlux);
        for (int i = 0; i < onsets.length; i++) {
            int index = it.next();
            onsets[i] = index * hopTime;
            y2Onsets[i] = spectralFlux[index];
        }
    }

    /**
     * Reads a text file containing a list of whitespace-separated feature values.
     * Created for paper submitted to ICASSP'07.
     *
     * @param fileName File containing the data
     * @return An array containing the feature values
     */
    public static double[] getFeatures(String fileName) {
        ArrayList<Double> l = new ArrayList<Double>();
        try {
            BufferedReader b = new BufferedReader(new FileReader(fileName));
            while (true) {
                String s = b.readLine();
                if (s == null)
                    break;
                int start = 0;
                while (start < s.length()) {
                    int len = s.substring(start).indexOf(' ');
                    String t = null;
                    if (len < 0)
                        t = s.substring(start);
                    else if (len > 0) {
                        t = s.substring(start, start + len);
                    }
                    if (t != null)
                        try {
                            l.add(Double.parseDouble(t));
                        } catch (NumberFormatException e) {
                            System.err.println(e);
                            if (l.size() == 0)
                                l.add(new Double(0));
                            else
                                l.add(new Double(l.get(l.size() - 1)));
                        }
                    start += len + 1;
                    if (len < 0)
                        break;
                }
            }
            double[] features = new double[l.size()];
            Iterator<Double> it = l.iterator();
            for (int i = 0; it.hasNext(); i++)
                features[i] = it.next().doubleValue();
            return features;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    } // getFeatures()

    /**
     * Reads a file of feature values, treated as an onset detection function,
     * and finds peaks, which are stored in <code>onsetList</code> and <code>onsets</code>.
     *
     * @param fileName The file of feature values
     * @param hopTime  The spacing of feature values in time
     */
    public void processFeatures(String fileName, double hopTime) {
        double hop = hopTime;
        double[] features = getFeatures(fileName);
        Peaks.normalise(features);
        LinkedList<Integer> peaks = Peaks.findPeaks(features, (int) Math.round(0.06 / hop), 0.35, 0.84, true);
        onsets = new double[peaks.size()];
        double[] y2 = new double[onsets.length];
        Iterator<Integer> it = peaks.iterator();
        //onsetList = new EventList();
        double minSalience = Peaks.min(features);
        for (int i = 0; i < onsets.length; i++) {
            int index = it.next();
            onsets[i] = index * hop;
            y2[i] = features[index];

        }
    } // processFeatures()

    public double[][] updateFrameVectors() {
        beatVectors = new double[onsets.length][frames[0].length];
        for (int onsetIndex = 0; onsetIndex < beatVectors.length; onsetIndex++) {
            for (int binIndex = 0; binIndex < beatVectors[onsetIndex].length; binIndex++) {
                for (int samplecounter = 0; samplecounter < 15; samplecounter++) {
                    beatVectors[onsetIndex][binIndex] += frames[((int) (onsets[onsetIndex] / hopTime)) + samplecounter][binIndex];
                }
                beatVectors[onsetIndex][binIndex] /= 15;
            }
        }
        return beatVectors;
    }

    public double[][] updaterawFrameVectors() {
        rawBeatVectors = new double[onsets.length][rawFrames[0].length];
        for (int onsetIndex = 0; onsetIndex < rawBeatVectors.length; onsetIndex++) {
            for (int binIndex = 0; binIndex < rawBeatVectors[onsetIndex].length; binIndex++) {
                for (int samplecounter = 0; samplecounter < 15; samplecounter++) {
                    rawBeatVectors[onsetIndex][binIndex] += rawFrames[((int) (onsets[onsetIndex] / hopTime)) + samplecounter][binIndex];
                }
                rawBeatVectors[onsetIndex][binIndex] /= 15;
            }
        }
        return rawBeatVectors;
    }

    /** Copies output of audio processing to the display panel. */


} // class AudioProcessor
