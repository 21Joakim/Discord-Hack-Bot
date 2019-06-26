//to be replaced with the actual bot's commands using an api
let commands = [{"categoryName":"cat1","commands":[{"name":"com1","aliases":["com1a","com1b","com1c"],"usage":"com1 <x>","subCommands":[{"name":"subcommmm","usage":"com1 subcom <x>","description":"this subcom does this thing","authorPermissions":0,"botPermissions":0},{"name":"subcom2","usage":"com1 subcom2 <x>","description":"this subcom2 does this thing","authorPermissions":0,"botPermissions":0}],"description":"this com does this thing","authorPermissions":0,"botPermissions":0},{"name":"com2","aliases":["com2a","com2b"],"usage":"com2 <x>","subCommands":[{"name":"subcom","usage":"com2 subcom <x>","description":"this subcom does this thing","authorPermissions":0,"botPermissions":0}],"description":"this com does this thing","authorPermissions":0,"botPermissions":0},{"name":"com3","aliases":[],"usage":"com3 <x>","subCommands":[],"description":"this com does this thing","authorPermissions":0,"botPermissions":0}]},{"categoryName":"cat2","commands":[{"name":"com1","aliases":["com1a","com1b","com1c"],"usage":"com1 <x>","subCommands":[{"name":"subcom","usage":"com1 subcom <x>","description":"this subcom does this thing","authorPermissions":0,"botPermissions":0}],"description":"this com does this thing","authorPermissions":0,"botPermissions":0},{"name":"com2","aliases":["com2a","com2b"],"usage":"com2 <x>","subCommands":[{"name":"subcom","usage":"com2 subcom <x>","description":"this subcom does this thing","authorPermissions":0,"botPermissions":0}],"description":"this com does this thing","authorPermissions":0,"botPermissions":0},{"name":"com3","aliases":[],"usage":"com3 <x>","subCommands":[],"description":"this com does this thing","authorPermissions":0,"botPermissions":0}]}];

addCategoryList();
function addCategoryList() {
  let __sidenav = document.getElementById("sectionNav");
  let __sectionContent = createDivAndClass("div", ["sectionContent", "categoryContent"]);
  
  for(let i = 0; i < commands.length; i++) {
    if(commands[i]) {
      addCategories(__sectionContent, commands[i]);
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

    __command.setAttribute("onclick", `showProperties(this)`)
    __command.innerText = x[i].name;

    arr.push(__command, addChildren(x[i].subCommands));
  }
  return arr;
}

function addChildren(x) {
  let __catChildren = createDivAndClass("ul", ["categoryChildren"]);
  let arr2 = []; 
  for(let i = 0; i < x.length; i++) {
    let __catChild = createDivAndClass("li", ["categoryChild"]);
    let __a = document.createElement("a");

    __a.setAttribute("href", `#${x[i].name}`)
    __a.innerText = x[i].name;

    __catChild.setAttribute("id", `${x[i].name}`)
    __catChild.appendChild(__a);
    
    arr2.push(__catChild);
  } 
  appendChildren(__catChildren, arr2);
  return __catChildren;
}

function showProperties(x) {
  x.classList.toggle("selected");
  let arr = document.getElementsByClassName("categoryParent");
  for(let i = 0; i < arr.length; i++) {
    if(arr[i] != x) {
      arr[i].classList.remove("selected");
    }
  }
}

function appendAfter(newNode, referenceNode) {
  referenceNode.parentNode.insertBefore(newNode, referenceNode.nextSibling);
}

function appendChildren(x, arr) {
  arr.forEach(y => {
    x.appendChild(y);
  })
}

function createDivAndClass(type, classname, att) {
  let __element = document.createElement(type);
  if(classname) {
    __element.classList.add(...classname);
  }

  if(att) {
    __element.setAttribute("command", att);
  }
  return __element;
}