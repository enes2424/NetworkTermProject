#!/bin/bash

javac -cp lib/gson-2.8.9.jar:src -d class src/P2P.java
exec java -cp lib/gson-2.8.9.jar:class -Djava.net.preferIPv4Stack=true P2P $APPNAME