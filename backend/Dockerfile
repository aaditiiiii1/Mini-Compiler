FROM openjdk:17
WORKDIR /app
COPY . .
RUN javac -encoding UTF-8 MiniCompiler.java CompilerServer.java && jar cfm compiler-server.jar MANIFEST.MF *.class
CMD ["java", "-jar", "compiler-server.jar"]