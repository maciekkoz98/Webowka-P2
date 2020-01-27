var nameElem;
var lastnameElem;
var loginElem;
var passwdElem;
var passwd2Elem;
var birthElem;
var peselElem;
var femaleElem;
var manElem;
var fileElem;
var submitButton;

document.onreadystatechange = function () {
    if (document.readyState === "complete") {
        initApplication();
    }
}

function initApplication() {
    nameElem = document.forms[0]["firstname"];
    lastnameElem = document.forms[0]["lastname"];
    loginElem = document.forms[0]["login"];
    passwdElem = document.forms[0]["password"];
    passwd2Elem = document.forms[0]["password2"];
    birthElem = document.forms[0]['birthdate'];
    peselElem = document.forms[0]['pesel'];
    femaleElem = document.getElementById('female');
    manElem = document.getElementById('male');
    fileElem = document.forms[0]['photo'];
    submitButton = document.forms[0]['submitButton'];

    nameElem.addEventListener('blur', () => checkName());
    lastnameElem.addEventListener('blur', () => checkLastname());
    loginElem.addEventListener('blur', () => checkLogin());
    passwdElem.addEventListener('blur', () => checkPasswd());
    passwd2Elem.addEventListener('blur', () => checkTheSamePasswd());
    birthElem.addEventListener('blur', () => checkBirth())
    peselElem.addEventListener('blur', () => checkPesel());
    femaleElem.addEventListener('blur', () => checkSex())
    manElem.addEventListener('blur', () => checkSex());
    fileElem.addEventListener('change', () => checkFile());
    submitButton.addEventListener('click', (e) => validateForm(e));
}
var correctLogin = false;

function checkName() {
    deleteDiv("wrongNameDiv", nameElem);
    var newTextMes = checkNameString(nameElem.value, "imię");

    if (newTextMes.length > 0) {
        addDivHTML(nameElem, newTextMes, "wrongNameDiv");
        return false;
    } else {
        return true;
    }
}

function checkLastname() {
    deleteDiv("wrongLastnameDiv", lastnameElem);
    var newTextMes = checkNameString(lastnameElem.value, "nazwisko");

    if (newTextMes.length > 0) {
        addDivHTML(lastnameElem, newTextMes, "wrongLastnameDiv");
        return false;
    } else {
        return true;
    }
}

function checkNameString(text, label) {
    var newTextMes = "";
    if (text == "") {
        newTextMes = "Podaj " + label;
    } else {
        var reg = new RegExp("^[A-Z-ÓĘĄŚŁŻŹĆŃ][a-z-zóęąśłżźćń]{2,40}$");
        if (reg.test(text) == false) {
            newTextMes = "Podane " + label + " jest nieprawidłowe";
        }
    }
    return newTextMes;
}

function checkLogin() {
    deleteDiv("wrongLoginDiv", loginElem);
    var login = loginElem.value;
    var addDiv = false;
    if (login == "") {
        newTextMes = "Podaj login";
        addDiv = true;
    } else {
        var reg = new RegExp("^[a-z]{2,12}$");
        if (login.length < 2) {
            newTextMes = "Podany login jest za krótki (2-12 znaków)";
            addDiv = true;
        } else if (login.length > 12) {
            newTextMes = "Podany login jest za długi (2-12 znaków)";
            addDiv = true;
        } else if (reg.test(login) == false) {
            newTextMes = "Podany login jest nieprawidłowy (tylko małe litery)";
            addDiv = true;
        } else {
            var url = "https://pi.iem.pw.edu.pl/user/" + login;
            fetch(url).then(
                (response) => {
                    if (response.status == 200) {
                        var errorText = "Podany login jest zajęty";
                        addDivHTML(loginElem, errorText, "wrongLoginDiv");
                        correctLogin = false;
                    } else if (response.status == 404) {
                        correctLogin = true;
                    }
                }
            ).catch(() => {
                var errorText = "Wystąpił błąd połączenia";
                addDivHTML(loginElem, errorText, "wrongLoginDiv");
                correctLogin = false;
            });
        }
    }

    if (addDiv) {
        addDivHTML(loginElem, newTextMes, "wrongLoginDiv");
        return false;
    } else {
        return true;
    }
}

function checkPasswd() {
    deleteDiv("wrongPasswd1Div", passwdElem);

    var passwd = passwdElem.value;
    var addDiv = false;
    if (passwd == "") {
        newTextMes = "Podaj hasło";
        addDiv = true;
    } else {
        var reg = RegExp("^[a-zA-Z]{8,}$");
        if (passwd.length < 8) {
            newTextMes = "Podane hasło jest za krótkie (min. 8 znaków)";
            addDiv = true;
        } else if (reg.test(passwd) == false) {
            newTextMes = "Podane hasło jest nieprawidłowe (znaki A-Z, a-z)";
            addDiv = true;
        }
    }

    if (addDiv) {
        addDivHTML(passwdElem, newTextMes, "wrongPasswd1Div");
        return false;
    } else {
        return true;
    }
}

function checkTheSamePasswd() {
    deleteDiv("wrongPasswd1Div", passwdElem);
    deleteDiv("wrongPasswd2Div", passwd2Elem);

    var firstPasswd = passwdElem.value;
    if (firstPasswd == "") {
        newTextMes = "Najpierw wpisz hasło";
        addDivHTML(passwdElem, newTextMes, "wrongPasswd1Div");
        return false;
    } else {
        var secondPasswd = passwd2Elem.value;
        if (firstPasswd != secondPasswd) {
            newTextMes = "Hasła nie są takie same";
            addDivHTML(passwd2Elem, newTextMes, "wrongPasswd2Div");
            return false;
        }
    }

    return true;
}

