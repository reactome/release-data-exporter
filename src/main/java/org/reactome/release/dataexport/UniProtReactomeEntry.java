package org.reactome.release.dataexport;

import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import javax.annotation.Nonnull;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Class for describing UniProt entries in Reactome and their associated Reactome Events (i.e. Pathways and Reaction
 * Like Events).
 * @author jweiser
 */
public class UniProtReactomeEntry implements Comparable<UniProtReactomeEntry> {
	private final Pattern PARENT_ACCESSION_REGEX = Pattern.compile(
		"([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2})"
	); // As defined at https://www.uniprot.org/help/accession_numbers (October 2019)

	private static Map<Long, UniProtReactomeEntry> uniProtReactomeEntryMap = new HashMap<>();

	private static Map<Session, Map<UniProtReactomeEntry, Set<ReactomeEvent>>>
		uniprotReactomeEntryToTopLevelPathwaysCache = new HashMap<>();
	private static Map<Session, Map<UniProtReactomeEntry, Set<ReactomeEvent>>>
		uniprotReactomeEntryToReactomeEventCache = new HashMap<>();
	private static Map<Session, Map<UniProtReactomeEntry, Set<Long>>>
		uniprotReactomeEntryToReactionLikeEventIdCache = new HashMap<>();

	private static Logger logger = LogManager.getLogger();

	private long dbId;
	private String accession;
	private String displayName;
	private Set<ReactomeEvent> reactomeEvents;
	private Set<ReactomeEvent> topLevelPathways;

	/**
	 * Creates/retrieves UniProtReactomeEntry object
	 * @param uniprotDbId UniProt instance database identifier in Reactome
	 * @param uniprotAccession UniProt instance accession (e.g. "P01234")
	 * @param uniprotDisplayName UniProt instance display name in Reactome (e.g. "UniProt:P01234 GENE_NAME")
	 * @return UniProtReactomeEntry describing UniProt instance
	 */
	public static UniProtReactomeEntry get(long uniprotDbId, String uniprotAccession, String uniprotDisplayName) {
		UniProtReactomeEntry uniProtReactomeEntry = uniProtReactomeEntryMap.computeIfAbsent(
			uniprotDbId, k -> new UniProtReactomeEntry(uniprotDbId, uniprotAccession, uniprotDisplayName)
		);

		if (accessionOrNameMismatched(uniProtReactomeEntry, uniprotAccession, uniprotDisplayName)) {
			throw new IllegalArgumentException(getExceptionMessage(
				uniProtReactomeEntry, uniprotDbId, uniprotAccession, uniprotDisplayName
			));
		}

		return uniProtReactomeEntry;
	}

	/**
	 * Checks if the UniProt accession or display name of the UniProtReactomeEntry does not match the expected values
	 * @param uniProtReactomeEntry UniProtReactomeEntry to check for mis-matches
	 * @param uniprotAccession Expected UniProt accession
	 * @param uniprotDisplayName Expected UniProt display name
	 * @return <code>true</code> if either the accession or display name of the UniProtReactomeEntry object does not
	 * match the expected values and <code>false</code> otherwise
	 */
	private static boolean accessionOrNameMismatched(UniProtReactomeEntry uniProtReactomeEntry,
													 String uniprotAccession, String uniprotDisplayName) {
		return !uniProtReactomeEntry.getAccession().equals(uniprotAccession) ||
			   !uniProtReactomeEntry.getDisplayName().equals(uniprotDisplayName);
	}

	/**
	 * Generates the exception message for a UniProtReactomeEntry that has unexpected values
	 * @param uniProtReactomeEntry UniProtReactomeEntry with unexpected values
	 * @param dbId Expected UniProt instance Reactome database identifier
	 * @param uniprotAccession Expected UniProt accession
	 * @param uniprotDisplayName Expected UniProt display name
	 * @return Exception message for mis-matching values of a UniProtReactomeEntry and other expected values
	 */
	private static String getExceptionMessage(UniProtReactomeEntry uniProtReactomeEntry, long dbId,
											  String uniprotAccession, String uniprotDisplayName) {
		return String.join(System.lineSeparator(),
			"Cached UniProt Reactome Entry was " + uniProtReactomeEntry,
			" but passed values were ",
			String.join(System.lineSeparator(),
				"Db id: " + dbId,
				"Accession: " + uniprotAccession,
				"Display name: " + uniprotDisplayName
			)
		);
	}

