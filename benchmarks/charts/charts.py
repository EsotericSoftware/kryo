#!/usr/bin/env python3
"""Generates the benchmark charts as SVG files from JMH results in JSON format (-rf json).

Usage: charts.py [results directory] [output directory]

Reads <chart name>.json for each chart defined below from the results directory (default: results, next to this script) and
writes <chart name>.svg to the output directory (default: the directory of this script). Charts without results are skipped.
Only the Python standard library is needed."""

import json
import math
import os
import sys
from xml.sax.saxutils import escape


def references(params):
    return "references" if params["references"] == "true" else "no references"


def chunked(params):
    return "chunked" if params.get("chunked") == "true" else "not chunked"


def references_chunked(params):
    return references(params) + (", chunked" if params.get("chunked") == "true" else "")


def code_generation(params):
    return params.get("codeGeneration") == "true"


def code_generation_chunked(params):
    return ("generated code" if code_generation(params) else "cached fields") + (
        ", chunked" if params.get("chunked") == "true" else "")


BUFFER_TYPES = ["array", "byteBuffer", "unsafeArray", "unsafeByteBuffer"]

# name: the chart file and, unless results is given, the results file. results: the results file, if it is shared with another
# chart. filter: returns whether a result is shown, from its parameters. unit: the JMH score unit of the results. axis: the title
# of the y axis. series: returns the series of a result, from its parameters. order: the order of the series. legend: the title
# of the legend. panel: a parameter that splits the chart into panels, each with its own axis. benchmarks: the benchmark methods
# to show and their order, the default is all in the order of the results.
CHARTS = [
    {
        "name": "fieldSerializer",
        "title": "FieldSerializerBenchmark",
        "unit": "ops/s",
        "axis": "Round trips per second (higher is better)",
        "series": references_chunked,
        "order": ["references", "no references", "references, chunked", "no references, chunked"],
        "legend": "Serializer settings",
        "panel": "objectType",
        "panels": {"sample": "Sample", "media": "Media"},
        "benchmarks": ["field", "version", "compatible", "tagged", "custom"],
    },
    {
        "name": "objectGraph",
        "title": "ObjectGraphBenchmark",
        "filter": lambda params: not code_generation(params),
        "unit": "ops/s",
        "axis": "Round trips per second (higher is better)",
        "series": chunked,
        "order": ["not chunked", "chunked"],
        "legend": "Serializer settings",
        "panel": "scale",
        "panels": {"4": "Scale 4 (about 1,400 objects)", "16": "Scale 16 (about 5,200 objects)"},
        "benchmarks": ["field", "version", "compatible", "tagged"],
    },
    {
        "name": "codeGeneration",
        "results": "objectGraph",
        "title": "ObjectGraphBenchmark with code generation",
        "unit": "ops/s",
        "axis": "Round trips per second (higher is better)",
        "series": code_generation_chunked,
        "order": ["cached fields", "generated code", "cached fields, chunked", "generated code, chunked"],
        "legend": "Serializer settings",
        "panel": "scale",
        "panels": {"4": "Scale 4 (about 1,400 objects)", "16": "Scale 16 (about 5,200 objects)"},
        "benchmarks": ["field", "version", "compatible", "tagged"],
    },
    {
        "name": "string",
        "title": "StringBenchmark",
        "unit": "ns/op",
        "axis": "Nanoseconds per operation (lower is better)",
        "series": lambda params: params["bufferType"],
        "order": BUFFER_TYPES,
        "legend": "Buffer type",
    },
    {
        "name": "variableEncoding",
        "title": "VariableEncodingBenchmark",
        "unit": "ns/op",
        "axis": "Nanoseconds per operation (lower is better)",
        "series": lambda params: params["bufferType"],
        "order": BUFFER_TYPES,
        "legend": "Buffer type",
    },
    {
        "name": "array",
        "title": "ArrayBenchmark",
        "unit": "ns/op",
        "axis": "Nanoseconds per operation (lower is better)",
        "series": lambda params: params["bufferType"],
        "order": BUFFER_TYPES,
        "legend": "Buffer type",
        # Without doubles, the chart has no space for more.
        "benchmarks": ["readInts", "readLongs", "readVarInts", "readVarLongs", "writeInts", "writeLongs", "writeVarInts",
            "writeVarLongs"],
    },
]

