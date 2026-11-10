package com.example; // <- jeśli Twoje widoki mają inny pakiet, zmień tę linię

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * PU05. Utworzenie rezerwacji (aktor: Klient)
 * z wykorzystaniem PU11. Sprawdzenie dostępności egzemplarza w wybranym terminie.
 *
 * Makieta - dane wpisane na sztywno, bez bazy danych.
 * UWAGA (symulacja): jeśli dzień miesiąca daty odbioru dzieli się przez 5
 * (5., 10., 15., 20., 25., 30.), makieta udaje, że brak wolnych egzemplarzy
 * - to pokazuje przebieg alternatywny z PU11.
 */
@Route(value = "rezerwacja", autoLayout = false)   // bez menu bocznego
@PageTitle("Rezerwacja")
public class RezerwacjaView extends VerticalLayout {

    record ModelInfo(String nazwa, double stawka, List<String> rozmiary) {
    }

    record Egzemplarz(String id, String model, String rozmiar, String kolor, double stawka) {
    }

    private final List<ModelInfo> modele = List.of(
            new ModelInfo("Suknia balowa „Księżniczka”", 120.0, List.of("S", "M", "L")),
            new ModelInfo("Garnitur wieczorowy", 90.0, List.of("M", "L", "XL")),
            new ModelInfo("Kostium Pirata", 70.0, List.of("S", "M", "L", "XL")),
            new ModelInfo("Strój Mikołaja", 60.0, List.of("L", "XL")),
            new ModelInfo("Kostium Elfa", 50.0, List.of("S", "M")),
            new ModelInfo("Strój krakowski", 80.0, List.of("S", "M", "L")),
            new ModelInfo("Kostium Czarownicy", 65.0, List.of("S", "M", "L")),
            new ModelInfo("Suknia wieczorowa", 110.0, List.of("XS", "S", "M")));

    private static final List<String> KOLORY = List.of("Czarny", "Czerwony", "Granatowy", "Zielony");

    // krok 2 - formularz wyboru
    private final ComboBox<ModelInfo> model = new ComboBox<>("Model");
    private final ComboBox<String> rozmiar = new ComboBox<>("Rozmiar");
    private final DatePicker odbior = new DatePicker("Planowana data odbioru");
    private final DatePicker zwrot = new DatePicker("Planowana data zwrotu");
    private final Button sprawdz = new Button("Sprawdź dostępność");

    // krok 3 - wynik sprawdzenia dostępności (PU11)
    private final Div sekcjaDostepnosci = new Div();
    private final Paragraph komunikatDostepnosci = new Paragraph();
    private final Grid<Egzemplarz> dostepneGrid = new Grid<>(Egzemplarz.class, false);
    private final List<Egzemplarz> dostepne = new ArrayList<>();

    // kroki 4-5 - wybrane egzemplarze i podsumowanie
    private final List<Egzemplarz> wybrane = new ArrayList<>();
    private final Grid<Egzemplarz> wybraneGrid = new Grid<>(Egzemplarz.class, false);
    private final Paragraph pusteWybrane = new Paragraph("Nie dodano jeszcze żadnych egzemplarzy.");
    private final Paragraph podsumowanie = new Paragraph();
    private final Button zatwierdz = new Button("Zatwierdź rezerwację");

    public RezerwacjaView() {
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);

        VerticalLayout tresc = new VerticalLayout();
        tresc.setPadding(false);
        tresc.setWidthFull();
        tresc.setMaxWidth("900px");

        konfigurujFormularz();
        konfigurujDostepnosc();
        konfigurujWybrane();

