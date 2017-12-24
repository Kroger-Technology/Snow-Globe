const bouncer = require('./bouncer');

let runningInstances = {};

const buildInstance = (config) => {
  return bouncer.buildServerInstance(config)
      .then((server) => {
        const port = server.address().port;
        runningInstances[port] = server;
        return port;
      });
};

const reset = () => {
  runningInstances.forEach(inst => inst.close());
  runningInstances = {};
};

const shutDownInstance = (port) => {
  runningInstances[port].close();
};

module.exports = {
  buildInstance,
  shutDownInstance,
  reset,
};