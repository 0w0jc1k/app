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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Zarządzanie asortymentem - PU05 (Zarządzanie garderobą w module Administracja):
 *  PU06. Dodanie nowego modelu, PU07. Dodanie egzemplarza,
 *  PU08. Wycofanie modelu, PU09. Wycofanie egzemplarza.
 *
 * Wycofać można tylko egzemplarz o statusie "dostępny" (zgodnie z Garderoba PU08:
 * nie wolno wycofać zarezerwowanego, wypożyczonego ani będącego w czyszczeniu/naprawie).
 * Wycofanie modelu wycofuje też wszystkie jego egzemplarze.
 */
@Route(value = "asortyment", autoLayout = false)   // bez menu bocznego
@PageTitle("Zarządzanie asortymentem | Wypożyczalnia kostiumów")
public class AsortymentView extends VerticalLayout {

    enum Status {
        DOSTEPNY("dostępny"),
        ZAREZERWOWANY("zarezerwowany"),
        WYPOZYCZONY("wypożyczony"),
        W_CZYSZCZENIU("w czyszczeniu"),
        W_NAPRAWIE("w naprawie"),
        WYCOFANY("wycofany");

        private final String etykieta;

        Status(String etykieta) {
            this.etykieta = etykieta;
        }

        String etykieta() {
            return etykieta;
        }

        boolean blokujeWycofanie() {
            return this != DOSTEPNY && this != WYCOFANY;
        }
    }

    static class Model {
        String nazwa;
        String kategoria;
        String opis;
        double stawka;
        boolean wycofany;

        Model(String nazwa, String kategoria, String opis, double stawka) {
            this.nazwa = nazwa;
            this.kategoria = kategoria;
            this.opis = opis;
            this.stawka = stawka;
        }
    }

    static class Egzemplarz {
        String id;
        Model model;
        String rozmiar;
        String kolor;
        String stan;
        String lokalizacja;
        Status status;

        Egzemplarz(String id, Model model, String rozmiar, String kolor, String stan,
                String lokalizacja, Status status) {
            this.id = id;
            this.model = model;
            this.rozmiar = rozmiar;
            this.kolor = kolor;
            this.stan = stan;
            this.lokalizacja = lokalizacja;
            this.status = status;
        }
    }

    private static final List<String> KATEGORIE = List.of("Postacie filmowe i bajkowe",
            "Stroje historyczne", "Mundury", "Suknie wieczorowe", "Garnitury", "Stroje ludowe",
            "Przebrania karnawałowe", "Dodatki");
    private static final List<String> ROZMIARY = List.of("XS", "S", "M", "L", "XL");
    private static final List<String> STANY = List.of("Nowy", "Bardzo dobry", "Dobry", "Zużyty");

    private final List<Model> modele = new ArrayList<>();
    private final List<Egzemplarz> egzemplarze = new ArrayList<>();

    private final Grid<Model> modeleGrid = new Grid<>(Model.class, false);
    private final Grid<Egzemplarz> egzGrid = new Grid<>(Egzemplarz.class, false);
    private GridListDataView<Model> modeleWidok;
    private GridListDataView<Egzemplarz> egzWidok;

    private final ComboBox<Model> filtrModelu = new ComboBox<>("Model");
    private final ComboBox<Status> filtrStatusu = new ComboBox<>("Status");
    private final Div sekcjaModeli = new Div();
    private final Div sekcjaEgz = new Div();
    private final Button dodajModel = new Button("Dodaj model", VaadinIcon.PLUS.create());
    private final Button dodajEgzemplarz =
        new Button("Dodaj egzemplarz", VaadinIcon.PLUS.create());