	/**
	 * Creates UniProtReactomeEntry object for dbId, UniProt accession, and UniProt display name
	 * @param dbId UniProt database identifier in Reactome
	 * @param accession UniProt accession (e.g. P04367)
	 * @param displayName UniProt display name (e.g. UniProt:P04367 TP53)
	 */
	private UniProtReactomeEntry(long dbId, String accession, String displayName) {
		setDbId(dbId);
		setAccession(accession);
		setDisplayName(displayName);
	}

	/**
	 * Retrieves, from the graph database, a Map of UniProt accessions to the set of Top Level Pathways in
	 * which each UniProt accession participates
	 * @param graphDBSession Neo4J Driver Session object for querying the graph database
	 * @return Map of UniProt accessions to set of Reactome Events representing top level pathways in Reactome
	 * @deprecated As of data-exporter 1.2.0, use {@link #fetchUniProtReactomeEntryToTopLevelPathways(Session)} instead
	 * as the UniProt entry in Reactome is represented by the returned map's keys rather than only the UniProt accession
	 */
	@Deprecated
	public static Map<String, Set<ReactomeEvent>> fetchUniProtAccessionToTopLevelPathways(
		Session graphDBSession
	) {
		return fetchUniProtReactomeEntryToTopLevelPathways(graphDBSession)
			.entrySet()
			.stream()
			.collect(
				Collectors.toMap(
					entry -> entry.getKey().getAccession(),
					Map.Entry::getValue
				)
			);
	}

	/**
	 * Retrieves, from the graph database, a Map of UniProtReactomeEntry objects to the set of Top Level Pathways in
	 * which each UniProtReactomeEntry participates
	 * @param graphDBSession Neo4J Driver Session object for querying the graph database
	 * @return Map of UniProtReactomeEntry objects to set of Reactome Events representing top level pathways in
	 * Reactome
	 */
	public static Map<UniProtReactomeEntry, Set<ReactomeEvent>> fetchUniProtReactomeEntryToTopLevelPathways(
		Session graphDBSession
	) {
		if (uniprotReactomeEntryToTopLevelPathwaysCache.containsKey(graphDBSession)) {
			return uniprotReactomeEntryToTopLevelPathwaysCache.get(graphDBSession);
		}

		logger.info("Computing UniProt to Top Level Pathways");

		Map<UniProtReactomeEntry, Set<ReactomeEvent>> uniprotReactomeEntryToTopLevelPathways =
			fetchUniProtReactomeEntryToReactomeEvents(graphDBSession).entrySet().stream().collect(Collectors.toMap(
			Map.Entry::getKey,
			entry -> entry.getValue()
					.stream()
					.filter(reactomeEvent ->
						PathwayHierarchyUtilities
							.getTopLevelPathwayIds(graphDBSession)
							.contains(reactomeEvent.getDbId())
					)
					.collect(Collectors.toSet())
		));
		uniprotReactomeEntryToTopLevelPathwaysCache.put(graphDBSession, uniprotReactomeEntryToTopLevelPathways);

		logger.info("Finished computing UniProt to Top Level Pathways");

		return uniprotReactomeEntryToTopLevelPathways;
	}

	/**
	 * Retrieves, from the graph database, a Map of UniProt accessions to the set of events (both Pathways
	 * and Reaction Like Events) in which each UniProt accession participates
	 * @param graphDBSession Neo4J Driver Session object for querying the graph database
	 * @return Map of UniProt accessions to set of Reactome Events in Reactome
	 * @deprecated As of data-exporter 1.2.0, use {@link #fetchUniProtReactomeEntryToReactomeEvents(Session)} instead
	 * as the UniProt entry in Reactome is represented by the returned map's keys rather than only the UniProt accession
	 */
	@Deprecated
	public static Map<String, Set<ReactomeEvent>> fetchUniProtAccessionToReactomeEvents(
		Session graphDBSession
	) {
		return fetchUniProtReactomeEntryToReactomeEvents(graphDBSession)
			.entrySet()
			.stream()
			.collect(
				Collectors.toMap(
					entry -> entry.getKey().getAccession(),
					Map.Entry::getValue
				)
			);
	}

