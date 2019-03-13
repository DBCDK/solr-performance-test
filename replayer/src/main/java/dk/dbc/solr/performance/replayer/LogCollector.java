package dk.dbc.solr.performance.replayer;
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
 * File created: 20/03/2019
 */

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collecor of status for program progression
 *
 * @author Mike Andersen (mran@dbc.dk)
 */
public class LogCollector {
    private List<LogEntry> log;
    private Map conf;

    public LogCollector() {
        log =  new ArrayList<LogEntry>(100);
        conf = new HashMap();
    }

    public void addConfig(Map conf) { this.conf = conf; }

    public void addStatusEntry(String status) {
        LogEntry e = new LogEntry();
        e.setStatus(status);
        this.log.add(e);
    }

    public void addEntry(LogEntry entry) {
        log.add(entry);
    }

    public void dump(OutputStream os) throws IOException {
        if(os == null)
            return;

        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));
        Map output = new HashMap();
        output.put("configuration", conf);
        output.put("loglines", log);

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(w, output);
    }


    public static LogEntry newEntry() {
        return new LogEntry();
    }


    public static class LogEntry {
        private long originalDelay;
        private long actualDelay;
        private long callDuration;
        private String query;
        private String status;
        private long timestamp;

        public LogEntry() {
            this.timestamp = System.currentTimeMillis();
            this.originalDelay = 0;
            this.actualDelay = 0;
            this.callDuration = 0;
            this.query = "";
            this.status = "";
        }

        public void setOriginalDelay(long originalDelay) {
            this.originalDelay = originalDelay;
        }

        public void setActualDelay(long actualDelay) {
            this.actualDelay = actualDelay;
        }

        public void setCallDuration(long callDuration) {
            this.callDuration = callDuration;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "LogEntry{" +
                    "  timestamp=" + timestamp +
                    ", originalDelay=" + originalDelay +
                    ", actualDelay=" + actualDelay +
                    ", callDuration=" + callDuration +
                    ", query='" + query + "'" +
                    ", status='" + status + "'" +
                    '}';
        }

        public long getOriginalDelay() {
            return originalDelay;
        }

        public long getActualDelay() {
            return actualDelay;
        }

        public long getCallDuration() {
            return callDuration;
        }

        public String getQuery() {
            return query;
        }

        public String getStatus() {
            return status;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
