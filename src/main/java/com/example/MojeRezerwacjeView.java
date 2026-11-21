package com.example;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * PU18. Zarządzanie rezerwacjami (aktor: Klient)
 *  - lista rezerwacji klienta,
 *  - PU06. Modyfikowanie rezerwacji (okno dialogowe),
 *  - PU07. Anulowanie rezerwacji (okno potwierdzenia),
 *  - PU05. Utworzenie rezerwacji (przycisk "Nowa rezerwacja" -> widok "rezerwacja").
 *
 * Makieta - dane na sztywno, bez bazy danych.
 * Symulacja PU11: egzemplarze są "niedostępne", gdy dzień daty odbioru dzieli się przez 5.
 */
@Route(value = "moje-rezerwacje", autoLayout = false)   // bez menu bocznego
@PageTitle("Moje rezerwacje")
public class MojeRezerwacjeView extends VerticalLayout {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Locale PL = Locale.forLanguageTag("pl-PL");

    enum Status {
        ZAREZERWOWANA("Zarezerwowana", "success"),
        WYPOZYCZONA("Wypożyczona", ""),
        ZREALIZOWANA("Zrealizowana", "contrast"),
        ANULOWANA("Anulowana", "error");

        final String etykieta;
        final String motyw;

        Status(String etykieta, String motyw) {
            this.etykieta = etykieta;
            this.motyw = motyw;
        }
    }

    record Pozycja(String id, String nazwa, String rozmiar, double stawka) {
        String opis() {
            return id + " · " + nazwa + " (" + rozmiar + ")";
        }
    }

    static class Rezerwacja {
        final String numer;
        LocalDate odbior;
        LocalDate zwrot;
        List<Pozycja> pozycje;
        Status status;

        Rezerwacja(String numer, LocalDate odbior, LocalDate zwrot, List<Pozycja> pozycje, Status status) {
            this.numer = numer;
            this.odbior = odbior;
            this.zwrot = zwrot;
            this.pozycje = new ArrayList<>(pozycje);
            this.status = status;
        }

        long doby() {
            return Math.max(1, ChronoUnit.DAYS.between(odbior, zwrot));
        }

        double koszt() {
            return pozycje.stream().mapToDouble(Pozycja::stawka).sum() * doby();
        }

        /** Modyfikować i anulować można tylko rezerwację w statusie "zarezerwowana". */
        boolean edytowalna() {
            return status == Status.ZAREZERWOWANA;
        }
    }

    /** Egzemplarze, które klient może dodać przy modyfikacji rezerwacji. */
    private static final List<Pozycja> DO_DODANIA = List.of(
            new Pozycja("EGZ-401", "Maska wenecka", "Uniwersalny", 30),
            new Pozycja("EGZ-402", "Peruka barokowa", "Uniwersalny", 40),
            new Pozycja("EGZ-403", "Peleryna wampira", "L", 35),
            new Pozycja("EGZ-404", "Kostium Czarownicy", "M", 65));

    private final LocalDate dzis = LocalDate.now();

    private final List<Rezerwacja> rezerwacje = new ArrayList<>(List.of(
            new Rezerwacja("REZ-003", dzis.plusDays(7), dzis.plusDays(9),
                    List.of(new Pozycja("EGZ-214", "Kostium Pirata", "M", 70),
                            new Pozycja("EGZ-318", "Kapelusz cylinder", "Uniwersalny", 25)),
                    Status.ZAREZERWOWANA),
            new Rezerwacja("REZ-002", dzis.plusDays(21), dzis.plusDays(23),
                    List.of(new Pozycja("EGZ-105", "Suknia balowa „Księżniczka”", "M", 120)),
                    Status.ZAREZERWOWANA),
            new Rezerwacja("REZ-001", dzis.minusDays(30), dzis.minusDays(28),
                    List.of(new Pozycja("EGZ-220", "Garnitur wieczorowy", "L", 90)),
                    Status.ZREALIZOWANA),
            new Rezerwacja("REZ-000", dzis.minusDays(60), dzis.minusDays(58),
                    List.of(new Pozycja("EGZ-150", "Strój Mikołaja", "XL", 60)),
                    Status.ANULOWANA)));

    private final Grid<Rezerwacja> grid = new Grid<>(Rezerwacja.class, false);
    private final Paragraph brakRezerwacji = new Paragraph(
            "Nie masz jeszcze żadnych rezerwacji. Kliknij „Nowa rezerwacja”, aby utworzyć pierwszą.");

    public MojeRezerwacjeView() {
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);

        Button wroc = new Button("Strona główna", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate(""));
        wroc.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button nowa = new Button("Nowa rezerwacja", VaadinIcon.PLUS.create(),
                e -> UI.getCurrent().navigate("rezerwacja"));
        nowa.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout pasek = new HorizontalLayout(wroc, nowa);
        pasek.setWidthFull();
        pasek.setJustifyContentMode(JustifyContentMode.BETWEEN);

