package com.example;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
- PU13. Zarządzanie wypożyczeniami (aktor: Pracownik obsługi, Kierownik)
-  - PU14. Wydanie egzemplarzy (na podstawie rezerwacji albo bez rezerwacji),
-  - PU15. Przyjęcie zwrotu (data zwrotu, kompletność, stan egzemplarza, uwagi),
-  - PU16. Zlecenie czyszczenia lub naprawy (z poziomu przyjęcia zwrotu),
-  - PU17. Naliczenie opłaty (tylko opóźnienie).
 *
- Makieta - dane na sztywno, bez bazy danych.
- Założenia makiety: stawka za opóźnienie 20 zł/dzień, limit 5 egzemplarzy
- w jednym wypożyczeniu, zalogowany pracownik wpisany na sztywno.
- Demonstracja przebiegów alternatywnych:
-  - rezerwacja REZ-007 zawiera niedostępny egzemplarz EGZ-777 (nie da się go wydać),
-  - wypożyczenie WYP-003 jest po terminie zwrotu (naliczy się opłata za opóźnienie).
 */
@Route(value = "wypozyczenia", autoLayout = false)   // bez menu bocznego
@PageTitle("Wypożyczenia")
public class WypozyczeniaView extends VerticalLayout {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Locale PL = Locale.forLanguageTag("pl-PL");

    private static final double STAWKA_OPOZNIENIA = 20.0;
    private static final int LIMIT_EGZEMPLARZY = 5;
    private static final String PRACOWNIK = "Jack Sparrow (Pracownik obsługi)";

    private static final String STAN_DOBRY = "Dobry – gotowy do wypożyczenia";
    private static final String STAN_CZYSZCZENIE = "Wymaga czyszczenia";
    private static final String STAN_NAPRAWA = "Wymaga naprawy";

    private static final String ZLECENIE_CZYSZCZENIE = "Czyszczenie";
    private static final String ZLECENIE_NAPRAWA = "Naprawa";

    private static final List<String> RODZAJE_CZYSZCZENIA = List.of(
            "Pranie chemiczne", "Pranie wodne", "Prasowanie i parowanie",
            "Usuwanie plam", "Odświeżanie i dezynfekcja");
    private static final List<String> RODZAJE_NAPRAWY = List.of(
            "Naprawa szwu", "Przyszycie guzików i zapięć", "Naprawa podszewki",
            "Uzupełnienie brakującego elementu", "Inna naprawa krawiecka");

    enum Status {
        DO_WYDANIA("Do wydania", ""),
        WYPOZYCZONE("Wypożyczone", "success"),
        ZWROCONE("Zwrócone", "contrast");

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

    /** Rezerwacja oczekująca na wydanie, aktualne wypożyczenie albo wypożyczenie zakończone. */
    static class Wypozyczenie {
        String numer;
        final String klient;
        LocalDate odbior;            // planowany odbiór (rezerwacja) lub data wydania (wypożyczenie)
        LocalDate planowanyZwrot;
        LocalDate dataZwrotu;
        List<Pozycja> pozycje;
        Status status;
        String rezerwacja;           // numer rezerwacji lub null
        String pracownik;
        double oplataDodatkowa;

        Wypozyczenie(String numer, String klient, LocalDate odbior, LocalDate planowanyZwrot,
                     List<Pozycja> pozycje, Status status, String rezerwacja, String pracownik) {
            this.numer = numer;
            this.klient = klient;
            this.odbior = odbior;
            this.planowanyZwrot = planowanyZwrot;
            this.pozycje = new ArrayList<>(pozycje);
            this.status = status;
            this.rezerwacja = rezerwacja;
            this.pracownik = pracownik;
        }

        long doby() {
            return Math.max(1, ChronoUnit.DAYS.between(odbior, planowanyZwrot));
        }

        /** Opłata za wypożyczenie: suma stawek dziennych razy liczba dób. */
        double oplata() {
            return pozycje.stream().mapToDouble(Pozycja::stawka).sum() * doby();
        }

