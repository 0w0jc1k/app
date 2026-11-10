package com.example;   // ← dopasuj do pakietu z Application.java

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Ekran logowania (wspólny dla klientów i pracowników).
 *
 * Odpowiada przypadkom użycia:
 *  - Wypożyczalnia PU02. Zalogowanie się (Klient),
 *  - Administracja PU14. Zalogowanie się (Pracownik niezalogowany),
 *  - Wypożyczalnia PU01. Zarejestrowanie konta (Gość) – przycisk "Utwórz konto klienta",
 *  - Wypożyczalnia PU04. Przeglądanie katalogu kostiumów (Gość) – przycisk "Przeglądaj katalog".
 *
 * Na razie tylko wygląd: logowanie nie jest podłączone.
 */
@Route(value = "login", autoLayout = false)   // bez menu bocznego
@PageTitle("Logowanie | Wypożyczalnia kostiumów")
public class LoginView extends Main {

    // Adresy ekranów, które powstaną w kolejnych krokach.
    private static final String ROUTE_REJESTRACJA = "rejestracja";
    private static final String ROUTE_KATALOG = "katalog";

    private final LoginForm login = new LoginForm(createI18n());

    public LoginView() {
        setSizeFull();

        // --- Nagłówek ---
        Span ikona = new Span("🎭");
        ikona.getStyle().set("font-size", "3rem").set("line-height", "1");
        ikona.getElement().setAttribute("aria-hidden", "true");

        H1 nazwa = new H1("Wypożyczalnia kostiumów i strojów okolicznościowych");
        nazwa.getStyle()
                .set("font-size", "1.5rem")
                .set("margin", "0")
                .set("text-align", "center");

        Paragraph opis = new Paragraph(
                "Zaloguj się, aby rezerwować stroje lub obsługiwać wypożyczalnię.");
        opis.getStyle()
                .set("margin", "0")
                .set("text-align", "center")
                .set("opacity", "0.7");

        // --- Formularz logowania (e-mail + hasło) ---
        login.setForgotPasswordButtonVisible(false);   // odzyskiwania hasła nie ma w wymaganiach
        login.addLoginListener(e -> {
            // TYMCZASOWO: pokazuje wygląd komunikatu o błędzie.
            // Docelowo tutaj będzie sprawdzenie danych (PU02, krok 4).
            login.setError(true);
            login.setEnabled(true);
        });

        // --- Akcje dla osób bez konta (Gość) ---
        Button rejestracja = new Button("Utwórz konto klienta", VaadinIcon.USER.create(),
                e -> getUI().ifPresent(ui -> ui.navigate(ROUTE_REJESTRACJA)));
        rejestracja.setWidthFull();

        Button katalog = new Button("Przeglądaj katalog bez logowania", VaadinIcon.SEARCH.create(),
                e -> getUI().ifPresent(ui -> ui.navigate(ROUTE_KATALOG)));
        katalog.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        katalog.setWidthFull();

        VerticalLayout akcje = new VerticalLayout(rejestracja, katalog);
        akcje.setPadding(false);
        akcje.setMaxWidth("21rem");
        akcje.setWidthFull();

        // --- Układ strony (działa też na tablecie i telefonie) ---
        VerticalLayout layout = new VerticalLayout(ikona, nazwa, opis, login, akcje);
        layout.setSizeFull();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.getStyle().set("overflow", "auto");

        add(layout);
    }

    private static LoginI18n createI18n() {
        LoginI18n i18n = LoginI18n.createDefault();

        LoginI18n.Form form = i18n.getForm();
        form.setTitle("Logowanie");
        form.setUsername("Adres e-mail");
        form.setPassword("Hasło");
        form.setSubmit("Zaloguj się");

        // Komunikat obejmuje też konto dezaktywowane (Administracja PU04).
        LoginI18n.ErrorMessage error = i18n.getErrorMessage();
        error.setTitle("Nie udało się zalogować");
        error.setMessage("Sprawdź adres e-mail i hasło. Jeśli dane są poprawne, "
                + "konto mogło zostać dezaktywowane. Skontaktuj się wtedy z kierownikiem.");

        i18n.setAdditionalInformation("Konta pracowników zakłada kierownik.");
        return i18n;
    }
}