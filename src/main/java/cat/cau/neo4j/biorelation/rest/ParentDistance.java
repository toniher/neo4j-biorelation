package cat.cau.neo4j.biorelation.rest;

import org.neo4j.graphdb.GraphDatabaseService;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.DynamicLabel;

import org.neo4j.graphdb.Result;

import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.DynamicRelationshipType;

import org.neo4j.graphdb.PathExpanders;

import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Direction;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Expander;


import org.neo4j.helpers.collection.IteratorUtil;


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
import java.util.Map;

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
		
		Integer maxdistance = 100;
		
		Label label = DynamicLabel.label( "GO_TERM" );
		String property = "acc";

		try (Transaction tx = db.beginTx()) {
			
			Node node1 = db.findNode( label, property, acc1 );
			Node node2 = db.findNode( label, property, acc2 );

			tx.success();
			
			// The relationships we will follow
			RelationshipType isa = DynamicRelationshipType.withName( "is_a" );
			RelationshipType partof = DynamicRelationshipType.withName( "part_of" );
			
			Object[] relations = new Object[4];
			
			relations[0] = isa;
			relations[1] = Direction.OUTGOING;
			relations[2] = partof;
			relations[3] = Direction.OUTGOING;

			maxdistance = shortestDistance( node1, node2, 100, relations );
			
		}

		JsonObject jsonObject = new JsonObject().add( "distance", maxdistance );
		String jsonStr = jsonObject.toString();

		return Response.ok( jsonStr, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/path/go/{acc1}/{acc2}")
	public Response getCommonGOPath(@PathParam("acc1") String acc1, @PathParam("acc2") String acc2, @Context GraphDatabaseService db) throws IOException {
		
		ArrayList<Long> pathNodes = new ArrayList<Long>();
		
		Label label = DynamicLabel.label( "GO_TERM" );
		String property = "acc";
		
		try (Transaction tx = db.beginTx()) {
			
			Node node1 = db.findNode( label, property, acc1 );
	
			// The relationships we will follow
			RelationshipType isa = DynamicRelationshipType.withName( "is_a" );
			RelationshipType partof = DynamicRelationshipType.withName( "part_of" );
			
			Object[] relations = new Object[4];
			
			relations[0] = isa;
			relations[1] = Direction.OUTGOING;
			relations[2] = partof;
			relations[3] = Direction.OUTGOING;
			
			if ( acc2.equals("root") ) {
				String baseGO = getBaseGO( node1 );
				Node node2 = db.findNode( label, property, baseGO );
				pathNodes = shortestPathNodes( node1, node2, 100, relations );			
			} else {
				Node node2 = db.findNode( label, property, acc2 );
				pathNodes = shortestPathNodes( node1, node2, 100, relations );
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
	
		Label label = DynamicLabel.label( "GO_TERM" );
		String property = "acc";
		
		// Two dimensional array we store all the pats
		ArrayList<ArrayList<Long>> pathNodes = new ArrayList<ArrayList<Long>>();
		// array we store the resulting path
		ArrayList<Long> pathResult = new ArrayList<Long>();
	
		String[] arrayIDs = list.split("-",-1); 
	
		try (Transaction tx = db.beginTx()) {
			
			// We should check if all acc are of the same type
			// We save already them in array
			Node[] arrayNodes = new Node[arrayIDs.length];
			String[] propsNodes = new String[arrayIDs.length];
	
			for (int i = 0; i < arrayIDs.length; i++) {
	
				arrayNodes[i] = db.findNode( label, property, arrayIDs[i] );
				propsNodes[i] = arrayNodes[i].getProperty("term_type").toString();
	
			}
	
			// The relationships we will follow
			RelationshipType isa = DynamicRelationshipType.withName( "is_a" );
			RelationshipType partof = DynamicRelationshipType.withName( "part_of" );
			
			Object[] relations = new Object[4];
			
			relations[0] = isa;
			relations[1] = Direction.OUTGOING;
			relations[2] = partof;
			relations[3] = Direction.OUTGOING;
	
			if ( allElementsTheSame( propsNodes ) ) {
	
				// We get GO of root
				Hashtable<String, String> rootGO = getrootGO();
				String rootGOacc = rootGO.get( propsNodes[0] );
	
				Node baseNode = db.findNode( label, property, rootGOacc );
	
				for (int i = 0; i < arrayNodes.length; i++) {
					pathNodes.add( shortestPathNodes( arrayNodes[i], baseNode, 100, relations ) );
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
		
		Integer maxdistance = 100;
		
		Label label = DynamicLabel.label( "TAXID" );
		String property = "id";
		
		try (Transaction tx = db.beginTx()) {
			
			// Taxonomy in integer
			Node node1 = db.findNode( label, property, Integer.parseInt( acc1 ) );
			Node node2 = db.findNode( label, property, Integer.parseInt( acc2 ) );
			
			tx.success();
	
			// The relationships we will follow
			RelationshipType parent = DynamicRelationshipType.withName( "has_parent" );
			
			Object[] relations = new Object[2];
			
			relations[0] = parent;
			relations[1] = Direction.OUTGOING;
				
			maxdistance = shortestDistance( node1, node2, 100, relations );
			
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
	
		Label label = DynamicLabel.label( "TAXID" );
		String property = "id";
	
		ArrayList<Long> pathNodes = new ArrayList<Long>();
	
		try (Transaction tx = db.beginTx()) {

			// Taxonomy in integer
			Node node1 = db.findNode( label, property, Integer.parseInt( acc1 ) );
			Node node2 = db.findNode( label, property, Integer.parseInt( acc2 ) );
			
			tx.success();
			
			// The relationships we will follow
			RelationshipType parent = DynamicRelationshipType.withName( "has_parent" );
			
			Object[] relations = new Object[2];
			
			relations[0] = parent;
			relations[1] = Direction.OUTGOING;
			
			pathNodes = shortestPathNodes( node1, node2, 100, relations );
			
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
	
		Label label = DynamicLabel.label( "TAXID" );
		String property = "id";
	
		// Two dimensional array we store all the pats
		ArrayList<ArrayList<Long>> pathNodes = new ArrayList<ArrayList<Long>>();
		// array we store the resulting path
		ArrayList<Long> pathResult = new ArrayList<Long>();
	
		String[] arrayIDs = list.split("-",-1); 
	
		try (Transaction tx = db.beginTx()) {
			
			Node baseNode = db.findNode( label, property, 1 );
	
			// The relationships we will follow
			RelationshipType parent = DynamicRelationshipType.withName( "has_parent" );
			
			Object[] relations = new Object[2];
			
			relations[0] = parent;
			relations[1] = Direction.OUTGOING;
	
			for (int i = 0; i < arrayIDs.length; i++) {
				Node nodeID = db.findNode( label, property, Integer.parseInt( arrayIDs[i] ) );
				pathNodes.add( shortestPathNodes( nodeID, baseNode, 100, relations ) );
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
		
			tx.success();
		}
		
		return Response.ok(outputStr, MediaType.APPLICATION_JSON).build();
		
	}

	@GET
	@Path("/leafnodes/go/{acc}")
	public Response getCommonGOPath(@PathParam("acc") String value, @Context GraphDatabaseService db) throws IOException {
	
		String label = "GO_TERM";
		String property = "acc";

		ArrayList<Node> leafNodes = getAllLeafNodes( label, property, value, db );
		// TODO: Handle leafNodes
		JsonArray jsonArray = arrayListNodes2JSON( leafNodes );
		
		String outputStr = jsonArray.toString();
		return Response.ok( outputStr, MediaType.APPLICATION_JSON).build();
	}
	//
	//@GET
	//@Path("/leafnodes/go/{acc}/closest")
	//public Response getCommonGOPath(@PathParam("acc") String acc1, @Context GraphDatabaseService db) throws IOException {
	//
	//	return "To be implemented";
	//}
	//
	//@GET
	//@Path("/leafnodes/go/{acc}/farthest")
	//public Response getCommonGOPath(@PathParam("acc") String acc1, @Context GraphDatabaseService db) throws IOException {
	//
	//	return "To be implemented";
	//}
	//
	//@GET
	//@Path("/leafnodes/tax/{acc}")
	//public Response getCommonGOPath(@PathParam("acc") String acc1, @Context GraphDatabaseService db) throws IOException {
	//
	//	return "To be implemented";
	//}
	//
	//@GET
	//@Path("/leafnodes/tax/{acc}/closest")
	//public Response getCommonGOPath(@PathParam("acc") String acc1, @Context GraphDatabaseService db) throws IOException {
	//
	//	return "To be implemented";
	//}
	//
	//@GET
	//@Path("/leafnodes/tax/{acc}/farthest")
	//public Response getCommonGOPath(@PathParam("acc") String acc1, @Context GraphDatabaseService db) throws IOException {
	//
	//	return "To be implemented";
	//}


	private ArrayList<Node> getAllLeafNodes( String label, String property, String value, GraphDatabaseService db ) {
	
	
		String query = "MATCH (n:"+label+" { "+property+":'"+value+"' })<-[r*]-(m:"+label+") where not(()-->m) return distinct m;";
	
		ArrayList<Node> leafNodes = new ArrayList<Node>();

		try ( Transaction tx = db.beginTx();
			Result result = db.execute( query )

		){
			Iterator<Node> node_column = result.columnAs( "m" );
			for ( Node node : IteratorUtil.asIterable( node_column ) ) {
				leafNodes.add( node );
			}
	
			tx.success();
		
		}
	
		return leafNodes;
	
	}
	
	private JsonArray arrayListNodes2JSON( ArrayList<Node> arrayNodes ) {
		
		JsonArray jsonArray = new JsonArray();
		
		Iterator<Node> nodeIterator = arrayNodes.iterator();
		while(nodeIterator.hasNext()){
			
			Node lNode = nodeIterator.next();

			JsonObject jsonObject = new JsonObject();

			try (Transaction tx = db.beginTx()) {

				Iterable<String> lNodeProps = lNode.getPropertyKeys();
				Iterator<String> itrProp = lNodeProps.iterator();
				while ( itrProp.hasNext() ) {

						String prop = itrProp.next();
						// TODO: Handle if prop value is int or float
						jsonObject.add( prop, lNode.getProperty( prop ).toString() );
				}

				tx.success();
			}

			jsonArray.add( jsonObject );

		}
		
		return jsonArray;
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

	private String getBaseGO( Node refNode ) {
	
		String property = refNode.getProperty("term_type").toString();
	
		// We get GO of root
		Hashtable<String, String> rootGO = getrootGO();
		String rootGOacc = rootGO.get( property );
		
		return rootGOacc;
	}
	
	private Integer shortestDistance( Node source, Node target, Integer depth, Object... relations ) {
		
		Integer maxdistance = 100;

		// Temporary: Any type and direction
		PathFinder<org.neo4j.graphdb.Path> finder = GraphAlgoFactory.shortestPath( PathExpanders.allTypesAndDirections(), depth );
		//PathFinder<org.neo4j.graphdb.Path> finder = GraphAlgoFactory.shortestPath( PathExpanders.forTypesAndDirections( relations ), depth );
		
		Iterable<org.neo4j.graphdb.Path> ListPaths = finder.findAllPaths( source, target );
		
		Iterator<org.neo4j.graphdb.Path> itr = ListPaths.iterator();
		
		while ( itr.hasNext() ) {
			
			Integer taxlength = itr.next().length();
			if ( taxlength < maxdistance ) {
				maxdistance = taxlength;
			}
		}
		
		return maxdistance;
	}
	
	private ArrayList<Long> shortestPathNodes( Node source, Node target, Integer depth, Object... relations ) {
		
		Integer maxdistance = 100;
		ArrayList<Long> pathNodes = new ArrayList<Long>();
		
		// Temporary: Any type and direction
		PathFinder<org.neo4j.graphdb.Path> finder = GraphAlgoFactory.shortestPath( PathExpanders.allTypesAndDirections(), depth );
		//PathFinder<org.neo4j.graphdb.Path> finder = GraphAlgoFactory.shortestPath( PathExpanders.forTypesAndDirections( relations ), depth );
		
		Iterable<org.neo4j.graphdb.Path> ListPaths = finder.findAllPaths( source, target );
		
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

}