        double razem() {
            return oplata() + oplataDodatkowa;
        }
    }

    /** Stan jednego zwracanego egzemplarza wprowadzany w oknie przyjęcia zwrotu. */
    static class ZwrotPozycji {
        final Pozycja pozycja;
        boolean kompletny = true;
        String stan = STAN_DOBRY;
        String uwagi = "";
        String zlecenie;             // opis zlecenia czyszczenia/naprawy (PU16) lub null

        ZwrotPozycji(Pozycja pozycja) {
            this.pozycja = pozycja;
        }
    }

    private static final List<String> KLIENCI = List.of(
            "Chuck Norris", "Anna Nowak", "Maria Wiśniewska", "Piotr Zieliński",
            "Teatr Miejski (Jan Mazur)");

    /** Egzemplarze niedostępne i powód - do pokazania przebiegu alternatywnego PU14. */
    private static final Map<String, String> NIEDOSTEPNE = Map.of("EGZ-777", "w czyszczeniu");

    /** Wolne egzemplarze do wypożyczenia bez rezerwacji. */
    private static final List<Pozycja> WOLNE = List.of(
            new Pozycja("EGZ-701", "Kostium Pirata", "L", 70),
            new Pozycja("EGZ-702", "Garnitur wieczorowy", "M", 90),
            new Pozycja("EGZ-703", "Suknia wieczorowa", "S", 90),
            new Pozycja("EGZ-704", "Strój Mikołaja", "XL", 60),
            new Pozycja("EGZ-705", "Maska wenecka", "Uniwersalny", 30),
            new Pozycja("EGZ-706", "Peruka barokowa", "Uniwersalny", 40));

    private final LocalDate dzis = LocalDate.now();
    private int licznik = 5;

    private final List<Wypozyczenie> lista = new ArrayList<>(List.of(
            new Wypozyczenie("REZ-006", "Chuck Norris", dzis, dzis.plusDays(2),
                    List.of(new Pozycja("EGZ-501", "Kostium Elfa", "M", 50)),
                    Status.DO_WYDANIA, "REZ-006", null),
            new Wypozyczenie("REZ-005", "Anna Nowak", dzis.plusDays(3), dzis.plusDays(5),
                    List.of(new Pozycja("EGZ-214", "Kostium Pirata", "M", 70),
                            new Pozycja("EGZ-318", "Kapelusz cylinder", "Uniwersalny", 25)),
                    Status.DO_WYDANIA, "REZ-005", null),
            new Wypozyczenie("REZ-007", "Teatr Miejski (Jan Mazur)", dzis.plusDays(1), dzis.plusDays(4),
                    List.of(new Pozycja("EGZ-610", "Strój krakowski", "S", 70),
                            new Pozycja("EGZ-777", "Płaszcz teatralny", "Uniwersalny", 40)),
                    Status.DO_WYDANIA, "REZ-007", null),
            new Wypozyczenie("WYP-004", "Piotr Zieliński", dzis.minusDays(1), dzis.plusDays(2),
                    List.of(new Pozycja("EGZ-220", "Garnitur wieczorowy", "L", 90),
                            new Pozycja("EGZ-319", "Kapelusz cylinder", "Uniwersalny", 25)),
                    Status.WYPOZYCZONE, null, PRACOWNIK),
            new Wypozyczenie("WYP-003", "Maria Wiśniewska", dzis.minusDays(4), dzis.minusDays(2),
                    List.of(new Pozycja("EGZ-105", "Suknia balowa „Księżniczka”", "M", 120)),
                    Status.WYPOZYCZONE, "REZ-003", PRACOWNIK),
            new Wypozyczenie("WYP-002", "Anna Nowak", dzis.minusDays(30), dzis.minusDays(28),
                    List.of(new Pozycja("EGZ-150", "Strój Mikołaja", "XL", 60)),
                    Status.ZWROCONE, null, PRACOWNIK)));

