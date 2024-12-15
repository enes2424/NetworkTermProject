FROM ubuntu:22.04

RUN apt update && apt install -y openjdk-17-jdk make
RUN apt clean

WORKDIR /CSE471TermProject

COPY src src
COPY images images 
COPY Makefile Makefile

ENTRYPOINT ["make"]
