import sys
import time
import xml.sax
import xml.sax.saxutils
from base import *
from reader import *
from writer import *
from processors import *
from extra import *


def osm2graph(inPath, opts):
    schedule = {'steps': list(), 'end': {'readers': list(), 'interreaders': list(), 'resources': set()}}
    pos = 1
    combs = ComboOsmProcessor.__subclasses__()
    writers = OsmWriter.__subclasses__()
    inters = InterReader.__subclasses__()
    readers = OsmReader.__subclasses__()
    simps = writers + inters + readers
    verbose = False
    for optNr in range(len(opts)):
        (opt_name, sub_opts) = opts[optNr]
        if opt_name == '-st' or opt_name == '--step':
            pos += 1
        elif opt_name == '-v' or opt_name == '--verbose':
            verbose = True
        else:
            sub_opts['id'] = str(optNr)
            if (not 'share' in sub_opts):
                sub_opts['share'] = str(pos)
            for p in combs:
                if '-' + p.shortName() == opt_name or '--' + p.longName() == opt_name:
                    inst = p(sub_opts)
                    inst.shedule(sub_opts, schedule, pos)
            for p in simps:
                if '-' + p.shortName() == opt_name or '--' + p.longName() == opt_name:
                    inst = p(sub_opts)
                    inst.shedule(sub_opts, schedule)
    schedule = schedule['steps']
    resources = dict()
    start = time.time()
    st = 0
    while(len(schedule)):
        if (verbose):
            print("STEP " + str(st))
            st += 1
        step = schedule.pop(0)
        # Collect resources
        for reader in step['readers']:
            collectResources(reader, resources)
        # Create the pipe
        pipe = xml.sax.ContentHandler()
        for reader in reversed(step['readers']):
            pipe = reader.setWriter(pipe)
            giveResources(reader, resources)
        # Execute pipeline
        pipeStart = time.time()
        if len(step['readers']):
            xml.sax.parse(inPath, pipe)
        pipeEnd = time.time()
        if (verbose):
            print("Pipe execution time: " + str(round((pipeEnd-pipeStart)*100)/100) + 's')
        # Execute the interreaders
        for interreader in step['interreaders']:
            giveResources(interreader, resources)
            irStart = time.time()
            interreader.execute()
            irEnd = time.time()
            if (verbose):
                print(type(interreader).__name__ + " execution time: " + str(round((irEnd-irStart)*100)/100) + 's')
            collectResources(interreader, resources)
        # Free useless resources
        resourcesNew = dict(resources)
        for resourceName in resources:
            if not resourceName in step['resources']:
                del resourcesNew[resourceName]
        resources = resourcesNew
    end = time.time()
    if (verbose):
        print('Total execution time: ' + str(round((end-start)*100)/100) + 's')
    
def giveResources(reader, resources):
    globResName_locResName = reader.imports()
    for globResName in globResName_locResName:
        setattr(reader, globResName_locResName[globResName], resources[globResName])
        
def collectResources(reader, resources):
    globResName_locResName = reader.exports()
    for globResName in globResName_locResName:
        resources[globResName] = getattr(reader, globResName_locResName[globResName])
        
def helpInfo():
    width = 80
    sp = 3
    print("Usage: python osm2graph [-options] infile")
    print("where options include:")
    printIndented("-v, --verbose: Print timing info and in which step the algorithm is.", 1, sp, width)
    printIndented("-st, --step: Increment the step in which the options following this command are started.", 1, sp, width)
    combs = ComboOsmProcessor.__subclasses__()
    for comb in combs:
        (cText, subOps) = comb.getInfo()
        printIndented("-" + comb.shortName() + ", --" + comb.longName() + ": " + cText, 1, sp, width)
        if subOps and len(subOps):
            printIndented("subarguments:", 2, sp, width)
            for subOp in subOps:
                printIndented(subOp[0] + ": " + subOp[1], 3, sp, width)
        
if __name__ == '__main__':
    IN = None
    OUT = None
    if len(sys.argv) >= 3:
        pos = 1
        opts = list()
        curOpt = None
        while pos<len(sys.argv)-1:
            if sys.argv[pos][0] == '-':
                if (curOpt):
                    opts.append((curOpt, curExtra))
                curOpt = sys.argv[pos]
                curExtra = dict()
            else:
                tmp = sys.argv[pos].split('=', 1)
                if (len(tmp)==2):
                    curExtra[tmp[0]] = tmp[1]
                else:
                    curExtra[tmp[0]] = None
            pos += 1
        if (curOpt):
            opts.append((curOpt, curExtra))
        IN = sys.argv[len(sys.argv)-1]
        osm2graph(IN, opts)
    else:
        helpInfo()