	/**
	 * Retrieves, from the graph database, a Map of UniProtReactomeEntry objects to the set of events (both Pathways
	 * and Reaction Like Events) in which each UniProtReactomeEntry participates
	 * @param graphDBSession Neo4J Driver Session object for querying the graph database
	 * @return Map of UniProtReactomeEntry objects to set of Reactome Events in Reactome
	 */
	public static Map<UniProtReactomeEntry, Set<ReactomeEvent>> fetchUniProtReactomeEntryToReactomeEvents(
		Session graphDBSession
	) {
		if (uniprotReactomeEntryToReactomeEventCache.containsKey(graphDBSession)) {
			return uniprotReactomeEntryToReactomeEventCache.get(graphDBSession);
		}

		logger.info("Computing UniProt to Reactome events");

		Map<Long, ReactomeEvent> eventCache = ReactomeEvent.fetchReactomeEventMap(graphDBSession);
		Map<UniProtReactomeEntry, Set<Long>> uniprotReactomeEntryToReactionLikeEventId =
			fetchUniProtReactomeEntryToRLEId(graphDBSession);
		Map<Long, Set<Long>> rleToPathwayId = PathwayHierarchyUtilities.fetchRLEIdToPathwayId(graphDBSession);

		AtomicInteger count = new AtomicInteger(0);
		Map<UniProtReactomeEntry, Set<ReactomeEvent>> uniprotReactomeEntryToReactomeEvent = new ConcurrentHashMap<>();
		uniprotReactomeEntryToReactionLikeEventId.keySet().stream().forEach(uniprotAccession -> {
			Set<Long> reactionLikeEventIds =
				uniprotReactomeEntryToReactionLikeEventId.computeIfAbsent(uniprotAccession, k -> new HashSet<>());

			// Add RLEs containing UniProt accession
			uniprotReactomeEntryToReactomeEvent
				.computeIfAbsent(uniprotAccession, k -> new HashSet<>())
				.addAll(reactionLikeEventIds.stream().map(eventCache::get).collect(Collectors.toSet()));

			for (long reactionLikeEventId : reactionLikeEventIds) {
				Set<Long> pathwayIds = rleToPathwayId.computeIfAbsent(reactionLikeEventId, k -> new HashSet<>());
				uniprotReactomeEntryToReactomeEvent
					.computeIfAbsent(uniprotAccession, k-> new HashSet<>())
					.addAll(pathwayIds.stream().map(eventCache::get).collect(Collectors.toSet()));
			}

			if (count.getAndIncrement() % 10000 == 0) {
				logger.info("Processed events for " + count.get() + " out of " +
					uniprotReactomeEntryToReactionLikeEventId.size() + " (" +
					String.format(
						"%.2f",
						(100 * (double) count.get()) / uniprotReactomeEntryToReactionLikeEventId.size()
					) + "%) " + LocalTime.now()
				);
			}
		});
		uniprotReactomeEntryToReactomeEventCache.put(graphDBSession, uniprotReactomeEntryToReactomeEvent);

		logger.info("Finished computing UniProt to Reactome events");

		return uniprotReactomeEntryToReactomeEvent;
	}

