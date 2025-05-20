# Optimizing Large-Scale Data Processing on Multicore Systems

## Running the Code

### Prerequisites

- Java 8 or higher

### Compile the Project

```bash
cd src/main/java/org
javac *.java
```

### Run the Code

```bash
cd src/main/java
java -DthreadCount={THREAD_NUMBER} -DfileName={WIKIPEDIA_FILE_DUMP_LOCATION} org.{CLASS_NAME_TO_RUN}
```

### Gather Results

To gather results, you can use the Java Flight Recorder (JFR) to analyze the performance of the program. The following command will start a flight recording for 30 minutes and save it to a file named `recording.jfr`.

```bash
cd src/main/java
java -XX:StartFlightRecording=duration=30m,filename=recording.jfr -DthreadCount={THREAD_NUMBER} -DfileName={WIKIPEDIA_FILE_DUMP_LOCATION} org.{CLASS_NAME_TO_RUN}
```

### Analyze the Results

You can use the Java Mission Control (JMC) tool to analyze the JFR recording. Open the `recording.jfr` file in JMC and explore the various metrics, such as CPU usage, memory usage, and thread activity.
You can also use the `jcmd` command to generate a report from the JFR recording. The following command will generate a report in HTML format.

## Artefacts

### Datasets Used to get the Results

- [27368 Wikipedia pages](https://dumps.wikimedia.org/enwiki/20241220/enwiki-20241220-pages-articles-multistream1.xml-p1p41242.bz2).
- [140196 Wikipedia pages](https://dumps.wikimedia.org/enwiki/20241220/enwiki-20241220-pages-articles-multistream4.xml-p311330p558391.bz2).
- [410616 Wikipedia pages](https://dumps.wikimedia.org/enwiki/20250401/enwiki-20250401-pages-articles-multistream9.xml-p2936261p4045402.bz2).
