/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.p3.geo.enriching.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import junit.framework.Assert;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.query.spatial.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.util.QueryExecUtils;
import com.hp.hpl.jena.tdb.TDBFactory;

import eu.fusepool.p3.geo.enriching.SpatialDataEnhancer;
import eu.fusepool.p3.geo.enriching.WGS84Point;

import org.junit.Test;

/**
 * Build a spatial search dataset
 */
public class JenaSpatialTest {

    final String TEST_DATASET = "spatial-data-latlong.ttl";    
    //final String TEST_DATASET_URI = "<spatial-data-latlong.ttl>";
    SpatialDataEnhancer jenas = null;
    
    static {
        LogCtl.setLog4j();
    }
    static Logger log = LoggerFactory.getLogger("JenaSpatialTest");

    @Before
    public void setUp() throws Exception {
        jenas = new SpatialDataEnhancer();
        URL testFile = getClass().getResource(TEST_DATASET);
        jenas.loadKnowledgeBase(jenas.getDataset(), testFile.getFile());
    }
    
    @Test
    public void testQueryKnowledgebase() throws Exception {
        
        queryData(jenas.getDataset());
    }
    
    @Test
    public void testGetPointList() throws IOException {
        TripleCollection graph = Parser.getInstance().parse(getClass().getResourceAsStream(TEST_DATASET), SupportedFormat.TURTLE);
        List<WGS84Point> pointList = jenas.getPointList(graph);
        Assert.assertTrue(pointList.size() > 0);
    }
    
    //@Test
    public void testImportKnowledgebase() throws Exception {
      final String sourceDataUrl = "https://raw.githubusercontent.com/fusepoolP3/p3-geo-enriching-transformer/master/src/test/resources/eu/fusepool/p3/geo/enriching/test/farmacie-trentino-grounded.ttl";
      InputStream in = jenas.importKnowledgebase(sourceDataUrl);
        
    }
    
