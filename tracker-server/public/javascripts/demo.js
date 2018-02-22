$(document).ready(function() {
    var source = new EventSource("../topn/updates");
    source.onmessage = function(event) {
        var data = JSON.parse(event.data);
        // $(".odometer").text(counter);
        network.setData({nodes: data.nodes, edges: data.edges});
        network.redraw();
        $(".transactions").text(data.numbers.numberOfTransactions);
        $(".chains").text(data.numbers.averageNumberOfChains);
        $(".blocks").text(data.numbers.averageNumberOfBlocks);
        $(".setC").text(data.numbers.averageSetCSize);
    };

    window.odometerOptions = {
        auto: false,
        format: '(,ddd).ddd', // Change how digit groups are formatted, and how many digits are shown after the decimal point
        duration: 100,
        animation: 'count'
    };

    var nodes = [];
    var edges = [];
    var network = null;

    function draw() {

        // Instantiate our network object.
        var container = document.getElementById('demo-graph');
        var data = {
            nodes: nodes,
            edges: edges
        };
        var options = {
            nodes: {
                shape: 'dot'
            },
            edges: {
                color: {
                    opacity: 0.5
                },
                smooth: {
                    type: 'discrete'
                }
            },
            layout: {
                randomSeed: 2
            },
            physics: {
                barnesHut: {
                    springLength: 1000
                }
            },
            configure: {
                filter:function (option, path) {
                    if (path.indexOf('physics') !== -1) {
                        return true;
                    }
                    if (path.indexOf('smooth') !== -1 || option === 'smooth') {
                        return true;
                    }
                    if (path.indexOf('color') !== -1 || option === 'color') {
                        return true;
                    }
                    if (path.indexOf('nodes') !== -1) {
                        return true;
                    }
                    return false;
                },
                container: document.getElementById('config')
            }
        };
        network = new vis.Network(container, data, options);
    }
    draw();

    var counter = 12;

    $("#saveButton").on("click", function() {
        $.ajax({
            method: 'POST',
            url: '/write-data'
        });
    });
});