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

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class Recorder {

    private static final Logger log = LoggerFactory.getLogger(Recorder.class);

    private final Config config;

    public Recorder(Config config) {
        this.config = config;
    }

    public void run() {
        try (OutputWriter outputWriter = getOutputWriter() ;
             LineSource lineSource = getLineSource()) {

            lineSource.stream()
                    .map(LogLine::of)
                    .filter(LogLine::isValid)
                    .forEach(outputWriter);
        } catch (CompletedException ex) {
            log.debug("Completed output");
        }
    }

    private OutputWriter getOutputWriter() {
        return new OutputWriter(System.out,
                                config.getSortBufferSize(),
                                config.getDuration(),
                                config.getLimit());
    }

    private LineSource getLineSource() {
        String kafka = config.getKafka();
        if (kafka == null) {
            return new LinesInputStream(System.in, StandardCharsets.UTF_8);
        } else {
            return new LinesKafka(kafka);
        }
    }

}
