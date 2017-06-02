FROM node:latest

ADD package.json /tmp/package.json
RUN cd /tmp && npm install
RUN mkdir -p /app && cp -a /tmp/node_modules /app/

COPY package.json internal.cert internal.key service.js /app/
WORKDIR /app
CMD ["node", "/app/service.js"]