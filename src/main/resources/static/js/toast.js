//  设置三秒的通知弹窗
function showToast(msg) {
    var toast = document.getElementById('toast');
    toast.innerText = msg;
    toast.style.display = 'block';
    setTimeout(function() {
        toast.style.display = 'none';
    }, 3000);
}