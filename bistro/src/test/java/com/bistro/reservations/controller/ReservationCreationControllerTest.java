package com.bistro.reservations.controller;

import com.bistro.reservations.model.Reservation;
import com.bistro.reservations.model.ReservationStatus;
import com.bistro.reservations.repository.ReservationRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ReservationCreationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor anaToken(){
        return jwt().jwt(token -> token
                .subject("ana-id")
                .claim("name", "Ana García")
                .claim("email", "ana@example.com"));
    }

    @Test
    void shouldConfirmReservation() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .with(anaToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationTime": "2026-08-20T19:00:00",
                                  "partySize": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String code = JsonPath.read(result.getResponse().getContentAsString(), "$.reservationCode");

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            Reservation reservation = reservationRepository.findByReservationCode(code)
                                    .orElseThrow();
                            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
                            assertThat(reservation.getAssignedTableId()).isNotNull();
                        }
                );


    }

    @Test
    void shouldRejectReservationWhenNoCapacity() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .with(anaToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "reservationTime": "2026-08-20T21:00:00",
                              "partySize": 10
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String code = JsonPath.read(result.getResponse().getContentAsString(), "$.reservationCode");

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Reservation reservation = reservationRepository.findByReservationCode(code).orElseThrow();
                    assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
                    assertThat(reservation.getAssignedTableId()).isNull();
                });
    }

    @Test
    void shouldReturn400WhenPartySizeExceeds12() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .with(anaToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationTime": "2026-08-20T20:00:00",
                                  "partySize": 13
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .with(anaToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationTime": null,
                                  "partySize": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldReturn400WhenReservationIsOutsideTurns() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .with(anaToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "reservationTime": "2026-08-20T21:30:00",
                              "partySize": 4
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message")
                        .value("La reserva debe ser en un turno: 12:00, 14:00, 19:00 o 21:00"));
    }


}












