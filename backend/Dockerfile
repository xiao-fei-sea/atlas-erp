FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY erp-common/pom.xml erp-common/
COPY erp-system/pom.xml erp-system/
COPY erp-security/pom.xml erp-security/
COPY erp-masterdata/pom.xml erp-masterdata/
COPY erp-workflow/pom.xml erp-workflow/
COPY erp-inventory/pom.xml erp-inventory/
COPY erp-finance/pom.xml erp-finance/
COPY erp-purchase/pom.xml erp-purchase/
COPY erp-sales/pom.xml erp-sales/
COPY erp-crm/pom.xml erp-crm/
COPY erp-report/pom.xml erp-report/
COPY erp-app/pom.xml erp-app/
RUN mvn -B -q -DskipTests dependency:go-offline || true
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /build/erp-app/target/erp-app-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
