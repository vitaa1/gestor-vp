# syntax=docker/dockerfile:1.7

FROM node:20.19.5-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /workspace/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode dependency:go-offline
COPY backend/src src
COPY --from=frontend-build /workspace/frontend/dist/vence-facil-web/browser src/main/resources/static
RUN ./mvnw --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S vencefacil && adduser -S vencefacil -G vencefacil
WORKDIR /app
COPY --from=backend-build --chown=vencefacil:vencefacil /workspace/backend/target/vence-facil-api-*.jar app.jar
USER vencefacil
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
