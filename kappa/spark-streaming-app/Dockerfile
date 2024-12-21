# Stage 1: Build the assembly JAR using sbt
FROM hseeberger/scala-sbt:11.0.5_1.3.6_2.13.1 as builder

# Set the working directory inside the container
WORKDIR /app

COPY kappa/build.sbt /app/
COPY kappa/project /app/project/
# Copy the sparkStreamingApp module
COPY kappa/sparkStreamingApp /app/sparkStreamingApp

# Navigate to the sparkStreamingApp project and build the assembly JAR
RUN sbt "project sparkStreamingApp" clean assembly

# Validate that the JAR exists
RUN test -f /app/sparkStreamingApp/target/scala-2.12/spark-streaming-assembly-0.1.jar

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
COPY --from=builder /app/sparkStreamingApp/target/scala-2.12/spark-streaming-assembly-0.1.jar /opt/bitnami/spark/work/kafka-spark-streaming/target/scala-2.12/spark-streaming-assembly-0.1.jar

# Ensure proper ownership
RUN chown -R appuser:appgroup /opt/bitnami/spark/work

# Define the entrypoint to submit the Spark job
ENTRYPOINT ["/opt/bitnami/spark/bin/spark-submit", \
    "--packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0,org.elasticsearch:elasticsearch-spark-30_2.12:7.17.13", \
    "--master", "spark://spark-master:7077", \
    "--class", "sparkstreaming.SparkKafkaConsumer", \
    "/opt/bitnami/spark/work/kafka-spark-streaming/target/scala-2.12/spark-streaming-assembly-0.1.jar"]
