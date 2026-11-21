FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build
COPY BackEnd/${MODULE}/pom.xml ./pom.xml
COPY BackEnd/${MODULE}/src ./src
RUN --mount=type=cache,id=sol-shiran-maven,target=/root/.m2,sharing=locked mvn -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl fontconfig && rm -rf /var/lib/apt/lists/*     && groupadd --gid 10001 app && useradd --uid 10001 --gid app --create-home app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/*.jar /app/app.jar
RUN mkdir -p /app/uploads/perfiles /app/uploads/comprobantes && chown -R app:app /app
USER app
ENTRYPOINT ["java","-jar","/app/app.jar"]
