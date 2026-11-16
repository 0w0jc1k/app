package com.example;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.example.WspolneElementy.blad;
import static com.example.WspolneElementy.naglowek;
import static com.example.WspolneElementy.wymagane;
import static com.example.WspolneElementy.zl;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * PU10. Utworzenie raportu (aktor: Kierownik):
 *  PU11. Dzienne zestawienie przychodów, PU12. Miesięczne zestawienie przychodów
 *  (opcjonalnie kategoria), PU13. Analiza popularności modeli i częstotliwości
 *  wypożyczania egzemplarzy.
 *
 * Makieta - historia wypożyczeń generowana losowo (stałe ziarno), bez bazy.
 */
@Route(value = "raporty", autoLayout = false)   // bez menu bocznego
@PageTitle("Raporty | Wypożyczalnia kostiumów")
public class RaportyView extends VerticalLayout {

    private static final Locale PL = Locale.forLanguageTag("pl-PL");

    enum Rodzaj {
        DZIENNY("Dzienne zestawienie przychodów"),
        MIESIECZNY("Miesięczne zestawienie przychodów"),
        POPULARNOSC("Analiza popularności modeli i częstotliwości wypożyczania egzemplarzy");

        private final String etykieta;

        Rodzaj(String etykieta) {
            this.etykieta = etykieta;
        }

        String etykieta() {
            return etykieta;
        }
    }

    record Mod(String nazwa, String kategoria, double stawka) {
    }

    record Wyp(LocalDate data, String model, String kategoria, String egzemplarz,
            double oplata, double dodatkowa) {
        double razem() {
            return oplata + dodatkowa;
        }
    }

    record WierszKategorii(String kategoria, long liczba, double oplaty, double dodatkowe) {
        double razem() {
            return oplaty + dodatkowe;
        }
    }

    record WierszModelu(String model, long liczba, double udzial, double przychod) {
    }

    record WierszEgz(String id, String model, long liczba) {
    }

    private static final List<Mod> MODELE = List.of(
            new Mod("Kostium Pirata", "Przebrania karnawałowe", 70),
            new Mod("Suknia wieczorowa", "Suknie wieczorowe", 90),
            new Mod("Garnitur wieczorowy", "Garnitury", 90),
            new Mod("Kostium Czarownicy", "Przebrania karnawałowe", 65),
            new Mod("Strój Mikołaja", "Przebrania karnawałowe", 60),
            new Mod("Strój krakowski", "Stroje ludowe", 70),
            new Mod("Suknia balowa „Księżniczka”", "Suknie wieczorowe", 120),
            new Mod("Kostium Elfa", "Przebrania karnawałowe", 50));

    private final List<Wyp> historia = new ArrayList<>();

    private final ComboBox<Rodzaj> rodzaj = new ComboBox<>("Rodzaj raportu");
    private final DatePicker dzien = new DatePicker("Dzień");
    private final ComboBox<YearMonth> miesiac = new ComboBox<>("Miesiąc");
    private final ComboBox<String> kategoria = new ComboBox<>("Kategoria (opcjonalnie)");
    private final DatePicker okresOd = new DatePicker("Okres od");
    private final DatePicker okresDo = new DatePicker("Okres do");
    private final Div wynik = new Div();

    public RaportyView() {
        setWidthFull();
        setAlignItems(Alignment.CENTER);

        wygenerujHistorie();
        konfigurujFormularz();

        Button generuj = new Button("Generuj raport", VaadinIcon.CHART.create(), e -> generuj());
        generuj.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout parametry = new HorizontalLayout(rodzaj, dzien, miesiac, kategoria,
                okresOd, okresDo, generuj);
        parametry.setAlignItems(FlexComponent.Alignment.END);
        parametry.getStyle().set("flex-wrap", "wrap");

        wynik.setWidthFull();
        wynik.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)");

        VerticalLayout tresc = new VerticalLayout();
        tresc.setPadding(false);
        tresc.setWidthFull();
        tresc.setMaxWidth("1100px");
        tresc.add(naglowek("Raporty",
                "Zestawienia przychodów oraz analiza popularności modeli."), parametry, wynik);
        add(tresc);

