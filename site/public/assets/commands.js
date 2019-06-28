let json = GET(URI+'/api/bot/commands');
json.onload = function() {
  let data = json.response.data.commands;
  addCategoryList(data);

  if(location.hash) {
    addSelection();
  }else{
    doSelection(data[0].commands[0].name);
  }
}

//functions
function addCategoryList(data) {
  let __sidenav = document.getElementById('sectionNav');
  let __commandContent = document.getElementById('commandContent');
  let __sectionContent = createElm('div', ['sectionContent', 'categoryContent']);
  
  for(let i = 0; i < data.length; i++) {
    if(data[i]) {
      addCategories(__sectionContent, data[i]);
      addCommandsDiv(__commandContent, data[i].commands);
    }
  }

  __sidenav.appendChild(__sectionContent);
}

function addCategories(x, cat) {
  let __catBlock = createElm('div', ['categoryBlock']);
  let __catContainer = createElm('ul', ['categoryContainer']);
  let __catName = createElm('div', ['categoryName'], cat.category);

  let arr = addCommands(cat.commands);
  arr.unshift(__catName);

  __catBlock.appendChild(__catContainer);
  appendChildren(__catContainer, arr);
  x.appendChild(__catBlock);
}

function addCommands(x) {
  let arr = [];
  for(let i = 0; i < x.length; i++) {
    let __command = createElm('li', ['categoryParent'], '', removeSpacesW(x[i].name, "_"));
    let __a = createElm('a', [], x[i].name);

    __a.setAttribute('href', `#${removeSpacesW(x[i].name, "_")}`);
    __command.appendChild(__a);

    arr.push(__command);
  }
  return arr;
}

function addCommandsDiv(x, y) {
  let arr = [];
  for(let i = 0; i < y.length; i++) {
    let arr2 = [];

    let __categoryHolder = createElm('div', ['categoryHolder'], '', removeSpacesW(y[i].name, "_"));

    let __title = createElm('h1', ['categoryTitle', 'big'], y[i].name + ' commands');
    __title.style = 'margin-top:0;'

    let __title2 = createElm('h2', ['categoryTitle'], 'main');
    let __maintable = createTable(y[i], false);

    arr2.push(__title, __title2, __maintable);
    
    
    if(y[i].subCommands.length != 0) {
      let __title3 = createElm('h2', ['categoryTitle'], 'sub');      
      let __subtable = createTable(y[i].subCommands, true);
      arr2.push(__title3, __subtable);
    }

    appendChildren(__categoryHolder, arr2);
    arr.push(__categoryHolder);
  }

  appendChildren(x, arr);
}

function createTable(x, sub) {
  let __tableContainer = createElm('div', ['tableContainer']);
  let __table = createElm('table');

  let __caption = createElm('caption');  
  __caption.innerText = (sub?'subcommand structure':'command structure');

  let __tr = createElm('tr');
  let __tableHeads = [
    createElm('th', [], 'name'), 
    createElm('th', [], 'description'), 
    createElm('th', [], 'usage'), 
    createElm('th', [], 'aliases')
  ]
  appendChildren(__tr, __tableHeads)

  let arr = []
  if(x.constructor !== Array) {
    x = [x];
  }

  for(let i = 0; i < x.length; i++) {
    let __tr2 = createElm('tr');
    let __tableBody = [
      createElm('td', [], x[i].name),  
      createElm('td', [], (x[i].description?x[i].description:'None')),  
      createElm('td', [], (x[i].usage?x[i].usage:'None')),  
      createElm('td', [], (x[i].aliases?x[i].aliases:'None'))
    ]
  
    appendChildren(__tr2, __tableBody);
    arr.push(__tr2);
  }

  arr.unshift(__caption, __tr);
  appendChildren(__table, arr)
  __tableContainer.appendChild(__table);

  return __tableContainer;
}

function removeSpacesW(x, y) {
  return x.replace(/\s/g, y)
}
