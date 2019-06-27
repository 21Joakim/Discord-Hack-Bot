if(location.hash) {
  addSelection();
}
window.onhashchange = function() {
  addSelection();
}

let __navList = document.getElementsByClassName("navListWrapper")[0];
let title = document.title.split("|")[1].replace(/\s/g, '');
for (let i = 0; i < __navList.children.length; i++) {
  if (__navList.children[i].children[0].innerText == title) {
    __navList.children[i].classList.add("selected");
  }
}

let __sidenav = document.getElementById("sectionNav");
let __newnav = __navList.cloneNode(true);
__newnav.classList.add("mobile")

if (__sidenav.firstChild.classList == undefined) {
  __sidenav.insertBefore(__newnav, __sidenav.firstChild);
}

document.onclick = function (e) {
  let __profile = document.getElementById("profile");
  if (e.target.closest(".profile")) {
    return;
  }
  if (e.target.classList != __profile.classList) {
    __profile.classList.remove("show");
  }
}

function showNav() {
  document.getElementById("nav").classList.toggle("show");
  document.getElementById("sectionNav").classList.toggle("show");
  document.getElementById("overlay").classList.toggle("show");
}

function showProfileHolder(x) {
  x.parentElement.children[1].classList.toggle("show");
}

function addSelection() {
  let val = location.hash.replace("#", "");
  if(location.hash) {
    let __def = document.getElementById("defaultContent");
    if(__def) {
      __def.style = "display:none;"
    }

    document.querySelectorAll(`[select="${val}"]`).forEach(x => {
      x.classList.add("selected")
    })
    document.querySelectorAll(`[select]:not([select="${val}"]`).forEach(x => {
      x.classList.remove("selected");
    })
  }
}