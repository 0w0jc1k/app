package com.example; 

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "katalog", autoLayout = false)   // bez menu bocznego
@PageTitle("Katalog kostiumów")
public class KatalogView extends VerticalLayout {

    /** Prosty model danych tylko na potrzeby makiety. */
    record ModelKostiumu(String nazwa, String kategoria, String opis,
                         double stawka, List<String> rozmiary, List<String> kolory) {
    }

    private static final String WSZYSTKIE = "Wszystkie kategorie";

    private final List<ModelKostiumu> modele = List.of(
            new ModelKostiumu("Suknia balowa „Księżniczka”", "Suknie i balowe",
                    "Długa suknia z tiulową spódnicą i gorsetem, idealna na bal lub sesję zdjęciową.",
                    120.0, List.of("S", "M", "L"), List.of("Błękitny", "Różowy")),
            new ModelKostiumu("Garnitur wieczorowy", "Garnitury",
                    "Klasyczny czarny garnitur z muszką, komplet: marynarka, spodnie, kamizelka.",
                    90.0, List.of("M", "L", "XL"), List.of("Czarny", "Granatowy")),
            new ModelKostiumu("Kostium Pirata", "Przebrania",
                    "Płaszcz, kapelusz i pas z akcesoriami. Świetny na imprezy tematyczne.",
                    70.0, List.of("S", "M", "L", "XL"), List.of("Brązowy")),
            new ModelKostiumu("Strój Mikołaja", "Stroje świąteczne",
                    "Czerwony strój z białym futerkiem, brodą i czapką.",
                    60.0, List.of("L", "XL"), List.of("Czerwony")),
            new ModelKostiumu("Kostium Elfa", "Stroje świąteczne",
                    "Zielono-czerwony kostium dla elfa z czapką i spiczastymi butami.",
                    50.0, List.of("S", "M"), List.of("Zielony")),
            new ModelKostiumu("Strój krakowski", "Stroje ludowe",
                    "Tradycyjny strój krakowski z gorsetem, spódnicą i wiankiem.",
                    70.0, List.of("S", "M", "L"), List.of("Czerwony", "Granatowy")),
            new ModelKostiumu("Kostium Czarownicy", "Przebrania",
                    "Czarna suknia z kapeluszem i peleryną, idealny na Halloween.",
                    65.0, List.of("S", "M", "L"), List.of("Czarny", "Fioletowy")),
            new ModelKostiumu("Suknia wieczorowa", "Suknie i balowe",
                    "Elegancka długa suknia wieczorowa z rozcięciem, na studniówkę lub galę.",
                    90.0, List.of("XS", "S", "M"), List.of("Czerwony", "Zielony", "Czarny"))
    );

    private final ComboBox<String> kategoria = new ComboBox<>("Kategoria");
    private final Div siatka = new Div();

    public KatalogView() {
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);

        add(new H2("Katalog kostiumów"),
            new Paragraph("Wybierz kategorię, aby zobaczyć dostępne stroje. "
                    + "Kliknij „Szczegóły”, aby poznać więcej informacji o danym modelu. "
                    ));

        konfigurujFiltr();
        konfigurujSiatke();

        add(kategoria, siatka);
        odswiez();
    }

    // --- filtr kategorii (krok 3) ---

    private void konfigurujFiltr() {
        List<String> kategorie = new ArrayList<>();
        kategorie.add(WSZYSTKIE);
        modele.stream().map(ModelKostiumu::kategoria).distinct().forEach(kategorie::add);

        kategoria.setItems(kategorie);
        kategoria.setValue(WSZYSTKIE);
        kategoria.setAllowCustomValue(false);
        kategoria.setWidth("300px");
        kategoria.addValueChangeListener(e -> odswiez());
    }

    private void konfigurujSiatke() {
        siatka.setWidthFull();
        siatka.setMaxWidth("1100px");
        siatka.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(260px, 1fr))")
                .set("gap", "var(--lumo-space-m)");
    }

    // --- lista modeli (kroki 2 i 3.1) ---

    private void odswiez() {
        siatka.removeAll();

        String wybrana = kategoria.getValue();
        List<ModelKostiumu> wynik = modele.stream()
                .filter(m -> wybrana == null || WSZYSTKIE.equals(wybrana) || m.kategoria().equals(wybrana))
                .toList();

        if (wynik.isEmpty()) {
            siatka.add(new Paragraph("Brak modeli w wybranej kategorii."));
            return;
        }
        wynik.forEach(m -> siatka.add(utworzKarte(m)));
    }

    private Div utworzKarte(ModelKostiumu m) {
        Span znacznikKategorii = new Span(m.kategoria());
        znacznikKategorii.getElement().getThemeList().add("badge");

        Paragraph stawka = new Paragraph(String.format("%.0f zł / doba", m.stawka()));
        stawka.getStyle().set("font-weight", "600").set("margin", "0");

        Button szczegoly = new Button("Szczegóły", e -> pokazSzczegoly(m));
        szczegoly.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button zarezerwuj = new Button("Zarezerwuj", e -> przejdzDoRezerwacji());
        zarezerwuj.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout przyciski = new HorizontalLayout(szczegoly, zarezerwuj);

        Div karta = new Div(new H4(m.nazwa()), znacznikKategorii, stawka, przyciski);
        karta.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-m)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");
        return karta;
    }

    // --- szczegóły modelu (krok 4) ---

    private void pokazSzczegoly(ModelKostiumu m) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(m.nazwa());

        VerticalLayout tresc = new VerticalLayout(
                new Paragraph(m.opis()),
                wiersz("Kategoria", m.kategoria()),
                wiersz("Stawka wypożyczenia", String.format("%.0f zł / doba", m.stawka())),
                wiersz("Dostępne rozmiary", String.join(", ", m.rozmiary())),
                wiersz("Dostępne kolory", String.join(", ", m.kolory())));
        tresc.setPadding(false);
        tresc.setSpacing(false);
        dialog.add(tresc);

        Button zamknij = new Button("Zamknij", e -> dialog.close());
        Button zarezerwuj = new Button("Zarezerwuj", e -> {
            dialog.close();
            przejdzDoRezerwacji();
        });
        zarezerwuj.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(zamknij, zarezerwuj);

        dialog.open();
    }

    private Div wiersz(String etykieta, String wartosc) {
        Span e = new Span(etykieta + ": ");
        e.getStyle().set("font-weight", "600");
        return new Div(e, new Span(wartosc));
    }

    private void przejdzDoRezerwacji() {
        // Widok rezerwacji (PU05) powstanie później - na razie ścieżka "rezerwacja"
        UI.getCurrent().navigate("rezerwacja");
    }
}