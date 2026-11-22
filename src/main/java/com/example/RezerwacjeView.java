package com.example;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.vaadin.flow.component.HasValidation;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * PU08. Zarządzanie rezerwacjami (aktor: Pracownik obsługi, Kierownik)
 *  - lista wszystkich rezerwacji z wyszukiwaniem i filtrem statusu,
 *  - utworzenie nowej rezerwacji dla klienta (+ PU11. Sprawdzenie dostępności),
 *  - modyfikowanie rezerwacji (okno dialogowe),
 *  - anulowanie rezerwacji (okno potwierdzenia).
 *
 * Makieta - dane na sztywno, bez bazy danych.
 * Symulacja PU11: egzemplarze są "niedostępne", gdy dzień daty odbioru dzieli się przez 5.
 */
@Route(value = "rezerwacje", autoLayout = false)   // bez menu bocznego
@PageTitle("Rezerwacje")
public class RezerwacjeView extends VerticalLayout {

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

    record ModelInfo(String nazwa, String kategoria, double stawka, List<String> rozmiary) {
    }

    static class Rezerwacja {
        final String numer;
        final String klient;
        LocalDate odbior;
        LocalDate zwrot;
        List<Pozycja> pozycje;
        Status status;

        Rezerwacja(String numer, String klient, LocalDate odbior, LocalDate zwrot,
                   List<Pozycja> pozycje, Status status) {
            this.numer = numer;
            this.klient = klient;
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

    private static final List<String> KLIENCI = List.of(
            "Chuck Norris", "Anna Nowak", "Maria Wiśniewska", "Piotr Zieliński",
            "Teatr Miejski (Jan Mazur)");

    private static final List<ModelInfo> MODELE = List.of(
            new ModelInfo("Suknia balowa „Księżniczka”", "Suknie i balowe", 120.0, List.of("S", "M", "L")),
            new ModelInfo("Suknia wieczorowa", "Suknie i balowe", 90.0, List.of("XS", "S", "M")),
            new ModelInfo("Garnitur wieczorowy", "Garnitury", 90.0, List.of("M", "L", "XL")),
            new ModelInfo("Kostium Pirata", "Przebrania", 70.0, List.of("S", "M", "L", "XL")),
            new ModelInfo("Kostium Czarownicy", "Przebrania", 65.0, List.of("S", "M", "L")),
            new ModelInfo("Strój Mikołaja", "Stroje świąteczne", 60.0, List.of("L", "XL")),
            new ModelInfo("Kostium Elfa", "Stroje świąteczne", 50.0, List.of("S", "M")),
            new ModelInfo("Strój krakowski", "Stroje ludowe", 70.0, List.of("S", "M", "L")),
            new ModelInfo("Kapelusz cylinder", "Dodatki", 25.0, List.of("Uniwersalny")),
            new ModelInfo("Maska wenecka", "Dodatki", 30.0, List.of("Uniwersalny")),
            new ModelInfo("Peruka barokowa", "Dodatki", 40.0, List.of("Uniwersalny")),
            new ModelInfo("Peleryna wampira", "Dodatki", 35.0, List.of("M", "L", "XL")),
            new ModelInfo("Zestaw biżuterii „Królowa”", "Dodatki", 45.0, List.of("Uniwersalny")));

    /** Egzemplarze do dodania przy modyfikacji rezerwacji. */
    private static final List<Pozycja> DO_DODANIA = List.of(
            new Pozycja("EGZ-401", "Maska wenecka", "Uniwersalny", 30),
            new Pozycja("EGZ-402", "Peruka barokowa", "Uniwersalny", 40),
            new Pozycja("EGZ-403", "Peleryna wampira", "L", 35),
            new Pozycja("EGZ-404", "Kostium Czarownicy", "M", 65));

    private final LocalDate dzis = LocalDate.now();
    private int licznik = 7;

    private final List<Rezerwacja> rezerwacje = new ArrayList<>(List.of(
            new Rezerwacja("REZ-006", "Chuck Norris", dzis.plusDays(2), dzis.plusDays(4),
                    List.of(new Pozycja("EGZ-501", "Kostium Elfa", "M", 50)),
                    Status.ZAREZERWOWANA),
            new Rezerwacja("REZ-005", "Anna Nowak", dzis.plusDays(7), dzis.plusDays(9),
                    List.of(new Pozycja("EGZ-214", "Kostium Pirata", "M", 70),
                            new Pozycja("EGZ-318", "Kapelusz cylinder", "Uniwersalny", 25)),
                    Status.ZAREZERWOWANA),
            new Rezerwacja("REZ-004", "Teatr Miejski (Jan Mazur)", dzis.plusDays(14), dzis.plusDays(17),
                    List.of(new Pozycja("EGZ-610", "Strój krakowski", "S", 70),
                            new Pozycja("EGZ-611", "Strój krakowski", "M", 70),
                            new Pozycja("EGZ-612", "Strój krakowski", "L", 70)),
                    Status.ZAREZERWOWANA),
            new Rezerwacja("REZ-003", "Maria Wiśniewska", dzis.minusDays(1), dzis.plusDays(1),
                    List.of(new Pozycja("EGZ-105", "Suknia balowa „Księżniczka”", "M", 120)),
                    Status.WYPOZYCZONA),
            new Rezerwacja("REZ-002", "Piotr Zieliński", dzis.minusDays(30), dzis.minusDays(28),
                    List.of(new Pozycja("EGZ-220", "Garnitur wieczorowy", "L", 90)),
                    Status.ZREALIZOWANA),
            new Rezerwacja("REZ-001", "Anna Nowak", dzis.minusDays(60), dzis.minusDays(58),
                    List.of(new Pozycja("EGZ-150", "Strój Mikołaja", "XL", 60)),
                    Status.ANULOWANA)));

    private final Grid<Rezerwacja> grid = new Grid<>(Rezerwacja.class, false);
    private final TextField szukaj = new TextField();
    private final ComboBox<Status> filtrStatusu = new ComboBox<>();
    private final Paragraph brakWynikow = new Paragraph("Brak rezerwacji spełniających kryteria.");

    public RezerwacjeView() {
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);

        Button wroc = new Button("Strona główna", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate(""));
        wroc.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button nowa = new Button("Nowa rezerwacja", VaadinIcon.PLUS.create(), e -> otworzNowaRezerwacje());
        nowa.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout pasek = new HorizontalLayout(wroc, nowa);
        pasek.setWidthFull();
        pasek.setJustifyContentMode(JustifyContentMode.BETWEEN);

        szukaj.setPlaceholder("Szukaj po kliencie lub numerze rezerwacji");
        szukaj.setPrefixComponent(VaadinIcon.SEARCH.create());
        szukaj.setValueChangeMode(ValueChangeMode.EAGER);
        szukaj.setClearButtonVisible(true);
        szukaj.setWidth("360px");
        szukaj.addValueChangeListener(e -> odswiez());

        filtrStatusu.setItems(Status.values());
        filtrStatusu.setItemLabelGenerator(s -> s.etykieta);
        filtrStatusu.setPlaceholder("Wszystkie statusy");
        filtrStatusu.setClearButtonVisible(true);
        filtrStatusu.addValueChangeListener(e -> odswiez());

        HorizontalLayout filtry = new HorizontalLayout(szukaj, filtrStatusu);
        filtry.setWidthFull();
        filtry.getStyle().set("flex-wrap", "wrap");

        konfigurujTabele();

        Div tresc = new Div(
                pasek,
                new H2("Rezerwacje"),
                new Paragraph("Przeglądaj rezerwacje klientów, twórz nowe oraz modyfikuj lub anuluj "
                        + "istniejące."),
                filtry,
                brakWynikow,
                grid);
        tresc.setWidthFull();
        tresc.setMaxWidth("1200px");

        add(tresc);
        odswiez();
    }

