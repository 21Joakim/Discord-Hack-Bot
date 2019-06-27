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