function checkBirth() {
    deleteDiv("wrongBirthDiv", birthElem);

    var birthDate = birthElem.value;
    var addDiv = false;
    if (birthDate == "") {
        newTextMes = "Podaj datę urodzenia";
        addDiv = true;
    } else {
        birthDate = birthDate.split("-");
        var year = parseInt(birthDate[0], 10);
        if (isNaN(birthDate[0]) || year < 1900 || year > 3000) {
            newTextMes = "Podany rok jest nieprawidłowy (rok > 1900)";
            addDiv = true;
        } else {
            var month = parseInt(birthDate[1], 10);
            if (isNaN(birthDate[1]) || month < 1 || month > 12) {
                newTextMes = "Podany miesiąc jest nieprawidłowy";
                addDiv = true;
            } else {
                var day = parseInt(birthDate[2], 10);
                if (isNaN(birthDate[2]) || day < 1 || day > 31) {
                    newTextMes = "Podany dzień jest niewłaściwy";
                    addDiv = true;
                }
            }
        }
    }

    if (addDiv) {
        addDivHTML(birthElem, newTextMes, "wrongBirthDiv");
        return false;
    } else {
        return true;
    }
}

function checkPesel() {
    deleteDiv("wrongPeselDiv", peselElem);

    var pesel = peselElem.value;
    var addDiv = false;
    if (pesel == "") {
        newTextMes = "Podaj numer PESEL";
        addDiv = true;
    } else {
        pesel = pesel.split("");
        var sum = 0;
        for (var i = 0; i < 10; i++) {
            if (i % 4 == 0) {
                sum += parseInt(pesel[i], 10);
            } else if (i % 4 == 1) {
                sum += parseInt(pesel[i], 10) * 3;
            } else if (i % 4 == 2) {
                sum += parseInt(pesel[i], 10) * 7;
            } else {
                sum += parseInt(pesel[i], 10) * 9;
            }
        }
        sum += parseInt(pesel[10], 10);
        if (sum % 10 != 0 || pesel.length > 11) {
            newTextMes = "Numer PESEL jest nieprawidłowy";
            addDiv = true;
        }

        if (!addDiv) {
            deleteSexDiv(femaleElem, manElem);
            if (parseInt(pesel[9]) % 2 == 0) {
                femaleElem.checked = true;
            } else {
                manElem.checked = true;
            }
        }
    }

    if (addDiv) {
        addDivHTML(peselElem, newTextMes, "wrongPeselDiv");
        return false;
    } else {
        return true;
    }
}

function checkSex() {
    deleteSexDiv(femaleElem, manElem);

    var addDiv = false;
    var femaleAns = femaleElem.checked;
    var manAns = manElem.checked;
    if (!manAns && !femaleAns) {
        newTextMes = "Zaznacz płeć";
        addDiv = true;
    }

    if (addDiv) {
        addDivSex(femaleElem, manElem, newTextMes);
        return false;
    } else {
        return true;
    }
}

function checkFile() {
    deleteDiv("wrongFileDiv", fileElem);

    var addDiv = false;
    var file = fileElem.value;
    if (file == "") {
        newTextMes = "Dodaj zdjęcie (.jpg, .png, .bmp)";
        addDiv = true;
    } else {
        file = file.split(".");
        if (file[1] != "jpg" && file[1] != "jpeg" && file[1] != "bmp" && file[1] != "png") {
            newTextMes = "Niepoprawny typ pliku (dozwolone .jpg, .png, .bmp)";
            addDiv = true;
        }
    }

    if (addDiv) {
        addDivHTML(fileElem, newTextMes, "wrongFileDiv");
        return false;
    } else {
        return true;
    }
}

function addDivHTML(elemBefore, newTextMes, id) {
    var newDiv = document.createElement("div");
    var newContent = document.createTextNode(newTextMes);
    newDiv.setAttribute("id", id);
    newDiv.setAttribute("class", "errorDiv");
    newDiv.appendChild(newContent);
    elemBefore.setAttribute("class", "error");
    var parent = elemBefore.parentNode;
    parent.insertBefore(newDiv, elemBefore.nextSibling);
}

function addDivSex(femaleElem, manElem, newTextMes) {
    var newDiv = document.createElement("div");
    var newContent = document.createTextNode(newTextMes);
    newDiv.setAttribute("id", "wrongSexDiv");
    newDiv.setAttribute("class", "errorDiv");
    newDiv.appendChild(newContent);
    femaleElem.setAttribute("class", "error");
    manElem.setAttribute("class", "error");
    var maleInput = document.getElementById("maleLabel");
    var parent = maleInput.parentNode;
    parent.insertBefore(newDiv, maleInput.nextSibling);
}

function deleteDiv(divId, elem) {
    var wrongDiv = document.getElementById(divId);
    if (wrongDiv != null) {
        wrongDiv.remove();
        elem.setAttribute("class", "elem");
    }
}

function deleteSexDiv(femaleElem, manElem) {
    var wrongDiv = document.getElementById("wrongSexDiv");
    if (wrongDiv != null) {
        wrongDiv.remove();
        femaleElem.setAttribute("class", "radio");
        manElem.setAttribute("class", "radio");
    }
}

function validateForm(e) {
    e.preventDefault();
    var result1 = checkName();
    var result2 = checkLastname();
    var result4 = checkPasswd();
    var result5 = checkTheSamePasswd();
    var result6 = checkBirth();
    var result7 = checkPesel();
    var result8 = checkSex();
    var result9 = checkFile();
    if (result1 && result2 && correctLogin && result4 && result5 && result6 && result7 && result8 && result9) {
        document.forms[0].submit();
    }
}