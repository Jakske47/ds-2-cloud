package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Model {

    List<Booking> bookings = new ArrayList<>();

    private static final String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    @Resource(name = "webClientBuilder")
    private WebClient.Builder webClientBuilder;

    public List<Show> getShows() {
        List<Show> result = new ArrayList<>();
        var shows = webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                .block();
        assert shows != null;
        result.addAll(shows.getContent());
        return result;
    }

    public Show getShow(String company, UUID showId) {
        var show = webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Show>() {})
                .block();
        assert show != null;
        return show;
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        List<LocalDateTime> result = new ArrayList<>();
        var shows = webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .pathSegment("times")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<LocalDateTime>>() {})
                .block();
        assert shows != null;
        result.addAll(shows.getContent());
        return result;
    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        List<Seat> result = new ArrayList<>();
        var shows = webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .pathSegment("seats")
                        .queryParam("time", time.toString())
                        .queryParam("available", "true")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                .block();
        assert shows != null;
        result.addAll(shows.getContent());
        return result;
    }

    public List<Seat> getAllSeats(String company, UUID showId, LocalDateTime time) {
        List<Seat> result = new ArrayList<>();
        var shows = webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .pathSegment("seats")
                        .queryParam("time", time.toString())
                        .queryParam("available", "false")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                .block();
        assert shows != null;
        result.addAll(shows.getContent());
        return result;
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        var seat = webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .pathSegment("seats")
                        .pathSegment(seatId.toString())
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Seat>() {})
                .block();
        assert seat != null;
        return seat;
    }

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        var ticket = webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .pathSegment("seats")
                        .pathSegment(seatId.toString())
                        .pathSegment("ticket")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                .block();
        return ticket;
    }

    public List<Booking> getBookings(String customer) {
        return bookings.stream().filter(booking -> booking.getCustomer().equals(customer)).collect(Collectors.toList());
    }

    public List<Booking> getAllBookings() {
        return bookings;
    }

    public Set<String> getBestCustomers() {
        Map<String, Integer> counter = new TreeMap<>();
        for (Booking booking:bookings){
            int actual = counter.getOrDefault(booking.getCustomer(), 0);
            counter.put(booking.getCustomer(),actual + booking.getTickets().size());
        }
        int max = 0;
        Set<String> result = new HashSet<>();
        for (String customer: counter.keySet()){
            int total = counter.get(customer);
            if (total > max){
                result = new HashSet<>();
                result.add(customer);
                max = total;
            }
            else if (total == max){
                result.add(customer);
            }
        }
        return result;
    }


    public void confirmQuotes(List<Quote> quotes, String customer) {
        List<Ticket> tickets = new ArrayList<>();
        for (Quote quote : quotes) {
            webClientBuilder
                    .baseUrl("https://reliabletheatrecompany.com/")
                    .build()
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("shows")
                            .pathSegment(quote.getShowId().toString())
                            .pathSegment("seats")
                            .pathSegment(quote.getSeatId().toString())
                            .pathSegment("ticket")
                            .queryParam("customer", customer)
                            .queryParam("key", API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                    .block();
            tickets.add(getTicket(quote.getCompany(), quote.getShowId(), quote.getSeatId()));
        }
        bookings.add(new Booking(UUID.randomUUID(), LocalDateTime.now(), tickets, customer));
    }
}
