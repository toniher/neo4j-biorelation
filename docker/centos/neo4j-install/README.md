INSTALLATION

This installs the basic software. Data population happens in another step.

docker build -t neo4j-biorelation-install .

docker run -d -p 1337:1337 -p 7474:7474 -p 7687:7687 -p 7473:7473 --name neo4jbio -v /path/neo4j-data:/data -v /path/to/scratch:/scratch neo4j-biorelation-install

Data population details are in ```../neo4j-population directory```.


## Performance ##

Memory values for ```docker-entrypoint.sh``` are based from recommendations here: https://neo4j.com/docs/operations-manual/current/performance/memory-configuration/ using a 48GB dedicated machine.

Please adjust according to your system.

