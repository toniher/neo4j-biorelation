#!/usr/bin/env python
import py2neo
from py2neo.packages.httpstream import http
from py2neo.cypher import cypher_escape
from multiprocessing import Pool

import httplib

import csv
import logging
import argparse
import pprint

httplib.HTTPConnection._http_vsn = 10
httplib.HTTPConnection._http_vsn_str = 'HTTP/1.0'

parser = argparse.ArgumentParser()
parser.add_argument("termfile",
                    help="The term.txt file as downloaded from the gene ontology site")
parser.add_argument("termdeffile",
                    help="The term_definition.txt file as downloaded from the gene ontology site")
parser.add_argument("term2termfile",
                    help="The term2term.txt file as downloaded from the gene ontology site")

opts=parser.parse_args()

logging.basicConfig(level=logging.ERROR)

graph = py2neo.Graph()
graph.bind("http://localhost:7474/db/data/")

relationshipmap={}
definition_list={}

http.socket_timeout = 9999

numiter = 5000

label = "GO_TERM"

idxout = graph.cypher.execute("CREATE CONSTRAINT ON (n:"+label+") ASSERT n.acc IS UNIQUE")
idxout = graph.cypher.execute("CREATE CONSTRAINT ON (n:"+label+") ASSERT n.id IS UNIQUE")

logging.info('adding definitions')
reader = csv.reader(open(opts.termdeffile),delimiter="\t")

for row in reader:
	
	definition = row[1]
	#definition = definition.replace("'", "\\'")
	definition = definition.replace('"', '\\"')
	definition_list[str(row[0])] = definition


def process_statement( statements ):
    
    tx = graph.cypher.begin()

    #print statements
    logging.info('proc sent')

    for statement in statements:
        #print statement
        tx.append(statement)

    tx.process()
    tx.commit()

poolnum = 4;

p = Pool(poolnum)

def create_go_term(line):
	if(line[6]=='1'):
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

res = p.map( process_statement, list_statements )


logging.info('adding relationships')
reader = csv.reader(open(opts.term2termfile),delimiter="\t")


iter = 0
list_statements =  []
statements = []

for row in reader:

    statement = "MATCH (c:"+label+" {id:"+row[3]+"}), (p:"+label+" {id:"+row[2]+"}) CREATE (c)-[:"+relationshipmap[row[1]]+"]->(p)"
    statements.append( statement )


    iter = iter + 1
    if ( iter > numiter ):
        list_statements.append( statements )
        iter = 0
        statements = []


#We force only one worker, fails if relation
p = Pool(1)

list_statements.append( statements )
res = p.map( process_statement, list_statements )

