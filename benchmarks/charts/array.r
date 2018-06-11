
source("../common.r")

data = jmhCSV("array.csv")

data = data[,grep("^(Benchmark|Score|Error|bufferType)$", colnames(data))] # keep only these columns

g = jmhBarChart(data, "bufferType", "Buffer type", "Operation", "Seconds per 12M operations", "ArrayBenchmark")

if (!rstudio) png("array.png", 1024, 445)
grid.arrange(g)
