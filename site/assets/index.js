let code = getUrlParam("code");

//triggers
if(code) {
  login(code);
}else{
  checkProfile();
}

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
if(__sidenav) {
  let __newnav = __navList.cloneNode(true);
  __newnav.classList.add("mobile")

  if (__sidenav.firstChild.classList == undefined) {
    __sidenav.insertBefore(__newnav, __sidenav.firstChild);
  }
}

document.onclick = function (e) {
  let __profile = document.getElementById("profile");
  if (e.target.closest("#profile")) {
    return;
  }
  if (e.target.classList != __profile.classList) {
    if(__profile.children[1]) {
      __profile.children[1].classList.remove("show");
    }
  }
}

function showNav() {
  document.getElementById("nav").classList.toggle("show");
  document.getElementById("sectionNav").classList.toggle("show");
  document.getElementById("overlay").classList.toggle("show");
}

function login(x) {
  let req = GET(`${uri}/api/user/auth?code=${x}`);
  req.onload = function() {
    let res = req.response;
    if(res.code && res.code == 500) {
      return;
    } 
    localStorage.setItem("token", res.token);
    checkProfile();
  }
}

function checkProfile() {
  let __profile = document.getElementById("profile");
  let __auth = localStorage.getItem("token");

  if(__auth) {
    createProfile(__auth, __profile);
  }
}

function createProfile(x, y) {
  let req = GET(uri+"/api/user/me", x);

  req.onload = function() {
    let res = req.response;
    
    let __img = createDivAndClass("img", ["profile"]);
    __img.setAttribute("src", res.data.avatarUrl);
    __img.setAttribute("onclick", "showProfileHolder(this)");

    let __holder = createDivAndClass("div", ["profileHolder"])
    let __leftpart = createDivAndClass("div", ["left"]);
    let __rightpart = createDivAndClass("div", ["right"]);

    __leftpart.innerHTML = `Logged in as <br><span class="twhite cut">${res.data.name}</span><span class="tgrey">#${res.data.discriminator}</span>`;
    __rightpart.innerHTML = `<div class="right-content" onclick="logOut()">Log Out</div>`;

    appendChildren(__holder, [__leftpart, __rightpart])

    y.innerHTML = '';
    appendChildren(y, [__img, __holder]);
  }
} 

function showProfileHolder(x) {
  x.parentElement.children[1].classList.toggle("show");
}

function logOut() {
  localStorage.removeItem("token");
  window.location.href = "./index.html";
}