    // ------------------------------------------------------------------
    // Lista rezerwacji
    // ------------------------------------------------------------------

    private void konfigurujTabele() {
        grid.addColumn(r -> r.numer).setHeader("Nr rezerwacji").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> r.klient).setHeader("Klient").setFlexGrow(1);
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
        String fraza = szukaj.getValue() == null ? "" : szukaj.getValue().trim().toLowerCase(PL);
        Status status = filtrStatusu.getValue();

        List<Rezerwacja> wynik = rezerwacje.stream()
                .filter(r -> fraza.isEmpty()
                        || r.klient.toLowerCase(PL).contains(fraza)
                        || r.numer.toLowerCase(PL).contains(fraza))
                .filter(r -> status == null || r.status == status)
                .toList();

        grid.setItems(new ArrayList<>(wynik));
        grid.setVisible(!wynik.isEmpty());
        brakWynikow.setVisible(wynik.isEmpty());
    }

    // ------------------------------------------------------------------
    // Utworzenie nowej rezerwacji (+ PU11. Sprawdzenie dostępności)
    // ------------------------------------------------------------------

    private void otworzNowaRezerwacje() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nowa rezerwacja");
        dialog.setWidth("800px");

        // elementy deklarowane najpierw, bo korzystają z nich pozostałe lambdy
        Grid<Pozycja> dostepneGrid = new Grid<>(Pozycja.class, false);
        Grid<Pozycja> wybraneGrid = new Grid<>(Pozycja.class, false);
        Paragraph komunikat = new Paragraph();
        Paragraph podsumowanie = new Paragraph();
        podsumowanie.getStyle().set("white-space", "pre-line");
        List<Pozycja> dostepne = new ArrayList<>();
        List<Pozycja> wybrane = new ArrayList<>();

