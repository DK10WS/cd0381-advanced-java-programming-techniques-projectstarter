FROM maven:amazoncorretto

WORKDIR /webcrawler

COPY starter/webcrawler .

RUN mvn package

CMD sh -c 'java -classpath target/udacity-webcrawler-1.0.jar com.udacity.webcrawler.main.WebCrawlerMain src/main/java/com/udacity/webcrawler/main/config/sample_config.json && cat profileData.txt'
