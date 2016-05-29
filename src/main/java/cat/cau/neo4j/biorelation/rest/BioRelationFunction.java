package cat.cau.neo4j.biorelation.rest;

import org.neo4j.graphdb.GraphDatabaseService;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.DynamicLabel;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.ResourceIterator;

import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.DynamicRelationshipType;

import org.neo4j.graphdb.PathExpanders;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Expander;

import org.neo4j.helpers.collection.IteratorUtil;


import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Direction;

import java.io.IOException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Hashtable;


public class BioRelationFunction {

	//ANCESTOR TAX -> 1
	//

	//ANCESTOR GO
	public Hashtable<String, String> getRootGO() {

		Hashtable<String, String> rootGO = new Hashtable<String, String>();
		rootGO.put("biological_process", "GO:0008150");
		rootGO.put("cellular_component", "GO:0005575");
		rootGO.put("molecular_function", "GO:0003674");
		
		return (rootGO);
	}

	public ArrayList<Node> getAllLeafNodes( String label, String property, String value, GraphDatabaseService db ) {
	
	
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

	public ArrayList<Node> getAllLinkedNodes( String baselabel, String label, String property, String value, String relation, GraphDatabaseService db ) {
	
	
		String query = "MATCH (n:"+baselabel+")-["+relation+"]->(m:"+label+") where n."+property+" in "+value+" return distinct m;";
	
		ArrayList<Node> linkedNodes = new ArrayList<Node>();

		try ( Transaction tx = db.beginTx();
			Result result = db.execute( query )

		){
			Iterator<Node> node_column = result.columnAs( "m" );
			for ( Node node : IteratorUtil.asIterable( node_column ) ) {
				linkedNodes.add( node );
			}
	
			tx.success();
		
		}
	
		return linkedNodes;
	
	}
	
	public ArrayList<Integer> calcDistanceNodes( ArrayList<Node> arrayNodes, Node queryNode, String type ) {
	
		ArrayList<Integer> distances = new ArrayList<Integer>();
		
		Iterator<Node> nodeIterator = arrayNodes.iterator();
		while(nodeIterator.hasNext()){
			
			Node lNode = nodeIterator.next();
			
			Integer distance = shortestDistance( queryNode, lNode, 100, type, "nodirection" );
			distances.add( distance );
		}
		
		return distances;
	}


	public String getBaseGO( Node refNode ) {
	
		String property = refNode.getProperty("term_type").toString();
	
		// We get GO of root
		Hashtable<String, String> rootGO = getRootGO();
		String rootGOacc = rootGO.get( property );
		
		return rootGOacc;
	}

	public Integer shortestDistance( Node source, Node target, Integer depth, String type, String direction ) {
		
		PathFinder<org.neo4j.graphdb.Path> finder;
		
		if ( type.equals("go") ) {
		
			// The relationships we will follow
			RelationshipType isa = DynamicRelationshipType.withName( "is_a" );
			RelationshipType partof = DynamicRelationshipType.withName( "part_of" );
			
			if ( direction.equals( "direction" ) ) {	
				finder = GraphAlgoFactory.shortestPath( PathExpanders.forTypesAndDirections( isa, Direction.OUTGOING, partof, Direction.OUTGOING ), depth );
			} else {
				finder = GraphAlgoFactory.shortestPath( PathExpanders.forTypesAndDirections( isa, Direction.BOTH, partof, Direction.BOTH ), depth );
			}
		
		} else {
		
			// The relationships we will follow
			RelationshipType parent = DynamicRelationshipType.withName( "has_parent" );

			if ( direction.equals( "direction" ) ) {	
				finder = GraphAlgoFactory.shortestPath( PathExpanders.forTypeAndDirection( parent, Direction.OUTGOING ), depth );
			} else {
				finder = GraphAlgoFactory.shortestPath( PathExpanders.forType( parent ), depth );

			}
		}

		
		Iterable<org.neo4j.graphdb.Path> ListPaths = finder.findAllPaths( source, target );
		
		Iterator<org.neo4j.graphdb.Path> itr = ListPaths.iterator();
		
		while ( itr.hasNext() ) {
			
			Integer hoplength = itr.next().length();
			if ( hoplength < depth ) {
				depth = hoplength;
			}
		}
		
		return depth;
	}
	
	public ArrayList<Long> shortestPathNodes( Node source, Node target, Integer depth, String type, String direction ) {
		
		ArrayList<Long> pathNodes = new ArrayList<Long>();

		PathFinder<org.neo4j.graphdb.Path> finder;
		
		BioRelationHelper helper = new BioRelationHelper(); 
		
		if ( type.equals("go") ) {
		
			// The relationships we will follow
			RelationshipType isa = DynamicRelationshipType.withName( "is_a" );
			RelationshipType partof = DynamicRelationshipType.withName( "part_of" );
			
			if ( direction.equals( "direction" ) ) {	
				finder = GraphAlgoFactory.shortestPath( PathExpanders.forTypesAndDirections( isa, Direction.OUTGOING, partof, Direction.OUTGOING ), depth );
			} else {
				finder = GraphAlgoFactory.shortestPath( PathExpanders.forTypesAndDirections( isa, Direction.BOTH, partof, Direction.BOTH ), depth );
			}
		
		} else {
		
			// The relationships we will follow
			RelationshipType parent = DynamicRelationshipType.withName( "has_parent" );

			if ( direction.equals( "direction" ) ) {	
				finder = GraphAlgoFactory.shortestPath( PathExpanders.forTypeAndDirection( parent, Direction.OUTGOING ), depth );
			} else {
				finder = GraphAlgoFactory.shortestPath( PathExpanders.forType( parent ), depth );

			}
		}
		
		Iterable<org.neo4j.graphdb.Path> ListPaths = finder.findAllPaths( source, target );
		
		Iterator<org.neo4j.graphdb.Path> itr = ListPaths.iterator();
		
		while ( itr.hasNext() ) {
			
			org.neo4j.graphdb.Path nodePath = itr.next();
			Integer hoplength = nodePath.length();
			if ( hoplength < depth ) {
				depth = hoplength;
				pathNodes.clear(); // Clear arrayList
				Iterable<Node> ListNodes = nodePath.nodes();
				
				pathNodes = helper.nodes2Array( ListNodes ); 
			}
		}
		
		return pathNodes;
	}

}