        Div sekcjaDostepnosci = new Div(new H4("Dostępne egzemplarze"), komunikat, dostepneGrid);
        sekcjaDostepnosci.setWidthFull();
        sekcjaDostepnosci.setVisible(false);

        ComboBox<String> klient = new ComboBox<>("Klient", KLIENCI);
        klient.setRequiredIndicatorVisible(true);

        ComboBox<String> kategoria = new ComboBox<>("Kategoria");
        kategoria.setItems(MODELE.stream().map(ModelInfo::kategoria).distinct().toList());
        kategoria.setRequiredIndicatorVisible(true);

        ComboBox<ModelInfo> model = new ComboBox<>("Model");
        model.setItemLabelGenerator(ModelInfo::nazwa);
        model.setEnabled(false);
        model.setRequiredIndicatorVisible(true);

        ComboBox<String> rozmiar = new ComboBox<>("Rozmiar");
        rozmiar.setRequiredIndicatorVisible(true);

        DatePicker odbior = new DatePicker("Planowana data odbioru");
        DatePicker zwrot = new DatePicker("Planowana data zwrotu");
        for (DatePicker d : new DatePicker[] {odbior, zwrot}) {
            d.setLocale(PL);
            d.setMin(dzis);
            d.setRequiredIndicatorVisible(true);
        }

        Runnable odswiezDostepne = () -> {
            dostepneGrid.setItems(new ArrayList<>(dostepne));
            dostepneGrid.setVisible(!dostepne.isEmpty());
            komunikat.setText(dostepne.isEmpty()
                    ? "Brak dostępnych egzemplarzy w wybranym terminie. "
                            + "Wybierz inny model, rozmiar lub termin."
                    : "Egzemplarze dostępne w wybranym terminie:");
        };

        Runnable odswiezWybrane = () -> {
            wybraneGrid.setItems(new ArrayList<>(wybrane));
            wybraneGrid.setVisible(!wybrane.isEmpty());
            StringBuilder sb = new StringBuilder("Klient: ")
                    .append(klient.isEmpty() ? "nie wybrano" : klient.getValue()).append("\n");
            if (odbior.getValue() != null && zwrot.getValue() != null) {
                long doby = Math.max(1, ChronoUnit.DAYS.between(odbior.getValue(), zwrot.getValue()));
                double suma = wybrane.stream().mapToDouble(Pozycja::stawka).sum() * doby;
                sb.append("Termin: ").append(FORMAT.format(odbior.getValue())).append(" – ")
                  .append(FORMAT.format(zwrot.getValue())).append(" (").append(doby).append(" dób)\n");
                sb.append("Liczba egzemplarzy: ").append(wybrane.size()).append("\n");
                sb.append(String.format("Szacowany koszt: %.0f zł", suma));
            } else {
                sb.append("Termin: nie wybrano\nLiczba egzemplarzy: ").append(wybrane.size());
            }
            podsumowanie.setText(sb.toString());
        };

