
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

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;


/**
 * Accept log-lines and send the contained query to a solr instance.
 * Record the execution time, and record the result
 */
public class SolrSender implements Consumer<LogLine> {
    private static final Logger log = LoggerFactory.getLogger(SolrSender.class);

    private long duration;
    private Instant timeStarted;
    private int replaySpeed;
    private long cutoff;
    private SolrClient solrClient;
    private LogCollector logCollector;


    /**
     * @param duration Maximum total time spend on test
     * @param replaySpeed Modifier for the original recorded time between calls (% of original time)
     * @param cutoff If calls to solr takes more than cutoff ms, abort the test
     * @param solrClient The solr client instance
     * @param collector A Log-collector
     */
    public SolrSender(long duration, int replaySpeed, long cutoff, SolrClient solrClient, LogCollector collector) {
        this.duration = duration;
        this.replaySpeed = replaySpeed;
        this.cutoff = cutoff;
        this.solrClient = solrClient;
        this.logCollector = collector;
        this.timeStarted = Instant.now();
    }

    @Override
    public void accept(LogLine logLine) {
        log.trace( "LogLine = " + logLine);

        LogCollector.LogEntry logEntry = new LogCollector.LogEntry();
        logCollector.addEntry(logEntry);

        long delay = logLine.getTimeDelta();
        long actualDelay = (long) (delay / 100.00 * replaySpeed);
        long callDuration = 0;
        final String q = logLine.getQuery();

        logEntry.setOriginalDelay(delay);
        logEntry.setActualDelay(actualDelay);
        logEntry.setQuery(q);

        if( timeOffsetMS(timeStarted, Instant.now()) > duration ) {
            logEntry.setStatus("Aborted due to timeduration exceeded!");
            throw new CompletedException();
        }

        try {
            log.info("Sleeping for {}ms", actualDelay);
            Thread.sleep(actualDelay);

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
            if( callDuration > cutoff )
                logCollector.addStatusEntry("Solr was taking more than " + cutoff +" to complete the call, aborting!!");

            log.info( "Call duration = {}ms", callDuration);
        }
    }

    /**
     * Get age in ms
     *
     * @param begin
     * @param end
     * @return milliseconds
     */
    public long timeOffsetMS(Instant begin, Instant end) {
        return Duration.between(begin, end).toMillis();
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
