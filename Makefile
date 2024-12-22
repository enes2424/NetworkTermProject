normal:
	@javac -cp lib/gson-2.8.9.jar:src -d class src/P2P.java
	@java -cp lib/gson-2.8.9.jar:class -Djava.net.preferIPv4Stack=true P2P

clean:
	@rm -rf class

docker:
	@docker rm peer 2>/dev/null | true
	@docker build -t p2p .
	@docker run -e APPNAME=peer -e DISPLAY=:0 -v /tmp/.X11-unix:/tmp/.X11-unix --name peer p2p

docker-compose:
	@docker-compose down
	@docker build -t p2p .
	@docker-compose up

clean-docker:
	@docker rm peer 2>/dev/null | true
	@docker-compose down

clean-all-docker:
	@docker system prune -a --volumes

.PHONY: normal clean docker docker-compose clean-docker