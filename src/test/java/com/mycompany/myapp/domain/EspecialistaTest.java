package com.mycompany.myapp.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EspecialistaTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Especialista.class);
        Especialista especialista1 = new Especialista();
        especialista1.setId(1L);
        Especialista especialista2 = new Especialista();
        especialista2.setId(especialista1.getId());
        assertThat(especialista1).isEqualTo(especialista2);
        especialista2.setId(2L);
        assertThat(especialista1).isNotEqualTo(especialista2);
        especialista1.setId(null);
        assertThat(especialista1).isNotEqualTo(especialista2);
    }
}