    private final Grid<Wypozyczenie> gridDoWydania = new Grid<>(Wypozyczenie.class, false);
    private final Grid<Wypozyczenie> gridAktualne = new Grid<>(Wypozyczenie.class, false);
    private final Grid<Wypozyczenie> gridZakonczone = new Grid<>(Wypozyczenie.class, false);
    private final Paragraph brakDoWydania = new Paragraph("Brak rezerwacji oczekujących na wydanie.");
    private final Paragraph brakAktualnych = new Paragraph("Brak aktualnych wypożyczeń.");
    private final Paragraph brakZakonczonych = new Paragraph("Brak zakończonych wypożyczeń.");
    private final TabSheet zakladki = new TabSheet();

    public WypozyczeniaView() {
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);

        Button wroc = new Button("Strona główna", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate(""));
        wroc.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button nowe = new Button("Nowe wypożyczenie", VaadinIcon.PLUS.create(), e -> otworzWydanie(null));
        nowe.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout pasek = new HorizontalLayout(wroc, nowe);
        pasek.setWidthFull();
        pasek.setJustifyContentMode(JustifyContentMode.BETWEEN);

        konfigurujTabele();

        zakladki.setWidthFull();
        zakladki.add("Do wydania", panel(brakDoWydania, gridDoWydania));
        zakladki.add("Aktualne wypożyczenia", panel(brakAktualnych, gridAktualne));
        zakladki.add("Zakończone", panel(brakZakonczonych, gridZakonczone));

        Div tresc = new Div(
                pasek,
                new H2("Wypożyczenia"),
                new Paragraph("Wydawaj egzemplarze "
                        + "i przyjmuj zwroty wraz z kontrolą stanu i naliczeniem opłat."),
                zakladki);
        tresc.setWidthFull();
        tresc.setMaxWidth("1200px");

        add(tresc);
        odswiez();
    }

    // ------------------------------------------------------------------
    // PU13 - listy
    // ------------------------------------------------------------------

    private Div panel(Paragraph pusty, Grid<Wypozyczenie> g) {
        Div d = new Div(pusty, g);
        d.setWidthFull();
        return d;
    }

