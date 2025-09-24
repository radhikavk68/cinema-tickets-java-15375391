package uk.gov.dwp.uc.pairtest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TicketTypeRequestTest {

    @Test
    void shouldThrowExceptionWhenTypeIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new TicketTypeRequest(null, 1)
        );

        assertEquals("Ticket type cannot be null", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNoOfTicketsIsZero() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0)
        );

        assertEquals("Number of tickets must be greater than zero", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNoOfTicketsIsNegative() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new TicketTypeRequest(TicketTypeRequest.Type.CHILD, -5)
        );

        assertEquals("Number of tickets must be greater than zero", ex.getMessage());
    }

    @Test
    void shouldCreateValidTicketTypeRequest() {
        TicketTypeRequest req = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        assertEquals(TicketTypeRequest.Type.INFANT, req.getTicketType());
        assertEquals(2, req.getNoOfTickets());
    }
}