#!/usr/bin/env python
from __future__ import print_function
import csv
import sys
import pprint

def main(argv):

        if len(sys.argv) < 2:
                sys.exit()

        termfile = sys.argv[1]
        termdeffile = sys.argv[2]

        definition_list={}

        reader = csv.reader(open(termdeffile),delimiter="\t")
        
        for row in reader:
            
            definition = row[1]
            definition = definition.replace('"', '\\"')
            definition_list[str(row[0])] = definition


        reader = csv.reader(open(termfile),delimiter="\t")
        
        print( "id:ID\tname\tterm_type\tacc\tdefinition" )

        
        for row in reader:
                print( row[0] + "\t" + row[1] + "\t" + row[2] + "\t" + row[3] + "\t", end='' )
                if row[0] in  definition_list :
                        print( definition_list[ row[0] ], end='' )
                print( "\n", end='' )
            


if __name__ == "__main__":
        main(sys.argv[1:])
