package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketPaymentService paymentService;

    @Mock
    private SeatReservationService reservationService;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    // ✅ Positive Scenarios

    @Test
    void purchaseAdultTickets_success() {
        var adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

        ticketService.purchaseTickets(1L, adult);

        verify(paymentService).makePayment(1L, 50);     // 2 * £25
        verify(reservationService).reserveSeat(1L, 2); // 2 seats
    }

    @Test
    void purchaseAdultAndChildTickets_success() {
        var adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        var child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);

        ticketService.purchaseTickets(1L, adult, child);

        verify(paymentService).makePayment(1L, 55);     // 25 + (2*15)
        verify(reservationService).reserveSeat(1L, 3); // 1 adult + 2 children
    }

    @Test
    void purchaseAdultChildAndInfantTickets_success() {
        var adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        var child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        var infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(1L, adult, child, infant);

        verify(paymentService).makePayment(1L, 40);     // 25 + 15 + 0
        verify(reservationService).reserveSeat(1L, 2); // infant doesn’t need seat
    }

    @Test
    void purchaseMaxTickets_success() {
        var adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25);

        ticketService.purchaseTickets(1L, adult);

        verify(paymentService).makePayment(1L, 625);     // 25 * 25
        verify(reservationService).reserveSeat(1L, 25); // 25 seats
    }

    // ❌ Negative Scenarios

    @Test
    void rejectPurchase_whenAccountIdIsNullOrInvalid() {
        var adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        assertAll(
                () -> {
                    var ex = assertThrows(InvalidPurchaseException.class,
                            () -> ticketService.purchaseTickets(null, adult));
                    assertEquals("Account ID is invalid. Please provide a valid account.", ex.getMessage());
                },
                () -> {
                    var ex = assertThrows(InvalidPurchaseException.class,
                            () -> ticketService.purchaseTickets(0L, adult));
                    assertEquals("Account ID is invalid. Please provide a valid account.", ex.getMessage());
                }
        );

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void rejectPurchase_whenNoTicketsProvided() {
        assertAll(
                () -> {
                    var ex = assertThrows(InvalidPurchaseException.class,
                            () -> ticketService.purchaseTickets(1L));
                    assertEquals("No tickets selected. Please choose at least one ticket.", ex.getMessage());
                },
                () -> {
                    var ex = assertThrows(InvalidPurchaseException.class,
                            () -> ticketService.purchaseTickets(1L, (TicketTypeRequest[]) null));
                    assertEquals("No tickets selected. Please choose at least one ticket.", ex.getMessage());
                },
                () -> {
                    var ex = assertThrows(InvalidPurchaseException.class,
                            () -> ticketService.purchaseTickets(1L, new TicketTypeRequest[0]));
                    assertEquals("No tickets selected. Please choose at least one ticket.", ex.getMessage());
                }
        );

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void rejectPurchase_whenChildOrInfantWithoutAdult() {
        var child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        var infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        assertAll(
                () -> {
                    var ex = assertThrows(InvalidPurchaseException.class,
                            () -> ticketService.purchaseTickets(1L, child));
                    assertEquals("At least one Adult ticket is required when purchasing Child or Infant tickets.", ex.getMessage());
                },
                () -> {
                    var ex = assertThrows(InvalidPurchaseException.class,
                            () -> ticketService.purchaseTickets(1L, infant));
                    assertEquals("At least one Adult ticket is required when purchasing Child or Infant tickets.", ex.getMessage());
                }
        );

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void rejectPurchase_whenInfantsExceedAdults() {
        var adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        var infants = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        var ex = assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adult, infants));

        assertEquals("Each Infant must be accompanied by one Adult.", ex.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void rejectPurchase_whenExceedingMaxTickets() {
        var adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);

        var ex = assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adult));

        assertEquals("You can purchase a maximum of 25 tickets per transaction.", ex.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }

}