	/**
	 * Retrieves, from the graph database, a Map of UniProtReactomeEntry objects to the set of identifiers for
	 * Reaction Like Events in which each UniProt accession participates
	 * @param graphDBSession Neo4J Driver Session object for querying the graph database
	 * @return Map of UniProtReactomeEntry objects to set of database identifiers for Reaction Like Events in Reactome
	 */
	private static Map<UniProtReactomeEntry, Set<Long>> fetchUniProtReactomeEntryToRLEId(Session graphDBSession) {
		if (uniprotReactomeEntryToReactionLikeEventIdCache.containsKey(graphDBSession)) {
			return uniprotReactomeEntryToReactionLikeEventIdCache.get(graphDBSession);
		}

		logger.info("Computing UniProt to RLE id");

		StatementResult statementResult = graphDBSession.run(
			String.join(System.lineSeparator(),
				"MATCH (rgp:ReferenceGeneProduct)<-[:referenceEntity|:referenceSequence|:hasModifiedResidue]-" +
				"(ewas:EntityWithAccessionedSequence)<-[:hasComponent|:hasMember|:hasCandidate|:repeatedUnit" +
				"|:input|:output|:catalystActivity|:physicalEntity*]-(rle:ReactionLikeEvent)",
				"RETURN rgp.dbId, coalesce(rgp.variantIdentifier, rgp.identifier) as rgp_accession, "
					+ "rgp.displayName, rle.dbId",
				"ORDER BY rgp.identifier, rgp.variantIdentifier"
			)
		);

		Map<UniProtReactomeEntry, Set<Long>> uniprotReactomeEntryToReactionLikeEventId = new HashMap<>();
		while (statementResult.hasNext()) {
			Record record = statementResult.next();

			long uniprotDbId = record.get("rgp.dbId").asLong();
			String uniprotAccession = record.get("rgp_accession").asString();
			String uniprotDisplayName = record.get("rgp.displayName").asString();
			UniProtReactomeEntry uniProtReactomeEntry = UniProtReactomeEntry.get(
				uniprotDbId, uniprotAccession, uniprotDisplayName
			);

			long reactionLikeEventId = record.get("rle.dbId").asLong();

			uniprotReactomeEntryToReactionLikeEventId
				.computeIfAbsent(uniProtReactomeEntry, k -> new HashSet<>())
				.add(reactionLikeEventId);
		}

		uniprotReactomeEntryToReactionLikeEventIdCache.put(graphDBSession, uniprotReactomeEntryToReactionLikeEventId);

		logger.info("Finished computing UniProt to RLE id");

		return uniprotReactomeEntryToReactionLikeEventId;
	}

	/**
	 * Retrieves the database identifier in the Reactome database for the UniProt entry represented by the
	 * UniProtReactomeEntry instance
	 * @return Database identifier of UniProt entry in Reactome
	 */
	public long getDbId() { return this.dbId; }

	/**
	 * Retrieves the accession for the UniProt entry represented by the UniProtReactomeEntry instance
	 * @return UniProt accession value
	 */
	public String getAccession() {
		return this.accession;
	}

	/**
	 * Retrieves the display name in the Reactome database for the UniProt entry represented by the
	 * UniProtReactomeEntry instance
	 * @return Display name of UniProt entry in Reactome
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Sets the UniProt database identifier, from the Reactome database, for the UniProtReactomeEntry instance
	 * @param dbId Database identifier
	 */
	private void setDbId(long dbId) {
		this.dbId = dbId;
	}

	/**
	 * Sets the UniProt accession for the UniProtReactomeEntry instance
	 * @param accession UniProt accession
	 * @throws NullPointerException Thrown if the UniProt accession in null
	 * @throws IllegalArgumentException Thrown if the UniProt accession is not a properly formatted accession (i.e.
	 * a 6 or 10 character String)
	 */
	private void setAccession(String accession) {
		if (accession == null) {
			throw new NullPointerException("UniProt Accession is null");
		}

		if (!isValidCanonicalAccession(accession) && !isValidIsoformAccession(accession)) {
			throw new IllegalArgumentException(
				accession + " is not a proper UniProt accession.  Must be an alphanumeric string of length 6 or 10 " +
				"optionally followed by a dash and one or more digits if the UniProt accession is representing an " +
				"isoform"
			);
		}

		this.accession = accession;
	}

	/**
	 * Sets the UniProt display name, from the Reactome database, for the UniProtReactomeEntry instance
	 * @param displayName UniProt display name
	 * @throws NullPointerException Thrown if the UniProt display name in null
	 * @throws IllegalArgumentException Thrown if the UniProt display name does not begin with the prefix "UniProt:"
	 */
	private void setDisplayName(String displayName) {
		final String DISPLAY_NAME_PREFIX = "UniProt:";

		if (displayName == null) {
			throw new NullPointerException("UniProt Display Name is null");
		}

		if (!displayName.startsWith(DISPLAY_NAME_PREFIX)) {
			throw new IllegalArgumentException(
				displayName + " is not a proper UniProt Display Name.  Must start with " + DISPLAY_NAME_PREFIX
			);
		}

		this.displayName = displayName;
	}

