call osmosis ^
  --read-xml ..\source\belgium_inter.osm ^
  --bounding-box top=51.15 left=3.65 bottom=51 right=3.85 ^
  --write-xml ..\source\ghent_inter.osm
call :createWgrAndJson ghent
  
call osmosis ^
  --read-xml ..\source\belgium_inter.osm ^
  --bounding-box top=50.81 left=3.79 bottom=50.74 right=3.93 ^
  --write-xml ..\source\gbergen_inter.osm
call :createWgrAndJson gbergen
  
call osmosis ^
  --read-xml ..\source\belgium_inter.osm ^
  --bounding-box top=51.4 left=3.3 bottom=50.7 right=4.3 ^
  --write-xml ..\source\east-flanders_inter.osm
call :createWgrAndJson east-flanders

goto :eof
  
:createWgrAndJson
call python osm2graph ^
	--verbose ^
	--clean-way ^
	--step ^
	--weights profile=bikeProfile ^
	--filter rules=+*,-osm.node.attr.version,-osm.node.attr.timestamp,-osm.way:allows_bikes~0,-osm.way.attr.allows_bikes,-osm.way.attr.version,-osm.way.attr.timestamp ^
	--json out=..\source\%1_full.json ^
	--filter rules=+*,-osm.way.tag:k~norm_score,-osm.way.tag:k~norm_score_safe,-osm.way.tag:k~norm_score_attr,-osm.way.tag:k~norm_score_fast,-osm.bounds,-osm.way.nd ^
	--xml out=..\source\%1.wgr ..\source\%1_inter.osm
goto :eof