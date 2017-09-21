package BeatDetection;

import VectorGroupingAlgorithm.Group;
import VectorGroupingAlgorithm.ThresholdAlgorithm;
import VectorGroupingAlgorithm.Vector;
import Kmean.Point;
import util.LineChartPolt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by amitp on 1/30/17.
 */

public class BeatDetection {

    public static void main(String[] args) {
        AudioProcessor audioProcessor = new AudioProcessor();
        audioProcessor.setInputFile("/Users/amitp/Desktop/DrummerBot/IOT/PythonServerAlgo/Sounds/bhagraSlow.wav");
        //audioProcessor.setInputFile("/../../PythonServerAlgo/Sounds/bhagraSlow.wav");
        //TODO: give relative path for files

        audioProcessor.processFile();
        audioProcessor.updateFrameVectors();
        audioProcessor.updaterawFrameVectors();


        Vector [] vectors=new Vector[audioProcessor.rawBeatVectors.length];
        Vector [] smoothedVectors = new Vector[audioProcessor.rawBeatVectors.length];

        for(int vectorIndex=0;vectorIndex<vectors.length;vectorIndex++) {
            vectors[vectorIndex] = new Vector(audioProcessor.rawBeatVectors[vectorIndex]);
            smoothedVectors[vectorIndex] = vectors[vectorIndex].windowNormalisedVector();
        }
        new LineChartPolt("Raw beat vector 1", "Raw beat vector ", vectors[0].getVector());

        new LineChartPolt("Raw beat vector 2", "Raw beat vector ", vectors[1].getVector());

        new LineChartPolt("Raw beat vector 3", "Raw beat vector ", vectors[2].getVector());

        new LineChartPolt("Smoothed Raw beat vector 1", "Raw beat vector ", smoothedVectors[0].getVector());

        new LineChartPolt("Smoothed Raw beat vector 2", "Raw beat vector ", smoothedVectors[1].getVector());

        new LineChartPolt("Smoothed  Raw beat vector 3", "Raw beat vector ", smoothedVectors[2].getVector());

        for(int vectorIndex=0;vectorIndex<vectors.length;vectorIndex++) {
            vectors[vectorIndex].getUnitVector();
        }

        for(int counter=0;counter<3;counter++){
            System.out.println(vectors[counter].getAngle(vectors[counter+1]));
        }

        for(int counter=0;counter<3;counter++){
            System.out.println(smoothedVectors[counter].getAngle(smoothedVectors[counter+1]));
        }

        ThresholdAlgorithm algorithm = new ThresholdAlgorithm(vectors);
        algorithm.assignGroup();

        ArrayList<Group> groups = algorithm.getGroupList();
        System.out.println("Number of groups: " + groups.size());

        for(int counter=0;counter<groups.size();counter++){
            System.out.println("Id: "+ groups.get(counter).id + " Size: "+ groups.get(counter).getVectors().size());
        }
        for(int counter=0;counter<vectors.length;counter++){
            System.out.println("Group Id: "+ vectors[counter].getGroupNumber() + " index: "+ counter);
        }
        System.out.println("End!");
    }
}