# The charts look like the default theme of ggplot2, which was used for the charts before.
COLORS = {2: ["#F8766D", "#00BFC4"], 4: ["#F8766D", "#7CAE00", "#00BFC4", "#C77CFF"]}
STYLE = """text { font-family: Arial, Helvetica, sans-serif; font-size: 13px; fill: #4D4D4D }
.title { font-size: 19px; fill: #000000 }
.axisTitle, .legendTitle { font-size: 16px; fill: #000000 }
.legend { fill: #000000 }
.background { fill: #FFFFFF }
.plot { fill: #EBEBEB }
.grid { stroke: #FFFFFF }
.minor { stroke: #FFFFFF; stroke-width: 0.5 }
.tick { stroke: #333333 }
.bar, .line { stroke: #000000 }"""

WIDTH = 1024
HEIGHT = 445  # Of a panel.
LEFT = 78  # Space for the axis title and tick labels left of the plot.
TOP = 34
BOTTOM = 52


def compact(value):
    """Formats a number with a suffix: 1,500,000 is 1.5M."""
    for limit, suffix in ((1e9, "B"), (1e6, "M"), (1e3, "K")):
        if value >= limit:
            return "%g%s" % (round(value / limit, 3), suffix)
    return "%g" % round(value, 3)


def tick_step(maximum):
    """The distance of the axis ticks: 1, 2 or 5 times a power of 10, for at most 5 ticks above 0."""
    raw = maximum / 5
    power = 10 ** math.floor(math.log10(raw))
    for factor in (1, 2, 5, 10):
        if factor * power >= raw:
            return factor * power


def load(path, unit):
    """Returns the results as dicts with benchmark (the method name), params, score and error (0 if unknown). Returns None if
    a result has another unit."""
    with open(path) as file:
        entries = json.load(file)
    results = []
    for entry in entries:
        metric = entry["primaryMetric"]
        if metric["scoreUnit"] != unit:
            return None
        error = metric["scoreError"]
        results.append({
            "benchmark": entry["benchmark"].rsplit(".", 1)[1],
            "params": entry.get("params") or {},
            "score": metric["score"],
            "error": error if isinstance(error, (int, float)) and not math.isnan(error) else 0,
            "jdk": entry.get("jdkVersion", ""),
        })
    return results


