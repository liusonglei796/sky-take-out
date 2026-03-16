# 苍穹外卖 - Dockerfile (Spring Boot 4.0.0)

# ========== 构建阶段 ==========
FROM maven:3.9-eclipse-temurin-25-alpine AS builder

WORKDIR /build

# 复制 pom 文件先下载依赖（利用 Docker 缓存）
COPY pom.xml .
COPY sky-common/pom.xml sky-common/
COPY sky-pojo/pom.xml sky-pojo/
COPY sky-server/pom.xml sky-server/

# 下载依赖
RUN mvn dependency:go-offline -B

# 复制源代码
COPY sky-common/src sky-common/src
COPY sky-pojo/src sky-pojo/src
COPY sky-server/src sky-server/src
COPY sky-server/src/main/resources sky-server/src/main/resources

# 构建项目（Spring Boot 4.0.0 + Java 25）
RUN mvn clean package -DskipTests -B

# ========== 运行阶段 ==========
FROM eclipse-temurin:25-jre-alpine

LABEL maintainer="liusonglei796"
LABEL version="4.0.0"
LABEL description="苍穹外卖 Spring Boot 4.0.0"

# 时区配置
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# 创建用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 复制构建产物
COPY --from=builder /build/sky-server/target/sky-server-1.0-SNAPSHOT.jar app.jar

# 设置权限
RUN chown -R appuser:appgroup /app

# 切换用户
USER appuser

# 暴露端口（内部 8080）
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/doc.html || exit 1

# 启动命令（Spring Boot 4.0 + 虚拟线程）
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx512m", \
    "-jar", \
    "app.jar"]
