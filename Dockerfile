# Stage 1: Build (Compila o código e gera o JAR)
# Usamos uma imagem com Maven para não depender do Maven da máquina do dev
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# O flag -DskipTests acelera o build (testes rodam no CI/CD, não no build da imagem)
RUN mvn clean package -DskipTests

# Stage 2: Runtime (Apenas o JRE para rodar)
# Usamos 'alpine' para a imagem ficar minúscula (menos superfície de ataque)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Criamos um usuário sem privilégios (root é perigoso em containers)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos APENAS o jar gerado no Stage 1
COPY --from=builder /app/target/*.jar app.jar

# Boas práticas de JVM em Container (respeitar limites de memória do Docker)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]