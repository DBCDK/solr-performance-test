/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of solr-perf-test-recorder
 *
 * solr-perf-test-recorder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * solr-perf-test-recorder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.solrperftest.recorder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OutputWriter implements AutoCloseable, Consumer<LogLine> {

    private static final Logger log = LoggerFactory.getLogger(OutputWriter.class);

    private final SortedSet<Entry> entries;
    private final OutputStream os;
    private final int orderBufferSize;
    private final long expire;
    private final long limit;

    private Instant origin;
    private long lastEntryTimeOffset;
    private long count;
    private boolean completed;

    public OutputWriter(OutputStream os, int orderBufferSize, long expire, long limit) {
        this.entries = new TreeSet<>();
        this.lastEntryTimeOffset = -600_000L;
        this.os = os;
        this.orderBufferSize = orderBufferSize;
        this.expire = expire;
        this.limit = limit;
        this.count = 0;
        this.completed = false;
        log.debug("orderBufferSize = {}", orderBufferSize);
        log.debug("expire = {}", expire);
        log.debug("limit = {}", limit);
    }

    @Override
    public void close() {
        try {
            if (!completed) {
                Iterator<Entry> i = entries.iterator();
                while (i.hasNext()) {
                    outputFromIterator(i.next());
                }
            }
        } finally {
            try {
                os.close();
            } catch (IOException ex) {
                log.error("Error closing output: {}", ex.getMessage());
                log.debug("Error closing output: ", ex);
            }
        }
    }

    @Override
    public void accept(LogLine t) {
        if (entries.isEmpty())
            this.origin = t.getInstant();
        long currentOffset = t.timeOffsetMS(origin);
        entries.add(new Entry(currentOffset, t.getQuery()));
        if (entries.size() > orderBufferSize) {
            Iterator<Entry> i = entries.iterator();
            outputFromIterator(i.next());
            i.remove();
        }
    }

    private void outputFromIterator(Entry entry) {
        long entryTimeOffset = entry.getTimeOffset();
        if (entryTimeOffset > expire)
            throw new CompletedException();
        if (entryTimeOffset < lastEntryTimeOffset) {
            log.warn("Buffered output is out of order, increase buffer size? (outputted={}, next={})", lastEntryTimeOffset, entryTimeOffset);
        } else {
            lastEntryTimeOffset = entryTimeOffset;
            entry.outputTo(os);
            if (count++ >= limit) {
                this.completed = true;
                throw new CompletedException();
            }
            log.debug("count = {}", count);
        }
    }

    private static class Entry implements Comparable<Entry> {

        private final long timeOffset;
        private final String content;

        public Entry(long timeOffset, String content) {
            this.timeOffset = timeOffset;
            this.content = content;
        }

        @Override
        public int compareTo(Entry t) {
            int ret = Long.compare(timeOffset, t.timeOffset);
            if (ret == 0)
                ret = content.compareTo(t.content);
            return ret;
        }

        public long getTimeOffset() {
            return timeOffset;
        }

        private void outputTo(OutputStream os) {
            byte[] line = new StringBuilder()
                    .append(timeOffset)
                    .append(" ")
                    .append(content)
                    .append("\n")
                    .toString()
                    .getBytes(StandardCharsets.UTF_8);
            try {
                os.write(line);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
