#!/bin/sh
cd "$(dirname "$0")"

#harvested collections from CLARIN from https://vlo.clarin.eu/resultsets/

HARVESTER_URL=https://vlo.clarin.eu/resultsets
RESULTSETS=(clarin.tar.bz2 others.tar.bz2)

CMDI_PATH=results/cmdi

#delete old data

echo "deleting old CMDI records ..."
mkdir empty_dir
rsync -a --delete empty_dir/  cmdi/


#donwload harvested records, unpack, clean 
for RESULTSET in ${RESULTSETS[@]}
do
	#download tar
	wget $HARVESTER_URL/$RESULTSET
	
	echo "unpacking $RESULTSET..."
	#unpack CMDI 1.2 files
	tar xjf $RESULTSET $CMDI_PATH
	
	#delete tar
	rm $RESULTSET
done

#rename results/cmdi to cmdi
echo "moving results/cmdi to cmdi ..."
mv $CMDI_PATH cmdi

echo "cleannig up ..."
rmdir results empty_dir

echo "finished!"

#run the importer
