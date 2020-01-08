function bindPresenterWithView(presenter, view) {
    function wrapper(e, m, f) {
        presenter.model[m] = e.target.value;
        f.bind(presenter.model)();
        presenter.model.isSubmitEnabled = presenter.canSubmitForm.bind(presenter.model)();
        view.loadFromModel();
    }
    view.year.addEventListener("change", e => { wrapper(e, 'year', presenter.onYearChange); });
    view.form.addEventListener("submit", e => { presenter.onFormSubmit(); e.preventDefault(); });
    view.loadFromModel();
}

bindPresenterWithView(presenter, view);