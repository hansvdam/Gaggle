function read_external_text_file(url) {
//    var text = "";
    var xmlhttp;
    if (window.XMLHttpRequest) {
        xmlhttp = new XMLHttpRequest();
    } else {
        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
    }
    if (xmlhttp !== null) {
    	xmlhttp.open("GET", url, false);
    	xmlhttp.send();
    	return xmlhttp.responseText;
//        text = xmlhttp.responseText;
    }
}
