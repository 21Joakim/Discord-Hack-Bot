let uri = "http://jockie.ddns.net:8080"

function appendChildren(x, arr) {
  arr.forEach(y => {
    x.appendChild(y);
  })
}

function createDivAndClass(type, classes, att) {
  let __element = document.createElement(type);
  if(classes) {
    __element.classList.add(...classes);
  }

  if(att) {
    __element.setAttribute("select", att);
  }
  return __element;
}

function createElmWtext(type, text, classes) {
  let __elm = document.createElement(type);
  __elm.innerHTML = text;
  if(classes) {
    __elm.classList.add(...classes);
  }
  return __elm;
}

function doSelection(val) {
  document.querySelectorAll(`[select="${val}"]`).forEach(x => {
    x.classList.add("selected")
  })
  document.querySelectorAll(`[select]:not([select="${val}"]`).forEach(x => {
    x.classList.remove("selected");
  })
}

function GET(x, auth) {
  let req = new XMLHttpRequest();
  req.open("GET", x, [true]);
  req.responseType = 'json';
  if(auth) {
    req.setRequestHeader("Authorization", auth);
  }
  req.send();
  return req;
}

function addSelection() {
  let val = location.hash.replace("#", "");
  if(location.hash) {
    doSelection(val);
  }
}

function getUrlParam(x) {
  let url = new URL(window.location.href);
  return url.searchParams.get(x);
}

function removeParams(x) {
  return x.replace(x.split('?')[1], "").replace("?", "");
}