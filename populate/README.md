Scripts for populating neo4j Docker instance

We run an instance that has not the daemon running:

docker run  -d -p 1337:1337 -p 7474:7474 -p 7687:7687 -p 7473:7473 --name neo4jbio -v /data/neo4j:/data -v /scratch/neo4j:/scratch neo4j tail -f /dev/null

Adjust variables in vars.sh

and run: ```bash ./dockerrun.sh```


