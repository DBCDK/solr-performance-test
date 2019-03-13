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

import dk.dbc.Arguments;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

/**
 * Parameters as supplied on the command line
 *
 * @author Mike Andersen (mran@dbc.dk)
 */
public final class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private static Options options() {
        Options options = new Options();

        options.addOption(Option.builder("c")
                .longOpt("cutoff")
                .hasArg()
                .argName("CUTOFF")
                .desc("If calls to Solr takes longer than this, the replay will stop (default: 5s)")
                .build());

        options.addOption(Option.builder("d")
                .longOpt("duration")
                .hasArg()
                .argName("DURATION")
                .desc("Run duration ie. 15s or 3h (default: 1h)")
                .build());

        options.addOption(Option.builder("l")
                .longOpt("limit")
                .hasArg()
                .argName("NUM")
                .desc("Max number of lines in input")
                .build());

        options.addOption(Option.builder("s")
                .longOpt("solr")
                .hasArg()
                .argName("URL")
                .desc("Connect url (host[:port])")
                .build());

        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArg()
                .argName("FILE")
                .desc("File to read log lines from")
                .build());

        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg()
                .argName("FILE")
                .desc("File to write log lines to (absent means no output)")
                .build());

        options.addOption(Option.builder("r")
                .longOpt("replay")
                .hasArg()
                .argName("REPLAY")
                .desc("Replayspeed (ex. 110 is 110% slower than original speed, 0 means no delay between calls")
                .build());
        return options;
    }

    private static final String FOOTER =
            String.join("\n",
                        "Copyright (C) 2019 DBC A/S (http://dbc.dk/)");

    private final long duration;
    private final long cutoff;
    private final long limit;
    private final String solr;
    private final String input;
    private final String output;
    private final int replay;

    /**
     * Construct a configuration from (main) args
     *
     * @param args argument list as supplied from main
     * @return configuration
     */
    public static Config of(String... args) {
        return Arguments.parse(options(), FOOTER, Config::new, args);
    }

    public long getDuration() {
        return duration;
    }

    public long getCutoff() {
        return cutoff;
    }

    public long getLimit() {
        return limit;
    }

    public int getReplay() {
        return replay;
    }

    public String getSolr() {
        return solr;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    private Config(Arguments args, Iterator<String> positionalArguments) throws ParseException {
        if (positionalArguments.hasNext())
            throw new ParseException("Unexpected positional argument(s) at: " + positionalArguments.next());
        this.duration = args.take("d", "1h", t -> {
                              String[] parts = t.split("(?=[^0-9])", 2);
                              if (parts.length != 2)
                                  throw new RuntimeException();
                              long number = Long.parseUnsignedLong(parts[0]);
                              if (number < 1)
                                  throw new RuntimeException();
                              switch (parts[1].toLowerCase(Locale.ROOT)) {
                                  case "s":
                                      return Duration.ofSeconds(number).toMillis();
                                  case "m":
                                      return Duration.ofMinutes(number).toMillis();
                                  case "h":
                                      return Duration.ofHours(number).toMillis();
                                  case "d":
                                      return Duration.ofDays(number).toMillis();
                                  default:
                                      throw new RuntimeException();
                              }
                          });
        this.cutoff = args.take("c", "5m", t -> {
            String[] parts = t.split("(?=[^0-9])", 2);
            if (parts.length != 2)
                throw new RuntimeException();
            long number = Long.parseUnsignedLong(parts[0]);
            if (number < 1)
                throw new RuntimeException();
            switch (parts[1].toLowerCase(Locale.ROOT)) {
                case "s":
                    return Duration.ofSeconds(number).toMillis();
                case "m":
                    return Duration.ofMinutes(number).toMillis();
                case "h":
                    return Duration.ofHours(number).toMillis();
                case "d":
                    return Duration.ofDays(number).toMillis();
                default:
                    throw new RuntimeException();
            }
        });

        this.solr = args.take("s", null, t -> t);
        this.input = args.take("i", null, t -> t);
        this.output= args.take("o", null, t -> t);

        if(this.solr == null)
            throw new RuntimeException("Solr-URL is mandatory");

        this.limit = args.take("l", String.valueOf(Long.MAX_VALUE), t -> {
                           long value = Long.parseLong(t);
                           if (value < 1)
                               throw new RuntimeException("Number of lines needs to be at least 1");
                           return value;
                       });

        this.replay = args.take( "r", "100", t -> {
            int value = Integer.parseInt(t);
            if( value < 0 || value > 200)
                throw new RuntimeException("Replay speed needs to be between 0 and 200 (0 means no delay)");
            return value;
        });

        log.debug(this.toString());
    }

    @Override
    public String toString() {
        return "Config: " + asMap().toString();
    }

    public Map asMap() {
        return Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put( "duration", String.valueOf(duration));
                put( "cutoff", String.valueOf(cutoff));
                put( "limit", String.valueOf(limit));
                put( "solr", solr);
                put( "input", input);
                put( "output", output);
                put( "replay", String.valueOf(replay));
            }
        });
    }
}
