# Visualisation

## Dependencies

 - Python 3, including pip. If you are a Windows user, do not forget to add both the python installation dir and the 'Scripts' subdir to your path
 - Following pip packages:
     - django
     - django-leaflet
	 
### Database

In order to visualize a routes, a database that maps the id's of the ways contained in the route to the corresponding nodes is necessary.

One option to get a database file is to download the [Belgian](https://www.dropbox.com/s/zv4rlmii71gsvde/db.tar.gz) or [French](https://www.dropbox.com/s/gxn6wwez9c5sl86/db.tar.gz) database corresponding to the graph files that are provided in [routing](../routing) and place the db.sqlite file in the visualisation folder.

Alternatively, a fixture-file, produced as a result of the [preprocessing](../preprocessing) step, can be used to create a database.
To create the database, execute:
```sh
$ python manage.py makemigrations
$ python manage.py migrate
```
Move the fixture file in the visualisation/showRoutes/fixtures folder and load the fixture-file into the database (this may take several hours):
```sh
$ python manage.py loaddata fixtureName
```
Optionally, create a superuser. Choose 'root' as password.
```sh
python manage.py createsuperuser --username=root --email=root@root.com
```

# Running the visualisation server

To start the visualisation server, open a terminal in the visulisations folder and execute:
```sh
$ python manage.py runserver
```
The pages that show visualisations can now be accessed through a browser. Following pages are available:
 - [http://127.0.0.1:8000/](http://127.0.0.1:8000/) : This page allows to load one or more routes or search trees. The routes/trees are visualised on a map. In the case of a route, a height profile is also shown. When hovering over the height profile, the corresponding position on the map is marked and the information on the edge that contains the position is shown next to the marker. Selecting a position on the map also marks the position in the height profile.
 - [http://127.0.0.1:8000/height/](http://127.0.0.1:8000/height/) : Very similar to the link above. For routes, an additional plot with the cumulative height of the tour is shown.
 - [http://127.0.0.1:8000/nodes/](http://127.0.0.1:8000/nodes/) : This page allows to visualise quantitiative attributes that are assigned to nodes, for example probabilities assigned to candidate nodes. The attribute that is visualised and the values corresponding to the extrema of the visualised colors (shown in red and green) can be changed. Nodes for which the attribute is not present, are not shown. When hovering over a node, all info corresponding to this node is shown.
 - [http://127.0.0.1:8000/pleasantness/](http://127.0.0.1:8000/pleasantness/) :  This page allows to visualise quantitiative attributes that are assigned to ways, for example perceived weights. The attribute that is visualised and the values corresponding to the extrema of the visualised colors (shown in red and green) can be changed. Ways for which the attribute is not present, are not shown. When hovering over a way, all info corresponding to the way is shown. To avoid loading too many ways in the browser memory, ways are only shown when zoomed in sufficiently.
 - [http://127.0.0.1:8000/admin/](http://127.0.0.1:8000/admin/) :  Admin page. This page allows to look at the structure of the database and to manually add ways/nodes. Both username and password are 'root'.
