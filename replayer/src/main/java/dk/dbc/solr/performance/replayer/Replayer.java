/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of solr-performance-test-recorder
 *
 * solr-performance-test-recorder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * solr-performance-test-recorder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.solr.performance.replayer;

import dk.dbc.solr.performance.LineSource;
import dk.dbc.solr.performance.LinesInputStream;
import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Replay the recorded solr queries against a solr instance
 * and record execution time and result.
 *
 * Various means of aborting the test exist through the config object (Command line)
 * These are
 * - Limit the number of queries sendt
 * - Limit the total time during test
 * - Abort if a query takes longer than x milliseconds
 *
 * @author Mike Andersen (mran@dbc.dk)
 */
public class Replayer {

    private static final Logger log = LoggerFactory.getLogger(Replayer.class);

    private final Config config;
    private SolrClient solrClient;

    public Replayer(Config config, SolrClient solrClient) {
        this.config = config;
        this.solrClient = solrClient;
    }

    /** Run the test, and record the result
     *
     */
    public void run() {
        LogCollector logCollector = new LogCollector();
        SolrSender sender = new SolrSender(config.getDuration(), config.getReplay(), config.getCutoff(), solrClient, logCollector);
        String input = config.getInput();

        if( ! fileExistsAndNotDir(input)) {
            log.error( "File "+ input + " does not exist or is not a file");
            return;
        }

        logCollector.addConfig(config.asMap());

        try (LineSource lineSource = getLineSource(input)) {
            lineSource.stream()
                    .map(LogLine::of)
                    .filter(LogLine::isValid)
                    .limit(config.getLimit())
                    .forEach(sender);
        } catch (CompletedException ex) {
            log.debug("Test stopped due to duration constraint (" + config.getDuration() +"ms)");
        } catch (IOException ex) {
            logCollector.addStatusEntry("Error processing input: {"+ ex.getMessage() + "}");
            log.error("Error processing input: {}", ex.getMessage());
            log.debug("Error processing input: ", ex);
        }

        try {
            logCollector.dump(getDestination(config.getOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean fileExistsAndNotDir(String input) {
        File f = new File(input);
        if( f.exists() &&  ! f.isDirectory() )
            return true;
        else
            return false;
    }

    private LineSource getLineSource(String input) throws FileNotFoundException {
        InputStream is = (InputStream)((input != null) ? new FileInputStream(input) : System.in);
        return new LinesInputStream(is, StandardCharsets.UTF_8);
    }

    private  OutputStream getDestination(String fileName) throws  FileNotFoundException {
        if(fileName == null)
            return null;
        else
            return (OutputStream) new FileOutputStream(fileName);
    }
}
