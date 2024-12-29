FROM eclipse-temurin:23-jdk-alpine
VOLUME /tmp
EXPOSE 8282
RUN mkdir /app
#RUN apt-get update && apt-get -y upgrade && apt-get -y install curl
COPY ./build/boot_jar_exploded/ /app/
ENV JDK_JAVA_OPTIONS=""
WORKDIR /app/BOOT-INF/classes
ENTRYPOINT exec java -cp .:../../org/springframework/boot/loader/:../lib/* net.flyingfishflash.loremlist.LoremListApplicationKt