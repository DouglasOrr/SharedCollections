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

getVersions(function(versions) {
    // update all the dependency panels to show the latest version number
    $(".deps").text(function(i,text) {
        return text.replace("latest.version", versions[0]);
    });
    $("pre code.deps").each(function(i,block) {
        hljs.highlightBlock(block);
    });
});

hljs.initHighlightingOnLoad();
