/**
 * Copyright (C) 2014, The logback-contrib developers. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.contrib.mongodb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.qos.logback.contrib.mongodb.MongoDBAppenderBase;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

/**
 * Tests for {@link MongoDBAppenderBase}.
 * 
 * @author Christian Trutz
 * @since 0.1
 */
@RunWith(JMockit.class)
public class MongoDBAppenderBaseTest {

    // to be tested
    private MongoDBAppenderBase<DeferredProcessingAware> appender;

    @Test
    public void testNullURI() {
        // when uri == null
        appender.setUri(null);
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    @Test
    public void testNotValidURI() {
        // when uri is not valid
        appender.setUri("notvalid");
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    @Test
    public void testNullDatabase() {
        // when uri does not contain a database
        appender.setUri("mongodb://server");
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    @Test
    public void testNullCollection() {
        // when uri does not contain a collection
        appender.setUri("mongodb://server/database");
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    @Test
    public void testDatabaseAndCollectionOK() {
        // when uri is valid and complete
        appender.setUri("mongodb://server/database.collection");
        appender.start();
        // then appender should start
        assertTrue(appender.isStarted());
    }

    @Test
    public void testPasswordNull() {
        // when uri does not contain password but username
        appender.setUri("mongodb://username@server/database.collection");
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    // TODO is empty username in MongoDB allowed?
    @Test
    public void testEmptyUsername() {
        // when uri contains empty username
        appender.setUri("mongodb://:password@server/database.collection");
        appender.start();
        // then appender should start
        assertTrue(appender.isStarted());
        new Verifications() {
            {
                db.authenticate("", "password".toCharArray());
            }
        };
    }

    // TODO is empty password in MongoDB allowed?
    @Test
    public void testEmptyPassword() {
        // when uri contains empty password
        appender.setUri("mongodb://username:@server/database.collection");
        appender.start();
        // then appender should start
        assertTrue(appender.isStarted());
        new Verifications() {
            {
                db.authenticate("username", "".toCharArray());
            }
        };
    }

    @Test
    public void testUsernameAndPasswordOK() {
        // when uri contains username and password
        appender.setUri("mongodb://username:password@server/database.collection");
        appender.start();
        // then appender should start
        assertTrue(appender.isStarted());
        new Verifications() {
            {
                db.authenticate("username", "password".toCharArray());
            }
        };
    }

    @Test
    public void testAppendOK() {
        // when calling doAppend()
        appender.setUri("mongodb://username:password@server/database.collection");
        appender.start();
        appender.doAppend(event);
        // then invoke collection.insert(...)
        new Verifications() {
            {
                collection.insert(dbObject);
            }
        };
    }

    @Test
    public void testStop() {
        // when calling stop()
        appender.setUri("mongodb://username:password@server/database.collection");
        appender.start();
        appender.doAppend(event);
        appender.stop();
        // then close MongoDB connection and stop appender
        new Verifications() {
            {
                mongo.close();
            }
        };
        assertFalse(appender.isStarted());
    }

    //
    //
    // MOCKING
    //

    @Mocked
    private Mongo mongo;
    @Mocked
    private DB db;
    @Mocked
    private DBCollection collection;
    @Mocked
    private DeferredProcessingAware event;

    // this object will be inserted in MongoDB and represents an logging event
    private BasicDBObject dbObject = new BasicDBObject();

    @Before
    public void before() {
        appender = new MongoDBAppenderBase<DeferredProcessingAware>() {
            @Override
            protected BasicDBObject toMongoDocument(DeferredProcessingAware event) {
                return dbObject;
            }
        };
        appender.setContext(new ContextBase());
        new NonStrictExpectations() {
            {
                mongo.getDB("database");
                result = db;
                db.getCollection("collection");
                result = collection;
            }
        };
    }
}
