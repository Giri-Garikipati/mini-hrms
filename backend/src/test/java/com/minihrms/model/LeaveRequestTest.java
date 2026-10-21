package com.minihrms.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveRequestTest {

    @Test
    void countsBothStartAndEndDay() {
        LeaveRequest l = new LeaveRequest(null, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4), null);
        assertThat(l.getDays()).isEqualTo(3);
    }

    @Test
    void singleDayLeaveIsOneDay() {
        LocalDate d = LocalDate.of(2026, 11, 2);
        assertThat(new LeaveRequest(null, d, d, null).getDays()).isEqualTo(1);
    }
}
