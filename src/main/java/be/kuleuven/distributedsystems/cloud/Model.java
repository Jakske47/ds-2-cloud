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
        List<Show> result = getShows("https://reliabletheatrecompany.com/", 0);
        result.addAll(getShows("https://unreliabletheatrecompany.com/", 0));
        return result;
    }

    public List<Show> getShows(String company, int retry){
        List<Show> result = new ArrayList<>();
        if (retry == 3) return result;
        try {
            var shows = webClientBuilder
                    .baseUrl(company)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("shows")
                            .queryParam("key", API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {
                    })
                    .block();
            result.addAll(shows.getContent());
        }catch (Exception e){
            System.out.println(e.getMessage());
            result = getShows(company, ++retry);
        }

        return result;
    }

    public Show getShow(String company, UUID showId) {
        return getShow(company, showId, 0);
    }

    public Show getShow(String company, UUID showId, int retry) {
        if (retry == 3) return null;
        try {
            var show = webClientBuilder
                    .baseUrl("https://" + company + "/")
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("shows")
                            .pathSegment(showId.toString())
                            .queryParam("key", API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Show>() {
                    })
                    .block();
            assert show != null;
            return show;
        }catch (Exception e){
            System.out.println(e.getMessage());
            return getShow(company, showId, ++retry);
        }
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        return getShowTimes(company, showId, 0);
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId, int retry) {
        if (retry == 3) return null;
        try{
            List<LocalDateTime> result = new ArrayList<>();
            var showTimes = webClientBuilder
                .baseUrl("https://" + company + "/")
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
            result.addAll(showTimes.getContent());
            return result;
        }catch (Exception e){
            System.out.println(e.getMessage());
            return getShowTimes(company, showId, ++retry);
        }
    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        return getAvailableSeats(company, showId, time, 0);
    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time, int retry) {
        if (retry == 3) return null;
        try{
            List<Seat> result = new ArrayList<>();
            var seats = webClientBuilder
                .baseUrl("https://" + company + "/")
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
            result.addAll(seats.getContent());
            return result;
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            return getAvailableSeats(company, showId, time, ++retry);
        }
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        return getSeat(company, showId, seatId, 0);
    }

    public Seat getSeat(String company, UUID showId, UUID seatId, int retry) {
        if (retry == 3) return null;
        try{
            var seat = webClientBuilder
                .baseUrl("https://" + company + "/")
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
            return seat;
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            return getSeat(company, showId, seatId, ++retry);
        }
    }

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        return getTicket(company, showId, seatId, 0);
    }

    public Ticket getTicket(String company, UUID showId, UUID seatId, int retry) {
        if (retry == 3) return null;
        try{
            var ticket = webClientBuilder
                .baseUrl("https://" + company + "/")
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
        catch (Exception e){
            System.out.println(e.getMessage());
            return getTicket(company, showId, seatId, ++retry);
        }
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
            try{
                var ticket = webClientBuilder
                    .baseUrl("https://" + quote.getCompany() + "/")
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
                tickets.add(ticket);
            }catch (Exception e){
                System.out.println(e.getMessage());
                System.out.println("deleting previous tickets");
                deleteTickets(tickets, 0);
                return;
            }
        }
        bookings.add(new Booking(UUID.randomUUID(), LocalDateTime.now(), tickets, customer));
    }

    private void deleteTickets(List<Ticket> tickets, int retry){
        if (retry==3) return;
        for (Ticket ticket : tickets) {
            try {
                webClientBuilder
                        .baseUrl("https://" + ticket.getCompany() + "/")
                        .build()
                        .delete()
                        .uri(uriBuilder -> uriBuilder
                                .pathSegment("shows")
                                .pathSegment(ticket.getShowId().toString())
                                .pathSegment("seats")
                                .pathSegment(ticket.getSeatId().toString())
                                .pathSegment("ticket")
                                .pathSegment(ticket.getTicketId().toString())
                                .queryParam("key", API_KEY)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                        })
                        .block();
                tickets.remove(ticket);
            }catch (Exception e){
                System.out.println(e.getMessage());
                deleteTickets(tickets, ++retry);
            }
        }
    }
}