    @Test
    public void testQueryNearby() throws Exception {
        WGS84Point point = new WGS84Point();
        point.setUri("http://geo.org/?lat=41.79,lon=12.24");
        point.setLat(41.79);
        point.setLong(12.24);
        TripleCollection pois = jenas.queryNearby(point);
        Assert.assertTrue(! pois.isEmpty());
    }
    
    
    private void queryData(Dataset spatialDataset) {
        log.info("START");
        long startTime = System.nanoTime();
        String pre = StrUtils.strjoinNL("PREFIX : <http://example/>",
                "PREFIX spatial: <http://jena.apache.org/spatial#>",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>");
        // NEARBY
        log.info("nearby");
        String qs = StrUtils.strjoinNL("SELECT * ",
                " { ?s spatial:nearby (41.79 12.24 10 'km') ;",
                "      rdfs:label ?label", " }");

        log.info(pre + "\n" + qs);
        spatialDataset.begin(ReadWrite.READ);
        try {
            Query q = QueryFactory.create(pre + "\n" + qs);
            QueryExecution qexec = QueryExecutionFactory.create(q, spatialDataset);
            QueryExecUtils.executeQuery(q, qexec);
        } finally {
            spatialDataset.end();
        }
        long finishTime = System.nanoTime();
        double time = (finishTime - startTime) / 1.0e6;
        log.info(String.format("FINISH - %.2fms", time));
        
        // WITHIN CIRCLE
        log.info("withinCircle");
        startTime = System.nanoTime();
        qs = StrUtils.strjoinNL("SELECT * ",
                " { ?s spatial:withinCircle (51.3000 -2.71000 100.0 'miles' 3) ;",
                "      rdfs:label ?label", " }");

        log.info(pre + "\n" + qs);
        spatialDataset.begin(ReadWrite.READ);
        try {
            Query q = QueryFactory.create(pre + "\n" + qs);
            QueryExecution qexec = QueryExecutionFactory.create(q, spatialDataset);
            QueryExecUtils.executeQuery(q, qexec);
        } finally {
            spatialDataset.end();
        }
        finishTime = System.nanoTime();
        time = (finishTime - startTime) / 1.0e6;
        log.info(String.format("FINISH - %.2fms", time));
         
        // WITHINBOX
        log.info("withinBox");
        startTime = System.nanoTime();
        qs = StrUtils.strjoinNL("SELECT * ",
                " { ?s spatial:withinBox (51.1000 -4.0000 51.4000 0.0000 -1) ;",
                "      rdfs:label ?label", " }");

        log.info(pre + "\n" + qs);
        spatialDataset.begin(ReadWrite.READ);
        try {
            Query q = QueryFactory.create(pre + "\n" + qs);
            QueryExecution qexec = QueryExecutionFactory.create(q, spatialDataset);
            QueryExecUtils.executeQuery(q, qexec);
        } finally {
            spatialDataset.end();
        }
        finishTime = System.nanoTime();
        time = (finishTime - startTime) / 1.0e6;
        log.info(String.format("FINISH - %.2fms", time));

        // INTERSECTBOX
        log.info("interesectBox");
        startTime = System.nanoTime();
        qs = StrUtils.strjoinNL("SELECT * ",
                " { ?s spatial:intersectBox (51.1000 -4.0000 51.4000 0.0000) ;",
                "      rdfs:label ?label", " }");

        log.info(pre + "\n" + qs);
        spatialDataset.begin(ReadWrite.READ);
        try {
            Query q = QueryFactory.create(pre + "\n" + qs);
            QueryExecution qexec = QueryExecutionFactory.create(q, spatialDataset);
            QueryExecUtils.executeQuery(q, qexec);
        } finally {
            spatialDataset.end();
        }
        finishTime = System.nanoTime();
        time = (finishTime - startTime) / 1.0e6;
        log.info(String.format("FINISH - %.2fms", time));

        // NORTH
        log.info("north");
        startTime = System.nanoTime();
        qs = StrUtils.strjoinNL("SELECT * ",
                " { ?s spatial:north (51.3000 0.0000) ;",
                "      rdfs:label ?label", " }");

        log.info(pre + "\n" + qs);
        spatialDataset.begin(ReadWrite.READ);
        try {
            Query q = QueryFactory.create(pre + "\n" + qs);
            QueryExecution qexec = QueryExecutionFactory.create(q, spatialDataset);
            QueryExecUtils.executeQuery(q, qexec);
        } finally {
            spatialDataset.end();
        }
        finishTime = System.nanoTime();
        time = (finishTime - startTime) / 1.0e6;
        log.info(String.format("FINISH - %.2fms", time));

        // SOUTH
        System.out.println("south");
        startTime = System.nanoTime();
        qs = StrUtils.strjoinNL("SELECT * ",
                " { ?s spatial:south (51.3000 0.0000) ;",
                "      rdfs:label ?label", " }");

        spatialDataset.begin(ReadWrite.READ);
        try {
            Query q = QueryFactory.create(pre + "\n" + qs);
            QueryExecution qexec = QueryExecutionFactory.create(q, spatialDataset);
            QueryExecUtils.executeQuery(q, qexec);
        } finally {
            spatialDataset.end();
        }
        finishTime = System.nanoTime();
        time = (finishTime - startTime) / 1.0e6;
        log.info(String.format("FINISH - %.2fms", time));

        // EAST
        System.out.println("east");
        startTime = System.nanoTime();
        qs = StrUtils.strjoinNL("SELECT * ",
                " { ?s spatial:east (51.3000 0.0000) ;",
                "      rdfs:label ?label", " }");

        log.info(pre + "\n" + qs);
        spatialDataset.begin(ReadWrite.READ);
        try {
            Query q = QueryFactory.create(pre + "\n" + qs);
            QueryExecution qexec = QueryExecutionFactory.create(q, spatialDataset);
            QueryExecUtils.executeQuery(q, qexec);
        } finally {
            spatialDataset.end();
        }
        finishTime = System.nanoTime();
        time = (finishTime - startTime) / 1.0e6;
        log.info(String.format("FINISH - %.2fms", time));

        // WEST
        System.out.println("west");
        startTime = System.nanoTime();
        qs = StrUtils.strjoinNL("SELECT * ",
                " { ?s spatial:west (51.3000 0.0000) ;",
                "      rdfs:label ?label", " }");

        spatialDataset.begin(ReadWrite.READ);
        try {
            Query q = QueryFactory.create(pre + "\n" + qs);
            QueryExecution qexec = QueryExecutionFactory.create(q, spatialDataset);
            QueryExecUtils.executeQuery(q, qexec);
        } finally {
            spatialDataset.end();
        }
        finishTime = System.nanoTime();
        time = (finishTime - startTime) / 1.0e6;
        log.info(String.format("FINISH - %.2fms", time));

        //WEST2
        System.out.println("west2");
        startTime = System.nanoTime();
        qs = StrUtils.strjoinNL("SELECT * ",
                " { ?s spatial:withinBox (51.1 -180.0000 51.9 0.0000) ;",
                "      rdfs:label ?label", " }");

        log.info(pre + "\n" + qs);
        spatialDataset.begin(ReadWrite.READ);
        try {
            Query q = QueryFactory.create(pre + "\n" + qs);
            QueryExecution qexec = QueryExecutionFactory.create(q, spatialDataset);
            QueryExecUtils.executeQuery(q, qexec);
        } finally {
            spatialDataset.end();
        }
        finishTime = System.nanoTime();
        time = (finishTime - startTime) / 1.0e6;
        log.info(String.format("FINISH - %.2fms", time));
    }

}
