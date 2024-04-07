package org.example.busmanager;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.example.busmanager.entity.Bus;
import org.example.busmanager.entity.City;
import org.example.busmanager.entity.CurrencyResponse;
import org.example.busmanager.service.BusService;
import org.example.busmanager.service.CityService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Route
public class MainView extends VerticalLayout {

    private final CityService cityService;
    private final BusService busService;

    Grid<Bus> routeGrid = new Grid<>(Bus.class);
    ReservationForm reservationForm;
    ComboBox<City> DepartureBox;
    ComboBox<City> ArrivalBox;
    Button searchButton;


    public MainView(CityService cityService, BusService busService) {
        this.cityService = cityService;
        this.busService = busService;
        setSizeFull();
        configureGrid();
        configureForm();

        add(new CitySelector(), getContent());
    }

    public void configureGrid() {
        routeGrid.setSizeFull();
        routeGrid.addColumn(Bus::getName).setHeader("Name");
        routeGrid.addColumn(e -> extractHourAndMinute(e.getRoute().getDeparture())).setHeader("Departure Time");
        routeGrid.addColumn(e -> extractHourAndMinute(e.getRoute().getArrival())).setHeader("Arrival Time");
        routeGrid.addColumn(this::formatSeats).setHeader("Taken Seats");
        routeGrid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    public void configureForm() {
        reservationForm = new ReservationForm();
        reservationForm.setWidth("25em");
    }

    public Component getContent() {
        HorizontalLayout content = new HorizontalLayout(routeGrid, reservationForm);
        content.setFlexGrow(2, routeGrid);
        content.setFlexGrow(1, reservationForm);
        content.setSizeFull();
        content.addClassName("content");
        return content;
    }

    public HoursMinutes extractHourAndMinute(Long epoch) {
        LocalDateTime date = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC);
        return new HoursMinutes(date);
    }

    public String formatSeats(Bus bus) {
        return busService.getTakenSeatNumbers(bus).size() + "/" + busService.getMaximumSeatsCount(bus);
    }

    static class Notifications {
        public static Notification defaultNotification(String message) {
            return Notification.show(message);
        }

        public static Notification errorNotification(String message) {
            Notification notification = new Notification();
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            Div content = new Div(new Text(message));

            Button closeButton = new Button(new Icon("lumo", "cross"));
            closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            closeButton.addClickListener(event -> notification.close());

            HorizontalLayout layout = new HorizontalLayout(content, closeButton);
            layout.setAlignItems(Alignment.CENTER);

            notification.add(layout);
            notification.open();
            return notification;
        }
    }

    static class HoursMinutes {
        public int hours;
        public int minutes;

        public HoursMinutes(LocalDateTime time) {
            this.hours = time.getHour();
            this.minutes = time.getMinute();
        }

        public String toString() {
            return hours + ":" + minutes;
        }
    }

    class CitySelector extends HorizontalLayout {
        public CitySelector() {
            DepartureBox = new ComboBox<>("Departure city");
            DepartureBox.addClassName("departure");
            DepartureBox.setItems(cityService.getAllCities());
            DepartureBox.setItemLabelGenerator(City::getName);

            ArrivalBox = new ComboBox<>("Arrival city");
            ArrivalBox.addClassName("arrival");
            ArrivalBox.setItems(cityService.getAllCities());
            ArrivalBox.setItemLabelGenerator(City::getName);

            add(DepartureBox);
            add(ArrivalBox);

            searchButton = getButton(DepartureBox, ArrivalBox);
            add(searchButton);
        }

        private Button getButton(ComboBox<City> cityComboBox, ComboBox<City> cityComboBox2) {
            Button searchButton = new Button("Search");
            searchButton.addClassName("search");
            searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            searchButton.addClickListener(e -> {
                if (cityComboBox.getValue().equals(cityComboBox2.getValue())) {
                    Notifications.errorNotification("Departure and arrival cities cannot be the same");
                } else if (cityComboBox.getValue() != null && cityComboBox2.getValue() != null) {
                    Notification.show("Searching for routes from " + cityComboBox.getValue().getName() + " to " + cityComboBox2.getValue().getName());
                } else {
                    Notifications.errorNotification("Please select both departure and arrival cities");
                }
                System.out.println(cityComboBox.getValue() + " -> " + cityComboBox2.getValue());
                System.out.println(cityComboBox.getValue().equals(cityComboBox2.getValue()));

                List<Bus> buses = busService.getBusByDepartureCityAndArrivalCity(cityComboBox.getValue().getName(), cityComboBox2.getValue().getName());
                routeGrid.setItems(buses);
            });

            return searchButton;
        }
    }

    class ReservationForm extends FormLayout {
        TextField name = new TextField("Name");
        EmailField email = new EmailField("Email");
        TextField phone = new TextField("Phone");

        TextField price = new TextField("Price");
        ComboBox<CurrencyResponse.Rate> currency = new ComboBox<>("Currency");

        Button reserve = new Button("Reserve");
        Button cancel = new Button("Cancel");

        public ReservationForm() {
            addClassName("reservation-form");

            add(name, email, phone, price, currency, createButtonsLayout());
        }

        private HorizontalLayout createButtonsLayout() {
            reserve.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            reserve.addClickShortcut(Key.ENTER);
            cancel.addClickShortcut(Key.ESCAPE);

            reserve.addClickListener(event -> {
                Notifications.defaultNotification("Reserved");
            });

            cancel.addClickListener(event -> {
                Notifications.defaultNotification("Cancelled");
            });

            return new HorizontalLayout(reserve, cancel);
        }
    }
}
