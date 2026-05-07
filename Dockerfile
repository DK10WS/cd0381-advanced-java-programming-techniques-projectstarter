FROM maven:amazoncorretto

WORKDIR /webcrawler

COPY starter/webcrawler .

CMD ["mvn" ,  "test" , "-Dtest=ConfigurationLoaderTest"]
