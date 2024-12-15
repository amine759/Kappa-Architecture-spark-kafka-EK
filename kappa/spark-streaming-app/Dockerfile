FROM hseeberger/scala-sbt:11.0.5_1.3.6_2.13.1 as builder

# Set the working directory inside the container
WORKDIR /app

# Copy the root build file and project directory
COPY kappa/build.sbt /app/
COPY kappa/project /app/project/
COPY kappa/spark-streaming-app /app/spark-streaming-app/
# Build the assembly JAR for the spark-streaming app
RUN sbt -debug "project sparkStreamingApp" clean assembly
