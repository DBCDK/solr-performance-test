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
 * File created: 19/03/2019
 */

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ReplayerIT {
    SolrClient solrClient;
    Config conf;

    private final int readTimeout = 1500;              // ms
    private final int fixedDelay  = readTimeout + 500; // ms


    @Rule
    public WireMockRule wireMockRule = ((Supplier<WireMockRule>)()-> {
        WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
        wireMockRule.start();
        return wireMockRule;
    }).get();


    @Before
    public void setUp() {
        solrClient= SolrClientFactory.makeSolrClient("http://localhost:" + wireMockRule.port());


        //essService = new EssService(conf.getSettings(), dropWizzardRule.getEnvironment().metrics(), solrClient );
    }

    @Test
    public void solrClientBaseFoundTest() throws Exception {
        stubFor(get(urlEqualTo("/select?q=.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type","application/octet-stream")
                        .withBody("")));
        //Replayer replayer = new Replayer(null, client);
        assertNotNull("solrClient should not be null",solrClient);
        QueryResponse result = solrClient.query(new SolrQuery("abc"));
        assertEquals(200, result.getStatus());
    }


}