    public AsortymentView() {
        setWidthFull();
        setAlignItems(Alignment.CENTER);

        dodajDaneTestowe();
        konfigurujModele();
        konfigurujEgzemplarze();

        dodajModel.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dodajModel.addClickListener(e -> otworzFormularzModelu());
        dodajEgzemplarz.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
dodajEgzemplarz.addClickListener(e -> otworzFormularzEgzemplarza());

        Tab tabModele = new Tab("Modele");
        Tab tabEgz = new Tab("Egzemplarze");
        Tabs zakladki = new Tabs(tabModele, tabEgz);
        zakladki.addSelectedChangeListener(e -> {
            boolean modeleAktywne = zakladki.getSelectedTab() == tabModele;
            sekcjaModeli.setVisible(modeleAktywne);
            sekcjaEgz.setVisible(!modeleAktywne);
            dodajModel.setVisible(modeleAktywne);
        });
        sekcjaEgz.setVisible(false);

        VerticalLayout tresc = new VerticalLayout();
        tresc.setPadding(false);
        tresc.setWidthFull();
        tresc.setMaxWidth("1100px");
        tresc.add(
                naglowek("Garderoba",
                        "Dodawanie i wycofywanie modeli oraz egzemplarzy.", dodajModel),
                zakladki, sekcjaModeli, sekcjaEgz);
        add(tresc);
    }

    // ------------------------------------------------------------------
    // Dane testowe
    // ------------------------------------------------------------------

    private void dodajDaneTestowe() {
        Model balowa = model("Suknia balowa „Księżniczka”", "Suknie wieczorowe",
                "Długa suknia z tiulową spódnicą i gorsetem.", 120);
        Model garnitur = model("Garnitur wieczorowy", "Garnitury",
                "Czarny garnitur z muszką: marynarka, spodnie, kamizelka.", 90);
        Model pirat = model("Kostium Pirata", "Przebrania karnawałowe",
                "Płaszcz, kapelusz i pas z akcesoriami.", 70);
        Model mikolaj = model("Strój Mikołaja", "Przebrania karnawałowe",
                "Czerwony strój z futerkiem, brodą i czapką.", 60);
        Model elf = model("Kostium Elfa", "Przebrania karnawałowe",
                "Zielono-czerwony kostium z czapką i butami.", 50);
        Model krakowski = model("Strój krakowski", "Stroje ludowe",
                "Tradycyjny strój z gorsetem, spódnicą i wiankiem.", 70);
        Model czarownica = model("Kostium Czarownicy", "Przebrania karnawałowe",
                "Czarna suknia z kapeluszem i peleryną.", 65);
        Model wieczorowa = model("Suknia wieczorowa", "Suknie wieczorowe",
                "Długa suknia z rozcięciem, na studniówkę lub galę.", 90);

        egz(balowa, "EGZ-101", "S", "Błękitny", "Bardzo dobry", "Regał A1", Status.DOSTEPNY);
        egz(balowa, "EGZ-102", "M", "Różowy", "Dobry", "Regał A1", Status.ZAREZERWOWANY);
        egz(garnitur, "EGZ-111", "L", "Czarny", "Dobry", "Regał B2", Status.WYPOZYCZONY);
        egz(garnitur, "EGZ-112", "XL", "Granatowy", "Bardzo dobry", "Regał B2", Status.DOSTEPNY);
        egz(pirat, "EGZ-121", "M", "Brązowy", "Zużyty", "Wieszak C1", Status.W_NAPRAWIE);
        egz(pirat, "EGZ-122", "L", "Brązowy", "Dobry", "Wieszak C1", Status.DOSTEPNY);
        egz(mikolaj, "EGZ-131", "L", "Czerwony", "Dobry", "Wieszak C2", Status.DOSTEPNY);
        egz(elf, "EGZ-141", "S", "Zielony", "Nowy", "Wieszak C3", Status.W_CZYSZCZENIU);
        egz(krakowski, "EGZ-151", "M", "Czerwony", "Dobry", "Regał D1", Status.DOSTEPNY);
        egz(czarownica, "EGZ-161", "M", "Czarny", "Bardzo dobry", "Wieszak C4", Status.DOSTEPNY);
        egz(wieczorowa, "EGZ-171", "S", "Czerwony", "Nowy", "Regał A2", Status.DOSTEPNY);
        egz(wieczorowa, "EGZ-172", "M", "Czarny", "Dobry", "Regał A2", Status.WYCOFANY);
    }

    private Model model(String nazwa, String kategoria, String opis, double stawka) {
        Model m = new Model(nazwa, kategoria, opis, stawka);
        modele.add(m);
        return m;
    }

