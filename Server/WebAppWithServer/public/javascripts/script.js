var webSocket = new WebSocket('ws://127.0.0.1:5000');

var maximumNumberOfSequenceInBeat = 50;
var timeArray = new Array(maximumNumberOfSequenceInBeat);
var amplitudeArray = new Array(maximumNumberOfSequenceInBeat);
var drumArray = new Array(maximumNumberOfSequenceInBeat);

for (var counter = 0; counter < maximumNumberOfSequenceInBeat; counter++) {
    timeArray[counter] = 200;
    amplitudeArray[counter] = 20;
    drumArray[counter] = counter % 2;
}

var numberOfSequencesInBeat = 5;


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

function onMessageReceived(event) {
    document.getElementById("receivedMessage").innerHTML += event.data.toString() + '<br>';
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

function uploadBeat() {
    var data = new Array(new Array());
    for (var counter = 0; counter < numberOfSequencesInBeat; counter++) {
        var beat = [timeArray[counter], amplitudeArray[counter], drumArray[counter]];
        data[counter] = beat;
    }
    console.log(data);

    if (webSocket.readyState != WebSocket.OPEN) {
        return;
    }
    webSocket.send(returnJSONForRequest('set', 'beat', data));
}
function getFrameTimeInterval(){
    var frameTimeInterval=0;
    for (var counter = 0; counter < numberOfSequencesInBeat; counter++) {
        frameTimeInterval+= timeArray[counter];
    }
    return frameTimeInterval;
}
function getTimeForBeatRelativeToFrame(beatNumber){
    var totalTime=0;
    for (var counter = 0; counter < beatNumber+1; counter++) {
        totalTime+= timeArray[counter];
    }
    return totalTime;
}