def render_panel(chart, results, top, title, label):
    """A bar chart with the benchmarks on the x axis and a bar for each series. top is the y position of the panel."""
    order = chart["order"]
    colors = COLORS[len(order)]
    benchmarks = list(dict.fromkeys(result["benchmark"] for result in results))
    if "benchmarks" in chart:
        benchmarks = [name for name in chart["benchmarks"] if name in benchmarks]
        results = [result for result in results if result["benchmark"] in benchmarks]
    out = []
    if title:
        out.append('<text class="title" x="%d" y="%d">%s</text>' % (LEFT - 30, top + 22, escape(title)))
    # The legend is right of the plot, its width is estimated from the longest text.
    legend = 16 + max(22 + 6.5 * max(len(name) for name in order), 8.5 * len(chart["legend"])) + 8
    left, right = LEFT, WIDTH - legend
    plot_top, plot_bottom = top + TOP, top + HEIGHT - BOTTOM
    out.append('<rect class="plot" x="%d" y="%d" width="%d" height="%d"/>' % (left, plot_top, right - left,
        plot_bottom - plot_top))

    # The y axis starts below 0 and ends above the highest bar, like ggplot2 does it.
    maximum = max(result["score"] + result["error"] for result in results)
    scale = (plot_bottom - plot_top) / (maximum * 1.1)
    zero = plot_bottom - maximum * 0.05 * scale
    step = tick_step(maximum)
    i = 0.5
    while i * step <= maximum * 1.05:
        y = zero - i * step * scale
        out.append('<line class="%s" x1="%d" y1="%.1f" x2="%d" y2="%.1f"/>' % ("grid" if i % 1 == 0 else "minor", left, y,
            right, y))
        i += 0.5
    i = 0
    while i * step <= maximum * 1.05:
        y = zero - i * step * scale
        out.append('<line class="tick" x1="%d" y1="%.1f" x2="%d" y2="%.1f"/>' % (left - 4, y, left, y))
        out.append('<text x="%d" y="%.1f" text-anchor="end">%s</text>' % (left - 7, y + 4.5, compact(i * step)))
        i += 1
    middle = (plot_top + plot_bottom) / 2
    out.append('<text class="axisTitle" transform="translate(%d %.1f) rotate(-90)" text-anchor="middle">%s</text>' % (
        LEFT - 56, middle, escape(chart["axis"])))

    unit = (right - left) / (len(benchmarks) + 0.2)  # The width of a benchmark, with a little space at both ends.
    centers = [left + (i + 0.6) * unit for i in range(len(benchmarks))]
    for name, center in zip(benchmarks, centers):
        out.append('<line class="grid" x1="%.1f" y1="%d" x2="%.1f" y2="%d"/>' % (center, plot_top, center, plot_bottom))
        out.append('<line class="tick" x1="%.1f" y1="%d" x2="%.1f" y2="%d"/>' % (center, plot_bottom, center,
            plot_bottom + 4))
        out.append('<text x="%.1f" y="%d" text-anchor="middle">%s</text>' % (center, plot_bottom + 19, escape(name)))
    out.append('<text class="axisTitle" x="%.1f" y="%d" text-anchor="middle">%s</text>' % ((left + right) / 2,
        plot_bottom + 44, escape(label)))

    # The bars of a benchmark share 90% of its width.
    for name, center in zip(benchmarks, centers):
        bars = sorted((result for result in results if result["benchmark"] == name),
            key=lambda result: order.index(chart["series"](result["params"])))
        width = unit * 0.9 / len(bars)
        for i, result in enumerate(bars):
            x = center - unit * 0.45 + i * width
            y = zero - result["score"] * scale
            out.append('<rect class="bar" fill="%s" x="%.1f" y="%.1f" width="%.1f" height="%.1f"/>' % (
                colors[order.index(chart["series"](result["params"]))], x, y, width, zero - y))
            if result["error"]:
                low = zero - max(result["score"] - result["error"], 0) * scale
                high = zero - (result["score"] + result["error"]) * scale
                x += width / 2
                cap = width * 0.2
                out.append('<path class="line" fill="none" d="M%.1f %.1fV%.1fM%.1f %.1fh%.1fM%.1f %.1fh%.1f"/>' % (x, low,
                    high, x - cap, low, 2 * cap, x - cap, high, 2 * cap))
    out.append('<line class="line" x1="%d" y1="%.1f" x2="%d" y2="%.1f"/>' % (left, zero, right, zero))

    # Legend.
    x = right + 16
    y = middle - (22 + 17 * len(order)) / 2
    out.append('<text class="legendTitle" x="%d" y="%.1f">%s</text>' % (x, y + 14, escape(chart["legend"])))
    y += 22
    for name, color in zip(order, colors):
        out.append('<rect class="bar" fill="%s" x="%d" y="%.1f" width="17" height="17"/>' % (color, x, y))
        out.append('<text class="legend" x="%d" y="%.1f">%s</text>' % (x + 22, y + 13, escape(name)))
        y += 17
    return out


def render(chart, results):
    title = chart["title"]
    jdks = sorted({result["jdk"] for result in results if result["jdk"]})
    if jdks:
        title += ", JDK " + ", ".join(jdks)
    if "panel" in chart:
        panels = [(label, [result for result in results if result["params"].get(chart["panel"]) == value])
            for value, label in chart["panels"].items()]
        panels = [panel for panel in panels if panel[1]]
    else:
        panels = [("Operation", results)]
    height = HEIGHT * len(panels)
    out = [
        '<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d" role="img">' % (WIDTH, height,
            WIDTH, height),
        "<title>%s</title>" % escape(chart["title"] + ": " + chart["axis"].lower()),
        "<style>\n%s\n</style>" % STYLE,
        '<rect class="background" width="%d" height="%d"/>' % (WIDTH, height),
    ]
    for i, (label, panel) in enumerate(panels):
        out += render_panel(chart, panel, i * HEIGHT, None if i else title, label)
    return "\n".join(out + ["</svg>"]) + "\n"


def main():
    directory = os.path.dirname(os.path.abspath(__file__))
    results_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(directory, "results")
    output_dir = sys.argv[2] if len(sys.argv) > 2 else directory
    for chart in CHARTS:
        path = os.path.join(results_dir, chart.get("results", chart["name"]) + ".json")
        if not os.path.exists(path):
            print("Skipped, no results:", path)
            continue
        results = load(path, chart["unit"])
        if not results:
            print("Skipped, results are not in %s: %s" % (chart["unit"], path))
            continue
        if "filter" in chart:
            results = [result for result in results if chart["filter"](result["params"])]
        if not results:
            print("Skipped, no results for the chart:", chart["name"])
            continue
        os.makedirs(output_dir, exist_ok=True)
        output = os.path.join(output_dir, chart["name"] + ".svg")
        with open(output, "w") as file:
            file.write(render(chart, results))
        print("Written:", output)


if __name__ == "__main__":
    main()
