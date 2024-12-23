FROM ubuntu:22.04

RUN apt update && apt install -y openjdk-17-jdk && apt clean
RUN apt install net-tools

WORKDIR /CSE471TermProject

COPY src src
COPY images images
COPY lib lib
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh
RUN sed -i 's/\r$//' entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]