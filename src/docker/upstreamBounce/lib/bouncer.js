const express = require('express');
const fs = require('fs');
const https = require('https');
const http = require('http');
var Promise = require('bluebird');

const buildResponseHandler = (responseHeaders, clusterName, instanceNumber) => {
  return (responseCode, req, response) => {
    if (responseHeaders) {
      Object.keys(responseHeaders).map((key) => response.set(key, responseHeaders[key]));
    }
    response.status(responseCode).json({
      cluster: clusterName,
      instance: instanceNumber,
      request: {
        baseUrl: req.baseUrl,
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
};

function setupRoutes(matchingPaths, app, responseHandler, responseCode) {
// Map all paths that should have "found" responses.
  if (matchingPaths) {
    const paths = matchingPaths.split("|");
    paths.forEach((path) => {
      app.all(path, (req, res) => {
        responseHandler(responseCode, req, res);
      });
    });
  }

  // Handle all other requests as a 404 with the same information.
  app.use(function (req, res) {
    responseHandler(404, req, res);
  });
}

const buildServerInstance = ({instanceNumber, clusterName, matchingPaths, responseCode, runHTTPS, responseHeaders, port}) => {

  // App
  const app = express();
  const responseHandler = buildResponseHandler(responseHeaders, clusterName, instanceNumber);
  setupRoutes(matchingPaths, app, responseHandler, responseCode);
  let server;
  if (runHTTPS) {
    const privateKey = fs.readFileSync('/app/internal.key');
    const certificate = fs.readFileSync('/app/internal.cert');
    const credentials = {key: privateKey, cert: certificate};
    server = https.createServer(credentials, app);
  } else {
    server = http.createServer(app);
  }
  return new Promise((res, err) => {
    server.listen(port);
    server.on('listening', () => {
      console.log('Server listening on: ' + port + '...');
      res(server)
    });
    server.on('error', (e) => {
      if (e.code === 'EADDRINUSE') {
        console.log('Address in use, retrying...');
        setTimeout(() => {
          server.close();
          server.listen(port);
        }, 50);
      } else {
         err(e);
      }
    });
  });
};

module.exports = {
  buildServerInstance
};
