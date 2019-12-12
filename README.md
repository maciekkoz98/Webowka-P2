# Projektowanie aplikacji webowych Projekt 2 oraz 3

## Projekt 2
### Setup
Before building the project with docker please rememeber to map site addresses to localhost.  
On MS Windows go under `C:/Windows/System32/drivers/etc` and edit `hosts` file.  
You should add two lines:  
`127.0.0.1 web.company.com`  
`127.0.0.1 fileshare.company.com`  

To run the aplication please use command:
`docker-compose up`

### Usage
In order to log in you need to use:  
username: `Jack` password: `gwiazdor`  
hash: `85f293f02afec08cc90ec9b9501ff532c8c46c094850516700b5e8bd95bb570c`

## Projekt 3
### Setup
To run the project make sure that you've done the steps from Project 2.  
Additionally you have to expand the `hosts` file with the line:  
`127.0.0.1 filesapi.company.com`  

To run the aplication please use command:
`docker-compose up`

### Usage

#### Postman
In order to get or add a publication to the website you need to give proper username with correct password.  
##### GET
When you want to get a publication please include login data in the url.
`https://filesapi.company.com/publications?username=Jack&password=<hash>`

##### POST
When you want to add a publication, you need to send a JSON under `/publications`.  
JSON must include:
* Title
* Author
* Year
* Publisher
* Username (here Jack)
* Password (hash presented above)

#### By address web.company.com
Using web client shouldn't be a problem. Just follow the website's instructions.  

#### Client
Using dedicated client shouldn't albo be any problem. Follow the instructions which are given by the app.  