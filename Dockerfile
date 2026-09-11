# --- Stage 1: build the WAR with Maven ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Stage 2: run on Tomcat ---
FROM tomcat:10.1-jdk17-temurin
# Deploy as ROOT so the app is served at "/" (Render gives one URL per service, no room for a
# context path); pageContext.request.contextPath in every JSP resolves to "" automatically.
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /build/target/canteen-webapp.war /usr/local/tomcat/webapps/ROOT.war

# Disable Tomcat's shutdown-command port (default 8005). It's how "shutdown.sh" tells a running
# Tomcat to stop — meaningless here since Docker/Render stop the container with SIGTERM instead —
# but left enabled it's a second listening port a PaaS's automatic port-detection can latch onto
# instead of 8080, sending it real HTTP traffic ("Invalid shutdown command [HEAD / HTTP/1.1]
# received" spamming the logs) instead of ever reaching the app. port="-1" stops Tomcat from
# opening it at all, so 8080 is the only port there is to find.
RUN sed -i 's/port="8005"/port="-1"/' /usr/local/tomcat/conf/server.xml

# No app.properties/db.properties baked into the image — DBConnection/AppConfig read DB_URL,
# DB_USERNAME, DB_PASSWORD, and any app.properties key (as UPPER_SNAKE_CASE) from env vars,
# which Render injects at runtime. See render-deploy.md for the required variables.

EXPOSE 8080
CMD ["catalina.sh", "run"]