    private void egz(Model m, String id, String rozmiar, String kolor, String stan,
            String lokalizacja, Status status) {
        egzemplarze.add(new Egzemplarz(id, m, rozmiar, kolor, stan, lokalizacja, status));
    }

    // ------------------------------------------------------------------
    // Lista modeli
    // ------------------------------------------------------------------

    private void konfigurujModele() {
        modeleGrid.addColumn(m -> m.nazwa).setHeader("Nazwa").setAutoWidth(true);
        modeleGrid.addColumn(m -> m.kategoria).setHeader("Kategoria").setAutoWidth(true);
        modeleGrid.addColumn(m -> String.format("%.0f zł / doba", m.stawka)).setHeader("Stawka");
        modeleGrid.addColumn(m -> egzemplarze.stream()
                .filter(e -> e.model == m && e.status != Status.WYCOFANY).count())
                .setHeader("Egzemplarze");
        modeleGrid.addComponentColumn(m -> {
            Span s = new Span(m.wycofany ? "Wycofany" : "W ofercie");
            s.getElement().getThemeList().add("badge");
            s.getElement().getThemeList().add(m.wycofany ? "error" : "success");
            return s;
        }).setHeader("Status");
        modeleGrid.addComponentColumn(m -> {
            HorizontalLayout akcje = new HorizontalLayout();
            if (!m.wycofany) {
                
                Button wycofaj = new Button("Wycofaj", VaadinIcon.BAN.create(),
                        e -> wycofajModel(m));
                wycofaj.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_SMALL);
                akcje.add(wycofaj);
            }
            return akcje;
        }).setHeader("Akcje").setAutoWidth(true);
        modeleGrid.setAllRowsVisible(true);
        modeleWidok = modeleGrid.setItems(modele);
        sekcjaModeli.add(modeleGrid);
        sekcjaModeli.setWidthFull();
    }

    // ------------------------------------------------------------------
    // Lista egzemplarzy
    // ------------------------------------------------------------------

    private void konfigurujEgzemplarze() {
        filtrModelu.setItems(modele);
        filtrModelu.setItemLabelGenerator(m -> m.nazwa);
        filtrModelu.setClearButtonVisible(true);
        filtrModelu.setPlaceholder("Wszystkie modele");
        filtrModelu.setWidth("320px");
        filtrModelu.addValueChangeListener(e -> filtrujEgzemplarze());

        filtrStatusu.setItems(Status.values());
        filtrStatusu.setItemLabelGenerator(Status::etykieta);
        filtrStatusu.setClearButtonVisible(true);
        filtrStatusu.setPlaceholder("Wszystkie statusy");
        filtrStatusu.addValueChangeListener(e -> filtrujEgzemplarze());

        egzGrid.addColumn(e -> e.id).setHeader("Numer egzemplarza").setAutoWidth(true);
        egzGrid.addColumn(e -> e.model.nazwa).setHeader("Model").setAutoWidth(true);
        egzGrid.addColumn(e -> e.rozmiar).setHeader("Rozmiar");
        egzGrid.addColumn(e -> e.kolor).setHeader("Kolor");
        egzGrid.addColumn(e -> e.stan).setHeader("Stan zachowania");
        egzGrid.addColumn(e -> e.lokalizacja).setHeader("Lokalizacja");
        egzGrid.addComponentColumn(e -> znacznikStatusu(e.status)).setHeader("Status");
        egzGrid.addComponentColumn(e -> {
            HorizontalLayout akcje = new HorizontalLayout();
            if (e.status != Status.WYCOFANY) {
                Button wycofaj = new Button("Wycofaj", VaadinIcon.BAN.create(),
                        ev -> wycofajEgzemplarz(e));
                wycofaj.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_SMALL);
                akcje.add(wycofaj);
            }
            return akcje;
        }).setHeader("Akcje").setAutoWidth(true);
        egzGrid.setAllRowsVisible(true);
        egzWidok = egzGrid.setItems(egzemplarze);

        HorizontalLayout filtry = new HorizontalLayout(
        dodajEgzemplarz,
        filtrModelu,
        filtrStatusu
);

filtry.setAlignItems(Alignment.END);

