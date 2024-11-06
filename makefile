
build:
	javac -cp '.:deps/*' *.java

run: build
	java -cp '.:deps/*' Main > main-run.txt

clean:
	rm -f *.class
