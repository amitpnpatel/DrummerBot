var BeatStream = require('./BeatStream.js');
var express = require('express');
var path = require("path");
var app = express();
var expressWs = require('express-ws')(app);
var net = require('net');

app.use(express.static('public'));

var webClient;
var deviceClient;
var beatStream = new BeatStream.BeatStream();

app.ws('/', function (ws, req) {
    if (webClient != null) {
        webClient.close();
    }
    webClient = ws;
    ws.on('message', function (message) {
        console.log('WEB IN: ', message.toString());
        handleMessageFromWebClient(message);
    });
    ws.on('close', function (event) {
        if (ws == webClient) {
            webClient = null;
        }
    });
});
app.listen(5000);

//TCP Server

net.createServer(function (socket) {
    if (deviceClient != null) {
        deviceClient.close();
    }
    deviceClient = socket;
    socket.on('data', function (data) {
        console.log('TCP IN: ', data.toString());
        var jsonStringArray = data.toString().split('\n');

        for (var counter = 0; counter < jsonStringArray.length; counter++) {
            processInputFromDeviceClient(jsonStringArray[counter]);
        }
    });
    socket.on('error', function (err) {
        console.log("Error: ");
        console.log(err.stack);
    });

    socket.on('close', function () {
        if (deviceClient == socket) {
            console.log("Socket closed from close function");
            socket.end();
            deviceClient = null;
        }
    });
}).listen(5001);

function isJson(str) {
    try {
        JSON.parse(str);
    } catch (e) {
        return false;
    }
    return true;
}

function handleMessageFromWebClient(message) {
    var receivedJSONObject = JSON.parse(message);
    if ((receivedJSONObject.command == 'set') && (receivedJSONObject.requestType == 'beat')) {

        var data = new Array(new Array());
        data = receivedJSONObject.data;
        beatStream.createNewBeatStream(data);
        sendMessageToDeviceClient(returnJSONForRequest("set", "reset", 0));

    }
    else {
        sendMessageToDeviceClient(message.toString());
    }
}

function processInputFromDeviceClient(data) {
    if (!isJson(data)) {
        return;
    }
    var receivedJSONObject = JSON.parse(data);
    if (receivedJSONObject.command == 'get') {
        if (receivedJSONObject.requestType == 'beat') {
            var indexToSend = receivedJSONObject.data;
            var beats = beatStream.getBeatArray(indexToSend);

            sendMessageToDeviceClient(returnJSONForRequest('set', 'beat', beats));
        }
    }
    else {
        sendMessageToWebClient(data.toString());
    }
}

function returnJSONForRequest(command, request, data) {
    return JSON.stringify({
        'command': command,
        'requestType': request,
        'data': data
    });
}

function sendMessageToDeviceClient(message) {
    console.log("TCP OUT: ", message);
    if (deviceClient == null) {
        console.log("Device Client NULL");
        return;
    }
    message += '\n';
    deviceClient.write(message);
}

function sendMessageToWebClient(message) {
    console.log("WEB OUT: ", message);
    if (webClient == null) {
        console.log("Web Client NULL");
        return;
    }
    webClient.send(message);
}
