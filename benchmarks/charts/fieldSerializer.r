
source("../common.r")
loadLibrary("gridExtra")

data = jmhCSV("fieldSerializer.csv")

data$references = sub("true", "refs", data$references)
data$references = sub("false", "no refs", data$references)

data$chunked = sub("true", ", chunked", data$chunked)
data$chunked = sub("false", "", data$chunked)

data$Type = paste(data$references, data$chunked, sep="")

data = data[,grep("^(Benchmark|Type|Score|Error|objectType)$", colnames(data))] # keep only these columns

g1 = jmhBarChart(subset(data, objectType == "sample"), "Type", "Serializer settings", "Sample", "Round trips per second")
g2 = jmhBarChart(subset(data, objectType == "media"), "Type", "Serializer settings", "Media", "Round trips per second")

if (!rstudio) {
	g1 = g1 + ggtitle("FieldSerializerBenchmark")
	png("fieldSerializer.png", 1024, 890)	
}
grid.arrange(g1, g2)
