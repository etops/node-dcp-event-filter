#!/bin/bash
echo "Starting DCP-EVENT-FILTER..."

wait_for_start() {
    "$@"
    while [ $? -ne 0 ]
    do
        echo 'waiting for couchbase/elastic to start'
        sleep 10
        "$@"
    done
}

echo "waiting for couchbase ..."
wait_for_start curl -v $COUCHBASE_PORT_8091_TCP_ADDR:8091 -C -

java -cp  /dcp-event-filter/lib/*:/dcp-event-filter/DCPEventFilter.jar com.nectarfinancial.dcp_event_filter.dcp_event_filter.DCPEventFilter