    private void konfigurujTabele() {
        // rezerwacje do wydania
        gridDoWydania.addColumn(w -> w.numer).setHeader("Rezerwacja").setAutoWidth(true).setFlexGrow(0);
        gridDoWydania.addColumn(w -> w.klient).setHeader("Klient").setFlexGrow(1);
        gridDoWydania.addColumn(w -> FORMAT.format(w.odbior) + " – " + FORMAT.format(w.planowanyZwrot))
                .setHeader("Planowany termin").setAutoWidth(true).setFlexGrow(0);
        gridDoWydania.addColumn(w -> egzemplarze(w)).setHeader("Egzemplarze").setFlexGrow(3);
        gridDoWydania.addColumn(w -> String.format("%.0f zł", w.oplata()))
                .setHeader("Opłata").setAutoWidth(true).setFlexGrow(0);
        gridDoWydania.addComponentColumn(w -> {
            Button wydaj = new Button("Wydaj", VaadinIcon.ARROW_RIGHT.create(), e -> otworzWydanie(w));
            wydaj.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            return wydaj;
        }).setHeader("Akcje").setAutoWidth(true).setFlexGrow(0);
        gridDoWydania.setAllRowsVisible(true);

        // aktualne wypożyczenia
        gridAktualne.addColumn(w -> w.numer).setHeader("Wypożyczenie").setAutoWidth(true).setFlexGrow(0);
        gridAktualne.addColumn(w -> w.rezerwacja == null ? "–" : w.rezerwacja)
                .setHeader("Z rezerwacji").setAutoWidth(true).setFlexGrow(0);
        gridAktualne.addColumn(w -> w.klient).setHeader("Klient").setFlexGrow(1);
        gridAktualne.addColumn(w -> FORMAT.format(w.odbior)).setHeader("Data wydania")
                .setAutoWidth(true).setFlexGrow(0);
        gridAktualne.addComponentColumn(w -> {
            HorizontalLayout h = new HorizontalLayout(new Span(FORMAT.format(w.planowanyZwrot)));
            h.setAlignItems(Alignment.CENTER);
            if (w.planowanyZwrot.isBefore(dzis)) {
                Span opoznione = new Span("Opóźnione");
                opoznione.getElement().getThemeList().add("badge");
                opoznione.getElement().getThemeList().add("error");
                h.add(opoznione);
            }
            return h;
        }).setHeader("Planowany zwrot").setAutoWidth(true).setFlexGrow(0);
        gridAktualne.addColumn(w -> egzemplarze(w)).setHeader("Egzemplarze").setFlexGrow(3);
        gridAktualne.addColumn(w -> String.format("%.0f zł", w.oplata()))
                .setHeader("Opłata").setAutoWidth(true).setFlexGrow(0);
        gridAktualne.addComponentColumn(w -> {
            Button zwrot = new Button("Przyjmij zwrot", VaadinIcon.ARROW_LEFT.create(), e -> otworzZwrot(w));
            zwrot.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            return zwrot;
        }).setHeader("Akcje").setAutoWidth(true).setFlexGrow(0);
        gridAktualne.setAllRowsVisible(true);

        // zakończone
        gridZakonczone.addColumn(w -> w.numer).setHeader("Wypożyczenie").setAutoWidth(true).setFlexGrow(0);
        gridZakonczone.addColumn(w -> w.klient).setHeader("Klient").setFlexGrow(1);
        gridZakonczone.addColumn(w -> FORMAT.format(w.odbior) + " – " + FORMAT.format(w.dataZwrotu != null ? w.dataZwrotu : w.planowanyZwrot))
                .setHeader("Wydano – zwrócono").setAutoWidth(true).setFlexGrow(0);
        gridZakonczone.addColumn(w -> egzemplarze(w)).setHeader("Egzemplarze").setFlexGrow(3);
        gridZakonczone.addColumn(w -> String.format("%.0f zł", w.razem()))
                .setHeader("Opłata łącznie").setAutoWidth(true).setFlexGrow(0);
        gridZakonczone.addComponentColumn(w -> {
            Span s = new Span(w.status.etykieta);
            s.getElement().getThemeList().add("badge");
            s.getElement().getThemeList().add(w.status.motyw);
            return s;
        }).setHeader("Status").setAutoWidth(true).setFlexGrow(0);
        gridZakonczone.setAllRowsVisible(true);
    }

    private String egzemplarze(Wypozyczenie w) {
        return w.pozycje.stream().map(Pozycja::opis).collect(Collectors.joining(", "));
    }

    private void odswiez() {
        ustaw(gridDoWydania, brakDoWydania, Status.DO_WYDANIA);
        ustaw(gridAktualne, brakAktualnych, Status.WYPOZYCZONE);
        ustaw(gridZakonczone, brakZakonczonych, Status.ZWROCONE);
    }

    private void ustaw(Grid<Wypozyczenie> g, Paragraph pusty, Status s) {
        List<Wypozyczenie> l = lista.stream().filter(w -> w.status == s).toList();
        g.setItems(new ArrayList<>(l));
        g.setVisible(!l.isEmpty());
        pusty.setVisible(l.isEmpty());
    }

    // ------------------------------------------------------------------
    // PU14. Wydanie egzemplarzy
    // ------------------------------------------------------------------

