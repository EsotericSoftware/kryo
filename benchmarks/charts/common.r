
options(scipen=999)

rstudio = Sys.getenv("RSTUDIO_USER_IDENTITY") != ""

loadLibrary = function (name) {
	if (!require(name, character.only=TRUE)) {
		install.packages(name)
		library(name, character.only=TRUE)
	}
}
loadLibrary("ggplot2")
loadLibrary("gridExtra")

jmhCSV = function (path) {
	data = read.csv(path, sep=",", header=T)

	# delete all before last dot in benchmark names
	data$Benchmark = sub("^.+\\.", "", data$Benchmark)

	# rename Error column
	colnames(data)[colnames(data) == "Score.Error..99.9.."] = "Error"

	# remove Param prefix
	colnames(data) = sub("Param..", "", colnames(data))
	
	data
}

jmhBarChart = function (data, fill, fillLabel, xLabel, yLabel, title=NULL) {
	g = ggplot(data, aes(x=Benchmark, y=Score, fill=data[,fill], ymin=Score - Error, ymax=Score + Error))
	g = g + geom_bar(stat="identity", position="dodge", color="black", width=0.9)
	g = g + geom_errorbar(width=.33, size=.5, position=position_dodge(0.9))
	g = g + labs(x=xLabel, y=yLabel, fill=fillLabel) + geom_hline(yintercept=0)
	if (!rstudio) {
		if (length(title) != 0) g = g + ggtitle(title)
		g = g + theme(text=element_text(size=16))
	}
	g
}
