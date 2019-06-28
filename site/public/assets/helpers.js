let URI = 'http://jockie.ddns.net:8080'
let TOKEN = localStorage.getItem('token'); 
let USER = ''

console.log('Requesting user data')
requestUSER();

function appendChildren(x, arr) {
  arr.forEach(y => {
    x.appendChild(y);
  })
}

function createElm(type, classes, text, att, p) {
  let __elm = document.createElement(type);
  if(classes) {
    __elm.classList.add(...classes);
  }

  if(text) {
    if(p) {
      __elm.innerText = text;
    }else{
      __elm.innerHTML = text;
    }
  }

  if(att) {
    __elm.setAttribute('select', att);
  }
  return __elm;
}

function doSelection(val) {
  document.querySelectorAll(`[select='${val}']`).forEach(x => {
    x.classList.add('selected')
  })
  document.querySelectorAll(`[select]:not([select='${val}']`).forEach(x => {
    x.classList.remove('selected');
  })
}

function GET(x, auth) {
  let req = new XMLHttpRequest();
  req.open('GET', x, [true]);
  req.responseType = 'json';
  if(auth) {
    req.setRequestHeader('Authorization', TOKEN);
  }
  req.send();
  return req;
}

function addSelection() {
  let val = location.hash.replace('#', '');
  if(location.hash) {
    doSelection(val);
  }
}

function getUrlParam(x) {
  let url = new URL(window.location.href);
  return url.searchParams.get(x);
}

function removeParam(x) {
  let params = new URLSearchParams(location.search)
  params.delete(x);
  history.replaceState(null, '', '?' + params + location.hash);
}

function addScripts() {
  let url = document.title.toLocaleLowerCase().split('|')[1].replace(/ /g,'');
  let js = createElm('script');
  let body = document.body;
  let path = '';

  if(url == 'manage') {
    path = '../'
  }

  js.setAttribute('src', path+'assets/index.js');

  if(url != 'home') {
    let js2 = createElm('script');
    js2.setAttribute('src', `${path}assets/${url}.js`);
    appendChildren(body, [js, js2])
  }else{
    body.append(js);
  }
}

function requestUSER() {
  let profileJSON = GET(URI+'/api/user/me', true);
  profileJSON.onloadend = function(state) {
    let res = profileJSON.response;
    if(res) {
      USER = res.data;
      console.log('User data successfully received');
    }else{
      console.log('Could not receive user data');
    }
    addScripts();
  };
}

function removeSpacesW(x, y) {
  return x.replace(/\s/g, y)
}