        Runnable ukryjDostepnosc = () -> {
            dostepne.clear();
            sekcjaDostepnosci.setVisible(false);
        };

        // kolumny tabel
        dostepneGrid.addColumn(Pozycja::opis).setHeader("Egzemplarz").setFlexGrow(1);
        dostepneGrid.addComponentColumn(p -> {
            Button dodaj = new Button("Dodaj do rezerwacji", ev -> {
                wybrane.add(p);
                dostepne.remove(p);
                odswiezDostepne.run();
                odswiezWybrane.run();
            });
            dodaj.addThemeVariants(ButtonVariant.LUMO_SMALL);
            return dodaj;
        }).setAutoWidth(true).setFlexGrow(0);
        dostepneGrid.setAllRowsVisible(true);

        wybraneGrid.addColumn(Pozycja::opis).setHeader("Egzemplarz").setFlexGrow(1);
        wybraneGrid.addColumn(p -> String.format("%.0f zł / doba", p.stawka()))
                .setHeader("Stawka").setAutoWidth(true).setFlexGrow(0);
        wybraneGrid.addComponentColumn(p -> {
            Button usun = new Button("Usuń", ev -> {
                wybrane.remove(p);
                odswiezWybrane.run();
            });
            usun.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            return usun;
        }).setAutoWidth(true).setFlexGrow(0);
        wybraneGrid.setAllRowsVisible(true);

        // zależności między polami
        klient.addValueChangeListener(e -> odswiezWybrane.run());

        kategoria.addValueChangeListener(e -> {
            model.clear();
            model.setItems(MODELE.stream()
                    .filter(m -> m.kategoria().equals(e.getValue()))
                    .toList());
            model.setEnabled(e.getValue() != null);
            ukryjDostepnosc.run();
        });
        model.addValueChangeListener(e -> {
            rozmiar.clear();
            rozmiar.setItems(e.getValue() == null ? List.<String>of() : e.getValue().rozmiary());
            ukryjDostepnosc.run();
        });
        rozmiar.addValueChangeListener(e -> ukryjDostepnosc.run());

        for (DatePicker d : new DatePicker[] {odbior, zwrot}) {
            d.addValueChangeListener(e -> {
                ukryjDostepnosc.run();
                if (!wybrane.isEmpty()) {
                    wybrane.clear();
                    Notification.show("Zmiana terminu wyczyściła listę wybranych egzemplarzy.");
                }
                odswiezWybrane.run();
            });
        }

