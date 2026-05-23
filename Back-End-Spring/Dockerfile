# --- ESTÁGIO 1: Build (Compilação) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia apenas o pom.xml primeiro para baixar as dependências
COPY pom.xml .
# Baixa as dependências. Como o pom.xml muda pouco, o Docker salva isso em cache
RUN mvn dependency:go-offline

# Agora sim copia o código-fonte
COPY src ./src
# Compila o projeto ignorando os testes (para o build ser mais rápido)
RUN mvn clean package -DskipTests

# --- ESTÁGIO 2: Run (Execução) ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copia apenas o arquivo .jar gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

# Informa que o container vai se comunicar na porta 8080
EXPOSE 8080

# O comando que o container vai rodar ao iniciar
ENTRYPOINT ["java", "-jar", "app.jar"]