package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Arrays;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_TICKETS = 25; // Consider externalizing to properties
    private static final int ADULT_PRICE = 25;
    private static final int CHILD_PRICE = 15;
    private static final int INFANT_PRICE = 0;

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        var ticketSummary = validatePurchaseRequest(accountId, ticketTypeRequests);

        var totalAmount = ticketSummary.totalAmount();
        var seatsToReserve = ticketSummary.seatsToReserve();

        paymentService.makePayment(accountId, totalAmount);
        reservationService.reserveSeat(accountId, seatsToReserve);
    }

    private TicketSummary validatePurchaseRequest(Long accountId, TicketTypeRequest... requests) throws InvalidPurchaseException {
        if (accountId == null || accountId <= 0)
            throw new InvalidPurchaseException("Account ID is invalid. Please provide a valid account.");

        if (requests == null || requests.length == 0)
            throw new InvalidPurchaseException("No tickets selected. Please choose at least one ticket.");

        var summary = aggregateTicketCounts(requests);

        if (summary.totalTickets() > MAX_TICKETS)
            throw new InvalidPurchaseException("You can purchase a maximum of %d tickets per transaction.".formatted(MAX_TICKETS));

        if (summary.adultTickets() == 0 && (summary.childTickets() > 0 || summary.infantTickets() > 0))
            throw new InvalidPurchaseException("At least one Adult ticket is required when purchasing Child or Infant tickets.");

        if (summary.infantTickets() > summary.adultTickets())
            throw new InvalidPurchaseException("Each Infant must be accompanied by one Adult.");

        return summary;
    }

    private TicketSummary aggregateTicketCounts(TicketTypeRequest... requests) {
        int adults = Arrays.stream(requests)
                .filter(req -> req.getTicketType() == TicketTypeRequest.Type.ADULT)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();

        int children = Arrays.stream(requests)
                .filter(req -> req.getTicketType() == TicketTypeRequest.Type.CHILD)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();

        int infants = Arrays.stream(requests)
                .filter(req -> req.getTicketType() == TicketTypeRequest.Type.INFANT)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();

        return new TicketSummary(adults, children, infants);
    }

    //As the usage is limited to this class only, therefore nester/private record to keep encapsulation tight.
    private record TicketSummary(int adultTickets, int childTickets, int infantTickets) {
        int totalTickets() {
            return adultTickets + childTickets + infantTickets;
        }

        int totalAmount() {
            return adultTickets * ADULT_PRICE
                    + childTickets * CHILD_PRICE
                    + infantTickets * INFANT_PRICE;
        }

        int seatsToReserve() {
            return adultTickets + childTickets;
        }
    }
}
