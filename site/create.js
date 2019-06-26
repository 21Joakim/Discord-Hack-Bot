const hbs = require('handlebars');
const fs = require('fs');
const ncp = require('ncp').ncp;
const path = require('path');

let source = fs.readFileSync('./template.hbs', 'utf8');
let template = hbs.compile(source);
let dir = './out/'
let dir2 = './resources/'
let dir3 = path.join(dir, "./assets/");

try {
  if(fs.existsSync(dir)) {
    console.log(`${dir} exists, deleting...`)
    console.log('-----------------------------------')

    unlinkFiles(dir);
    console.log(`successfully deleted folder, initiating conversion...`)
    console.log('-----------------------------------')

    fs.mkdirSync(dir);
    fs.mkdirSync(dir3)
    toStatic()
  }else{
    console.log(`${dir} doesn't exist, creating...`)
    console.log('-----------------------------------')
    fs.mkdirSync(dir);
    toStatic()
  }
} catch(err) {
  console.error(err)
}

function unlinkFiles(paths) {
  if (fs.existsSync(paths)) {
    fs.readdirSync(paths).forEach(function(file, index) {
      let curPath = path.join(paths, file);
      if (fs.lstatSync(curPath).isDirectory()) {
        unlinkFiles(curPath);
      } else {
        fs.unlinkSync(curPath);
      }
    });
    fs.rmdirSync(paths);
  }
};

function toStatic() {
  fs.readdirSync(dir2).forEach(function(file, index) {
    let name = file.replace(/\.[^/.]+$/, "");
    let info = {
      title: `a discord bot | ${name.charAt(0).toUpperCase()+name.substring(1)}`
    }
    info.dashboard = fs.readFileSync(path.join(dir2, file))

    fs.writeFileSync(path.join(dir, file), template(info))
  })
  console.log('conversion successful, copying assets...')
  console.log('-----------------------------------')
  copyFiles();
}

function copyFiles() {
  ncp('./assets/', dir3, x => {
    (x ? console.error(x) : console.log('assets successfully copied'))
    console.log('-----------------------------------')
  });
}