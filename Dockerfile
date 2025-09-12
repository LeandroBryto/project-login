# Stage 1: Build
FROM ubuntu:latest AS build

# Atualizar os pacotes do sistema
RUN apt-get update

# Instalar Java 21 e Maven
RUN apt-get install -y openjdk-21-jdk maven

# Definir JAVA_HOME

# Criar diretório de trabalho
WORKDIR /app

# Copiar arquivos do projeto
COPY . .

# Compilar o projeto
RUN mvn clean install -DskipTests

# Stage 2: Runtime
FROM openjdk:21-jdk-slim

# Criar diretório de trabalho
WORKDIR /app

# Copiar o JAR do stage de build
COPY --from=build /app/target/e-commerce-0.0.1-SNAPSHOT.jar app.jar

# Expor a porta da aplicação
EXPOSE 8080

# Comando para executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]

