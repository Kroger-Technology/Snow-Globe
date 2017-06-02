const express = require('express');
const fs = require('fs');
const https = require('https');
const http = require('http');

// Constants;
const instanceNumber = Number(process.env.INSTANCE_NUMBER);
const clusterName = process.env.CLUSTER_NAME;
const matchingPaths = process.env.APP_PATHS;
const responseCode = Number(process.env.RESPONSE_CODE);
const runHTTPS = (process.env.USE_HTTPS === 'https');
const responseHeaders = process.env.RESPONSE_HEADERS;

const privateKey = fs.readFileSync('/app/internal.key');
const certificate = fs.readFileSync('/app/internal.cert');


const getResponseHeaders = () => {
  if(responseHeaders) {
      return JSON.parse(JSON.parse(responseHeaders));
    }
  return null;
};

const respond = (responseCode, req, response) => {
  const headers = getResponseHeaders();
  if(headers) {
      Object.keys(headers).map((key) => response.set(key, headers[key]));
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

const credentials = {key: privateKey, cert: certificate};
// App
const app = express();

// Map all paths that should have "found" responses.
if(matchingPaths) {
    const paths = matchingPaths.split("|");
    paths.forEach((path) => {
      app.all(path, function (req, res) { respond(responseCode, req, res); });
    });
}

// Handle all other requests as a 404 with the same information.
app.use(function (req, res) {
    respond(404, req, res);
});

let server;
if(runHTTPS) {
    server = https.createServer(credentials, app);
} else {
    server = http.createServer(app);
}
server.listen(3000);