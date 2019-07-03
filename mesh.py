import csv


fileToWrite = 'omnet_nedFiles/net3mesh10.ned'
appendFile = open(fileToWrite, 'a')

data = 'network Net3Mesh10\n{\n\ttypes:\n\t\tchannel c extends ned.DatarateChannel\n\t\t{\n\t\t\tdatarate = 56 kbps;\n\t\t\tdelay = 17 ms;\n\t\t\tber = 1e-10; \n\t\t}'
data += '\n\n\t'
data += 'submodules:\n\t\t'

with open('clusters.csv', 'r') as csv_file:
	csv_reader = csv.reader(csv_file)
	index = 1
	for line in csv_reader:
		data += 'node'+str(index)+' : Node{Id = '+str(index)+'; clus1 = \"'+line[1]+'\"'+'; clus2 = \"'+line[2]+'\"'+'; clus3 = \"'+line[3]+'\"'+'; clus4 = \"'+line[4]+'\"'+'; clus5 = \"'+line[5]+'\";}'
		data += '\n\t\t'
		index+=1
		if (index > 1000):
			break;

data += '\n\tconnections:\n\t\t'

with open('WISE/networks/network3/10000graph.csv', 'r') as net_file:
	net_reader = csv.reader(net_file)
	index = 1
	for record in net_reader:
		for i in range(1, len(record)):
			if index < int(record[i]):
				data+= 'node'+str(index)+'.port++ <--> c <--> node'+str(record[i])+'.port++;\n\t\t'
		index+=1
data += '\n}'

























appendFile.write(data)
appendFile.close()