    /** rez == null oznacza wypożyczenie bez rezerwacji. */
    private void otworzWydanie(Wypozyczenie rez) {
        boolean zRezerwacji = rez != null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(zRezerwacji
                ? "Wydanie egzemplarzy – rezerwacja " + rez.numer
                : "Nowe wypożyczenie (bez rezerwacji)");
        dialog.setWidth("800px");

        ComboBox<String> klient = new ComboBox<>("Klient", KLIENCI);
        klient.setRequiredIndicatorVisible(true);
        klient.setWidthFull();
        if (zRezerwacji) {
            klient.setValue(rez.klient);
            klient.setReadOnly(true);
        }

        DatePicker zwrot = new DatePicker("Planowany termin zwrotu");
        zwrot.setLocale(PL);
        zwrot.setMin(dzis);
        zwrot.setRequiredIndicatorVisible(true);
        if (zRezerwacji) {
            zwrot.setValue(rez.planowanyZwrot);
        }

        List<Pozycja> egz = zRezerwacji ? new ArrayList<>(rez.pozycje) : new ArrayList<>();

        Paragraph blad = new Paragraph();
        blad.getStyle().set("color", "var(--lumo-error-text-color)");
        blad.setVisible(false);

        Paragraph podsumowanie = new Paragraph();
        podsumowanie.getStyle().set("white-space", "pre-line");

        Checkbox weryfikacja = new Checkbox("Zweryfikowano stan i kompletność wydawanych egzemplarzy");

        Button zatwierdz = new Button("Zatwierdź wydanie");
        zatwierdz.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Grid<Pozycja> egzGrid = new Grid<>(Pozycja.class, false);

        Runnable odswiezEgz = () -> {
            egzGrid.setItems(new ArrayList<>(egz));
            double suma = egz.stream().mapToDouble(Pozycja::stawka).sum();
            StringBuilder sb = new StringBuilder();
            sb.append("Klient: ").append(klient.isEmpty() ? "nie wybrano" : klient.getValue()).append("\n");
            sb.append("Pracownik: ").append(PRACOWNIK).append("\n");
            sb.append("Data wydania: ").append(FORMAT.format(dzis)).append("\n");
            if (zwrot.isEmpty()) {
                sb.append("Planowany termin zwrotu: nie wybrano\n");
                sb.append("Liczba egzemplarzy: ").append(egz.size());
            } else {
                long doby = Math.max(1, ChronoUnit.DAYS.between(dzis, zwrot.getValue()));
                sb.append("Planowany termin zwrotu: ").append(FORMAT.format(zwrot.getValue()))
                  .append(" (").append(doby).append(" dób)\n");
                sb.append("Liczba egzemplarzy: ").append(egz.size()).append("\n");
                sb.append(String.format("Opłata za wypożyczenie: %.0f zł", suma * doby));
            }
            podsumowanie.setText(sb.toString());
        };

        egzGrid.addColumn(Pozycja::opis).setHeader("Egzemplarz").setFlexGrow(1);
        egzGrid.addColumn(p -> String.format("%.0f zł / doba", p.stawka()))
                .setHeader("Stawka").setAutoWidth(true).setFlexGrow(0);
        if (!zRezerwacji) {
            egzGrid.addComponentColumn(p -> {
                Button usun = new Button("Usuń", ev -> {
                    egz.remove(p);
                    odswiezEgz.run();
                });
                usun.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                return usun;
            }).setAutoWidth(true).setFlexGrow(0);
        }
        egzGrid.setAllRowsVisible(true);

        klient.addValueChangeListener(e -> odswiezEgz.run());
        zwrot.addValueChangeListener(e -> odswiezEgz.run());

        // wybór egzemplarzy - tylko przy wypożyczeniu bez rezerwacji
        ComboBox<Pozycja> dodawany = new ComboBox<>("Dodaj wolny egzemplarz");
        dodawany.setItems(WOLNE);
        dodawany.setItemLabelGenerator(Pozycja::opis);
        dodawany.setWidth("400px");
        Button dodaj = new Button("Dodaj", VaadinIcon.PLUS.create(), ev -> {
            Pozycja p = dodawany.getValue();
            if (p == null) {
                return;
            }
            if (egz.contains(p)) {
                Notification.show("Ten egzemplarz jest już na liście.");
                return;
            }
            egz.add(p);
            dodawany.clear();
            odswiezEgz.run();
        });
        HorizontalLayout dodawanie = new HorizontalLayout(dodawany, dodaj);
        dodawanie.setAlignItems(Alignment.END);
        dodawanie.setVisible(!zRezerwacji);

        // krok 1: sprawdzenie dostępności wydawanych egzemplarzy (rezerwacja)
        String niedostepne = egz.stream()
                .filter(p -> NIEDOSTEPNE.containsKey(p.id()))
                .map(p -> p.id() + " (" + NIEDOSTEPNE.get(p.id()) + ")")
                .collect(Collectors.joining(", "));
        if (!niedostepne.isEmpty()) {
            blad.setText("Nie można wydać rezerwacji – niedostępne egzemplarze: " + niedostepne
                    + ". Zmień rezerwację lub poczekaj na zakończenie konserwacji.");
            blad.setVisible(true);
            zatwierdz.setEnabled(false);
        }

        // krok 4-6: zatwierdzenie wydania i rejestracja wypożyczenia
        zatwierdz.addClickListener(ev -> {
            if (klient.isEmpty()) {
                klient.setErrorMessage("Wybierz klienta");
                klient.setInvalid(true);
                pokazBlad("Wybierz klienta.");
                return;
            }
            klient.setInvalid(false);
            if (zwrot.isEmpty() || zwrot.getValue().isBefore(dzis)) {
                zwrot.setErrorMessage("Podaj planowany termin zwrotu (nie wcześniejszy niż dziś)");
                zwrot.setInvalid(true);
                pokazBlad("Podaj poprawny planowany termin zwrotu.");
                return;
            }
            zwrot.setInvalid(false);
            if (egz.isEmpty()) {
                pokazBlad("Dodaj co najmniej jeden egzemplarz do wypożyczenia.");
                return;
            }
            if (egz.size() > LIMIT_EGZEMPLARZY) {
                pokazBlad("Przekroczono limit egzemplarzy w jednym wypożyczeniu (" + LIMIT_EGZEMPLARZY + ").");
                return;
            }
            if (!weryfikacja.getValue()) {
                pokazBlad("Potwierdź weryfikację stanu i kompletności wydawanych egzemplarzy.");
                return;
            }

            String numer = String.format("WYP-%03d", licznik++);
            if (zRezerwacji) {
                rez.numer = numer;
                rez.odbior = dzis;
                rez.planowanyZwrot = zwrot.getValue();
                rez.pozycje = new ArrayList<>(egz);
                rez.pracownik = PRACOWNIK;
                rez.status = Status.WYPOZYCZONE;
            } else {
                lista.add(0, new Wypozyczenie(numer, klient.getValue(), dzis, zwrot.getValue(),
                        egz, Status.WYPOZYCZONE, null, PRACOWNIK));
            }
            dialog.close();
            odswiez();
            zakladki.setSelectedIndex(1);
            Notification n = Notification.show("Wypożyczenie " + numer + " zostało zarejestrowane. "
                    + "Egzemplarze mają status „wypożyczony”.");
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

        VerticalLayout zawartosc = new VerticalLayout(
                blad, klient, zwrot, new H4("Egzemplarze"), egzGrid, dodawanie,
                weryfikacja, new H4("Podsumowanie wypożyczenia"), podsumowanie);
        zawartosc.setPadding(false);
        dialog.add(zawartosc);
        odswiezEgz.run();

        Button zamknij = new Button("Zamknij", ev -> dialog.close());
        dialog.getFooter().add(zamknij, zatwierdz);
        dialog.open();
    }

    // ------------------------------------------------------------------
    // PU15. Przyjęcie zwrotu (+ PU16, PU17)
    // ------------------------------------------------------------------

    private void otworzZwrot(Wypozyczenie w) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Przyjęcie zwrotu – " + w.numer + " (" + w.klient + ")");
        dialog.setWidth("1050px");

        DatePicker dataZwrotu = new DatePicker("Data zwrotu", dzis);
        dataZwrotu.setLocale(PL);
        dataZwrotu.setMin(w.odbior);
        dataZwrotu.setMax(dzis);
        dataZwrotu.setRequiredIndicatorVisible(true);

        List<ZwrotPozycji> pozycje = w.pozycje.stream().map(ZwrotPozycji::new).collect(Collectors.toList());
        Grid<ZwrotPozycji> zwrotGrid = new Grid<>(ZwrotPozycji.class, false);

        // PU17 - opłaty dodatkowe (Tylko za opóźnienie)
        Paragraph opoznienie = new Paragraph();

        VerticalLayout sekcjaOplat = new VerticalLayout(
                new H4("Opłaty dodatkowe"), opoznienie);
        sekcjaOplat.setPadding(false);

        Paragraph razem = new Paragraph();
        razem.getStyle().set("white-space", "pre-line");

        Runnable odswiezOplaty = () -> {
            long dni = dniOpoznienia(w, dataZwrotu.getValue());
            sekcjaOplat.setVisible(dni > 0);
            opoznienie.setText(dni > 0
                    ? String.format("Zwrot po terminie: %d dni × %.0f zł = %.0f zł", dni, STAWKA_OPOZNIENIA, dni * STAWKA_OPOZNIENIA)
                    : "Zwrot w terminie – brak opłaty za opóźnienie.");
            
            // Kwota opłaty dodatkowej opiera się już tylko i wyłącznie na dniach opóźnienia
            double dodatkowa = dni * STAWKA_OPOZNIENIA;
            razem.setText(String.format("Opłata za wypożyczenie: %.0f zł\nOpłaty dodatkowe: %.0f zł\nŁącznie: %.0f zł",
                    w.oplata(), dodatkowa, w.oplata() + dodatkowa));
        };

        dataZwrotu.addValueChangeListener(e -> odswiezOplaty.run());

        // tabela kontroli egzemplarzy: kompletność, stan, uwagi
        zwrotGrid.addColumn(z -> z.pozycja.opis()).setHeader("Egzemplarz").setFlexGrow(2);
        zwrotGrid.addComponentColumn(z -> {
            Checkbox c = new Checkbox("Komplet");
            c.setValue(z.kompletny);
            c.addValueChangeListener(e -> {
                z.kompletny = e.getValue();
                odswiezOplaty.run();
            });
            return c;
        }).setHeader("Kompletność").setAutoWidth(true).setFlexGrow(0);
        zwrotGrid.addComponentColumn(z -> {
            ComboBox<String> stan = new ComboBox<>();
            stan.setItems(STAN_DOBRY, STAN_CZYSZCZENIE, STAN_NAPRAWA);
            stan.setValue(z.stan);
            stan.setAllowCustomValue(false);
            stan.setWidth("240px");

            Span info = new Span(z.zlecenie == null ? "" : "✔ " + z.zlecenie);
            Button skieruj = new Button("Skieruj", VaadinIcon.TOOLS.create(),
                    ev -> otworzSkierowanie(z, info));
            skieruj.addThemeVariants(ButtonVariant.LUMO_SMALL);
            skieruj.setEnabled(!STAN_DOBRY.equals(z.stan));

            stan.addValueChangeListener(e -> {
                z.stan = e.getValue() == null ? STAN_DOBRY : e.getValue();
                boolean dobry = STAN_DOBRY.equals(z.stan);
                skieruj.setEnabled(!dobry);
                if (dobry) {
                    z.zlecenie = null;
                    info.setText("");
                }
                odswiezOplaty.run();
            });
            HorizontalLayout h = new HorizontalLayout(stan, skieruj, info);
            h.setAlignItems(Alignment.CENTER);
            return h;
        }).setHeader("Stan egzemplarza").setFlexGrow(3);
        zwrotGrid.addComponentColumn(z -> {
            TextField uwagi = new TextField();
            uwagi.setPlaceholder("Uwagi");
            uwagi.setValue(z.uwagi);
            uwagi.setWidthFull();
            uwagi.setValueChangeMode(ValueChangeMode.EAGER);
            uwagi.addValueChangeListener(e -> z.uwagi = e.getValue());
            return uwagi;
        }).setHeader("Uwagi").setFlexGrow(2);
        zwrotGrid.setItems(pozycje);
        zwrotGrid.setAllRowsVisible(true);

        Paragraph termin = new Paragraph("Wydano: " + FORMAT.format(w.odbior)
                + " · Planowany termin zwrotu: " + FORMAT.format(w.planowanyZwrot)
                + " · Wydał: " + (w.pracownik == null ? "–" : w.pracownik));

        VerticalLayout zawartosc = new VerticalLayout(
                termin, dataZwrotu, new H4("Kontrola zwracanych egzemplarzy"), zwrotGrid,
                sekcjaOplat, new H4("Podsumowanie"), razem);
        zawartosc.setPadding(false);
        dialog.add(zawartosc);
        odswiezOplaty.run();

        Button zatwierdz = new Button("Zatwierdź zwrot", ev -> {
            if (dataZwrotu.isEmpty()) {
                dataZwrotu.setErrorMessage("Podaj datę zwrotu");
                dataZwrotu.setInvalid(true);
                pokazBlad("Podaj datę zwrotu.");
                return;
            }
            dataZwrotu.setInvalid(false);

            for (ZwrotPozycji z : pozycje) {
                // Weryfikacja braków: Wymagamy jedynie opisania, bez opłaty
                if (!z.kompletny && z.uwagi.isBlank()) {
                    pokazBlad("Opisz brakujący element w uwagach – egzemplarz " + z.pozycja.id() + ".");
                    return;
                }
                if (!STAN_DOBRY.equals(z.stan) && z.zlecenie == null) {
                    pokazBlad("Skieruj egzemplarz " + z.pozycja.id()
                            + " do czyszczenia lub naprawy (przycisk „Skieruj”) albo zmień jego stan.");
                    return;
                }
            }

            // Opłata jest wyliczana jedynie z opóźnienia
            w.dataZwrotu = dataZwrotu.getValue();
            w.oplataDodatkowa = dniOpoznienia(w, dataZwrotu.getValue()) * STAWKA_OPOZNIENIA;
            w.status = Status.ZWROCONE;
            long doKonserwacji = pozycje.stream().filter(z -> z.zlecenie != null).count();

            dialog.close();
            odswiez();
            zakladki.setSelectedIndex(2);
            Notification n = Notification.show("Zwrot zarejestrowany."
                    + (doKonserwacji > 0 ? " Do konserwacji skierowano egzemplarzy: " + doKonserwacji + "." : ""));
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        zatwierdz.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button zamknij = new Button("Zamknij", ev -> dialog.close());
        dialog.getFooter().add(zamknij, zatwierdz);
        dialog.open();
    }

    private long dniOpoznienia(Wypozyczenie w, LocalDate dataZwrotu) {
        if (dataZwrotu == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(w.planowanyZwrot, dataZwrotu));
    }

    // ------------------------------------------------------------------
    // PU16. Zlecenie czyszczenia lub naprawy
    // ------------------------------------------------------------------

    private void otworzSkierowanie(ZwrotPozycji z, Span info) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Zlecenie czyszczenia lub naprawy");
        dialog.setWidth("520px");

        Paragraph egzemplarz = new Paragraph("Egzemplarz: " + z.pozycja.opis());
        
        ComboBox<String> rodzaj = new ComboBox<>("Rodzaj usługi");
        rodzaj.setWidthFull();
        if (STAN_CZYSZCZENIE.equals(z.stan)) {
            rodzaj.setItems(RODZAJE_CZYSZCZENIA);
        } else {
            rodzaj.setItems(RODZAJE_NAPRAWY);
        }
        
        Button zapisz = new Button("Zapisz", ev -> {
            if (rodzaj.isEmpty()) {
                rodzaj.setErrorMessage("Wybierz rodzaj usługi");
                rodzaj.setInvalid(true);
                return;
            }
            z.zlecenie = (STAN_CZYSZCZENIE.equals(z.stan) ? ZLECENIE_CZYSZCZENIE : ZLECENIE_NAPRAWA) 
                         + " - " + rodzaj.getValue();
            info.setText("✔ " + z.zlecenie);
            dialog.close();
        });
        zapisz.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button anuluj = new Button("Anuluj", ev -> dialog.close());
        
        dialog.add(new VerticalLayout(egzemplarz, rodzaj));
        dialog.getFooter().add(anuluj, zapisz);
        dialog.open();
    }

    private void pokazBlad(String komunikat) {
        Notification n = Notification.show(komunikat);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}