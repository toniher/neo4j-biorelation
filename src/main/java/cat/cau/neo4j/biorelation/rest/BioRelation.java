package cat.cau.neo4j.biorelation.rest;

import org.neo4j.dbms.api.DatabaseManagementService;

import org.neo4j.graphdb.GraphDatabaseService;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Label;

import org.neo4j.graphdb.Transaction;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;

// Neo4j 5.26 ships jakarta.ws.rs-api 2.1.x, whose Java package is still
// javax.ws.rs (the package was renamed to jakarta.ws.rs only in the 3.0 API).
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;

@Path("/")
public class BioRelation {

	// Neo4j 5 injects the DatabaseManagementService; the extension operates on
	// the default database.
	private static final String DATABASE_NAME = "neo4j";

	@GET
	@Path("/helloworld")
	public String helloWorld() {
		return "Hello World!";
	}

	@GET
	@Path("/distance/{type}/{acc1}/{acc2}{dirout: (/[^/]+?)?}")
	public Response getCommonTaxDistance(@PathParam("type") String type, @PathParam("acc1") String acc1, @PathParam("acc2") String acc2, @PathParam("dirout") String dirout, @Context DatabaseManagementService dbms) throws IOException {

		GraphDatabaseService db = dbms.database( DATABASE_NAME );

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

		if ( type.equals( "go" ) ) {
			label = Label.label( "GO" );
			property = "acc";

		} else {
			label = Label.label( "TAXID" );
			property = "taxid";
			proptype = "int";

		}

		try (Transaction tx = db.beginTx()) {

			Node node1;
			Node node2;

			if ( proptype.equals( "int" ) ) {

				node1 = tx.findNode( label, property, Integer.parseInt( acc1 ) );
				node2 = tx.findNode( label, property, Integer.parseInt( acc2 ) );

			} else {

				node1 = tx.findNode( label, property, acc1 );
				node2 = tx.findNode( label, property, acc2 );
			}

			maxdistance = func.shortestDistance( node1, node2, maxdistance, type, direction, tx, db );

			tx.commit();

		}

		JsonObject jsonObject = new JsonObject().add( "distance", maxdistance ).add( "type", direction );
		String jsonStr = jsonObject.toString();

		return Response.ok( jsonStr, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/path/go/{acc1}/{acc2}")
	public Response getCommonGOPath(@PathParam("acc1") String acc1, @PathParam("acc2") String acc2, @Context DatabaseManagementService dbms) throws IOException {

		GraphDatabaseService db = dbms.database( DATABASE_NAME );

		ArrayList<Long> pathNodes = new ArrayList<Long>();

		Label label = Label.label( "GO" );
		String property = "acc";

		BioRelationFunction func = new BioRelationFunction();

		String outputStr = "";

		try (Transaction tx = db.beginTx()) {

			Node node1 = tx.findNode( label, property, acc1 );

			if ( acc2.equals("root") ) {
				String baseGO = func.getBaseGO( node1 );
				Node node2 = tx.findNode( label, property, baseGO );
				pathNodes = func.shortestPathNodes( node1, node2, 100, "go", "nodirection", tx, db );
			} else {
				Node node2 = tx.findNode( label, property, acc2 );
				pathNodes = func.shortestPathNodes( node1, node2, 100, "go", "nodirection", tx, db );
			}

			JsonArray jsonArray = new JsonArray();

			for ( Long l: pathNodes ) {

				Node lNode = tx.getNodeById( l );
				String lAcc = lNode.getProperty("acc").toString();
				String lType = lNode.getProperty("term_type").toString();
				String lName = lNode.getProperty("name").toString();
				String lDefinition = lNode.getProperty("definition").toString();

				JsonObject jsonObject = new JsonObject().add( "acc", lAcc ).add( "term_type", lType ).add( "name", lName ).add( "definition", lDefinition );
				jsonArray.add( jsonObject );

			}

			outputStr = jsonArray.toString();

			tx.commit();

		}

		return Response.ok(outputStr, MediaType.APPLICATION_JSON).build();

	}


	// LCA n nodes
	@GET
	@Path("/common/go/{list}")
	public Response getCommonGO(@PathParam("list") String list, @Context DatabaseManagementService dbms) throws IOException {

		GraphDatabaseService db = dbms.database( DATABASE_NAME );

		Label label = Label.label( "GO" );
		String property = "acc";

		// Two dimensional array we store all the pats
		ArrayList<ArrayList<Long>> pathNodes = new ArrayList<ArrayList<Long>>();
		// array we store the resulting path
		ArrayList<Long> pathResult = new ArrayList<Long>();

		BioRelationHelper helper = new BioRelationHelper();
		BioRelationFunction func = new BioRelationFunction();

		String[] arrayIDs = list.split("-",-1);

		String outputStr = "";

		try (Transaction tx = db.beginTx()) {

			// We should check if all acc are of the same type
			// We save already them in array
			Node[] arrayNodes = new Node[arrayIDs.length];
			String[] propsNodes = new String[arrayIDs.length];

			for (int i = 0; i < arrayIDs.length; i++) {

				arrayNodes[i] = tx.findNode( label, property, arrayIDs[i] );
				propsNodes[i] = arrayNodes[i].getProperty("term_type").toString();

			}

			if ( helper.allElementsTheSame( propsNodes ) ) {

				// We get GO of root
				Hashtable<String, String> rootGO = func.getRootGO();
				String rootGOacc = rootGO.get( propsNodes[0] );

				Node baseNode = tx.findNode( label, property, rootGOacc );

				for (int i = 0; i < arrayNodes.length; i++) {
					pathNodes.add( func.shortestPathNodes( arrayNodes[i], baseNode, 100, "go", "nodirection", tx, db ) );
				}

				//// First we assing the pathResult
				pathResult = pathNodes.get(0);
			}


			for ( int p = 1; p < pathNodes.size(); p++ ) {

				// The array to compare
				ArrayList<Long> pathCompare = pathNodes.get(p);

				boolean detectMatch = false;
				Integer rSize = pathResult.size();

				for ( int n=0; n < rSize; n++ ) {

					// If already detected out
					if ( detectMatch ) {
						break;
					}

					Integer cSize = pathCompare.size();

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
			Long lca = pathResult.get(0);

			Node lcaNode = tx.getNodeById( lca );
			String lcaAcc = lcaNode.getProperty("acc").toString();
			String lcaType = lcaNode.getProperty("term_type").toString();
			String lcaName = lcaNode.getProperty("name").toString();
			String lcaDefinition = lcaNode.getProperty("definition").toString();

			JsonObject jsonObject = new JsonObject().add( "acc", lcaAcc ).add( "term_type", lcaType ).add( "name", lcaName ).add( "definition", lcaDefinition );
			outputStr = jsonObject.toString();

			tx.commit();

		}

		return Response.ok(outputStr, MediaType.APPLICATION_JSON).build();

	}

	// Path between two nodes
	@GET
	@Path("/path/tax/{acc1}/{acc2}")
	public Response getCommonTaxPath(@PathParam("acc1") String acc1, @PathParam("acc2") String acc2, @Context DatabaseManagementService dbms) throws IOException {

		GraphDatabaseService db = dbms.database( DATABASE_NAME );

		// Let's get root -> 1
		if ( acc2.equals("root") ) {
			acc2 = "1";
		}

		Label label = Label.label( "TAXID" );
		String property = "taxid";

		BioRelationFunction func = new BioRelationFunction();

		ArrayList<Long> pathNodes = new ArrayList<Long>();

		String outputStr = "";

		try (Transaction tx = db.beginTx()) {

			// Taxonomy in integer
			Node node1 = tx.findNode( label, property, Integer.parseInt( acc1 ) );
			Node node2 = tx.findNode( label, property, Integer.parseInt( acc2 ) );

			pathNodes = func.shortestPathNodes( node1, node2, 100, "tax", "nodirection", tx, db );

			JsonArray jsonArray = new JsonArray();

			for ( Long l: pathNodes ) {

				Node lNode = tx.getNodeById( l );
				Long lId = Long.parseLong( lNode.getProperty("taxid").toString() );
				String lScientific = lNode.getProperty("scientific_name").toString();
				String lRank = lNode.getProperty("rank").toString();

				JsonObject jsonObject = new JsonObject().add( "id", lId ).add( "scientific_name", lScientific ).add( "rank", lRank );
				jsonArray.add( jsonObject );

			}

			outputStr = jsonArray.toString();

			tx.commit();

		}

		return Response.ok(outputStr, MediaType.APPLICATION_JSON).build();
	}


	// LCA n nodes
	@GET
	@Path("/common/tax/{list}")
	public Response getCommonTax(@PathParam("list") String list, @Context DatabaseManagementService dbms) throws IOException {

		GraphDatabaseService db = dbms.database( DATABASE_NAME );

		Label label = Label.label( "TAXID" );
		String property = "taxid";

		// Two dimensional array we store all the pats
		ArrayList<ArrayList<Long>> pathNodes = new ArrayList<ArrayList<Long>>();
		// array we store the resulting path
		ArrayList<Long> pathResult = new ArrayList<Long>();

		String[] arrayIDs = list.split("-",-1);

		BioRelationFunction func = new BioRelationFunction();

		String outputStr = "";

		try (Transaction tx = db.beginTx()) {

			Node baseNode = tx.findNode( label, property, 1 );

			for (int i = 0; i < arrayIDs.length; i++) {
				Node nodeID = tx.findNode( label, property, Integer.parseInt( arrayIDs[i] ) );
				pathNodes.add( func.shortestPathNodes( nodeID, baseNode, 100, "tax", "nodirection", tx, db ) );
			}

			//// First we assing the pathResult
			pathResult = pathNodes.get(0);

			for ( int p = 1; p < pathNodes.size(); p++ ) {

				// The array to compare
				ArrayList<Long> pathCompare = pathNodes.get(p);

				boolean detectMatch = false;
				Integer rSize = pathResult.size();

				for ( int n=0; n < rSize; n++ ) {

					// If already detected out
					if ( detectMatch ) {
						break;
					}

					Integer cSize = pathCompare.size();

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
			Long lca = pathResult.get(0);

			Node lcaNode = tx.getNodeById( lca );
			Long lcaId = Long.parseLong( lcaNode.getProperty("id").toString() );
			String lcaScientific = lcaNode.getProperty("scientific_name").toString();
			String lcaRank = lcaNode.getProperty("rank").toString();
			JsonObject jsonObject = new JsonObject().add( "id", lcaId ).add( "scientific_name", lcaScientific ).add( "rank", lcaRank );
			outputStr = jsonObject.toString();

			tx.commit();
		}

		return Response.ok(outputStr, MediaType.APPLICATION_JSON).build();

	}

	@GET
	@Path("/leafnodes/go/{acc}")
	public Response getLeafNodesGO(@PathParam("acc") String value, @Context DatabaseManagementService dbms) throws IOException {

		GraphDatabaseService db = dbms.database( DATABASE_NAME );

		String labelStr = "GO";
		String property = "acc";

		BioRelationHelper helper = new BioRelationHelper();
		BioRelationFunction func = new BioRelationFunction();

		String outputStr = "";

		try (Transaction tx = db.beginTx()) {

			ArrayList<Node> leafNodes = func.getAllLeafNodes( labelStr, property, value, tx );
			JsonArray jsonArray = helper.arrayListNodes2JSON( leafNodes );

			outputStr = jsonArray.toString();

			tx.commit();
		}

		return Response.ok( outputStr, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/leafnodes/go/{acc}/distance")
	public Response getLeafNodesGODistance(@PathParam("acc") String value, @Context DatabaseManagementService dbms) throws IOException {

		GraphDatabaseService db = dbms.database( DATABASE_NAME );

		String labelStr = "GO";
		String property = "acc";

		BioRelationHelper helper = new BioRelationHelper();
		BioRelationFunction func = new BioRelationFunction();

		Label label = Label.label( labelStr );

		String outputStr = "";

		try (Transaction tx = db.beginTx()) {

			ArrayList<Node> leafNodes = func.getAllLeafNodes( labelStr, property, value, tx );

			Node queryNode = tx.findNode( label, property, value );

			ArrayList<Integer> distanceNodes = func.calcDistanceNodes( leafNodes, queryNode, "go", tx, db );

			JsonArray jsonArray = helper.arrayListNodes2JSONextraInt( leafNodes, distanceNodes, "distance" );

			outputStr = jsonArray.toString();

			tx.commit();

		}

		return Response.ok( outputStr, MediaType.APPLICATION_JSON).build();
	}


	// Let's handle list from here
	@GET
	@Path("/rels/{type}/{list}{method: (/[^/]+?)?}")
	public Response getRelations(@PathParam("type") String type, @PathParam("list") String list, @PathParam("method") String methStr, @Context DatabaseManagementService dbms) throws IOException {

		GraphDatabaseService db = dbms.database( DATABASE_NAME );

		String nodelabel = "MOL";
		String nodeproperty = "id"; // Array synonyms

		String label;
		String relproperty;

		if ( type.equals( "go" ) ) {
			label = "GO";
			relproperty = "has_go";
		} else {
			label = "TAXID";
			relproperty = "has_taxon";
		}

		String[] arrayAcc = list.split("-",-1);

		BioRelationHelper helper = new BioRelationHelper();
		BioRelationFunction func = new BioRelationFunction();

		ArrayList<Node> listNodes = new ArrayList<Node>();
		Hashtable<String, ArrayList<Node>> mapNodes = new Hashtable<String, ArrayList<Node>>();

		String method = "all";

		if ( methStr.startsWith("/") ) {
			method = methStr.replace("/", "");

			if ( method.equals("") ) {
				method = "all";
			}
		}

		String outputStr;

		try (Transaction tx = db.beginTx()) {

			for (int i = 0; i < arrayAcc.length; i++) {

				mapNodes.put( arrayAcc[i], func.getLinkedNodes( nodelabel, label, nodeproperty, arrayAcc[i], relproperty, "distinct", tx ) );

			}


			Enumeration<String> enumKey = mapNodes.keys();
			while(enumKey.hasMoreElements()) {
				String key = enumKey.nextElement();
				listNodes.addAll( mapNodes.get(key) );
			}

			// TODO more methods!
			if ( ! method.equals( "all" ) ) {

				if ( method.equals( "nonred" ) ) {
					// By default let's remove non redundant
					listNodes = helper.nonRedundantArrayNodeList( listNodes );
				}
				else if ( method.equals( "ancestor" ) ) {
					listNodes = helper.nonRedundantArrayNodeList( listNodes );
					listNodes = helper.ancestorArrayNodeList( listNodes, type );
				}
				else {

					int min = (int) Math.ceil( ( arrayAcc.length + 1 ) / 2 ); //TODO: This could have more stuff
					listNodes = helper.minArrayNodeList( listNodes, min );

				}

			}

			// For all listNodes
			// Get relationships, return according above

			if ( type.equals( "go" ) ) {
				JsonObject jsonRoot = helper.arrayListNodes2JSONkeyRoot( listNodes, "term_type" );
				outputStr = jsonRoot.toString();
			} else {
				JsonArray jsonRoot = helper.arrayListNodes2JSON( listNodes );
				outputStr = jsonRoot.toString();
			}

			tx.commit();

		}

		return Response.ok( outputStr, MediaType.APPLICATION_JSON).build();

	}

}
