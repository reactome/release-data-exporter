package org.reactome.release.dataexport.testutils;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.logging.Level;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.internal.logging.JULogging;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DummyGraphDBServer {
	private static DummyGraphDBServer dummyGraphDBServer;

	private Session session;

	public static DummyGraphDBServer getInstance() {
		if (dummyGraphDBServer == null) {
			dummyGraphDBServer = new DummyGraphDBServer();
		}

		return dummyGraphDBServer;
	}

	public void initializeNeo4j() {
		ServerControls embeddedDatabaseServer = TestServerBuilders.newInProcessBuilder().newServer();
		this.session = GraphDatabase.driver(
			embeddedDatabaseServer.boltURI(),
			Config.build().withLogging(new JULogging(Level.OFF)).toConfig()
		).session();
	}

	public void populateDummyGraphDB() {
		final String TEST_GRAPHDB_CYPHER_DATA_FILE = "test_reactome_graphdb_content.txt";

		List<String> cypherStatements;

		try {
			Path cypherDataFilePath = Paths.get(
				Objects.requireNonNull(
					getClass()
					.getClassLoader()
					.getResource(TEST_GRAPHDB_CYPHER_DATA_FILE)
					.toURI()
				)
			);
			cypherStatements =
				Files.readAllLines(cypherDataFilePath);
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException("Populating dummy graph db failed", e);
		}

		StringBuilder query = new StringBuilder();
		cypherStatements.forEach(line -> query.append(line).append(System.lineSeparator()));

		getSession().run(query.toString());
	}

	public Session getSession() {
		return this.session;
	}
}
