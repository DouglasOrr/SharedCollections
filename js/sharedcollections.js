// Calls handler(["latest.version", "older.version", ...]) after the document is loaded
function getVersions(handler) {
    $.get("versions", function(data) {
        var versions = data.trim().split(" ");
        // sorts in decending version number order (versions[0] is the latest)
        versions.sort(function (a,b) {
            return a.split(".").map(Math.round) < b.split(".").map(Math.round);
        });
        $(function () {
            handler(versions);
        });
    });
}

function getProfile(version, handler) {
    $.get("releases/" + version + "/profile/profile.json", handler);
}

getVersions(function(versions) {
    // update all the dependency panels to show the latest version number
    $(".deps").text(function(i,text) {
        return text.replace("latest.version", versions[0]);
    });
    $("pre code.deps").each(function(i,block) {
        hljs.highlightBlock(block);
    });
    getProfile(versions[0], function(data) {
        var results = data.profile["Map.put"];
        var series = [];
        for (var impl in results) {
            series.push({
                key: impl,
                values: results[impl].map(function(o) {
                    return {x: o.size,
                            y: o.latency_ns / (o.repetitions * o.size)};
                })});
        }

        nv.addGraph(function() {
            var chart = nv.models.lineChart()
                .xScale(d3.scale.log().base(2));
            chart.xAxis.axisLabel("Peak collection size")
                .showMaxMin(false);
            chart.yAxis.axisLabel("Average operation time /ns")
                .showMaxMin(false);
            d3.select('#performance-chart svg')
                .datum(series)
                .call(chart);
            nv.utils.windowResize(chart.update);
            chart.update();
            return chart;
        });
    });
});

hljs.initHighlightingOnLoad();