sekcjaEgz.add(filtry, egzGrid);
sekcjaEgz.setWidthFull();
    }

    private void filtrujEgzemplarze() {
        Model m = filtrModelu.getValue();
        Status s = filtrStatusu.getValue();
        egzWidok.setFilter(e -> (m == null || e.model == m) && (s == null || e.status == s));
    }

    private Span znacznikStatusu(Status status) {
        Span s = new Span(status.etykieta());
        s.getElement().getThemeList().add("badge");
        switch (status) {
            case DOSTEPNY -> s.getElement().getThemeList().add("success");
            case W_CZYSZCZENIU, W_NAPRAWIE -> s.getElement().getThemeList().add("warning");
            case WYCOFANY -> s.getElement().getThemeList().add("error");
            default -> s.getElement().getThemeList().add("contrast");
        }
        return s;
    }

    private void odswiezWszystko() {
        modeleWidok.refreshAll();
        egzWidok.refreshAll();
        filtrModelu.setItems(modele);
    }

    // ------------------------------------------------------------------
    // PU06. Dodanie nowego modelu
    // ------------------------------------------------------------------

    private void otworzFormularzModelu() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Dodanie nowego modelu");
        dialog.setWidth("min(500px, 95vw)");

        TextField nazwa = new TextField("Nazwa");
        ComboBox<String> kategoria = new ComboBox<>("Kategoria");
        kategoria.setItems(KATEGORIE);
        TextArea opis = new TextArea("Opis");
        NumberField stawka = new NumberField("Stawka wypożyczenia");
        stawka.setSuffixComponent(new Span("zł / doba"));

        for (var pole : new com.vaadin.flow.component.HasValueAndElement<?, ?>[] {
                nazwa, kategoria, opis, stawka}) {
            pole.setRequiredIndicatorVisible(true);
        }

        FormLayout form = new FormLayout(nazwa, kategoria, opis, stawka);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button anuluj = new Button("Anuluj", e -> dialog.close());
        Button zapisz = new Button("Dodaj model");
        zapisz.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(anuluj, zapisz);

        zapisz.addClickListener(e -> {
            boolean ok = true;
            ok &= wymagane(nazwa, nazwa.getValue());
            ok &= wymagane(kategoria, kategoria.isEmpty(), "Wybierz kategorię");
            ok &= wymagane(opis, opis.getValue());
            ok &= wymagane(stawka, stawka.isEmpty() || stawka.getValue() <= 0,
                    "Podaj stawkę większą od zera");
            if (!ok) {
                blad("Dane modelu są niepoprawne lub niekompletne.");
                return;
            }
            String nazwaModelu = nazwa.getValue().trim();
            if (modele.stream().anyMatch(m -> m.nazwa.equalsIgnoreCase(nazwaModelu))) {
                bladPola(nazwa, "Model o takiej nazwie już istnieje");
                blad("Model o takiej nazwie już istnieje.");
                return;
            }
            modele.add(new Model(nazwaModelu, kategoria.getValue(), opis.getValue().trim(),
                    stawka.getValue()));
            odswiezWszystko();
            dialog.close();
            sukces("Nowy model został dodany do garderoby.");
        });

        dialog.open();
    }

    // ------------------------------------------------------------------
    // PU07. Dodanie egzemplarza
    // ------------------------------------------------------------------

    
