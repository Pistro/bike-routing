call :createWgr %1

:createWgr
call python osm2graph ^
	--verbose ^
	--clean-way ^
	--step ^
	--weights profile=bikeProfile ^
	--filter rules=+*,-osm.node.attr.version,-osm.node.attr.timestamp,-osm.way:allows_bikes~0,-osm.way.attr.allows_bikes,-osm.way.attr.version,-osm.way.attr.timestamp,-osm.way.tag:k~norm_score,-osm.way.tag:k~norm_score_safe,-osm.way.tag:k~norm_score_attr,-osm.way.tag:k~norm_score_fast,-osm.bounds,-osm.way.nd ^
	--xml out=..\source\%1.wgr ..\source\%1_inter.osm
goto :eof