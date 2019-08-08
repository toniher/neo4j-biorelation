#!/usr/bin/env python
from __future__ import print_function
import csv
import sys
import pprint

def main(argv):

    if len(sys.argv) < 2:
            sys.exit()

    intactfile = sys.argv[1]

    reader = csv.reader(open(intactfile),delimiter="\t")
    
    for row in reader:
        
        # Interaction detection method(s)
        print( row[2] )

        # Interaction type
        
        # Confidence
        
        # Update date


if __name__ == "__main__":
    main(sys.argv[1:])

