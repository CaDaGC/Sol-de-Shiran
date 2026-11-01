package pe.upeu.edu.seguridad.security;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RateLimitFilterTest {
    @Test
    void bloqueaDespuesDeTreintaIntentosYSeRecupera() {
        var limiter = new RateLimitFilter();
        for (int i = 0; i < 30; i++) assertThat(limiter.permitir("ip", 1000)).isTrue();
        assertThat(limiter.permitir("ip", 1000)).isFalse();
        assertThat(limiter.permitir("otra-ip", 1000)).isTrue();
        assertThat(limiter.permitir("ip", 62000)).isTrue();
    }
}
