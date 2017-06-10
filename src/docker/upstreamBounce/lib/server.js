const express = require('express');
const manager = require('./manager');
const bodyParser = require('body-parser');
const http = require('http');

const jsonParser = bodyParser.json();

const startServer = () => {
  const app = express();
  app.post('/startServer', jsonParser, (req, res) => {
    manager.buildInstance(req.body)
        .then((instancePort) => {
          res.status(200).send(`${instancePort}`);
        });
  });

  app.post('/stopServer', (req, res) => {
    const stopRequestPort = req.body;
    manager.shutDownInstance(stopRequestPort);
    res.status(200).end();
  });

  http.createServer(app).listen(3000, () => console.log("Ready to go!"));
};

module.exports = {
  startServer
};