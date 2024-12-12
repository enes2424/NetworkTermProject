FROM debian:bullseye

RUN apt update && apt install -y openjdk-11-jdk
RUN apt clean

WORKDIR /CSE471TermProject

COPY * .

RUN javac P2P.java

ENTRYPOINT ["java", "P2P"]
