//to be replaced with the actual bot's commands using an api
let commands = [{"categoryName":"cat1","commands":[{"name":"com1","aliases":["com1a","com1b","com1c"],"usage":"com1 <x>","subCommands":[{"name":"subcommmm","usage":"com1 subcom <x>","description":"this subcom does this thing","authorPermissions":0,"botPermissions":0},{"name":"subcom2","usage":"com1 subcom2 <x>","description":"this subcom2 does this thing","authorPermissions":0,"botPermissions":0}],"description":"this com does this thing","authorPermissions":0,"botPermissions":0},{"name":"com2","aliases":["com2a","com2b"],"usage":"com2 <x>","subCommands":[{"name":"subcom","usage":"com2 subcom <x>","description":"this subcom does this thing","authorPermissions":0,"botPermissions":0}],"description":"this com does this thing","authorPermissions":0,"botPermissions":0},{"name":"com3","aliases":[],"usage":"com3 <x>","subCommands":[],"description":"this com does this thing","authorPermissions":0,"botPermissions":0}]},{"categoryName":"cat2","commands":[{"name":"com4","aliases":["com1a","com1b","com1c"],"usage":"com1 <x>","subCommands":[{"name":"subcom","usage":"com1 subcom <x>","description":"this subcom does this thing","authorPermissions":0,"botPermissions":0}],"description":"this com does this thing","authorPermissions":0,"botPermissions":0},{"name":"com5","aliases":["com2a","com2b"],"usage":"com2 <x>","subCommands":[{"name":"subcom","usage":"com2 subcom <x>","description":"this subcom does this thing","authorPermissions":0,"botPermissions":0}],"description":"this com does this thing","authorPermissions":0,"botPermissions":0},{"name":"com6","aliases":[],"usage":"com3 <x>","subCommands":[],"description":"this com does this thing","authorPermissions":0,"botPermissions":0}]}]

//triggers
addCategoryList();
doSelection(commands[0].commands[0].name);

//functions
function addCategoryList() {
  let __sidenav = document.getElementById("sectionNav");
  let __commandContent = document.getElementById("commandContent");
  let __sectionContent = createDivAndClass("div", ["sectionContent", "categoryContent"]);
  
  for(let i = 0; i < commands.length; i++) {
    if(commands[i]) {
      addCategories(__sectionContent, commands[i]);
      addCommandsDiv(__commandContent, commands[i].commands);
    }
  }

  __sidenav.appendChild(__sectionContent);
}

function addCategories(x, cat) {
  let __catBlock = createDivAndClass("div", ["categoryBlock"]);
  let __catContainer = createDivAndClass("ul", ["categoryContainer"]);
  let __catName = createDivAndClass("div", ["categoryName"]);

  __catName.innerText = cat.categoryName;


  let arr = addCommands(cat.commands);
  arr.unshift(__catName);

  __catBlock.appendChild(__catContainer);
  appendChildren(__catContainer, arr);
  x.appendChild(__catBlock);
}

function addCommands(x) {
  let arr = [];
  for(let i = 0; i < x.length; i++) {
    let __command = createDivAndClass("li", ["categoryParent"], x[i].name);
    let __a = document.createElement("a");

    __a.setAttribute("href", `#${x[i].name}`);
    __a.innerText = x[i].name;

    __command.appendChild(__a);

    arr.push(__command);
  }
  return arr;
}

function addCommandsDiv(x, y) {
  let arr = [];
  for(let i = 0; i < y.length; i++) {
    let arr2 = [];

    let __categoryHolder = createDivAndClass("div", ["categoryHolder"], y[i].name);

    let __title = createDivAndClass("h1", ["categoryTitle", "big"]);
    __title.innerText = y[i].name + " commands";
    __title.style = "margin-top:0;"

    let __title2 = createDivAndClass("h2", ["categoryTitle"]);
    __title2.innerText = "main";

    let __maintable = createTable(y[i], false);

    arr2.push(__title, __title2, __maintable);
    
    
    if(y[i].subCommands.length != 0) {
      let __title3 = createDivAndClass("h2", ["categoryTitle"]);
      __title3.innerText = "sub";
      
      let __subtable = createTable(y[i].subCommands, true);
      arr2.push(__title3, __subtable);
    }

    appendChildren(__categoryHolder, arr2);
    arr.push(__categoryHolder);
  }

  appendChildren(x, arr);
}

function createTable(x, sub) {
  let __tableContainer = createDivAndClass("div", ["tableContainer"]);
  let __table = document.createElement("table");

  let __caption = document.createElement("caption");  
  __caption.innerText = (sub?"subcommand structure":"command structure");

  let __tr = document.createElement("tr");
  let __tableHeads = [
    createElmWtext("th", "name"), 
    createElmWtext("th", "description"), 
    createElmWtext("th", "usage"), 
    createElmWtext("th", "aliases")
  ]
  appendChildren(__tr, __tableHeads)

  let arr = []
  if(x.constructor !== Array) {
    x = [x];
  }

  for(let i = 0; i < x.length; i++) {
    let __tr2 = document.createElement("tr");
    let __tableBody = [
      createElmWtext("td", x[i].name),  
      createElmWtext("td", (x[i].description?x[i].description:"None")),  
      createElmWtext("td", (x[i].usage?x[i].usage:"None")),  
      createElmWtext("td", (x[i].aliases?x[i].aliases:"None"))
    ]
  
    appendChildren(__tr2, __tableBody);
    arr.push(__tr2);
  }

  arr.unshift(__caption, __tr);
  appendChildren(__table, arr)
  __tableContainer.appendChild(__table);

  return __tableContainer;
}

