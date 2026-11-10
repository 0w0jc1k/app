package com.example;

import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Strona główna systemu - wyświetlana po wejściu na adres "/".
 *
 * Zawartość zależy od aktora (Rysunek 1 w sprawozdaniu):
 *  - Gość            - oferta, katalog, logowanie i rejestracja (Wypożyczalnia PU01, PU02, PU04),
 *  - Klient          - rezerwacja, katalog, wylogowanie (Wypożyczalnia PU03, PU05),
 *  - Pracownik obsługi, Pracownik garderoby, Serwisant, Kierownik
 *                    - kafelki modułów: Administracja, Garderoba, Konserwacja,
 *                      Stanowisko obsługi, Rezerwacje, Wypożyczenia, Raporty.
 *
 * Makieta: rola wybierana ręcznie z listy "Podgląd jako". Docelowo rola
 * będzie pochodzić z sesji po zalogowaniu (PU02 / Administracja PU14).
 */
@Route(value = "", autoLayout = false)   // bez menu bocznego
@PageTitle("Wypożyczalnia kostiumów i strojów okolicznościowych")
public class StronaGlownaView extends VerticalLayout {

    enum Rola {
        GOSC("Gość"),
        KLIENT("Klient"),
        PRACOWNIK_OBSLUGI("Pracownik obsługi"),
        PRACOWNIK_GARDEROBY("Pracownik garderoby"),
        SERWISANT("Serwisant"),
        KIEROWNIK("Kierownik");

        private final String etykieta;

        Rola(String etykieta) {
            this.etykieta = etykieta;
        }

        String etykieta() {
            return etykieta;
        }

        boolean pracownik() {
            return this != GOSC && this != KLIENT;
        }
    }

    record Kategoria(String ikona, String nazwa, String opis) {
    }

    // Widoki, które już istnieją w projekcie. Pozostałe pokażą tylko komunikat.
    private static final Set<String> ISTNIEJACE_TRASY =
            Set.of("login", "rejestracja", "katalog", "rezerwacja");

    private static final List<Kategoria> KATEGORIE = List.of(
            new Kategoria("🎬", "Postacie filmowe i bajkowe", "Kostiumy znanych bohaterów"),
            new Kategoria("🏰", "Stroje historyczne", "Na wydarzenia i inscenizacje"),
            new Kategoria("🎖️", "Mundury", "Na przedstawienia i sesje"),
            new Kategoria("👗", "Suknie wieczorowe", "Na studniówkę, bal i galę"),
            new Kategoria("🤵", "Garnitury", "Eleganckie komplety"),
            new Kategoria("🌾", "Stroje ludowe", "Tradycyjne stroje regionalne"),
            new Kategoria("🎭", "Przebrania karnawałowe", "Na bale przebierańców i Halloween"),
            new Kategoria("🎩", "Dodatki", "Kapelusze, maski, peruki, peleryny, biżuteria"));

    private Rola rola = Rola.GOSC;

    private final ComboBox<Rola> podgladRoli = new ComboBox<>("Podgląd jako:");
    private final VerticalLayout zawartosc = new VerticalLayout();

    public StronaGlownaView() {
        setWidthFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);