        konfigurujTabele();

        Div tresc = new Div(
                pasek,
                new H2("Moje rezerwacje"),
                new Paragraph("Tu możesz zobaczyć swoje rezerwacje, zmienić ich termin lub skład "
                        + "albo je anulować."),
                brakRezerwacji,
                grid);
        tresc.setWidthFull();
        tresc.setMaxWidth("1100px");

        add(tresc);
        odswiez();
    }

    // ------------------------------------------------------------------
    // PU08 - lista rezerwacji
    // ------------------------------------------------------------------

    private void konfigurujTabele() {
        grid.addColumn(r -> r.numer).setHeader("Nr rezerwacji").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> FORMAT.format(r.odbior) + " – " + FORMAT.format(r.zwrot))
                .setHeader("Termin").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> r.pozycje.stream().map(Pozycja::opis).collect(Collectors.joining(", ")))
                .setHeader("Egzemplarze").setFlexGrow(3);
        grid.addColumn(r -> String.format("%.0f zł", r.koszt()))
                .setHeader("Szacowany koszt").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::znacznikStatusu).setHeader("Status").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::przyciskiAkcji).setHeader("Akcje").setAutoWidth(true).setFlexGrow(0);
        grid.setAllRowsVisible(true);
    }

    private Span znacznikStatusu(Rezerwacja r) {
        Span s = new Span(r.status.etykieta);
        s.getElement().getThemeList().add("badge");
        if (!r.status.motyw.isEmpty()) {
            s.getElement().getThemeList().add(r.status.motyw);
        }
        return s;
    }

    private HorizontalLayout przyciskiAkcji(Rezerwacja r) {
        Button modyfikuj = new Button("Modyfikuj", VaadinIcon.EDIT.create(), e -> otworzModyfikacje(r));
        modyfikuj.addThemeVariants(ButtonVariant.LUMO_SMALL);
        modyfikuj.setEnabled(r.edytowalna());

        Button anuluj = new Button("Anuluj", VaadinIcon.CLOSE_SMALL.create(), e -> potwierdzAnulowanie(r));
        anuluj.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        anuluj.setEnabled(r.edytowalna());

        return new HorizontalLayout(modyfikuj, anuluj);
    }

    private void odswiez() {
        grid.setItems(new ArrayList<>(rezerwacje));
        grid.setVisible(!rezerwacje.isEmpty());
        brakRezerwacji.setVisible(rezerwacje.isEmpty());
    }

    // ------------------------------------------------------------------
    // PU07. Anulowanie rezerwacji
    // ------------------------------------------------------------------

    private void potwierdzAnulowanie(Rezerwacja r) {
        ConfirmDialog d = new ConfirmDialog();
        d.setHeader("Anulować rezerwację " + r.numer + "?");
        d.setText("Zarezerwowane egzemplarze zostaną zwolnione i będą ponownie dostępne "
                + "w tym terminie. Tej operacji nie można cofnąć.");
        d.setCancelable(true);
        d.setCancelText("Wróć");
        d.setConfirmText("Anuluj rezerwację");
        d.setConfirmButtonTheme("error primary");
        d.addConfirmListener(e -> {
            if (!r.edytowalna()) {
                pokazBlad("Tej rezerwacji nie można już anulować.");
                return;
            }
            r.status = Status.ANULOWANA;
            odswiez();
            Notification n = Notification.show("Rezerwacja " + r.numer + " została anulowana.");
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        d.open();
    }

    // ------------------------------------------------------------------
    // PU06. Modyfikowanie rezerwacji (+ PU11 - ponowne sprawdzenie dostępności)
    // ------------------------------------------------------------------

    private void otworzModyfikacje(Rezerwacja r) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Modyfikowanie rezerwacji " + r.numer);
        dialog.setWidth("720px");

        // krok 3-4: dane rezerwacji i zmiana terminu
        DatePicker odbior = new DatePicker("Data odbioru", r.odbior);
        DatePicker zwrot = new DatePicker("Data zwrotu", r.zwrot);
        for (DatePicker d : new DatePicker[] {odbior, zwrot}) {
            d.setLocale(PL);
            d.setMin(dzis);
            d.setRequiredIndicatorVisible(true);
        }
        FormLayout terminy = new FormLayout(odbior, zwrot);
        terminy.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));

        Paragraph komunikat = new Paragraph();
        komunikat.setVisible(false);

        // krok 4: usunięcie / dodanie egzemplarzy
        List<Pozycja> robocze = new ArrayList<>(r.pozycje);
        Grid<Pozycja> lista = new Grid<>(Pozycja.class, false);
        lista.addColumn(Pozycja::opis).setHeader("Egzemplarz").setFlexGrow(1);
        lista.addColumn(p -> String.format("%.0f zł / doba", p.stawka())).setHeader("Stawka").setAutoWidth(true);
        lista.addComponentColumn(p -> {
            Button usun = new Button("Usuń", ev -> {
                robocze.remove(p);
                lista.setItems(new ArrayList<>(robocze));
                komunikat.setVisible(false);
            });
            usun.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            return usun;
        }).setAutoWidth(true).setFlexGrow(0);
        lista.setItems(new ArrayList<>(robocze));
        lista.setAllRowsVisible(true);

        ComboBox<Pozycja> dodawany = new ComboBox<>("Dodaj egzemplarz");
        dodawany.setItems(DO_DODANIA);
        dodawany.setItemLabelGenerator(Pozycja::opis);
        dodawany.setWidth("350px");
        Button dodaj = new Button("Dodaj", VaadinIcon.PLUS.create(), ev -> {
            Pozycja p = dodawany.getValue();
            if (p == null) {
                return;
            }
            if (robocze.contains(p)) {
                Notification.show("Ten egzemplarz jest już w rezerwacji.");
                return;
            }
            robocze.add(p);
            lista.setItems(new ArrayList<>(robocze));
            dodawany.clear();
            komunikat.setVisible(false);
        });
        HorizontalLayout dodawanie = new HorizontalLayout(dodawany, dodaj);
        dodawanie.setAlignItems(Alignment.END);

        VerticalLayout zawartosc = new VerticalLayout(
                terminy, komunikat, new H4("Egzemplarze w rezerwacji"), lista, dodawanie);
        zawartosc.setPadding(false);
        dialog.add(zawartosc);

        // krok 5: sprawdzenie dostępności (PU11)
        Button sprawdz = new Button("Sprawdź dostępność", ev -> {
            if (!terminPoprawny(odbior, zwrot)) {
                return;
            }
            komunikat.setVisible(true);
            komunikat.setText(dostepny(odbior.getValue())
                    ? "Wszystkie egzemplarze są dostępne w wybranym terminie."
                    : "Wybrane egzemplarze są niedostępne w nowym terminie. Wybierz inny termin.");
        });

        // krok 6: zatwierdzenie zmian
        Button zapisz = new Button("Zapisz zmiany", ev -> {
            if (!terminPoprawny(odbior, zwrot)) {
                return;
            }
            if (robocze.isEmpty()) {
                pokazBlad("Rezerwacja musi zawierać co najmniej jeden egzemplarz.");
                return;
            }
            boolean terminZmieniony = !odbior.getValue().equals(r.odbior) || !zwrot.getValue().equals(r.zwrot);
            if (terminZmieniony && !dostepny(odbior.getValue())) {
                pokazBlad("Wybrane egzemplarze są niedostępne w nowym terminie.");
                return;
            }
            r.odbior = odbior.getValue();
            r.zwrot = zwrot.getValue();
            r.pozycje = new ArrayList<>(robocze);
            dialog.close();
            odswiez();
            Notification n = Notification.show("Rezerwacja " + r.numer + " została zmodyfikowana.");
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        zapisz.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button zamknij = new Button("Zamknij", ev -> dialog.close());
        dialog.getFooter().add(zamknij, sprawdz, zapisz);

        dialog.open();
    }

    // ------------------------------------------------------------------
    // Pomocnicze
    // ------------------------------------------------------------------

    /** Symulacja PU11: brak dostępności, gdy dzień daty odbioru dzieli się przez 5. */
    private boolean dostepny(LocalDate odbior) {
        return odbior.getDayOfMonth() % 5 != 0;
    }

    private boolean terminPoprawny(DatePicker odbior, DatePicker zwrot) {
        boolean ok = true;

        if (odbior.isEmpty()) {
            odbior.setErrorMessage("Podaj datę odbioru");
            odbior.setInvalid(true);
            ok = false;
        } else {
            odbior.setInvalid(false);
        }
        if (zwrot.isEmpty()) {
            zwrot.setErrorMessage("Podaj datę zwrotu");
            zwrot.setInvalid(true);
            ok = false;
        } else {
            zwrot.setInvalid(false);
        }
        if (ok && zwrot.getValue().isBefore(odbior.getValue())) {
            zwrot.setErrorMessage("Data zwrotu nie może być wcześniejsza niż data odbioru");
            zwrot.setInvalid(true);
            ok = false;
        }

        if (!ok) {
            pokazBlad("Uzupełnij poprawnie daty rezerwacji.");
        }
        return ok;
    }

    private void pokazBlad(String tekst) {
        Notification n = Notification.show(tekst, 4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}