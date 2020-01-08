view = {
    year: document.forms['addPubForm']['year'],
    submit: document.forms['addPubForm']['submit'],
    form: document.forms['addPubForm'],
    errors: {},
    loadFromModel: () => {
        m = view.model;
        v = view;
        v.year.value = m.year;
        v.submit.disabled = !m.isSubmitEnabled;
        v.errors.year.hidden = !m.isYearErrorVisible;
    }
}

function createViewErrors(view) {
    function createErrorFor(component, message) {
        error = document.createElement('div');
        error.innerHTML = message || "Niewłaściwe dane";
        component.parentElement.appendChild(error);
        return error;
    }
    
    view.errors.year = createErrorFor(view.year, "Podany rok jest nieprawidłowy! (1900-2100)");
}

function checkView(view) {
    for (var component in view) {
        if (view.hasOwnProperty(component)) {
            if (!view[component]) {
                return false;
            }
        }
    }
    return true;
}

if (checkView(view)) {
    createViewErrors(view);
}