def readRange(rangeString):
	startEnd = rangeString.split("-", 1)
	if len(startEnd)==1:
		startEnd.append(startEnd[0])
	start = int(startEnd[0])
	stop = startEnd[1]
	if not (stop=='end'):
		stop = int(stop)
	return (start, stop)
	
def readFlList(listString):
	numbers = listString.replace('[', '').replace(']', '').split(",")
	l = list()
	for number in numbers:
		l.append(float(numbers))
	return l