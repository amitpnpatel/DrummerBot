var webSocket = new WebSocket('ws://127.0.0.1:5000');

var maximumNumberOfSequenceInBeat = 6;
var timeArray = new Array(maximumNumberOfSequenceInBeat);
var amplitudeArray = new Array(maximumNumberOfSequenceInBeat);
var drumArray = new Array(maximumNumberOfSequenceInBeat);
var namesOfBeatsSavedInDatabase;
var inputBeatSequencesFromUser = new Array(new Array);

for (var counter = 0; counter < maximumNumberOfSequenceInBeat; counter++) {
    timeArray[counter] = 150;
    amplitudeArray[counter] = 50;
    drumArray[counter] = counter % 3;
}

var numberOfSequencesInBeat = 6;


webSocket.onopen = function () {
    console.log('webSocket opened')
};
webSocket.onerror = function (error) {
    console.log('Error');
};
webSocket.onmessage = function (message) {
    try {
        onMessageReceived(message);
    } catch (e) {
        console.log('This doesn\'t look like a valid JSON: ', message.data);
        return;
    }
};
webSocket.onclose = function () {
    console.log('Close');
    document.getElementById("receivedMessage").innerHTML += "Socket closed :(" + '<br>';

};

var currentDeviceStatus = false;

function processSetConfigurationCommand(receivedJSON) {
    var namesOfBeats = new Array();
    namesOfBeatsSavedInDatabase = namesOfBeats;
    for (var beatCounter = 0; beatCounter < receivedJSON.data.length; beatCounter++) {
        namesOfBeats.push(receivedJSON.data[beatCounter].name);
    }
    inflateDropDownForBeatsNames(namesOfBeats, "uploadedBeats");
}

function processSetBeatSequenceCommand(receivedJSON) {
    for (var beatCounter = 0; beatCounter < receivedJSON.data.length; beatCounter++) {
        var timeIndexInBeatObject = 0;
        var amplitudeIndexInBeatObject = 1;
        var drumNumberIndexInBeatObject = 2;
        timeArray[beatCounter] = receivedJSON.data[beatCounter][timeIndexInBeatObject];
        amplitudeArray[beatCounter] = receivedJSON.data[beatCounter][amplitudeIndexInBeatObject];
        drumArray[beatCounter] = receivedJSON.data[beatCounter][drumNumberIndexInBeatObject];
    }
    numberOfSequencesInBeat = receivedJSON.data.length;
    setUIBasedOnLoadedBeats();
}

function processSetCommand(receivedJSON) {
    if (receivedJSON.requestType == "configuration") {
        processSetConfigurationCommand(receivedJSON);
    }
    else if (receivedJSON.requestType == "beatSequence") {
        processSetBeatSequenceCommand(receivedJSON);
    }
    else if (receivedJSON.requestType == "compiledBeatSequence") {
        makeLoadButtonVisible(receivedJSON.data);
    }
}
function onMessageReceived(event) {

    var receivedData = event.data.toString();
    var receivedJSON = JSON.parse(receivedData);
    if (receivedJSON.command == "set") {
        processSetCommand(receivedJSON);
    }
    document.getElementById("receivedMessage").innerHTML = event.data.toString() + document.getElementById("receivedMessage").innerHTML + '<br>';
}

// Client JS code

function startStopActuator() {
    if (webSocket.readyState != WebSocket.OPEN) {
        return;
    }

    currentDeviceStatus = !currentDeviceStatus;
    webSocket.send(returnJSONForRequest('set', 'state', currentDeviceStatus));
}

function resetBeatStream() {
    if (webSocket.readyState != WebSocket.OPEN) {
        return;
    }
    webSocket.send(returnJSONForRequest('set', 'reset'));
}

function returnJSONForRequest(command, request, data) {
    return JSON.stringify({
        'command': command,
        'requestType': request,
        'data': data
    });
}

function returnJSONForSavingBeatRequest(command, request, beatSequenceName, data) {
    return JSON.stringify({
        'command': command,
        'requestType': request,
        'beatSequenceName': beatSequenceName,
        'data': data
    });
}

function getCurrentBeatSequence() {
    var data = new Array(new Array());
    var sortedBeatSequence = sortBeatSequenceArrayOnBasisOfTimeInterval();

    data[0] = sortedBeatSequence[0];

    for (var counter = 1; counter < inputBeatSequencesFromUser.length ; counter++) {


        var differenceOfTimeBetweenTwoConsecutiveBeats = sortedBeatSequence[counter][0] - sortedBeatSequence[counter - 1][0];
        var amplitudeIndex = 1;
        var drumNumberIndex = 2;
        var beat = [differenceOfTimeBetweenTwoConsecutiveBeats, sortedBeatSequence[counter][amplitudeIndex], sortedBeatSequence[counter][drumNumberIndex]];
        data[counter] = beat;
    }
    return data;
}

