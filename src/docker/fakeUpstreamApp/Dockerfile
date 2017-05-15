FROM node:latest

COPY package.json internal.cert internal.key service.js /app/
WORKDIR /app
RUN npm install
CMD ["node", "/app/service.js"]