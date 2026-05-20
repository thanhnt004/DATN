# syntax=docker/dockerfile:1

# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Nhận tên service cần build từ docker-compose
ARG SERVICE_NAME

# 1. Copy tất cả pom.xml để tạo khung xương (Skeleton)
#    Layer này chỉ thay đổi khi pom.xml thay đổi
COPY pom.xml .
COPY common-core/pom.xml common-core/
COPY config-server/pom.xml config-server/
COPY discovery-service/pom.xml discovery-service/
COPY api-gateway/pom.xml api-gateway/
COPY identity-service/pom.xml identity-service/
COPY user-service/pom.xml user-service/
COPY notification-service/pom.xml notification-service/
COPY category-service/pom.xml category-service/
COPY cart-service/pom.xml cart-service/
COPY order-service/pom.xml order-service/
COPY product-service/pom.xml product-service/
COPY banner-service/pom.xml banner-service/
COPY payment-service/pom.xml payment-service/
COPY file-service/pom.xml file-service/
COPY discount-service/pom.xml discount-service/
COPY seller-service/pom.xml seller-service/
COPY inventory-service/pom.xml inventory-service/
COPY search-service/pom.xml search-service/
COPY review-service/pom.xml review-service/
COPY chatbot-service/pom.xml chatbot-service/
COPY chat-service/pom.xml chat-service/
COPY websocket-worker/pom.xml websocket-worker/
COPY shipping-service/pom.xml shipping-service/

# 2. Tải dependencies với BuildKit cache mount
#    Maven local repo được cache trên host, không cần tải lại dù layer bị invalidate
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -B

# 3. Copy CHỈ source code của common-core (dependency chung) và service cần build
#    Thay vì COPY . . (invalidate cache khi bất kỳ file nào thay đổi)
COPY common-core/src common-core/src
COPY ${SERVICE_NAME}/src ${SERVICE_NAME}/src

# 4. Build module cụ thể với Maven cache mount
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -pl ${SERVICE_NAME} -am -DskipTests

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Cài curl để phục vụ Docker Healthcheck
RUN apk add --no-cache curl

WORKDIR /app
ARG SERVICE_NAME

# Copy file jar từ kết quả build phía trên
COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]