	/**
	 * Checks if a passed String is a valid canonical UniProt accession (i.e. 6 or 10 alphanumeric characters matching
	 * the regular expression "[OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2}" as defined at
	 * https://www.uniprot.org/help/accession_numbers as of October 2019).
	 * E.g. P12345
	 * @param accession UniProt accession to test for validity
	 * @return true if the accession passed is a valid canonical (i.e. not an isoform) UniProt accession,
	 * false otherwise
	 */
	private boolean isValidCanonicalAccession(String accession) {
		final Pattern ANCHORED_PARENT_ACCESSION_REGEX = Pattern.compile("^" + PARENT_ACCESSION_REGEX.pattern() + "$");

		return ANCHORED_PARENT_ACCESSION_REGEX.matcher(accession).matches();
	}

	/**
	 * Checks if a passed String is a valid isoform UniProt accession (i.e. 6 or 10 alphanumeric characters matching
	 * the regular expression "[OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2}" as defined at
	 * https://www.uniprot.org/help/accession_numbers as of October 2019, and then followed by a dash and by any number
	 * of digits).  E.g. P12345-1
	 * @param accession UniProt accession to test for validity
	 * @return true if the accession passed is a valid UniProt accession for an isoform, false otherwise
	 */
	private boolean isValidIsoformAccession(String accession) {
		Pattern isoformSuffixRegex = Pattern.compile("-[0-9]+");
		Pattern isoformRegex = Pattern.compile(
			"^" + PARENT_ACCESSION_REGEX.pattern() + isoformSuffixRegex.pattern() + "$"
		);
		return isoformRegex.matcher(accession).matches();
	}

	/**
	 * Retrieves, from the graph database, the set of events (both Pathways and
	 * Reaction Like Events in which the UniProtReactomeEntry participates
	 * @param graphDBSession Neo4J Driver Session object for querying the graph database
	 * @return Set of Reactome Events in Reactome
	 */
	public Set<ReactomeEvent> getEvents(Session graphDBSession) {
		if (this.reactomeEvents == null) {
			this.reactomeEvents = fetchUniProtReactomeEntryToReactomeEvents(graphDBSession)
				.computeIfAbsent(this, k -> new HashSet<>());
		}

		return this.reactomeEvents;
	}

	/**
	 * Retrieves, from the graph database, the set of Top Level Pathways in which
	 * the UniProtReactomeEntry participates
	 * @param graphDBSession Neo4J Driver Session object for querying the graph database
	 * @return Set of Reactome Events representing top level pathways in Reactome
	 */
	public Set<ReactomeEvent> getTopLevelPathways(Session graphDBSession) {
		if (this.topLevelPathways == null) {
			this.topLevelPathways = fetchUniProtReactomeEntryToTopLevelPathways(graphDBSession)
				.computeIfAbsent(this, k -> new HashSet<>());
		}

		return this.topLevelPathways;
	}

	/**
	 * Compares UniProt accession values of this object and parameter
	 * @param obj UniProtReactomeEntry object to compare
	 * @return Value of String compare between this UniProt accession and the parameter's UniProt accession
	 */
	@Override
	public int compareTo(@Nonnull UniProtReactomeEntry obj) {
		return this.getAccession().compareTo(obj.getAccession());
	}

	/**
	 * Checks equality based on object type and value of UniProt db id, accession, and display name
	 * @param obj Object to check for equality with the calling UniProtReactomeEntry.
	 * @return <code>true</code> if the same object or a UniProtReactomeEntry object with the same UniProt db id,
	 * accession, and display name.  Returns <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof UniProtReactomeEntry)) {
			return false;
		}

		return ((UniProtReactomeEntry) obj).getDbId() == this.getDbId() &&
			   ((UniProtReactomeEntry) obj).getAccession().equals(this.getAccession()) &&
			   ((UniProtReactomeEntry) obj).getDisplayName().equals(this.getDisplayName());
	}

	/**
	 * Retrieves a hash code based on the object's set fields
	 * @return Hash code of UniProtReactomeEntry object
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getDbId(), getAccession(), getDisplayName());
	}

	/**
	 * Retrieves a String representation of the defining data of the UniProtReactomeEntry object
	 * @return String representation of UniProtReactomeEntry object
	 */
	@Override
	public String toString() {
		return "UniProtReactomeEntry: " + System.lineSeparator() +
			String.join(System.lineSeparator(),
				"Db id: " + getDbId(),
				"Accession: " + getAccession(),
				"Display name: " + getDisplayName()
			) + System.lineSeparator();
	}
}