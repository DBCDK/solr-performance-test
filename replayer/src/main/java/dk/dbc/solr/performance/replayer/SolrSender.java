
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Accept log-lines and send the contained query to a solr instance.
 * Record the execution time, and record the result
 */
public class SolrSender {
    private static final Logger log = LoggerFactory.getLogger(SolrSender.class);

    private String baseUrl;
    private LogCollector logCollector;


    /**
     * @param baseUrl Base Solr url
     * @param collector A Log-collector
     */
    public SolrSender(String baseUrl, LogCollector collector) {
        this.baseUrl = baseUrl;
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
            URL url = new URL(baseUrl + "/select?" + q );
            HttpURLConnection solrClient= (HttpURLConnection) url.openConnection();
            solrClient.setRequestMethod("GET");

            Timer.start();
            solrClient.connect();
            int responseCode = solrClient.getResponseCode();

            if (responseCode != 200) {
                log.error( "Got non-zero status({}) from solr on query: {}", responseCode, q);
                logEntry.setStatus("Non-zero exit status from solr(" + responseCode + ")");
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
