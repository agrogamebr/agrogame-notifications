# Estágio 1: Build da aplicação usando Maven
# Usamos a imagem oficial do Eclipse Temurin (Java 21) com Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia apenas o pom.xml primeiro (otimiza o cache do Docker)
COPY pom.xml .
# Baixa as dependências (se o pom.xml não mudar, o Docker faz cache deste passo demorado)
RUN mvn dependency:go-offline -B

# Copia o código fonte e os templates HTML
COPY src ./src
# Executa o build, empacotando tudo em um .jar e pulando os testes
RUN mvn clean package -DskipTests

# Estágio 2: Run da aplicação (Imagem otimizada para o Cloud Run)
# Usamos uma imagem JRE Alpine, que é extremamente leve e segura
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o arquivo .jar gerado no Estágio 1 para a imagem final
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta 8080 (Padrão do Cloud Run / Spring Boot)
EXPOSE 8080

# Define as propriedades de memória da JVM adequadas para containers pequenos
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Comando de inicialização do Spring Boot
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