// Dodawanie egzemplarza z zakładki Egzemplarze
private void otworzFormularzEgzemplarza() {

    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Dodanie nowego egzemplarza");
    dialog.setWidth("min(600px, 95vw)");

    // Wybór modelu
    ComboBox<Model> wyborModelu = new ComboBox<>("Model");
    wyborModelu.setItems(
            modele.stream()
                    .filter(m -> !m.wycofany)
                    .toList()
    );
    wyborModelu.setItemLabelGenerator(m -> m.nazwa);
    wyborModelu.setPlaceholder("Wybierz model");
    wyborModelu.setWidthFull();

    TextField id = new TextField("Identyfikator");
    id.setPlaceholder("np. EGZ-201");

    ComboBox<String> rozmiar = new ComboBox<>("Rozmiar");
    rozmiar.setItems(ROZMIARY);

    TextField kolor = new TextField("Kolor");

    ComboBox<String> stan = new ComboBox<>("Stan zachowania");
    stan.setItems(STANY);

    TextField lokalizacja = new TextField("Lokalizacja w magazynie");
    lokalizacja.setPlaceholder("np. Regał A1");

    // Pola wymagane
    for (var pole : new com.vaadin.flow.component.HasValueAndElement<?, ?>[] {
            wyborModelu, id, rozmiar, kolor, stan, lokalizacja
    }) {
        pole.setRequiredIndicatorVisible(true);
    }

    FormLayout form = new FormLayout(
            wyborModelu, id, rozmiar, kolor, stan, lokalizacja
    );

    form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
    );

    form.setColspan(wyborModelu, 2);
    form.setColspan(lokalizacja, 2);

    dialog.add(form);

    Button anuluj = new Button("Anuluj", e -> dialog.close());

    Button zapisz = new Button("Dodaj egzemplarz");
    zapisz.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    dialog.getFooter().add(anuluj, zapisz);

    zapisz.addClickListener(e -> {

        boolean ok = true;

        ok &= wymagane(wyborModelu, wyborModelu.isEmpty(), "Wybierz model");
        ok &= wymagane(id, id.getValue());
        ok &= wymagane(rozmiar, rozmiar.isEmpty(), "Wybierz rozmiar");
        ok &= wymagane(kolor, kolor.getValue());
        ok &= wymagane(stan, stan.isEmpty(), "Wybierz stan zachowania");
        ok &= wymagane(lokalizacja, lokalizacja.getValue());

        if (!ok) {
            blad("Dane egzemplarza są niepoprawne lub niekompletne.");
            return;
        }

        String numer = id.getValue().trim().toUpperCase();

        if (egzemplarze.stream()
                .anyMatch(x -> x.id.equalsIgnoreCase(numer))) {

            bladPola(id, "Egzemplarz o takim numerze już istnieje");
            blad("Numer egzemplarza już istnieje.");
            return;
        }

        Model model = wyborModelu.getValue();

        egzemplarze.add(new Egzemplarz(
                numer,
                model,
                rozmiar.getValue(),
                kolor.getValue().trim(),
                stan.getValue(),
                lokalizacja.getValue().trim(),
                Status.DOSTEPNY
        ));

        odswiezWszystko();

        dialog.close();

        sukces("Egzemplarz " + numer + " został przypisany do modelu.");
    });

    dialog.open();
}


    // ------------------------------------------------------------------
    // PU08. Wycofanie modelu
    // ------------------------------------------------------------------

    private void wycofajModel(Model model) {
        long blokujace = egzemplarze.stream()
                .filter(e -> e.model == model && e.status.blokujeWycofanie()).count();
        if (blokujace > 0) {
            blad("Nie można wycofać modelu – " + blokujace + " egz. jest zarezerwowanych, "
                    + "wypożyczonych lub w konserwacji.");
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Wycofanie modelu");
        dialog.setText("Czy na pewno wycofać model „" + model.nazwa
                + "” z oferty? Wszystkie jego egzemplarze zostaną wycofane.");
        dialog.setCancelable(true);
        dialog.setCancelText("Anuluj");
        dialog.setConfirmText("Wycofaj model");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            model.wycofany = true;
            egzemplarze.stream().filter(x -> x.model == model)
                    .forEach(x -> x.status = Status.WYCOFANY);
            odswiezWszystko();
            sukces("Model został wycofany z oferty.");
        });
        dialog.open();
    }

    // ------------------------------------------------------------------
    // PU09. Wycofanie egzemplarza
    // ------------------------------------------------------------------

    private void wycofajEgzemplarz(Egzemplarz egz) {
        if (egz.status.blokujeWycofanie()) {
            blad("Nie można wycofać egzemplarza " + egz.id + " – ma status „"
                    + egz.status.etykieta() + "”.");
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Wycofanie egzemplarza");
        dialog.setText("Czy na pewno wycofać egzemplarz " + egz.id + " (" + egz.model.nazwa + ")?");
        dialog.setCancelable(true);
        dialog.setCancelText("Anuluj");
        dialog.setConfirmText("Wycofaj egzemplarz");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            egz.status = Status.WYCOFANY;
            odswiezWszystko();
            sukces("Egzemplarz został wycofany z użytku.");
        });
        dialog.open();
    }
}