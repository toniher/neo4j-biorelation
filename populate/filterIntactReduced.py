#!/usr/bin/env python
from __future__ import print_function
import csv
import sys
import pprint
import re

def main(argv):

    if len(sys.argv) < 2:
            sys.exit()

    intactfile = sys.argv[1]

    reader = csv.reader(open(intactfile),delimiter="\t")
    
    for row in reader:
        
        amol = row[0]
        bmol = row[1]
        method = ""
        intype = ""
        confidence = ""
        update = row[5]
        
        # Interaction detection method(s)
        match1 = re.findall( "psi-mi:\"(\S+)\"", row[2] )
        if len( match1 ) > 0 :
            method = match1[0]

        # Interaction type
        match2 = re.findall( "psi-mi:\"(\S+)\"", row[3] )
        if len( match2 ) > 0 :
            intype = match2[0]
            
        # Confidence
        match3 = re.findall( "intact\-miscore\:(\S+)", row[4] )
        if len( match3 ) > 0 :
            confidence = match3[0]
        
        if amol and bmol and update :
        
            print( amol + "\t" + bmol + "\t" + method + "\t" + intype + "\t" + confidence + "\t" + update )


if __name__ == "__main__":
    main(sys.argv[1:])

