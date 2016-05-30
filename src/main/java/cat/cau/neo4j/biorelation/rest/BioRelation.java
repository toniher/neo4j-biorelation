package cat.cau.neo4j.biorelation.rest;

import org.neo4j.graphdb.GraphDatabaseService;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.DynamicLabel;

import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.DynamicRelationshipType;

import org.neo4j.graphdb.Transaction;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;

import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

@Path("/")
public class BioRelation {

	@GET
	@Path("/helloworld")
	public String helloWorld() {
		return "Hello World!";
	}

	@GET
	@Path("/distance/{type}/{acc1}/{acc2}{dirout: (/[^/]+?)?}")
	public Response getCommonTaxDistance(@PathParam("type") String type, @PathParam("acc1") String acc1, @PathParam("acc2") String acc2, @PathParam("dirout") String dirout, @Context GraphDatabaseService db) throws IOException {
		
		Integer maxdistance = 100;

		String direction;
		
		if ( dirout.equals("/direction") ) {
			direction = "direction";
		} else {
			direction = "nodirection";
		}

		Label label;
		String property;
		String proptype = "string";

		BioRelationFunction func = new BioRelationFunction();

		Object[] relations;

		if ( type.equals( "go" ) ) {
			label = DynamicLabel.label( "GO_TERM" );
			property = "acc";

		} else {
			label = DynamicLabel.label( "TAXID" );
			property = "id";
			proptype = "int";

		}
		
		try (Transaction tx = db.beginTx()) {
			
			Node node1;
			Node node2;

			if ( proptype.equals( "int" ) ) {
	
				node1 = db.findNode( label, property, Integer.parseInt( acc1 ) );
				node2 = db.findNode( label, property, Integer.parseInt( acc2 ) );
				
			} else {

				node1 = db.findNode( label, property, acc1 );
				node2 = db.findNode( label, property, acc2 );
			}

			tx.success();
				
			maxdistance = func.shortestDistance( node1, node2, maxdistance, type, direction );
			
		}
	
		JsonObject jsonObject = new JsonObject().add( "distance", maxdistance ).add( "type", direction );
		String jsonStr = jsonObject.toString();
	
		return Response.ok( jsonStr, MediaType.APPLICATION_JSON).build();
	}
	
