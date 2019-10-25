#!/usr/bin/env python
from __future__ import print_function
import csv
import sys
import pprint

def main(argv):

        if len(sys.argv) < 2:
                sys.exit()

        termfile = sys.argv[1]
        term2termfile = sys.argv[2]

        relationshipmap={}

        reader = csv.reader(open(termfile),delimiter="\t")
        
        for row in reader:
            
            relationshipmap[str(row[0])] = row[1]


        reader = csv.reader(open(term2termfile),delimiter="\t")
        
        print( ":TYPE\tGO:START_ID\tGO:END_ID" )

        
        for row in reader:
                print( relationshipmap[ row[1] ] + "\t" + row[2] + "\t" + row[3] )
            


if __name__ == "__main__":
        main(sys.argv[1:])
