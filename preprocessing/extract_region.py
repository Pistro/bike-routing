import subprocess
import os.path
  
# Extract subregion of Ghent
subprocess.call(['osmosis',
	'--read-xml', os.path.join('..', 'graph', 'belgium-weighted.osm'),
	'--bounding-box', 'top=51.15', 'left=3.65', 'bottom=51', 'right=3.85',
	'--write-xml', os.path.join('..', 'graph', 'ghent-weighted.osm')], shell=True)
	
# Create a json file for visualisation
subprocess.call(['java', '-jar', 'osm2graph.jar',
	'-verbose',
	'--json', 'out=' + os.path.join('..', 'graph', 'ghent-weighted.json'),
	os.path.join('..', 'graph', 'ghent-weighted.osm')])