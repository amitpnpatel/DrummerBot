exports.BeatStream = function () {
    this.lastIndexSent = 0;
    this.setToDefault = function () {
        this.timeForNextBeat = new Array(1000);
        this.amplitudeForNextBeat = new Array(1000);
        this.drumNumber = new Array(1000);
        this.lastIndexSent = 0;
        for (var counter = 0; counter < this.timeForNextBeat.length; counter++) {
            this.timeForNextBeat[counter] = 5000;
            this.amplitudeForNextBeat[counter] = 4;
            this.drumNumber[counter] = 0;
        }
    };

    this.createNewBeatStream = function (data) {
        this.timeForNextBeat = new Array(1000);
        this.amplitudeForNextBeat = new Array(1000);
        this.drumNumber = new Array(1000);
        for (var counter = 0; counter < this.timeForNextBeat.length; counter++) {
            this.timeForNextBeat[counter] = data[counter%(data.length)][0];
            this.amplitudeForNextBeat[counter] = data[counter%data.length][1];
            this.drumNumber[counter] = data[counter%data.length][2];
        }
    };

    this.getBeat = function (index) {
        var result = new Array(4);
        var beatIndex = index;
        result[0] = index;
        result[1] = this.timeForNextBeat[index];
        result[2] = this.amplitudeForNextBeat[index];
        result[3] = this.drumNumber[index];
        console.info("Result[0], Result[1] : " + result[0] + ", " + result[1]);
        this.lastIndexSent = beatIndex;
        return result;
    };

    this.getBeatArray = function (index) {
        var arrayLength;
        if (this.timeForNextBeat.length - index > 100) {
            arrayLength = 100;
        }
        else {
            arrayLength = this.timeForNextBeat.length - index;
        }
        var result = new Array(arrayLength);
        for (var beatCounter = 0; beatCounter < arrayLength; beatCounter++) {
            result[beatCounter] = this.getBeat(beatCounter + index);
        }
        return result;
    };

    this.setToDefault();

};
