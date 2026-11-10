package com.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;


@Route(value = "rejestracja", autoLayout = false)   // bez menu bocznego
@PageTitle("Rejestracja")
public class RejestracjaView extends VerticalLayout {

    // Adres, który w makiecie udaje "konto już istnieje" (przebieg alternatywny)
    private static final String ZAJETY_EMAIL = "zajety@example.com";

    private final TextField imie = new TextField("Imię");
    private final TextField nazwisko = new TextField("Nazwisko");
    private final TextField telefon = new TextField("Numer telefonu");
    private final EmailField email = new EmailField("Adres e-mail");
    private final PasswordField haslo = new PasswordField("Hasło");
    private final PasswordField powtorzHaslo = new PasswordField("Powtórz hasło");

    // krok 3.1 - klient instytucjonalny
    private final Checkbox instytucjonalny = new Checkbox("Rejestruję się jako instytucja");
    private final Checkbox regulamin = new Checkbox("Zapoznałem się z regulaminem");
    private final TextField nazwaInstytucji = new TextField("Nazwa instytucji");

    public RejestracjaView() {
        setWidthFull();                          // widok zajmuje całą szerokość strony
        setAlignItems(Alignment.CENTER);         // elementy wyśrodkowane w poziomie
        setJustifyContentMode(JustifyContentMode.START); // nagłówek zostaje na górze
        setJustifyContentMode(JustifyContentMode.START);
        setSizeFull();
        setPadding(true);

        konfigurujPola();

        FormLayout form = new FormLayout();
        form.add(imie, nazwisko, telefon, email, haslo, powtorzHaslo, instytucjonalny, nazwaInstytucji,
            regulamin);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));
        // checkbox i nazwa instytucji na całą szerokość
        form.setColspan(instytucjonalny, 2);
        form.setColspan(nazwaInstytucji, 2);
        form.setColspan(email, 2);
        form.setColspan(regulamin, 2);
        form.setWidth("100%");
        form.setMaxWidth("600px");
        form.getStyle().set("margin", "0 auto"); // formularz na środku strony

        Button zarejestruj = new Button("Zarejestruj się", e -> zatwierdz());
        zarejestruj.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button zalogujSie = new Button("Mam już konto", e -> UI.getCurrent().navigate("login"));
        zalogujSie.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        add(new H2("Rejestracja konta"),
            new Paragraph("Utwórz konto, aby rezerwować i wypożyczać stroje."),
            form,
            zarejestruj,
            zalogujSie);
    }

    private void konfigurujPola() {
        for (var pole : new com.vaadin.flow.component.HasValueAndElement<?, ?>[]
                {imie, nazwisko, telefon, email, haslo, powtorzHaslo, nazwaInstytucji}) {
            pole.setRequiredIndicatorVisible(true);
        }
        regulamin.setRequiredIndicatorVisible(true);

        telefon.setPlaceholder("np. 600 123 456");
        telefon.setAllowedCharPattern("[0-9+ ]");
        telefon.setMaxLength(15);

        email.setPlaceholder("jan.kowalski@example.com");
        haslo.setHelperText("Minimum 8 znaków");

        // pole instytucji widoczne tylko po zaznaczeniu checkboxa
        nazwaInstytucji.setVisible(false);
        instytucjonalny.addValueChangeListener(e -> {
            nazwaInstytucji.setVisible(e.getValue());
            if (!e.getValue()) {
                nazwaInstytucji.clear();
                nazwaInstytucji.setInvalid(false);
            }
        });
    }

    /** Krok 4 - zatwierdzenie rejestracji + przebieg alternatywny. */
    private void zatwierdz() {
        boolean poprawne = true;

        poprawne &= wymagane(imie);
        poprawne &= wymagane(nazwisko);
        poprawne &= wymagane(telefon);
        poprawne &= wymagane(email);
        poprawne &= wymagane(haslo);
        poprawne &= wymagane(powtorzHaslo);
        if (instytucjonalny.getValue()) {
            poprawne &= wymagane(nazwaInstytucji);
        }
        if (!regulamin.getValue()) {
            regulamin.setErrorMessage("Akceptacja regulaminu jest wymagana");
            regulamin.setInvalid(true);
            poprawne = false;
        } else {
            regulamin.setInvalid(false);
        }

        // dodatkowe reguły
        if (!telefon.isEmpty() && telefon.getValue().replaceAll("[^0-9]", "").length() < 9) {
            blad(telefon, "Numer telefonu musi mieć co najmniej 9 cyfr");
            poprawne = false;
        }
        if (!email.isEmpty() && email.isInvalid()) {
            blad(email, "Niepoprawny adres e-mail");
            poprawne = false;
        }
        if (!haslo.isEmpty() && haslo.getValue().length() < 8) {
            blad(haslo, "Hasło musi mieć minimum 8 znaków");
            poprawne = false;
        }
        if (!haslo.isEmpty() && !powtorzHaslo.isEmpty()
                && !haslo.getValue().equals(powtorzHaslo.getValue())) {
            blad(powtorzHaslo, "Hasła nie są takie same");
            poprawne = false;
        }

        if (!poprawne) {
            pokazBlad("Dane są niepoprawne lub niekompletne.");
            return;
        }

        // symulacja: konto z tym adresem e-mail już istnieje
        if (ZAJETY_EMAIL.equalsIgnoreCase(email.getValue().trim())) {
            blad(email, "Konto z tym adresem e-mail już istnieje");
            pokazBlad("Konto z podanym adresem e-mail już istnieje.");
            return;
        }

        // sukces - warunek końcowy: Gość staje się Klientem
        Notification n = Notification.show("Konto zostało utworzone. Możesz się zalogować.");
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("login");
    }

    // --- pomocnicze ---

    private boolean wymagane(com.vaadin.flow.component.textfield.TextFieldBase<?, String> pole) {
        if (pole.isEmpty() || pole.getValue().isBlank()) {
            blad(pole, "To pole jest wymagane");
            return false;
        }
        pole.setInvalid(false);
        return true;
    }

    private void blad(com.vaadin.flow.component.textfield.TextFieldBase<?, String> pole, String komunikat) {
        pole.setErrorMessage(komunikat);
        pole.setInvalid(true);
    }

    private void pokazBlad(String tekst) {
        Notification n = Notification.show(tekst, 4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}