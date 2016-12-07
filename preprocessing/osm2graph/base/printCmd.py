def printIndented(str, inLvl, spPerIn, maxWidth):
	strSplitted = str.split(" ")
	current = ""
	for i in range(inLvl*spPerIn-1):
		current += " "
	inLvl += 1
	for strPart in strSplitted:
		if (len(current)+1+len(strPart)>=maxWidth or (len(current) == 0)):
			print(current)
			current = ""
			for i in range(inLvl*spPerIn-1):
				current += " "
			current += " " + strPart
		else:
			current += " " + strPart
	print(current)