	@GET
	@Path("/path/go/{acc1}/{acc2}")
	public Response getCommonGOPath(@PathParam("acc1") String acc1, @PathParam("acc2") String acc2, @Context GraphDatabaseService db) throws IOException {
		
		ArrayList<Long> pathNodes = new ArrayList<Long>();
		
		Label label = DynamicLabel.label( "GO_TERM" );
		String property = "acc";
		
		BioRelationFunction func = new BioRelationFunction();
		
		try (Transaction tx = db.beginTx()) {
			
			Node node1 = db.findNode( label, property, acc1 );
			
			if ( acc2.equals("root") ) {
				String baseGO = func.getBaseGO( node1 );
				Node node2 = db.findNode( label, property, baseGO );
				pathNodes = func.shortestPathNodes( node1, node2, 100, "go", "nodirection" );			
			} else {
				Node node2 = db.findNode( label, property, acc2 );
				pathNodes = func.shortestPathNodes( node1, node2, 100, "go", "nodirection" );
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
	
		BioRelationHelper helper = new BioRelationHelper();
		BioRelationFunction func = new BioRelationFunction();
	
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
	
			if ( helper.allElementsTheSame( propsNodes ) ) {
	
				// We get GO of root
				Hashtable<String, String> rootGO = func.getRootGO();
				String rootGOacc = rootGO.get( propsNodes[0] );
	
				Node baseNode = db.findNode( label, property, rootGOacc );
	
				for (int i = 0; i < arrayNodes.length; i++) {
					pathNodes.add( func.shortestPathNodes( arrayNodes[i], baseNode, 100, "go", "nodirection" ) );
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
	
		BioRelationFunction func = new BioRelationFunction();

		ArrayList<Long> pathNodes = new ArrayList<Long>();
	
		try (Transaction tx = db.beginTx()) {

			// Taxonomy in integer
			Node node1 = db.findNode( label, property, Integer.parseInt( acc1 ) );
			Node node2 = db.findNode( label, property, Integer.parseInt( acc2 ) );
			
			tx.success();
			
			pathNodes = func.shortestPathNodes( node1, node2, 100, "tax", "nodirection" );
			
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
		
		BioRelationFunction func = new BioRelationFunction();
	
		try (Transaction tx = db.beginTx()) {
			
			Node baseNode = db.findNode( label, property, 1 );
	
			for (int i = 0; i < arrayIDs.length; i++) {
				Node nodeID = db.findNode( label, property, Integer.parseInt( arrayIDs[i] ) );
				pathNodes.add( func.shortestPathNodes( nodeID, baseNode, 100, "tax", "nodirection" ) );
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
	public Response getLeafNodesGO(@PathParam("acc") String value, @Context GraphDatabaseService db) throws IOException {
	
		String labelStr = "GO_TERM";
		String property = "acc";
		
		BioRelationHelper helper = new BioRelationHelper();
		BioRelationFunction func = new BioRelationFunction();

		ArrayList<Node> leafNodes = func.getAllLeafNodes( labelStr, property, value, db );
		JsonArray jsonArray = helper.arrayListNodes2JSON( leafNodes, db );
		
		String outputStr = jsonArray.toString();
		return Response.ok( outputStr, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/leafnodes/go/{acc}/distance")
	public Response getLeafNodesGODistance(@PathParam("acc") String value, @Context GraphDatabaseService db) throws IOException {
	
		String labelStr = "GO_TERM";
		String property = "acc";
		
		BioRelationHelper helper = new BioRelationHelper(); 
		BioRelationFunction func = new BioRelationFunction();
		
		ArrayList<Node> leafNodes = func.getAllLeafNodes( labelStr, property, value, db );

		Label label = DynamicLabel.label( labelStr );
		Node queryNode;
		JsonArray jsonArray;

		try (Transaction tx = db.beginTx()) {
			
			queryNode = db.findNode( label, property, value );
				
			ArrayList<Integer> distanceNodes = func.calcDistanceNodes( leafNodes, queryNode, "go" );
	
			jsonArray = helper.arrayListNodes2JSONextraInt( leafNodes, distanceNodes, "distance", db );
	
			tx.success();
	
		}
		
		String outputStr = jsonArray.toString();
		return Response.ok( outputStr, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/rels/{type}/{list}")
	public Response getRelations(@PathParam("type") String type, @PathParam("list") String list, @Context GraphDatabaseService db) throws IOException {

		String nodelabel = "MOL";
		String nodeproperty = "id"; // Array synonyms

		String label;
		String relproperty;

		if ( type.equals( "go" ) ) {
			label = "GO_TERM";
			relproperty = "has_go";
		} else {
			label = "TAXID";
			relproperty = "has_taxon";
		}

		String[] arrayAcc = list.split("-",-1);
		
		BioRelationHelper helper = new BioRelationHelper(); 
		BioRelationFunction func = new BioRelationFunction();

		ArrayList<Node> listNodes = new ArrayList<Node>();

		// Prepare string of values
		String strValues;
		
		for (int i = 0; i < arrayAcc.length; i++) {
			arrayAcc[i] = "\"" + arrayAcc[i] + "\"";
		}
		
		strValues = "["  + StringUtils.join( arrayAcc, "," ) +  "]";

		listNodes = func.getAllLinkedNodes( nodelabel, label, nodeproperty, strValues, relproperty, "distinct", db );

		// For all listNodes
		// Get relationships, return according above
		JsonArray jsonArray = helper.arrayListNodes2JSON( listNodes, db );
		String outputStr = jsonArray.toString();
		
		return Response.ok( outputStr, MediaType.APPLICATION_JSON).build();

	}

	@GET
	@Path("/rels/common/{type}/{list}")
	public Response getCommonRelations(@PathParam("type") String type, @PathParam("list") String list, @Context GraphDatabaseService db) throws IOException {

		String nodelabel = "MOL";
		String nodeproperty = "id"; // Array synonyms

		String label;
		String relproperty;

		if ( type.equals( "go" ) ) {
			label = "GO_TERM";
			relproperty = "has_go";
		} else {
			label = "TAXID";
			relproperty = "has_taxon";
		}

		String[] arrayAcc = list.split("-",-1);
		
		BioRelationHelper helper = new BioRelationHelper(); 
		BioRelationFunction func = new BioRelationFunction();

		ArrayList<Node> listNodes = new ArrayList<Node>();
		Hashtable<String, ArrayList<Node>> commonNodes = new Hashtable<String, ArrayList<Node>>();

		// Prepare string of values
		String strValues;
		
		for (int i = 0; i < arrayAcc.length; i++) {
			arrayAcc[i] = "\"" + arrayAcc[i] + "\"";
		}
		
		strValues = "["  + StringUtils.join( arrayAcc, "," ) +  "]";

		listNodes = func.getAllLinkedNodes( nodelabel, label, nodeproperty, strValues, relproperty, "", db );

		if ( type.equals( "go" ) ) {
			// Iterate by term_type
			commonNodes = func.getCommonNodesSet( listNodes, "term_type", "method", db );
		} else {
			commonNodes.put( "tax", listNodes );
		}

		// For all listNodes
		// Get relationships, return according above
		JsonObject jsonResult = helper.hashMapNodes2JSON( commonNodes, db );
		String outputStr = jsonResult.toString();
		
		return Response.ok( outputStr, MediaType.APPLICATION_JSON).build();

	}

}
