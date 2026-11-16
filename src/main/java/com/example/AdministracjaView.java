package com.example;

import java.util.ArrayList;
import java.util.List;

import static com.example.WspolneElementy.blad;
import static com.example.WspolneElementy.bladPola;
import static com.example.WspolneElementy.naglowek;
import static com.example.WspolneElementy.sukces;
import static com.example.WspolneElementy.wymagane;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Moduł Administracja (Rysunek 2):
 *  PU01. Zarządzanie kontami pracowników lub kierowników (lista kont),
 *  PU02. Dodanie konta, PU03. Edytowanie danych konta, PU04. Dezaktywowanie konta.
 *
 * Makieta - dane w pamięci, bez bazy. Zalogowany jest "Jack Sparrow (Kierownik)".
 */
@Route(value = "administracja", autoLayout = false)   // bez menu bocznego
@PageTitle("Administracja | Wypożyczalnia kostiumów")
public class AdministracjaView extends VerticalLayout {

    enum RolaKonta {
        PRACOWNIK_OBSLUGI("Pracownik obsługi"),
        PRACOWNIK_GARDEROBY("Pracownik garderoby"),
        SERWISANT("Serwisant"),
        KIEROWNIK("Kierownik");

        private final String etykieta;

        RolaKonta(String etykieta) {
            this.etykieta = etykieta;
        }

        String etykieta() {
            return etykieta;
        }
    }

    static class Konto {
        String imie;
        String nazwisko;
        String email;
        String haslo;
        RolaKonta rola;
        boolean aktywne;

        Konto(String imie, String nazwisko, String email, String haslo, 
                RolaKonta rola, boolean aktywne) {
            this.imie = imie;
            this.nazwisko = nazwisko;
            this.email = email;
            this.haslo = haslo;
            this.rola = rola;
            this.aktywne = aktywne;
        }

        String pelneImie() {
            return imie + " " + nazwisko;
        }
    }

    private final Konto zalogowany = new Konto("Jack", "Sparrow", "jack.sparrow@example.com",
            "Haslo1234", RolaKonta.KIEROWNIK, true);
    private final List<Konto> konta = new ArrayList<>();

    private final Grid<Konto> siatka = new Grid<>(Konto.class, false);
    private GridListDataView<Konto> widok;
    private final TextField szukaj = new TextField();
    private final Checkbox pokazNieaktywne = new Checkbox("Pokaż dezaktywowane", true);

    public AdministracjaView() {
        setWidthFull();
        setAlignItems(Alignment.CENTER);

        konta.add(zalogowany);
        konta.add(new Konto("Anna", "Nowak", "anna.nowak@example.com", "Haslo1234", 
                RolaKonta.PRACOWNIK_OBSLUGI, true));
        konta.add(new Konto("Marek", "Zieliński", "marek.zielinski@example.com", "Haslo1234",
                RolaKonta.PRACOWNIK_OBSLUGI, true));
        konta.add(new Konto("Ewa", "Lis", "ewa.lis@example.com", "Haslo1234",
                RolaKonta.PRACOWNIK_GARDEROBY, true));
        konta.add(new Konto("Tomasz", "Krawiec", "tomasz.krawiec@example.com", "Haslo1234",
                RolaKonta.SERWISANT, true));
        konta.add(new Konto("Barbara", "Stara", "barbara.stara@example.com", "Haslo1234",
                RolaKonta.SERWISANT, false));

        Button dodaj = new Button("Dodaj konto", VaadinIcon.PLUS.create(), e -> otworzFormularz(null));
        dodaj.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        konfigurujFiltry();
        konfigurujSiatke();

        HorizontalLayout filtry = new HorizontalLayout(szukaj, pokazNieaktywne);
        filtry.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);

