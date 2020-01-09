# ,,Projektowanie aplikacji webowych'' Project 2, Project 3, Project 4

## Project 2
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

## Project 3
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

#### Android Client
In order to use Android Client you need to have an android emulator. The minimum version of Android OS is Android 6.0 Marshmallow.  
To login to the app please use the same credentials as in the web app.

App provides the same possiblities as the web client. You can list, add and delete publications. You can also add and delete files attached to the publications.  
In order to delete publication please hold it in the main view and click rubbish dump icon. To add a publication please click Floating Action Button. To download file press the publication in main view or long tap on publication and choose download file. To delete attached file long tap the publication and choose delete file button (crossed attachment icon).  

There is a problem in downloading files attached by the web client. The reason of such behaviour is not known.  

## Project P4
### Web client login
In order to log in you need to you use this credentials:  
login: `jack@pw.edu.pl`  
password: `Gwiazdor!`  

In the login page I used css, pictures from auth0.com example.  