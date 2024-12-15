normal:
	@javac -cp lib/gson-2.8.9.jar:src -d class src/P2P.java
	@java -cp lib/gson-2.8.9.jar:class P2P

clean:
	@rm -rf class

docker:
	@docker rm peer 2>/dev/null | true
	@docker build -t p2p .
	@docker run -e DISPLAY=:0 -v /tmp/.X11-unix:/tmp/.X11-unix --name peer p2p

docker-compose:
	@docker-compose up --build

down:
	@docker-compose down

clean-docker:
	@docker system prune -a --volumes

.PHONY: normal clean docker docker-compose clean-docker