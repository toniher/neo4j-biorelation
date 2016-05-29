package cat.cau.neo4j.biorelation.rest;

import org.neo4j.graphdb.GraphDatabaseService;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.DynamicLabel;

import org.neo4j.graphdb.Transaction;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class BioRelationHelper {

	public JsonArray arrayListNodes2JSON( ArrayList<Node> arrayNodes, GraphDatabaseService db ) {
		
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
						String value = lNode.getProperty( prop ).toString();
						
						if ( StringUtils.isNumeric( value ) ) {
							int valueInt = Integer.parseInt( value );
							double valueFloat = Float.parseFloat( value );
							
							if ( valueInt == valueFloat ) {
								jsonObject.add( prop, valueInt );
							} else {
								jsonObject.add( prop, valueFloat );
							}
						} else {
						
							jsonObject.add( prop, value );
						}
				}

				tx.success();
			}

			jsonArray.add( jsonObject );

		}
		
		return jsonArray;
	}


	public JsonArray arrayListNodes2JSONextraInt( ArrayList<Node> arrayNodes, ArrayList<Integer> arrayInteger, String extra, GraphDatabaseService db ) {
		
		JsonArray jsonArray = new JsonArray();
		
		Integer intIter = 0;
		
		Iterator<Node> nodeIterator = arrayNodes.iterator();
		while(nodeIterator.hasNext()){
			
			Node lNode = nodeIterator.next();

			JsonObject jsonObject = new JsonObject();

			try (Transaction tx = db.beginTx()) {

				Iterable<String> lNodeProps = lNode.getPropertyKeys();
				Iterator<String> itrProp = lNodeProps.iterator();
				while ( itrProp.hasNext() ) {

						String prop = itrProp.next();
						String value = lNode.getProperty( prop ).toString();
						
						if ( StringUtils.isNumeric( value ) ) {
							int valueInt = Integer.parseInt( value );
							double valueFloat = Float.parseFloat( value );
							
							if ( valueInt == valueFloat ) {
								jsonObject.add( prop, valueInt );
							} else {
								jsonObject.add( prop, valueFloat );
							}
						} else {
						
							jsonObject.add( prop, value );
						}
						
						// Adding extra arrayvalue
						jsonObject.add( extra, arrayInteger.get( intIter ) );
						
				}

				tx.success();
			}

			jsonArray.add( jsonObject );
			intIter++;

		}
		
		return jsonArray;
	}


	public static boolean allElementsTheSame(String[] array) {
		
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
	
	public ArrayList<Long> nodes2Array( Iterable<Node> ListNodes ) {
		
		ArrayList<Long> pathNodes = new ArrayList<Long>();
		
		Iterator<Node> itr = ListNodes.iterator();
		while ( itr.hasNext() ) {
			
			pathNodes.add( itr.next().getId() ); 
		}
		
		return pathNodes;
	}
	
}