FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .

# COPY 源码和 settings.xml
COPY pom.xml .
COPY src/ src/
COPY settings.xml /tmp/settings.xml

# 使用 Maven 缓存加速依赖
RUN --mount=type=cache,target=/root/.m2 \
    mvn -s /tmp/settings.xml clean package -DskipTests -Dmaven.compiler.release=25

FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/canal-sync-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", \
    "--add-opens", "java.base/java.nio=ALL-UNNAMED", \
    "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", \
    "-Drocketmq.client.logUseSlf4j=true", \
    "-Drocketmq.client.log.level=DEBUG", \
    "-jar", "app.jar"]