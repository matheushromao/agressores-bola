# ---- Build: compila e empacota com o Maven Wrapper ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Dependências primeiro, em camada própria: só é refeita quando o pom muda
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Os testes rodam no host (Testcontainers precisaria de Docker dentro do Docker)
COPY src src
RUN ./mvnw -B -q -DskipTests package

# ---- Runtime: só o JRE e o jar ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /build/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
