window.addEventListener("load", popupsHandler);

function popupsHandler() {
    var source = new EventSource('/stream');
    var out = document.getElementById('out');
    source.onmessage = function (e) {
        out.setAttribute("class", "visible");
        out.innerHTML = "Dodano nową publikację o tytule " + e.data + "\n" + "Odśwież by ją zobaczyć!\n" + out.innerHTML;
    };
}