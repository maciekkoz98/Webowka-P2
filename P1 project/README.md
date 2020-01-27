In order to run this site you need to build docker image first:
`docker build -t form-nginx .`

Next step is to run image:
`docker run --rm --name form -d -p 8080:80 form-nginx`

Then you will be able to access site through:
`localhost:8080/form.html`  

In order to check availability of the login, you need to accept the self-signed certificate first. Just go to the address shown in the console error log and choose to trust the site.    