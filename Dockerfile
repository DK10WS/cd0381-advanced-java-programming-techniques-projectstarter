FROM maven:amazoncorretto

WORKDIR /webcrawler

COPY starter/webcrawler .

RUN mvn package -Dmaven.test.skip=true

CMD ["java", "-classpath", "target/udacity-webcrawler-1.0.jar", "com.udacity.webcrawler.main.WebCrawlerMain", "src/main/java/com/udacity/webcrawler/main/config/sample_config_sequential.json"]