        podgladRoli.setItems(Rola.values());
        podgladRoli.setItemLabelGenerator(Rola::etykieta);
        podgladRoli.setValue(rola);
        podgladRoli.setAllowCustomValue(false);
        podgladRoli.setWidth("260px");
        podgladRoli.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                rola = e.getValue();
                odswiez();
            }
        });

        zawartosc.setPadding(false);
        zawartosc.setSpacing(false);
        zawartosc.setWidthFull();
        zawartosc.setAlignItems(Alignment.CENTER);

        add(utworzPasekGorny(), zawartosc);
        odswiez();
    }

    private void odswiez() {
        zawartosc.removeAll();
        zawartosc.add(utworzPasekRoli());

        VerticalLayout strona = new VerticalLayout();
        strona.setWidthFull();
        strona.setMaxWidth("1100px");
        strona.setPadding(true);
        strona.setSpacing(true);

        switch (rola) {
            case GOSC -> {
                strona.add(utworzBohaterGoscia());
                strona.add(utworzSekcjeKategorii());
                strona.add(utworzSekcjeJakToDziala());
                strona.add(utworzSekcjeZasad());
            }
            case KLIENT -> {
                strona.add(utworzBohaterKlienta());
                strona.add(utworzSekcjeKategorii());
                strona.add(utworzSekcjeZasad());
            }
            default -> strona.add(utworzPanelPracownika());
        }

        zawartosc.add(strona, utworzStopke());
    }

    private Div utworzPasekGorny() {
        Span logo = new Span("🎭");
        logo.getStyle().set("font-size", "1.8rem").set("line-height", "1");
        logo.getElement().setAttribute("aria-hidden", "true");

        Span nazwa = new Span("Wypożyczalnia kostiumów i strojów okolicznościowych");
        nazwa.getStyle().set("font-weight", "600");

        HorizontalLayout lewa = new HorizontalLayout(logo, nazwa);
        lewa.setAlignItems(FlexComponent.Alignment.CENTER);

        Div pasek = new Div(lewa);
        pasek.setWidthFull();
        pasek.getStyle()
                .set("box-sizing", "border-box")
                .set("padding", "var(--lumo-space-m)")
                .set("border-bottom", "1px solid var(--lumo-contrast-20pct)");
        return pasek;
    }

    /** Przełącznik roli (tylko makieta) + przyciski zależne od roli. */
    private HorizontalLayout utworzPasekRoli() {
        HorizontalLayout pasek = new HorizontalLayout();
        pasek.setWidthFull();
        pasek.setMaxWidth("1100px");
        pasek.setPadding(true);
        pasek.setAlignItems(FlexComponent.Alignment.END);
        pasek.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        pasek.getStyle().set("flex-wrap", "wrap");

        pasek.add(podgladRoli);

        if (rola == Rola.GOSC) {
            
        } else {
            Span kto = new Span(opisZalogowanego());
            kto.getStyle().set("font-weight", "600");
            Button wyloguj = new Button("Wyloguj się", VaadinIcon.SIGN_OUT.create(),
                    e -> wyloguj());
            wyloguj.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            HorizontalLayout prawa = new HorizontalLayout(kto, wyloguj);
            prawa.setAlignItems(FlexComponent.Alignment.CENTER);
            pasek.add(prawa);
        }
        return pasek;
    }

    private String opisZalogowanego() {
        // dane na sztywno - jak w widoku rezerwacji ("zalogowany klient")
        return switch (rola) {
            case KLIENT -> "Chuck Norris (Klient)";
            default -> "Jack Sparrow (" + rola.etykieta() + ")";
        };
    }

    /** PU03. Wylogowanie się: koniec sesji i powrót do strony głównej dla gościa. */
    private void wyloguj() {
        rola = Rola.GOSC;
        podgladRoli.setValue(Rola.GOSC);   // wywoła odswiez()
        Notification.show("Zostałeś wylogowany.");
    }

    // ------------------------------------------------------------------
    // Gość
    // ------------------------------------------------------------------

    private Div utworzBohaterGoscia() {
        H1 tytul = new H1("Stroje na każdą okazję");
        tytul.getStyle().set("margin", "0");

        Paragraph opis = new Paragraph(
                "Wypożycz kostium lub strój okolicznościowy na bal przebierańców, "
                        + "przedstawienie, studniówkę, wesele, Halloween czy sesję zdjęciową. "
                        + "Wybierz model, sprawdź dostępność w swoim terminie i zarezerwuj online!");
        opis.getStyle().set("max-width", "700px").set("opacity", "0.8");

        Button katalog = new Button("Przeglądaj katalog", VaadinIcon.SEARCH.create(),
                e -> idzDo("katalog"));
        katalog.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        Button rejestracja = new Button("Utwórz konto klienta", VaadinIcon.USER.create(),
                e -> idzDo("rejestracja"));
        rejestracja.addThemeVariants(ButtonVariant.LUMO_LARGE);

        Button logowanie = new Button("Mam już konto", VaadinIcon.SIGN_IN.create(),
                e -> idzDo("login"));
        logowanie.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_LARGE);

        HorizontalLayout przyciski = new HorizontalLayout(katalog, rejestracja, logowanie);
        przyciski.getStyle().set("flex-wrap", "wrap");

        return ramkaBohatera(tytul, opis, przyciski);
    }

    private Div utworzSekcjeJakToDziala() {
        Div siatka = siatka("220px");
        siatka.add(
                krok("1", "Wybierz strój", "Przejrzyj katalog i wybierz model, rozmiar oraz kolor."),
                krok("2", "Zarezerwuj", "Podaj datę odbioru i zwrotu. System sprawdzi dostępność."),
                krok("3", "Odbierz w lokalu", "Pracownik przygotuje stroje i zarejestruje wypożyczenie."),
                krok("4", "Zwróć w terminie", "Po kontroli kompletności i stanu zamykamy wypożyczenie."));
        return sekcja("Jak to działa?", siatka);
    }

    // ------------------------------------------------------------------
    // Klient
    // ------------------------------------------------------------------

    private Div utworzBohaterKlienta() {
        H2 tytul = new H2("Witaj, Chuck!");
        tytul.getStyle().set("margin", "0");

        Paragraph opis = new Paragraph("Co chcesz dziś zrobić?");
        opis.getStyle().set("margin", "0").set("opacity", "0.8");

        Div siatka = siatka("240px");
        siatka.add(
                kafelek(VaadinIcon.CALENDAR_USER, "Nowa rezerwacja",
                        "Wybierz model, rozmiar i termin.", "rezerwacja"),
                kafelek(VaadinIcon.SEARCH, "Katalog kostiumów",
                        "Przeglądaj ofertę według kategorii.", "katalog"),
                kafelek(VaadinIcon.LIST, "Moje rezerwacje",
                        "Podgląd, modyfikacja i anulowanie rezerwacji.", "moje-rezerwacje"));

        Div blok = new Div(tytul, opis, siatka);
        blok.setWidthFull();
        blok.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)");
        return blok;
    }

    // ------------------------------------------------------------------
    // Pracownicy (moduły z diagramów)
    // ------------------------------------------------------------------

    private Div utworzPanelPracownika() {
        H2 tytul = new H2("Panel pracownika – " + rola.etykieta());
        tytul.getStyle().set("margin", "0");

        Paragraph opis = new Paragraph("Wybierz moduł, z którego chcesz skorzystać.");
        opis.getStyle().set("margin", "0").set("opacity", "0.8");

        Div siatka = siatka("260px");

        switch (rola) {
            case PRACOWNIK_OBSLUGI -> siatka.add(
                    kafelekRezerwacje(), kafelekWypozyczenia(), kafelekStanowiskoObslugi());
            case PRACOWNIK_GARDEROBY -> siatka.add(kafelekGarderoba(), kafelekSkanerQr());
            case SERWISANT -> siatka.add(kafelekKonserwacja());
            case KIEROWNIK -> siatka.add(
                    kafelekAdministracja(), kafelekGarderobaKierownik(), kafelekRaporty(),
                    kafelekRezerwacje(), kafelekWypozyczenia(), kafelekStanowiskoObslugi(),
                    kafelekGarderoba(), kafelekKonserwacja());
            default -> { }
        }

        Div blok = new Div(tytul, opis, siatka);
        blok.setWidthFull();
        blok.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)");
        return blok;
    }

    private Div kafelekAdministracja() {
        return kafelek(VaadinIcon.USERS, "Administracja",
                "Konta pracowników i kierowników: dodawanie, edycja, dezaktywacja.", "administracja");
    }

    private Div kafelekGarderobaKierownik() {
        return kafelek(VaadinIcon.ARCHIVE, "Zarządzanie asortymentem",
                "Dodawanie i wycofywanie modeli oraz egzemplarzy, cennik.", "asortyment");
    }

    private Div kafelekRaporty() {
        return kafelek(VaadinIcon.CHART, "Raporty",
                "Zestawienia przychodów oraz analiza popularności modeli.", "raporty");
    }

    private Div kafelekRezerwacje() {
        return kafelek(VaadinIcon.CALENDAR, "Rezerwacje",
                "Tworzenie, modyfikacja i anulowanie rezerwacji.", "rezerwacje");
    }

    private Div kafelekWypozyczenia() {
        return kafelek(VaadinIcon.EXCHANGE, "Wypożyczenia",
                "Wydawanie egzemplarzy, przyjęcie zwrotu, naliczanie opłat.", "wypozyczenia");
    }

    private Div kafelekStanowiskoObslugi() {
        return kafelek(VaadinIcon.USER, "Stanowisko obsługi",
                "Wyszukiwanie klientów, lista wypożyczonych, historia współpracy.", "stanowisko-obslugi");
    }

    private Div kafelekGarderoba() {
        return kafelek(VaadinIcon.CLIPBOARD_TEXT, "Garderoba",
                "Lokalizacja, kontrole stanu, zlecenia czyszczenia i napraw.", "garderoba");
    }

    private Div kafelekSkanerQr() {
        return kafelek(VaadinIcon.QRCODE, "Skanuj kod QR",
                "Szybkie przejście do egzemplarza lub zmiana jego lokalizacji.", "garderoba");
    }

    private Div kafelekKonserwacja() {
        return kafelek(VaadinIcon.TOOLS, "Konserwacja",
                "Lista zleceń, wykonanie czyszczenia i napraw.", "konserwacja");
    }

    // ------------------------------------------------------------------
    // Sekcje wspólne (Gość i Klient)
    // ------------------------------------------------------------------

    private Div utworzSekcjeKategorii() {
        Div siatka = siatka("240px");
        for (Kategoria k : KATEGORIE) {
            Span ikona = new Span(k.ikona());
            ikona.getStyle().set("font-size", "2rem").set("line-height", "1");
            ikona.getElement().setAttribute("aria-hidden", "true");

            H4 nazwa = new H4(k.nazwa());
            nazwa.getStyle().set("margin", "0");
            Paragraph opis = new Paragraph(k.opis());
            opis.getStyle().set("margin", "0").set("opacity", "0.7");

            Div karta = new Div(ikona, nazwa, opis);
            stylKarty(karta);
            klikalna(karta, () -> idzDo("katalog"));
            siatka.add(karta);
        }
        return sekcja("Nasz asortyment", siatka);
    }

    /** Przykładowe parametry operacyjne i opłaty z opisu świata rzeczywistego. */
    private Div utworzSekcjeZasad() {
        Div siatka = siatka("240px");
        siatka.add(
                infoKarta("📅 Okres wypożyczenia", "Standardowo od 1 do 5 dni."),
                infoKarta("👕 Limit egzemplarzy", "Jednocześnie maksymalnie 5 egzemplarzy."),
                infoKarta("💰 Stawki od 15 zł / dzień",
                        "Dodatki 15 zł, proste kostiumy 30 zł, stroje historyczne 60 zł, "
                                + "suknie wieczorowe 90 zł."),
                infoKarta("⏰ Opóźnienie zwrotu", "20 zł za każdy dzień opóźnienia."));
        return sekcja("Zasady wypożyczania", siatka);
    }

    private Div utworzStopke() {
        Paragraph tekst = new Paragraph("Wypożyczalnia kostiumów i strojów okolicznościowych · "
                + "Lokal z magazynem, przymierzalnią i stanowiskiem obsługi klientów");
        tekst.getStyle().set("margin", "0").set("opacity", "0.6").set("text-align", "center");

        Div stopka = new Div(tekst);
        stopka.setWidthFull();
        stopka.getStyle()
                .set("box-sizing", "border-box")
                .set("padding", "var(--lumo-space-l)")
                .set("margin-top", "var(--lumo-space-l)")
                .set("border-top", "1px solid var(--lumo-contrast-20pct)");
        return stopka;
    }

    // ------------------------------------------------------------------
    // Elementy pomocnicze (wygląd)
    // ------------------------------------------------------------------

    private Div ramkaBohatera(com.vaadin.flow.component.Component... elementy) {
        Div ramka = new Div(elementy);
        ramka.setWidthFull();
        ramka.getStyle()
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-xl)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background", "var(--lumo-contrast-5pct)");
        return ramka;
    }

    private Div sekcja(String naglowek, Div tresc) {
        H3 h = new H3(naglowek);
        h.getStyle().set("margin", "0");
        Div s = new Div(h, tresc);
        s.setWidthFull();
        s.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)")
                .set("margin-top", "var(--lumo-space-m)");
        return s;
    }

    private Div siatka(String minSzerokosc) {
        Div siatka = new Div();
        siatka.setWidthFull();
        siatka.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(" + minSzerokosc + ", 1fr))")
                .set("gap", "var(--lumo-space-m)");
        return siatka;
    }

    private Div kafelek(VaadinIcon ikona, String tytul, String opis, String trasa) {
        Icon i = ikona.create();
        i.setSize("2rem");

        H4 t = new H4(tytul);
        t.getStyle().set("margin", "0");
        Paragraph o = new Paragraph(opis);
        o.getStyle().set("margin", "0").set("opacity", "0.7");

        Div karta = new Div(i, t, o);
        stylKarty(karta);
        klikalna(karta, () -> idzDo(trasa));
        return karta;
    }

    private Div krok(String numer, String tytul, String opis) {
        Span n = new Span(numer);
        n.getElement().getThemeList().add("badge");
        n.getElement().getThemeList().add("pill");
        n.getElement().getThemeList().add("primary");

        H4 t = new H4(tytul);
        t.getStyle().set("margin", "0");
        Paragraph o = new Paragraph(opis);
        o.getStyle().set("margin", "0").set("opacity", "0.7");

        Div karta = new Div(n, t, o);
        stylKarty(karta);
        return karta;
    }

    private Div infoKarta(String tytul, String opis) {
        H4 t = new H4(tytul);
        t.getStyle().set("margin", "0");
        Paragraph o = new Paragraph(opis);
        o.getStyle().set("margin", "0").set("opacity", "0.7");

        Div karta = new Div(t, o);
        stylKarty(karta);
        return karta;
    }

    private void stylKarty(Div karta) {
        karta.getStyle()
                .set("box-sizing", "border-box")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-m)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");
    }

    private void klikalna(Div karta, Runnable akcja) {
        karta.getStyle().set("cursor", "pointer");
        karta.addClickListener(e -> akcja.run());
    }

    // ------------------------------------------------------------------
    // Nawigacja
    // ------------------------------------------------------------------

    private void idzDo(String trasa) {
        if (ISTNIEJACE_TRASY.contains(trasa)) {
            UI.getCurrent().navigate(trasa);
        } else {
            Notification.show("Ten widok powstanie w kolejnym kroku projektu.");
        }
    }
}