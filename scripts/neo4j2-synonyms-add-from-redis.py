#!/usr/bin/env python

import csv
import logging
import argparse
import sys
import os.path
from pprint import pprint
import redis
import json


parser = argparse.ArgumentParser()
parser.add_argument("protein",
                    help="The protein file")


r = redis.StrictRedis(host='localhost' )

opts=parser.parse_args()

logging.basicConfig(level=logging.ERROR)


numiter = 1000

listinfo = []
listid = []

def process_synonyms( listid, listinfo ):
	
	vals = r.mget( listid )

	variter = 0
	for val in vals :
		if val is None:
			val = listid[ variter ]
			val = val.replace( " ", "" );
			val = val.replace( "\"", "" );
		print "\t".join( listinfo[ variter ] )+"\t"+ val
		variter = variter + 1


reader =  csv.reader(open(opts.protein),delimiter="\t")


for row in reader:

	listid.append( row[0] )
	listinfo.append( row )
	
	if ( len( listinfo ) > numiter  ) :
		process_synonyms( listid, listinfo )
		listinfo = []
		listid = []


process_synonyms( listid, listinfo )


