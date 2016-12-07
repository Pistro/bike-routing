# Installing rtree on Windows (64 bit)
## libspatialindex
Rtree requires the libspatialindex library, so we start by installing this.
 - Download the latest windows build from the [libspatialindex github](https://libspatialindex.github.io/)
 - Unzip it and place it at a convenient place
 - Create a system environment variable 'LIBSPATIAL' that points to the build/bin subfolder
 - Add the build/bin subfolder to the path variable

## rtree
 - Download the .tar.gz rtree source file from the [rtree python page](https://pypi.python.org/pypi/Rtree/)
 - Unpack it
 - In `setup.py` in the main rtree folder, change:

```python
if os.name == 'nt':
    data_files=[('Lib/site-packages/rtree',
                 [r'D:\libspatialindex\bin\spatialindex.dll',
                  r'D:\libspatialindex\bin\spatialindex_c.dll',]),]
else:
    data_files = None
```
in:
```python
if os.name == 'nt':
    data_files=[('Lib/site-packages/rtree',
                 [os.path.join(os.environ['LIBSPATIAL'], 'spatialindex-64.dll'),
                  os.path.join(os.environ['LIBSPATIAL'], 'spatialindex_c-64.dll'),]),]
else:
    data_files = None
```
 - In `core.py` in the rtree subfolder, change:
 ```python
if os.name == 'nt':

    def _load_library(dllname, loadfunction, dllpaths=('', )):
        """Load a DLL via ctypes load function. Return None on failure.

        Try loading the DLL from the current package directory first,
        then from the Windows DLL search path.

        """
        try:
            dllpaths = (os.path.abspath(os.path.dirname(__file__)),
                        ) + dllpaths
        except NameError:
            pass # no __file__ attribute on PyPy and some frozen distributions
        for path in dllpaths:
            if path:
                # temporarily add the path to the PATH environment variable
                # so Windows can find additional DLL dependencies.
                try:
                    oldenv = os.environ['PATH']
                    os.environ['PATH'] = path + ';' + oldenv
                except KeyError:
                    oldenv = None
            try:
                return loadfunction(os.path.join(path, dllname))
            except (WindowsError, OSError):
                pass
            finally:
                if path and oldenv is not None:
                    os.environ['PATH'] = oldenv
        return None

    rt = _load_library('spatialindex_c.dll', ctypes.cdll.LoadLibrary)
    if not rt:
        raise OSError("could not find or load spatialindex_c.dll")
```
in:
```python
if os.name == 'nt':
    rt = ctypes.cdll.LoadLibrary("spatialindex_c-64.dll")
    if not rt:
        raise OSError("could not find or load spatialindex_c-64.dll")
```
 - Install the package by opening a command window in the main rtree folder and executing:
```sh
 $ python setup.py install
```