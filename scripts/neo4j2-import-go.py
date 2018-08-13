#!/usr/bin/env python
from py2neo import Graph, Node, Relationship

import json
import csv
import logging
import argparse
import pprint

parser = argparse.ArgumentParser()
parser.add_argument("termfile",
                    help="The term.txt file as downloaded from the gene ontology site")
parser.add_argument("termdeffile",
                    help="The term_definition.txt file as downloaded from the gene ontology site")
parser.add_argument("term2termfile",
                    help="The term2term.txt file as downloaded from the gene ontology site")
parser.add_argument("config",
                    help="JSON configuration file", default=None, nargs='*')

opts=parser.parse_args()

conf = {}
conf["host"] = "localhost"
conf["port"] = 7687
conf["protocol"] = "bolt"

if len( opts.config ) > 0:
		with open(opts.config) as json_data_file:
				data = json.load(json_data_file)
				
				if data.has_key("neo4j"):
								if data["neo4j"].has_key("host"):
												conf["host"] = data["neo4j"]["host"]
								if data["neo4j"].has_key("protocol"):
												conf["protocol"] = data["neo4j"]["protocol"]
								if data["neo4j"].has_key("port"):
												conf["port"] = data["neo4j"]["port"]

server = conf["protocol"]+"://"+conf["host"]+":"+str( conf["port"] )

logging.basicConfig(level=logging.ERROR)

graph = Graph( server )

relationshipmap={}
definition_list={}

numiter = 5000

label = "GO_TERM"

idxout = graph.run("CREATE CONSTRAINT ON (n:"+label+") ASSERT n.acc IS UNIQUE")
idxout = graph.run("CREATE CONSTRAINT ON (n:"+label+") ASSERT n.id IS UNIQUE")

logging.info('adding definitions')
reader = csv.reader(open(opts.termdeffile),delimiter="\t")

for row in reader:
	
	definition = row[1]
	#definition = definition.replace("'", "\\'")
	definition = definition.replace('"', '\\"')
	definition_list[str(row[0])] = definition


def process_relationship( statements, graph ):
	
	tx = graph.begin()
	
	#print statements
	logging.info('proc sent')
	
	for statement in statements:
		#print statement
		start = graph.nodes.match(statement[0], id=int( statement[1] )).first()
		end = graph.nodes.match(statement[0], id=int( statement[2] )).first()
		rel = Relationship( start, statement[3], end )
		
		tx.create( rel )
	
	tx.process()
	tx.commit()


def process_statement( statements, graph ):
    
    tx = graph.begin()

    #print statements
    logging.info('proc sent')

    for statement in statements:
        #print statement
        tx.run(statement)

    tx.process()
    tx.commit()


def create_go_term(line):
	relationshipmap[line[0]]=line[1]
	goid = line[0]
	goacc = line[3]
	gotype = line[2]
	goname = line[1]
	goname = goname.replace('"', '\\"')

	defclause = ""
	if str(goid) in definition_list:
		defclause = ", definition: \""+definition_list[str(goid)]+"\""

	statement = "CREATE (n:"+label+" { id : "+goid+", acc : \""+goacc+"\", term_type: \""+gotype+"\", name: \""+goname+"\""+defclause+" })"

	return statement


logging.info('creating terms')

reader = csv.reader(open(opts.termfile),delimiter="\t")
iter = 0

list_statements =  []
statements = []

for row in reader:
    statement = create_go_term(row)
    statements.append( statement )
    iter = iter + 1
    if ( iter > numiter ):
        
        list_statements.append( statements )
        iter = 0
        statements = []
    

list_statements.append( statements )


print len( list_statements )

for statements in list_statements :
	process_statement( statements, graph )


logging.info('adding relationships')
reader = csv.reader(open(opts.term2termfile),delimiter="\t")


iter = 0
list_statements =  []
statements = []

for row in reader:

	statement = [ label, row[3], row[2], relationshipmap[row[1]] ]
	statements.append( statement )
	
	
	iter = iter + 1
	if ( iter > numiter ):
		list_statements.append( statements )
		iter = 0
		statements = []


#We force only one worker, fails if relation
list_statements.append( statements )

for statements in list_statements :
	process_relationship( statements, graph )

#res = p.map( process_statement, list_statements )

