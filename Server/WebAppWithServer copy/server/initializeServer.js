var BeatStream = require('./BeatStream.js');
var express = require('express');
var path = require("path");
var app = express();
var expressWs = require('express-ws')(app);
var net = require('net');

var MongoClient = require('mongodb').MongoClient;
var url = 'mongodb://localhost:27017/beats';

app.use(express.static('public'));

var webClient;
var deviceClient;
var beatStream = new BeatStream.BeatStream();

var currentCompiledBeatSequence = Array();


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

    sendBeatSequenceNamesFromDatabaseToWebSocket();
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


    if ((receivedJSONObject.command == 'get')) {
        processGetCommandFromWebClient(receivedJSONObject);
    }

    else if ((receivedJSONObject.command == 'set')) {
        processSetCommandFromWebClient(receivedJSONObject);
    }

    else if ((receivedJSONObject.command == 'save') && (receivedJSONObject.requestType == 'beatSequence')) {
        processSaveBeatSequence(receivedJSONObject);
    }

    else if ((receivedJSONObject.command == 'delete') && (receivedJSONObject.requestType == 'beatSequence')) {
        removeBeatSequenceFromDatabase(receivedJSONObject.data);
    }
    else {
        sendMessageToDeviceClient(message.toString());
    }
}

function processSetCommandFromWebClient(receivedJSONObject) {
    if (receivedJSONObject.requestType == 'beat') {
        var data = new Array(new Array());
        data = receivedJSONObject.data;
        beatStream.createNewBeatStream(data);
        sendMessageToDeviceClient(returnJSONForRequest("set", "reset", 0));
    }
    else if (receivedJSONObject.requestType == 'compiledBeatSequence') {
        sendMessageToWebClient(returnJSONForRequest("set", "compiledBeatSequence", false));
        compileBeatSequence(receivedJSONObject.data);
        sendMessageToWebClient(returnJSONForRequest("set", "compiledBeatSequence", true));
    }
    else
    {
        sendMessageToDeviceClient(JSON.stringify(receivedJSONObject));
    }
}


function processSaveBeatSequence(receivedJSONObject) {
    var beatSequenceName = receivedJSONObject.beatSequenceName;
    var data = new Array(new Array());
    data = receivedJSONObject.data;
    saveBeatSequenceToDatabase(beatSequenceName, data);
}

function processGetCommandFromWebClient(receivedJSONObject) {
    if (receivedJSONObject.requestType == 'beatSequenceNames') {
        sendBeatSequenceNamesFromDatabaseToWebSocket();
    }
    else if (receivedJSONObject.requestType == 'beatSequence') {
        sendBeatSequenceOfName(receivedJSONObject.data);
    }
    else if (receivedJSONObject.requestType == 'compiledBeatSequence') {
        sendMessageToWebClient(returnJSONForRequest("set", "beatSequence", getCompiledBeatSequence()));
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

function saveBeatSequenceToDatabase(beatSequenceName, data) {
    MongoClient.connect(url, function (err, db) {
        if (err) {
            console.log('Unable to connect to the mongoDB server. Error:', err);
        } else {
            db.open();
            db.collection('uploadedBeats').insertOne({
                "name": beatSequenceName,
                "data": data
            }, function (err, result) {
                console.log("Inserted a document into the uploadedBeats collection.");
                sendBeatSequenceNamesFromDatabaseToWebSocket();
            });
            db.close();
        }
    });
}


function getInitialConfiguration(items) {
    return returnJSONForRequest('set', 'configuration', items);
}

function sendBeatSequenceNamesFromDatabaseToWebSocket() {
    MongoClient.connect(url, function (err, db) {
        if (err) {
            console.log('Unable to connect to the mongoDB server. Error:', err);
        } else {
            db.open();
            db.collection('uploadedBeats').find({}, {name: 1, _id: 0}).toArray(function (err, items) {
                sendMessageToWebClient(getInitialConfiguration(items));
            });
            db.close();
        }
    });
}

function sendBeatSequenceOfName(beatName) {
    MongoClient.connect(url, function (err, db) {
        if (err) {
            console.log('Unable to connect to the mongoDB server. Error:', err);
        } else {
            db.open();
            db.collection('uploadedBeats').findOne({name: beatName}, function (err, item) {
                sendMessageToWebClient(returnJSONForRequest("set", "beatSequence", item.data));
            });
            db.close();
        }
    });
}

function removeBeatSequenceFromDatabase(beatName) {
    MongoClient.connect(url, function (err, db) {
        if (err) {
            console.log('Unable to connect to the mongoDB server. Error:', err);
        } else {
            db.open();
            db.collection('uploadedBeats').deleteOne({name: beatName}, function (err, item) {
                sendBeatSequenceNamesFromDatabaseToWebSocket();
            });
            db.close();
        }
    });
}

function saveBeatSequenceToCompiledBeatSequence(beatSequence, index) {
    currentCompiledBeatSequence[index] = beatSequence;
    return;
}

function getCompiledBeatSequence() {
    return currentCompiledBeatSequence;
}

function compileBeatSequence(beatSequencesToBeCompiled) {
    currentCompiledBeatSequence = [];
    MongoClient.connect(url, function (err, db) {
        if (err) {
            console.log('Unable to connect to the mongoDB server. Error:', err);
        } else {
            db.open();
            var index = 0;
            beatSequencesToBeCompiled.forEach(function (jsonObject) {
                for (var repeatCounter = 0; repeatCounter < jsonObject.timesToRepeat; repeatCounter++) {
                    db.collection('uploadedBeats').findOne({name: jsonObject.nameOfBeatSequence}, function (err, item) {
                        item.data.forEach(function (beatSequence) {
                            saveBeatSequenceToCompiledBeatSequence(beatSequence, index);
                            index++;
                        })

                    });
                }
            });

            db.close();
        }
    });

}