main.jar: main.kt
	kotlinc -Xopt-in=kotlin.RequiresOptIn main.kt -include-runtime -d main.jar

.PHONY: run
run: main.jar
	java -jar main.jar input.txt