        FormLayout form = new FormLayout(model, rozmiar, odbior, zwrot);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));
        form.setColspan(model, 2);

        tresc.add(
                new H2("Nowa rezerwacja"),
                new Paragraph("Wybierz model, rozmiar i termin, sprawdź dostępność, "
                        + "a następnie dodaj egzemplarze do rezerwacji."),
                form,
                sprawdz,
                sekcjaDostepnosci,
                new H4("Wybrane egzemplarze"),
                pusteWybrane,
                wybraneGrid,
                new H4("Podsumowanie rezerwacji"),
                podsumowanie,
                zatwierdz);

        add(tresc);
        odswiezWybrane();
    }

    // ------------------------------------------------------------------
    // Konfiguracja komponentów
    // ------------------------------------------------------------------

    private void konfigurujFormularz() {
        model.setItems(modele);
        model.setItemLabelGenerator(ModelInfo::nazwa);
        model.setRequiredIndicatorVisible(true);
        model.addValueChangeListener(e -> {
            rozmiar.clear();
            rozmiar.setItems(e.getValue() == null ? List.<String>of() : e.getValue().rozmiary());
            ukryjDostepnosc();
        });

        rozmiar.setRequiredIndicatorVisible(true);
        rozmiar.addValueChangeListener(e -> ukryjDostepnosc());

        for (DatePicker d : new DatePicker[] {odbior, zwrot}) {
            d.setLocale(Locale.forLanguageTag("pl-PL"));
            d.setMin(LocalDate.now());
            d.setRequiredIndicatorVisible(true);
            d.addValueChangeListener(e -> terminZmieniony());
        }

        sprawdz.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sprawdz.addClickListener(e -> sprawdzDostepnosc());
    }

    private void konfigurujDostepnosc() {
        dostepneGrid.addColumn(Egzemplarz::id).setHeader("Numer egzemplarza");
        dostepneGrid.addColumn(Egzemplarz::model).setHeader("Model");
        dostepneGrid.addColumn(Egzemplarz::rozmiar).setHeader("Rozmiar");
        dostepneGrid.addColumn(Egzemplarz::kolor).setHeader("Kolor");
        dostepneGrid.addComponentColumn(eg -> {
            Button dodaj = new Button("Dodaj do rezerwacji", ev -> dodaj(eg));
            dodaj.addThemeVariants(ButtonVariant.LUMO_SMALL);
            return dodaj;
        });
        dostepneGrid.setAllRowsVisible(true);

        sekcjaDostepnosci.add(new H4("Dostępne egzemplarze"), komunikatDostepnosci, dostepneGrid);
        sekcjaDostepnosci.setWidthFull();
        sekcjaDostepnosci.setVisible(false);
    }

    private void konfigurujWybrane() {
        wybraneGrid.addColumn(Egzemplarz::id).setHeader("Numer egzemplarza");
        wybraneGrid.addColumn(Egzemplarz::model).setHeader("Model");
        wybraneGrid.addColumn(Egzemplarz::rozmiar).setHeader("Rozmiar");
        wybraneGrid.addColumn(Egzemplarz::kolor).setHeader("Kolor");
        wybraneGrid.addColumn(eg -> String.format("%.0f zł / doba", eg.stawka())).setHeader("Stawka");
        wybraneGrid.addComponentColumn(eg -> {
            Button usun = new Button("Usuń", ev -> {
                wybrane.remove(eg);
                odswiezWybrane();
            });
            usun.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            return usun;
        });
        wybraneGrid.setAllRowsVisible(true);

        zatwierdz.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        zatwierdz.addClickListener(e -> zatwierdzRezerwacje());
    }

    // ------------------------------------------------------------------
    // PU11. Sprawdzenie dostępności egzemplarza w wybranym terminie
    // ------------------------------------------------------------------

    private void sprawdzDostepnosc() {
        if (!walidujFormularz()) {
            return;
        }

        dostepne.clear();
        dostepne.addAll(wygenerujEgzemplarze(model.getValue(), rozmiar.getValue()));
        // nie pokazuj egzemplarzy, które klient już dodał
        dostepne.removeIf(wybrane::contains);

        sekcjaDostepnosci.setVisible(true);
        odswiezDostepne();
    }

    private void odswiezDostepne() {
        dostepneGrid.setItems(new ArrayList<>(dostepne));
        boolean sa = !dostepne.isEmpty();
        dostepneGrid.setVisible(sa);
        komunikatDostepnosci.setText(sa
                ? "Egzemplarze dostępne w wybranym terminie (" + odbior.getValue() + " – " + zwrot.getValue() + "):"
                : "Brak dostępnych egzemplarzy w wybranym terminie. "
                        + "Wybierz inny model, rozmiar lub termin.");
    }

    /** Symulacja: 1-3 egzemplarze; dla dat odbioru z dniem podzielnym przez 5 - brak wolnych. */
    private List<Egzemplarz> wygenerujEgzemplarze(ModelInfo m, String rozm) {
        List<Egzemplarz> wynik = new ArrayList<>();
        if (odbior.getValue().getDayOfMonth() % 5 == 0) {
            return wynik;
        }
        int baza = Math.abs((m.nazwa() + rozm).hashCode()) % 900 + 100;
        int ile = 1 + (m.nazwa().length() % 3);
        for (int i = 0; i < ile; i++) {
            wynik.add(new Egzemplarz("EGZ-" + (baza + i), m.nazwa(), rozm,
                    KOLORY.get((baza + i) % KOLORY.size()), m.stawka()));
        }
        return wynik;
    }

    // ------------------------------------------------------------------
    // Krok 4 - dodawanie egzemplarzy, krok 5 - podsumowanie
    // ------------------------------------------------------------------

    private void dodaj(Egzemplarz eg) {
        wybrane.add(eg);
        dostepne.remove(eg);
        odswiezDostepne();
        odswiezWybrane();
        Notification.show("Dodano egzemplarz " + eg.id() + " do rezerwacji.");
    }

    private void odswiezWybrane() {
        wybraneGrid.setItems(new ArrayList<>(wybrane));
        wybraneGrid.setVisible(!wybrane.isEmpty());
        pusteWybrane.setVisible(wybrane.isEmpty());
        zatwierdz.setEnabled(!wybrane.isEmpty());

        // dane klienta na sztywno - w makiecie "zalogowany klient"
        StringBuilder sb = new StringBuilder("Klient: Jan Kowalski (jan.kowalski@example.com)\n");
        if (odbior.getValue() != null && zwrot.getValue() != null) {
            long doby = Math.max(1, ChronoUnit.DAYS.between(odbior.getValue(), zwrot.getValue()));
            // double suma = wybrane.stream().mapToDouble(Egzemplarz::stawka).sum() * doby;     TO JEZELI DODAJEMY SZACOWANY KOSZT
            sb.append("Termin: ").append(odbior.getValue()).append(" – ").append(zwrot.getValue())
              .append(" (").append(doby).append(" dób)\n");
            sb.append("Liczba egzemplarzy: ").append(wybrane.size()).append("\n");              
            // sb.append(String.format("Szacowany koszt: %.0f zł", suma));                      TU TEZ
        } else {
            sb.append("Termin: nie wybrano\nLiczba egzemplarzy: ").append(wybrane.size());
        }
        podsumowanie.setText(sb.toString());
        podsumowanie.getStyle().set("white-space", "pre-line");
    }

    // krok 6 - zatwierdzenie
    private void zatwierdzRezerwacje() {
        if (wybrane.isEmpty()) {
            pokazBlad("Dodaj co najmniej jeden egzemplarz do rezerwacji.");
            return;
        }
        Notification n = Notification.show(
                "Rezerwacja została utworzona. Wybrane egzemplarze mają status „zarezerwowany”.",
                4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        // widok "Moje rezerwacje" powstanie później - na razie wracamy do katalogu
        UI.getCurrent().navigate("katalog");
    }

    // ------------------------------------------------------------------
    // Pomocnicze
    // ------------------------------------------------------------------

    private boolean walidujFormularz() {
        boolean ok = true;

        ok &= wymagane(model.isEmpty(), model, "Wybierz model");
        ok &= wymagane(rozmiar.isEmpty(), rozmiar, "Wybierz rozmiar");
        ok &= wymagane(odbior.isEmpty(), odbior, "Podaj datę odbioru");
        ok &= wymagane(zwrot.isEmpty(), zwrot, "Podaj datę zwrotu");

        if (!odbior.isEmpty() && !zwrot.isEmpty() && zwrot.getValue().isBefore(odbior.getValue())) {
            zwrot.setErrorMessage("Data zwrotu nie może być wcześniejsza niż data odbioru");
            zwrot.setInvalid(true);
            ok = false;
        }

        if (!ok) {
            pokazBlad("Uzupełnij poprawnie wszystkie pola.");
        }
        return ok;
    }

    private boolean wymagane(boolean brak, com.vaadin.flow.component.HasValidation pole, String komunikat) {
        pole.setInvalid(brak);
        if (brak) {
            pole.setErrorMessage(komunikat);
        }
        return !brak;
    }

    /** Zmiana terminu unieważnia wcześniej wybrane egzemplarze (dostępność zależy od terminu). */
    private void terminZmieniony() {
        odbior.setInvalid(false);
        zwrot.setInvalid(false);
        ukryjDostepnosc();
        if (!wybrane.isEmpty()) {
            wybrane.clear();
            Notification.show("Zmiana terminu wyczyściła listę wybranych egzemplarzy.");
        }
        odswiezWybrane();
    }

    private void ukryjDostepnosc() {
        dostepne.clear();
        sekcjaDostepnosci.setVisible(false);
    }

    private void pokazBlad(String tekst) {
        Notification n = Notification.show(tekst, 4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}