        pokazParametry();
    }

    // ------------------------------------------------------------------
    // Dane i formularz
    // ------------------------------------------------------------------

    private void wygenerujHistorie() {
        Random r = new Random(2026);
        LocalDate dzis = LocalDate.now();
        for (int i = 0; i < 240; i++) {
            int idx = Math.min(MODELE.size() - 1, (int) Math.abs(r.nextGaussian() * 3));
            Mod m = MODELE.get(idx);
            int doby = 1 + r.nextInt(5);
            double dodatkowa = (r.nextInt(100) < 20 ? 20.0 * (1 + r.nextInt(3)) : 0)
                    + (r.nextInt(100) < 8 ? 50.0 : 0);
            historia.add(new Wyp(dzis.minusDays(r.nextInt(180)), m.nazwa(), m.kategoria(),
                    "EGZ-" + (100 + idx * 10 + r.nextInt(4)), m.stawka() * doby, dodatkowa));
        }
    }

    private void konfigurujFormularz() {
        rodzaj.setItems(Rodzaj.values());
        rodzaj.setItemLabelGenerator(Rodzaj::etykieta);
        rodzaj.setValue(Rodzaj.DZIENNY);
        rodzaj.setAllowCustomValue(false);
        rodzaj.setWidth("360px");
        rodzaj.addValueChangeListener(e -> {
            pokazParametry();
            wynik.removeAll();
        });

        for (DatePicker d : new DatePicker[] {dzien, okresOd, okresDo}) {
            d.setLocale(PL);
            d.setMax(LocalDate.now());
            d.setRequiredIndicatorVisible(true);
        }
        dzien.setValue(LocalDate.now());
        okresOd.setValue(LocalDate.now().minusDays(30));
        okresDo.setValue(LocalDate.now());

        YearMonth teraz = YearMonth.now();
        List<YearMonth> miesiace = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            miesiace.add(teraz.minusMonths(i));
        }
        miesiac.setItems(miesiace);
        miesiac.setItemLabelGenerator(ym -> ym.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, PL)
                + " " + ym.getYear());
        miesiac.setValue(teraz);
        miesiac.setRequiredIndicatorVisible(true);

        kategoria.setItems(MODELE.stream().map(Mod::kategoria).distinct().toList());
        kategoria.setClearButtonVisible(true);
        kategoria.setPlaceholder("Wszystkie kategorie");
    }

    private void pokazParametry() {
        Rodzaj r = rodzaj.getValue();
        dzien.setVisible(r == Rodzaj.DZIENNY);
        miesiac.setVisible(r == Rodzaj.MIESIECZNY);
        kategoria.setVisible(r == Rodzaj.MIESIECZNY);
        okresOd.setVisible(r == Rodzaj.POPULARNOSC);
        okresDo.setVisible(r == Rodzaj.POPULARNOSC);
    }

    // ------------------------------------------------------------------
    // PU10. Utworzenie raportu
    // ------------------------------------------------------------------

    private void generuj() {
        wynik.removeAll();
        if (!wymagane(rodzaj, rodzaj.isEmpty(), "Wybierz rodzaj raportu")) {
            blad("Wybierz rodzaj raportu.");
            return;
        }
        switch (rodzaj.getValue()) {
            case DZIENNY -> raportDzienny();
            case MIESIECZNY -> raportMiesieczny();
            case POPULARNOSC -> raportPopularnosci();
        }
    }

    /** PU11 */
    private void raportDzienny() {
        if (!wymagane(dzien, dzien.isEmpty(), "Wskaż dzień")) {
            blad("Wskaż dzień raportu.");
            return;
        }
        LocalDate d = dzien.getValue();
        List<Wyp> lista = historia.stream().filter(w -> w.data().equals(d)).toList();
        wynik.add(new H3("Przychody z dnia " + d), tabelaPrzychodow(lista));
    }

    /** PU12 */
    private void raportMiesieczny() {
        if (!wymagane(miesiac, miesiac.isEmpty(), "Wskaż miesiąc")) {
            blad("Wskaż miesiąc raportu.");
            return;
        }
        YearMonth ym = miesiac.getValue();
        String kat = kategoria.getValue();
        List<Wyp> lista = historia.stream()
                .filter(w -> YearMonth.from(w.data()).equals(ym))
                .filter(w -> kat == null || w.kategoria().equals(kat))
                .toList();
        String tytul = "Przychody – " + ym.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, PL)
                + " " + ym.getYear() + (kat == null ? "" : " (" + kat + ")");
        wynik.add(new H3(tytul), tabelaPrzychodow(lista));
    }

    /** PU13 */
    private void raportPopularnosci() {
        boolean ok = wymagane(okresOd, okresOd.isEmpty(), "Podaj początek okresu");
        ok &= wymagane(okresDo, okresDo.isEmpty(), "Podaj koniec okresu");
        if (ok && okresOd.getValue().isAfter(okresDo.getValue())) {
            okresDo.setErrorMessage("Koniec okresu nie może być wcześniejszy niż początek");
            okresDo.setInvalid(true);
            ok = false;
        }
        if (!ok) {
            blad("Wskaż poprawny okres analizy.");
            return;
        }
        LocalDate od = okresOd.getValue();
        LocalDate dod = okresDo.getValue();
        List<Wyp> lista = historia.stream()
                .filter(w -> !w.data().isBefore(od) && !w.data().isAfter(dod)).toList();

        wynik.add(new H3("Popularność w okresie " + od + " – " + dod));
        if (lista.isEmpty()) {
            wynik.add(new Paragraph("Brak wypożyczeń w wybranym okresie."));
            return;
        }

        long suma = lista.size();
        List<WierszModelu> modele = lista.stream().collect(Collectors.groupingBy(Wyp::model))
                .entrySet().stream()
                .map(e -> new WierszModelu(e.getKey(), e.getValue().size(),
                        100.0 * e.getValue().size() / suma,
                        e.getValue().stream().mapToDouble(Wyp::razem).sum()))
                .sorted(Comparator.comparingLong(WierszModelu::liczba).reversed())
                .toList();

        Grid<WierszModelu> gm = new Grid<>(WierszModelu.class, false);
        gm.addColumn(WierszModelu::model).setHeader("Model").setAutoWidth(true);
        gm.addColumn(WierszModelu::liczba).setHeader("Liczba wypożyczeń");
        gm.addColumn(w -> String.format(PL, "%.1f%%", w.udzial())).setHeader("Udział");
        gm.addColumn(w -> zl(w.przychod())).setHeader("Przychód");
        gm.setItems(modele);
        gm.setAllRowsVisible(true);

        Map<String, String> modelEgz = lista.stream()
                .collect(Collectors.toMap(Wyp::egzemplarz, Wyp::model, (a, b) -> a));
        List<WierszEgz> egz = lista.stream()
                .collect(Collectors.groupingBy(Wyp::egzemplarz, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new WierszEgz(e.getKey(), modelEgz.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingLong(WierszEgz::liczba).reversed())
                .toList();

        Grid<WierszEgz> ge = new Grid<>(WierszEgz.class, false);
        ge.addColumn(WierszEgz::id).setHeader("Numer egzemplarza");
        ge.addColumn(WierszEgz::model).setHeader("Model").setAutoWidth(true);
        ge.addColumn(WierszEgz::liczba).setHeader("Liczba wypożyczeń");
        ge.setItems(egz);
        ge.setHeight("320px");

        wynik.add(new H4("Popularność modeli"), gm,
                new H4("Częstotliwość wypożyczania egzemplarzy"), ge);
    }

    // ------------------------------------------------------------------
    // Tabela przychodów (wspólna dla PU11 i PU12)
    // ------------------------------------------------------------------

    private Div tabelaPrzychodow(List<Wyp> lista) {
        Div blok = new Div();
        blok.setWidthFull();
        if (lista.isEmpty()) {
            blok.add(new Paragraph("Brak przychodów w wybranym okresie."));
            return blok;
        }

        List<WierszKategorii> wiersze = lista.stream()
                .collect(Collectors.groupingBy(Wyp::kategoria, TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new WierszKategorii(e.getKey(), e.getValue().size(),
                        e.getValue().stream().mapToDouble(Wyp::oplata).sum(),
                        e.getValue().stream().mapToDouble(Wyp::dodatkowa).sum()))
                .toList();

        Grid<WierszKategorii> g = new Grid<>(WierszKategorii.class, false);
        g.addColumn(WierszKategorii::kategoria).setHeader("Kategoria").setAutoWidth(true);
        g.addColumn(WierszKategorii::liczba).setHeader("Liczba wypożyczeń");
        g.addColumn(w -> zl(w.oplaty())).setHeader("Opłaty za wypożyczenie");
        g.addColumn(w -> zl(w.dodatkowe())).setHeader("Opłaty dodatkowe");
        g.addColumn(w -> zl(w.razem())).setHeader("Razem");
        g.setItems(wiersze);
        g.setAllRowsVisible(true);

        double podstawowe = lista.stream().mapToDouble(Wyp::oplata).sum();
        double dodatkowe = lista.stream().mapToDouble(Wyp::dodatkowa).sum();
        Paragraph suma = new Paragraph("Razem: " + zl(podstawowe + dodatkowe)
                + " (w tym opłaty dodatkowe: " + zl(dodatkowe) + "), wypożyczeń: " + lista.size());
        suma.getStyle().set("font-weight", "600");

        blok.add(g, suma);
        return blok;
    }
}