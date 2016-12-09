FROM docker.kroger.com/library/tinynode

RUN mkdir /home/default/app
COPY internal.cert internal.key service.js /home/default/app/

CMD ["--minUptime", "1000", "--spinSleepTime", "1000", "app/service.js"]