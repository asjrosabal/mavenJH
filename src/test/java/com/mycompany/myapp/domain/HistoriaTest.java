package com.mycompany.myapp.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class HistoriaTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Historia.class);
        Historia historia1 = new Historia();
        historia1.setId(1L);
        Historia historia2 = new Historia();
        historia2.setId(historia1.getId());
        assertThat(historia1).isEqualTo(historia2);
        historia2.setId(2L);
        assertThat(historia1).isNotEqualTo(historia2);
        historia1.setId(null);
        assertThat(historia1).isNotEqualTo(historia2);
    }
}
