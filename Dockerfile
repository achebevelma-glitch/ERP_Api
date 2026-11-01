# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Cache dependencies: only re-downloaded when pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the jar (tests are skipped here; run them in CI or locally)
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: run ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as a non-root user
RUN useradd --system --create-home spring
USER spring

COPY --from=build /app/target/ERP_Api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]