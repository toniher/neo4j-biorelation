package cat.cau.neo4j.biorelation.rest;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.Traversal;
import org.neo4j.graphdb.Direction;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.RelationshipExpander;
import org.neo4j.graphdb.traversal.*;
// import org.neo4j.kernel.ShortestPathsBranchCollisionDetector;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Expander;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Hashtable;

//ANCESTOR TAX -> 1
//


@Path("/parent")
public class ParentDistance {

	//ANCESTOR GO
	private Hashtable<String, String> getrootGO() {

		Hashtable<String, String> rootGO = new Hashtable<String, String>();
		rootGO.put("biological_process", "GO:0008150");
		rootGO.put("cellular_component", "GO:0005575");
		rootGO.put("molecular_function", "GO:0003674");
		
		return (rootGO);
	}

	@GET
	@Path("/helloworld")
	public String helloWorld() {
		return "Hello World!";
	}

	@GET
	@Path("/distance/go/{acc1}/{acc2}")
	public Response getCommonGODistance(@PathParam("acc1") String acc1, @PathParam("acc2") String acc2, @Context GraphDatabaseService db) throws IOException {
		
		Integer maxdistance = 1000;

		try (Transaction tx = db.beginTx()) {
			Index<Node> taxid = db.index().forNodes("GO_TERM");
			Node node1 = taxid.get( "acc", acc1 ).getSingle();
			Node node2 = taxid.get( "acc", acc2 ).getSingle();
			tx.success();
			
			// The relationships we will follow
			String[] relations = new String[2];
			relations[0] = "is_a";
			relations[1] = "part_of";
			maxdistance = shortestDistance( node1, node2, relations, 1000 );
			
		}

		JsonObject jsonObject = new JsonObject().add( "distance", maxdistance );
		String jsonStr = jsonObject.toString();

		return Response.ok( jsonStr, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/path/go/{acc1}/{acc2}")
	public Response getCommonGOPath(@PathParam("acc1") String acc1, @PathParam("acc2") String acc2, @Context GraphDatabaseService db) throws IOException {
		
		ArrayList<Long> pathNodes = new ArrayList<Long>();
		
		try (Transaction tx = db.beginTx()) {
			Index<Node> taxid = db.index().forNodes("GO_TERM");
			Node node1 = taxid.get( "acc", acc1 ).getSingle();

			// The relationships we will follow
			String[] relations = new String[2];
			relations[0] = "is_a";
			relations[1] = "part_of";
			
			if ( acc2.equals("root") ) {
				Node node2 = getBaseGO( node1, taxid );
				pathNodes = shortestPathNodes( node1, node2, null, 1000 );
			} else {
				Node node2 = taxid.get( "acc", acc2 ).getSingle();
				pathNodes = shortestPathNodes( node1, node2, null, 1000 );
			}
			
			tx.success();
			
		}

		String outputStr = "";
		
		try (Transaction tx = db.beginTx()) {
		
			JsonArray jsonArray = new JsonArray();
		
			for ( Long l: pathNodes ) {

				Node lNode = db.getNodeById( l );
				String lAcc = lNode.getProperty("acc").toString();
				String lType = lNode.getProperty("term_type").toString();
				String lName = lNode.getProperty("name").toString();
				String lDefinition = lNode.getProperty("definition").toString();

				JsonObject jsonObject = new JsonObject().add( "acc", lAcc ).add( "term_type", lType ).add( "name", lName ).add( "definition", lDefinition );
				jsonArray.add( jsonObject );
				
			}

			outputStr = jsonArray.toString();
		
		}
		
		return Response.ok(outputStr, MediaType.APPLICATION_JSON).build();

	}


	// LCA n nodes
	@GET
	@Path("/common/go/{list}")
	public Response getCommonGO(@PathParam("list") String list, @Context GraphDatabaseService db) throws IOException {

		// Two dimensional array we store all the pats
		ArrayList<ArrayList<Long>> pathNodes = new ArrayList<ArrayList<Long>>();
		// array we store the resulting path
		ArrayList<Long> pathResult = new ArrayList<Long>();

		String[] arrayIDs = list.split("-",-1); 

		try (Transaction tx = db.beginTx()) {
			Index<Node> taxid = db.index().forNodes("GO_TERM");

			// We should check if all acc are of the same type
			// We save already them in array
			Node[] arrayNodes = new Node[arrayIDs.length];
			String[] propsNodes = new String[arrayIDs.length];

			for (int i = 0; i < arrayIDs.length; i++) {

				arrayNodes[i] = taxid.get( "acc", arrayIDs[i] ).getSingle();
				propsNodes[i] = arrayNodes[i].getProperty("term_type").toString();

			}

			if ( allElementsTheSame( propsNodes ) ) {

				// We get GO of root
				Hashtable<String, String> rootGO = getrootGO();
				String rootGOacc = rootGO.get( propsNodes[0] );

				Node base = taxid.get("acc", rootGOacc ).getSingle();

				for (int i = 0; i < arrayNodes.length; i++) {
					pathNodes.add( shortestPathNodes( arrayNodes[i], base, null, 1000 ) );
				}

				//// First we assing the pathResult
				pathResult = pathNodes.get(0);
			}

			tx.success();
			
		}
		

		for ( int p = 1; p < pathNodes.size(); p++ ) {
			
			// The array to compare
			ArrayList<Long> pathCompare = pathNodes.get(p);
			
			boolean detectMatch = false;
			Integer rSize = pathResult.size();
			// listString += String.valueOf(rSize) + "@";
		
			for ( int n=0; n < rSize; n++ ) {
				
				// If already detected out
				if ( detectMatch ) {
					break;
				}
		
				Integer cSize = pathCompare.size();
				// listString += String.valueOf(cSize) + "||";
		
				for ( int c=0; c < cSize; c++ ) {
					if ( pathResult.get(n).equals( pathCompare.get(c) ) ) {
						// we got it. From c to the end is new pathResult
						pathResult.clear();
						for ( int m = c; m < cSize; m ++ ) {
							pathResult.add( pathCompare.get(m) );
						}
						detectMatch = true;
						break;
					}
				}
			}
		}

		//// Get first value
		String outputStr = "";
		Long lca = pathResult.get(0);
		try (Transaction tx = db.beginTx()) {
		
			Node lcaNode = db.getNodeById( lca );
			String lcaAcc = lcaNode.getProperty("acc").toString();
			String lcaType = lcaNode.getProperty("term_type").toString();
			String lcaName = lcaNode.getProperty("name").toString();
			String lcaDefinition = lcaNode.getProperty("definition").toString();

			JsonObject jsonObject = new JsonObject().add( "acc", lcaAcc ).add( "term_type", lcaType ).add( "name", lcaName ).add( "definition", lcaDefinition );
			outputStr = jsonObject.toString();
		
		}
		
		return Response.ok(outputStr, MediaType.APPLICATION_JSON).build();
		
	}

	@GET
	@Path("/distance/tax/{acc1}/{acc2}")
	public Response getCommonTaxDistance(@PathParam("acc1") String acc1, @PathParam("acc2") String acc2, @Context GraphDatabaseService db) throws IOException {
		
		Integer maxdistance = 1000;

		try (Transaction tx = db.beginTx()) {
			Index<Node> taxid = db.index().forNodes("TAXID");
			Node node1 = taxid.get( "id", acc1 ).getSingle();
			Node node2 = taxid.get( "id", acc2 ).getSingle();
			tx.success();
			
			maxdistance = shortestDistance( node1, node2, null, 1000 );
			
		}

		JsonObject jsonObject = new JsonObject().add( "distance", maxdistance );
		String jsonStr = jsonObject.toString();

		return Response.ok( jsonStr, MediaType.APPLICATION_JSON).build();
	}
	
	// Path between two nodes
	@GET
	@Path("/path/tax/{acc1}/{acc2}")
	public Response getCommonTaxPath(@PathParam("acc1") String acc1, @PathParam("acc2") String acc2, @Context GraphDatabaseService db) throws IOException {

		// Let's get root -> 1
		if ( acc2.equals("root") ) {
			acc2 = "1";
		}

		ArrayList<Long> pathNodes = new ArrayList<Long>();

		try (Transaction tx = db.beginTx()) {
			Index<Node> taxid = db.index().forNodes("TAXID");
			Node node1 = taxid.get( "id", acc1 ).getSingle();
			Node node2 = taxid.get( "id", acc2 ).getSingle();
			tx.success();
			
			pathNodes = shortestPathNodes( node1, node2, null, 1000 );
			
		}
		
		String outputStr = "";
		
		try (Transaction tx = db.beginTx()) {
		
			JsonArray jsonArray = new JsonArray();
		
			for ( Long l: pathNodes ) {

				Node lNode = db.getNodeById( l );
				Long lId = Long.parseLong( lNode.getProperty("id").toString() );
				String lScientific = lNode.getProperty("scientific_name").toString();
				String lRank = lNode.getProperty("rank").toString();
				
				JsonObject jsonObject = new JsonObject().add( "id", lId ).add( "scientific_name", lScientific ).add( "rank", lRank );
				jsonArray.add( jsonObject );
				
			}

			outputStr = jsonArray.toString();
		
		}
		
		return Response.ok(outputStr, MediaType.APPLICATION_JSON).build();
	}
	

	// LCA n nodes
	@GET
	@Path("/common/tax/{list}")
	public Response getCommonTax(@PathParam("list") String list, @Context GraphDatabaseService db) throws IOException {

		// Two dimensional array we store all the pats
		ArrayList<ArrayList<Long>> pathNodes = new ArrayList<ArrayList<Long>>();
		// array we store the resulting path
		ArrayList<Long> pathResult = new ArrayList<Long>();

		String[] arrayIDs = list.split("-",-1); 

		try (Transaction tx = db.beginTx()) {
			Index<Node> taxid = db.index().forNodes("TAXID");
			Node base = taxid.get("id", 1 ).getSingle();

			for (int i = 0; i < arrayIDs.length; i++) {
				Node nodeID = taxid.get( "id", arrayIDs[i] ).getSingle();
				pathNodes.add( shortestPathNodes( nodeID, base, null, 1000 ) );
			}
			tx.success();
			
		}
		
		//// First we assing the pathResult
		pathResult = pathNodes.get(0);
		
		for ( int p = 1; p < pathNodes.size(); p++ ) {
			
			// The array to compare
			ArrayList<Long> pathCompare = pathNodes.get(p);
			
			boolean detectMatch = false;
			Integer rSize = pathResult.size();
			// listString += String.valueOf(rSize) + "@";
		
			for ( int n=0; n < rSize; n++ ) {
				
				// If already detected out
				if ( detectMatch ) {
					break;
				}
		
				Integer cSize = pathCompare.size();
				// listString += String.valueOf(cSize) + "||";
		
				for ( int c=0; c < cSize; c++ ) {
					if ( pathResult.get(n).equals( pathCompare.get(c) ) ) {
						// we got it. From c to the end is new pathResult
						pathResult.clear();
						for ( int m = c; m < cSize; m ++ ) {
							pathResult.add( pathCompare.get(m) );
						}
						detectMatch = true;
						break;
					}
				}
			}
		}

		//// Get first value
		String outputStr = "";
		Long lca = pathResult.get(0);
		try (Transaction tx = db.beginTx()) {
		
			Node lcaNode = db.getNodeById( lca );
			Long lcaId = Long.parseLong( lcaNode.getProperty("id").toString() );
			String lcaScientific = lcaNode.getProperty("scientific_name").toString();
			String lcaRank = lcaNode.getProperty("rank").toString();
			JsonObject jsonObject = new JsonObject().add( "id", lcaId ).add( "scientific_name", lcaScientific ).add( "rank", lcaRank );
			outputStr = jsonObject.toString();
		
		}
		
		return Response.ok(outputStr, MediaType.APPLICATION_JSON).build();
		
	}

	private static boolean allElementsTheSame(String[] array) {
		if (array.length == 0) {
			return true;
		} else {
	
		String first = array[0];
		for (String element : array) {
			if (!element.equals(first)) {
				return false;
			}
		}
		return true;
		}
	}

	private Node getBaseGO( Node refNode, Index<Node> taxid ) {
	
		String property = refNode.getProperty("term_type").toString();

		// We get GO of root
		Hashtable<String, String> rootGO = getrootGO();
		String rootGOacc = rootGO.get( property );

		Node base = taxid.get("acc", rootGOacc ).getSingle();
		
		
		return base;
	}

	private Integer shortestDistance( Node source, Node target, String[] types, Integer depth ) {
		
		Integer maxdistance = 1000;
		
		Iterable<org.neo4j.graphdb.Path> ListPaths = shortestPath( source, target, null, 1000 );
		Iterator<org.neo4j.graphdb.Path> itr = ListPaths.iterator();
		
		while ( itr.hasNext() ) {
			
			Integer taxlength = itr.next().length();
			if ( taxlength < maxdistance ) {
				maxdistance = taxlength;
			}
		}
		
		return maxdistance;
	}

	private ArrayList<Long> shortestPathNodes( Node source, Node target, String[] types, Integer depth ) {
		
		Integer maxdistance = 1000;
		ArrayList<Long> pathNodes = new ArrayList<Long>();
		
		Iterable<org.neo4j.graphdb.Path> ListPaths = shortestPath( source, target, null, 1000 );
		Iterator<org.neo4j.graphdb.Path> itr = ListPaths.iterator();
		
		while ( itr.hasNext() ) {
			
			org.neo4j.graphdb.Path nodePath = itr.next();
			Integer taxlength = nodePath.length();
			if ( taxlength < maxdistance ) {
				maxdistance = taxlength;
				pathNodes.clear(); // Clear arrayList
				Iterable<Node> ListNodes = nodePath.nodes();
				
				pathNodes = nodes2Array( ListNodes ); 
			}
		}
		
		return pathNodes;
	}
	
	private ArrayList<Long> nodes2Array( Iterable<Node> ListNodes ) {
		
		ArrayList<Long> pathNodes = new ArrayList<Long>();
		
		Iterator<Node> itr = ListNodes.iterator();
		while ( itr.hasNext() ) {
			
			pathNodes.add( itr.next().getId() ); 
		}
		
		return pathNodes;
	}
	
	// Reference: http://neo4j.com/docs/stable/tutorials-java-embedded-graph-algo.html
	
	private Iterable<org.neo4j.graphdb.Path> shortestPath( Node source, Node target, String[] types, Integer depth ) {
		
		Expander expander;
		if ( types == null ) {
			expander = Traversal.expanderForAllTypes();
		}
		else {
			expander = Traversal.emptyExpander();
			for ( int i = 0; i < types.length; i++ ) {
				expander = expander.add( DynamicRelationshipType.withName( types[i] ) );
			}
		}
		PathFinder<org.neo4j.graphdb.Path> shortestPath = GraphAlgoFactory.shortestPath( expander, depth == null ? 4 : depth.intValue() );
		
		return shortestPath.findAllPaths( source, target );
	}

}
