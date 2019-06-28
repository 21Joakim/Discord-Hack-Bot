let guildId = location.href.split('/').pop().split('#')[0];
let sets = ['logger', 'templates', 'filtering', 'warnings']
let set = location.hash.replace('#', '');

if(!set) {
  set = sets[0];
}

let json = doRequest(set);
json.onload = function() {
  let data = json.response.data;
  getSection(sets[0], data);

  if(location.hash) {
    getSection(set, data);
    doSelection(set);
  }else{
    doSelection(sets[0]);
  }
}

function getSection(x, data, connect) {
  let y = (typeof x === 'string'? x : x.getAttribute('select'));
  let z = document.querySelectorAll(`.categoryHolder[select='${y}']`);

  if(z.length == 0) {
    for(let i = 0; i < sets.length; i++) {
      if(y == sets[i]) {
        if(connect) {
          let dt = doRequest(y);
          dt.onload = function() {
            createModule(y, dt.response.data);
            doSelection(y);
          }
        }else{
          createModule(y, data);
        }
      }
    }
  }
}

function createModule(name, data) {
  if(name == sets[0]) {
    createLoggers(data.loggers);
  }else if(name == sets[3]) {
    createWarnings(data.warnings);
  }
}

function createLoggers(data) {
  let text = {
    title: sets[0],
    paragraph: 'Here you will be able to manage how the logging system works on your server.<br><br>Currently, the system offers having more than 1 channel from which the bot can log to. A few things to note is that you are only limited to <strong>5</strong> channels to log to and the same event cannot be used in a different channel.'
  }
  
  let __manageContent = document.getElementById('manageContent');
  let __catholder = createElm('div', ['categoryHolder'], '', sets[0]);

  let __title = createElm('h1', ['categoryTitle'], text.title)
  __title.style = 'margin-top:0;'
  
  let __p = createElm('p', ['sectionDescription'], text.paragraph); 
  __p.style = 'margin-bottom:40px;'

  let __sectionHeader = createElm('div', ['sectionHeader'])
  let __title2 = createElm('h2', ['sectionTitle'], 'My Loggers');
  let __btn = createElm('button', ['btn'], 'New Logger')

  appendChildren(__sectionHeader, [__title2, __btn])

  if(data.length != 0) {
    let __placeholders = createLoggerHolders(data);
    appendChildren(__catholder, [__title, __p, __sectionHeader, __placeholders]);
  }else{
    appendChildren(__catholder, [__title, __p, __sectionHeader]);
  }

  __manageContent.appendChild(__catholder);
}

function createLoggerHolders(x) {
  let __mplaceHolder = createElm('div', ['placeholder-main']);
  let arr = [];
  for(let i = 0; i < x.length; i++) {
    let __placeholder = createElm('div', ['placeholder']);    
    let __placeholderTitle = createPlaceholderItem('channel', '#'+x[i].channel.name);
    let __placeholderTitle2 = createPlaceholderItem('events', getEvents(x[i].events));
    let __placeholderBtn = createElm('div', ['placeholderBtn']);
    let __btn = createElm('button', ['btn', 'small', 'white'], 'Edit');

    __placeholderTitle2.style = 'line-height: 20px;'

    let y = {}
    if(x[i].enabled) {
      y.clr = 'red'
      y.txt = 'Disable'
    }else{
      y.clr = 'green'
      y.txt = 'Enable'
    }


    let __btn2 = createElm('button', ['btn', 'small', y.clr], y.txt)
    __btn2.style = 'margin-right:10px'

    appendChildren(__placeholderBtn, [__btn2, __btn]);
    appendChildren(__placeholder, [__placeholderTitle, __placeholderTitle2, __placeholderBtn]);
    arr.push(__placeholder)
  }
  appendChildren(__mplaceHolder, arr);
  return __mplaceHolder;
}

function getEvents(x) {
  let txt = '';
  if(x.length == 0) {
    txt = 'None';
  }else{
    for(let i = 0; i < x.length; i++) {
      txt += `<code>${x[i]}</code> `
    }
  }
  return txt;
}

function createWarnings(data) {
  let text = {
    title: 'Warnings',
    paragraph: 'List of people who have been warned on the server. You can see when, where and by whom that warning is. You can also delete the warning if deemed so.'
  }
  
  let __manageContent = document.getElementById('manageContent');
  let __catholder = createElm('div', ['categoryHolder'], '', 'warnings');

  let __title = createElm('h1', ['categoryTitle'], text.title)
  __title.style = 'margin-top:0;'
  
  let __p = createElm('p', ['sectionDescription'], text.paragraph); 
  __p.style = 'margin-bottom:40px;'

  if(data.length != 0) {
    let __placeholders = createWarningsHolders(data);
    appendChildren(__catholder, [__title, __p, __placeholders]);
  }else{
    appendChildren(__catholder, [__title, __p]);
  }

  __manageContent.appendChild(__catholder);
}

function createWarningsHolders(x) {
  let __mplaceHolder = createElm('div', ['placeholder-main']);
  let arr = [];
  for(let i = 0; i < x.length; i++) {
    let __placeholder = createElm('div', ['placeholder']);    
    
    let __placeholderTitle = createPlaceholderItem('user', x[i].user.name+'#'+x[i].user.discriminator);
    __placeholderTitle.style = 'flex-basis: 50%;'

    let __placeholderTitle2 = createPlaceholderItem('moderator', x[i].moderator.name+'#'+x[i].moderator.discriminator);
    __placeholderTitle2.style = 'flex-basis: 50%;margin-top:0;'
    
    let __placeholderTitle3 = createPlaceholderItem('reason', (x[i].reason?x[i].reason:'Reason was not provided.'));
    
    let __placeholderBtn = createElm('div', ['placeholderBtn']);
    let __btn = createElm('button', ['btn', 'small', 'red'], 'Delete');

    appendChildren(__placeholderBtn, [__btn]);
    appendChildren(__placeholder, [
      __placeholderTitle, 
      __placeholderTitle2,
      __placeholderTitle3,
      __placeholderBtn
    ]);
    arr.push(__placeholder)
  }
  appendChildren(__mplaceHolder, arr);
  return __mplaceHolder;
}

function createPlaceholderItem(x, y) {
  let z = `${x}<br><span>${y}</span>`
  return createElm('div', z, ['placeholderTitle'])
}

function doRequest(cog) {
  return GET(`${URI}/api/bot/guild/${guildId}/${cog}`, true)
}