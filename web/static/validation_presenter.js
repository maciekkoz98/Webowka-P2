data = {
    year: '', isYearErrorVisible: false,
    isSubmitEnabled: false
}

presenter = {
    model: data,
    onYearChange() {
        var yearNum = parseInt(this.year, 10);
        if (isNaN(yearNum), yearNum < 2500 || yearNum > 0) {
            this.isYearErrorVisible = true;
        } else {
            this.isYearErrorVisible = false;
        }
    },
    onFormSubmit() {
        if (this.canSubmitForm()) {
            document.forms['addPubForm'].submit();
        }
    },
    canSubmitForm() {
        if (this.isYearErrorVisible)
            return false;
        return true;
    }
}

view.model = presenter.model;