function uploadBeat() {
    var data = getCurrentBeatSequence();
    if (webSocket.readyState != WebSocket.OPEN) {
        return;
    }
    webSocket.send(returnJSONForRequest('set', 'beat', data));
}
function getFrameTimeInterval() {
    var frameTimeInterval = 0;
    for (var counter = 0; counter < numberOfSequencesInBeat; counter++) {
        frameTimeInterval += timeArray[counter];
    }
    return frameTimeInterval;
}
function getTimeForBeatRelativeToFrame(beatNumber) {
    var totalTime = 0;
    for (var counter = 0; counter < beatNumber + 1; counter++) {
        totalTime += timeArray[counter];
    }
    return totalTime;
}

function saveBeatSequenceToDatabase() {
    var nameOfBeat = document.getElementById("nameOfBeatSequence").value;
    webSocket.send(returnJSONForSavingBeatRequest('save', 'beatSequence', nameOfBeat, getCurrentBeatSequence()));
}

function requestBeatSequenceForBeatName(nameOfBeat) {
    webSocket.send(returnJSONForRequest('get', 'beatSequence', nameOfBeat));
}

function deleteBeatSequence(nameOfBeat) {
    webSocket.send(returnJSONForRequest('delete', 'beatSequence', nameOfBeat));
}

function returnJSONForRepeatingABeatSequence(nameOfBeatSequence, numberOfTimesToRepeat) {
    return ({
        'nameOfBeatSequence': nameOfBeatSequence,
        'timesToRepeat': numberOfTimesToRepeat,
    });
}

function getAllBeatsToBeCompiledFromUserInput(numberOfDifferentBeatSequences, beatsToBeComplied) {
    for (var beatSequenceCounter = 0; beatSequenceCounter < numberOfDifferentBeatSequences; beatSequenceCounter++) {
        var identifierForTextBox = "textBoxForCompilingBeatSequences" + beatSequenceCounter;
        var identifierForSelectBox = "selectBoxForBeatSequenceNames" + beatSequenceCounter;
        var numberOfTimesToRepeat = getInputNumberFromTextBox(identifierForTextBox);
        var nameOfBeatSequenceToRepeat = getBeatNameGivenByUser(identifierForSelectBox);
        beatsToBeComplied.push(returnJSONForRepeatingABeatSequence(nameOfBeatSequenceToRepeat, numberOfTimesToRepeat));
    }
}
function compileBeatSequences() {
    var idOfInputTextForSequencesToStitch = "inputNumberOfBeatSequencesToStitch";
    var numberOfDifferentBeatSequences = getInputNumberFromTextBox(idOfInputTextForSequencesToStitch);
    var beatsToBeComplied = Array();
    getAllBeatsToBeCompiledFromUserInput(numberOfDifferentBeatSequences, beatsToBeComplied);
    webSocket.send(returnJSONForRequest("set", "compiledBeatSequence", beatsToBeComplied));
}

function getBeatSequenceNames() {
    return namesOfBeatsSavedInDatabase;
}

function loadCompiledBeatSequences() {
    webSocket.send(returnJSONForRequest('get', 'compiledBeatSequence'));
}

function saveToBeatSequenceArray(timeFrame, amplitude, drumNumber) {
    inputBeatSequencesFromUser.push([timeFrame, amplitude, drumNumber]);
}

function sortBeatSequenceArrayOnBasisOfTimeInterval() {
    var sortedBeatSequences = inputBeatSequencesFromUser.sort(sortFunction);

    function sortFunction(inputOne, inputTwo) {
        var timeIndex = 0;
        if (inputOne[timeIndex] === inputTwo[timeIndex]) {
            return timeIndex;
        }
        else {
            return (inputOne[timeIndex] < inputTwo[timeIndex]) ? -1 : 1;
        }
    }

    return sortedBeatSequences;
}

function isBeatAtThatTimeFrameAlreadyExisting(timeFrame, canvasId) {

    var isBeatAlreadySavedAtThatTime = false;
    for (var inputBeatsCounter = 0; inputBeatsCounter < inputBeatSequencesFromUser.length; inputBeatsCounter++) {
        if ((Math.abs(inputBeatSequencesFromUser[inputBeatsCounter][0] - timeFrame) <= 5) && inputBeatSequencesFromUser[inputBeatsCounter][2] == canvasId) {
            isBeatAlreadySavedAtThatTime = true;
        }
    }
    console.log(inputBeatSequencesFromUser);

    return isBeatAlreadySavedAtThatTime;
}

function deleteBeatSequenceAtThatTimeFrame(timeFrame, canvasId) {

    var index;

    for (var inputBeatsCounter = 0; inputBeatsCounter < inputBeatSequencesFromUser.length; inputBeatsCounter++) {
        if ((Math.abs(inputBeatSequencesFromUser[inputBeatsCounter][0] - timeFrame) <= 5 ) && inputBeatSequencesFromUser[inputBeatsCounter][2] == canvasId) {
            index = inputBeatsCounter;
        }
    }
    if (index > -1) {
        inputBeatSequencesFromUser.splice(index, 1);
    }

    console.log(inputBeatSequencesFromUser);
}