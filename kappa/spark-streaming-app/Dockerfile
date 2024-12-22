# Stage 1: Build the assembly JAR using sbt
FROM hseeberger/scala-sbt:11.0.5_1.3.6_2.13.1 as builder

# Set the working directory inside the container
WORKDIR /app

COPY kappa/build.sbt /app/
COPY kappa/project /app/project/
# Copy the sparkStreamingApp module
COPY kappa/spark-streaming-app /app/sparkStreamingApp

# Navigate to the sparkStreamingApp project and build the assembly JAR
RUN sbt "project sparkStreamingApp" clean assembly

# Stage 2: Create the runtime image
FROM openjdk:11-jre-slim

# Metadata
LABEL maintainer="amine@abdelaali.com"
LABEL description="spark-app Service" 

# Create a non-root user and group
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Set the working directory
WORKDIR /app

# Copy the assembly JAR from the builder stage
COPY --from=builder /app/sparkStreamingApp/target/scala-2.12/websocket-spark-streaming-assembly-0.1.jar app.jar

# Change ownership to the non-root user
RUN chown appuser:appgroup app.jar

# Switch to the non-root user
USER appuser
