
source("../common.r")

data = jmhCSV("variableEncoding.csv")

data = data[,grep("^(Benchmark|Score|Error|bufferType)$", colnames(data))] # keep only these columns

g = jmhBarChart(data, "bufferType", "Buffer type", "Operation", "Operations per second", "VariableEncodingBenchmark")

if (!rstudio) png("variableEncoding.png", 1024, 445)
grid.arrange(g)
