createServers(USER)
function createServers(data) {
  let __serContent = document.getElementById('serverContent');
  let __secHeader = createElm('div', ['sectionHeader']);
  let __secH2 = createElm('h2', ['sectionTitle', 'twhite'], 'my servers')
  let __a = createElm('a', ['btn'], 'Add a server');

  __secH2.style = 'text-transform:uppercase;margin-left:10px;'
  __a.setAttribute('href', 'https://discord.com/oauth2/authorize?scope=bot&permissions=8&client_id=592768736315047946');

  appendChildren(__secHeader, [__secH2, __a]);

  if(data.length == 0) {
    __serContent.appendChild(__secHeader);
  }else{
    let arr = addServers(data.guilds);
    arr.unshift(__secHeader);
    appendChildren(__serContent, arr)
  }
}

function addServers(x) {
  let arr = [];
  for(let i = 0; i < x.length; i++) {
    let __a = createElm('a', ['serverContent']);
    let __img = createElm('img', ['serverImg']);
    let __name = createElm('div', ['serverName', 'twhite'], x[i].name);

    __a.setAttribute('href', `/manage/${x[i].id}`)
    __img.setAttribute('src', x[i].iconUrl);

    appendChildren(__a, [__img, __name]);
    arr.push(__a);
  }
  return arr;
}