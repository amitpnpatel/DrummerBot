package VectorGroupingAlgorithm;

/**
 * Created by rajatvaryani on 3/8/17.
 */

public class Vector {

    private double[] vector;

    private int groupNumber = 0;

    public Vector(double[] vector) {
        this.vector = vector;
    }

    public double[] getVector() {
        return vector;
    }

    public void setVector(double[] vector) {
        this.vector = vector;
    }

    public int getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(int clusterNumber) {
        this.groupNumber = clusterNumber;
    }


    public Vector getUnitVector() {

        double denominatorSummation = 0.0;

        for (int counter = 0; counter < vector.length; counter++) {
            denominatorSummation += vector[counter] * vector[counter];
        }

        double denominator = Math.sqrt(denominatorSummation);

        for (int counter = 0; counter < vector.length; counter++) {
            vector[counter] /= denominator;
        }
        return this;
    }

    public double getAngle(Vector vector) {
        double cosineAngle = 0.0;

        for (int counter = 0; counter < vector.getVector().length; counter++) {
            cosineAngle += vector.getVector()[counter] * this.getVector()[counter];
        }

        return cosineAngle;
    }


    public Vector windowNormalisedVector() {
        double[] vectorMagnitudes = new double[this.getVector().length];

        for (int counter = 0; counter < vectorMagnitudes.length; counter++) {
            vectorMagnitudes[counter] = this.getVector()[counter];
        }

        Vector vector = new Vector(vectorMagnitudes);

        if (vector.getVector().length < 10) {

            vector.getUnitVector();
            return vector;
        }

        vectorMagnitudes = this.getWindowAverage(vector.getVector());
        return new Vector(vectorMagnitudes);

    }

    public double[] getWindowAverage(double[] values) {


        double sum = 0.0;
        double[] averageArray = new double[values.length - 9];

        for (int counter = 0; counter < 10; counter++) {
            sum += values[counter];
        }
        averageArray[0] = sum;
        for (int counter = 1; counter < averageArray.length; counter++) {
            averageArray[counter] = averageArray[counter - 1] - values[counter - 1] + values[counter + 9];

        }

        return averageArray;


    }


}
