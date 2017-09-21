FROM java:8

RUN mkdir /dcp-event-filter
WORKDIR /dcp-event-filter

# Copy the jar file and the libs folder
COPY target/dcp-event-filter-0.1.0-SNAPSHOT-jar-with-dependencies.jar /dcp-event-filter/DCPEventFilter.jar

# Copy the init script and make it runnable
COPY init.sh /ebics-connector/init.sh
RUN chmod +x /ebics-connector/init.sh

ENTRYPOINT ["/ebics-connector/init.sh"]