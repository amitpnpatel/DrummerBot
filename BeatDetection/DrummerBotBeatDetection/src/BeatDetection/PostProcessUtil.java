package BeatDetection;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by amitp on 3/23/17.
 */
public class PostProcessUtil {
    public static void printBeatFFT(double[] onSets, double[] energy, double energyComputationTime, double[][] ffts) {

        for (int counter = 0; counter < onSets.length; counter++) {
            System.out.print("Beat Number:" + (counter + 1) + " ,Time: " + onSets[counter] + " ,Amplitude: " + energy[(int) (onSets[counter] / energyComputationTime) + 1]);
            for (int innerCounter = 0; innerCounter < ffts[(int) (onSets[counter] / energyComputationTime) / 2].length; innerCounter++) {
                System.out.print(" " + ffts[(int) (onSets[counter] / energyComputationTime) / 2][innerCounter]);

            }
            System.out.println();
        }
    }
    public static double[][] getOnsetSpectral(double[][] binspectra, double[] onset, double hoptime) {
        double[][] result = new double[onset.length][binspectra.length];
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[i].length; j++) {
                result[i][j] = binspectra[j][(int) (onset[i] / hoptime)];

            }
            System.out.println(onset[i]);
        }
        return result;
    }
    public static boolean writeArrayToTextFile(double [] data){
        BufferedWriter bw = null;
        FileWriter fw = null;
        String FILENAME = "beats.txt";
        try {


            fw = new FileWriter(FILENAME);
            bw = new BufferedWriter(fw);
            for(int index=0;index<data.length;index++){
                bw.write(""+data[index]+"\n");
            }

            System.out.println("Done");

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }
        return true;

    }

}
