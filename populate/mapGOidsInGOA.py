#!/usr/bin/env python
from __future__ import print_function
import csv
import sys
import pprint

pp = pprint.PrettyPrinter(indent=4)


# Hashes for storing stuff
gomap={}

def main(argv):

		if len(sys.argv) < 2:
			sys.exit()
	
		gofile = sys.argv[1]
		goafile = sys.argv[2]
	
		reader =  csv.reader(open(gofile),delimiter="\t")

		for row in reader:
			goid = row[0]
			goacc = row[3]
			gomap[goacc] = goid

		
		readergoa =  csv.reader(open(goafile),delimiter="\t")

		for row in readergoa:
			goacc = row[2]
			
			if goacc in gomap :
				
				row[2] = gomap[goacc]
				
				print( "\t".join( row )  )
		
		

if __name__ == "__main__":
        main(sys.argv[1:])