        VerticalLayout tresc = new VerticalLayout();
        tresc.setPadding(false);
        tresc.setWidthFull();
        tresc.setMaxWidth("1100px");
        tresc.add(
                naglowek("Administracja",
                        "Konta pracowników i kierowników: dodawanie, edycja i dezaktywacja.", dodaj),
                filtry,
                siatka);
        add(tresc);
    }

    // ------------------------------------------------------------------
    // PU01. Lista kont
    // ------------------------------------------------------------------

    private void konfigurujFiltry() {
        szukaj.setPlaceholder("Szukaj po imieniu, nazwisku, e-mailu lub roli");
        szukaj.setPrefixComponent(VaadinIcon.SEARCH.create());
        szukaj.setClearButtonVisible(true);
        szukaj.setWidth("380px");
        szukaj.setValueChangeMode(ValueChangeMode.EAGER);
        szukaj.addValueChangeListener(e -> zastosujFiltr());
        pokazNieaktywne.addValueChangeListener(e -> zastosujFiltr());
    }

    private void konfigurujSiatke() {
        siatka.addColumn(Konto::pelneImie).setHeader("Imię i nazwisko").setAutoWidth(true);
        siatka.addColumn(k -> k.email).setHeader("Adres e-mail").setAutoWidth(true);
        siatka.addColumn(k -> k.rola.etykieta()).setHeader("Rola").setAutoWidth(true);
        siatka.addComponentColumn(k -> {
            Span s = new Span(k.aktywne ? "Aktywne" : "Dezaktywowane");
            s.getElement().getThemeList().add("badge");
            s.getElement().getThemeList().add(k.aktywne ? "success" : "error");
            return s;
        }).setHeader("Status").setAutoWidth(true);
        siatka.addComponentColumn(k -> {
            Button edytuj = new Button("Edytuj", VaadinIcon.EDIT.create(), e -> otworzFormularz(k));
            edytuj.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            HorizontalLayout akcje = new HorizontalLayout(edytuj);
            if (k.aktywne) {   // PU04: dezaktywować można tylko konto aktywne
                Button dez = new Button("Dezaktywuj", VaadinIcon.BAN.create(), e -> dezaktywuj(k));
                dez.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_SMALL);
                akcje.add(dez);
            }
            return akcje;
        }).setHeader("Akcje").setAutoWidth(true);

        siatka.setAllRowsVisible(true);
        widok = siatka.setItems(konta);
    }

    private void zastosujFiltr() {
        String fraza = szukaj.getValue().trim().toLowerCase();
        boolean pokaz = pokazNieaktywne.getValue();
        widok.setFilter(k -> (pokaz || k.aktywne)
                && (fraza.isEmpty()
                        || (k.pelneImie() + " " + k.email + " " + k.rola.etykieta())
                                .toLowerCase().contains(fraza)));
    }

    // ------------------------------------------------------------------
    // PU02. Dodanie konta (edytowane == null) i PU03. Edytowanie danych konta
    // ------------------------------------------------------------------

    private void otworzFormularz(Konto edytowane) {
        boolean nowe = edytowane == null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(nowe ? "Dodanie konta" : "Edycja konta");
        dialog.setWidth("min(600px, 95vw)");

        TextField imie = new TextField("Imię");
        TextField nazwisko = new TextField("Nazwisko");
        EmailField email = new EmailField("Adres e-mail");
        PasswordField haslo = new PasswordField(nowe ? "Hasło" : "Nowe hasło");
        ComboBox<RolaKonta> rola = new ComboBox<>("Rola");

        rola.setItems(RolaKonta.values());
        rola.setItemLabelGenerator(RolaKonta::etykieta);
        haslo.setHelperText(nowe ? "Minimum 8 znaków" : "Zostaw puste, aby nie zmieniać hasła");
        email.setPlaceholder("jan.kowalski@example.com");

        for (var pole : new com.vaadin.flow.component.HasValueAndElement<?, ?>[] {
                imie, nazwisko, email, rola}) {
            pole.setRequiredIndicatorVisible(true);
        }
        haslo.setRequiredIndicatorVisible(nowe);

        if (!nowe) {
            imie.setValue(edytowane.imie);
            nazwisko.setValue(edytowane.nazwisko);
            email.setValue(edytowane.email);
            rola.setValue(edytowane.rola);
            if (edytowane == zalogowany) {   // żeby kierownik nie odebrał sobie uprawnień
                rola.setReadOnly(true);
                rola.setHelperText("Nie możesz zmienić własnej roli.");
            }
        }

        FormLayout form = new FormLayout(imie, nazwisko, email, haslo, rola);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));
        form.setColspan(email, 2);
        dialog.add(form);

        Button anuluj = new Button("Anuluj", e -> dialog.close());
        Button zapisz = new Button(nowe ? "Utwórz konto" : "Zapisz zmiany");
        zapisz.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(anuluj, zapisz);

        zapisz.addClickListener(e -> {
            boolean ok = true;
            ok &= wymagane(imie, imie.getValue());
            ok &= wymagane(nazwisko, nazwisko.getValue());
            ok &= wymagane(email, email.getValue());
            ok &= wymagane(rola, rola.isEmpty(), "Wybierz rolę");
            if (nowe) {
                ok &= wymagane(haslo, haslo.getValue());
            }

            String adres = email.getValue() == null ? "" : email.getValue().trim();
            if (!adres.isEmpty() && !adres.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                bladPola(email, "Niepoprawny adres e-mail");
                ok = false;
            }
            if (!haslo.isEmpty() && haslo.getValue().length() < 8) {
                bladPola(haslo, "Hasło musi mieć minimum 8 znaków");
                ok = false;
            }
            if (!ok) {
                blad("Dane są niepoprawne lub niekompletne.");   // alternatywny: krok 4
                return;
            }
            if (konta.stream().anyMatch(k -> k != edytowane && k.email.equalsIgnoreCase(adres))) {
                bladPola(email, "Konto z tym adresem e-mail już istnieje");
                blad("Konto z podanym adresem e-mail już istnieje.");
                return;
            }

           
            if (nowe) {
                konta.add(new Konto(imie.getValue().trim(), nazwisko.getValue().trim(), adres,
                        haslo.getValue(), rola.getValue(), true));
                sukces("Konto zostało utworzone.");
            } else {
                edytowane.imie = imie.getValue().trim();
                edytowane.nazwisko = nazwisko.getValue().trim();
                edytowane.email = adres;
                edytowane.rola = rola.getValue();
                if (!haslo.isEmpty()) {
                    edytowane.haslo = haslo.getValue();
                }
                sukces("Dane konta zostały zmienione.");
            }
            widok.refreshAll();
            dialog.close();
        });

        dialog.open();
    }

    // ------------------------------------------------------------------
    // PU04. Dezaktywowanie konta
    // ------------------------------------------------------------------

    private void dezaktywuj(Konto konto) {
        if (konto == zalogowany) {
            blad("Nie możesz dezaktywować własnego konta.");
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Dezaktywacja konta");
        dialog.setText("Czy na pewno dezaktywować konto: " + konto.pelneImie() + " ("
                + konto.email + ")? Nie będzie można się na nie zalogować.");
        dialog.setCancelable(true);
        dialog.setCancelText("Anuluj");
        dialog.setConfirmText("Dezaktywuj");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            konto.aktywne = false;
            widok.refreshAll();
            sukces("Konto zostało dezaktywowane.");
        });
        dialog.open();
    }
}