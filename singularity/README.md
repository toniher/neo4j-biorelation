# Build and run

    sudo singularity build neo4j-3.5.6.sif Singularity


    singularity run -e -B $(pwd)/scratch:/scratch -B $(pwd)/neo4j.conf:/var/lib/neo4j/conf/neo4j.conf -B $(pwd)/db:/data -B $(pwd)/logs:/logs -B $(pwd)/certificates:/certificates neo4j-3.5.6.sif