        // PU11. Sprawdzenie dostępności egzemplarza w wybranym terminie
        Button sprawdz = new Button("Sprawdź dostępność", ev -> {
            boolean ok = true;
            ok &= wymagane(kategoria.isEmpty(), kategoria, "Wybierz kategorię");
            ok &= wymagane(model.isEmpty(), model, "Wybierz model");
            ok &= wymagane(rozmiar.isEmpty(), rozmiar, "Wybierz rozmiar");
            ok &= wymagane(odbior.isEmpty(), odbior, "Podaj datę odbioru");
            ok &= wymagane(zwrot.isEmpty(), zwrot, "Podaj datę zwrotu");
            if (ok && zwrot.getValue().isBefore(odbior.getValue())) {
                zwrot.setErrorMessage("Data zwrotu nie może być wcześniejsza niż data odbioru");
                zwrot.setInvalid(true);
                ok = false;
            }
            if (!ok) {
                pokazBlad("Uzupełnij poprawnie dane rezerwacji.");
                return;
            }

            dostepne.clear();
            if (dostepny(odbior.getValue())) {
                ModelInfo m = model.getValue();
                String r = rozmiar.getValue();
                int baza = Math.abs((m.nazwa() + r).hashCode()) % 900 + 100;
                int ile = 1 + (m.nazwa().length() % 3);
                for (int i = 0; i < ile; i++) {
                    dostepne.add(new Pozycja("EGZ-" + (baza + i), m.nazwa(), r, m.stawka()));
                }
            }
            dostepne.removeAll(wybrane);
            sekcjaDostepnosci.setVisible(true);
            odswiezDostepne.run();
        });
        sprawdz.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // układ okna
        FormLayout form = new FormLayout(klient, kategoria, model, rozmiar, odbior, zwrot);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));
        form.setColspan(klient, 2);
        form.setColspan(rozmiar, 2);

        VerticalLayout zawartosc = new VerticalLayout(
                form, sprawdz, sekcjaDostepnosci,
                new H4("Wybrane egzemplarze"), wybraneGrid,
                new H4("Podsumowanie rezerwacji"), podsumowanie);
        zawartosc.setPadding(false);
        dialog.add(zawartosc);
        odswiezWybrane.run();

        // zatwierdzenie rezerwacji
        Button zatwierdz = new Button("Zatwierdź rezerwację", ev -> {
            if (klient.isEmpty()) {
                klient.setErrorMessage("Wybierz klienta");
                klient.setInvalid(true);
                pokazBlad("Wybierz klienta, dla którego tworzysz rezerwację.");
                return;
            }
            klient.setInvalid(false);
            if (wybrane.isEmpty()) {
                pokazBlad("Dodaj co najmniej jeden egzemplarz do rezerwacji.");
                return;
            }
            String numer = String.format("REZ-%03d", licznik++);
            rezerwacje.add(0, new Rezerwacja(numer, klient.getValue(),
                    odbior.getValue(), zwrot.getValue(), wybrane, Status.ZAREZERWOWANA));
            dialog.close();
            odswiez();
            Notification n = Notification.show("Rezerwacja " + numer + " została utworzona.");
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        zatwierdz.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button zamknij = new Button("Zamknij", ev -> dialog.close());
        dialog.getFooter().add(zamknij, zatwierdz);

        dialog.open();
    }

    // ------------------------------------------------------------------
    // Anulowanie rezerwacji
    // ------------------------------------------------------------------

    private void potwierdzAnulowanie(Rezerwacja r) {
        ConfirmDialog d = new ConfirmDialog();
        d.setHeader("Anulować rezerwację " + r.numer + "?");
        d.setText("Rezerwacja klienta: " + r.klient + ". Zarezerwowane egzemplarze zostaną zwolnione "
                + "i będą ponownie dostępne w tym terminie. Tej operacji nie można cofnąć.");
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
    // Modyfikowanie rezerwacji (+ PU11 - ponowne sprawdzenie dostępności)
    // ------------------------------------------------------------------

    private void otworzModyfikacje(Rezerwacja r) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Modyfikowanie rezerwacji " + r.numer + " – " + r.klient);
        dialog.setWidth("720px");

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

        Button sprawdz = new Button("Sprawdź dostępność", ev -> {
            if (!terminPoprawny(odbior, zwrot)) {
                return;
            }
            komunikat.setVisible(true);
            komunikat.setText(dostepny(odbior.getValue())
                    ? "Wszystkie egzemplarze są dostępne w wybranym terminie."
                    : "Wybrane egzemplarze są niedostępne w nowym terminie. Wybierz inny termin.");
        });

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

    private boolean wymagane(boolean brak, HasValidation pole, String komunikat) {
        pole.setInvalid(brak);
        if (brak) {
            pole.setErrorMessage(komunikat);
        }
        return !brak;
    }

    private boolean terminPoprawny(DatePicker odbior, DatePicker zwrot) {
        boolean ok = true;
        ok &= wymagane(odbior.isEmpty(), odbior, "Podaj datę odbioru");
        ok &= wymagane(zwrot.isEmpty(), zwrot, "Podaj datę zwrotu");
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