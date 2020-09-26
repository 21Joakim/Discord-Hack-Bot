const http = require('http');
const path = require('path')
const express = require('express')
const exphbs = require('express-handlebars')
const app = express()
const port = 80;

//settings
app.engine('.hbs', exphbs({defaultLayout: 'template', extname: '.hbs', layoutsDir: path.join(__dirname, 'views/layouts')}))
app.set('view engine', '.hbs')
app.set('views', path.join(__dirname, 'views'))
app.use(express.static(path.join(__dirname, 'public')));

//requests
app.get('/', function(req, res) {
  res.render('index', {title: 'Home', home: true});
})

app.get('/servers', function(req, res) {
  res.render("servers", {title: 'Servers', secure: true});
})

app.get('/commands', function(req, res) {
  res.render('commands', {title: 'Commands'});
})

app.get('/manage/:id', function(req, res) {
  res.render('manage', {title: 'Manage', secure: true, path: '../'});
})

http.createServer(app).listen(port);
console.log(`started server on port ${port}`);