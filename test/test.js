function uploadImgForm() {
    let sendData = document.getElementById("image");

    let uploadReq = new XMLHttpRequest();
    uploadReq.onreadystatechange = function() {
        if (this.readyState === 4) {
            if (this.status === 200) {
                alert(this.responseText);
            } else {
                alert("Failed to get images! Code: " + this.status + " Message: " + this.responseText);
            }
        }
    };
    uploadReq.open("PUT", "/api/upload_image_form?fileName=" + encodeURIComponent(sendData.files[0].name), true);
    uploadReq.send(sendData.files[0]);
}