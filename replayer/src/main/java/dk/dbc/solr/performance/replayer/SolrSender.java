
/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of solr-performance-test
 *
 * solr-performance-test is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * solr-performance-test is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * File created: 14/03/2019
 */
package dk.dbc.solr.performance.replayer;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Accept log-lines and send the contained query to a solr instance.
 * Record the execution time, and record the result
 */
public class SolrSender {
    private static final Logger log = LoggerFactory.getLogger(SolrSender.class);

    private SolrClient solrClient;
    private LogCollector logCollector;


    /**
     * @param solrClient The solr client instance
     * @param collector A Log-collector
     */
    public SolrSender(SolrClient solrClient, LogCollector collector) {
        this.solrClient = solrClient;
        this.logCollector = collector;
    }

    /**
     * Send a query to solr and capture information about the request
     * @param logLine a Line from the recorded log
     * @return Duration of solr-call in ms
     */
    public long send(LogLine logLine) {
        log.trace( "LogLine = " + logLine);

        LogCollector.LogEntry logEntry = new LogCollector.LogEntry();

        long callDuration = 0;
        final String q = logLine.getQuery();
        logEntry.setQuery(q);

        try {
            Timer.start();
            QueryResponse resp = solrClient.query(new SolrQuery(q));

            if (resp.getStatus() != 0) {
                log.error( "Got non-zero status({}) from solr on query: {}", resp.getStatus(), q);
                logEntry.setStatus("Non-zero exit status from solr(" + resp.getStatus() + ")");
            }
        } catch (Exception e) {
            log.error("Exception from solrClient caught ({}) on query: {}", e.getMessage() , q);
            logEntry.setStatus("Exception from solr (" + e.getMessage() + ")");
        }

        finally {
            callDuration = Timer.end();
            logEntry.setCallDuration(callDuration);
            logCollector.addEntry(logEntry);

            log.info( "Call duration = {}ms", callDuration);
        }

        return callDuration;
    }
    private static class Timer {
        private static long startTS;

        public static void start() {
            startTS = System.currentTimeMillis();
        }

        public static long end() {
            return System.currentTimeMillis() - startTS;
        }
    }
}
