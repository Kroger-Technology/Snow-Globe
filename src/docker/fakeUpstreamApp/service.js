var express = require('express');
var fs = require('fs');
var https = require('https');
var http = require('http');

// Constants;
var instanceNumber = Number(process.env.INSTANCE_NUMBER);
var clusterName = process.env.CLUSTER_NAME;
var matchingPaths = process.env.APP_PATHS;
var responseCode = Number(process.env.RESPONSE_CODE);
var responseHeaders = process.env.RESPONSE_HEADERS;
var runHTTPS = (process.env.USE_HTTPS === 'https');


var getResponseHeaders = function() {
    if(responseHeaders) {
        return JSON.parse(JSON.parse(responseHeaders));
    }
    return null;
};

var respond = function(responseCode, req, response) {
    var headers = getResponseHeaders();
    if(headers) {
       Object.keys(headers).map(function(key) {response.set(key, headers[key])});
    }
    response.status(responseCode).json({
        cluster: clusterName,
        instance: instanceNumber,
        request: {
            baseUrl:  req.baseUrl,
            body: req.body,
            cookies: req.cookies,
            headers: req.headers,
            hostname: req.hostname,
            urlToRp: req.get("host"),
            urlToApplication: req.protocol + '://' + req.get('host') + req.originalUrl,
            params: req.params,
            path: req.path,
            protocol: req.protocol,
            query: req.query,
            secure: req.secure,
            signedCookies: req.signedCookies,
            xhr: req.xhr
        }
    });
};

var privateKey = fs.readFileSync('/home/default/app/internal.key');
var certificate = fs.readFileSync('/home/default/app/internal.cert');

var credentials = {key: privateKey, cert: certificate};
// App
var app = express();

// Map all paths that should have "found" responses.
if(matchingPaths) {
    var paths = matchingPaths.split("|");
    for (var i = 0; i < paths.length; i++) {
        app.all(paths[i], function (req, res) { respond(responseCode, req, res); });
    }
}

app.get("/INTERNALHEALTHCHECKFORSTARTUP", function(req, res) {
   res.status(200).end();
});

// Handle all other requests as a 404 with the same information.
app.use(function (req, res) {
    respond(404, req, res);
});

var server;
if(runHTTPS) {
    server = https.createServer(credentials, app);
} else {
    server = http.createServer(app);
}
var runningServer = server.listen(3000, function () {
    var host = runningServer.address().address;
    var port = runningServer.address().port;

    console.log('Fake service app listening at http://%s:%s', host, port);
});