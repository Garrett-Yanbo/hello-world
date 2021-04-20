############################
# STEP 1 构建可执行文件
############################

FROM harbor.tsingj.local/data-market/ubuntu:18.04newgradle as base
WORKDIR /src/app/privpy_sdk_java/
ENV GRADLE_HOME=/opt/gradle/gradle-6.3
ENV PATH=${GRADLE_HOME}/bin:${PATH} JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
COPY ./src /src/app/privpy_sdk_java/src
COPY ./gradlew /src/app/privpy_sdk_java/gradlew
COPY ./gradlew.bat /src/app/privpy_sdk_java/gradlew.bat
COPY ./settings.gradle /src/app/privpy_sdk_java/settings.gradle
COPY ./build.gradle /src/app/privpy_sdk_java/build.gradle
COPY ./gradle /src/app/privpy_sdk_java/gradle
RUN gradle jar  && apt-get update && apt-get -y install tar && \
    mv /src/app/privpy_sdk_java/build/libs/* /src/app/privpy_sdk_java/sdk_java.jar && \
    tar zcvf sdk_java.tar.gz sdk_java.jar && \
    rsync /src/app/privpy_sdk_java/sdk_java.tar.gz 10.18.0.17::gfs/nginx/data/html/download/




