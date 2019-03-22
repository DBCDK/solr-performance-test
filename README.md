# solr-performance-test
Tools to run a performance test of a solr cluster


## Tools

The applications (jar) that are produced are:

### Recorder

name: `solr-performance-test-recorder.jar`

This produces an output with lines that looks like this:

    delta query-string

the delta is number of ms between the 1st line in this file was requested, and
current line was requested. The query-string is the content posted to SolR

### Replayer

name: `solr-performance-test-replayer.jar`

This takes a file generated from solr-performance-test-recorder.jar and replays it
against a Solr instance. The queries sendt with the actual execution time is recorded 
as json in a file.
